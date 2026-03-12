# Void Runner: 중력의 끝

> 중력을 반전시키며 끝없는 복도를 질주하는 아케이드 러너 게임

## 게임 소개

**Void Runner**는 원터치 중력 반전 메커니즘을 기반으로 한 자동 스크롤 러너 게임입니다. 플레이어는 네온 복도를 질주하며 탭 한 번으로 중력을 뒤집어 천장과 바닥을 오가며 장애물을 피하고 크리스탈을 수집합니다.

## 플레이 방법

### 기본 조작
- **탭**: 중력 반전 (바닥 ↔ 천장)
- 그 외 조작 없음 — 원터치로 모든 것을 해결

### 장애물 유형
| 유형 | 설명 | 등장 존 |
|------|------|---------|
| 바닥 스파이크 | 바닥에서 솟아오르는 뾰족한 장애물 | Zone 1+ |
| 천장 스파이크 | 천장에서 내려오는 뾰족한 장애물 | Zone 1+ |
| 양면 스파이크 | 바닥과 천장 모두에 스파이크 | Zone 2+ |
| 벽 (간격 통과) | 작은 틈새를 통과해야 하는 벽 | Zone 2+ |
| 레이저 | 주기적으로 점멸하는 수평 레이저 | Zone 3+ |

### 크리스탈
- **일반 크리스탈** (시안): 100점
- **희귀 크리스탈** (골드): 500점
- 연속 수집 시 콤보 배율 적용 (x1.5, x2.0, x2.5...)
- 콤보 유지 시간: 2.5초

### 존(Zone) 시스템
- 400m마다 새로운 존 진입
- 존이 올라갈수록:
  - 스크롤 속도 증가
  - 새로운 장애물 유형 등장
  - 장애물 간격 감소
  - 벽 틈새 크기 감소

### 점수 체계
- **거리 점수**: 이동 거리 × 10
- **크리스탈 점수**: 기본값 × 콤보 배율
- **총점**: 거리 점수 + 크리스탈 점수

## 빌드 및 실행

### 사전 요구사항
- Android Studio Hedgehog (2023.1.1) 이상
- JDK 17
- Android SDK 34

### 설정
```bash
# local.properties 설정
cp local.properties.example local.properties
# sdk.dir 경로를 실제 Android SDK 경로로 수정

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
android-game-2026-03-12/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/voidrunner/gravity/
│   │   ├── MainActivity.kt          # 엔트리포인트
│   │   └── game/
│   │       ├── GameState.kt         # 불변 게임 상태 데이터
│   │       ├── GameLogic.kt         # 물리, 충돌, 월드 생성
│   │       ├── GameRenderer.kt      # Canvas 렌더링
│   │       ├── GameScreen.kt        # 게임 루프 + 입력
│   │       └── MenuScreen.kt        # 메인 메뉴 UI
│   └── res/values/
│       ├── strings.xml
│       ├── colors.xml
│       └── themes.xml
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── metadata.json
```

## 기술 스택

- **언어**: Kotlin 1.9.22 (100%)
- **UI**: Jetpack Compose + Canvas API
- **아키텍처**: 불변 상태 + 순수 함수 업데이트
- **렌더링**: 60fps Canvas 기반
- **네트워킹**: 없음 (완전 오프라인)
- **권한**: 없음
