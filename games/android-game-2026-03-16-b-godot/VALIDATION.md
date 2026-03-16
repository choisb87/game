# VALIDATION

## 확인한 항목

- `project.godot`에 메인 씬이 `res://scenes/Main.tscn`으로 연결되어 있습니다.
- 요구된 폴더 `scenes/`, `scripts/`, `assets/`를 모두 생성했습니다.
- 요구된 문서 `README.md`, `PLAN.md`, `MARKET_BENCHMARK.md`, `VALIDATION.md`, `metadata.json`를 모두 포함했습니다.
- 메인 씬은 `Main` 루트 아래에 배경, 보드, HUD, 완료 카드, 버튼 UI가 연결된 구조로 작성했습니다.
- 모든 외부 스크립트/에셋 경로는 프로젝트 내부 상대 경로로만 참조합니다.
- `gdlint scripts/*.gd` 정적 검사에서 문제 없이 통과했습니다.
- `project.godot`와 `scenes/Main.tscn`이 참조하는 `res://` 경로가 모두 실제 파일로 존재함을 확인했습니다.

## 현재 환경 한계

- 이 작업 환경에는 `godot4` 또는 `godot` 실행 파일이 없어 실제 에디터 실행과 Android export는 수행하지 못했습니다.
- 따라서 이번 검증은 정적 구조 검토와 파일 연결 확인까지입니다.

## Godot 에디터에서 추가 확인할 항목

1. 프로젝트 Import 후 `Main.tscn`이 바로 열리고 실행되는지 확인
2. 5개 레벨이 제한 회전 수 안에서 정상적으로 클리어 가능한지 플레이테스트
3. Android export preset 생성 후 실제 기기에서 터치 입력과 세로 레이아웃 확인
4. 필요시 사운드, 폰트, 햅틱, 저장 시스템 추가
