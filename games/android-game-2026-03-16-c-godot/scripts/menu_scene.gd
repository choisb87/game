extends Control

signal start_pressed

@onready var eyebrow_label: Label = $SafeArea/Layout/HeroCard/HeroContent/EyebrowLabel
@onready var title_label: Label = $SafeArea/Layout/HeroCard/HeroContent/TitleLabel
@onready var subtitle_label: Label = $SafeArea/Layout/HeroCard/HeroContent/SubtitleLabel
@onready var hook_label: Label = $SafeArea/Layout/HeroCard/HeroContent/HookLabel
@onready var best_label: Label = $SafeArea/Layout/InfoCard/InfoContent/BestLabel
@onready var rules_label: Label = $SafeArea/Layout/InfoCard/InfoContent/RulesLabel
@onready var pulse_label: Label = $SafeArea/Layout/InfoCard/InfoContent/PulseLabel
@onready var start_button: Button = $SafeArea/Layout/StartButton


func _ready() -> void:
	_apply_skin()
	start_button.pressed.connect(_on_start_pressed)
	_play_entry_tween()


func set_summary(best_score: int, best_rank: String) -> void:
	best_label.text = "최고 기록  %s점   최고 랭크  %s" % [_format_score(best_score), best_rank]


func _apply_skin() -> void:
	title_label.add_theme_font_size_override("font_size", 76)
	subtitle_label.add_theme_font_size_override("font_size", 30)
	hook_label.add_theme_font_size_override("font_size", 24)
	eyebrow_label.add_theme_font_size_override("font_size", 20)
	best_label.add_theme_font_size_override("font_size", 24)
	rules_label.add_theme_font_size_override("font_size", 22)
	pulse_label.add_theme_font_size_override("font_size", 22)
	start_button.add_theme_font_size_override("font_size", 36)

	for label in [eyebrow_label, subtitle_label, hook_label, best_label, rules_label, pulse_label]:
		label.modulate = Color(0.88, 0.94, 1.0, 0.94)

	title_label.modulate = Color(0.97, 0.96, 1.0, 1.0)
	start_button.modulate = Color(1.0, 1.0, 1.0, 1.0)

	var panel_style := StyleBoxFlat.new()
	panel_style.bg_color = Color(0.03, 0.06, 0.12, 0.82)
	panel_style.border_color = Color(0.20, 0.86, 0.98, 0.18)
	panel_style.border_width_left = 2
	panel_style.border_width_top = 2
	panel_style.border_width_right = 2
	panel_style.border_width_bottom = 2
	panel_style.corner_radius_top_left = 28
	panel_style.corner_radius_top_right = 28
	panel_style.corner_radius_bottom_right = 28
	panel_style.corner_radius_bottom_left = 28
	panel_style.shadow_color = Color(0.0, 0.0, 0.0, 0.28)
	panel_style.shadow_size = 30
	panel_style.shadow_offset = Vector2(0.0, 12.0)
	$SafeArea/Layout/HeroCard.add_theme_stylebox_override("panel", panel_style)
	$SafeArea/Layout/InfoCard.add_theme_stylebox_override("panel", panel_style)

	var button_style := StyleBoxFlat.new()
	button_style.bg_color = Color(0.44, 0.27, 0.10, 0.98)
	button_style.border_color = Color(0.99, 0.82, 0.32, 0.34)
	button_style.border_width_left = 2
	button_style.border_width_top = 2
	button_style.border_width_right = 2
	button_style.border_width_bottom = 2
	button_style.corner_radius_top_left = 24
	button_style.corner_radius_top_right = 24
	button_style.corner_radius_bottom_right = 24
	button_style.corner_radius_bottom_left = 24
	start_button.add_theme_stylebox_override("normal", button_style)
	start_button.add_theme_stylebox_override("hover", button_style)
	start_button.add_theme_stylebox_override("pressed", button_style)


func _play_entry_tween() -> void:
	var hero_card: Control = $SafeArea/Layout/HeroCard
	var info_card: Control = $SafeArea/Layout/InfoCard
	hero_card.modulate.a = 0.0
	info_card.modulate.a = 0.0
	start_button.modulate.a = 0.0

	var tween := create_tween()
	tween.set_parallel(false)
	tween.tween_property(hero_card, "modulate:a", 1.0, 0.35)
	tween.tween_property(info_card, "modulate:a", 1.0, 0.3)
	tween.tween_property(start_button, "modulate:a", 1.0, 0.25)


func _on_start_pressed() -> void:
	start_pressed.emit()


func _format_score(value: int) -> String:
	var digits := str(max(value, 0))
	var parts := []
	while digits.length() > 3:
		parts.push_front(digits.substr(digits.length() - 3, 3))
		digits = digits.substr(0, digits.length() - 3)
	parts.push_front(digits)
	return ",".join(parts)
