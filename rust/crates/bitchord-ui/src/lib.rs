//! Native window binding. No Kotlin runtime or browser frontend is linked.

use bitchord_core::{AppState, Theme};
use slint::ComponentHandle;
use std::cell::RefCell;
use std::rc::Rc;

slint::include_modules!();

pub fn run(state: AppState) -> Result<(), slint::PlatformError> {
    tracing::info!("opening native window");
    build_window(state)?.run()
}

fn build_window(state: AppState) -> Result<MainWindow, slint::PlatformError> {
    let window = MainWindow::new()?;
    let state = Rc::new(RefCell::new(state));
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
    window.on_toggle_theme(move || {
        let mut state = theme_state.borrow_mut();
        state.toggle_theme();
        tracing::debug!(theme = ?state.theme, "theme changed");
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
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn native_window_callbacks_update_route_and_theme() {
        i_slint_backend_testing::init_no_event_loop();
        let window = build_window(AppState::default()).expect("headless Slint window");
        assert_eq!(window.get_current_page().as_str(), "home");
        assert!(window.get_dark_mode());

        window.invoke_navigate("search".into());
        assert_eq!(window.get_current_page().as_str(), "search");
        assert_eq!(window.get_page_title().as_str(), "Search");
        window.invoke_navigate("nonexistent".into());
        assert_eq!(window.get_current_page().as_str(), "search");

        window.invoke_toggle_theme();
        assert!(!window.get_dark_mode());
    }
}
