#!/usr/bin/env bash
set -euo pipefail

REPO_DIR="/home/sb/game"
DATE_KST="$(TZ=Asia/Seoul date +%F)"
SLUG="android-game-${DATE_KST}"
TARGET_DIR="$REPO_DIR/games/$SLUG"

cd "$REPO_DIR"
mkdir -p "$TARGET_DIR"

if ! command -v claude >/dev/null 2>&1; then
  echo "claude CLI not found" >&2
  exit 1
fi

# 1) Generate Kotlin-native Android game project (NOT web game)
claude --permission-mode bypassPermissions --print "
Create a native Android game project in Kotlin (Jetpack Compose) under: $TARGET_DIR

MANDATORY:
- Native Android app, Kotlin-based. No HTML/CSS/JS web game.
- Build files included: settings.gradle.kts, build.gradle.kts (project/app), gradle.properties, local.properties.example
- App structure included:
  - app/src/main/AndroidManifest.xml
  - app/src/main/java/.../MainActivity.kt
  - app/src/main/java/.../game/* (game loop/state/input)
  - app/src/main/res/values/*
- Gameplay must be actually playable and fun-first (first 30s hook).
- Monetization style: premium paid app quality (no ad bait).
- Visual/resource quality must avoid bare-prototype feel:
  - use better-looking game UI, icons, background motifs, and resource polish
  - when useful, create generated image assets or use curated reference-inspired assets
  - the result should look closer to a commercial mobile game than a debug demo
- Include docs:
  1) README.md (Korean, with run/build instructions)
  2) PLAN.md (fun loop + retention + paid-app rationale)
  3) MARKET_BENCHMARK.md (top chart references and what to adopt/avoid)
  4) metadata.json (title/genre/core_loop/session_length)

Also include a lightweight CI check script at scripts/check.sh that validates project structure and Kotlin file presence.
" >/tmp/claude_game_gen_${DATE_KST}.log

# 2) Validation pass (Claude)
claude --permission-mode bypassPermissions --print "
Validate native-android quality for $TARGET_DIR.
Write VALIDATION.md with:
- Native Android checks (Kotlin/Manifest/Gradle structure)
- Gameplay/fun checks
- Market-fit checks (from MARKET_BENCHMARK.md)
- Visual/resource quality checks (does it look commercially presentable, or too placeholder/simple?)
- Bug list
- Final verdict PASS/FAIL
If FAIL, fix and make it PASS.
" >/tmp/claude_game_verify_${DATE_KST}.log

# 3) Build verification (hard gate before git push)
export JAVA_HOME=${JAVA_HOME:-/home/sb/.local/jdk/jdk-17}
export ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT:-/home/sb/.local/android-sdk}
export PATH="$JAVA_HOME/bin:/home/sb/.local/gradle/gradle-8.5/bin:$PATH"

cat > "$TARGET_DIR/local.properties" <<EOF
sdk.dir=/home/sb/.local/android-sdk
EOF

cd "$TARGET_DIR"
if ! gradle assembleRelease bundleRelease >/tmp/android_game_build_${DATE_KST}.log 2>&1; then
  echo "BUILD FAILED for $SLUG"
  tail -n 80 /tmp/android_game_build_${DATE_KST}.log || true
  exit 1
fi

# 4) Git commit/push only after successful build outputs exist
cd "$REPO_DIR"
if [ -d "games/daily/$SLUG" ]; then
  git rm -r "games/daily/$SLUG" || true
fi

# stage current target + deletions
git add "games/$SLUG" || true
git add -u

if git diff --cached --quiet; then
  echo "No changes to commit for $SLUG"
  exit 0
fi

git commit -m "feat(game): native kotlin android game $DATE_KST (validated + built)"
git push origin main

echo "DONE: $SLUG pushed with build-verified APK/AAB"
