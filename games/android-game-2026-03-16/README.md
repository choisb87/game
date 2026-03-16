# Gravity Well (중력의 우물)

중력을 조종하여 별을 유도하는 물리 퍼즐 아케이드 게임.

## 게임 소개

떨어지는 별들을 화면에 배치한 중력 우물로 끌어당기거나 밀어내어 올바른 색상 영역에 안착시키세요.
인력과 척력을 전략적으로 사용하고, 연속 포획 콤보로 높은 점수를 노리세요.

## 핵심 메카닉

- **중력 우물 배치**: 화면 탭으로 인력/척력 우물 생성
- **4종 별**: 금별(기본), 은별(빠름), 루비별(무거움), 다이아몬드별(작고 빠름)
- **색상 매칭**: 별을 같은 색 타겟 영역으로 유도
- **콤보 시스템**: 연속 포획 시 점수 배율 상승 + 슬로모션 효과
- **레벨 진행**: 40% 이상 포획 시 다음 레벨 진출

## 기술 스택

- **언어**: Kotlin 100%
- **UI**: Jetpack Compose
- **렌더링**: Compose Canvas API (프로시저럴 드로잉)
- **아키텍처**: Single Activity, 불변 상태, 순수 함수 업데이트
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34

## 빌드 & 실행

### 요구사항
- Android Studio Hedgehog (2023.1.1) 이상
- JDK 17
- Android SDK 34

### 설정
```bash
# 1. local.properties 생성
cp local.properties.example local.properties
# 2. sdk.dir 경로를 본인의 Android SDK 경로로 수정

# 3. 빌드
./gradlew assembleDebug

# 4. 설치 (연결된 디바이스/에뮬레이터)
./gradlew installDebug
```

### Android Studio에서 실행
1. Android Studio에서 이 디렉토리를 프로젝트로 열기
2. Gradle sync 대기
3. Run ▶ 클릭

## 프로젝트 구조

```
app/src/main/
├── AndroidManifest.xml
├── java/com/gravitywell/game/
│   ├── MainActivity.kt          # 진입점, 화면 네비게이션
│   └── game/
│       ├── GameState.kt          # 불변 게임 상태 데이터 클래스
│       ├── GameLogic.kt          # 순수 함수 게임 로직
│       ├── GameRenderer.kt       # Canvas 기반 렌더링
│       ├── GameScreen.kt         # 게임 루프 + 입력 처리
│       └── MenuScreen.kt         # 메뉴 화면 UI
└── res/
    ├── drawable/                  # 앱 아이콘
    ├── mipmap-anydpi-v26/        # 적응형 아이콘
    └── values/                    # 문자열, 색상, 테마
```
