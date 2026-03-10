# game

안드로이드 게임을 **기획 → 구현 → 검증 → 빌드 → 저장**하는 저장소입니다.

## 목표
- **재미 우선**: 광고 낚시형이 아니라 실제로 재미있는 게임 만들기
- **안드로이드 네이티브 우선**: Kotlin + Jetpack Compose 기반 프로젝트 지향
- **마켓 지향**: 유료앱 출시 가능성을 고려한 품질/문서/AAB 빌드 포함
- **반복 개선**: 마켓 인기 게임 참고 + 피드백 반영 + 매일 개선

## 작업 방식
이 저장소의 게임 제작은 여러 에이전트/도구 협업을 전제로 합니다.

### Codex 역할
- 저장소 구조/자동화 파이프라인 정리
- 빌드 환경 세팅
- Android SDK/JDK 세팅 및 실제 빌드 검증
- Git 커밋/푸시 및 산출물 관리

### Claude Code 역할
- 게임 기획안 발전
- 실제 게임 코드 생성/수정
- 게임성/완성도 관점 검증
- 문서(PLAN, VALIDATION, README 일부) 보완

즉, 이 저장소는 **Codex + Claude Code 협업 흐름**을 포함한 게임 제작 레포입니다.

## 기본 파이프라인
1. 게임 아이디어/장르 선정
2. 시장 참고 자료 수집 및 벤치마크 정리
3. Claude Code로 게임 생성/개선
4. Codex로 빌드 환경/자동화/산출물 검증
5. APK/AAB 생성
6. GitHub 반영

## 폴더 구조
```text
games/
  android-game-YYYY-MM-DD/
    app/
    README.md
    PLAN.md
    MARKET_BENCHMARK.md
    VALIDATION.md
    metadata.json
```

## 산출물 형식
각 게임 폴더 안에는 가능한 경우 아래가 포함됩니다.
- Android 프로젝트 소스
- 한국어 README
- 기획 문서 (`PLAN.md`)
- 시장 참고 문서 (`MARKET_BENCHMARK.md`)
- 검증 문서 (`VALIDATION.md`)
- AAB/APK 빌드 산출물

## 원칙
- README 및 주요 문서는 한국어 우선
- 실제 안드로이드 앱 기준으로 제작
- 단순 데모보다 유료앱 가능성을 우선 검토
- 기획이 약하면 구현보다 먼저 기획을 갈아엎음
