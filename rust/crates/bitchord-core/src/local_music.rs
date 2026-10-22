//! Filesystem music discovery matching Kotlin `DesktopLocalMusic`'s default roots,
//! depth, extensions, title parsing, and optional non-music filter.

use std::collections::HashSet;
use std::fs;
use std::path::{Path, PathBuf};
use std::time::SystemTime;

const EXTENSIONS: &[&str] = &["aac", "flac", "m4a", "mp3", "ogg", "wav", "webm"];
const NON_MUSIC_MARKERS: &[&str] = &[
    "/ringtones/",
    "/notifications/",
    "/alarms/",
    "/ui/",
    "/recordings/",
    "/voice recorder/",
    "voice note",
    "voicenote",
    "recording",
    "ringtone",
    "notification",
    "alarm",
];

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct LocalSong {
    pub id: String,
    pub path: PathBuf,
    pub title: String,
    pub artist: String,
    pub codec: String,
    pub date_added: Option<SystemTime>,
    pub date_modified: Option<SystemTime>,
}

pub fn default_roots() -> Vec<PathBuf> {
    let Some(home) = directories::UserDirs::new().map(|dirs| dirs.home_dir().to_path_buf()) else {
        return Vec::new();
    };
    vec![home.join("Music"), home.join("Downloads")]
}

pub fn scan(roots: &[PathBuf], filter_non_music: bool) -> Vec<LocalSong> {
    let mut songs = Vec::new();
    let mut seen = HashSet::new();
    for root in roots {
        if root.is_dir() {
            scan_directory(root, 0, filter_non_music, &mut seen, &mut songs);
        }
    }
    songs.sort_by_key(|song| song.title.to_lowercase());
    songs
}

fn scan_directory(
    dir: &Path,
    depth: usize,
    filter_non_music: bool,
    seen: &mut HashSet<PathBuf>,
    songs: &mut Vec<LocalSong>,
) {
    let Ok(entries) = fs::read_dir(dir) else {
        return;
    };
    for entry in entries.flatten() {
        let path = entry.path();
        let Ok(file_type) = entry.file_type() else {
            continue;
        };
        if file_type.is_dir() {
            // Files.walk(root, 4) includes files through the fourth child directory.
            if depth < 3 {
                scan_directory(&path, depth + 1, filter_non_music, seen, songs);
            }
        } else if file_type.is_file() && supported(&path) {
            let Ok(absolute) = path.canonicalize() else {
                continue;
            };
            if seen.insert(absolute.clone()) && (!filter_non_music || is_music(&absolute)) {
                songs.push(to_song(absolute));
            }
        }
    }
}

fn supported(path: &Path) -> bool {
    path.extension()
        .and_then(|part| part.to_str())
        .is_some_and(|ext| EXTENSIONS.contains(&ext.to_ascii_lowercase().as_str()))
}

pub fn is_music(path: &Path) -> bool {
    if path
        .extension()
        .and_then(|part| part.to_str())
        .is_some_and(|ext| ext.eq_ignore_ascii_case("wav"))
    {
        return false;
    }
    if fs::metadata(path).map_or(true, |metadata| metadata.len() < 240 * 1024) {
        return false;
    }
    let haystack = path.to_string_lossy().to_lowercase().replace('\\', "/");
    !NON_MUSIC_MARKERS
        .iter()
        .any(|marker| haystack.contains(marker))
}

fn to_song(path: PathBuf) -> LocalSong {
    let name = path
        .file_stem()
        .and_then(|part| part.to_str())
        .unwrap_or_default();
    let (title, artist) = match name.split_once(" - ") {
        Some((artist, title)) if !artist.trim().is_empty() && !title.trim().is_empty() => {
            (title.trim().to_owned(), artist.trim().to_owned())
        }
        _ => (name.to_owned(), "On This Computer".to_owned()),
    };
    let ext = path
        .extension()
        .and_then(|part| part.to_str())
        .unwrap_or_default()
        .to_ascii_lowercase();
    let codec = match ext.as_str() {
        "aac" | "m4a" => "AAC".to_owned(),
        "webm" => "OPUS".to_owned(),
        _ => ext.to_ascii_uppercase(),
    };
    let (date_added, date_modified) = fs::metadata(&path)
        .map(|meta| {
            (
                meta.created().ok().or_else(|| meta.modified().ok()),
                meta.modified().ok(),
            )
        })
        .unwrap_or((None, None));
    let id = format!("local:{}", path.display());
    LocalSong {
        id,
        path,
        title,
        artist,
        codec,
        date_added,
        date_modified,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn discovers_supported_files_with_kotlin_naming_and_depth() {
        let temp = tempfile::tempdir().unwrap();
        let root = temp.path();
        fs::write(root.join("Artist - Track.MP3"), b"audio").unwrap();
        fs::write(root.join("Unlabeled.flac"), b"audio").unwrap();
        fs::write(root.join("ignore.txt"), b"audio").unwrap();
        fs::create_dir(root.join("child")).unwrap();
        fs::write(root.join("child").join("other.ogg"), b"audio").unwrap();
        let songs = scan(&[root.to_path_buf(), root.to_path_buf()], false);
        assert_eq!(songs.len(), 3);
        let track = songs.iter().find(|song| song.title == "Track").unwrap();
        assert_eq!(track.artist, "Artist");
        assert_eq!(track.codec, "MP3");
        assert_eq!(songs[0].artist, "On This Computer");
        assert!(songs.iter().all(|song| song.id.starts_with("local:")));
    }

    #[test]
    fn optional_filter_excludes_clips_and_ringtones() {
        let temp = tempfile::tempdir().unwrap();
        let root = temp.path();
        fs::write(root.join("ordinary.mp3"), vec![0; 250 * 1024]).unwrap();
        fs::write(root.join("small.mp3"), b"short").unwrap();
        fs::write(root.join("track.wav"), vec![0; 250 * 1024]).unwrap();
        fs::write(root.join("ringtone.mp3"), vec![0; 250 * 1024]).unwrap();
        let songs = scan(&[root.to_path_buf()], true);
        assert_eq!(songs.len(), 1);
        assert_eq!(songs[0].title, "ordinary");
    }
}
