# game

매일 안드로이드 게임을 기획·생성·검증·빌드해서 올리는 저장소입니다.

## 목표
- **재미 우선**: 광고 낚시형이 아니라 실제로 플레이가 재미있는 게임
- **안드로이드 네이티브**: Kotlin + Jetpack Compose 기반 우선
- **마켓 지향**: 유료앱 출시를 염두에 둔 품질/문서/AAB 빌드 포함
- **반복 개선**: 마켓 인기 게임 참고 + 피드백 반영 + 매일 개선

## 현재 운영 방식
1. 게임 아이디어 기획
2. Claude Code로 게임 생성/검증
3. Android 빌드 검증
4. APK/AAB 산출물 생성
5. GitHub에 커밋 및 푸시

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

## 오늘 게임
- 폴더: `games/android-game-2026-03-10`
- 컨셉: **Getaway: Night Heist**
- 장르: 경찰과 도둑 컨셉의 탑다운 스텔스 추격 게임

## 산출물
- AAB: `games/android-game-2026-03-10/app/build/outputs/bundle/release/app-release.aab`
- APK: `games/android-game-2026-03-10/app/build/outputs/apk/release/app-release-unsigned.apk`

## 원칙
- README 및 주요 문서는 한국어 우선
- 실제 안드로이드 앱 기준으로 제작
- 단순 데모보다 유료앱 가능성을 우선 검토
