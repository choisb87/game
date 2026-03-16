extends Control

const MENU_SCENE := preload("res://scenes/MenuScene.tscn")
const GAME_SCENE := preload("res://scenes/GameScene.tscn")
const RESULT_SCENE := preload("res://scenes/ResultScene.tscn")
const SAVE_PATH := "user://neon_striker.save"

var best_score := 0
var best_rank := "D"
var last_run := {}
var current_view: Node

@onready var scene_host: Control = $SceneHost
@onready var fade_rect: ColorRect = $FadeLayer/FadeRect


func _ready() -> void:
	_load_progress()
	fade_rect.color = Color(0.0, 0.0, 0.0, 1.0)
	fade_rect.modulate.a = 0.0
	_show_menu()


func _show_menu() -> void:
	var scene := MENU_SCENE.instantiate()
	_switch_to(scene)
	scene.set_summary(best_score, best_rank)
	scene.start_pressed.connect(_show_game)


func _show_game() -> void:
	var scene := GAME_SCENE.instantiate()
	_switch_to(scene)
	scene.retreat_pressed.connect(_show_menu)
	scene.run_finished.connect(_on_game_finished)


func _show_result() -> void:
	var scene := RESULT_SCENE.instantiate()
	_switch_to(scene)
	scene.set_result(last_run, bool(last_run.get("is_new_best", false)), best_score)
	scene.retry_pressed.connect(_show_game)
	scene.menu_pressed.connect(_show_menu)


func _on_game_finished(result: Dictionary) -> void:
	last_run = result.duplicate(true)
	var score := int(result.get("score", 0))
	if score > best_score:
		best_score = score
		best_rank = String(result.get("rank", "D"))
		last_run["is_new_best"] = true
		_save_progress()
	else:
		last_run["is_new_best"] = false
	_show_result()


func _switch_to(next_view: Node) -> void:
	if current_view != null:
		current_view.queue_free()

	current_view = next_view
	scene_host.add_child(current_view)

	if current_view is Control:
		var control := current_view as Control
		control.anchor_right = 1.0
		control.anchor_bottom = 1.0
		control.grow_horizontal = Control.GROW_DIRECTION_BOTH
		control.grow_vertical = Control.GROW_DIRECTION_BOTH

	_play_fade()


func _play_fade() -> void:
	fade_rect.color = Color(0.0, 0.0, 0.0, 1.0)
	fade_rect.modulate.a = 0.42
	var tween := create_tween()
	tween.tween_property(fade_rect, "modulate:a", 0.0, 0.28)


func _load_progress() -> void:
	var config := ConfigFile.new()
	var err := config.load(SAVE_PATH)
	if err != OK:
		return

	best_score = int(config.get_value("player", "best_score", 0))
	best_rank = String(config.get_value("player", "best_rank", "D"))


func _save_progress() -> void:
	var config := ConfigFile.new()
	config.set_value("player", "best_score", best_score)
	config.set_value("player", "best_rank", best_rank)
	config.save(SAVE_PATH)
