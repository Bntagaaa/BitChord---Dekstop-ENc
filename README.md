<div align="center">

<img src="Logo.png" alt="Original BitChord logo" width="140" />

# BitChord Desktop — Revived

**Keeping the original BitChord desktop experience alive.**

An independent, community-maintained continuation of the native **BitChord Desktop** app for **Windows and Linux**, built with Kotlin and Compose Multiplatform.

![Status](https://img.shields.io/badge/status-revival%20in%20progress-DAA520?style=flat-square)
![Platforms](https://img.shields.io/badge/platforms-Windows%20%7C%20Linux-6677AA?style=flat-square)
[![GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-555555?style=flat-square)](LICENSE)

[**Downloads**](#downloads) · [**Features**](#features) · [**Build**](#building-from-source) · [**Roadmap**](#roadmap) · [**Credits**](#credits--upstream)

</div>

> [!IMPORTANT]
> **Unofficial community revival.** This repository is a fork of [BitChord by @kushagrasinghx](https://github.com/kushagrasinghx/BitChord). It is **not an official continuation** and is not endorsed, maintained, or supported by the original creator or upstream contributors. All credit for the original project belongs to its creators and contributors.

## Why this project exists

The original BitChord project is moving its desktop experience to a **web player**. Its maintainer proposed retiring the native desktop application in [upstream PR #874](https://github.com/kushagrasinghx/BitChord/pull/874).

We love what the original team built and believe the native desktop client is worth preserving. **BitChord Desktop — Revived** aims to maintain and improve that existing application, not replace or compete with the original BitChord project.

Our focus is straightforward:

- **Preserve** the Kotlin/Compose desktop client and its native desktop integrations.
- **Stabilize** playback, window behavior, and everyday desktop usability.
- **Improve** keyboard navigation, search, and Windows/Linux compatibility.
- **Keep it open** to contributions, with transparent attribution to upstream.

The original [BitChord repository](https://github.com/kushagrasinghx/BitChord) remains the home of the main project, including Android and its web-player direction.

## Project foundation

BitChord Desktop — Revived builds on the original native desktop codebase created by [@kushagrasinghx](https://github.com/kushagrasinghx) and developed with the [BitChord contributors](https://github.com/kushagrasinghx/BitChord/graphs/contributors). This is an **independent, community-maintained fork**, not an official upstream release.

| | |
| --- | --- |
| Original project | [kushagrasinghx/BitChord](https://github.com/kushagrasinghx/BitChord) |
| Community continuation | [Bntagaaa/BitChord-Desktop-Revived](https://github.com/Bntagaaa/BitChord-Desktop-Revived) |
| Technology | Kotlin, Compose Multiplatform, Gradle |
| Desktop targets | Windows and Linux |
| Revival release branch | [release/v1.8.1-revived.1](https://github.com/Bntagaaa/BitChord-Desktop-Revived/tree/release/v1.8.1-revived.1) |

Revival version numbers track development of this community fork. A version labeled `v1.8.1-revived.1` **does not imply that every upstream v1.8.1 change has been integrated**. We review upstream changes selectively to preserve the native desktop client.

## Project status

> [!WARNING]
> **Revival in progress.** This is an early community effort, not a claim that all desktop bugs are fixed or that a new stable release is ready. Features and fixes developed on experimental branches may not yet be present in the default branch or downloadable builds.

| Area | Current direction |
| --- | --- |
| Windows | Preserve and stabilize the native desktop app |
| Linux | Maintain desktop support, including window-manager compatibility |
| macOS | Not currently a configured/supported desktop target |
| Releases | Community testing builds first; stable releases after verification |
| Upstream updates | Review and selectively port compatible fixes rather than blindly merging desktop removal |

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

Our initial priorities (not promises of completed work):

- [ ] Establish a protected, buildable baseline for the preserved desktop client.
- [ ] Investigate and fix playback stalls and decoder lifecycle issues.
- [ ] Improve native window behavior on Windows.
- [ ] Address Linux maximize/window-manager issues, including Wayland compositors.
- [ ] Refine keyboard shortcuts, focus recovery, and Quick Search.
- [ ] Set up independent Windows/Linux build checks and properly labeled releases.

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
