use bitchord_core::{AppState, settings::SettingsStore};
use std::process::{Command, Stdio};
use std::time::{Duration, Instant};

fn command_output(program: &str, args: &[&str]) -> Option<String> {
    let mut child = Command::new(program)
        .args(args)
        .stdout(Stdio::piped())
        .stderr(Stdio::null())
        .spawn()
        .ok()?;
    let deadline = Instant::now() + Duration::from_millis(1200);
    loop {
        if let Some(status) = child.try_wait().ok()? {
            return status.success().then(|| {
                let mut text = String::new();
                use std::io::Read;
                let _ = child.stdout.take().unwrap().read_to_string(&mut text);
                text
            });
        }
        if Instant::now() >= deadline {
            let _ = child.kill();
            let _ = child.wait();
            return None;
        }
        std::thread::sleep(Duration::from_millis(15));
    }
}

fn system_dark() -> bool {
    #[cfg(target_os = "windows")]
    {
        let output = command_output(
            "reg",
            &[
                "query",
                r"HKCU\Software\Microsoft\Windows\CurrentVersion\Themes\Personalize",
                "/v",
                "AppsUseLightTheme",
            ],
        );
        return output.is_some_and(|text| {
            text.lines().any(|line| {
                line.contains("AppsUseLightTheme") && line.split_whitespace().last() == Some("0x0")
            })
        });
    }
    #[cfg(target_os = "linux")]
    {
        if let Some(scheme) = command_output(
            "gsettings",
            &["get", "org.gnome.desktop.interface", "color-scheme"],
        ) {
            if scheme.contains("prefer-dark") {
                return true;
            }
            if scheme.contains("prefer-light") {
                return false;
            }
        }
        if let Some(theme) = command_output(
            "gsettings",
            &["get", "org.gnome.desktop.interface", "gtk-theme"],
        ) && theme.to_lowercase().contains("dark")
        {
            return true;
        }
        std::env::var("GTK_THEME").is_ok_and(|theme| theme.to_lowercase().contains("dark"))
    }
    #[cfg(not(any(target_os = "windows", target_os = "linux")))]
    false
}

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
            let settings_store = match SettingsStore::for_current_user() {
                Ok(store) => store,
                Err(error) => {
                    tracing::error!(%error, "unable to locate native settings directory");
                    std::process::exit(1);
                }
            };
            let settings = match settings_store.load() {
                Ok(settings) => settings,
                Err(error) => {
                    tracing::error!(%error, path = %settings_store.path().display(), "unable to read native settings; existing data was left untouched");
                    std::process::exit(1);
                }
            };
            if std::env::var_os("SLINT_SCALE_FACTOR").is_none() {
                // Startup is single-threaded. Slint reads this before constructing its platform.
                unsafe {
                    std::env::set_var(
                        "SLINT_SCALE_FACTOR",
                        format!("{}", f32::from(settings.ui_scale_percent) / 100.0),
                    );
                }
            }
            let state = AppState::with_settings(&settings, system_dark());
            if let Err(error) = bitchord_ui::run(state, settings_store) {
                tracing::error!(%error, "unable to open native window");
                std::process::exit(1);
            }
        }
    }
}
