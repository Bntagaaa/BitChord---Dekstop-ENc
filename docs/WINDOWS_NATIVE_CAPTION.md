# Windows: always-on native caption

Windows now has a dedicated title strip above the application. Minimize, maximize/restore and
close are rendered and hit-tested by Windows/DWM, not Compose traffic lights. The title-bar
preference is no longer offered. An existing `window_title_bar=false` is ignored.

## Architecture

- `Main.kt` hosts the transparent caption strip outside both the application background and
  `DesktopFlyoutHost`. Audio output, account, shortcuts, Quick Search and Now Playing overlays
  cannot cover the native caption. Toolbar contents do not share its horizontal space.
- `DesktopPlatform.drawsOwnWindowFrame` is the legacy Compose-caption capability and is now false.
  This disables the existing Settings row and both inline caption slots. Small compatibility
  functions in `DesktopWindowChrome.kt` keep the application host unchanged; they neither paint
  controls nor read/write a preference. The actual strip is `DesktopNativeTitleBar` in Main.
- `native_caption.cpp` is compiled into the existing `bitchord_window` DLL. The old functions in
  `window_jni.cpp` are not installed by the new Kotlin bridge. That file is deliberately untouched
  because it also contains the live WASAPI implementation.
- The new procedure gives DWM the first opportunity to handle non-client messages, including
  native button hit testing and mouse leave. Its dedicated empty strip returns `HTCAPTION`.
  Unhandled commands still reach AWT's original procedure so close-to-tray and window-state
  notifications continue through their existing application paths.
- Caption bounds come from DWM, with DPI-scaled fallback metrics for hidden/minimized windows.
  Native pixel measurements are converted using AWT's current per-monitor transform. No player
  toolbar width is reserved for caption buttons.
- The top frame stays extended when Mica/Acrylic are off. Material settings remain available
  when the new bridge is installed on Windows 11; unsupported material requests remain Off.
- Probe the native API before constructing the peer. Missing/outdated DLLs use a normal,
  opaque, system-decorated AWT window. If installation fails after creation, recreate only the
  startup window before composing the playback UI. Do not leave an unusable frameless window.
- Linux stays system-decorated, without an extra application title strip. Windows' existing
  maximize-instead-of-Skiko-fullscreen workaround remains explicitly Windows-only.

## Validation status

Nine pure Kotlin caption-metrics tests passed outside Gradle. Eight static integration guards
passed. Eight Kotlin files parsed without syntax errors. These checks do not prove native
Windows rendering, Snap Layouts, JNI linkage, or Compose compilation.

Native Windows compilation, Gradle tests, installer packaging and the following interactive
checks still need to run on a Windows machine. Do not treat source inspection as a UI pass.

## Build and interactive checklist

Run the existing Build Test Windows workflow on this feature branch, with tests enabled.
Confirm that the log reports `native Windows caption installed (always on)`; otherwise the
safe system-decoration fallback is being used.

1. Start with an old profile containing `window_title_bar=false`, then a fresh profile. Both
   must show exactly one title bar, real Windows controls on the right and no macOS dots.
2. Appearance settings and its search must not offer a Title bar toggle. Sidebars must not
   retain the empty traffic-light header. Test wide and compact layouts.
3. Test drag, double-click maximize/restore, resize edges/corners, right-click system menu,
   Alt+Space, Alt+F4, taskbar minimize/restore and Windows 11 maximize-button Snap Layout.
4. Close with close-to-tray on and off. Restore from tray. Do not restart or duplicate playback.
5. Open every flyout and Now Playing/lyrics. Caption controls must stay visible and clickable;
   Escape and all previously fixed in-app shortcuts must still work after dismissal.
6. Test Mica, Acrylic and Off; maximize/restore in each. No missing controls or white flash.
7. Test 100%, 125%, 150% and 200% DPI, mixed-DPI monitors, and a monitor left of the primary.
   Caption geometry must not overlap the application, clip buttons or drift after moving.
8. Verify a Windows build with the bridge missing takes the system-titlebar fallback. Verify
   Linux has no extra strip and its decoration, fullscreen, MPRIS and audio behavior are unchanged.

References: Microsoft's Custom Window Frame Using DWM documentation, DwmDefWindowProc contract,
and DWMWA_CAPTION_BUTTON_BOUNDS documentation.
