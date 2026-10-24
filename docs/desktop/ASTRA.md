# ASTRA desktop visual implementation

Reference: the eight-page ASTRA.pdf supplied on 2026-10-24. Branch `visual-astra1.8.3` was created from `main` at `7e0af6f4`.

- The bottom player dock stays visible before playback. It shows the BitChord logo and disables playback buttons until a track is selected. Lyrics and Up Next show an empty message when opened without a track.
- The side panel keeps its existing width and pushes the page as before. Only Home uses the translucent glass surface; other destinations retain their solid fill. The existing Remove Blur preference remains effective.
- Song context menus, app dropdowns, and the account selector share a dark glass treatment. Their menu entries and callbacks remain as before.
- Desktop lyrics provider selection uses a compact dark menu at the lyrics footer. It keeps the existing provider status and selection behavior; the Android drawer stays in place.
- The expanded player gives lyrics more horizontal room, centers the active line when following playback, and has minimize and fullscreen controls. Its existing transport row stays in place.

Validation: `:desktopApp:compileKotlin :desktopApp:test` passed with writable Java preferences and cache paths. `:sharedUi:jvmTest` could not resolve `kotlinx-datetime:0.6.0` because Maven/Google/JitPack DNS requests failed in this environment. GUI inspection on Windows and Linux remains pending, particularly the blur material, provider menu position, DPI and lyric scrolling.
