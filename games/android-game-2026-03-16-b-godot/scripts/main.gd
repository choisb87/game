extends Control

enum FlowState { MENU, PLAYING, RESULT }

const BODY_TEXT := Color(0.86, 0.91, 0.89, 0.98)
const SOFT_TEXT := Color(0.68, 0.79, 0.75, 0.98)
const ACCENT_TEXT := Color(0.98, 0.95, 0.84, 1.0)
const WARNING_TEXT := Color(0.98, 0.83, 0.47, 1.0)
const DANGER_TEXT := Color(0.97, 0.57, 0.49, 1.0)
const SHADOW_TEXT := Color(0.03, 0.05, 0.05, 0.72)

var levels := []
var current_level_index := 0
var flow_state := FlowState.MENU
var campaign_started := false
var menu_primary_action := "start"
var result_primary_action := ""
var result_secondary_action := ""
var last_result_kind := ""

@onready var background_layer: Control = $Background
@onready var safe_area: MarginContainer = $SafeArea
@onready var eyebrow_label: Label = $SafeArea/Layout/TopPanel/TopContent/EyebrowLabel
@onready var title_label: Label = $SafeArea/Layout/TopPanel/TopContent/HeaderRow/TitleStack/TitleLabel
@onready
var subtitle_label: Label = $SafeArea/Layout/TopPanel/TopContent/HeaderRow/TitleStack/SubtitleLabel
@onready
var progress_badge: PanelContainer = $SafeArea/Layout/TopPanel/TopContent/HeaderRow/ProgressBadge
@onready
var progress_label: Label = (
	$SafeArea/Layout/TopPanel/TopContent/HeaderRow/ProgressBadge/ProgressBadgeText
)
@onready
var objective_panel: PanelContainer = $SafeArea/Layout/TopPanel/TopContent/ObjectivePanel
@onready
var objective_eyebrow: Label = (
	$SafeArea/Layout/TopPanel/TopContent/ObjectivePanel/ObjectiveContent/ObjectiveEyebrow
)
@onready
var objective_label: Label = $SafeArea/Layout/TopPanel/TopContent/ObjectivePanel/ObjectiveContent/ObjectiveLabel
@onready
var stage_card: PanelContainer = $SafeArea/Layout/TopPanel/TopContent/StatsRow/StageCard
@onready
var level_label: Label = $SafeArea/Layout/TopPanel/TopContent/StatsRow/StageCard/StageCardContent/LevelLabel
@onready
var move_card: PanelContainer = $SafeArea/Layout/TopPanel/TopContent/StatsRow/MoveCard
@onready
var move_label: Label = $SafeArea/Layout/TopPanel/TopContent/StatsRow/MoveCard/MoveCardContent/MoveLabel
@onready
var bloom_card: PanelContainer = $SafeArea/Layout/TopPanel/TopContent/StatsRow/BloomCard
@onready
var status_label: Label = $SafeArea/Layout/TopPanel/TopContent/StatsRow/BloomCard/BloomCardContent/StatusLabel
@onready var board_view = $SafeArea/Layout/Center/BoardView
@onready var hint_card: PanelContainer = $SafeArea/Layout/BottomPanel/BottomContent/HintCard
@onready
var hint_label: Label = $SafeArea/Layout/BottomPanel/BottomContent/HintCard/HintContent/HintLabel
@onready
var feedback_card: PanelContainer = $SafeArea/Layout/BottomPanel/BottomContent/FeedbackCard
@onready
var feedback_label: Label = $SafeArea/Layout/BottomPanel/BottomContent/FeedbackCard/FeedbackLabel
@onready var menu_button: Button = $SafeArea/Layout/BottomPanel/BottomContent/Buttons/MenuButton
@onready var restart_button: Button = $SafeArea/Layout/BottomPanel/BottomContent/Buttons/RestartButton
@onready var result_layer: Control = $ResultLayer
@onready var result_dim: ColorRect = $ResultLayer/ResultDim
@onready var result_card: PanelContainer = $ResultLayer/ResultCard
@onready var result_eyebrow: Label = $ResultLayer/ResultCard/ResultContent/ResultEyebrow
@onready var result_title: Label = $ResultLayer/ResultCard/ResultContent/ResultTitle
@onready var result_text: Label = $ResultLayer/ResultCard/ResultContent/ResultText
@onready var result_meta: Label = $ResultLayer/ResultCard/ResultContent/ResultMeta
@onready
var result_secondary_button: Button = (
	$ResultLayer/ResultCard/ResultContent/ResultButtons/ResultSecondaryButton
)
@onready
var result_primary_button: Button = (
	$ResultLayer/ResultCard/ResultContent/ResultButtons/ResultPrimaryButton
)
@onready var overlay_layer: Control = $OverlayLayer
@onready var menu_card: PanelContainer = $OverlayLayer/MenuCard
@onready var menu_mark: TextureRect = $OverlayLayer/MenuCard/MenuContent/MenuMarkWrap/MenuMark
@onready var menu_eyebrow: Label = $OverlayLayer/MenuCard/MenuContent/MenuEyebrow
@onready var menu_title: Label = $OverlayLayer/MenuCard/MenuContent/MenuTitle
@onready var menu_text: Label = $OverlayLayer/MenuCard/MenuContent/MenuText
@onready
var menu_stage_card: PanelContainer = $OverlayLayer/MenuCard/MenuContent/MenuStageCard
@onready
var menu_stage_summary: Label = $OverlayLayer/MenuCard/MenuContent/MenuStageCard/MenuStageSummary
@onready var menu_rules: Label = $OverlayLayer/MenuCard/MenuContent/MenuRules
@onready
var menu_secondary_button: Button = (
	$OverlayLayer/MenuCard/MenuContent/MenuButtons/MenuSecondaryButton
)
@onready var start_button: Button = $OverlayLayer/MenuCard/MenuContent/MenuButtons/StartButton
@onready var top_panel: PanelContainer = $SafeArea/Layout/TopPanel
@onready var bottom_panel: PanelContainer = $SafeArea/Layout/BottomPanel


func _ready() -> void:
	levels = _build_levels()
	_connect_signals()
	_apply_skin()
	_load_level(0)
	_enter_menu(false)
	_update_responsive_layout()


func _notification(what: int) -> void:
	if what == NOTIFICATION_RESIZED and is_node_ready():
		_update_responsive_layout()


func _unhandled_input(event: InputEvent) -> void:
	if not event.is_action_pressed("ui_cancel"):
		return

	match flow_state:
		FlowState.PLAYING:
			_enter_menu()
			get_viewport().set_input_as_handled()
		FlowState.RESULT:
			_enter_menu()
			get_viewport().set_input_as_handled()
		FlowState.MENU:
			if campaign_started and menu_primary_action == "resume":
				_start_play(false)
				get_viewport().set_input_as_handled()


func _connect_signals() -> void:
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

	for button in [
		menu_button,
		restart_button,
		menu_secondary_button,
		start_button,
		result_secondary_button,
		result_primary_button
	]:
		button.focus_mode = Control.FOCUS_NONE
		button.button_down.connect(_on_button_down.bind(button))
		button.button_up.connect(_on_button_up.bind(button))


func _load_level(index: int) -> void:
	current_level_index = clampi(index, 0, levels.size() - 1)
	var level: Dictionary = levels[current_level_index] as Dictionary

	_hide_result()
	_apply_level_theme(level)
	board_view.setup_level(level)

	title_label.text = "Glass Garden"
	eyebrow_label.text = String(level.get("region", "BOTANICAL LIGHT ROUTING"))
	subtitle_label.text = String(level.get("tagline", ""))
	level_label.text = String(level.get("title", ""))
	hint_label.text = String(level.get("hint", ""))
	objective_eyebrow.text = "이번 목표"
	objective_label.text = "꽃 %d송이를 모두 빛이 지나가게 하세요." % board_view.get_total_blooms()

	_refresh_progress_copy()
	_refresh_menu_copy()
	_refresh_result_meta()
	_set_feedback(String(level.get("intro_feedback", "")), false)


func _enter_menu(animate: bool = true) -> void:
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
	if animate:
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
	_refresh_progress_copy()
	_set_feedback("광원 입구를 먼저 읽고, 필요한 거울만 회전시키세요.", false)


func _show_result(kind: String) -> void:
	last_result_kind = kind
	flow_state = FlowState.RESULT
	board_view.set_input_enabled(false)
	result_layer.visible = true
	result_secondary_button.visible = true

	match kind:
		"win":
			result_eyebrow.text = "구획 %d 완료" % (current_level_index + 1)
			result_title.text = "온실 복원"
			result_text.text = "빛의 흐름이 안정화되었습니다. 다음 구획으로 이어가거나, 같은 퍼즐을 더 깔끔하게 다시 풀 수 있습니다."
			result_secondary_button.text = "한 번 더"
			result_primary_button.text = "다음 구획"
			result_secondary_action = "retry"
			result_primary_action = "next"
		"campaign_complete":
			result_eyebrow.text = "최종 복원 완료"
			result_title.text = "정원이 다시 숨을 쉽니다"
			result_text.text = "다섯 개 구획이 모두 되살아났습니다. 캠페인을 처음부터 다시 돌리거나 메뉴에서 흐름을 정리할 수 있습니다."
			result_secondary_button.text = "메뉴"
			result_primary_button.text = "처음부터"
			result_secondary_action = "menu"
			result_primary_action = "replay_campaign"
		_:
			result_eyebrow.text = "회전 제한 소진"
			result_title.text = "빛이 끊겼습니다"
			result_text.text = "이번 배치는 길이 완성되기 전에 멈췄습니다. 즉시 재도전하거나 메뉴로 돌아가 현재 구획을 다시 읽으세요."
			result_secondary_button.text = "메뉴"
			result_primary_button.text = "다시 시도"
			result_secondary_action = "menu"
			result_primary_action = "retry"

	_refresh_result_meta()
	_sync_buttons()
	_fade_in(result_card)


func _hide_result() -> void:
	result_layer.visible = false
	result_primary_action = ""
	result_secondary_action = ""


func _refresh_progress_copy() -> void:
	progress_label.text = "구획 %d / %d" % [current_level_index + 1, levels.size()]


func _refresh_menu_copy() -> void:
	var level: Dictionary = levels[current_level_index] as Dictionary
	var preview_index := current_level_index
	if menu_primary_action == "next":
		preview_index = clampi(current_level_index + 1, 0, levels.size() - 1)
	elif menu_primary_action == "replay_campaign":
		preview_index = 0

	var preview_level: Dictionary = levels[preview_index] as Dictionary
	var preview_title := String(preview_level.get("title", ""))
	var preview_story := String(preview_level.get("story", ""))
	var preview_grid := int(preview_level.get("grid_size", 4))
	var preview_blooms := (preview_level.get("blooms", []) as Array).size()
	var preview_sources := (preview_level.get("sources", []) as Array).size()
	var preview_moves := int(preview_level.get("moves", 0))

	menu_title.text = "Glass Garden"

	if campaign_started and flow_state != FlowState.RESULT:
		match menu_primary_action:
			"resume":
				menu_eyebrow.text = "진행 중인 복원"
				menu_text.text = (
					"%s 구획을 플레이 중입니다. 흐름을 이어가거나 캠페인을 처음부터 다시 시작할 수 있습니다."
					% String(level.get("title", ""))
				)
				start_button.text = "이어하기"
				menu_secondary_button.visible = true
			"next":
				menu_eyebrow.text = "다음 구획 준비"
				menu_text.text = (
					"%s을 복원했습니다. 다음 구획으로 이동해 톤이 달라진 온실을 이어서 정리하세요."
					% String(level.get("title", ""))
				)
				start_button.text = "다음 구획"
				menu_secondary_button.visible = true
			"retry":
				menu_eyebrow.text = "재도전 준비"
				menu_text.text = (
					"%s 구획을 다시 정렬할 수 있습니다. 같은 퍼즐을 바로 재시도하거나 전체 캠페인을 다시 시작하세요."
					% String(level.get("title", ""))
				)
				start_button.text = "같은 구획 다시"
				menu_secondary_button.visible = true
			"replay_campaign":
				menu_eyebrow.text = "캠페인 완료"
				menu_text.text = "모든 구획을 복원했습니다. 처음부터 다시 플레이해 전체 리듬을 한 번 더 깔끔하게 돌릴 수 있습니다."
				start_button.text = "처음부터"
				menu_secondary_button.visible = false
			_:
				menu_eyebrow.text = "퍼즐 캠페인"
				menu_text.text = "빛의 경로를 정렬해 다섯 개의 유리 온실 구획을 차례대로 복원하세요."
				start_button.text = "플레이 시작"
				menu_secondary_button.visible = campaign_started
	else:
		menu_eyebrow.text = "퍼즐 캠페인"
		menu_text.text = (
			"%s\n\n%s" % [
				"빛의 경로를 정렬해 다섯 개의 유리 온실 구획을 차례대로 복원하세요.",
				preview_story
			]
		)
		start_button.text = "플레이 시작"
		menu_secondary_button.visible = false

	menu_stage_summary.text = (
		"%s · %dx%d 보드 · 광원 %d · 꽃 %d · 제한 %d회전"
		% [preview_title, preview_grid, preview_grid, preview_sources, preview_blooms, preview_moves]
	)
	menu_rules.text = (
		"조작: 거울 탭으로 90도 회전\n실패: 회전 제한 소진\n힌트: %s"
		% String(preview_level.get("hint", ""))
	)


func _refresh_result_meta() -> void:
	var level: Dictionary = levels[current_level_index] as Dictionary
	var lit_blooms: int = board_view.get_lit_bloom_count()
	var total_blooms: int = board_view.get_total_blooms()
	var used_turns: int = int(level.get("moves", 0)) - board_view.get_turns_left()
	result_meta.text = (
		"잔여 회전 %d · 사용 회전 %d · 광원 %d · 꽃 %d/%d"
		% [
			board_view.get_turns_left(),
			used_turns,
			board_view.get_source_count(),
			lit_blooms,
			total_blooms
		]
	)


func _sync_buttons() -> void:
	var is_playing := flow_state == FlowState.PLAYING
	menu_button.disabled = not is_playing
	restart_button.disabled = not is_playing


func _on_turns_changed(turns_left: int) -> void:
	move_label.text = "%d회" % turns_left
	_refresh_result_meta()

	var color := SOFT_TEXT
	if turns_left <= 0:
		color = DANGER_TEXT
	elif turns_left == 1:
		color = WARNING_TEXT
	move_label.label_settings.font_color = color

	if flow_state == FlowState.PLAYING:
		if turns_left == 1:
			_set_feedback("마지막 회전입니다. 광선의 출구를 다시 확인하세요.", true)
		elif turns_left == 0:
			_set_feedback("회전 기회를 모두 사용했습니다.", true)


func _on_blooms_changed(lit_count: int, total_count: int) -> void:
	status_label.text = "%d / %d" % [lit_count, total_count]
	_refresh_result_meta()

	var color := BODY_TEXT
	if lit_count == total_count and total_count > 0:
		color = ACCENT_TEXT
		objective_label.text = "목표 달성: 모든 꽃이 빛을 받았습니다."
	else:
		objective_label.text = "꽃 %d/%d송이를 빛이 지나가게 하세요." % [lit_count, total_count]

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
	_set_feedback("이번 구획은 실패했습니다. 광원 입구부터 다시 읽고 재도전하세요.", true)
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
	_set_feedback("현재 구획을 초기 상태로 되돌렸습니다.", false)


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


func _on_button_down(button: Button) -> void:
	_tween_button_scale(button, Vector2(0.985, 0.985), 0.06)


func _on_button_up(button: Button) -> void:
	_tween_button_scale(button, Vector2.ONE, 0.10)


func _tween_button_scale(button: Button, target: Vector2, duration: float) -> void:
	var tween := create_tween()
	tween.set_trans(Tween.TRANS_QUAD).set_ease(Tween.EASE_OUT)
	tween.tween_property(button, "scale", target, duration)


func _fade_in(card: Control) -> void:
	card.pivot_offset = card.size * 0.5
	card.modulate = Color(1.0, 1.0, 1.0, 0.0)
	card.scale = Vector2(0.96, 0.96)
	var tween := create_tween()
	tween.set_trans(Tween.TRANS_QUAD).set_ease(Tween.EASE_OUT)
	tween.tween_property(card, "modulate", Color(1.0, 1.0, 1.0, 1.0), 0.22)
	tween.parallel().tween_property(card, "scale", Vector2.ONE, 0.22)


func _update_responsive_layout() -> void:
	var viewport := get_viewport_rect().size
	var compact := viewport.y < 1750.0
	var narrow := viewport.x < 980.0

	safe_area.add_theme_constant_override("margin_left", 26 if narrow else 34)
	safe_area.add_theme_constant_override("margin_top", 34 if compact else 42)
	safe_area.add_theme_constant_override("margin_right", 26 if narrow else 34)
	safe_area.add_theme_constant_override("margin_bottom", 24 if compact else 30)

	top_panel.custom_minimum_size = Vector2(0.0, 214.0 if compact else 236.0)
	bottom_panel.custom_minimum_size = Vector2(0.0, 160.0 if compact else 176.0)

	var result_half_width: float = minf(viewport.x * 0.44, 320.0)
	var result_half_height: float = minf(viewport.y * 0.22, 250.0)
	result_card.offset_left = -result_half_width
	result_card.offset_right = result_half_width
	result_card.offset_top = -result_half_height
	result_card.offset_bottom = result_half_height

	var menu_half_width: float = minf(viewport.x * 0.46, 336.0)
	var menu_half_height: float = minf(viewport.y * 0.29, 318.0)
	menu_card.offset_left = -menu_half_width
	menu_card.offset_right = menu_half_width
	menu_card.offset_top = -menu_half_height
	menu_card.offset_bottom = menu_half_height


func _build_levels() -> Array:
	return [
		{
			"title": "새벽 온실",
			"region": "DAWN GLASSHOUSE SECTOR",
			"tagline": "첫 번째 광선을 두 송이의 꽃까지 정돈해 보내세요.",
			"story": "이른 새벽, 첫 유리 지붕 아래에서 가장 쉬운 구획이 플레이어에게 규칙을 가르칩니다.",
			"hint": "거울을 탭하면 90도 회전합니다. 광원 입구부터 읽고 두 꽃을 모두 지나게 하세요.",
			"intro_feedback": "첫 구획은 온보딩입니다. 거울 두 개만 읽으면 해법이 보입니다.",
			"grid_size": 4,
			"moves": 4,
			"sources": [{"side": "top", "index": 1, "color": Color("ffd972")}],
			"blooms": [Vector2i(1, 1), Vector2i(3, 1)],
			"pieces":
			[
				{"pos": Vector2i(1, 2), "type": "mirror", "rotation": 1},
				{"pos": Vector2i(3, 2), "type": "mirror", "rotation": 0}
			],
			"palette":
			{
				"sky_top": Color("142926"),
				"sky_mid": Color("17332f"),
				"sky_bottom": Color("091214"),
				"mist": Color(0.58, 0.74, 0.60, 0.28),
				"glow": Color("ffd972"),
				"glow_secondary": Color("d0f4dd"),
				"board": Color(0.08, 0.15, 0.14, 0.88),
				"board_border": Color(0.99, 0.92, 0.72, 0.24),
				"cell": Color(0.20, 0.31, 0.29, 0.56),
				"cell_lit": Color(0.39, 0.48, 0.31, 0.70),
				"glass": Color(0.84, 0.96, 0.90, 0.26),
				"bloom": Color("f1f5dc"),
				"bloom_core": Color("f6c86a"),
				"shadow": Color(0.0, 0.0, 0.0, 0.38)
			}
		},
		{
			"title": "호박빛 아치",
			"region": "AMBER ARCHIVE",
			"tagline": "한 줄의 빛을 세 번의 개화가 이어지는 곡선으로 바꾸세요.",
			"story": "노을빛이 깊게 스며드는 아치형 구획입니다. 짧은 곡선이 연속으로 이어져야 합니다.",
			"hint": "먼저 아래쪽 곡선을 만든 뒤 위쪽 회전을 맞추면 경로가 안정됩니다.",
			"intro_feedback": "이번 구획은 위아래 순서를 읽는 감각이 중요합니다.",
			"grid_size": 5,
			"moves": 5,
			"sources": [{"side": "left", "index": 3, "color": Color("ffc96c")}],
			"blooms": [Vector2i(2, 3), Vector2i(3, 2), Vector2i(4, 1)],
			"pieces":
			[
				{"pos": Vector2i(3, 3), "type": "mirror", "rotation": 0},
				{"pos": Vector2i(3, 1), "type": "mirror", "rotation": 0},
				{"pos": Vector2i(1, 0), "type": "blocker", "rotation": 0, "locked": true}
			],
			"palette":
			{
				"sky_top": Color("2b2117"),
				"sky_mid": Color("3a281b"),
				"sky_bottom": Color("0f1112"),
				"mist": Color(0.83, 0.55, 0.33, 0.22),
				"glow": Color("ffc96c"),
				"glow_secondary": Color("ffe9c0"),
				"board": Color(0.14, 0.14, 0.11, 0.88),
				"board_border": Color(0.99, 0.81, 0.54, 0.25),
				"cell": Color(0.30, 0.24, 0.18, 0.56),
				"cell_lit": Color(0.50, 0.34, 0.20, 0.72),
				"glass": Color(0.97, 0.86, 0.74, 0.22),
				"bloom": Color("f5e8d4"),
				"bloom_core": Color("ffb95f"),
				"shadow": Color(0.0, 0.0, 0.0, 0.42)
			}
		},
		{
			"title": "유리 회랑",
			"region": "GLASS CORRIDOR",
			"tagline": "긴 회랑 끝에서 마지막 꽃까지 광선을 끌고 가야 합니다.",
			"story": "길게 뻗은 유리 통로가 시작과 끝의 거리감을 강조합니다. 경로를 크게 읽어야 합니다.",
			"hint": "오른쪽 끝까지 먼저 보낸 뒤 아래로 한 번, 다시 왼쪽으로 흘러갑니다.",
			"intro_feedback": "긴 회랑형 퍼즐입니다. 부분보다 최종 출구를 먼저 보는 편이 쉽습니다.",
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
			],
			"palette":
			{
				"sky_top": Color("102225"),
				"sky_mid": Color("153238"),
				"sky_bottom": Color("081113"),
				"mist": Color(0.44, 0.74, 0.78, 0.22),
				"glow": Color("ffe48f"),
				"glow_secondary": Color("bfe7ff"),
				"board": Color(0.07, 0.14, 0.16, 0.88),
				"board_border": Color(0.79, 0.92, 0.95, 0.24),
				"cell": Color(0.16, 0.28, 0.31, 0.56),
				"cell_lit": Color(0.33, 0.48, 0.42, 0.72),
				"glass": Color(0.78, 0.93, 0.96, 0.24),
				"bloom": Color("ecf8f2"),
				"bloom_core": Color("f2c470"),
				"shadow": Color(0.0, 0.0, 0.0, 0.42)
			}
		},
		{
			"title": "이중 채광실",
			"region": "TWIN ATRIUM",
			"tagline": "두 개의 광원을 교차시키며 상하층을 동시에 밝히세요.",
			"story": "서로 다른 색 온도의 광원이 만나는 구획입니다. 두 경로를 따로 읽는 힘이 필요합니다.",
			"hint": "두 경로는 서로 방해하지 않습니다. 위쪽과 왼쪽 광원을 분리해서 읽으세요.",
			"intro_feedback": "광원이 둘일 때는 경로를 따로 읽은 뒤 겹치는 부분만 확인하세요.",
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
			],
			"palette":
			{
				"sky_top": Color("0f2528"),
				"sky_mid": Color("15363a"),
				"sky_bottom": Color("081113"),
				"mist": Color(0.62, 0.83, 0.81, 0.18),
				"glow": Color("ffdf88"),
				"glow_secondary": Color("bfe7ff"),
				"board": Color(0.08, 0.16, 0.18, 0.88),
				"board_border": Color(0.78, 0.93, 0.91, 0.24),
				"cell": Color(0.16, 0.30, 0.31, 0.56),
				"cell_lit": Color(0.30, 0.48, 0.41, 0.74),
				"glass": Color(0.82, 0.95, 0.92, 0.24),
				"bloom": Color("f0f8e4"),
				"bloom_core": Color("f6c86a"),
				"shadow": Color(0.0, 0.0, 0.0, 0.44)
			}
		},
		{
			"title": "황혼 보존동",
			"region": "DUSK CONSERVATORY",
			"tagline": "마지막 구획에서 두 광선을 길게 감아 다섯 송이를 모두 피우세요.",
			"story": "가장 깊은 구획입니다. 따뜻한 광선과 차가운 광선이 길게 교차하며 최종 퍼즐의 무게를 만듭니다.",
			"hint": "아래에서 올라오는 빛과 오른쪽에서 들어오는 빛이 서로 다른 꽃 묶음을 담당합니다.",
			"intro_feedback": "최종 구획은 길고 느린 퍼즐입니다. 각 광원이 맡는 꽃 묶음을 먼저 구분하세요.",
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
			],
			"palette":
			{
				"sky_top": Color("18221f"),
				"sky_mid": Color("22302b"),
				"sky_bottom": Color("091012"),
				"mist": Color(0.63, 0.70, 0.58, 0.16),
				"glow": Color("ffd66b"),
				"glow_secondary": Color("cdefff"),
				"board": Color(0.09, 0.15, 0.16, 0.90),
				"board_border": Color(0.98, 0.90, 0.62, 0.24),
				"cell": Color(0.19, 0.26, 0.29, 0.58),
				"cell_lit": Color(0.35, 0.42, 0.34, 0.74),
				"glass": Color(0.83, 0.92, 0.94, 0.22),
				"bloom": Color("f2f3de"),
				"bloom_core": Color("f6c86a"),
				"shadow": Color(0.0, 0.0, 0.0, 0.46)
			}
		}
	]


func _apply_skin() -> void:
	title_label.label_settings = _label_settings(58, ACCENT_TEXT, 3, Color(0.0, 0.0, 0.0, 0.42), 0)
	subtitle_label.label_settings = _label_settings(23, BODY_TEXT, 1, Color(0.0, 0.0, 0.0, 0.28), 0)
	eyebrow_label.label_settings = _label_settings(16, SOFT_TEXT, 0, Color(0.0, 0.0, 0.0, 0.0), 3)
	progress_label.label_settings = _label_settings(18, ACCENT_TEXT, 1, SHADOW_TEXT, 0)
	objective_eyebrow.label_settings = _label_settings(15, SOFT_TEXT, 0, Color(0.0, 0.0, 0.0, 0.0), 2)
	objective_label.label_settings = _label_settings(24, BODY_TEXT, 1, Color(0.0, 0.0, 0.0, 0.18), 0)
	level_label.label_settings = _label_settings(26, BODY_TEXT, 1, Color(0.0, 0.0, 0.0, 0.18), 0)
	move_label.label_settings = _label_settings(28, BODY_TEXT, 1, Color(0.0, 0.0, 0.0, 0.18), 0)
	status_label.label_settings = _label_settings(28, BODY_TEXT, 1, Color(0.0, 0.0, 0.0, 0.18), 0)

	for label in [
		$SafeArea/Layout/TopPanel/TopContent/StatsRow/StageCard/StageCardContent/StageEyebrow,
		$SafeArea/Layout/TopPanel/TopContent/StatsRow/MoveCard/MoveCardContent/MoveEyebrow,
		$SafeArea/Layout/TopPanel/TopContent/StatsRow/BloomCard/BloomCardContent/BloomEyebrow,
		$SafeArea/Layout/BottomPanel/BottomContent/HintCard/HintContent/HintEyebrow
	]:
		label.label_settings = _label_settings(15, SOFT_TEXT, 0, Color(0.0, 0.0, 0.0, 0.0), 2)

	hint_label.label_settings = _label_settings(21, BODY_TEXT, 1, Color(0.0, 0.0, 0.0, 0.14), 0)
	feedback_label.label_settings = _label_settings(20, BODY_TEXT, 1, Color(0.0, 0.0, 0.0, 0.14), 0)
	menu_eyebrow.label_settings = _label_settings(17, SOFT_TEXT, 0, Color(0.0, 0.0, 0.0, 0.0), 3)
	menu_title.label_settings = _label_settings(44, ACCENT_TEXT, 2, SHADOW_TEXT, 0)
	menu_text.label_settings = _label_settings(23, BODY_TEXT, 1, Color(0.0, 0.0, 0.0, 0.18), 0)
	menu_stage_summary.label_settings = _label_settings(20, BODY_TEXT, 1, Color(0.0, 0.0, 0.0, 0.12), 0)
	menu_rules.label_settings = _label_settings(20, SOFT_TEXT, 1, Color(0.0, 0.0, 0.0, 0.12), 0)
	result_eyebrow.label_settings = _label_settings(17, SOFT_TEXT, 0, Color(0.0, 0.0, 0.0, 0.0), 3)
	result_title.label_settings = _label_settings(38, ACCENT_TEXT, 2, SHADOW_TEXT, 0)
	result_text.label_settings = _label_settings(22, BODY_TEXT, 1, Color(0.0, 0.0, 0.0, 0.16), 0)
	result_meta.label_settings = _label_settings(19, SOFT_TEXT, 1, Color(0.0, 0.0, 0.0, 0.12), 0)

	menu_mark.modulate = Color(1.0, 1.0, 1.0, 0.95)

	for button in [
		menu_button,
		restart_button,
		menu_secondary_button,
		start_button,
		result_secondary_button,
		result_primary_button
	]:
		button.custom_minimum_size = Vector2(0.0, 76.0)

	_apply_button_skin(menu_button, false)
	_apply_button_skin(restart_button, false)
	_apply_button_skin(menu_secondary_button, false)
	_apply_button_skin(result_secondary_button, false)
	_apply_button_skin(start_button, true)
	_apply_button_skin(result_primary_button, true)


func _apply_level_theme(level: Dictionary) -> void:
	var palette := level.get("palette", {}) as Dictionary
	var accent := _palette_color(palette, "glow", Color("f5d38e"))
	var accent_secondary := _palette_color(palette, "glow_secondary", Color("bfe7ff"))
	var board_color := _palette_color(palette, "board", Color(0.08, 0.15, 0.14, 0.88))
	var board_border := _palette_color(palette, "board_border", Color(0.86, 0.92, 0.84, 0.22))
	var glass := _palette_color(palette, "glass", Color(0.84, 0.96, 0.90, 0.26))
	var shadow := _palette_color(palette, "shadow", Color(0.0, 0.0, 0.0, 0.42))

	top_panel.add_theme_stylebox_override(
		"panel",
		_panel_style(
			Color(board_color.r, board_color.g, board_color.b, 0.84),
			Color(board_border.r, board_border.g, board_border.b, 0.20),
			28,
			26,
			22,
			shadow,
			36,
			Vector2(0.0, 16.0)
		)
	)
	bottom_panel.add_theme_stylebox_override(
		"panel",
		_panel_style(
			Color(board_color.r, board_color.g, board_color.b, 0.82),
			Color(board_border.r, board_border.g, board_border.b, 0.18),
			28,
			24,
			20,
			shadow,
			34,
			Vector2(0.0, 16.0)
		)
	)
	progress_badge.add_theme_stylebox_override(
		"panel",
		_panel_style(
			Color(accent.r, accent.g, accent.b, 0.18),
			Color(accent.r, accent.g, accent.b, 0.42),
			18,
			16,
			12,
			Color(0.0, 0.0, 0.0, 0.18),
			16,
			Vector2(0.0, 6.0),
			Vector2(-0.06, 0.0)
		)
	)
	objective_panel.add_theme_stylebox_override(
		"panel",
		_panel_style(
			Color(glass.r, glass.g, glass.b, 0.12),
			Color(accent_secondary.r, accent_secondary.g, accent_secondary.b, 0.20),
			22,
			22,
			18,
			Color(0.0, 0.0, 0.0, 0.10),
			16,
			Vector2(0.0, 6.0)
		)
	)

	for card in [stage_card, move_card, bloom_card]:
		card.add_theme_stylebox_override(
			"panel",
			_panel_style(
				Color(glass.r, glass.g, glass.b, 0.10),
				Color(board_border.r, board_border.g, board_border.b, 0.16),
				20,
				20,
				16,
				Color(0.0, 0.0, 0.0, 0.08),
				12,
				Vector2(0.0, 4.0)
			)
		)

	hint_card.add_theme_stylebox_override(
		"panel",
		_panel_style(
			Color(glass.r, glass.g, glass.b, 0.08),
			Color(board_border.r, board_border.g, board_border.b, 0.16),
			20,
			18,
			16,
			Color(0.0, 0.0, 0.0, 0.10),
			14,
			Vector2(0.0, 6.0)
		)
	)
	feedback_card.add_theme_stylebox_override(
		"panel",
		_panel_style(
			Color(accent_secondary.r, accent_secondary.g, accent_secondary.b, 0.12),
			Color(accent.r, accent.g, accent.b, 0.28),
			20,
			18,
			16,
			Color(0.0, 0.0, 0.0, 0.12),
			16,
			Vector2(0.0, 6.0)
		)
	)
	menu_stage_card.add_theme_stylebox_override(
		"panel",
		_panel_style(
			Color(glass.r, glass.g, glass.b, 0.10),
			Color(accent_secondary.r, accent_secondary.g, accent_secondary.b, 0.20),
			18,
			16,
			14,
			Color(0.0, 0.0, 0.0, 0.10),
			14,
			Vector2(0.0, 5.0)
		)
	)

	var floating_background := Color(board_color.r, board_color.g, board_color.b, 0.94)
	var floating_border := Color(accent.r, accent.g, accent.b, 0.24)
	menu_card.add_theme_stylebox_override(
		"panel",
		_panel_style(floating_background, floating_border, 32, 28, 24, shadow, 48, Vector2(0.0, 18.0))
	)
	result_card.add_theme_stylebox_override(
		"panel",
		_panel_style(floating_background, floating_border, 32, 28, 24, shadow, 48, Vector2(0.0, 18.0))
	)

	progress_label.label_settings.font_color = accent
	objective_eyebrow.label_settings.font_color = accent_secondary
	menu_title.label_settings.font_color = accent
	result_title.label_settings.font_color = accent

	if background_layer.has_method("configure_level"):
		background_layer.call("configure_level", level)


func _apply_button_skin(button: Button, accent: bool) -> void:
	var normal_bg := Color(0.10, 0.18, 0.17, 0.92)
	var hover_bg := Color(0.16, 0.25, 0.23, 0.96)
	var pressed_bg := Color(0.08, 0.13, 0.12, 0.98)
	var disabled_bg := Color(0.11, 0.15, 0.15, 0.72)
	var accent_bg := Color(0.46, 0.34, 0.16, 0.98)
	var accent_hover := Color(0.56, 0.42, 0.17, 1.0)
	var accent_pressed := Color(0.31, 0.23, 0.12, 1.0)
	var border := Color(0.82, 0.91, 0.88, 0.18)
	var accent_border := Color(0.99, 0.91, 0.67, 0.34)

	button.add_theme_stylebox_override(
		"normal",
		_panel_style(
			accent_bg if accent else normal_bg,
			accent_border if accent else border,
			22,
			18,
			16,
			Color(0.0, 0.0, 0.0, 0.18),
			20,
			Vector2(0.0, 8.0)
		)
	)
	button.add_theme_stylebox_override(
		"hover",
		_panel_style(
			accent_hover if accent else hover_bg,
			accent_border if accent else border,
			22,
			18,
			16,
			Color(0.0, 0.0, 0.0, 0.18),
			20,
			Vector2(0.0, 8.0)
		)
	)
	button.add_theme_stylebox_override(
		"pressed",
		_panel_style(
			accent_pressed if accent else pressed_bg,
			accent_border if accent else border,
			22,
			18,
			16,
			Color(0.0, 0.0, 0.0, 0.14),
			12,
			Vector2(0.0, 3.0)
		)
	)
	button.add_theme_stylebox_override(
		"disabled",
		_panel_style(
			disabled_bg,
			Color(0.76, 0.84, 0.82, 0.10),
			22,
			18,
			16,
			Color(0.0, 0.0, 0.0, 0.08),
			10,
			Vector2(0.0, 3.0)
		)
	)
	button.add_theme_color_override("font_color", Color(0.98, 0.96, 0.92, 1.0))
	button.add_theme_color_override("font_hover_color", Color(1.0, 0.99, 0.95, 1.0))
	button.add_theme_color_override("font_pressed_color", Color(1.0, 0.99, 0.95, 1.0))
	button.add_theme_color_override("font_disabled_color", Color(0.56, 0.65, 0.62, 1.0))
	button.add_theme_font_size_override("font_size", 23)


func _panel_style(
	background: Color,
	border: Color,
	radius: int = 24,
	padding_x: int = 22,
	padding_y: int = 18,
	shadow_color: Color = Color(0.0, 0.0, 0.0, 0.32),
	shadow_size: int = 28,
	shadow_offset: Vector2 = Vector2(0.0, 12.0),
	skew: Vector2 = Vector2.ZERO
) -> StyleBoxFlat:
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
	style.content_margin_left = padding_x
	style.content_margin_top = padding_y
	style.content_margin_right = padding_x
	style.content_margin_bottom = padding_y
	style.shadow_color = shadow_color
	style.shadow_size = shadow_size
	style.shadow_offset = shadow_offset
	style.skew = skew
	return style


func _label_settings(
	size_px: int,
	color: Color,
	outline_size: int = 0,
	shadow_color: Color = Color(0.0, 0.0, 0.0, 0.0),
	uppercase_spacing: int = 0
) -> LabelSettings:
	var settings := LabelSettings.new()
	settings.font_size = size_px
	settings.font_color = color
	settings.outline_size = outline_size
	settings.outline_color = Color(0.03, 0.06, 0.06, 0.42)
	settings.shadow_color = shadow_color
	settings.shadow_size = 1
	settings.line_spacing = uppercase_spacing
	return settings


func _palette_color(palette: Dictionary, key: String, fallback: Color) -> Color:
	return palette.get(key, fallback) as Color
