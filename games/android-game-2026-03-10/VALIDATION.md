# Validation Report — Getaway: Night Heist (v2.0 Premium)

**Date**: 2026-03-10
**Platform**: Native Android (Kotlin + Jetpack Compose)
**Package**: com.getaway.nightheist

---

## 1. Native Android Structure Checks

| Check | Status | Notes |
|-------|--------|-------|
| Language: 100% Kotlin | PASS | 모든 소스 파일이 .kt, Java 없음 |
| AndroidManifest.xml | PASS | Launcher intent, portrait lock, 올바른 테마 참조 |
| Single-activity architecture | PASS | MainActivity + Compose 화면 전환 |
| Root build.gradle.kts | PASS | AGP 8.2.2, Kotlin 1.9.22 |
| App build.gradle.kts | PASS | compileSdk=34, minSdk=24, targetSdk=34, Compose 활성화 |
| settings.gradle.kts | PASS | rootProject.name = "GetawayNightHeist" |
| gradle.properties | PASS | AndroidX, nonTransitiveRClass |
| Gradle wrapper | PASS | gradle-wrapper.properties (Gradle 8.5) + JAR 포함 |
| gradlew script | PASS | APP_HOME → CLASSPATH 순서 수정, JVM opts 수정 |
| ProGuard config | PASS | proguard-rules.pro 포함 |
| Release build config | PASS | isMinifyEnabled=true |
| Launcher icon | PASS | Adaptive icon with vector foreground |
| Resource files | PASS | strings.xml, colors.xml, themes.xml |
| Fullscreen theme | PASS | Theme.Material.NoActionBar + windowFullscreen |
| Edge-to-edge | PASS | enableEdgeToEdge() 호출 |
| Dependencies | PASS | Compose BOM 2024.01.00, 필수 라이브러리 포함 |
| Package structure | PASS | com.getaway.nightheist + game/ 서브패키지 |
| No unnecessary permissions | PASS | INTERNET 없음, 완전 오프라인 |
| **Debug build** | **PASS** | `assembleDebug` 경고만, 에러 없이 성공 |

## 2. Gameplay / Fun Checks

| Check | Status | Notes |
|-------|--------|-------|
| 코어 메카닉 작동 | PASS | 가상 조이스틱 → 전방향 이동 → 벽 충돌 처리 |
| 스닉/러닝 이중속도 | PASS | 조이스틱 드래그 거리로 자동 전환, 시각 표시 |
| 경찰 AI 4단계 상태 | PASS | PATROL → ALERT → CHASE → SEARCH 정상 전환 |
| 경찰 기절 (SMOKE) | PASS | stunTimer로 무력화, 보라색 시각 효과 |
| 시야콘 탐지 | PASS | 각도 + 거리 + 시선 차단(벽) 체크 |
| 은신 시스템 | PASS | 은신처에서 정지 시 자동 숨기, 시야 무효화 |
| 보석 수집 | PASS | 타일 접촉 시 수집, 파티클 + 점수 텍스트 피드백 |
| **콤보 시스템** | **PASS** | 4초 윈도우, 배율 증가, 시각/점수 반영 |
| **파워업 시스템** | **PASS** | SMOKE/SPEED/GHOST 3종, 수집→효과→타이머 |
| 탈출 시스템 | PASS | 전체 보석 수집 → 탈출구 개방 → 접촉 시 클리어 |
| 체포 시스템 | PASS | 추격 중 경찰 근접 시 체포, 라이프 차감 |
| **화면 흔들림** | **PASS** | 체포/증원 시 카메라 셰이크 |
| 절차적 맵 생성 | PASS | BSP 기반 방+복도, 매 레벨 새 맵 |
| **레벨 인트로** | **PASS** | 카메라 패닝, 레벨 제목, 레벨별 자막, 레벨1 튜토리얼 |
| 레벨 진행 | PASS | 경찰 수/속도/시야 스케일링, 보석/파워업 수 증가 |
| 증원 시스템 | PASS | 타이머 경과 시 추가 경찰 등장, 화면 흔들림 |
| 점수 시스템 | PASS | 보석(×콤보) + 시간 + 스텔스 + 콤보 보너스 |
| **점수 분해 화면** | **PASS** | 레벨 클리어 시 각 보너스 항목별 표시 |
| 최고 점수 저장 | PASS | SharedPreferences 저장/로드 |
| **통계 저장** | **PASS** | 클리어 레벨 수 저장, 메뉴에서 표시 |
| **텍스트 HUD** | **PASS** | 레벨, 라이프, 보석, 점수, 타이머 등 텍스트로 표시 |
| **GHOST 파워업** | **PASS** | 투명 효과 + 쉬머 애니메이션 + 시야 무효화 |
| **SPEED 파워업** | **PASS** | 1.5배 속도 + 트레일 이펙트 |
| 미니맵 | PASS | 파워업 위치 포함, 색상 구분 |
| **메뉴 화면** | **PASS** | 텍스트 타이틀, 기록 표시, 조작법, 브랜딩 |
| **오버레이 화면** | **PASS** | 체포/게임오버/레벨클리어 — 텍스트 + 상세 정보 |
| 가상 조이스틱 | PASS | 스닉존 시각 표시 (파란 링) |
| deltaTime 상한 | PASS | 0.05초 캡으로 물리 폭발 방지 |

## 3. 상품성 개선 검증 (before vs after)

| 항목 | Before (데모급) | After (프리미엄급) | 상태 |
|------|-----------------|-------------------|------|
| 첫인상 | 점 패턴 메뉴, 의미 불명 | 텍스트 타이틀 + 기록 + 조작법 | **개선** |
| 정보 전달 | 점/원으로 표시 (판독 불가) | 텍스트 HUD (LV, 라이프, 점수) | **개선** |
| 게임 시작 | 즉시 플레이 (맵 모름) | 레벨 인트로 카메라 패닝 | **개선** |
| 판단 요소 | 루트 + 은신 2가지 | 루트 + 스닉 + 파워업 + 콤보 + 은신 5가지 | **개선** |
| 반복 동기 | 점수만 | 콤보 배율 + 파워업 전략 + 통계 | **개선** |
| 시각 피드백 | 파티클만 | 텍스트 + 화면 흔들림 + 콤보 표시 + 파워업 효과 | **개선** |
| 오버레이 | 원 몇 개 | 텍스트 + 점수 분해 + 상태 정보 | **개선** |
| 게임오버 | 원 + 점 | SCORE + BEST + LEVEL 텍스트 | **개선** |
| 온보딩 | 없음 | 레벨1 튜토리얼 오버레이 | **신규** |

## 4. 유료앱 가치 검증

| 기준 | Status | Notes |
|------|--------|-------|
| 광고 없는 몰입 | PASS | 광고 SDK 없음 |
| 콘텐츠 지속성 | PASS | 절차적 생성으로 무한 레벨 |
| 전략적 깊이 | **PASS** | 5가지 이상 판단 요소 |
| 시스템 다층성 | **PASS** | 콤보 + 파워업 + 스닉 + 스텔스 보너스 |
| 프리미엄 연출 | **PASS** | 텍스트 HUD, 인트로, 셰이크, 분해 화면 |
| 완전한 게임 | PASS | IAP/에너지 시스템 없음 |
| 오프라인 | PASS | 네트워크 권한 없음 |
| 소용량 | PASS | 게임 엔진 없음, 벡터 에셋 |
| 온보딩 | **PASS** | 레벨1 튜토리얼 |
| 통계/진행감 | **PASS** | 최고 점수 + 클리어 레벨 저장 |

## 5. v2.0 개선 사항 (2차 개선 루프)

| 영역 | v1.0 | v2.0 | 체감 차이 |
|------|------|------|-----------|
| Loot 렌더링 | 원형 circle | 다면체 다이아몬드 (facet 하이라이트 + 반짝이 별) | 보석 느낌 → 수집 욕구↑ |
| 벽 깊이감 | 단색 평면 | 상/하/우 그림자 + 미세 텍스처 패턴 | 디버그 뷰 → 게임 맵 느낌 |
| 플레이어 비주얼 | 초록 원 | 마스크 + 눈(시선추적) + 전리품 보따리 + hiding 시 Z 표시 | 캐릭터성 부여 |
| 메뉴 화면 | 밋밋한 배경+작은 아이콘 | 녹색 그리드 + 떠다니는 보석 + 큰 도둑아이콘(동공이동+웃는입) + 전리품 가방 | 첫인상 대폭 상승 |
| Exit 타일 | 작은 화살표 | 아치 문 + 글로우 링 + 잠금 시 X 표시 | 목표 가시성↑ |
| 레벨 인트로 | 텍스트만 | 시네마틱 레터박스 + 미션 정보(보석수/경찰수) + TAP TO SKIP | 영화적 연출 |
| 레벨 완료 | 단순 텍스트 나열 | 패널 레이아웃 + 보석 아이콘 + 좌우 정렬 점수표 + 구분선 | 보상감 극대화 |
| 게임오버 | 밋밋 | 글로우 + 패널 + ★NEW BEST★ 강조 + 맥동 프롬프트 | 재도전 동기↑ |
| 첫 1분 체험 | 첫 보석 위치 랜덤 | 스폰 4타일 내 첫 보석 보장 | 즉각적 보상 확보 |

## 6. Final Verdict

### PASS — Premium Quality v2.0

v1.0의 "유료앱 품질"에서 v2.0의 "유료앱 첫인상 경쟁력"으로 강화:

1. **프로그래머 아트 탈피**: 다이아몬드 보석, 벽 그림자, 캐릭터 마스크로 "게임"처럼 보임
2. **첫 1분 보장**: 스폰 근처 첫 보석으로 즉시 보상 → 콤보 체험까지 자연스러운 흐름
3. **메뉴 첫인상**: 큰 타이틀 + 눈 움직이는 도둑 + 배경 보석 파티클로 "제대로 된 게임" 인식
4. **정보 전달 고급화**: 점수 분해가 좌우 정렬 패널, 아이콘 포함으로 대시보드 느낌
5. **시네마틱 연출**: 레터박스 바 + 미션 브리핑으로 매 레벨이 "새 미션" 느낌
