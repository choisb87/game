# Gravity Pulse

A one-touch gravity-flipping arcade game built with Kotlin and Jetpack Compose.

Tap to reverse gravity. Thread through neon barriers. Chase your high score.

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17+
- Android SDK 34
- Kotlin 1.9.22+

## Setup

```bash
# Clone and open in Android Studio
cd games/android-game-2026-03-10

# Copy and configure local properties
cp local.properties.example local.properties
# Edit local.properties to set your sdk.dir path

# Build via command line (optional)
./gradlew assembleDebug
```

Or simply open the project folder in Android Studio and run on a device/emulator.

## Project Structure

```
├── app/
│   ├── build.gradle.kts          # App-level build config
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/gravitypulse/game/
│       │   ├── MainActivity.kt    # Entry point, screen navigation, high score persistence
│       │   └── game/
│       │       ├── GameState.kt   # Core game state, physics, collision detection
│       │       ├── GameLoop.kt    # Frame-synced update loop via LaunchedEffect
│       │       ├── GameRenderer.kt# Canvas-based rendering (grid, obstacles, particles)
│       │       ├── GameScreen.kt  # Composable game screen with HUD overlay
│       │       ├── GameInput.kt   # Touch input handling
│       │       └── MenuScreen.kt  # Main menu with animated visuals
│       └── res/values/
│           ├── strings.xml
│           ├── colors.xml
│           └── themes.xml
├── build.gradle.kts               # Project-level build config
├── settings.gradle.kts
├── gradle.properties
└── local.properties.example
```

## How to Play

1. **Tap** anywhere on the screen to flip gravity
2. Your orb falls up or down based on the current gravity direction
3. **Thread through gaps** in the neon barriers scrolling upward
4. Each barrier passed = +1 score, consecutive passes build combos
5. Hitting a barrier ends the run — your best score is saved

## Controls

- **Single tap**: Flip gravity direction (up ↔ down)

That's it. One touch, infinite depth.

## Building a Release APK

```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release-unsigned.apk
```

For a signed release, configure signing in `app/build.gradle.kts`.

## Tech Stack

- **Language**: Kotlin 1.9.22
- **UI**: Jetpack Compose (BOM 2024.01.00)
- **Rendering**: Compose Canvas API
- **Architecture**: Single-activity, composable-based state management
- **Min SDK**: 24 (Android 7.0) — covers 97%+ of active devices
