use bitchord_core::AppState;

fn main() {
    match std::env::args().nth(1).as_deref() {
        Some("--version") => println!("BitChord Native foundation {}", env!("CARGO_PKG_VERSION")),
        Some("--help") => {
            println!("BitChord Native foundation\nUsage: bitchord-app [--help|--version]");
        }
        Some(other) => {
            eprintln!("Unknown argument: {other}");
            std::process::exit(2);
        }
        None => {
            tracing_subscriber::fmt()
                .with_env_filter(
                    tracing_subscriber::EnvFilter::try_from_default_env()
                        .unwrap_or_else(|_| tracing_subscriber::EnvFilter::new("info")),
                )
                .with_target(false)
                .init();
            if let Err(error) = bitchord_ui::run(AppState::default()) {
                tracing::error!(%error, "unable to open native window");
                std::process::exit(1);
            }
        }
    }
}
