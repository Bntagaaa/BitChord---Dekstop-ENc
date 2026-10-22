# Native Rust rewrite: architecture audit

Audited from `origin/main` at `f6c4b70cb2292be86c23aba850d42dbf7435037a` on 2026-10-22. The Kotlin desktop app remains the behavioral reference. This document describes the current source and the initial Rust foundation; it does not claim feature parity.

## Existing application

The Gradle workspace has `:desktopApp`, `:shared`, and `:sharedUi` (`settings.gradle.kts`). `desktopApp/src/main/kotlin/com/music/bitchord/desktop/Main.kt` initializes the shared data/UI hosts, PoToken provider, Spotify authentication bridge, and Compose `Window`. The window starts decorated and opaque at 1220 × 780 dp with a 900 × 600 dp minimum. `DesktopApp.kt::BitChordDesktopApp` owns much of the desktop navigation, page state, playback dispatch, overlays, and layout. The 220 dp sidebar is assembled in `DesktopApp.kt::DesktopSidebar`.

`sharedUi` supplies Compose Home, Explore, Search, Library, Now Playing, player controls, lyrics, icons, and artwork effects. `shared` supplies the `Song`/`SearchResult` models, queue and transition logic, YouTube Music repository and stream resolver, Spotify library logic, lyrics providers, and analysis helpers. Android uses some of these modules; the new Rust code must reproduce their desktop behavior without changing them.

| Capability | Authoritative current source | Target boundary |
| --- | --- | --- |
| Window, state, theme, scaling | `desktopApp/.../Main.kt`, `DesktopWindowMode.kt`, `DesktopWindowVisibility.kt`, `DesktopTheme.kt`, `DesktopUiScaleSettings.kt` | `bitchord-app`, `bitchord-ui`, future platform/storage crates |
| Desktop routes, sidebar, overlays, focus | `DesktopApp.kt`, `DesktopFlyoutHost.kt`, `DesktopShortcuts.kt`, `DesktopQuickSearch.kt` | `bitchord-core` state and `bitchord-ui` input/rendering |
| Home/Search/Explore/Library | `DesktopSearchClient.kt`, `DesktopSearchSession.kt`, `sharedUi/.../screens/{Home,Search,Explore,Library}Screen.kt` | Future network/core services and real UI pages |
| Playback/queue/sources | `DesktopAudioEngine.kt`, `DesktopAudioDecoder.kt`, `DesktopDecodeAhead.kt`, `DesktopAudioSink.kt`, `DesktopSourceRegistry.kt`, `shared/.../playback/QueueTimeline.kt`, `shared/.../data/innertube/StreamResolver.kt` | Future player/network crates; FFmpeg/output parity must be tested |
| Main player, lyrics, Canvas | `sharedUi/.../player/{NowPlayingScreen,PlayerDock,PlayerLyrics,ArtworkMeshBackdrop}.kt`, `DesktopLyricsClient.kt`, `DesktopCanvasVideo.kt` | Future UI, lyrics, video/graphics services |
| YouTube sign-in/account | `DesktopWebSignIn.kt`, `DesktopBrowserSignIn.kt`, `DesktopYouTubeAuth.kt`, `DesktopYouTubeSession.kt`, `DesktopAccounts.kt` | Future auth/platform services; no credentials in UI state |
| Spotify library/auth | `DesktopSpotifyLibrary.kt`, `DesktopSpotifyLibraryAuth.kt`, `shared/.../data/spotify/SpotifyLibrary.kt` | Future auth/network services; current web-player token flow needs separate compatibility design |
| Downloads/local music/playlists/history | `DesktopDownloadManager.kt`, `DesktopLocalMusic.kt`, `DesktopPersistence.kt`, `DesktopPlaylistUi.kt`, `DesktopSharedPages.kt::DesktopSearchHistory` | Future storage/network/player services and UI |
| Automix/effects | `DesktopTrackAnalyzer.kt`, `DesktopAnalysisRuntime.kt`, `shared/.../playback/smart/BeatTracker.kt`, native analyzer C++ | Future analysis/player service; model licensing and native lifetime need review |
| Platform integrations | `DesktopMpris.kt`, `DesktopWindowsMedia.kt`, `DesktopAwtTray.kt`, `DesktopDiscordRpc.kt`, `DesktopListenTogether.kt` | OS-specific platform/integration crates |

The Gradle desktop module currently uses Compose, JavaFX WebView, JavaCPP/FFmpeg, ONNX Runtime, Ktor, SQLite, JNA, and D-Bus dependencies (`desktopApp/build.gradle.kts`). `Main.kt` also uses JavaFX WebView for authentication and PoTokens. Thus removing the JVM is more than replacing the window toolkit.

## GUI decision

Use Slint as the native Rust UI foundation, with a native winit window and software renderer for an initial portable build. Slint supports declarative layout, custom drawing, animation, input, font/image assets, and Rust state callbacks without a Java or browser UI runtime. This repository's GPL-3.0 license is compatible with Slint's GPL-3.0 option. The baseline shell will preserve observed desktop proportions and dark/light palette tokens (`DesktopTheme.kt`). Software rendering is a build choice, not a performance claim; a GPU backend must be benchmarked before selection for full artwork/lyrics rendering.

There is no verified one-to-one Slint replacement for the current Haze backdrop blur, Skia mesh artwork, animated synchronized lyrics, or video Canvas. These need renderer experiments using native GPU composition/custom drawing, screenshot comparison at the same size and scale, and integrated-GPU profiling. Do not replace them with flat rectangles and call them equivalent. A native login webview may be necessary for the existing service flows; it is never the main UI.

## Initial Rust layout

`rust/crates/bitchord-core` owns typed routes and theme state without UI or platform dependencies. `bitchord-ui` owns the compiled Slint window, visual assets, and event binding. `bitchord-app` is the process entry point. Additional crates from the master prompt should be created when their first real feature is ported. Keep service interfaces narrow and async work off the UI event loop. Isolate required FFI and credential handling in platform/auth crates; no unsafe code is needed in the foundation.

The foundation has real navigation and theme switching. Content pages, playback, accounts, and persistence remain explicitly unimplemented. It does not touch Kotlin data and cannot be presented as a replacement build.
