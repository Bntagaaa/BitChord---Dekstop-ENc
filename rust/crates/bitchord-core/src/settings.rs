//! Native settings are versioned and stored separately from Java Preferences.
//! No Kotlin preference or account file is modified by this module.

use crate::ThemeMode;
use serde::{Deserialize, Serialize};
use std::fs;
use std::io;
use std::path::{Path, PathBuf};

const VERSION: u32 = 1;
pub const UI_SCALE_OPTIONS: [u16; 5] = [80, 90, 100, 110, 125];

pub fn normalize_ui_scale(value: u16) -> u16 {
    let bounded = value.clamp(80, 125);
    *UI_SCALE_OPTIONS
        .iter()
        .min_by_key(|option| option.abs_diff(bounded))
        .expect("nonempty scale choices")
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct NativeSettings {
    pub theme_mode: ThemeMode,
    pub ui_scale_percent: u16,
}

impl Default for NativeSettings {
    fn default() -> Self {
        Self {
            theme_mode: ThemeMode::System,
            ui_scale_percent: 100,
        }
    }
}

#[derive(Serialize, Deserialize)]
#[serde(deny_unknown_fields)]
struct StoredSettings {
    version: u32,
    theme_mode: String,
    ui_scale_percent: u16,
}

pub struct SettingsStore {
    path: PathBuf,
}

impl SettingsStore {
    pub fn for_current_user() -> io::Result<Self> {
        let project = directories::ProjectDirs::from("com", "music", "BitChord Native")
            .ok_or_else(|| {
                io::Error::new(io::ErrorKind::NotFound, "user config directory unavailable")
            })?;
        Ok(Self::at(project.config_dir().join("settings-v1.json")))
    }

    pub fn at(path: PathBuf) -> Self {
        Self { path }
    }

    pub fn load(&self) -> io::Result<NativeSettings> {
        let data = match fs::read(&self.path) {
            Ok(data) => data,
            Err(error) if error.kind() == io::ErrorKind::NotFound => {
                return Ok(NativeSettings::default());
            }
            Err(error) => return Err(error),
        };
        let stored: StoredSettings = serde_json::from_slice(&data).map_err(invalid_data)?;
        if stored.version != VERSION {
            return Err(io::Error::new(
                io::ErrorKind::InvalidData,
                format!("unsupported settings version {}", stored.version),
            ));
        }
        let theme_mode = ThemeMode::from_id(&stored.theme_mode)
            .ok_or_else(|| io::Error::new(io::ErrorKind::InvalidData, "unknown theme mode"))?;
        Ok(NativeSettings {
            theme_mode,
            ui_scale_percent: normalize_ui_scale(stored.ui_scale_percent),
        })
    }

    pub fn save(&self, settings: &NativeSettings) -> io::Result<()> {
        let parent = self.path.parent().ok_or_else(|| {
            io::Error::new(io::ErrorKind::InvalidInput, "settings path has no parent")
        })?;
        fs::create_dir_all(parent)?;
        let value = StoredSettings {
            version: VERSION,
            theme_mode: settings.theme_mode.id().to_owned(),
            ui_scale_percent: normalize_ui_scale(settings.ui_scale_percent),
        };
        let bytes = serde_json::to_vec_pretty(&value).map_err(invalid_data)?;
        atomicwrites::AtomicFile::new(&self.path, atomicwrites::AllowOverwrite)
            .write(|file| {
                use std::io::Write;
                file.write_all(&bytes)?;
                file.sync_all()
            })
            .map_err(|error| io::Error::other(error.to_string()))
    }

    pub fn path(&self) -> &Path {
        &self.path
    }
}

fn invalid_data(error: impl std::error::Error + Send + Sync + 'static) -> io::Error {
    io::Error::new(io::ErrorKind::InvalidData, error)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn scale_matches_kotlin_tie_breaking_and_bounds() {
        assert_eq!(normalize_ui_scale(0), 80);
        assert_eq!(normalize_ui_scale(85), 80);
        assert_eq!(normalize_ui_scale(86), 90);
        assert_eq!(normalize_ui_scale(117), 110);
        assert_eq!(normalize_ui_scale(118), 125);
        assert_eq!(normalize_ui_scale(200), 125);
    }

    #[test]
    fn settings_round_trip_and_preserve_kotlin_data_boundary() {
        let dir = std::env::temp_dir().join(format!(
            "bitchord-native-settings-test-{}-{:?}",
            std::process::id(),
            std::thread::current().id()
        ));
        let path = dir.join("settings-v1.json");
        let store = SettingsStore::at(path.clone());
        assert_eq!(store.load().unwrap(), NativeSettings::default());
        let settings = NativeSettings {
            theme_mode: ThemeMode::Dark,
            ui_scale_percent: 125,
        };
        store.save(&settings).unwrap();
        assert_eq!(store.load().unwrap(), settings);
        fs::write(
            &path,
            br#"{"version":2,"theme_mode":"DARK","ui_scale_percent":100}"#,
        )
        .unwrap();
        assert_eq!(store.load().unwrap_err().kind(), io::ErrorKind::InvalidData);
        let _ = fs::remove_dir_all(dir);
    }
}
