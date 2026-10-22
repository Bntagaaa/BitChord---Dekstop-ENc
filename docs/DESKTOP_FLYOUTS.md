# Desktop flyout consistency

## Scope

Quick Search and Keyboard Shortcuts reuse Audio Output's `DesktopFlyoutCard` and
`DesktopFlyoutBackdrop`: 20 dp corners, the existing `DesktopCardFill`, 0.5 dp
`DesktopCardEdge`, and `DesktopScrim`. Quick Search keeps its top alignment and
650 dp maximum width; Keyboard Shortcuts keeps its centered 540 dp panel.
Account Picker keeps its existing top-right glass appearance.

Quick Search's own BasicTextField now uses the dark search appearance (36 dp
height, 7 dp corners, subtle border, light text). Do not replace it with
DesktopSearchField: the full-page field has different Enter and modal-editing
rules. Search requests, query state, selection, scroll, play callbacks, and
DesktopQuickSearchKeys are unchanged.

## Escape ownership

`DesktopFlyoutHost` wraps the app in Main.kt, before DesktopFrame's existing
preview handler. Each visible flyout registers its layer in a per-window
`DesktopFlyoutState`. The host handles Escape only. Audio Output opts in to
dismissal, as does Account Picker and Keyboard Shortcuts. Other registered
dialogs are barriers, without changing their existing dismiss/cancel callbacks.
Quick Search delegates Escape to its established router, including the release
after a mouse dismissal.

A press belongs to the exact registration at KeyDown. Both halves are consumed
for intercepted flyouts; dismissal happens once at KeyUp, only if that same
registration is still topmost. Auto-repeat, replacement panels, mouse dismissal,
and focus loss cannot dismiss a second layer. A single Escape must never also
close the player/lyrics behind a flyout.

The host adds no native window and no new focus target. Its FocusRequester
resolves to DesktopFrame's existing shortcut root. After the final registered
flyout is disposed, it repairs missing focus on the next frame, with bounded
retries. It does not steal focus from a real editor/control, a surviving flyout,
or another native window. Existing Quick Search and player focus recovery stay
in place.

Audio Output rendered as a Settings page retains its original page navigation;
only the actual flyout registers for Escape dismissal.

## Validation

Run `./gradlew :desktopApp:test --console=plain` (or `gradlew.bat` on Windows).
`DesktopFlyoutStateTest` covers one-dismiss-per-press, paint order, nested panels,
replacement/mouse dismissal, window focus cancellation, repeated cycles,
independent windows, and coexistence with the unchanged Quick Search key router.
These are logic tests, not proof of native focus/visual behavior.

Before shipping, test interactively on Linux and Windows:

1. Open Audio Output and Account Picker from Home and from a player/lyrics view.
   Escape closes only the flyout, without changing the output or selected account.
2. Repeated Escape KeyDowns followed by one release close one layer only.
3. Open/close Quick Search with Escape, Shift+Enter, song click, and outside click.
   Space types spaces, arrows select/scroll, plain Enter is inert, and the old
   Ctrl+Shift+L Search query remains intact.
4. Open/close Keyboard Shortcuts with Ctrl+/, Escape, the close button, and outside
   click. Check scrolling and the canonical shortcut list.
5. Immediately after closing, test Space/Ctrl+K/Ctrl+/ without changing tabs.
   Repeat after clicking lyrics, clicking a menu row, and switching windows.
6. Check the smallest supported window and normal/maximized layouts for clipping.

The full Gradle build, rendering, playback, and native focus must be validated
in a dependency-enabled build/runtime environment; standalone logic tests alone
are not a release gate.
