# 🔥 Flame Dash: 불꽃 질주

아래에서 끊임없이 올라오는 화염을 피해 최대한 높이 올라가는 원터치 대시 아케이드 게임.

## 게임 방법

- **왼쪽 탭**: 왼쪽 대시 (대각선 위로 이동 + 수평 가속)
- **오른쪽 탭**: 오른쪽 대시 (대각선 위로 이동 + 수평 가속)
- 플랫폼에 착지하면 자동으로 점프
- 화면 좌우 끝을 넘으면 반대편으로 나옴 (화면 랩)

## 플랫폼 종류

| 유형 | 색상 | 효과 |
|------|------|------|
| 일반 | 파란색 | 표준 점프 |
| 바운시 | 초록색 | 슈퍼 점프 |
| 크럼블링 | 황갈색 | 착지 후 0.5초 뒤 파괴 |
| 아이스 | 하늘색 | 미끄러짐 (관성 유지) |

## 보석 수집

4종류의 보석을 수집하여 점수를 획득합니다. 연속 수집 시 콤보 보너스!

## 프로젝트 구조

```
android-game-2026-03-13/
├── app/src/main/java/com/flamedash/runner/
│   ├── MainActivity.kt          # 앱 진입점
│   └── game/
│       ├── GameState.kt         # 불변 게임 상태
│       ├── GameLogic.kt         # 순수 함수 게임 로직
│       ├── GameRenderer.kt      # Canvas 렌더링
│       ├── GameScreen.kt        # Compose 게임 화면 + 게임 루프
│       └── MenuScreen.kt        # 메뉴 UI
├── app/src/main/res/
│   └── values/                  # 문자열, 색상, 테마
├── build.gradle.kts             # 프로젝트 빌드
├── app/build.gradle.kts         # 앱 빌드
├── settings.gradle.kts          # Gradle 설정
└── gradle.properties            # Gradle 속성
```

## 빌드 방법

### 사전 준비

1. Android Studio 설치 (Hedgehog 이상)
2. `local.properties.example`을 `local.properties`로 복사 후 SDK 경로 설정

### 빌드 및 실행

```bash
# 디버그 APK 빌드
./gradlew assembleDebug

# 디버그 APK 위치
app/build/outputs/apk/debug/app-debug.apk

# 릴리즈 빌드
./gradlew assembleRelease
```

### 에뮬레이터/기기에 설치

```bash
./gradlew installDebug
```

## 기술 스택

- **언어**: Kotlin 1.9.22
- **UI**: Jetpack Compose (BOM 2024.01.00)
- **렌더링**: Compose Canvas API (60fps 게임 루프)
- **아키텍처**: 불변 상태 + 순수 함수 업데이트
- **최소 SDK**: API 24 (Android 7.0)
