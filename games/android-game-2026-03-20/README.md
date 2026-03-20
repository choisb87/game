# 네온 브레이커 (Neon Breaker)

네온 비주얼의 프리미엄 벽돌깨기 게임. 패들을 드래그하여 공을 튕기고, 네온 벽돌을 부수며 콤보를 쌓아 최고 점수를 달성하세요.

## 주요 기능

- **네온 비주얼**: 글로우 이펙트, 파티클 시스템, 화면 흔들림
- **콤보 시스템**: 연속 타격으로 점수 배율 증가 (5히트마다 1x 추가)
- **5가지 파워업**: 와이드 패들, 멀티볼, 파이어볼, 슬로우, 추가 생명
- **다양한 레벨 패턴**: 체커보드, 다이아몬드, V자 등 레벨별 다른 배치
- **3종 벽돌**: 일반(1히트), 강화(2히트), 아머(3히트)

## 빌드 & 실행

### 요구 사항

- Android Studio Hedgehog 이상
- JDK 17
- Android SDK 34

### 설정

```bash
# local.properties 생성
cp local.properties.example local.properties
# sdk.dir 경로를 본인 환경에 맞게 수정
```

### 빌드

```bash
# 디버그 빌드
./gradlew assembleDebug

# 릴리스 빌드 (서명 설정 필요)
./gradlew assembleRelease
```

### 실행

Android Studio에서 프로젝트를 열고 Run 버튼을 누르거나:

```bash
./gradlew installDebug
adb shell am start -n com.neonbreaker.game/.MainActivity
```

## 조작법

| 조작 | 동작 |
|------|------|
| 드래그 | 패들 좌우 이동 |
| 탭 | 공 발사 / 재시작 |

## 기술 스택

- Kotlin + Jetpack Compose
- Canvas API 기반 렌더링
- 60fps 게임 루프 (withFrameMillis)
- 순수 Compose — 외부 게임 엔진 없음
