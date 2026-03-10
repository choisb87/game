# Getaway: Night Heist

Kotlin + Jetpack Compose로 만든 탑다운 스텔스 추격 안드로이드 게임입니다.

경찰 시야를 피해 보석을 훔치고, 탈출구까지 도달하세요. 매 레벨 절차적으로 생성되는 맵에서 경찰 AI와 두뇌싸움을 펼치는 긴장감 넘치는 게임입니다.

## 요구 사항

- Android Studio Hedgehog(2023.1.1) 이상
- JDK 17 이상
- Android SDK 34
- Kotlin 1.9.22 이상

## 실행 방법

```bash
# 프로젝트 폴더로 이동
cd games/android-game-2026-03-10

# local.properties 파일 준비
cp local.properties.example local.properties
# local.properties에서 sdk.dir 경로를 환경에 맞게 수정

# 디버그 빌드
./gradlew assembleDebug
```

또는 Android Studio에서 폴더를 열고 기기/에뮬레이터로 바로 실행하면 됩니다.

## 프로젝트 구조

```
├── app/
│   ├── build.gradle.kts          # 앱 레벨 빌드 설정
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/getaway/nightheist/
│       │   ├── MainActivity.kt    # 진입점, 화면 전환
│       │   └── game/
│       │       ├── GameState.kt   # 게임 상태 데이터 클래스
│       │       ├── GameMap.kt     # BSP 절차적 맵 생성
│       │       ├── GameLogic.kt   # 게임 로직, 경찰 AI, 충돌
│       │       ├── GameRenderer.kt# Canvas 렌더링, HUD, 미니맵
│       │       ├── GameScreen.kt  # 게임 화면 + 게임 루프
│       │       └── MenuScreen.kt  # 메인 메뉴 화면
│       └── res/values/
│           ├── strings.xml
│           ├── colors.xml
│           └── themes.xml
├── build.gradle.kts               # 프로젝트 레벨 빌드 설정
├── settings.gradle.kts
├── gradle.properties
└── local.properties.example
```

## 플레이 방법

1. 화면을 **드래그**하면 가상 조이스틱이 나타나 캐릭터가 이동합니다.
2. **노란 보석**을 모두 수집하면 **탈출구**(초록)가 열립니다.
3. 경찰의 **시야콘**(파란색/빨간색 삼각형)에 걸리지 않게 이동하세요.
4. **파란 타일**(은신처)에서 멈추면 자동으로 숨습니다 — 경찰이 볼 수 없습니다.
5. 경찰에게 잡히면 라이프가 줄어들고, 라이프가 0이 되면 게임 오버입니다.
6. 시간이 지나면 **증원 경찰**이 도착합니다 — 서두르세요!

## 점수 체계

- **보석 수집**: 100 × 현재 레벨
- **시간 보너스**: 남은 증원 타이머 × 10
- **스텔스 보너스**: 한 번도 발각되지 않으면 +500

## 경찰 AI 행동

| 상태 | 색상 | 행동 |
|------|------|------|
| 순찰 | 파랑 | 정해진 루트를 따라 이동 |
| 경계 | 노랑 | 플레이어 발견! 방향 전환 중 |
| 추격 | 빨강 | 직접 추격, 속도 증가 |
| 수색 | 주황 | 마지막 목격 위치에서 주변 탐색 |

## 릴리즈 APK 빌드

```bash
./gradlew assembleRelease
# 결과물: app/build/outputs/apk/release/app-release-unsigned.apk
```

서명된 릴리즈를 만들려면 `app/build.gradle.kts`에 signing 설정을 추가하면 됩니다.

## 기술 스택

- **언어**: Kotlin 1.9.22
- **UI**: Jetpack Compose (BOM 2024.01.00)
- **렌더링**: Compose Canvas API
- **맵 생성**: BSP 기반 절차적 생성
- **AI**: 4단계 상태 머신 (순찰 → 경계 → 추격 → 수색)
- **구조**: Single-activity + 불변 상태 관리
- **최소 SDK**: 24 (Android 7.0)
