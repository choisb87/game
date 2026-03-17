extends Control

signal retry_pressed
signal menu_pressed

@onready var eyebrow_label: Label = $SafeArea/Layout/ResultCard/ResultContent/EyebrowLabel
@onready var title_label: Label = $SafeArea/Layout/ResultCard/ResultContent/TitleLabel
@onready var score_label: Label = $SafeArea/Layout/ResultCard/ResultContent/ScoreLabel
@onready var detail_label: Label = $SafeArea/Layout/ResultCard/ResultContent/DetailLabel
@onready var best_label: Label = $SafeArea/Layout/ResultCard/ResultContent/BestLabel
@onready var footer_label: Label = $SafeArea/Layout/FooterLabel
@onready var retry_button: Button = $SafeArea/Layout/Buttons/RetryButton
@onready var menu_button: Button = $SafeArea/Layout/Buttons/MenuButton


func _ready() -> void:
	_apply_skin()
	retry_button.pressed.connect(_on_retry_pressed)
	menu_button.pressed.connect(_on_menu_pressed)


func set_result(data: Dictionary, is_new_best: bool, best_score: int) -> void:
	var cleared := bool(data.get("cleared", false))
	var score := int(data.get("score", 0))
	var kills := int(data.get("kills", 0))
	var wave := int(data.get("wave", 1))
	var rank := String(data.get("rank", "D"))
	var time_survived := float(data.get("time_survived", 0.0))
	var shield_left := int(data.get("shield_left", 0))
	var overdrives_used := int(data.get("overdrives_used", 0))

	eyebrow_label.text = "SORTIE CLEAR" if cleared else "SIGNAL LOST"
	title_label.text = "랭크 %s  |  %s" % [rank, "스카이라인 돌파" if cleared else "강제 이탈"]
	score_label.text = "%s 점" % _format_score(score)
	detail_label.text = (
		"격추 %d기  |  위협 단계 %d  |  체류 %.1f초  |  실드 %d%%  |  오버드라이브 %d회"
		% [kills, wave, time_survived, shield_left, overdrives_used]
	)
	best_label.text = "최고 점수  %s%s" % [_format_score(best_score), "  |  NEW BEST" if is_new_best else ""]
	footer_label.text = "다시 출격해 패턴 숙련도를 올리거나 메뉴로 돌아가 기록을 확인할 수 있습니다."


func _apply_skin() -> void:
	title_label.add_theme_font_size_override("font_size", 42)
	score_label.add_theme_font_size_override("font_size", 66)
	eyebrow_label.add_theme_font_size_override("font_size", 20)
	detail_label.add_theme_font_size_override("font_size", 24)
	best_label.add_theme_font_size_override("font_size", 26)
	footer_label.add_theme_font_size_override("font_size", 22)
	retry_button.add_theme_font_size_override("font_size", 30)
	menu_button.add_theme_font_size_override("font_size", 28)

	for label in [eyebrow_label, title_label, score_label, detail_label, best_label, footer_label]:
		label.modulate = Color(0.94, 0.97, 1.0, 0.97)

	var panel_style := StyleBoxFlat.new()
	panel_style.bg_color = Color(0.04, 0.06, 0.12, 0.84)
	panel_style.border_color = Color(0.98, 0.73, 0.28, 0.22)
	panel_style.border_width_left = 2
	panel_style.border_width_top = 2
	panel_style.border_width_right = 2
	panel_style.border_width_bottom = 2
	panel_style.corner_radius_top_left = 30
	panel_style.corner_radius_top_right = 30
	panel_style.corner_radius_bottom_right = 30
	panel_style.corner_radius_bottom_left = 30
	panel_style.shadow_color = Color(0.0, 0.0, 0.0, 0.30)
	panel_style.shadow_size = 30
	panel_style.shadow_offset = Vector2(0.0, 12.0)
	$SafeArea/Layout/ResultCard.add_theme_stylebox_override("panel", panel_style)


func _on_retry_pressed() -> void:
	retry_pressed.emit()


func _on_menu_pressed() -> void:
	menu_pressed.emit()


func _format_score(value: int) -> String:
	var digits := str(max(value, 0))
	var parts := []
	while digits.length() > 3:
		parts.push_front(digits.substr(digits.length() - 3, 3))
		digits = digits.substr(0, digits.length() - 3)
	parts.push_front(digits)
	return ",".join(parts)

