# Shadow Dungeon (그림자 던전)

턴 기반 로그라이크 던전 크롤러. 절차적으로 생성되는 던전을 탐험하며 몬스터와 싸우고, 아이템을 수집하고, 더 깊은 곳으로 내려가세요.

## 게임플레이

- **스와이프**로 4방향 이동 (턴 기반)
- 적에게 걸어가면 **자동 공격**
- 아이템을 밟으면 **자동 획득**
- 계단을 찾아 **다음 층**으로 내려가기
- **퍼마데스** — 죽으면 처음부터 다시

## 조작법

| 조작 | 동작 |
|------|------|
| ↑ 스와이프 | 위로 이동 |
| ↓ 스와이프 | 아래로 이동 |
| ← 스와이프 | 왼쪽 이동 |
| → 스와이프 | 오른쪽 이동 |

## 게임 요소

### 적
- 🐀 쥐, 💧 슬라임 (초반)
- 💀 스켈레톤, 👻 유령 (중반)
- 👹 오크, 🗿 골렘 (후반)
- 😈 악마, 🐉 드래곤 (심층)

### 아이템
- ♥ 포션 (HP 회복)
- ⚔ 무기 (공격력 증가)
- 🛡 방패 (방어력 증가)
- ◆ 금화 (점수)
- ✦ 두루마리 (특수 효과)

## 빌드 방법

### 사전 요구사항
- Android Studio Hedgehog (2023.1.1) 이상
- JDK 17
- Android SDK 34

### 빌드 실행
```bash
# local.properties 설정
cp local.properties.example local.properties
# sdk.dir 경로를 자신의 Android SDK 경로로 수정

# 디버그 빌드
./gradlew assembleDebug

# 릴리스 빌드
./gradlew assembleRelease
```

### APK 위치
- 디버그: `app/build/outputs/apk/debug/app-debug.apk`
- 릴리스: `app/build/outputs/apk/release/app-release.apk`

## 프로젝트 구조

```
android-game-2026-03-18/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/shadowdungeon/game/
│   │   ├── MainActivity.kt          # 앱 진입점
│   │   └── game/
│   │       ├── GameState.kt          # 데이터 클래스 정의
│   │       ├── GameLogic.kt          # 게임 로직 (전투, AI, 턴)
│   │       ├── GameRenderer.kt       # Canvas 렌더링
│   │       ├── GameScreen.kt         # Compose 게임 화면 + 루프
│   │       ├── MenuScreen.kt         # 메뉴 UI
│   │       └── DungeonGenerator.kt   # 절차적 던전 생성
│   └── res/values/
│       ├── strings.xml
│       ├── colors.xml
│       └── themes.xml
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── local.properties.example
```

## 기술 스택

- Kotlin 1.9.22
- Jetpack Compose (BOM 2024.01.00)
- Compose Canvas API (렌더링)
- 외부 라이브러리 없음
