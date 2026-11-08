# MHJ Player

Hybrid music + video player for Android (Media3 / ExoPlayer).

## Phase 1 (this branch)

- Media3 playback engine for music and video
- Foreground `MediaSessionService` with lock-screen / notification controls
- DataStore settings, Room queue + playlists skeleton, crash-safe position restore
- Modular architecture: `core/playback`, `core/data`, `core/ui`, `feature/music`, `feature/video`
- UI skeleton: Porcelain White + Midnight Gold themes, swipe left music→video (no mode button), top/bottom drawers, equalizer visualizer, raw MHJ logo

### Build

```bash
./gradlew :app:assembleDebug
```

SDK required: Android API 35, JDK 17+.

### Gestures

- Swipe left on music → video space
- Swipe down from top → library / settings drawer
- Swipe up from bottom → audio processing drawer
- Theme toggle inside the top drawer
