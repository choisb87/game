# Sky Stacker

하늘 높이 블록을 쌓아 올리는 타이밍 기반 아케이드 게임.

## 게임 방법

- 블록이 좌우로 움직입니다
- 타이밍에 맞춰 **화면을 탭**하면 블록이 아래 블록 위에 놓입니다
- 어긋난 부분은 잘려나가 블록이 점점 좁아집니다
- **정확히 맞추면 (PERFECT)** 콤보가 쌓이고 추가 점수를 얻습니다
- 블록이 완전히 벗어나면 게임 오버

## 핵심 특징

- 원탭 조작의 직관적 게임플레이
- PERFECT 콤보 시스템 (골든 파티클 이펙트)
- 점진적 속도 증가로 텐션 상승
- 밤하늘 배경 + 반짝이는 별 연출
- 60fps 부드러운 렌더링 (Jetpack Compose Canvas)

## 빌드 및 실행

### 요구사항

- Android Studio Hedgehog (2023.1.1) 이상
- JDK 17
- Android SDK 34

### 설정

```bash
# 1. local.properties 설정
cp local.properties.example local.properties
# local.properties에 sdk.dir 경로 입력

# 2. 빌드
./gradlew assembleDebug

# 3. APK 위치
# app/build/outputs/apk/debug/app-debug.apk
```

### Android Studio에서 실행

1. Android Studio에서 이 디렉토리를 프로젝트로 열기
2. 에뮬레이터 또는 실기기 연결
3. Run 버튼 클릭

## 기술 스택

- **언어**: Kotlin
- **UI**: Jetpack Compose (Canvas API)
- **최소 SDK**: API 24 (Android 7.0)
- **타겟 SDK**: API 34 (Android 14)
- **게임 루프**: Compose LaunchedEffect + delay 기반 프레임 루프
- **렌더링**: DrawScope 기반 커스텀 렌더링 (별도 게임 엔진 없음)

## 프로젝트 구조

```
app/src/main/java/com/skystacker/game/
├── MainActivity.kt          # 앱 진입점, 전체화면 설정
└── game/
    ├── GameState.kt         # 데이터 모델 (Block, Particle 등)
    ├── GameLogic.kt         # 게임 로직 (충돌, 점수, 콤보)
    ├── GameRenderer.kt      # Canvas 렌더링 (배경, 블록, HUD)
    ├── GameScreen.kt        # Composable 게임 화면 + 게임 루프
    └── MenuScreen.kt        # 메뉴/게임오버 오버레이
```
