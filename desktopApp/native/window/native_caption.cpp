// Native caption support is deliberately separate from window_jni.cpp: that file also contains
// the live WASAPI implementation. No audio code is changed by the title-bar migration.
#define _WIN32_WINNT 0x0A00
#define NOMINMAX
#define WIN32_LEAN_AND_MEAN
#include <jni.h>
#include <windows.h>
#include <windowsx.h>
#include <dwmapi.h>
#include <algorithm>
#include <cstdint>
#include <new>

namespace {
constexpr wchar_t CAPTION_PROPERTY[] = L"BitChord.NativeCaption.v1";
constexpr UINT SET_BACKDROP = WM_APP + 0x53;
constexpr int BACKDROP_ATTRIBUTE = 38;
constexpr int DARK_ATTRIBUTE = 20;
constexpr int CORNER_ATTRIBUTE = 33;
constexpr int CAPTION_COLOR_ATTRIBUTE = 35;
constexpr int BACKDROP_NONE = 1;

struct CaptionState {
    WNDPROC original;
    LONG_PTR old_style;
    bool material = false;
    int last_height = -1;
    bool last_material = false;
};

CaptionState* caption_state(HWND window) {
    return static_cast<CaptionState*>(GetPropW(window, CAPTION_PROPERTY));
}

bool own_window(HWND window) {
    if (!IsWindow(window)) return false;
    DWORD pid = 0;
    GetWindowThreadProcessId(window, &pid);
    return pid == GetCurrentProcessId();
}

UINT window_dpi(HWND window) {
    const UINT dpi = GetDpiForWindow(window);
    return dpi == 0 ? 96 : dpi;
}

int border_size(HWND window, bool horizontal) {
    const UINT dpi = window_dpi(window);
    return GetSystemMetricsForDpi(horizontal ? SM_CXFRAME : SM_CYFRAME, dpi) +
        GetSystemMetricsForDpi(SM_CXPADDEDBORDER, dpi);
}

struct CaptionMetrics {
    int height;
    int right_inset;
};

CaptionMetrics caption_metrics(HWND window) {
    const UINT dpi = window_dpi(window);
    CaptionMetrics result{
        std::max(MulDiv(32, dpi, 96), GetSystemMetricsForDpi(SM_CYCAPTION, dpi) + border_size(window, false)),
        MulDiv(160, dpi, 96),
    };
    // DWM explicitly documents undefined bounds for a hidden/minimized window.
    if (!IsWindowVisible(window) || IsIconic(window)) return result;
    RECT buttons{}, bounds{}, client{};
    POINT origin{};
    if (FAILED(DwmGetWindowAttribute(window, DWMWA_CAPTION_BUTTON_BOUNDS, &buttons, sizeof(buttons))) ||
        !GetWindowRect(window, &bounds) || !GetClientRect(window, &client) || !ClientToScreen(window, &origin)) {
        return result;
    }
    if (buttons.right <= buttons.left || buttons.bottom <= buttons.top) return result;
    // The attribute is window-relative, not client-relative. Maximization changes their offset.
    const int bottom = static_cast<int>(bounds.top + buttons.bottom - origin.y);
    const int inset = static_cast<int>(client.right - (bounds.left + buttons.left - origin.x));
    if (bottom > 0 && bottom <= MulDiv(128, dpi, 96)) result.height = std::max(result.height, bottom);
    if (inset >= MulDiv(48, dpi, 96) && inset <= MulDiv(640, dpi, 96)) result.right_inset = inset;
    return result;
}

bool frame_margins(HWND window, CaptionState* state, bool force = false) {
    const int height = caption_metrics(window).height;
    if (!force && height == state->last_height && state->material == state->last_material) return true;
    // Caption space must remain extended even when the user turns Mica/Acrylic OFF.
    const MARGINS margins = state->material ? MARGINS{-1, -1, -1, -1} : MARGINS{1, 1, height, 1};
    const int previous_height = state->last_height;
    const bool previous_material = state->last_material;
    // Cache before the Win32 call: a synchronous frame notification must not re-enter forever.
    state->last_height = height;
    state->last_material = state->material;
    if (FAILED(DwmExtendFrameIntoClientArea(window, &margins))) {
        state->last_height = previous_height;
        state->last_material = previous_material;
        return false;
    }
    return true;
}

void frame_colors(HWND window) {
    HIGHCONTRASTW contrast{sizeof(HIGHCONTRASTW)};
    const bool high_contrast = SystemParametersInfoW(SPI_GETHIGHCONTRAST, sizeof(contrast), &contrast, 0) &&
        (contrast.dwFlags & HCF_HIGHCONTRASTON) != 0;
    const BOOL dark = high_contrast ? FALSE : TRUE;
    if (FAILED(DwmSetWindowAttribute(window, static_cast<DWMWINDOWATTRIBUTE>(DARK_ATTRIBUTE), &dark, sizeof(dark)))) {
        // Older Windows 10 builds used attribute 19. Unsupported attributes are harmless.
        DwmSetWindowAttribute(window, static_cast<DWMWINDOWATTRIBUTE>(19), &dark, sizeof(dark));
    }
    const COLORREF caption = high_contrast ? 0xFFFFFFFF : RGB(32, 32, 32);
    DwmSetWindowAttribute(window, static_cast<DWMWINDOWATTRIBUTE>(CAPTION_COLOR_ATTRIBUTE), &caption, sizeof(caption));
}

bool set_backdrop(HWND window, CaptionState* state, int kind) {
    const bool material = kind == 2 || kind == 3;
    const int type = material ? kind : BACKDROP_NONE;
    const bool applied = SUCCEEDED(DwmSetWindowAttribute(
        window, static_cast<DWMWINDOWATTRIBUTE>(BACKDROP_ATTRIBUTE), &type, sizeof(type)));
    if (applied) state->material = material;
    frame_colors(window);
    // Never collapse the native caption to the old one-pixel top margin on failure or OFF.
    const bool extended = frame_margins(window, state, true);
    return applied && extended;
}

LRESULT CALLBACK caption_proc(HWND window, UINT message, WPARAM wparam, LPARAM lparam) {
    CaptionState* state = caption_state(window);
    if (state == nullptr) return DefWindowProcW(window, message, wparam, lparam);
    const WNDPROC original = state->original;
    LRESULT dwm_result = 0;
    const BOOL dwm_handled = DwmDefWindowProc(window, message, wparam, lparam, &dwm_result);

    if (message == WM_NCDESTROY) {
        RemovePropW(window, CAPTION_PROPERTY);
        const LRESULT result = CallWindowProcW(original, window, message, wparam, lparam);
        delete state;
        return result;
    }
    if (message == SET_BACKDROP) return set_backdrop(window, state, static_cast<int>(wparam)) ? 1 : 0;

    if (message == WM_NCCALCSIZE && wparam == TRUE) {
        // Keep WS_CAPTION and the OS buttons, but extend the client behind their DWM frame.
        if (IsZoomed(window)) {
            MONITORINFO monitor{sizeof(MONITORINFO)};
            const HMONITOR handle = MonitorFromWindow(window, MONITOR_DEFAULTTONEAREST);
            if (handle != nullptr && GetMonitorInfoW(handle, &monitor)) {
                reinterpret_cast<NCCALCSIZE_PARAMS*>(lparam)->rgrc[0] = monitor.rcWork;
            }
        }
        return 0;
    }
    // Includes HTMAXBUTTON for Windows 11 Snap Layouts, and WM_NCMOUSELEAVE to clear hover.
    if (dwm_handled) return dwm_result;

    if (message == WM_NCHITTEST) {
        POINT point{GET_X_LPARAM(lparam), GET_Y_LPARAM(lparam)};
        RECT bounds{};
        if (GetWindowRect(window, &bounds) && !IsZoomed(window)) {
            const int bx = border_size(window, true);
            const int by = border_size(window, false);
            const bool left = point.x < bounds.left + bx;
            const bool right = point.x >= bounds.right - bx;
            const bool top = point.y < bounds.top + by;
            const bool bottom = point.y >= bounds.bottom - by;
            if (top && left) return HTTOPLEFT;
            if (top && right) return HTTOPRIGHT;
            if (bottom && left) return HTBOTTOMLEFT;
            if (bottom && right) return HTBOTTOMRIGHT;
            if (left) return HTLEFT;
            if (right) return HTRIGHT;
            if (top) return HTTOP;
            if (bottom) return HTBOTTOM;
        }
        RECT client{};
        if (ScreenToClient(window, &point) && GetClientRect(window, &client) &&
            point.x >= 0 && point.x < client.right && point.y >= 0 &&
            point.y < caption_metrics(window).height) {
            return HTCAPTION;
        }
        // Outside this dedicated strip, all input stays with AWT/Compose.
    }

    switch (message) {
    case WM_NCLBUTTONDOWN:
    case WM_NCLBUTTONUP:
    case WM_NCLBUTTONDBLCLK:
    case WM_NCRBUTTONDOWN:
    case WM_NCRBUTTONUP:
    case WM_NCRBUTTONDBLCLK:
    case WM_NCMOUSEMOVE:
    case WM_NCMOUSELEAVE:
    case WM_NCPAINT:
    case WM_NCACTIVATE:
        // AWT still considers the peer undecorated. Let Windows implement NC interactions.
        // Resulting WM_SYSCOMMAND/WM_CLOSE messages go through ORIGINAL below, preserving
        // Window.onCloseRequest, close-to-tray, and Compose's minimize/maximize notifications.
        return DefWindowProcW(window, message, wparam, lparam);
    default:
        break;
    }

    const LRESULT result = CallWindowProcW(original, window, message, wparam, lparam);
    if (IsWindow(window) && caption_state(window) == state) {
        switch (message) {
        case WM_ACTIVATE:
        case WM_SIZE:
        case WM_DPICHANGED:
            frame_margins(window, state);
            break;
        case WM_THEMECHANGED:
        case WM_SETTINGCHANGE:
        case WM_DWMCOMPOSITIONCHANGED:
            frame_colors(window);
            frame_margins(window, state, true);
            break;
        default:
            break;
        }
    }
    return result;
}

bool install_caption(HWND window) {
    if (!own_window(window)) return false;
    if (caption_state(window) != nullptr) return true;
    auto original = reinterpret_cast<WNDPROC>(GetWindowLongPtrW(window, GWLP_WNDPROC));
    if (original == nullptr) return false;
    auto* state = new (std::nothrow) CaptionState{original, GetWindowLongPtrW(window, GWL_STYLE)};
    if (state == nullptr) return false;
    if (!SetPropW(window, CAPTION_PROPERTY, state)) { delete state; return false; }
    SetLastError(0);
    const LONG_PTR replaced = SetWindowLongPtrW(window, GWLP_WNDPROC, reinterpret_cast<LONG_PTR>(caption_proc));
    if (replaced == 0 && GetLastError() != 0) {
        RemovePropW(window, CAPTION_PROPERTY);
        delete state;
        return false;
    }
    auto rollback = [&] {
        SetWindowLongPtrW(window, GWLP_WNDPROC, reinterpret_cast<LONG_PTR>(original));
        SetWindowLongPtrW(window, GWL_STYLE, state->old_style);
        RemovePropW(window, CAPTION_PROPERTY);
        delete state;
        SetWindowPos(window, nullptr, 0, 0, 0, 0,
            SWP_FRAMECHANGED | SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE);
        return false;
    };
    const LONG_PTR style = (state->old_style & ~static_cast<LONG_PTR>(WS_POPUP)) |
        WS_CAPTION | WS_SYSMENU | WS_THICKFRAME | WS_MINIMIZEBOX | WS_MAXIMIZEBOX;
    SetLastError(0);
    if (SetWindowLongPtrW(window, GWL_STYLE, style) == 0 && GetLastError() != 0) return rollback();
    const DWMNCRENDERINGPOLICY policy = DWMNCRP_ENABLED;
    if (FAILED(DwmSetWindowAttribute(window, DWMWA_NCRENDERING_POLICY, &policy, sizeof(policy)))) return rollback();
    const int rounded = 2;
    DwmSetWindowAttribute(window, static_cast<DWMWINDOWATTRIBUTE>(CORNER_ATTRIBUTE), &rounded, sizeof(rounded));
    frame_colors(window);
    if (!frame_margins(window, state, true)) return rollback();
    if (!SetWindowPos(window, nullptr, 0, 0, 0, 0,
        SWP_FRAMECHANGED | SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE)) return rollback();
    RedrawWindow(window, nullptr, nullptr, RDW_FRAME | RDW_INVALIDATE);
    return true;
}

HWND from_java(jlong value) { return reinterpret_cast<HWND>(static_cast<intptr_t>(value)); }
} // namespace

extern "C" {
JNIEXPORT jint JNICALL
Java_com_music_bitchord_desktop_DesktopWindowsFrame_nativeCaptionApiVersion(JNIEnv*, jclass) {
    BOOL enabled = FALSE;
    return SUCCEEDED(DwmIsCompositionEnabled(&enabled)) && enabled ? 1 : 0;
}

JNIEXPORT jboolean JNICALL
Java_com_music_bitchord_desktop_DesktopWindowsFrame_nativeCaptionInstall(JNIEnv*, jclass, jlong handle) {
    return install_caption(from_java(handle)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jintArray JNICALL
Java_com_music_bitchord_desktop_DesktopWindowsFrame_nativeCaptionMetrics(JNIEnv* env, jclass, jlong handle) {
    HWND window = from_java(handle);
    const CaptionMetrics metrics = own_window(window) ? caption_metrics(window) : CaptionMetrics{0, 0};
    const jint values[] = {metrics.height, metrics.right_inset};
    jintArray result = env->NewIntArray(2);
    if (result != nullptr) env->SetIntArrayRegion(result, 0, 2, values);
    return result;
}

JNIEXPORT jboolean JNICALL
Java_com_music_bitchord_desktop_DesktopWindowsFrame_nativeCaptionCommand(JNIEnv*, jclass, jlong handle, jint action) {
    HWND window = from_java(handle);
    if (!own_window(window) || caption_state(window) == nullptr) return JNI_FALSE;
    const UINT command = action == 0 ? SC_MINIMIZE : (IsZoomed(window) ? SC_RESTORE : SC_MAXIMIZE);
    return PostMessageW(window, WM_SYSCOMMAND, command, 0) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_music_bitchord_desktop_DesktopWindowsFrame_nativeCaptionSetBackdrop(JNIEnv*, jclass, jlong handle, jint kind) {
    HWND window = from_java(handle);
    if (!own_window(window) || caption_state(window) == nullptr) return JNI_FALSE;
    DWORD_PTR result = 0;
    // Execute mutation on the owning HWND thread, with no borrowed pointer or unbounded UI wait.
    return SendMessageTimeoutW(window, SET_BACKDROP, kind, 0, SMTO_ABORTIFHUNG | SMTO_BLOCK, 1000, &result) && result
        ? JNI_TRUE : JNI_FALSE;
}
}
