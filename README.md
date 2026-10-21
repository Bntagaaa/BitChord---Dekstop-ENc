<div align="center">

<img src="Logo.png" alt="Original BitChord logo" width="140" />

# BitChord Desktop — Revived

**The community-led continuation of BitChord for PC.**

BitChord Desktop — Revived exists to continue development of the original native **BitChord Desktop** application after the upstream desktop app was discontinued. Built on the original Kotlin and Compose Multiplatform codebase, this project aims to keep the Windows and Linux desktop experience moving forward—not simply preserve it as-is.

![Status](https://img.shields.io/badge/status-revival%20in%20progress-DAA520?style=flat-square)
![Platforms](https://img.shields.io/badge/platforms-Windows%20%7C%20Linux-6677AA?style=flat-square)
[![GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-555555?style=flat-square)](LICENSE)

[**Downloads**](#downloads) · [**Features**](#features) · [**Build**](#building-from-source) · [**Roadmap**](#roadmap) · [**Credits**](#credits--upstream)

</div>

> [!IMPORTANT]
> **Independent community project.** This repository continues development of the original PC/desktop app after its discontinuation upstream. It is a fork of [BitChord by @kushagrasinghx](https://github.com/kushagrasinghx/BitChord), but it is **not an official continuation** and is not endorsed, maintained, or supported by the original creator or upstream maintainers. We respect the original work, preserve its attribution and license, and welcome community contributions.

## Why this project exists

The original project has discontinued its native desktop app as it shifts its desktop direction toward the **Web Player**. See the upstream maintainer's [PR to remove the desktop app](https://github.com/kushagrasinghx/BitChord/pull/874).

We want to continue the PC app's development as a community project. The goal is not to freeze an old snapshot or create a competing replacement for BitChord. Instead, we want to **keep the native desktop edition alive while learning from the ongoing development of BitChord on other platforms**.

### Our direction

- **Continue the PC app:** maintain the existing Kotlin/Compose desktop application and its Windows/Linux integrations.
- **Bring useful improvements across platforms:** review features, fixes, UI ideas, and behavior developed for BitChord's mobile and web experiences, then adapt them for desktop when they make technical and UX sense.
- **Build with the community:** use feedback, bug reports, testing, and contributions to decide what to improve next.
- **Protect reliability:** prioritize playback, keyboard navigation, window management, and platform-specific stability before claiming new features are ready.
- **Respect the original project:** preserve upstream credit and license notices, and clearly distinguish community changes from official upstream releases.

The original [BitChord repository](https://github.com/kushagrasinghx/BitChord) remains the home of the upstream project, including its mobile development and web-player direction. This revival is independent; we will selectively port compatible work rather than assume every change from another platform can be copied directly.

## Project foundation

BitChord Desktop — Revived builds on the original native desktop codebase created by [@kushagrasinghx](https://github.com/kushagrasinghx) and developed with the [BitChord contributors](https://github.com/kushagrasinghx/BitChord/graphs/contributors). This is an **independent, community-maintained fork**, not an official upstream release.

| | |
| --- | --- |
| Original project | [kushagrasinghx/BitChord](https://github.com/kushagrasinghx/BitChord) |
| Community continuation | [Bntagaaa/BitChord-Desktop-Revived](https://github.com/Bntagaaa/BitChord-Desktop-Revived) |
| Technology | Kotlin, Compose Multiplatform, Gradle |
| Desktop targets | Windows and Linux |
| Revival release branch | [release/v1.8.1-revived.1](https://github.com/Bntagaaa/BitChord-Desktop-Revived/tree/release/v1.8.1-revived.1) |

Revival version numbers track this community fork's own development. A version labeled `v1.8.1-revived.1` **does not imply that every upstream v1.8.1 change has been integrated**. We review improvements from upstream, mobile, and web development selectively, adapting them to desktop rather than assuming platform-specific code is interchangeable.

## Project status

> [!WARNING]
> **Revival in progress.** This is an early community effort, not a claim that all desktop bugs are fixed or that a new stable release is ready. Features and fixes developed on experimental branches may not yet be present in the default branch or downloadable builds.

| Area | Current direction |
| --- | --- |
| Windows | Preserve and stabilize the native desktop app |
| Linux | Maintain desktop support, including window-manager compatibility |
| macOS | Not currently a configured/supported desktop target |
| Releases | Community testing builds first; stable releases after verification |
| Cross-platform development | Review and adapt useful mobile/web improvements where technically compatible |
| Community | Prioritize user feedback, testing, and contributions |
| Upstream updates | Selectively port compatible changes while preserving desktop-specific behavior |

## Features

The existing desktop codebase includes:

- **YouTube Music** search, browsing, and playback.
- **Local library** support and downloads.
- **Lyrics** with synced display and multiple provider integrations.
- **Audio features** including crossfade, equalizer, and Automix (beta).
- **Integrations** such as Spotify library access, Discord Rich Presence, and scrobbling.
- **Native desktop controls** and media integrations for supported operating systems.

Availability and reliability can vary by build, operating system, and upstream service changes. We are actively prioritizing stability over adding features that are not yet dependable.

## Downloads

**Community test builds:** [View this fork's Releases](https://github.com/Bntagaaa/BitChord-Desktop-Revived/releases).

The currently published `1.8.0-test` packages are **older experimental Windows builds**, *not* a verified stable release of the revived project. Please treat them as test artifacts.

Looking for the original BitChord project instead? Visit the [official upstream repository](https://github.com/kushagrasinghx/BitChord).

## Building from source

The existing desktop target uses **Java 21** and **Compose Multiplatform**.

**Windows** (PowerShell or Command Prompt):

```powershell
.\gradlew.bat :desktopApp:run
```

**Linux**:

```bash
./gradlew :desktopApp:run
```

Linux requires the appropriate system graphics/audio libraries and native dependencies. See [DESKTOP.md](DESKTOP.md) for the inherited desktop setup and packaging notes. **Note:** portions of that document still refer to historical upstream releases and are being reviewed for this fork.

## Roadmap

Our priorities (not promises of completed work):

- [ ] Maintain a reliable, buildable baseline for the original desktop client.
- [ ] Review useful mobile and web improvements for possible desktop adoption.
- [ ] Investigate playback stalls and decoder lifecycle issues.
- [ ] Improve native window behavior on Windows and Linux, including Wayland compositors.
- [ ] Refine keyboard shortcuts, focus recovery, Quick Search, and desktop-specific workflows.
- [ ] Make it easy for the community to report issues, test builds, and contribute fixes.
- [ ] Maintain independent Windows/Linux build checks and clearly labeled community releases.

Issues and feature branches may contain work in progress; only tested and merged changes should be described as shipped.

## Contributing

Contributions, bug reports, and testing are welcome. Please open an issue with your operating system, exact reproduction steps, logs if available, and the build or commit tested. Keep pull requests focused and note which environments you tested.

Existing documentation includes [CONTRIBUTING.md](CONTRIBUTING.md) and the [Code of Conduct](CODE_OF_CONDUCT.md). Some inherited upstream workflow instructions may still need adaptation for this community fork.

## Credits & upstream

**BitChord was originally created by [@kushagrasinghx](https://github.com/kushagrasinghx).** The original concept, app, much of the desktop architecture, visual design, and countless features come from the work of the upstream creator and contributors.

A huge thank-you to everyone who made BitChord possible:

- **Original project and source:** [kushagrasinghx/BitChord](https://github.com/kushagrasinghx/BitChord)
- **Original contributors:** [View upstream contributors](https://github.com/kushagrasinghx/BitChord/graphs/contributors)
- **Original project documentation and third-party acknowledgements:** [Upstream README](https://github.com/kushagrasinghx/BitChord/blob/main/README.md)
- **Support the original creator:** [Original creator's Ko-fi](https://ko-fi.com/kushagrasinghx)

This fork does **not** claim authorship of the original BitChord project. Revival maintainers are responsible for their own changes and releases; upstream contributors should not be contacted for support with this fork.

## License & disclaimer

This project retains the original **[GNU General Public License v3.0](LICENSE)**. Modified versions and redistributions must comply with its terms, including applicable attribution, license notices, and corresponding-source obligations.

BitChord Desktop — Revived is an independent third-party client. It is not affiliated with or endorsed by **YouTube, Google, Spotify**, or any other referenced service. Users are responsible for complying with relevant laws and third-party terms of service.

---

<div align="center">
  <sub>Built on the work of the original BitChord community. Preserved with respect, continued with care.</sub>
</div>
