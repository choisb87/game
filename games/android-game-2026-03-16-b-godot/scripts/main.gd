extends Control

enum FlowState { MENU, PLAYING, RESULT }

const BODY_TEXT := Color(0.79, 0.88, 0.82, 0.95)
const SOFT_TEXT := Color(0.69, 0.77, 0.72, 0.92)
const ACCENT_TEXT := Color(0.97, 0.94, 0.82, 1.0)
const WARNING_TEXT := Color(0.96, 0.79, 0.46, 1.0)
const DANGER_TEXT := Color(0.97, 0.57, 0.49, 1.0)

var levels := []
var current_level_index := 0
var flow_state := FlowState.MENU
var campaign_started := false
var menu_primary_action := "start"
var result_primary_action := ""
var result_secondary_action := ""
var last_result_kind := ""

@onready var title_label: Label = $SafeArea/Layout/TopPanel/TopContent/HeaderRow/TitleLabel
@onready var progress_label: Label = $SafeArea/Layout/TopPanel/TopContent/HeaderRow/ProgressLabel
@onready var subtitle_label: Label = $SafeArea/Layout/TopPanel/TopContent/SubtitleLabel
@onready var objective_label: Label = $SafeArea/Layout/TopPanel/TopContent/ObjectiveLabel
@onready var level_label: Label = $SafeArea/Layout/TopPanel/TopContent/StatsRow/LevelLabel
@onready var move_label: Label = $SafeArea/Layout/TopPanel/TopContent/StatsRow/MoveLabel
@onready var status_label: Label = $SafeArea/Layout/TopPanel/TopContent/StatsRow/StatusLabel
@onready var board_view = $SafeArea/Layout/Center/BoardView
@onready var result_dim: ColorRect = $SafeArea/Layout/Center/ResultDim
@onready var result_card: PanelContainer = $SafeArea/Layout/Center/ResultCard
@onready var result_eyebrow: Label = $SafeArea/Layout/Center/ResultCard/ResultContent/ResultEyebrow
@onready var result_title: Label = $SafeArea/Layout/Center/ResultCard/ResultContent/ResultTitle
@onready var result_text: Label = $SafeArea/Layout/Center/ResultCard/ResultContent/ResultText
@onready var result_content: VBoxContainer = $SafeArea/Layout/Center/ResultCard/ResultContent
@onready
var result_buttons: HBoxContainer = result_content.get_node("ResultButtons") as HBoxContainer
@onready
var result_secondary_button: Button = result_buttons.get_node("ResultSecondaryButton") as Button
@onready
var result_primary_button: Button = result_buttons.get_node("ResultPrimaryButton") as Button
@onready var bottom_content: VBoxContainer = $SafeArea/Layout/BottomPanel/BottomContent
@onready var hint_label: Label = bottom_content.get_node("HintLabel") as Label
@onready var feedback_label: Label = bottom_content.get_node("FeedbackLabel") as Label
@onready var bottom_buttons: HBoxContainer = bottom_content.get_node("Buttons") as HBoxContainer
@onready var menu_button: Button = bottom_buttons.get_node("MenuButton") as Button
@onready var restart_button: Button = bottom_buttons.get_node("RestartButton") as Button
@onready var overlay_layer: Control = $OverlayLayer
@onready var menu_card: PanelContainer = $OverlayLayer/MenuCard
@onready var menu_eyebrow: Label = $OverlayLayer/MenuCard/MenuContent/MenuEyebrow
@onready var menu_title: Label = $OverlayLayer/MenuCard/MenuContent/MenuTitle
@onready var menu_text: Label = $OverlayLayer/MenuCard/MenuContent/MenuText
@onready var menu_rules: Label = $OverlayLayer/MenuCard/MenuContent/MenuRules
@onready var menu_buttons: HBoxContainer = $OverlayLayer/MenuCard/MenuContent/MenuButtons
@onready var menu_secondary_button: Button = menu_buttons.get_node("MenuSecondaryButton") as Button
@onready var start_button: Button = menu_buttons.get_node("StartButton") as Button
@onready var top_panel: PanelContainer = $SafeArea/Layout/TopPanel
@onready var bottom_panel: PanelContainer = $SafeArea/Layout/BottomPanel


func _ready() -> void:
	levels = _build_levels()
	_apply_skin()

	board_view.turns_changed.connect(_on_turns_changed)
	board_view.blooms_changed.connect(_on_blooms_changed)
	board_view.level_completed.connect(_on_level_completed)
	board_view.level_failed.connect(_on_level_failed)
	board_view.interaction_feedback.connect(_on_board_feedback)

	menu_button.pressed.connect(_on_menu_pressed)
	restart_button.pressed.connect(_on_restart_pressed)
	start_button.pressed.connect(_on_start_pressed)
	menu_secondary_button.pressed.connect(_on_menu_restart_pressed)
	result_primary_button.pressed.connect(_on_result_primary_pressed)
	result_secondary_button.pressed.connect(_on_result_secondary_pressed)

	_load_level(0)
	_enter_menu()


func _load_level(index: int) -> void:
	current_level_index = clampi(index, 0, levels.size() - 1)
	var level: Dictionary = levels[current_level_index] as Dictionary

	_hide_result()

	title_label.text = "Glass Garden"
	progress_label.text = "구획 %d / %d" % [current_level_index + 1, levels.size()]
	subtitle_label.text = String(level.get("tagline", ""))
	level_label.text = String(level.get("title", ""))
	hint_label.text = String(level.get("hint", ""))
	objective_label.text = "목표: 꽃 %d송이를 모두 피우세요." % (level.get("blooms", []) as Array).size()
	_set_feedback("광원 방향을 먼저 읽고, 필요한 거울만 돌리세요.", false)

	board_view.setup_level(level)
	_refresh_menu_copy()


func _enter_menu() -> void:
	var previous_state := flow_state
	if not campaign_started:
		menu_primary_action = "start"
	elif previous_state == FlowState.RESULT:
		match last_result_kind:
			"win":
				menu_primary_action = "next"
			"campaign_complete":
				menu_primary_action = "replay_campaign"
			_:
				menu_primary_action = "retry"
	else:
		menu_primary_action = "resume"

	flow_state = FlowState.MENU
	board_view.set_input_enabled(false)
	_hide_result()
	overlay_layer.visible = true
	_refresh_menu_copy()
	_sync_buttons()
	_fade_in(menu_card)


func _start_play(reset_to_first: bool) -> void:
	if reset_to_first:
		_load_level(0)

	campaign_started = true
	flow_state = FlowState.PLAYING
	overlay_layer.visible = false
	_hide_result()
	board_view.set_input_enabled(true)
	_sync_buttons()
	_set_feedback("빛이 꽃을 모두 지나가도록 경로를 완성하세요.", false)


func _show_result(kind: String) -> void:
	last_result_kind = kind
	flow_state = FlowState.RESULT
	board_view.set_input_enabled(false)
	result_dim.visible = true
	result_card.visible = true
	result_secondary_button.visible = true

	match kind:
		"win":
			result_eyebrow.text = "구획 %d 완료" % (current_level_index + 1)
			result_title.text = "온실 복원"
			result_text.text = "빛이 안정적으로 흐릅니다. 다음 구획으로 이동하거나 같은 퍼즐을 다시 풀 수 있습니다."
			result_secondary_button.text = "한 번 더"
			result_primary_button.text = "다음 구획"
			result_secondary_action = "retry"
			result_primary_action = "next"
		"campaign_complete":
			result_eyebrow.text = "최종 복원 완료"
			result_title.text = "정원이 다시 숨을 쉽니다"
			result_text.text = "모든 구획을 복원했습니다. 처음부터 다시 플레이하거나 메뉴로 돌아가세요."
			result_secondary_button.text = "메뉴"
			result_primary_button.text = "처음부터"
			result_secondary_action = "menu"
			result_primary_action = "replay_campaign"
		_:
			result_eyebrow.text = "회전 제한 소진"
			result_title.text = "빛이 끊겼습니다"
			result_text.text = "남은 회전 없이 경로가 멈췄습니다. 같은 구획을 다시 시도하거나 메뉴로 돌아갈 수 있습니다."
			result_secondary_button.text = "메뉴"
			result_primary_button.text = "다시 시도"
			result_secondary_action = "menu"
			result_primary_action = "retry"

	_sync_buttons()
	_fade_in(result_card)


func _hide_result() -> void:
	result_dim.visible = false
	result_card.visible = false
	result_primary_action = ""
	result_secondary_action = ""


func _refresh_menu_copy() -> void:
	var level: Dictionary = levels[current_level_index] as Dictionary
	var preview_index := current_level_index
	if menu_primary_action == "next":
		preview_index = clampi(current_level_index + 1, 0, levels.size() - 1)
	elif menu_primary_action == "replay_campaign":
		preview_index = 0
	var preview_level: Dictionary = levels[preview_index] as Dictionary
	menu_title.text = "Glass Garden"

	if campaign_started and flow_state != FlowState.RESULT:
		match menu_primary_action:
			"resume":
				menu_eyebrow.text = "메뉴"
				menu_text.text = (
					"%s 구획을 진행 중입니다. 이어서 플레이하거나 처음부터 다시 시작할 수 있습니다."
					% String(level.get("title", ""))
				)
				start_button.text = "이어하기"
				menu_secondary_button.visible = true
			"next":
				menu_eyebrow.text = "구획 복원 완료"
				menu_text.text = (
					"%s을 복원했습니다. 다음 구획으로 이어가거나 캠페인을 처음부터 다시 시작할 수 있습니다."
					% String(level.get("title", ""))
				)
				start_button.text = "다음 구획"
				menu_secondary_button.visible = true
			"retry":
				menu_eyebrow.text = "재도전 준비"
				menu_text.text = (
					"%s 구획을 다시 정렬할 수 있습니다. 같은 퍼즐을 재도전하거나 처음부터 다시 시작하세요."
					% String(level.get("title", ""))
				)
				start_button.text = "같은 구획 다시"
				menu_secondary_button.visible = true
			"replay_campaign":
				menu_eyebrow.text = "캠페인 완료"
				menu_text.text = "모든 구획을 복원했습니다. 처음부터 다시 플레이해 전체 흐름을 한 번 더 돌릴 수 있습니다."
				start_button.text = "처음부터"
				menu_secondary_button.visible = false
			_:
				menu_eyebrow.text = "퍼즐 캠페인"
				menu_text.text = "빛의 경로를 정렬해 다섯 개의 유리 온실 구획을 차례대로 복원하세요."
				start_button.text = "플레이 시작"
				menu_secondary_button.visible = campaign_started
	else:
		menu_eyebrow.text = "퍼즐 캠페인"
		menu_text.text = "빛의 경로를 정렬해 다섯 개의 유리 온실 구획을 차례대로 복원하세요."
		start_button.text = "플레이 시작"
		menu_secondary_button.visible = false

	menu_rules.text = (
		"준비 구획: %d / %d\n이번 목표: 꽃 %d송이 개화\n조작: 거울 탭으로 90도 회전"
		% [preview_index + 1, levels.size(), (preview_level.get("blooms", []) as Array).size()]
	)


func _sync_buttons() -> void:
	var is_playing := flow_state == FlowState.PLAYING
	menu_button.disabled = not is_playing
	restart_button.disabled = not is_playing


func _on_turns_changed(turns_left: int) -> void:
	move_label.text = "남은 회전 %d" % turns_left

	var color := SOFT_TEXT
	if turns_left <= 0:
		color = DANGER_TEXT
	elif turns_left == 1:
		color = WARNING_TEXT
	move_label.label_settings.font_color = color

	if flow_state == FlowState.PLAYING:
		if turns_left == 1:
			_set_feedback("마지막 회전입니다. 경로를 다시 확인하세요.", true)
		elif turns_left == 0:
			_set_feedback("회전 기회를 모두 사용했습니다.", true)


func _on_blooms_changed(lit_count: int, total_count: int) -> void:
	status_label.text = "개화 %d/%d" % [lit_count, total_count]
	objective_label.text = "목표 진행: 꽃 %d/%d송이 개화" % [lit_count, total_count]

	var color := BODY_TEXT
	if lit_count == total_count and total_count > 0:
		color = ACCENT_TEXT
		objective_label.text = "목표 달성: 모든 꽃이 빛을 받았습니다."
	status_label.label_settings.font_color = color
	objective_label.label_settings.font_color = color


func _on_level_completed() -> void:
	_set_feedback("모든 꽃이 개화했습니다. 다음 구획으로 넘어갈 수 있습니다.", true)
	if current_level_index == levels.size() - 1:
		_show_result("campaign_complete")
	else:
		_show_result("win")


func _on_level_failed() -> void:
	objective_label.label_settings.font_color = DANGER_TEXT
	_set_feedback("이번 구획은 실패했습니다. 배치를 다시 읽고 재도전하세요.", true)
	_show_result("lose")


func _on_board_feedback(message: String, emphasized: bool) -> void:
	if flow_state == FlowState.PLAYING:
		_set_feedback(message, emphasized)


func _set_feedback(message: String, emphasized: bool) -> void:
	feedback_label.text = message
	feedback_label.label_settings.font_color = ACCENT_TEXT if emphasized else BODY_TEXT


func _on_menu_pressed() -> void:
	_enter_menu()


func _on_restart_pressed() -> void:
	_load_level(current_level_index)
	flow_state = FlowState.PLAYING
	board_view.set_input_enabled(true)
	_sync_buttons()


func _on_start_pressed() -> void:
	match menu_primary_action:
		"resume":
			_start_play(false)
		"next":
			_load_level(current_level_index + 1)
			_start_play(false)
		"retry":
			_load_level(current_level_index)
			_start_play(false)
		"replay_campaign":
			_start_play(true)
		_:
			_start_play(false)


func _on_menu_restart_pressed() -> void:
	_start_play(true)


func _on_result_primary_pressed() -> void:
	match result_primary_action:
		"next":
			_load_level(current_level_index + 1)
			_start_play(false)
		"retry":
			_load_level(current_level_index)
			_start_play(false)
		"replay_campaign":
			_start_play(true)


func _on_result_secondary_pressed() -> void:
	match result_secondary_action:
		"retry":
			_load_level(current_level_index)
			_start_play(false)
		"menu":
			_enter_menu()


func _fade_in(card: Control) -> void:
	card.modulate = Color(1.0, 1.0, 1.0, 0.0)
	var tween := create_tween()
	tween.set_trans(Tween.TRANS_QUAD).set_ease(Tween.EASE_OUT)
	tween.tween_property(card, "modulate", Color(1.0, 1.0, 1.0, 1.0), 0.18)


func _build_levels() -> Array:
	return [
		{
			"title": "새벽 온실",
			"tagline": "첫 번째 빛을 두 송이의 꽃까지 연결하세요.",
			"hint": "거울을 탭하면 90° 회전합니다. 빛이 두 꽃을 모두 지나도록 경로를 만드세요.",
			"grid_size": 4,
			"moves": 4,
			"sources": [{"side": "top", "index": 1, "color": Color("ffd972")}],
			"blooms": [Vector2i(1, 1), Vector2i(3, 1)],
			"pieces":
			[
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
			"sources": [{"side": "left", "index": 3, "color": Color("ffc96c")}],
			"blooms": [Vector2i(2, 3), Vector2i(3, 2), Vector2i(4, 1)],
			"pieces":
			[
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
			"sources": [{"side": "top", "index": 2, "color": Color("ffe48f")}],
			"blooms": [Vector2i(2, 1), Vector2i(3, 2), Vector2i(1, 4)],
			"pieces":
			[
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
			"sources":
			[
				{"side": "top", "index": 1, "color": Color("ffdf88")},
				{"side": "left", "index": 4, "color": Color("bfe7ff")}
			],
			"blooms": [Vector2i(1, 2), Vector2i(4, 2), Vector2i(2, 4), Vector2i(2, 0)],
			"pieces":
			[
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
			"sources":
			[
				{"side": "bottom", "index": 1, "color": Color("ffd66b")},
				{"side": "right", "index": 4, "color": Color("cdefff")}
			],
			"blooms":
			[Vector2i(1, 4), Vector2i(4, 3), Vector2i(5, 5), Vector2i(3, 4), Vector2i(3, 1)],
			"pieces":
			[
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
	var floating_style := _panel_style(Color(0.10, 0.16, 0.14, 0.92), Color(1.0, 0.95, 0.78, 0.20))

	top_panel.add_theme_stylebox_override("panel", panel_style)
	bottom_panel.add_theme_stylebox_override("panel", panel_style)
	menu_card.add_theme_stylebox_override("panel", floating_style)
	result_card.add_theme_stylebox_override("panel", floating_style)

	title_label.label_settings = _label_settings(42, ACCENT_TEXT)
	progress_label.label_settings = _label_settings(15, SOFT_TEXT)
	subtitle_label.label_settings = _label_settings(18, BODY_TEXT)
	objective_label.label_settings = _label_settings(16, BODY_TEXT)
	level_label.label_settings = _label_settings(16, SOFT_TEXT)
	move_label.label_settings = _label_settings(16, SOFT_TEXT)
	status_label.label_settings = _label_settings(16, BODY_TEXT)
	hint_label.label_settings = _label_settings(17, BODY_TEXT)
	feedback_label.label_settings = _label_settings(16, BODY_TEXT)
	menu_eyebrow.label_settings = _label_settings(14, SOFT_TEXT)
	menu_title.label_settings = _label_settings(34, ACCENT_TEXT)
	menu_text.label_settings = _label_settings(18, BODY_TEXT)
	menu_rules.label_settings = _label_settings(16, SOFT_TEXT)
	result_eyebrow.label_settings = _label_settings(14, SOFT_TEXT)
	result_title.label_settings = _label_settings(28, ACCENT_TEXT)
	result_text.label_settings = _label_settings(17, BODY_TEXT)

	menu_button.custom_minimum_size = Vector2(0, 58)
	restart_button.custom_minimum_size = Vector2(0, 58)
	menu_secondary_button.custom_minimum_size = Vector2(0, 58)
	start_button.custom_minimum_size = Vector2(0, 58)
	result_secondary_button.custom_minimum_size = Vector2(0, 58)
	result_primary_button.custom_minimum_size = Vector2(0, 58)

	_apply_button_skin(menu_button, false)
	_apply_button_skin(restart_button, false)
	_apply_button_skin(menu_secondary_button, false)
	_apply_button_skin(result_secondary_button, false)
	_apply_button_skin(start_button, true)
	_apply_button_skin(result_primary_button, true)


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
