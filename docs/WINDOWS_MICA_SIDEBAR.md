# Windows 11 Mica-style sidebar

The desktop navigation rail uses an **opaque wallpaper-tinted Mica-style surface** on Windows 11 by default. It supports both the 72dp collapsed icon rail and 220dp expanded sidebar, follows BitChord's dark/light theme, and has its own **Use Mica-style sidebar** preference in Settings.

## Why this is not a DWM Mica surface

DWM's `DWMWA_SYSTEMBACKDROP_TYPE` targets an **entire native window**, not a rectangle inside a Compose UI. BitChord's decorated AWT / Compose window deliberately remains opaque so the real Windows caption, native resize, Snap and taskbar integrations remain stable. Its existing `Native title bar material` setting requests genuine DWM Mica, but that material cannot be exposed in just the client-side Compose navigation rail without rearchitecting the app around Windows App SDK/XAML islands or breaking this opaque-window invariant.

The sidebar instead approximates the main **wallpaper-tint** part of Mica using one cached average color from the user's wallpaper, blended very subtly with a dark/light neutral base. It does **not** use Haze, live desktop capture, acrylic blur, translucency, reflection gradients, window hooks, or a GPU shader. This is **not native Mica** and must not be advertised as such.

## Behavior

- On Windows 11 (build 22000+), the option defaults to ON; on other platforms the sidebar stays unchanged.
- Wallpaper is sampled off the UI thread via `SystemParametersInfoW` and `ImageIO`, at most once per wallpaper file/version. Source subsampling keeps memory usage low. While the sidebar is mounted and the feature is enabled, the cached source signature is checked every 60 seconds.
- If wallpaper extraction fails, transparency effects are disabled in Windows settings, or high contrast is enabled, the original solid `sidebarSurface` is rendered.
- The existing **Reduce dynamic blur** preference remains independent; no blur is added to the sidebar in either mode.
- Light/dark and wallpaper changes update the color without restarting the window. Sidebar width, controls, icons, playlist navigation, collapse animation and tooltips remain unchanged.
- No wallpapers or other user files are uploaded or cached on disk by BitChord; only the in-process sampled RGB tone is held.

## Windows verification checklist

1. On Windows 11 with Transparency effects enabled, open a brightly colored wallpaper and confirm the navigation rail gets a restrained tinted shade. Compare with a different wallpaper or after changing it.
2. Test both dark and light modes, 100%, 125%, 200% UI scale, expanded/collapsed rail and sidebar animation.
3. Toggle **Use Mica-style sidebar** OFF to restore the exact previous opaque color; ON to restore the wallpaper-tinted material.
4. Toggle **Reduce dynamic blur** independently; no reflective or blurry sidebar artifacts should appear.
5. Disable Windows Transparency effects, enable High Contrast, or test on Windows 10/Linux: the original solid fallback should be used.
6. Confirm Windows title bar, Snap, drag/resize, player, search, lyrics/queue, and Windows portable packaging are unchanged.

Run `./gradlew :desktopApp:test` and desktop distributable build on the GitHub Actions Windows job. A successful compile cannot replace this visual test on Windows.
