# Getaway: Night Heist

Kotlin + Jetpack Compose로 만든 탑다운 스텔스 추격 안드로이드 게임입니다.

경찰 시야를 피해 보석을 훔치고, 파워업을 활용하고, 콤보를 쌓아 탈출구까지 도달하세요. 매 레벨 절차적으로 생성되는 맵에서 경찰 AI와 두뇌싸움을 펼치는 긴장감 넘치는 프리미엄 게임입니다.

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
JAVA_HOME=/path/to/jdk-17 ./gradlew assembleDebug
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
│       │       ├── GameState.kt   # 게임 상태 + 파워업/콤보 데이터
│       │       ├── GameMap.kt     # BSP 절차적 맵 생성
│       │       ├── GameLogic.kt   # 게임 로직, AI, 파워업, 콤보 시스템
│       │       ├── GameRenderer.kt# Canvas 렌더링, 텍스트 HUD, 연출
│       │       ├── GameScreen.kt  # 게임 화면 + 루프 + 통계 저장
│       │       └── MenuScreen.kt  # 메인 메뉴 (브랜딩, 통계, 조작법)
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

1. **레벨 인트로**: 카메라가 탈출구에서 시작점까지 패닝하며 맵을 보여줌 (탭으로 스킵 가능)
2. **드래그**: 화면을 드래그하면 가상 조이스틱 등장
   - **짧게 드래그** = **스닉 모드** (느리지만 조용)
   - **길게 드래그** = **달리기** (빠르지만 눈에 띔)
3. **보석 수집**: 노란 보석을 모두 모으면 탈출구(초록)가 열림
4. **콤보 시스템**: 4초 안에 연속 수집하면 콤보 배율 상승 (x2, x3...)
5. **파워업 활용**: 맵에 있는 아이템을 수집하면 특수 능력 발동
6. **은신**: 파란 타일에서 멈추면 자동으로 숨음 — 경찰이 볼 수 없음
7. **탈출**: 모든 보석 수집 후 탈출구에 도달하면 레벨 클리어

## 파워업 시스템

| 파워업 | 색상 | 효과 | 지속시간 |
|--------|------|------|----------|
| SMOKE | 보라 | 근처 경찰 기절 + 시야 무효화 | 3초 |
| SPEED | 시안 | 이동속도 1.5배 증가 | 5초 |
| GHOST | 연보라 | 완전 투명 (경찰에게 안 보임) | 3초 |

## 점수 체계

- **보석 수집**: 100 × 레벨 × 콤보배율
- **시간 보너스**: 남은 증원 타이머 × 10
- **스텔스 보너스**: 한 번도 발각되지 않으면 +500
- **콤보 보너스**: 최대 콤보 × 200

## 경찰 AI 행동

| 상태 | 색상 | 행동 |
|------|------|------|
| 순찰 | 파랑 | 정해진 루트를 따라 이동 |
| 경계 | 노랑 | 플레이어 발견! 방향 전환 중 |
| 추격 | 빨강 | 직접 추격, 속도 증가 |
| 수색 | 주황 | 마지막 목격 위치에서 주변 탐색 |
| 기절 | 보라 | SMOKE 효과로 무력화 |

## 릴리즈 APK 빌드

```bash
JAVA_HOME=/path/to/jdk-17 ./gradlew assembleRelease
```

## 기술 스택

- **언어**: Kotlin 1.9.22
- **UI**: Jetpack Compose (BOM 2024.01.00)
- **렌더링**: Compose Canvas API + Android Native Canvas (텍스트)
- **맵 생성**: BSP 기반 절차적 생성
- **AI**: 4단계 상태 머신 (순찰 → 경계 → 추격 → 수색)
- **구조**: Single-activity + 불변 상태 관리
- **최소 SDK**: 24 (Android 7.0)
