# Orbit Shield ProGuard Rules

# Keep Compose runtime
-keep class androidx.compose.** { *; }

# Keep game classes
-keep class com.orbitshield.game.** { *; }

# Keep data classes for proper copy() behavior
-keepclassmembers class com.orbitshield.game.game.GameState { *; }
-keepclassmembers class com.orbitshield.game.game.Asteroid { *; }
-keepclassmembers class com.orbitshield.game.game.Particle { *; }
-keepclassmembers class com.orbitshield.game.game.Star { *; }
-keepclassmembers class com.orbitshield.game.game.ShieldFragment { *; }
-keepclassmembers class com.orbitshield.game.game.ScorePopup { *; }
