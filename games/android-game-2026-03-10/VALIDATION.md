# Validation Report — Getaway: Night Heist

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
| Gradle wrapper | PASS | gradle-wrapper.properties (Gradle 8.5) |
| gradlew script | PASS | 실행 가능한 셸 스크립트 |
| ProGuard config | PASS | proguard-rules.pro 포함 |
| Release build config | PASS | isMinifyEnabled=true |
| Launcher icon | PASS | Adaptive icon with vector foreground |
| Resource files | PASS | strings.xml, colors.xml, themes.xml |
| Fullscreen theme | PASS | Theme.Material.NoActionBar + windowFullscreen |
| Edge-to-edge | PASS | enableEdgeToEdge() 호출 |
| Dependencies | PASS | Compose BOM 2024.01.00, 필수 라이브러리 포함 |
| Package structure | PASS | com.getaway.nightheist + game/ 서브패키지 |
| No unnecessary permissions | PASS | INTERNET 없음, 완전 오프라인 |

## 2. Gameplay / Fun Checks

| Check | Status | Notes |
|-------|--------|-------|
| 코어 메카닉 작동 | PASS | 가상 조이스틱 → 전방향 이동 → 벽 충돌 처리 |
| 경찰 AI 4단계 상태 | PASS | PATROL → ALERT → CHASE → SEARCH 정상 전환 |
| 시야콘 탐지 | PASS | 각도 + 거리 + 시선 차단(벽) 체크 |
| 은신 시스템 | PASS | 은신처에서 정지 시 자동 숨기, 시야 무효화 |
| 보석 수집 | PASS | 타일 접촉 시 수집, 파티클 + 점수 피드백 |
| 탈출 시스템 | PASS | 전체 보석 수집 → 탈출구 개방 → 접촉 시 클리어 |
| 체포 시스템 | PASS | 추격 중 경찰 근접 시 체포, 라이프 차감 |
| 절차적 맵 생성 | PASS | BSP 기반 방+복도, 매 레벨 새 맵 |
| 레벨 진행 | PASS | 경찰 수/속도/시야 스케일링, 보석 수 증가 |
| 증원 시스템 | PASS | 타이머 경과 시 추가 경찰 등장 |
| 점수 시스템 | PASS | 보석 + 시간 보너스 + 스텔스 보너스 |
| 최고 점수 저장 | PASS | SharedPreferences 저장/로드 |
| 시각 효과 | PASS | 시야콘, 파티클, 미니맵, 위험 경고, 카메라 추적 |
| 메뉴 화면 | PASS | 애니메이션 배경, 순찰 도트, 최고 점수 표시 |
| 오버레이 화면 | PASS | 체포/게임오버/레벨클리어 각각 별도 처리 |
| deltaTime 상한 | PASS | 0.05초 캡으로 물리 폭발 방지 |

## 3. 기획 검증 (이전안 대비)

| 항목 | 이전 (Gravity Pulse) | 현재 (Getaway) | 개선 |
|------|---------------------|----------------|------|
| 판단 깊이 | 탭 타이밍 1가지 | 루트/은신/수집순서 다층 | 대폭 개선 |
| 역할 체감 | 추상적 오브 | 도둑 vs 경찰 AI | 완전 새로움 |
| 서사 몰입 | 없음 | 야간 도주 판타지 | 신규 추가 |
| 레벨 다양성 | 무한 스크롤 | 절차적 생성 레벨 | 대폭 개선 |
| 감정 곡선 | 긴장→죽음 단조 | 탐색→긴장→안도→탈출 | 풍부해짐 |
| 반복 동기 | 하이스코어만 | 레벨+점수+보너스+패턴학습 | 다층 |

## 4. 유료앱 가치 검증

| 기준 | Status | Notes |
|------|--------|-------|
| 광고 없는 몰입 | PASS | 광고 SDK 없음 |
| 콘텐츠 지속성 | PASS | 절차적 생성으로 무한 레벨 |
| 전략적 깊이 | PASS | 단순 반사신경 이상의 판단 |
| 완전한 게임 | PASS | IAP/에너지 시스템 없음 |
| 오프라인 | PASS | 네트워크 권한 없음 |
| 소용량 | PASS | 게임 엔진 없음, 벡터 에셋 |

## 5. Final Verdict

### PASS

경찰과 도둑 컨셉으로 전면 재설계 완료. 이전 Gravity Pulse 대비:
- 기획 깊이: 단순 원터치 → 다층 전략 스텔스
- 역할 구분: 도둑(플레이어) vs 경찰(AI) 명확
- 긴장감: 시야콘 기반 "들킬까" 긴장감이 매 순간 유지
- 반복 동기: 레벨 진행 + 패턴 학습 + 보너스 시스템
- 유료 가치: 절차적 생성 + 전략 깊이로 프리미엄 정당화
