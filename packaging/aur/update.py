#!/usr/bin/env python3
"""Prepare a DEB-backed AUR version bump from a published BitChord release.

Updates the PKGBUILD only; run makepkg --printsrcinfo afterwards to update .SRCINFO.
No AUR credentials or automatic AUR publication are involved.
"""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import re
import urllib.parse
import urllib.request

REPO = "Bntagaaa/BitChord-Desktop-Revived"
PKGBUILD = Path(__file__).resolve().with_name("PKGBUILD")
SHA_LINE = r"(?m)^sha256sums=\('([0-9a-f]{64})'\)$"
VERSION_LINE = r"(?m)^pkgver=(\d+\.\d+\.\d+)$"


def release_metadata(tag: str | None) -> dict:
    endpoint = (
        f"https://api.github.com/repos/{REPO}/releases/tags/{urllib.parse.quote(tag, safe='')}"
        if tag
        else f"https://api.github.com/repos/{REPO}/releases/latest"
    )
    headers = {"Accept": "application/vnd.github+json", "User-Agent": "BitChord-AUR-updater"}
    token = os.environ.get("GH_TOKEN") or os.environ.get("GITHUB_TOKEN")
    if token:
        headers["Authorization"] = f"Bearer {token}"
    with urllib.request.urlopen(urllib.request.Request(endpoint, headers=headers), timeout=30) as response:
        return json.load(response)


def validated_release(release: dict) -> tuple[str, str]:
    if release.get("draft") or release.get("prerelease"):
        raise ValueError("Only published, non-prerelease versions may update the AUR recipe")
    tag = release.get("tag_name", "")
    match = re.fullmatch(r"v(\d+\.\d+\.\d+)", tag)
    if not match:
        raise ValueError(f"Unsupported release tag: {tag!r}")
    version = match.group(1)
    expected_name = f"BitChord-{version}-linux-amd64.deb"
    assets = [asset for asset in release.get("assets", []) if asset.get("name") == expected_name]
    if len(assets) != 1:
        raise ValueError(f"Expected exactly one release asset named {expected_name}")
    digest = assets[0].get("digest", "")
    if not re.fullmatch(r"sha256:[0-9a-f]{64}", digest):
        raise ValueError(f"Missing or invalid published GitHub SHA-256 for {expected_name}")
    return version, digest.removeprefix("sha256:")


def change_one(content: str, pattern: str, replacement: str) -> str:
    updated, count = re.subn(pattern, lambda _: replacement, content)
    if count != 1:
        raise ValueError(f"Expected exactly one match for PKGBUILD pattern {pattern!r}; got {count}")
    return updated


def updated_pkgbuild(content: str, version: str, digest: str) -> str:
    current_match = re.search(VERSION_LINE, content)
    current_sha = re.search(SHA_LINE, content)
    if not current_match or not current_sha:
        raise ValueError("PKGBUILD version or checksum format has changed; update it manually")
    current = current_match.group(1)
    if tuple(map(int, version.split("."))) < tuple(map(int, current.split("."))):
        print(f"Ignoring older release v{version}; package already targets v{current}")
        return content
    if version == current:
        if digest != current_sha.group(1):
            raise ValueError("Release asset changed without a version bump; review manually")
        print(f"AUR recipe already tracks v{version}")
        return content

    updated = change_one(content, VERSION_LINE, f"pkgver={version}")
    updated = change_one(updated, r"(?m)^pkgrel=\d+$", "pkgrel=1")
    updated = change_one(updated, SHA_LINE, f"sha256sums=('{digest}')")
    return updated


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--tag", help="Published release tag, e.g. v1.8.3; default: latest stable")
    args = parser.parse_args()
    version, digest = validated_release(release_metadata(args.tag))
    original = PKGBUILD.read_text(encoding="utf-8")
    updated = updated_pkgbuild(original, version, digest)
    changed = updated != original
    if changed:
        PKGBUILD.write_text(updated, encoding="utf-8")
        print(f"Prepared AUR package v{version} with SHA-256 {digest}")
    github_output = os.environ.get("GITHUB_OUTPUT")
    if github_output:
        with open(github_output, "a", encoding="utf-8") as output:
            output.write(f"changed={'true' if changed else 'false'}\nversion={version}\n")


if __name__ == "__main__":
    main()
