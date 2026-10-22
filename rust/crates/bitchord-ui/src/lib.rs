//! Native window binding. No Kotlin runtime or browser frontend is linked.

use bitchord_audio::LocalAudioPlayer;
use bitchord_core::{
    AppState, Theme, ThemeMode,
    local_music::{LocalSong, default_roots, scan},
    settings::{NativeSettings, SettingsStore},
};
use slint::{ComponentHandle, ModelRc, VecModel};
use std::cell::{Cell, RefCell};
use std::rc::Rc;
use std::sync::{Arc, Mutex};
use std::time::Duration;

slint::include_modules!();

pub fn run(state: AppState, settings: SettingsStore) -> Result<(), slint::PlatformError> {
    tracing::info!("opening native window");
    let (window, playback) = build_window(state, Some(settings))?;
    start_local_scan(&window, Arc::clone(&playback.songs));
    let weak_window = window.as_weak();
    let timer = slint::Timer::default();
    timer.start(
        slint::TimerMode::Repeated,
        Duration::from_millis(250),
        move || {
            if let Some(window) = weak_window.upgrade() {
                let ended = playback.player.borrow().snapshot().ended;
                if ended {
                    let next = playback
                        .current
                        .get()
                        .and_then(|index| index.checked_add(1));
                    let advanced = next.is_some_and(|index| play_local(&window, &playback, index));
                    if !advanced {
                        playback.player.borrow_mut().stop();
                        playback.current.set(None);
                        window.set_has_local_track(false);
                    }
                } else {
                    sync_playback(&window, &playback);
                }
            }
        },
    );
    window.run()
}

struct PlaybackController {
    songs: Arc<Mutex<Vec<LocalSong>>>,
    player: RefCell<LocalAudioPlayer>,
    current: Cell<Option<usize>>,
}

fn build_window(
    state: AppState,
    settings: Option<SettingsStore>,
) -> Result<(MainWindow, Rc<PlaybackController>), slint::PlatformError> {
    let window = MainWindow::new()?;
    let playback = Rc::new(PlaybackController {
        songs: Arc::new(Mutex::new(Vec::new())),
        player: RefCell::new(LocalAudioPlayer::default()),
        current: Cell::new(None),
    });
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

    let weak_window = window.as_weak();
    let selected_playback = Rc::clone(&playback);
    window.on_play_local(move |index| {
        if let (Ok(index), Some(window)) = (usize::try_from(index), weak_window.upgrade()) {
            play_local(&window, &selected_playback, index);
        }
    });

    let weak_window = window.as_weak();
    let toggle_playback = Rc::clone(&playback);
    window.on_toggle_local_playback(move || {
        let mut player = toggle_playback.player.borrow_mut();
        let result = if player.snapshot().playing {
            player.pause()
        } else {
            player.resume()
        };
        drop(player);
        if let Some(window) = weak_window.upgrade() {
            if let Err(error) = result {
                window.set_local_error(error.into());
            }
            sync_playback(&window, &toggle_playback);
        }
    });

    let weak_window = window.as_weak();
    let next_playback = Rc::clone(&playback);
    window.on_next_local_track(move || {
        if let (Some(index), Some(window)) = (next_playback.current.get(), weak_window.upgrade()) {
            play_local(&window, &next_playback, index + 1);
        }
    });

    let weak_window = window.as_weak();
    let previous_playback = Rc::clone(&playback);
    window.on_previous_local_track(move || {
        if let (Some(index), Some(window)) =
            (previous_playback.current.get(), weak_window.upgrade())
        {
            play_local(&window, &previous_playback, index.saturating_sub(1));
        }
    });

    Ok((window, playback))
}

fn start_local_scan(window: &MainWindow, songs: Arc<Mutex<Vec<LocalSong>>>) {
    window.set_local_scanning(true);
    let weak_window = window.as_weak();
    std::thread::spawn(move || {
        let found = scan(&default_roots(), false);
        let rows: Vec<(String, String, String)> = found
            .iter()
            .map(|song| (song.title.clone(), song.artist.clone(), song.codec.clone()))
            .collect();
        if let Ok(mut current) = songs.lock() {
            *current = found;
        }
        let _ = weak_window.upgrade_in_event_loop(move |window| {
            let rows = rows
                .into_iter()
                .map(|(title, artist, codec)| LocalTrackRow {
                    title: title.into(),
                    artist: artist.into(),
                    codec: codec.into(),
                })
                .collect::<Vec<_>>();
            window.set_local_tracks(ModelRc::new(VecModel::from(rows)));
            window.set_local_scanning(false);
        });
    });
}

fn play_local(window: &MainWindow, controller: &PlaybackController, index: usize) -> bool {
    let song = controller
        .songs
        .lock()
        .ok()
        .and_then(|songs| songs.get(index).cloned());
    let Some(song) = song else { return false };
    let result = controller.player.borrow_mut().play(&song.path);
    match result {
        Ok(()) => {
            controller.current.set(Some(index));
            window.set_playing_title(song.title.into());
            window.set_playing_artist(song.artist.into());
            window.set_has_local_track(true);
            window.set_local_error("".into());
            sync_playback(window, controller);
            true
        }
        Err(error) => {
            tracing::warn!(%error, "local playback failed");
            window.set_local_error(error.into());
            false
        }
    }
}

fn sync_playback(window: &MainWindow, controller: &PlaybackController) {
    let snapshot = controller.player.borrow().snapshot();
    window.set_local_playing(snapshot.playing);
    let format_time = |millis: u64| format!("{}:{:02}", millis / 60_000, millis / 1000 % 60);
    window.set_local_time(
        format!(
            "{} / {}",
            format_time(snapshot.position_ms),
            format_time(snapshot.duration_ms)
        )
        .into(),
    );
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
        let (window, _playback) =
            build_window(AppState::default(), None).expect("headless Slint window");
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
