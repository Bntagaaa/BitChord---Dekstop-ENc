# BitChord native Rust foundation

This separate workspace is an early, incomplete Slint implementation of BitChord Desktop Revived. Its native window has working route selection and a small Appearance settings screen with System/Light/Dark and the Kotlin app's UI scale choices. Other screens clearly identify unported content. It does not play audio, log in, read Kotlin user data, or replace the Kotlin desktop application.

The UI compiles Slint to Rust and embeds the logo and SF Pro Display files copied from the existing desktop resources. It uses Slint's winit native window with software rendering, with no JVM or browser frontend in this Rust process. This renderer is provisional pending a visual/performance comparison.

The workspace pins Rust 1.99.1. Linux needs the `fontconfig` runtime library for system font discovery; its development headers are not required by this workspace.

From the repository root:

```sh
cd rust
cargo test --workspace --locked
cargo run -p bitchord-app --locked
```

`cargo run -p bitchord-app -- --version` checks the compiled binary without a GUI. Running the window needs a graphical desktop session. Windows and Linux GUI validation, service parity, and user-data migration remain open in [`FEATURE_PARITY.md`](../docs/rust-rewrite/FEATURE_PARITY.md).

The new process logs startup and UI state changes with `tracing` (`RUST_LOG=debug` for route/theme events). Native appearance settings are saved as versioned JSON in the current user's config directory with atomic replacement; unsupported or damaged settings stop startup without overwriting the file. The Kotlin Java Preferences and account data are never read or written. The OS theme is sampled at startup. UI scale is applied on the next launch through Slint's scale factor. These two behaviors still differ from the Kotlin app's live updates. This milestone copies the current desktop logo and three SF Pro Display weights from the Kotlin resources; they are compiled into the native UI.
