//! UI-independent application state for the native desktop.

pub mod local_music;
pub mod settings;

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub enum Page {
    #[default]
    Home,
    Search,
    Library,
    Spotify,
    History,
    Downloads,
    LocalMusic,
    Settings,
}

impl Page {
    pub const ALL: [Self; 8] = [
        Self::Home,
        Self::Search,
        Self::Library,
        Self::Spotify,
        Self::History,
        Self::Downloads,
        Self::LocalMusic,
        Self::Settings,
    ];

    pub const fn id(self) -> &'static str {
        match self {
            Self::Home => "home",
            Self::Search => "search",
            Self::Library => "library",
            Self::Spotify => "spotify",
            Self::History => "history",
            Self::Downloads => "downloads",
            Self::LocalMusic => "local-music",
            Self::Settings => "settings",
        }
    }

    pub const fn title(self) -> &'static str {
        match self {
            Self::Home => "Home",
            Self::Search => "Search",
            Self::Library => "Library",
            Self::Spotify => "Spotify",
            Self::History => "History",
            Self::Downloads => "Downloads",
            Self::LocalMusic => "Local Music",
            Self::Settings => "Settings",
        }
    }

    pub fn from_id(id: &str) -> Option<Self> {
        Self::ALL.into_iter().find(|page| page.id() == id)
    }
}

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub enum Theme {
    #[default]
    Dark,
    Light,
}

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub enum ThemeMode {
    #[default]
    System,
    Light,
    Dark,
}

impl ThemeMode {
    pub const fn id(self) -> &'static str {
        match self {
            Self::System => "SYSTEM",
            Self::Light => "LIGHT",
            Self::Dark => "DARK",
        }
    }

    pub fn from_id(id: &str) -> Option<Self> {
        match id {
            "SYSTEM" => Some(Self::System),
            "LIGHT" => Some(Self::Light),
            "DARK" => Some(Self::Dark),
            _ => None,
        }
    }

    pub const fn resolve(self, system_dark: bool) -> Theme {
        match self {
            Self::System if system_dark => Theme::Dark,
            Self::System | Self::Light => Theme::Light,
            Self::Dark => Theme::Dark,
        }
    }
}

#[derive(Debug, Eq, PartialEq)]
pub struct AppState {
    pub page: Page,
    pub theme: Theme,
    pub theme_mode: ThemeMode,
    pub ui_scale_percent: u16,
    pub system_dark: bool,
}

impl Default for AppState {
    fn default() -> Self {
        Self {
            page: Page::Home,
            theme: Theme::Dark,
            theme_mode: ThemeMode::Dark,
            ui_scale_percent: 100,
            system_dark: false,
        }
    }
}

impl AppState {
    pub fn navigate(&mut self, id: &str) -> bool {
        let Some(next) = Page::from_id(id) else {
            return false;
        };
        self.page = next;
        true
    }

    pub fn toggle_theme(&mut self) {
        self.select_theme_mode(match self.theme {
            Theme::Dark => ThemeMode::Light,
            Theme::Light => ThemeMode::Dark,
        });
    }

    pub fn select_theme_mode(&mut self, mode: ThemeMode) {
        self.theme_mode = mode;
        self.theme = mode.resolve(self.system_dark);
    }

    pub fn update_system_theme(&mut self, dark: bool) {
        self.system_dark = dark;
        self.theme = self.theme_mode.resolve(dark);
    }

    pub fn select_ui_scale(&mut self, value: u16) {
        self.ui_scale_percent = settings::normalize_ui_scale(value);
    }
}

impl AppState {
    pub fn with_settings(settings: &settings::NativeSettings, system_dark: bool) -> Self {
        let mut state = Self {
            ui_scale_percent: settings.ui_scale_percent,
            ..Self::default()
        };
        state.update_system_theme(system_dark);
        state.select_theme_mode(settings.theme_mode);
        state
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn every_route_has_a_distinct_round_trip_id() {
        let mut ids = std::collections::HashSet::new();
        for page in Page::ALL {
            assert!(ids.insert(page.id()));
            assert_eq!(Page::from_id(page.id()), Some(page));
            assert!(!page.title().is_empty());
        }
    }

    #[test]
    fn unknown_route_cannot_discard_current_navigation() {
        let mut state = AppState::default();
        assert!(state.navigate("library"));
        assert!(!state.navigate("not-a-route"));
        assert_eq!(state.page, Page::Library);
    }

    #[test]
    fn theme_toggle_is_reversible() {
        let mut state = AppState::default();
        state.toggle_theme();
        assert_eq!(state.theme, Theme::Light);
        state.toggle_theme();
        assert_eq!(state.theme, Theme::Dark);
    }

    #[test]
    fn system_theme_only_controls_system_mode() {
        let mut state = AppState::with_settings(&settings::NativeSettings::default(), false);
        assert_eq!(state.theme, Theme::Light);
        state.update_system_theme(true);
        assert_eq!(state.theme, Theme::Dark);
        state.select_theme_mode(ThemeMode::Light);
        state.update_system_theme(false);
        state.update_system_theme(true);
        assert_eq!(state.theme, Theme::Light);
    }
}
