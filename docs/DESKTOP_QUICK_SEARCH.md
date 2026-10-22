# Desktop Quick Search

`Ctrl+K` opens a separate, compact search overlay. The original Search page remains
available through `Ctrl+Shift+L`; its query, results, filters, scroll and navigation
are not replaced by the overlay.

## Interaction

| Input | Behavior |
| --- | --- |
| Ctrl+K | Open; when already open, focus its query without clearing it |
| Typing / Space | Edit the query; a 300 ms debounce requests live song results |
| Up / Down | Select previous/next song, clamped to the list bounds |
| Shift+Enter | Play the selected song through the existing player and close |
| Enter alone | No action |
| Esc | Close without playing or leaving the underlying page |
| Mouse movement | Highlight the song under the pointer |
| Click song | Play that song and close |
| Click backdrop | Close |
| Tab / Shift+Tab | Keep focus inside the overlay's query |

The query starts empty each time. With a blank query, only the input is shown.
Only tracks are shown; artist/album/playlist browsing still belongs to full Search.
The first result is selected automatically. Selection alone never starts playback.
No selection/loading/error produces no playback action.

## Implementation boundaries

- `DesktopSearchClient.searchTypeahead()` supplies the existing `SearchResult` models.
- `TopTrack` and `Track` supply `Song`; browse results are omitted.
- Results are deduplicated by video ID and limited to ten.
- Editing clears old rows immediately. Each request has a revision so canceled,
  superseded or previous-open responses cannot overwrite newer results.
- `playQuickSearchSong` in `DesktopApp.kt` delegates to `recordSongSearch` and the
  existing `playSong` path, including existing queue/source/party policies.
- The overlay is drawn inside the app's existing overlay layer, not a new native
  popup. Its keys are routed from the existing root preview handler.
- Play and dismiss key actions run on key-up and consume their complete key cycle.
  Pending releases are also consumed after mouse dismissal.
- Closing restores root keyboard focus after the query is removed from composition;
  restoration is deferred while an unrelated dialog is open.
- `DesktopQuickSearchState<T>` and `DesktopQuickSearchKeys` can be tested without a
  UI or network. No Android or shared UI behavior is changed.

## Validation

Automated tests cover state transitions, debouncing request identity, stale response
rejection, blank/empty/error states, selection, repeat handling, Enter/Shift+Enter,
Escape, focus-reset bookkeeping and coexistence with the existing shortcut registry.
The debounce timing itself is implemented by the view's cancellable `LaunchedEffect`.

Run the project tests and package with:

```sh
bash ./gradlew :shared:jvmTest :desktopApp:test :desktopApp:packageDeb --console=plain -Pbitchord.version=1.8.0-quicksearch
```

Manual checks still required on the real desktop:

1. Open from Home, existing Search, Now Playing and the lyrics panel.
2. Verify the empty compact box, results, artwork, scrolling and hover highlighting.
3. Type spaces and edit using Ctrl+Left/Right and Shift+Left/Right.
4. Confirm Enter alone does nothing and Shift+Enter plays exactly once.
5. Close using Esc/click/play and immediately use Space/Ctrl+/ without switching tabs.
6. Confirm the old Search query and underlying player/lyrics panel stay intact.
7. Check app switching while keys are held and closing after Audio output/account dialogs.

A passing unit-test/build run is not a claim that these visual/native-focus checks
have already been performed on Ubuntu or Windows.
