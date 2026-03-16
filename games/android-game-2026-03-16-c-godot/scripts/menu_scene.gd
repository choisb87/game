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
	title_label.add_theme_font_size_override("font_size", 72)
	subtitle_label.add_theme_font_size_override("font_size", 30)
	hook_label.add_theme_font_size_override("font_size", 24)
	eyebrow_label.add_theme_font_size_override("font_size", 22)
	best_label.add_theme_font_size_override("font_size", 24)
	rules_label.add_theme_font_size_override("font_size", 24)
	pulse_label.add_theme_font_size_override("font_size", 22)
	start_button.add_theme_font_size_override("font_size", 34)

	for label in [eyebrow_label, subtitle_label, hook_label, best_label, rules_label, pulse_label]:
		label.modulate = Color(0.88, 0.94, 1.0, 0.94)

	title_label.modulate = Color(0.97, 0.96, 1.0, 1.0)
	start_button.modulate = Color(1.0, 1.0, 1.0, 1.0)


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
