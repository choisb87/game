extends Control

var levels := []

var current_level_index := 0

@onready var title_label: Label = $SafeArea/Layout/TopPanel/TopContent/TitleLabel
@onready var subtitle_label: Label = $SafeArea/Layout/TopPanel/TopContent/SubtitleLabel
@onready var level_label: Label = $SafeArea/Layout/TopPanel/TopContent/StatsRow/LevelLabel
@onready var move_label: Label = $SafeArea/Layout/TopPanel/TopContent/StatsRow/MoveLabel
@onready var status_label: Label = $SafeArea/Layout/TopPanel/TopContent/StatsRow/StatusLabel
@onready var board_view = $SafeArea/Layout/Center/BoardView
@onready var completion_card: PanelContainer = $SafeArea/Layout/Center/CompletionCard
@onready var completion_title: Label = (
	$SafeArea/Layout/Center/CompletionCard/CompletionContent/CompletionTitle
)
@onready var completion_text: Label = (
	$SafeArea/Layout/Center/CompletionCard/CompletionContent/CompletionText
)
@onready var hint_label: Label = $SafeArea/Layout/BottomPanel/BottomContent/HintLabel
@onready var restart_button: Button = (
	$SafeArea/Layout/BottomPanel/BottomContent/Buttons/RestartButton
)
@onready var next_button: Button = $SafeArea/Layout/BottomPanel/BottomContent/Buttons/NextButton
@onready var top_panel: PanelContainer = $SafeArea/Layout/TopPanel
@onready var bottom_panel: PanelContainer = $SafeArea/Layout/BottomPanel


func _ready() -> void:
	levels = _build_levels()
	_apply_skin()

	board_view.turns_changed.connect(_on_turns_changed)
	board_view.blooms_changed.connect(_on_blooms_changed)
	board_view.level_completed.connect(_on_level_completed)
	board_view.level_failed.connect(_on_level_failed)

	restart_button.pressed.connect(_on_restart_pressed)
	next_button.pressed.connect(_on_next_pressed)

	_load_level(0)


func _load_level(index: int) -> void:
	current_level_index = clampi(index, 0, levels.size() - 1)
	var level: Dictionary = levels[current_level_index] as Dictionary

	completion_card.visible = false
	next_button.visible = false
	next_button.text = "처음부터" if current_level_index == levels.size() - 1 else "다음 구획"

	title_label.text = "Glass Garden"
	subtitle_label.text = String(level.get("tagline", ""))
	level_label.text = "LEVEL %d · %s" % [current_level_index + 1, String(level.get("title", ""))]
	hint_label.text = String(level.get("hint", ""))
	status_label.text = "개화 0/%d" % (level.get("blooms", []) as Array).size()

	board_view.setup_level(level)


func _on_turns_changed(turns_left: int) -> void:
	move_label.text = "남은 회전 %d" % turns_left


func _on_blooms_changed(lit_count: int, total_count: int) -> void:
	status_label.text = "개화 %d/%d" % [lit_count, total_count]


func _on_level_completed() -> void:
	completion_card.visible = true
	completion_title.text = "온실 복원"
	if current_level_index == levels.size() - 1:
		completion_text.text = "모든 유리 정원이 다시 숨을 쉽니다. 처음부터 다시 플레이할 수 있습니다."
	else:
		completion_text.text = "빛이 안정적으로 흐릅니다. 다음 구획으로 이동하세요."
	next_button.visible = true


func _on_level_failed() -> void:
	completion_card.visible = true
	completion_title.text = "빛이 끊겼습니다"
	completion_text.text = "회전 수를 모두 사용했습니다. 거울을 다시 정렬해 경로를 복구하세요."
	next_button.visible = false


func _on_restart_pressed() -> void:
	_load_level(current_level_index)


func _on_next_pressed() -> void:
	if current_level_index == levels.size() - 1:
		_load_level(0)
	else:
		_load_level(current_level_index + 1)


func _build_levels() -> Array:
	return [
		{
			"title": "새벽 온실",
			"tagline": "첫 번째 빛을 두 송이의 꽃까지 연결하세요.",
			"hint": "빛은 빈 유리를 통과하고, 금빛 거울에서 꺾입니다.",
			"grid_size": 4,
			"moves": 4,
			"sources": [
				{"side": "top", "index": 1, "color": Color("ffd972")}
			],
			"blooms": [Vector2i(1, 1), Vector2i(3, 1)],
			"pieces": [
				{"pos": Vector2i(1, 2), "type": "mirror", "rotation": 1},
				{"pos": Vector2i(3, 2), "type": "mirror", "rotation": 0}
			]
		},
		{
			"title": "호박빛 아치",
			"tagline": "한 줄의 빛을 세 번의 개화로 이어지는 곡선으로 만드세요.",
			"hint": "먼저 아래쪽 곡선을 만든 뒤 위쪽 회전을 맞추면 안정적입니다.",
			"grid_size": 5,
			"moves": 5,
			"sources": [
				{"side": "left", "index": 3, "color": Color("ffc96c")}
			],
			"blooms": [Vector2i(2, 3), Vector2i(3, 2), Vector2i(4, 1)],
			"pieces": [
				{"pos": Vector2i(3, 3), "type": "mirror", "rotation": 0},
				{"pos": Vector2i(3, 1), "type": "mirror", "rotation": 0},
				{"pos": Vector2i(1, 0), "type": "blocker", "rotation": 0, "locked": true}
			]
		},
		{
			"title": "유리 회랑",
			"tagline": "긴 회랑 끝에서 마지막 꽃까지 빛을 끌고 가야 합니다.",
			"hint": "오른쪽 끝까지 먼저 보낸 뒤 아래로 한 번, 다시 왼쪽으로 흘러갑니다.",
			"grid_size": 5,
			"moves": 6,
			"sources": [
				{"side": "top", "index": 2, "color": Color("ffe48f")}
			],
			"blooms": [Vector2i(2, 1), Vector2i(3, 2), Vector2i(1, 4)],
			"pieces": [
				{"pos": Vector2i(2, 2), "type": "mirror", "rotation": 1},
				{"pos": Vector2i(4, 2), "type": "mirror", "rotation": 1},
				{"pos": Vector2i(4, 4), "type": "mirror", "rotation": 0},
				{"pos": Vector2i(0, 2), "type": "blocker", "rotation": 0, "locked": true},
				{"pos": Vector2i(1, 0), "type": "blocker", "rotation": 0, "locked": true}
			]
		},
		{
			"title": "이중 채광실",
			"tagline": "두 개의 광원을 교차시키며 온실의 상하층을 동시에 밝히세요.",
			"hint": "두 경로는 서로 방해하지 않습니다. 위쪽과 왼쪽 광원을 따로 읽으세요.",
			"grid_size": 6,
			"moves": 6,
			"sources": [
				{"side": "top", "index": 1, "color": Color("ffdf88")},
				{"side": "left", "index": 4, "color": Color("bfe7ff")}
			],
			"blooms": [Vector2i(1, 2), Vector2i(4, 2), Vector2i(2, 4), Vector2i(2, 0)],
			"pieces": [
				{"pos": Vector2i(1, 3), "type": "mirror", "rotation": 1},
				{"pos": Vector2i(4, 3), "type": "mirror", "rotation": 0},
				{"pos": Vector2i(3, 4), "type": "mirror", "rotation": 0},
				{"pos": Vector2i(3, 0), "type": "mirror", "rotation": 0, "locked": true},
				{"pos": Vector2i(5, 5), "type": "blocker", "rotation": 0, "locked": true}
			]
		},
		{
			"title": "황혼 보존동",
			"tagline": "마지막 구획은 두 광선을 길게 감아 다섯 송이를 모두 피워야 합니다.",
			"hint": "아래에서 올라오는 빛과 오른쪽에서 들어오는 빛이 각각 다른 꽃 묶음을 담당합니다.",
			"grid_size": 6,
			"moves": 7,
			"sources": [
				{"side": "bottom", "index": 1, "color": Color("ffd66b")},
				{"side": "right", "index": 4, "color": Color("cdefff")}
			],
			"blooms": [Vector2i(1, 4), Vector2i(4, 3), Vector2i(5, 5), Vector2i(3, 4), Vector2i(3, 1)],
			"pieces": [
				{"pos": Vector2i(1, 3), "type": "mirror", "rotation": 0},
				{"pos": Vector2i(5, 3), "type": "mirror", "rotation": 0, "locked": true},
				{"pos": Vector2i(2, 4), "type": "mirror", "rotation": 1},
				{"pos": Vector2i(2, 1), "type": "mirror", "rotation": 0},
				{"pos": Vector2i(0, 0), "type": "blocker", "rotation": 0, "locked": true}
			]
		}
	]


func _apply_skin() -> void:
	var panel_style := _panel_style(Color(0.09, 0.15, 0.14, 0.76), Color(0.82, 0.90, 0.84, 0.14))
	var floating_style := _panel_style(Color(0.10, 0.16, 0.14, 0.90), Color(1.0, 0.95, 0.78, 0.20))
	var subtle_settings := _label_settings(18, Color(0.79, 0.88, 0.82, 0.95))
	var small_settings := _label_settings(15, Color(0.69, 0.77, 0.72, 0.92))
	var title_settings := _label_settings(42, Color(0.95, 0.93, 0.82, 1.0))

	top_panel.add_theme_stylebox_override("panel", panel_style)
	bottom_panel.add_theme_stylebox_override("panel", panel_style)
	completion_card.add_theme_stylebox_override("panel", floating_style)

	title_label.label_settings = title_settings
	subtitle_label.label_settings = subtle_settings
	level_label.label_settings = small_settings
	move_label.label_settings = small_settings
	status_label.label_settings = small_settings
	hint_label.label_settings = subtle_settings
	completion_title.label_settings = _label_settings(26, Color(0.97, 0.94, 0.86, 1.0))
	completion_text.label_settings = _label_settings(16, Color(0.80, 0.87, 0.82, 0.96))

	restart_button.custom_minimum_size = Vector2(0, 58)
	next_button.custom_minimum_size = Vector2(0, 58)
	_apply_button_skin(restart_button, false)
	_apply_button_skin(next_button, true)


func _apply_button_skin(button: Button, accent: bool) -> void:
	var normal_bg := Color(0.14, 0.21, 0.19, 0.92)
	var hover_bg := Color(0.18, 0.27, 0.23, 0.98)
	var accent_bg := Color(0.36, 0.29, 0.15, 0.96)
	var accent_hover := Color(0.45, 0.36, 0.17, 0.98)
	var border := Color(0.82, 0.90, 0.84, 0.18)
	var accent_border := Color(0.99, 0.90, 0.62, 0.34)

	button.add_theme_stylebox_override(
		"normal",
		_panel_style(accent_bg if accent else normal_bg, accent_border if accent else border, 18)
	)
	button.add_theme_stylebox_override(
		"hover",
		_panel_style(accent_hover if accent else hover_bg, accent_border if accent else border, 18)
	)
	button.add_theme_stylebox_override(
		"pressed",
		_panel_style(Color(0.12, 0.18, 0.16, 0.98), accent_border if accent else border, 18)
	)
	button.add_theme_color_override("font_color", Color(0.96, 0.94, 0.88, 1.0))
	button.add_theme_color_override("font_pressed_color", Color(1.0, 0.98, 0.92, 1.0))
	button.add_theme_font_size_override("font_size", 18)


func _panel_style(background: Color, border: Color, radius: int = 24) -> StyleBoxFlat:
	var style := StyleBoxFlat.new()
	style.bg_color = background
	style.border_color = border
	style.border_width_left = 1
	style.border_width_top = 1
	style.border_width_right = 1
	style.border_width_bottom = 1
	style.corner_radius_top_left = radius
	style.corner_radius_top_right = radius
	style.corner_radius_bottom_right = radius
	style.corner_radius_bottom_left = radius
	style.content_margin_left = 22
	style.content_margin_top = 18
	style.content_margin_right = 22
	style.content_margin_bottom = 18
	return style


func _label_settings(size_px: int, color: Color) -> LabelSettings:
	var settings := LabelSettings.new()
	settings.font_size = size_px
	settings.font_color = color
	return settings
