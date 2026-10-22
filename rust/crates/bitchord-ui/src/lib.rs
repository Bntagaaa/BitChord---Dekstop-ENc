//! Native window binding. No Kotlin runtime or browser frontend is linked.

use bitchord_core::{
    AppState, Theme, ThemeMode,
    settings::{NativeSettings, SettingsStore},
};
use slint::ComponentHandle;
use std::cell::RefCell;
use std::rc::Rc;

slint::include_modules!();

pub fn run(state: AppState, settings: SettingsStore) -> Result<(), slint::PlatformError> {
    tracing::info!("opening native window");
    build_window(state, Some(settings))?.run()
}

fn build_window(
    state: AppState,
    settings: Option<SettingsStore>,
) -> Result<MainWindow, slint::PlatformError> {
    let window = MainWindow::new()?;
    let state = Rc::new(RefCell::new(state));
    let settings = Rc::new(settings);
    sync_window(&window, &state.borrow());

    let weak_window = window.as_weak();
    let route_state = Rc::clone(&state);
    window.on_navigate(move |route| {
        let mut state = route_state.borrow_mut();
        if state.navigate(route.as_str()) {
            tracing::debug!(page = state.page.id(), "navigated");
            if let Some(window) = weak_window.upgrade() {
                sync_window(&window, &state);
            }
        }
    });

    let weak_window = window.as_weak();
    let theme_state = Rc::clone(&state);
    let theme_settings = Rc::clone(&settings);
    window.on_toggle_theme(move || {
        let mut state = theme_state.borrow_mut();
        state.toggle_theme();
        save_settings(&theme_settings, &state);
        tracing::debug!(theme = ?state.theme, "theme changed");
        if let Some(window) = weak_window.upgrade() {
            sync_window(&window, &state);
        }
    });

    let weak_window = window.as_weak();
    let mode_state = Rc::clone(&state);
    let mode_settings = Rc::clone(&settings);
    window.on_select_theme_mode(move |mode| {
        let Some(mode) = ThemeMode::from_id(mode.as_str()) else {
            return;
        };
        let mut state = mode_state.borrow_mut();
        state.select_theme_mode(mode);
        save_settings(&mode_settings, &state);
        if let Some(window) = weak_window.upgrade() {
            sync_window(&window, &state);
        }
    });

    let weak_window = window.as_weak();
    let scale_state = Rc::clone(&state);
    let scale_settings = Rc::clone(&settings);
    window.on_select_ui_scale(move |scale| {
        let Ok(scale) = u16::try_from(scale) else {
            return;
        };
        let mut state = scale_state.borrow_mut();
        state.select_ui_scale(scale);
        save_settings(&scale_settings, &state);
        if let Some(window) = weak_window.upgrade() {
            sync_window(&window, &state);
        }
    });

    Ok(window)
}

fn sync_window(window: &MainWindow, state: &AppState) {
    window.set_current_page(state.page.id().into());
    window.set_page_title(state.page.title().into());
    window.set_dark_mode(state.theme == Theme::Dark);
    window.set_theme_mode(state.theme_mode.id().into());
    window.set_ui_scale_percent(i32::from(state.ui_scale_percent));
}

fn save_settings(store: &Option<SettingsStore>, state: &AppState) {
    if let Some(store) = store {
        let settings = NativeSettings {
            theme_mode: state.theme_mode,
            ui_scale_percent: state.ui_scale_percent,
        };
        if let Err(error) = store.save(&settings) {
            tracing::error!(%error, path = %store.path().display(), "unable to save native settings");
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn native_window_callbacks_update_route_and_theme() {
        i_slint_backend_testing::init_no_event_loop();
        let window = build_window(AppState::default(), None).expect("headless Slint window");
        assert_eq!(window.get_current_page().as_str(), "home");
        assert!(window.get_dark_mode());

        window.invoke_navigate("search".into());
        assert_eq!(window.get_current_page().as_str(), "search");
        assert_eq!(window.get_page_title().as_str(), "Search");
        window.invoke_navigate("nonexistent".into());
        assert_eq!(window.get_current_page().as_str(), "search");

        window.invoke_toggle_theme();
        assert!(!window.get_dark_mode());
        window.invoke_select_theme_mode("SYSTEM".into());
        assert_eq!(window.get_theme_mode().as_str(), "SYSTEM");
        window.invoke_select_ui_scale(125);
        assert_eq!(window.get_ui_scale_percent(), 125);
    }
}
