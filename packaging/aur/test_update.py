"""Offline tests for the safe AUR release updater."""
from pathlib import Path
import unittest

from update import updated_pkgbuild, validated_release

RECIPE = Path(__file__).with_name("PKGBUILD").read_text(encoding="utf-8")
NEW_HASH = "a" * 64


class AurUpdaterTests(unittest.TestCase):
    def test_new_version_updates_digest_and_resets_pkgrel(self):
        changed = updated_pkgbuild(RECIPE, "1.8.3", NEW_HASH)
        self.assertIn("pkgver=1.8.3", changed)
        self.assertIn("pkgrel=1", changed)
        self.assertIn(f"sha256sums=('{NEW_HASH}')", changed)
        self.assertIn("BitChord-${pkgver}-linux-x86_64.AppImage", changed)

    def test_same_release_is_idempotent(self):
        self.assertEqual(
            RECIPE,
            updated_pkgbuild(
                RECIPE, "1.8.2", "07f7069323d5bcdd0397616be31a1bd104a6d0af1fac9c56942ad7b05c3a7b8c"
            ),
        )

    def test_changed_asset_without_version_bump_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "asset changed"):
            updated_pkgbuild(RECIPE, "1.8.2", NEW_HASH)

    def test_older_release_cannot_downgrade_recipe(self):
        self.assertEqual(RECIPE, updated_pkgbuild(RECIPE, "1.8.1", NEW_HASH))

    def test_release_digest_must_be_explicit_and_valid(self):
        release = {
            "tag_name": "v1.8.3",
            "draft": False,
            "prerelease": False,
            "assets": [{"name": "BitChord-1.8.3-linux-x86_64.AppImage", "digest": "sha256:" + NEW_HASH}],
        }
        self.assertEqual(("1.8.3", NEW_HASH), validated_release(release))
        release["assets"][0]["digest"] = ""
        with self.assertRaisesRegex(ValueError, "SHA-256"):
            validated_release(release)

    def test_prereleases_are_not_published_as_stable(self):
        release = {"tag_name": "v1.8.3", "draft": False, "prerelease": True, "assets": []}
        with self.assertRaisesRegex(ValueError, "published"):
            validated_release(release)


if __name__ == "__main__":
    unittest.main()
