"""Offline tests for the safe AUR release updater."""
from pathlib import Path
import re
import unittest

from update import updated_pkgbuild, validated_release

RECIPE = Path(__file__).with_name("PKGBUILD").read_text(encoding="utf-8")
NEW_HASH = "a" * 64
CURRENT_VERSION = re.search(r"(?m)^pkgver=(\\d+\\.\\d+\\.\\d+)$", RECIPE).group(1)
CURRENT_HASH = re.search(r"(?m)^sha256sums=\\('([0-9a-f]{64})'\\)$", RECIPE).group(1)
MAJOR, MINOR, PATCH = map(int, CURRENT_VERSION.split("."))
NEXT_VERSION = f"{MAJOR}.{MINOR}.{PATCH + 1}"


class AurUpdaterTests(unittest.TestCase):
    def test_new_version_updates_digest_and_resets_pkgrel(self):
        changed = updated_pkgbuild(RECIPE, NEXT_VERSION, NEW_HASH)
        self.assertIn(f"pkgver={NEXT_VERSION}", changed)
        self.assertIn("pkgrel=1", changed)
        self.assertIn(f"sha256sums=('{NEW_HASH}')", changed)
        self.assertIn("BitChord-${pkgver}-linux-x86_64.AppImage", changed)

    def test_same_release_is_idempotent(self):
        self.assertEqual(
            RECIPE,
            updated_pkgbuild(
                RECIPE, CURRENT_VERSION, CURRENT_HASH
            ),
        )

    def test_changed_asset_without_version_bump_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "asset changed"):
            updated_pkgbuild(RECIPE, CURRENT_VERSION, NEW_HASH)

    def test_older_release_cannot_downgrade_recipe(self):
        self.assertEqual(RECIPE, updated_pkgbuild(RECIPE, "0.0.1", NEW_HASH))

    def test_release_digest_must_be_explicit_and_valid(self):
        release = {
            "tag_name": "v1.8.3",
            "draft": False,
            "prerelease": False,
            "assets": [{"name": "BitChord-1.8.3-linux-x86_64.AppImage", "digest": "sha256:" + NEW_HASH}],
        }
        self.assertEqual((NEXT_VERSION, NEW_HASH), validated_release(release))
        release["assets"][0]["digest"] = ""
        with self.assertRaisesRegex(ValueError, "SHA-256"):
            validated_release(release)

    def test_prereleases_are_not_published_as_stable(self):
        release = {"tag_name": "v1.8.3", "draft": False, "prerelease": True, "assets": []}
        with self.assertRaisesRegex(ValueError, "published"):
            validated_release(release)


if __name__ == "__main__":
    unittest.main()
