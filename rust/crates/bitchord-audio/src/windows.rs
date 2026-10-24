//! Windows native audio output via rodio/CPAL (WASAPI), isolated from
//! the Linux miniaudio backend until upstream's Windows bindings are fixed.
use rodio::{Decoder, OutputStream, Sink, Source};
use std::fs::File;
use std::io::BufReader;
use std::path::Path;
use std::time::Duration;

#[derive(Debug)]
pub struct PlaybackSnapshot {
    pub playing: bool,
    pub ended: bool,
    pub position_ms: u64,
    pub duration_ms: u64,
}

struct ActiveTrack {
    // Keep the playback stream alive as long as its sink exists.
    sink: Sink,
    _stream: OutputStream,
    duration: Duration,
}

#[derive(Default)]
pub struct LocalAudioPlayer {
    active: Option<ActiveTrack>,
}

impl LocalAudioPlayer {
    pub fn play(&mut self, path: &Path) -> Result<(), String> {
        // Verify and open the new track before stopping the old track.
        let file = File::open(path)
            .map_err(|error| format!("cannot open {}: {error}", path.display()))?;
        let decoder = Decoder::new(BufReader::new(file))
            .map_err(|error| format!("cannot decode {}: {error}", path.display()))?;
        let duration = decoder.total_duration().unwrap_or_default();
        let (stream, handle) = OutputStream::try_default()
            .map_err(|error| format!("cannot open audio output: {error}"))?;
        let sink = Sink::try_new(&handle)
            .map_err(|error| format!("cannot start audio output: {error}"))?;
        sink.append(decoder);
        sink.play();
        self.active = Some(ActiveTrack {
            sink,
            _stream: stream,
            duration,
        });
        Ok(())
    }

    pub fn pause(&mut self) -> Result<(), String> {
        if let Some(track) = &self.active {
            track.sink.pause();
        }
        Ok(())
    }

    pub fn resume(&mut self) -> Result<(), String> {
        if let Some(track) = &self.active {
            track.sink.play();
        }
        Ok(())
    }

    pub fn stop(&mut self) {
        self.active = None;
    }

    pub fn seek_ms(&mut self, position_ms: u64) -> Result<(), String> {
        if let Some(track) = &self.active {
            track.sink
                .try_seek(Duration::from_millis(position_ms))
                .map_err(|error| error.to_string())?;
        }
        Ok(())
    }

    pub fn set_volume(&self, volume: f32) -> Result<(), String> {
        if let Some(track) = &self.active {
            track.sink.set_volume(volume.clamp(0.0, 1.0));
        }
        Ok(())
    }

    pub fn snapshot(&self) -> PlaybackSnapshot {
        match &self.active {
            Some(track) => PlaybackSnapshot {
                playing: !track.sink.is_paused() && !track.sink.empty(),
                ended: track.sink.empty(),
                position_ms: track.sink.get_pos().as_millis().min(u128::from(u64::MAX)) as u64,
                duration_ms: track.duration.as_millis().min(u128::from(u64::MAX)) as u64,
            },
            None => PlaybackSnapshot {
                playing: false,
                ended: false,
                position_ms: 0,
                duration_ms: 0,
            },
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn missing_file_does_not_create_device() {
        let mut player = LocalAudioPlayer::default();
        assert!(!player.snapshot().playing);
        let missing = std::env::temp_dir().join("bitchord-missing-win-track-908198127.mp3");
        assert!(player.play(&missing).is_err());
        assert!(!player.snapshot().playing);
    }

    #[test]
    fn decodes_wav_without_audio_device() {
        let temp = tempfile::tempdir().unwrap();
        let path = temp.path().join("tone.wav");
        let samples = [0i16, 16_384, -16_384, 0];
        let mut wav = Vec::new();
        wav.extend_from_slice(b"RIFF");
        wav.extend_from_slice(&(36u32 + samples.len() as u32 * 2).to_le_bytes());
        wav.extend_from_slice(b"WAVEfmt ");
        wav.extend_from_slice(&16u32.to_le_bytes());
        wav.extend_from_slice(&1u16.to_le_bytes());
        wav.extend_from_slice(&1u16.to_le_bytes());
        wav.extend_from_slice(&44_100u32.to_le_bytes());
        wav.extend_from_slice(&(44_100u32 * 2).to_le_bytes());
        wav.extend_from_slice(&2u16.to_le_bytes());
        wav.extend_from_slice(&16u16.to_le_bytes());
        wav.extend_from_slice(b"data");
        wav.extend_from_slice(&(samples.len() as u32 * 2).to_le_bytes());
        for sample in samples {
            wav.extend_from_slice(&sample.to_le_bytes());
        }
        std::fs::write(&path, wav).unwrap();

        let file = File::open(&path).unwrap();
        let decoder = Decoder::new(BufReader::new(file)).unwrap();
        assert_eq!(decoder.count(), 4);
    }
}
