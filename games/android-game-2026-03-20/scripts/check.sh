#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ERRORS=0

check() {
    local path="$1"
    local desc="$2"
    if [ -e "$PROJECT_DIR/$path" ]; then
        echo "  ✓ $desc"
    else
        echo "  ✗ $desc ($path)"
        ERRORS=$((ERRORS + 1))
    fi
}

echo "═══ Neon Breaker — 프로젝트 구조 검증 ═══"
echo ""

echo "▸ 빌드 파일"
check "settings.gradle.kts"       "settings.gradle.kts"
check "build.gradle.kts"          "프로젝트 build.gradle.kts"
check "app/build.gradle.kts"      "앱 build.gradle.kts"
check "gradle.properties"         "gradle.properties"
check "local.properties.example"  "local.properties.example"

echo ""
echo "▸ 앱 구조"
check "app/src/main/AndroidManifest.xml"                          "AndroidManifest.xml"
check "app/src/main/java/com/neonbreaker/game/MainActivity.kt"    "MainActivity.kt"

echo ""
echo "▸ 게임 코드"
check "app/src/main/java/com/neonbreaker/game/game/Models.kt"         "Models.kt"
check "app/src/main/java/com/neonbreaker/game/game/GameState.kt"      "GameState.kt"
check "app/src/main/java/com/neonbreaker/game/game/GameEngine.kt"     "GameEngine.kt"
check "app/src/main/java/com/neonbreaker/game/game/GameRenderer.kt"   "GameRenderer.kt"
check "app/src/main/java/com/neonbreaker/game/game/GameScreen.kt"     "GameScreen.kt"
check "app/src/main/java/com/neonbreaker/game/game/LevelGenerator.kt" "LevelGenerator.kt"

echo ""
echo "▸ 리소스"
check "app/src/main/res/values/strings.xml"  "strings.xml"
check "app/src/main/res/values/colors.xml"   "colors.xml"
check "app/src/main/res/values/themes.xml"   "themes.xml"

echo ""
echo "▸ 문서"
check "README.md"            "README.md"
check "PLAN.md"              "PLAN.md"
check "MARKET_BENCHMARK.md"  "MARKET_BENCHMARK.md"
check "metadata.json"        "metadata.json"

echo ""
echo "▸ Kotlin 파일 수"
KT_COUNT=$(find "$PROJECT_DIR/app/src" -name "*.kt" 2>/dev/null | wc -l)
echo "  Kotlin 파일: ${KT_COUNT}개"
if [ "$KT_COUNT" -lt 5 ]; then
    echo "  ✗ Kotlin 파일이 5개 미만입니다"
    ERRORS=$((ERRORS + 1))
else
    echo "  ✓ 충분한 Kotlin 파일"
fi

echo ""
if [ "$ERRORS" -eq 0 ]; then
    echo "═══ 검증 완료: 모든 항목 통과 ✓ ═══"
    exit 0
else
    echo "═══ 검증 실패: ${ERRORS}개 오류 발견 ═══"
    exit 1
fi
