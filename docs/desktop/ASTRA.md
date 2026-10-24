# ASTRA desktop visual implementation

Reference: the eight-page ASTRA.pdf supplied on 2026-10-24. Branch `visual-astra1.8.3` was created from `main` at `7e0af6f4`.

- The bottom player dock stays visible before playback. It shows the BitChord logo and disables playback buttons until a track is selected. Lyrics and Up Next show an empty message when opened without a track.
- Home and the other destinations keep the side panel in its own animated column. The feed narrows when Lyrics or Queue opens, preserving shelf controls and avoiding the destination-dependent layout switch. Haze blurs whatever the page backdrop draws behind the column; with the resized Home feed, that area is generally the page background. The panel material stays stable across navigation, and Reduce dynamic blur remains effective.
- Song context menus, app dropdowns, and the account selector share a dark glass treatment. Their menu entries and callbacks remain as before.
- Desktop lyrics provider selection uses a compact dark menu at the lyrics footer. It keeps the existing provider status and selection behavior; the Android drawer stays in place.
- The expanded player restores its original cover width, makes the lyrics viewport shorter, starts a new track at the top, then brings later active lines toward the center. Minimize and fullscreen controls remain; the transport row stays in place.
- The desktop provider menu uses text rows without icons or row pills and is centered over the footer provider label. The song context menu now renders inside the main window, where Haze can sample the actual page behind the menu. The artwork-based approximation was removed.
- Appearance includes temporary opacity and blur-radius sliders for the Lyrics/Queue glass. Expanded lyrics keep the first lines at the top and begin following toward the center only as the song progresses.

Validation: `:desktopApp:compileKotlin :desktopApp:test` passed with writable Java preferences and cache paths. `:sharedUi:jvmTest` could not resolve `kotlinx-datetime:0.6.0` because Maven/Google/JitPack DNS requests failed in this environment. GUI inspection on Windows and Linux remains pending, particularly the Home backdrop, provider menu position, context menu appearance, DPI and lyric scrolling.
