# Draft PR #31 status

## Completed in this increment

- Native Settings appearance controls for System, Light, and Dark theme choices.
- Native settings file with schema version, guarded loading, and atomic replacement. Kotlin preferences are left untouched.
- UI scale selection matching Kotlin's five choices and normalization; applied at next launch.
- Startup OS theme sampling (Windows application-theme registry value; Linux GNOME/GTK fallback).
- Headless tests for settings round trip, invalid schema, scale normalization, and UI callbacks.
- Local Music discovery and virtualized list from the default desktop folders, plus the first native local file playback path (MP3, FLAC, WAV, Ogg Vorbis). A generated WAV was decoded in an automated test without requiring speakers.

## Partial or missing

The native application is **not a replacement for Kotlin BitChord**. Local playback is limited to four formats and has not been heard on a real output device. AAC/M4A/WebM, online service integration, accounts, search, general library, lyrics, downloads, media controls, visual effects, and most pages are unported. Theme changes in the OS are not reflected until the next launch. The Kotlin settings, accounts, and other user data are not migrated. Full details are in [FEATURE_PARITY.md](FEATURE_PARITY.md).

## Validation and blockers

- Linux: `cargo fmt --all --check`, `cargo clippy --workspace --all-targets --locked -- -D warnings`, `cargo test --workspace --locked` (11 tests), and `cargo build --release --locked -p bitchord-app` passed for this increment.
- Windows: the branch's Windows portable workflow has not been run for this increment at the time of writing. Windows GUI and settings persistence have not been tested interactively.
- No authenticated service, playback, performance, or screenshot comparison was run in this environment.
- GitHub API access for editing Draft PR #31's description returned `Forbidden`; this tracked document supplies the review status until that access is available.
