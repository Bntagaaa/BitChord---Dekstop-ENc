# ASTRA desktop visual implementation

Reference: the eight-page ASTRA.pdf supplied on 2026-10-24. Branch `visual-astra1.8.3` was created from `main` at `7e0af6f4`.

- The bottom player dock stays visible before playback. It shows the BitChord logo and disables playback buttons until a track is selected. Lyrics and Up Next show an empty message when opened without a track.
- On Home, the feed extends behind the side panel so Haze can blur its actual pixels. The page remains anchored on the left. Other destinations retain their side-by-side layout and solid panel fill. Remove Blur remains effective.
- Song context menus, app dropdowns, and the account selector share a dark glass treatment. Their menu entries and callbacks remain as before.
- Desktop lyrics provider selection uses a compact dark menu at the lyrics footer. It keeps the existing provider status and selection behavior; the Android drawer stays in place.
- The expanded player restores its original cover width, makes the lyrics viewport shorter, starts a new track at the top, then brings later active lines toward the center. Minimize and fullscreen controls remain; the transport row stays in place.
- The desktop provider menu now uses text rows without icons or row pills and is centered over the footer provider label. The song context menu uses a blurred version of the selected track artwork behind a dark tint because Compose Popup cannot sample the window behind it.

Validation: `:desktopApp:compileKotlin :desktopApp:test` passed with writable Java preferences and cache paths. `:sharedUi:jvmTest` could not resolve `kotlinx-datetime:0.6.0` because Maven/Google/JitPack DNS requests failed in this environment. GUI inspection on Windows and Linux remains pending, particularly the Home backdrop, provider menu position, context menu appearance, DPI and lyric scrolling.
