# Spotify desktop integration port

Source of truth: BitChord Mobile release `v1.8.1`.

## Porting rules

- Keep `SpotifyLibrary.kt` aligned with the mobile release; its blob already matches the release.
- Keep `SpotifyImporter.kt` aligned with mobile `v1.8.1`, including URL/URI validation, paged playlist reads, bounded matching, and the mobile fallback matching behavior.
- Adapt the mobile `SpotifyToken` flow to JavaFX WebEngine: apply the saved `sp_dc` cookie, inject a fetch/XHR hook at the earliest page-load transition, and capture the actual authenticated `/api/token` response. Do not depend on the old static TOTP-v12 token mint.
- Reuse the captured bearer/client-token pair for both Spotify library queries and Spotify Canvas lookups.
- Provide a dedicated Spotify destination like mobile's `AppRoute.Spotify`; keep Spotify playlists out of the YouTube Music library feed.
- Open Spotify playlist pages as desktop collections, stream pending rows as pages arrive, match tracks with bounded concurrency, and prevent unresolved rows from entering playback.
- Preserve the existing desktop queue, playback engine, and navigation.

## Validation

- Unit tests cover the mobile Spotify library parser and playlist-ID validation.
- CI must pass desktop Linux and Windows builds/tests.
- Android's live Genius integration test is external to this desktop port; failures there must be checked separately rather than attributed to Spotify changes.
- A real Spotify session is still required to validate login, playlist retrieval, token refresh, and actual playback end-to-end.
