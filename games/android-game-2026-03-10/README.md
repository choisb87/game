# Gravity Pulse

Kotlin + Jetpack Compose로 만든 원터치 아케이드 안드로이드 게임입니다.

화면을 탭해서 중력을 뒤집고, 네온 장애물의 틈을 통과하며 최고 점수를 노리는 구조예요.

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
│       ├── java/com/gravitypulse/game/
│       │   ├── MainActivity.kt    # 진입점, 화면 전환, 최고 점수 저장
│       │   └── game/
│       │       ├── GameState.kt   # 게임 상태, 물리, 충돌 판정
│       │       ├── GameLoop.kt    # 프레임 동기화 업데이트 루프
│       │       ├── GameRenderer.kt# Canvas 렌더링(배경, 장애물, 파티클)
│       │       ├── GameScreen.kt  # HUD 포함 게임 화면
│       │       ├── GameInput.kt   # 터치 입력 처리
│       │       └── MenuScreen.kt  # 메인 메뉴 및 연출
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

1. 화면 아무 곳이나 **탭**하면 중력이 반전됩니다.
2. 구체는 현재 중력 방향에 따라 위 또는 아래로 이동합니다.
3. 위로 스크롤되는 네온 장애물의 **틈 사이를 통과**해야 합니다.
4. 장애물 하나를 통과할 때마다 점수가 +1 오르고, 연속 통과 시 콤보가 쌓입니다.
5. 장애물에 부딪히면 게임 종료, 최고 점수는 저장됩니다.

## 조작법

- **한 번 탭**: 중력 방향 전환 (위 ↔ 아래)

조작은 단순하지만, 타이밍과 감각으로 깊이를 만드는 게임입니다.

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
- **구조**: Single-activity + Composable 상태 관리
- **최소 SDK**: 24 (Android 7.0)
