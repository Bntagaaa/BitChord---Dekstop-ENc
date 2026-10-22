//! Cross-platform local audio adapter. Miniaudio is retained on Linux;
//! Windows uses rodio/CPAL while the miniaudio MSVC binding bug remains.

#[cfg(target_os = "windows")]
mod windows;
#[cfg(target_os = "windows")]
pub use windows::{LocalAudioPlayer, PlaybackSnapshot};

#[cfg(not(target_os = "windows"))]
mod miniaudio_backend {
//! First native output path: local MP3/FLAC/WAV/Ogg Vorbis through miniaudio's built-in
//! decoder and Windows WASAPI / Linux desktop output. Other Kotlin formats,
//! networking, decode-ahead, transitions, and device selection remain open.

use miniaudio::{Decoder, DecoderConfig, Device, DeviceConfig, DeviceType, Format};
use std::path::Path;
use std::sync::{
    Arc, Mutex,
    atomic::{AtomicBool, AtomicU64, Ordering},
};

const RATE: u32 = 44_100;

#[derive(Debug)]
pub struct PlaybackSnapshot {
    pub playing: bool,
    pub ended: bool,
    pub position_ms: u64,
    pub duration_ms: u64,
}

struct ActiveTrack {
    device: Device,
    decoder: Arc<Mutex<Decoder>>,
    position_frames: Arc<AtomicU64>,
    duration_frames: u64,
    ended: Arc<AtomicBool>,
}

#[derive(Default)]
pub struct LocalAudioPlayer {
    active: Option<ActiveTrack>,
}

impl LocalAudioPlayer {
    pub fn play(&mut self, path: &Path) -> Result<(), String> {
        // Open and validate the next track before replacing an audible one.
        let config = DecoderConfig::new(Format::F32, 2, RATE);
        let mut decoder = Decoder::from_file(path, Some(&config))
            .map_err(|error| format!("cannot decode {}: {error}", path.display()))?;
        let duration_frames = decoder.length_in_pcm_frames();
        let decoder = Arc::new(Mutex::new(decoder));
        let position_frames = Arc::new(AtomicU64::new(0));
        let ended = Arc::new(AtomicBool::new(false));
        let callback_decoder = Arc::clone(&decoder);
        let callback_position = Arc::clone(&position_frames);
        let callback_ended = Arc::clone(&ended);
        let mut output_config = DeviceConfig::new(DeviceType::Playback);
        output_config.set_sample_rate(RATE);
        output_config.playback_mut().set_format(Format::F32);
        output_config.playback_mut().set_channels(2);
        output_config.set_data_callback(move |_device, output, _input| {
            let frames = callback_decoder
                .lock()
                .map_or(0, |mut decoder| decoder.read_pcm_frames(output));
            callback_position.fetch_add(frames, Ordering::Relaxed);
            if frames == 0 {
                callback_ended.store(true, Ordering::Release);
            }
        });
        let device = Device::new(None, &output_config)
            .map_err(|error| format!("cannot open audio output: {error}"))?;
        device
            .start()
            .map_err(|error| format!("cannot start audio output: {error}"))?;
        self.active = Some(ActiveTrack {
            device,
            decoder,
            position_frames,
            duration_frames,
            ended,
        });
        Ok(())
    }

    pub fn pause(&mut self) -> Result<(), String> {
        if let Some(track) = &self.active {
            track.device.stop().map_err(|error| error.to_string())?;
        }
        Ok(())
    }

    pub fn resume(&mut self) -> Result<(), String> {
        if let Some(track) = &self.active {
            track.device.start().map_err(|error| error.to_string())?;
        }
        Ok(())
    }

    pub fn stop(&mut self) {
        self.active = None;
    }

    pub fn seek_ms(&mut self, position_ms: u64) -> Result<(), String> {
        if let Some(track) = &self.active {
            let frame = position_ms.saturating_mul(u64::from(RATE)) / 1000;
            let mut decoder = track.decoder.lock().map_err(|error| error.to_string())?;
            decoder
                .seek_to_pcm_frame(frame)
                .map_err(|error| error.to_string())?;
            track.position_frames.store(frame, Ordering::Relaxed);
            track.ended.store(false, Ordering::Release);
        }
        Ok(())
    }

    pub fn set_volume(&self, volume: f32) -> Result<(), String> {
        if let Some(track) = &self.active {
            track
                .device
                .set_master_volume(volume.clamp(0.0, 1.0))
                .map_err(|error| error.to_string())?;
        }
        Ok(())
    }

    pub fn snapshot(&self) -> PlaybackSnapshot {
        match &self.active {
            Some(track) => PlaybackSnapshot {
                playing: track.device.is_started() && !track.ended.load(Ordering::Acquire),
                ended: track.ended.load(Ordering::Acquire),
                position_ms: track.position_frames.load(Ordering::Relaxed) * 1000 / u64::from(RATE),
                duration_ms: track.duration_frames * 1000 / u64::from(RATE),
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
    use miniaudio::FramesMut;

    #[test]
    fn player_starts_idle_and_missing_file_does_not_create_device() {
        let mut player = LocalAudioPlayer::default();
        let missing = std::env::temp_dir().join("bitchord-no-such-track-908198127.mp3");
        assert!(player.play(&missing).is_err());
        let snapshot = player.snapshot();
        assert!(!snapshot.playing);
        assert_eq!(snapshot.position_ms, 0);
    }

    #[test]
    fn decodes_a_real_wav_without_an_audio_device() {
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
        wav.extend_from_slice(&RATE.to_le_bytes());
        wav.extend_from_slice(&(RATE * 2).to_le_bytes());
        wav.extend_from_slice(&2u16.to_le_bytes());
        wav.extend_from_slice(&16u16.to_le_bytes());
        wav.extend_from_slice(b"data");
        wav.extend_from_slice(&(samples.len() as u32 * 2).to_le_bytes());
        for sample in samples {
            wav.extend_from_slice(&sample.to_le_bytes());
        }
        std::fs::write(&path, wav).unwrap();

        let config = DecoderConfig::new(Format::F32, 2, RATE);
        let mut decoder = Decoder::from_file(&path, Some(&config)).unwrap();
        let mut output = [0f32; 8];
        let mut frames = FramesMut::wrap(&mut output, Format::F32, 2);
        assert_eq!(decoder.read_pcm_frames(&mut frames), 4);
        assert!(output[2] > 0.49 && output[2] < 0.51);
        assert!(output[4] < -0.49 && output[4] > -0.51);
    }
}

}

#[cfg(not(target_os = "windows"))]
pub use miniaudio_backend::{LocalAudioPlayer, PlaybackSnapshot};
