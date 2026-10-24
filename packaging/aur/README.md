# BitChord Desktop Revived for Arch Linux / CachyOS

This directory contains the **binary AUR package recipe**
`bitchord-desktop-revived-bin`. It targets Arch Linux and derivatives including
CachyOS, EndeavourOS, and other x86_64 systems.

**Status:** The package files are maintained in this GitHub repository.
They are **not automatically registered or published on aur.archlinux.org**.
The `paru -S bitchord-desktop-revived-bin` command works only *after* an
AUR maintainer has published the package under that name.

## Install directly from GitHub (before AUR publication)

Once this change is merged into `main`:

```bash
git clone https://github.com/Bntagaaa/BitChord-Desktop-Revived.git
cd BitChord-Desktop-Revived/packaging/aur
makepkg -si
```

On a test branch, check out that branch instead of `main`. The build downloads
the published Linux AppImage (~325 MB for v1.8.2) and verifies its **fixed SHA-256**
from GitHub's release metadata. You need `base-devel` and `git`:

```bash
sudo pacman -S --needed base-devel git
```

After installation:

- Open **BitChord Desktop Revived** in KDE's Application Launcher.
- Or run `bitchord-desktop-revived` from a terminal.
- Uninstall with `sudo pacman -Rns bitchord-desktop-revived-bin`.

The package installs the original app binaries and bundled Java runtime under
`/opt/bitchord-desktop-revived`. It extracts the AppImage **at package build
time**, so the installed app launches directly from its stable jpackage path:
there is no AppImage mount on each launch and no runtime FUSE dependency.
It preserves the AppImage's existing niri-specific AWT hint without changing KDE
or GNOME behavior.

User preferences, accounts, and saved sessions are **not** deleted or migrated
by package install/uninstall. As with standalone AppImage or installer builds,
a newly replaced executable may prompt onboarding once, under BitChord's
per-install welcome policy.

If you previously created `~/.local/share/applications/Bitchord.desktop`
manually, it is independent of this package and may remain as a **second**
launcher. Remove or update that personal shortcut manually if desired;
the package intentionally never edits user-owned shortcut files.

## When published to AUR

Users can install and update normally:

```bash
paru -S bitchord-desktop-revived-bin
paru -Syu
```

This is the same BitChord Linux binary released on GitHub; it is not specially
compiled for CachyOS's x86-64-v3/v4 repositories. The AUR helper builds an Arch
package from the upstream binary without recompiling BitChord.

## Update and release maintenance

1. Publish a stable semantic-versioned GitHub release such as `v1.8.3` with
   `BitChord-1.8.3-linux-x86_64.AppImage` and its GitHub SHA-256 digest.
2. `.github/workflows/aur-update.yml` checks the release, updates `PKGBUILD`,
   regenerates `.SRCINFO`, and opens a reviewable GitHub PR.
3. `.github/workflows/aur-package-check.yml` verifies the source checksum,
   regenerates and checks metadata, and builds the package in an Arch container.
4. Review and merge the update PR only after checks pass.
5. The AUR maintainer then copies `PKGBUILD` and `.SRCINFO` to the separate
   AUR Git repository and pushes the version bump.

First AUR publication requires an [AUR account](https://aur.archlinux.org/)
with an SSH public key and an available package name. Example (from this repo):

```bash
git clone ssh://aur@aur.archlinux.org/bitchord-desktop-revived-bin.git /tmp/bitchord-aur
cp packaging/aur/PKGBUILD packaging/aur/.SRCINFO /tmp/bitchord-aur/
cd /tmp/bitchord-aur
git add PKGBUILD .SRCINFO
git commit -m "Initial BitChord Desktop Revived package"
git push
```

For later releases, repeat the copy, commit, and push with the updated files.
GitHub Actions intentionally **does not** push to the AUR: that requires a
separately configured AUR SSH identity, and a human should approve new release
binaries before publishing them to package users.

The auto-PR workflow relies on GitHub repository Actions permissions allowing
creation of pull requests by `GITHUB_TOKEN`; enable that repository setting if
GitHub reports an authorization error.

## Developer checks

```bash
python -m unittest discover -s packaging/aur -p 'test_*.py' -v
cd packaging/aur
bash -n PKGBUILD
makepkg --printsrcinfo
makepkg --verifysource
makepkg -s
```

Avoid running `makepkg` as root. Builds can consume several GB of temporary
disk space because the original AppImage includes a full application runtime.
