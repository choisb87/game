# Orbit Shield: 궤도 방패

행성을 지키는 궤도 방패 아케이드 게임. 사방에서 날아오는 소행성을 방패로 튕겨내며 최대한 오래 생존하라!

## 게임 방법

### 조작
- **화면 탭**: 방패의 회전 방향을 전환 (시계/반시계)
- 방패는 행성 주위를 자동으로 회전
- 타이밍에 맞춰 탭하여 소행성을 정확히 막아라

### 소행성 종류
| 종류 | 색상 | HP | 점수 |
|------|------|-----|------|
| 일반 | 회색 | 1 | 10 |
| 강화 | 노란색 | 1 | 15 |
| 중장갑 | 주황색 | 2 | 25 |
| 보스급 | 빨간색 | 3 | 50 |

### 콤보 시스템
- 연속 튕겨내기 시 콤보 배율 적용 (최대 x10)
- 콤보는 3초간 유지
- 콤보 유지가 고득점의 핵심

### 파워업
- **방패 확장** (금색): 방패 크기 증가 + 속도 상승 (8초)
- **체력 회복** (녹색): 생명력 1 회복

### 난이도
- 12초마다 레벨 상승
- 소행성 출현 빈도 및 속도 증가
- 고레벨에서 다중 소행성 동시 출현

## 빌드 및 실행

### 요구사항
- Android Studio Hedgehog 이상
- JDK 17
- Android SDK 34

### 설정
```bash
# local.properties 설정
cp local.properties.example local.properties
# sdk.dir 경로를 실제 Android SDK 위치로 수정

# 디버그 빌드
./gradlew assembleDebug

# 기기에 설치
./gradlew installDebug
```

## 프로젝트 구조
```
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/orbitshield/game/
│   │   ├── MainActivity.kt          # 진입점
│   │   └── game/
│   │       ├── GameState.kt          # 불변 게임 상태
│   │       ├── GameLogic.kt          # 순수 함수 게임 로직
│   │       ├── GameRenderer.kt       # Canvas 렌더링
│   │       ├── GameScreen.kt         # 게임 루프 + 입력
│   │       └── MenuScreen.kt         # 메뉴 화면
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
- **언어**: Kotlin 1.9.22
- **UI**: Jetpack Compose (BOM 2024.01.00)
- **렌더링**: Compose Canvas API (외부 게임 엔진 없음)
- **아키텍처**: 단일 Activity, 불변 상태, 순수 함수 업데이트
