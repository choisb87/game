# Chain Reactor (체인 리액터)

**연쇄 폭발 퍼즐 아케이드** — 제한된 스파크로 최대한 많은 오브를 연쇄 폭파시키는 전략 게임

## 게임 설명

화면에 떠다니는 다양한 오브들 사이에서, 제한된 횟수의 스파크(탭)를 전략적으로 배치하여 가장 큰 연쇄 반응을 만들어내세요.

### 조작법
- **화면 탭**: 해당 위치에 폭발 생성
- 폭발이 오브에 닿으면 해당 오브도 폭발 → 연쇄 반응 시작!

### 오브 종류
| 오브 | 효과 |
|------|------|
| 일반 오브 | 기본 폭발 반경 |
| 🟣 메가 오브 | 5배 넓은 폭발 반경 |
| 🔵 스플리터 | 폭발 시 6방향 파편 발사 |
| 🧊 프리즈 | 주변 오브를 얼려서 느리게 함 |
| 🟡 골든 오브 | 3배 점수 |

### 게임 시스템
- **라운드 시스템**: 라운드마다 오브 수 증가, 스파크 수도 점진적 증가
- **체인 콤보**: 연속 폭파 시 최대 x10 배율
- **클리어 조건**: 50% 이상 오브 폭파 시 다음 라운드 진행
- **퍼펙트 보너스**: 모든 오브 폭파 시 추가 점수
- **슬로우 모션**: 5체인 이상 달성 시 극적인 슬로우 모션 효과

## 빌드 및 실행

### 요구사항
- Android Studio Hedgehog (2023.1.1) 이상
- JDK 17
- Android SDK 34
- Kotlin 1.9.22

### 실행 방법
1. `local.properties.example`을 `local.properties`로 복사 후 SDK 경로 설정
2. Android Studio에서 프로젝트 열기
3. Gradle 동기화 후 실행

```bash
cp local.properties.example local.properties
# local.properties 내 sdk.dir 경로 수정
```

### 명령줄 빌드
```bash
./gradlew assembleDebug
```

## 프로젝트 구조
```
app/src/main/java/com/chainreactor/game/
├── MainActivity.kt          # 앱 진입점
└── game/
    ├── GameState.kt          # 불변 상태 데이터 모델
    ├── GameLogic.kt          # 순수 함수 게임 로직
    ├── GameRenderer.kt       # Canvas 렌더링
    ├── GameScreen.kt         # 게임 루프 + 입력 처리
    └── MenuScreen.kt         # 메뉴 화면
```

## 기술 스택
- **언어**: Kotlin 1.9.22
- **UI**: Jetpack Compose (BOM 2024.01.00)
- **렌더링**: Compose Canvas API (프로시저럴 드로잉)
- **아키텍처**: 단일 액티비티, 불변 상태, 함수형 업데이트
- **최소 SDK**: 24 (Android 7.0, 97%+ 커버리지)
