# Feature parity ledger

Source: Kotlin desktop `origin/main` at `f6c4b70c`. Statuses describe the Rust branch only. `Foundation` means an interactive shell exists, **not** that service behavior or appearance is equivalent.

| Feature | Kotlin source | Rust status / next parity test |
| --- | --- | --- |
| Native window | `Main.kt`, `DesktopWindowMode.kt` | Foundation: Slint window; verify native caption, minimum size, resizing, DPI on Windows/Linux |
| Sidebar navigation | `DesktopApp.kt::DesktopSidebar`, `DesktopDestination` | Foundation: typed route selection; screenshot and keyboard comparison pending |
| Dark/light/follow system | `DesktopTheme.kt`, `DesktopSystemTheme.kt` | Partial: System/Light/Dark selection and native persistence; OS theme sampled at launch only, no live follow yet; visual parity pending |
| UI scale | `DesktopUiScaleSettings.kt`, `Main.kt` | Partial: same 80/90/100/110/125% choices and normalization, persisted; scale applies on next launch, unlike Kotlin's live update; compare layouts at all scales |
| Home/Explore feeds | `DesktopSearchClient.kt`, `sharedUi/.../HomeScreen.kt`, `ExploreScreen.kt` | Missing; network, artwork, shelf, scroll parity pending |
| Full Search | `DesktopSearchSession.kt`, `sharedUi/.../SearchScreen.kt` | Missing; requests, suggestions, browse, focus, click-to-play pending |
| Quick Search / shortcuts | `DesktopQuickSearch.kt`, `DesktopShortcuts.kt` | Missing; Ctrl+K, focus, shortcut ownership pending |
| Library/history/playlists | `DesktopSearchClient.kt`, `DesktopPersistence.kt`, `sharedUi/.../LibraryScreen.kt` | Missing; data and account parity pending |
| Downloads/local music | `DesktopDownloadManager.kt`, `DesktopLocalMusic.kt` | Partial: native background scan of default Music/Downloads roots, Kotlin-compatible depth, extensions, filename labels, sorting, optional filter in core, and a virtualized list. Folder selection, watcher, metadata enrichment, and downloads remain missing |
| Audio playback/queue/seek/volume | `DesktopAudioEngine.kt`, `DesktopAudioDecoder.kt`, `DesktopAudioSink.kt`, `DesktopQueue.kt` | Partial: native local MP3/FLAC/WAV/Ogg Vorbis decode and output via miniaudio, click-to-play, pause/resume, previous/next, end advance, volume/seek API. AAC/M4A/WebM, online streams, seek/volume UI, crossfade, decode-ahead, output-device selection and playback error recovery remain missing. WAV decode tested without device; speaker output unverified |
| Stream resolution/YouTube Music | `shared/.../StreamResolver.kt`, `DesktopSourceRegistry.kt`, `DesktopYouTubeAuth.kt` | Missing; service/anonymous/auth tests pending |
| Spotify library/auth | `DesktopSpotifyLibrary.kt`, `DesktopSpotifyLibraryAuth.kt` | Missing; login/session/library/playback behavior pending |
| Lyrics/translation | `DesktopLyricsClient.kt`, `sharedUi/.../PlayerLyrics.kt`, `shared/.../lyrics/` | Missing; timing/source/visual parity pending |
| Artwork/Canvas/blur/animation | `DesktopArtwork.kt`, `DesktopCanvasVideo.kt`, `sharedUi/.../ArtworkMeshBackdrop.kt` | Missing; image/video/GPU comparison pending |
| Automix/equalizer/crossfade | `DesktopTrackAnalyzer.kt`, `DesktopEqualizer.kt`, `DesktopAudioEngine.kt` | Missing; audio and native analysis parity pending |
| Window tray/media keys/MPRIS/SMTC | `DesktopAwtTray.kt`, `DesktopMpris.kt`, `DesktopWindowsMedia.kt` | Missing; OS test matrix pending |
| Discord/scrobbling/listen together | `DesktopDiscordRpc.kt`, `DesktopScrobbling.kt`, `DesktopListenTogether.kt` | Missing; integration tests pending |
| Account/settings/user data | `DesktopAccounts.kt`, `DesktopPersistence.kt`, `DesktopSecretStore.kt` | Partial: isolated versioned native appearance settings with atomic writes; Kotlin Java Preferences and all accounts, secrets, downloads, playlists, history remain untouched and are not imported; migration tests pending |
| Windows/Linux installers | `desktopApp/build.gradle.kts`, `.github/workflows/` | Missing; separate Rust packaging workflow pending |

The Kotlin build is the fallback while this ledger contains missing core features. A completed checkbox requires a behavior test and a same-size, same-scale visual comparison where the feature has UI.
