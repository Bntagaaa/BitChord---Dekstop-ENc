# Feature parity ledger

Source: Kotlin desktop `origin/main` at `f6c4b70c`. Statuses describe the Rust branch only. `Foundation` means an interactive shell exists, **not** that service behavior or appearance is equivalent.

| Feature | Kotlin source | Rust status / next parity test |
| --- | --- | --- |
| Native window | `Main.kt`, `DesktopWindowMode.kt` | Foundation: Slint window; verify native caption, minimum size, resizing, DPI on Windows/Linux |
| Sidebar navigation | `DesktopApp.kt::DesktopSidebar`, `DesktopDestination` | Foundation: typed route selection; screenshot and keyboard comparison pending |
| Dark/light/follow system | `DesktopTheme.kt`, `DesktopSystemTheme.kt` | Foundation: palette and manual toggle; system-follow and persistence pending |
| UI scale | `DesktopUiScaleSettings.kt`, `Main.kt` | Missing; compare 80/90/100/110/125% |
| Home/Explore feeds | `DesktopSearchClient.kt`, `sharedUi/.../HomeScreen.kt`, `ExploreScreen.kt` | Missing; network, artwork, shelf, scroll parity pending |
| Full Search | `DesktopSearchSession.kt`, `sharedUi/.../SearchScreen.kt` | Missing; requests, suggestions, browse, focus, click-to-play pending |
| Quick Search / shortcuts | `DesktopQuickSearch.kt`, `DesktopShortcuts.kt` | Missing; Ctrl+K, focus, shortcut ownership pending |
| Library/history/playlists | `DesktopSearchClient.kt`, `DesktopPersistence.kt`, `sharedUi/.../LibraryScreen.kt` | Missing; data and account parity pending |
| Downloads/local music | `DesktopDownloadManager.kt`, `DesktopLocalMusic.kt` | Missing; migration and file checks pending |
| Audio playback/queue/seek/volume | `DesktopAudioEngine.kt`, `DesktopAudioDecoder.kt`, `DesktopAudioSink.kt`, `DesktopQueue.kt` | Missing; format/device/transition/recovery tests pending |
| Stream resolution/YouTube Music | `shared/.../StreamResolver.kt`, `DesktopSourceRegistry.kt`, `DesktopYouTubeAuth.kt` | Missing; service/anonymous/auth tests pending |
| Spotify library/auth | `DesktopSpotifyLibrary.kt`, `DesktopSpotifyLibraryAuth.kt` | Missing; login/session/library/playback behavior pending |
| Lyrics/translation | `DesktopLyricsClient.kt`, `sharedUi/.../PlayerLyrics.kt`, `shared/.../lyrics/` | Missing; timing/source/visual parity pending |
| Artwork/Canvas/blur/animation | `DesktopArtwork.kt`, `DesktopCanvasVideo.kt`, `sharedUi/.../ArtworkMeshBackdrop.kt` | Missing; image/video/GPU comparison pending |
| Automix/equalizer/crossfade | `DesktopTrackAnalyzer.kt`, `DesktopEqualizer.kt`, `DesktopAudioEngine.kt` | Missing; audio and native analysis parity pending |
| Window tray/media keys/MPRIS/SMTC | `DesktopAwtTray.kt`, `DesktopMpris.kt`, `DesktopWindowsMedia.kt` | Missing; OS test matrix pending |
| Discord/scrobbling/listen together | `DesktopDiscordRpc.kt`, `DesktopScrobbling.kt`, `DesktopListenTogether.kt` | Missing; integration tests pending |
| Account/settings/user data | `DesktopAccounts.kt`, `DesktopPersistence.kt`, `DesktopSecretStore.kt` | Missing; read-only compatibility audit and migration tests pending |
| Windows/Linux installers | `desktopApp/build.gradle.kts`, `.github/workflows/` | Missing; separate Rust packaging workflow pending |

The Kotlin build is the fallback while this ledger contains missing core features. A completed checkbox requires a behavior test and a same-size, same-scale visual comparison where the feature has UI.
