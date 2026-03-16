# Glass Garden

`Glass Garden`은 Godot 4 기반으로 만든 프리미엄 모바일 퍼즐 프로토타입입니다. 플레이어는 유리 온실의 거울 타일을 회전시켜 빛의 경로를 정렬하고, 잠든 꽃을 모두 피워 각 구획을 복원합니다.

## 콘셉트

- 장르: 프리미엄 라이트 라우팅 퍼즐
- 세션 길이: 1~3분
- 조작: 탭 한 손 조작
- 플랫폼 목표: Android 세로 화면
- 톤앤매너: 반투명 유리, 금속 프레임, 황금빛 광원, 차분한 식물원 감성

## 포함된 내용

- `project.godot`: Godot 4 프로젝트 설정, 메인 씬 지정
- `scenes/Main.tscn`: 에디터에서 바로 열 수 있는 메인 씬
- `scripts/`: 메인 UI, 보드 렌더링, 배경 연출 스크립트
- `assets/`: 앱 아이콘 및 브랜드/비주얼 SVG 리소스
- `PLAN.md`: 기획 의도와 구현 범위
- `MARKET_BENCHMARK.md`: 시장/레퍼런스 포지셔닝
- `VALIDATION.md`: 검증 범위와 미검증 항목
- `metadata.json`: 프로젝트 메타데이터

## 플레이 방법

1. 광원은 보드 바깥에서 들어옵니다.
2. 거울 타일을 탭하면 90도씩 회전합니다.
3. 빛이 꽃이 있는 칸을 지나가면 해당 꽃이 개화합니다.
4. 각 레벨의 제한 회전 수 안에 모든 꽃을 피우면 클리어입니다.
5. 검은 유리 블로커는 빛을 흡수합니다.

## Godot 에디터에서 열기

1. Godot 4.x 에디터에서 이 폴더를 프로젝트로 Import 합니다.
2. 메인 씬은 `res://scenes/Main.tscn`으로 지정되어 있습니다.
3. 에디터 상단의 Run Current Scene 또는 Play 버튼으로 즉시 실행할 수 있습니다.

## Android export 가이드

1. Godot 4 Android export 템플릿을 설치합니다.
2. `Project > Export`에서 Android preset을 생성합니다.
3. 패키지명 예시로 `com.glassgarden.game`을 사용하면 됩니다.
4. 세로 화면 앱으로 배포할 수 있도록 프로젝트 설정은 portrait 기준으로 맞춰져 있습니다.

## 폴더 구조

```text
android-game-2026-03-16-b-godot/
├── assets/
│   ├── art/
│   └── ui/
├── scenes/
├── scripts/
├── project.godot
├── README.md
├── PLAN.md
├── MARKET_BENCHMARK.md
├── VALIDATION.md
└── metadata.json
```

## 프로토타입 범위

- 수작업으로 설계한 5개 레벨
- 광선 추적 기반 퍼즐 플레이
- 레벨 재시작 / 다음 구획 이동 UI
- Godot 기본 UI와 커스텀 드로잉만으로 구성된 가벼운 구조

