# Windows: always-on Windows caption

Windows has one dedicated title strip above the application. The title-bar preference is retired
and an existing `window_title_bar=false` value is ignored.

The first DWM-only attempt intentionally kept the whole top edge as client area while asking DWM
to provide its caption buttons. On the Compose/AWT undecorated peer used by BitChord, that left a
correct-looking strip but no painted/clickable buttons and could consume caption hit-testing before
our own `HTCAPTION` result. The current implementation avoids that ambiguous ownership.

## Architecture

- `Main.kt` hosts the title strip outside both the application background and
  `DesktopFlyoutHost`, so Quick Search, flyouts and Now Playing never cover its controls.
- `DesktopWindowChrome.kt` draws Windows-style Minimize, Maximize/Restore and Close controls at
  the right. Minimize/maximize are backed by Win32 `WM_SYSCOMMAND`; Close keeps BitChord's
  existing close-to-tray path.
- `native_caption.cpp` reserves that rightmost 3 × 46dp area as `HTCLIENT`, so Compose receives
  hover/click there. Every other pixel in the strip is `HTCAPTION`, so Windows itself handles
  dragging and double-click maximize/restore. Resize edges/corners remain native hit tests.
- The bridge no longer calls `DwmDefWindowProc` before BitChord's hit-test. That was the path
  suppressing the custom caption result in the failed build.
- The title strip is transparent whenever Mica/Acrylic is active. The bridge does not force
  `DWMWA_CAPTION_COLOR`, because a solid caption colour would cover the material.
- Caption height and the 138dp control area are scaled by the HWND DPI and converted back to
  Compose logical space through AWT's current monitor transform.
- Missing/outdated native DLLs still fall back to a normal system-decorated window. Caption bridge
  API v2 prevents the previous DLL from being accepted accidentally.
- Linux keeps its normal window-manager decoration and gets no extra application title strip.
- The old Title bar Settings row and macOS traffic-light controls remain disabled.

## Windows validation checklist

Run Build Test Windows on this branch with tests enabled, then verify:

1. The top-right shows `— □ ×`; Close becomes red on hover.
2. All three controls click correctly; maximize changes to Restore.
3. Drag anywhere in the empty title area; double-click it to maximize/restore.
4. Resize every edge and corner.
5. Test Mica, Acrylic and Off. Mica/Acrylic should continue through the title strip.
6. Open every flyout and Now Playing; caption controls must remain reachable.
7. Check 100%, 125%, 150% and 200% scaling and mixed-DPI monitors.
8. Confirm the Title bar option is absent from Appearance.
9. Confirm Linux remains unchanged.
