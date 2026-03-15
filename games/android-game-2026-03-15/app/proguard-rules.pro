# Keep Compose runtime
-keep class androidx.compose.** { *; }

# Keep game classes
-keep class com.chainreactor.game.** { *; }

# Keep data classes
-keepclassmembers class com.chainreactor.game.game.GameState { *; }
-keepclassmembers class com.chainreactor.game.game.Orb { *; }
-keepclassmembers class com.chainreactor.game.game.Explosion { *; }
-keepclassmembers class com.chainreactor.game.game.Particle { *; }
-keepclassmembers class com.chainreactor.game.game.Star { *; }
-keepclassmembers class com.chainreactor.game.game.ScorePopup { *; }
