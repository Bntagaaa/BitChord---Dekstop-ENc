# ASTRA visual changes — work in progress

Reference: ASTRA.pdf, eight annotated pages supplied on 2026-10-24.
Base: main at 7e0af6f4.

Implemented for review:
- Keep the floating player visible when playback is empty, with BitChord logo and inactive transport.
- Permit empty Lyrics / Up Next panels.
- Enable panel glass only on Home, preserving the existing content displacement and panel dimensions. Reduced-blur preference continues to apply.

Still pending:
- Verify Home blur visually against the reference (including what is sampled behind the panel).
- Unify dark translucent context menus and account flyout without changing their actions.
- Convert lyrics provider selection to anchored menu; retain footer trigger placement.
- Expanded player: increase lyrics space, add collapse/fullscreen controls, center the active line; preserve existing transport layout.
- Windows/Linux interactive checks, including empty player, navigation, focus, menus and lyrics scrolling.

This branch does not yet implement the complete PDF.
