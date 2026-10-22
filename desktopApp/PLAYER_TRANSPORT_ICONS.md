# Desktop transport icon attribution

The nine SVG glyphs in `src/main/composeResources/drawable/player_transport_*.svg`
were supplied in `bitchord-player-icons(1).zip`.

- Upstream: [kushagrasinghx/BitChord](https://github.com/kushagrasinghx/BitChord)
- Source file: `web/app/src/components/icons/Transport.tsx`
- Referenced upstream commit: `edc1e6f`
- License declared by the supplied pack: GPL-3.0 (same as this repository's LICENSE)

The original SVG path geometry and view boxes are preserved. For Compose Desktop
resource compatibility, `currentColor` was changed to an opaque white fill;
the Compose `Icon` tint supplies the runtime color (including selected/disabled
states). The Previous glyph uses the same mirrored Next path as the supplied SVG.

Included glyphs: play, pause, previous, next, shuffle, repeat, repeat-one,
lyrics, and up-next (queue). Volume and BitChord logo were not in the pack and
were not replaced.
