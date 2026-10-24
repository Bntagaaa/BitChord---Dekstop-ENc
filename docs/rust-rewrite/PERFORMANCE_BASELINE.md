# Performance baseline protocol

No comparable Kotlin-versus-Rust numbers are available yet. The current execution environment has no desktop display or real audio device, and the Rust foundation has no playback. A source audit cannot establish reduced RAM/CPU. Do not use the Rust shell's idle memory as a playback comparison.

Known configuration facts: Compose distributions set `-Xmx512m` in `desktopApp/build.gradle.kts`; the separate Windows portable batch launcher is assembled by the same file and must be measured independently. `DesktopTrackAnalyzer.kt` may decode a whole track for Automix, `DesktopArtwork.kt` budgets decoded artwork, and `DesktopCanvasVideo.kt` creates image frames. These are possible contributors, not measured shares of process memory.

## Comparable measurement plan

Record the exact Git SHA, build type, OS/version, CPU, RAM, GPU/driver, display resolution/scale, audio device, account/source, track IDs or reproducible local fixtures, enabled Automix/Canvas/blur, and tool versions. Use the same machine and workload for Kotlin and Rust:

| Scenario | Measure |
| --- | --- |
| Cold launch, 10 runs | Time to interactive window and first content; median/p95 |
| Idle, 10 minutes | Private bytes/RSS, heap/native split where available, CPU, GPU memory |
| Playback, 30 minutes | Private bytes/RSS, CPU, underruns, frame times, audio quality |
| 100 track switches | Peak/steady memory, switching latency, decoder/resource cleanup |
| Canvas + lyrics + resizing | Frame p50/p95/p99, dropped frames, GPU memory, input latency |
| 8-hour playback | Memory growth slope, stalls, crashes, recovery |

On Windows use Performance Monitor/Process Explorer plus GPU tooling; on Linux use `/proc/<pid>/smaps_rollup`, `pidstat`, and an appropriate frame profiler. Distinguish virtual size, resident memory, private bytes, and GPU memory. Include raw logs and a screenshot/notes for visual checks. Compare release builds, not a Kotlin release against a Rust debug binary.

| Baseline | Result |
| --- | --- |
| Kotlin idle/playback | NOT RUN — no GUI/audio workload in this environment |
| Rust foundation idle | NOT RUN — no GUI session; not comparable to a full player |
| Rust playback | NOT IMPLEMENTED |
