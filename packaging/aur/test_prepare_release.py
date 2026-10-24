"""Offline tests for converting a prebuilt DEB into a pinned Arch source."""
import hashlib
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import prepare_release

BASE_RECIPE = Path(__file__).with_name("PKGBUILD").read_text(encoding="utf-8")


class PrepareReleaseTests(unittest.TestCase):
    def test_prebuilt_deb_is_reused_and_verified(self):
        with tempfile.TemporaryDirectory() as dirname:
            root = Path(dirname)
            recipe = root / "PKGBUILD"
            recipe.write_text(BASE_RECIPE, encoding="utf-8")
            file = root / "BitChord-1.8.9-linux-amd64.deb"
            file.write_bytes(b"test deb bytes with deterministic digest")
            expected_sha = hashlib.sha256(file.read_bytes()).hexdigest()

            with patch.object(prepare_release, "PKGBUILD", recipe):
                cached, sha = prepare_release.prepare("1.8.9", file)

            self.assertEqual(expected_sha, sha)
            self.assertEqual(file.read_bytes(), cached.read_bytes())
            self.assertEqual(root / "bitchord-desktop-revived-bin-1.8.9.deb", cached)
            updated = recipe.read_text(encoding="utf-8")
            self.assertIn("pkgver=1.8.9", updated)
            self.assertIn(f"sha256sums=('{expected_sha}')", updated)
            self.assertIn("pkgrel=1", updated)

    def test_rejects_mismatched_file_name(self):
        with tempfile.TemporaryDirectory() as dirname:
            file = Path(dirname) / "unrelated.deb"
            file.write_bytes(b"not the release")
            with self.assertRaisesRegex(ValueError, "Expected built Debian artifact"):
                prepare_release.prepare("1.8.9", file)

    def test_rejects_invalid_release_version(self):
        with self.assertRaisesRegex(ValueError, "MAJOR.MINOR.PATCH"):
            prepare_release.prepare("1.8.9;echo unsafe", Path("irrelevant.deb"))


if __name__ == "__main__":
    unittest.main()
