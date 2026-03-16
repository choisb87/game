# Gravity Well ProGuard Rules

# Jetpack Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Keep data classes used in game state
-keep class com.gravitywell.game.game.** { *; }
