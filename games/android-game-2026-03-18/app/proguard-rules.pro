# Shadow Dungeon ProGuard Rules
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }
