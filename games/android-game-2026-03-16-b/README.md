# Velvet Lift

`Velvet Lift`는 아르데코 호텔의 밤 근무 컨시어지가 되어 엘리베이터 정차 큐를 설계하는 프리미엄 전략 게임입니다. 손님을 태우고 내리는 단순한 테마에 그치지 않고, VIP 프라이빗 보너스, 특송 시간 보너스, 평론가 리스크, 살롱 동시 하차 보너스를 겹쳐서 "어떤 층을 먼저 예약할지"를 계속 판단하게 만듭니다.

## 게임 개요

- 장르: 부티크 전략 / 엘리베이터 컨시어지 시뮬레이션
- 플랫폼: Android 네이티브
- 엔진/프레임워크: Kotlin + Jetpack Compose + Canvas
- 비즈니스 모델: 유료 완전판
- 가격 포지셔닝: `$3.99` 권장
- 세션 길이: 2분 내외 계약 플레이, 10~15분 캠페인 러닝

## 핵심 플레이

- 건물 층을 탭해서 최대 4개 정차 지점을 미리 큐에 넣습니다.
- 엘리베이터는 큐 순서대로 이동하며, 도착한 층에서 자동으로 손님을 승하차시킵니다.
- 손님 유형마다 보너스와 리스크가 달라 우선순위가 바뀝니다.
- 계약 시간이 끝나기 전에 목표 매출을 넘기고 `House Mood`를 지키면 성공입니다.

## 손님 유형

| 유형 | 설명 |
| --- | --- |
| Suite Guest | 기본 수익원. 안정적으로 점수를 채웁니다. |
| Patron | 단독 탑승 시 보너스가 큰 VIP 손님입니다. |
| Courier | 여유 있게 내려주면 시간을 추가합니다. |
| Critic | 놓치면 평판이 크게 깎이는 핵심 리스크 손님입니다. |
| Salon | 여러 명이 같은 층에서 함께 내릴 때 보너스를 얻습니다. |

## 상용 품질 포인트

- 추상 아케이드가 아니라 "호텔 운영 판타지"와 계약 진행 구조를 붙였습니다.
- 메뉴, 인게임, 결과 화면 모두 동일한 아르데코 톤으로 통일했습니다.
- 기본 텍스트만 얹은 프로토타입이 아니라 손님 칩, 정차 큐, 캐빈 매니페스트, 평판 바, 계약 카드까지 제품형 UI를 갖췄습니다.
- 광고, 에너지, 뽑기, 일일 과금 루프 없이 바로 전체 콘텐츠를 제공하는 유료 앱 스타일입니다.

## 계약 진행 구조

- 총 6개 계약으로 구성된 시즌형 캠페인
- 계약 성공 시 다음 계약이 열립니다.
- 누적 해금에 따라 영구 편의 효과가 열립니다.
- `Brass Servo`: 엘리베이터 속도 증가
- `Suite Ledger`: 캐빈 수용 인원 증가
- `Soft Lighting`: 손님 인내도 지속 시간 증가

## 조작법

- 건물의 원하는 층을 탭: 해당 층을 정차 큐에 추가 또는 제거
- 상단 `Lobby` 버튼 탭: 현재 계약 중단 후 메뉴 복귀

## 빌드

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew bundleRelease
```

필수 환경:

- JDK 17
- Android SDK / Build Tools

## 프로젝트 구조

```text
app/src/main/java/com/velvetlift/game/
├── MainActivity.kt
├── game/
│   ├── AppRoot.kt
│   ├── GameEngine.kt
│   ├── GameModels.kt
│   ├── GameScreen.kt
│   ├── HotelRenderer.kt
│   ├── MenuScreen.kt
│   ├── ProfileStore.kt
│   └── ResultScreen.kt
└── ui/theme/
    └── VelvetLiftTheme.kt
```

## 산출 문서

- `PLAN.md`: 게임 설계 및 시스템 설명
- `MARKET_BENCHMARK.md`: 시장 포지셔닝과 비교 분석
- `VALIDATION.md`: 빌드 및 구조 검증 결과
- `metadata.json`: 배포/분류용 메타데이터
