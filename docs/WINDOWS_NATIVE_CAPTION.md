# Windows system caption

BitChord creates its main Compose `Window` with `undecorated = false` and
`transparent = false`. The AWT peer is opaque and retains the standard Windows
non-client frame. Windows, rather than Compose or a subclassed window procedure,
owns the title, icon, caption buttons, move/resize hit tests, system menu and Snap.
The application toolbar starts below that frame. The saved `window_title_bar`
preference is ignored, but is not deleted.

`Main.kt` keeps the same `onCloseRequest` policy: Close can hide to the tray, and
tray/MPRIS raise restores the existing window. `DesktopWindowMode` still uses
maximize instead of fullscreen on Windows. No window recreation is tied to
material, move, resize or placement changes.

## Optional title bar material

`DesktopWindowsCaptionAppearance` requests dark caption styling and the selected
DWM system backdrop on the displayable HWND. DWM type 2 requests Mica; type 3
requests desktop Acrylic; Off requests no material. An unsupported API, missing
JNA bridge, or failed DWM call leaves the decorated window usable. A successful
attribute call does not guarantee a visible effect under every Windows policy.

The Compose client remains opaque and uses BitChord's normal backgrounds. Settings
labels this option **Native title bar material**. The retired app-background
preference is ignored without deleting its stored value. These settings do not
promise Mica or Acrylic throughout the application content.

`bitchord_window.dll` remains built and packaged for `DesktopWindowsAudio` WASAPI
JNI. The old caption hook and its JNI exports are removed; audio entrypoints stay.

## Validation still required on Windows

Use JDK 21 and the project's Compose version. Run the shared/desktop tests and
`packageMsi`, `packageExe`, and `createDistributable` on Windows. On the actual
BitChord executable, check
drag and drag-to-restore, double-click maximize, native hover/buttons/system menu,
all resize edges, Snap zones, taskbar/tray, flyouts, old preferences, 100–200% and
mixed-monitor DPI. Verify material visually and also test a failed styling helper.
Static review and Linux compilation cannot establish these runtime outcomes.

The 900 logical-pixel minimum width may prevent narrow Snap zones from fitting;
separate that limitation from whether the native Snap menu appears. Change the
minimum only after checking compact-mode controls at the proposed size.
