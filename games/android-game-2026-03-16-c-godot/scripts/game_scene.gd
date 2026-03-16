extends Control

signal retreat_pressed
signal run_finished(result: Dictionary)

const BAR_WIDTH := 280.0

@onready var score_label: Label = $SafeArea/HUD/TopRow/Right/ScoreLabel
@onready var time_label: Label = $SafeArea/HUD/TopRow/Right/TimeLabel
@onready var wave_label: Label = $SafeArea/HUD/StatusCard/StatusContent/ComboLabel
@onready var status_label: Label = $SafeArea/HUD/StatusCard/StatusContent/StatusLabel
@onready var shield_fill: ColorRect = $SafeArea/HUD/StatusCard/StatusContent/ShieldRow/ShieldBarBase/ShieldFill
@onready var charge_fill: ColorRect = $SafeArea/HUD/StatusCard/StatusContent/ChargeRow/ChargeBarBase/ChargeFill
@onready var shield_value_label: Label = $SafeArea/HUD/StatusCard/StatusContent/ShieldRow/ShieldValueLabel
@onready var charge_value_label: Label = $SafeArea/HUD/StatusCard/StatusContent/ChargeRow/ChargeValueLabel
@onready var overdrive_button: Button = $SafeArea/HUD/BottomRow/ActionStack/OverdriveButton
@onready var retreat_button: Button = $SafeArea/HUD/BottomRow/ActionStack/RetreatButton
@onready var battlefield: Node2D = $Battlefield


func _ready() -> void:
	_apply_skin()

	overdrive_button.pressed.connect(_on_overdrive_pressed)
	retreat_button.pressed.connect(_on_retreat_pressed)
	battlefield.score_changed.connect(_on_score_changed)
	battlefield.shield_changed.connect(_on_shield_changed)
	battlefield.charge_changed.connect(_on_charge_changed)
	battlefield.time_changed.connect(_on_time_changed)
	battlefield.combo_changed.connect(_on_combo_changed)
	battlefield.status_changed.connect(_on_status_changed)
	battlefield.run_finished.connect(_on_battlefield_finished)

	battlefield.begin()


func _apply_skin() -> void:
	score_label.add_theme_font_size_override("font_size", 34)
	time_label.add_theme_font_size_override("font_size", 28)
	wave_label.add_theme_font_size_override("font_size", 28)
	status_label.add_theme_font_size_override("font_size", 24)
	shield_value_label.add_theme_font_size_override("font_size", 24)
	charge_value_label.add_theme_font_size_override("font_size", 24)
	overdrive_button.add_theme_font_size_override("font_size", 28)
	retreat_button.add_theme_font_size_override("font_size", 24)

	for label in [score_label, time_label, wave_label, status_label, shield_value_label, charge_value_label]:
		label.modulate = Color(0.93, 0.97, 1.0, 0.96)

	_set_bar_value(shield_fill, 1.0)
	_set_bar_value(charge_fill, 0.38)


func _on_score_changed(value: int) -> void:
	score_label.text = "SCORE  %s" % _format_score(value)


func _on_shield_changed(value: float, max_value: float) -> void:
	var ratio := value / max(max_value, 1.0)
	_set_bar_value(shield_fill, ratio)
	shield_fill.color = Color(0.20, 0.93, 0.98, 0.95).lerp(Color(1.0, 0.42, 0.46, 0.98), 1.0 - ratio)
	shield_value_label.text = "SHIELD  %d%%" % int(round(ratio * 100.0))


func _on_charge_changed(value: float, max_value: float) -> void:
	var ratio := value / max(max_value, 1.0)
	_set_bar_value(charge_fill, ratio)
	charge_fill.color = Color(0.99, 0.75, 0.30, 0.96)
	charge_value_label.text = "OVERDRIVE  %d%%" % int(round(ratio * 100.0))
	overdrive_button.disabled = ratio < 0.999
	overdrive_button.text = "OVERDRIVE READY" if ratio >= 0.999 else "OVERDRIVE CHARGING"


func _on_time_changed(value: float) -> void:
	var seconds := int(ceil(value))
	time_label.text = "TIME  00:%02d" % max(seconds, 0)


func _on_combo_changed(value: int, multiplier_value: int) -> void:
	wave_label.text = "CHAIN  %02d   MULTI  x%d   THREAT  WAVE %d" % [value, multiplier_value, _wave_from_time()]


func _on_status_changed(text: String) -> void:
	status_label.text = text if text != "" else "드래그로 적 탄막을 비우고 연쇄 점수를 유지하세요."


func _on_overdrive_pressed() -> void:
	battlefield.request_overdrive()


func _on_retreat_pressed() -> void:
	retreat_pressed.emit()


func _on_battlefield_finished(result: Dictionary) -> void:
	run_finished.emit(result)


func _set_bar_value(fill: ColorRect, ratio: float) -> void:
	fill.size.x = BAR_WIDTH * clampf(ratio, 0.0, 1.0)


func _wave_from_time() -> int:
	var time_text := time_label.text
	if time_text.is_empty():
		return 1
	var value_text := time_text.get_slice(":", 1)
	var seconds_left := int(value_text)
	return clampi(int((45 - seconds_left) / 9) + 1, 1, 5)


func _format_score(value: int) -> String:
	var digits := str(max(value, 0))
	var parts := []
	while digits.length() > 3:
		parts.push_front(digits.substr(digits.length() - 3, 3))
		digits = digits.substr(0, digits.length() - 3)
	parts.push_front(digits)
	return ",".join(parts)

