//! UI-independent application state for the first native desktop milestone.

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

#[derive(Debug, Default, Eq, PartialEq)]
pub struct AppState {
    pub page: Page,
    pub theme: Theme,
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
        self.theme = match self.theme {
            Theme::Dark => Theme::Light,
            Theme::Light => Theme::Dark,
        };
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
}
