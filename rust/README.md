# BitChord native Rust foundation

This separate workspace is an early, incomplete Slint implementation of BitChord Desktop Revived. Its native window has working route selection and a dark/light toggle. The screen clearly identifies unported content. It does not play audio, log in, read user data, or replace the Kotlin desktop application.

The UI compiles Slint to Rust and embeds the logo and SF Pro Display files copied from the existing desktop resources. It uses Slint's winit native window with software rendering, with no JVM or browser frontend in this Rust process. This renderer is provisional pending a visual/performance comparison.

The workspace pins Rust 1.99.1. Linux needs the `fontconfig` runtime library for system font discovery; its development headers are not required by this workspace.

From the repository root:

```sh
cd rust
cargo test --workspace --locked
cargo run -p bitchord-app --locked
```

`cargo run -p bitchord-app -- --version` checks the compiled binary without a GUI. Running the window needs a graphical desktop session. Windows and Linux GUI validation, service parity, and user-data migration remain open in [`FEATURE_PARITY.md`](../docs/rust-rewrite/FEATURE_PARITY.md).

The new process logs startup and UI state changes with `tracing` (`RUST_LOG=debug` for route/theme events). It never reads or writes the Kotlin user data. This milestone copies the current desktop logo and three SF Pro Display weights from the Kotlin resources; they are compiled into the native UI.
