extends Control

signal turns_changed(turns_left)
signal blooms_changed(lit_count, total_count)
signal level_completed
signal level_failed

const BEAM_FALLBACK := Color("ffe08a")
const INVALID_CELL := Vector2i(-1, -1)

var grid_size := 4
var max_moves := 0
var turns_used := 0
var board_padding := 24.0

var pieces := {}
var blooms := []
var bloom_lookup := {}
var sources := []
var beam_segments := []
var lit_path_lookup := {}
var lit_bloom_lookup := {}

var completed := false
var failed := false

var board_style: StyleBoxFlat
var cell_style: StyleBoxFlat
var lit_cell_style: StyleBoxFlat
var blocker_style: StyleBoxFlat


func _ready() -> void:
	mouse_filter = MOUSE_FILTER_STOP


func get_turns_left() -> int:
	return max(0, max_moves - turns_used)


func setup_level(level: Dictionary) -> void:
	grid_size = int(level.get("grid_size", 4))
	max_moves = int(level.get("moves", 6))
	turns_used = 0
	completed = false
	failed = false

	pieces.clear()
	blooms.clear()
	bloom_lookup.clear()
	sources.clear()
	beam_segments.clear()
	lit_path_lookup.clear()
	lit_bloom_lookup.clear()

	for bloom in level.get("blooms", []):
		var bloom_pos := bloom as Vector2i
		blooms.append(bloom_pos)
		bloom_lookup[_cell_key(bloom_pos)] = true

	for source in level.get("sources", []):
		sources.append((source as Dictionary).duplicate(true))

	for piece in level.get("pieces", []):
		var piece_data := piece as Dictionary
		var piece_pos := piece_data.get("pos", Vector2i.ZERO) as Vector2i
		pieces[_cell_key(piece_pos)] = {
			"pos": piece_pos,
			"type": String(piece_data.get("type", "mirror")),
			"rotation": int(piece_data.get("rotation", 0)),
			"locked": bool(piece_data.get("locked", false))
		}

	_recalculate()
	turns_changed.emit(get_turns_left())
	blooms_changed.emit(lit_bloom_lookup.size(), blooms.size())
	queue_redraw()


func _gui_input(event: InputEvent) -> void:
	if completed or failed:
		return

	var pointer_pos := Vector2.ZERO
	var should_handle := false

	if event is InputEventMouseButton:
		var mouse_event := event as InputEventMouseButton
		if mouse_event.button_index == MOUSE_BUTTON_LEFT and mouse_event.pressed:
			pointer_pos = mouse_event.position
			should_handle = true
	elif event is InputEventScreenTouch:
		var touch_event := event as InputEventScreenTouch
		if touch_event.pressed:
			pointer_pos = touch_event.position
			should_handle = true

	if not should_handle:
		return

	var cell := _cell_from_local(pointer_pos)
	if cell == INVALID_CELL:
		return

	var key := _cell_key(cell)
	if not pieces.has(key):
		return

	var piece := pieces[key] as Dictionary
	if String(piece.get("type", "")) != "mirror" or bool(piece.get("locked", false)):
		return

	piece["rotation"] = wrapi(int(piece.get("rotation", 0)) + 1, 0, 4)
	pieces[key] = piece
	turns_used += 1
	_recalculate()
	turns_changed.emit(get_turns_left())
	blooms_changed.emit(lit_bloom_lookup.size(), blooms.size())

	if lit_bloom_lookup.size() == blooms.size():
		completed = true
		level_completed.emit()
	elif turns_used >= max_moves:
		failed = true
		level_failed.emit()

	accept_event()
	queue_redraw()


func _draw() -> void:
	_ensure_styles()

	var board_rect := _board_rect()
	draw_style_box(board_style, board_rect)

	for y in range(grid_size):
		for x in range(grid_size):
			var cell := Vector2i(x, y)
			var cell_rect := _cell_rect(board_rect, cell)
			var key := _cell_key(cell)
			var cell_box := lit_cell_style if lit_path_lookup.has(key) else cell_style
			draw_style_box(cell_box, cell_rect)

	for segment in beam_segments:
		var from_point := _grid_to_local(segment["from"] as Vector2, board_rect)
		var to_point := _grid_to_local(segment["to"] as Vector2, board_rect)
		var beam_color := segment["color"] as Color
		draw_line(from_point, to_point, Color(beam_color.r, beam_color.g, beam_color.b, 0.18), 20.0, true)
		draw_line(from_point, to_point, Color(beam_color.r, beam_color.g, beam_color.b, 0.85), 8.0, true)
		draw_line(from_point, to_point, Color(1.0, 1.0, 1.0, 0.95), 2.0, true)

	for y in range(grid_size):
		for x in range(grid_size):
			var cell := Vector2i(x, y)
			var key := _cell_key(cell)
			var cell_rect := _cell_rect(board_rect, cell)

			if pieces.has(key):
				_draw_piece(cell_rect, pieces[key] as Dictionary)

			if bloom_lookup.has(key):
				_draw_bloom(cell_rect, lit_bloom_lookup.has(key))

	_draw_sources(board_rect)
	_draw_board_frame(board_rect)


func _recalculate() -> void:
	beam_segments.clear()
	lit_path_lookup.clear()
	lit_bloom_lookup.clear()

	for source in sources:
		var result := _trace_source(source as Dictionary)
		for segment in result["segments"]:
			beam_segments.append(segment)
		for path_key in result["path"]:
			lit_path_lookup[path_key] = true
		for bloom_key in result["blooms"]:
			lit_bloom_lookup[bloom_key] = true


func _trace_source(source: Dictionary) -> Dictionary:
	var segments := []
	var path_cells := []
	var lit_blooms := []

	var side := String(source.get("side", "top"))
	var index := int(source.get("index", 0))
	var beam_color := source.get("color", BEAM_FALLBACK) as Color
	var direction := _direction_from_side(side)
	var current_cell := _source_entry_cell(side, index)
	var travel_point := _source_start_point(side, index)
	var visited := {}
	var safety := 0
	var exited_board := false
	var blocked := false

	while _is_inside(current_cell) and safety < 128:
		var state_key := "%d:%d:%d:%d" % [current_cell.x, current_cell.y, direction.x, direction.y]
		if visited.has(state_key):
			break
		visited[state_key] = true

		var current_key := _cell_key(current_cell)
		path_cells.append(current_key)
		if bloom_lookup.has(current_key):
			lit_blooms.append(current_key)

		var center := _cell_center(current_cell)
		segments.append({"from": travel_point, "to": center, "color": beam_color})

		var piece := pieces.get(current_key, {}) as Dictionary
		var piece_type := String(piece.get("type", "empty"))
		if piece_type == "blocker":
			blocked = true
			break

		var outgoing := direction
		if piece_type == "mirror":
			outgoing = _reflect(direction, int(piece.get("rotation", 0)))

		var exit_point := _cell_exit_point(current_cell, outgoing)
		segments.append({"from": center, "to": exit_point, "color": beam_color})
		travel_point = exit_point
		current_cell += outgoing
		direction = outgoing
		safety += 1

	exited_board = not _is_inside(current_cell)
	if exited_board and not blocked and safety < 128:
		var outside := travel_point + Vector2(direction.x, direction.y) * 0.18
		segments.append({"from": travel_point, "to": outside, "color": beam_color})

	return {
		"segments": segments,
		"path": path_cells,
		"blooms": lit_blooms
	}


func _draw_piece(cell_rect: Rect2, piece: Dictionary) -> void:
	var piece_type := String(piece.get("type", ""))
	if piece_type == "blocker":
		draw_style_box(blocker_style, cell_rect.grow(-cell_rect.size.x * 0.14))
		var top_left := cell_rect.position + Vector2(cell_rect.size.x * 0.26, cell_rect.size.y * 0.34)
		var top_right := cell_rect.position + Vector2(cell_rect.size.x * 0.74, cell_rect.size.y * 0.34)
		var bottom_left := cell_rect.position + Vector2(cell_rect.size.x * 0.30, cell_rect.size.y * 0.66)
		var bottom_right := cell_rect.position + Vector2(cell_rect.size.x * 0.70, cell_rect.size.y * 0.66)
		draw_line(top_left, top_right, Color(0.74, 0.84, 0.82, 0.55), 3.0, true)
		draw_line(bottom_left, bottom_right, Color(0.74, 0.84, 0.82, 0.35), 2.0, true)
		return

	var line_color := Color("f4d08f")
	var glow_color := Color(0.96, 0.88, 0.68, 0.24)
	var inset := cell_rect.size.x * 0.18
	var from_point := cell_rect.position + Vector2(inset, inset)
	var to_point := cell_rect.position + Vector2(cell_rect.size.x - inset, cell_rect.size.y - inset)
	if int(piece.get("rotation", 0)) % 2 == 1:
		from_point = cell_rect.position + Vector2(cell_rect.size.x - inset, inset)
		to_point = cell_rect.position + Vector2(inset, cell_rect.size.y - inset)

	draw_line(from_point, to_point, glow_color, 16.0, true)
	draw_line(from_point, to_point, line_color, 7.0, true)
	draw_circle(cell_rect.get_center(), cell_rect.size.x * 0.085, Color("0f1716"))
	draw_circle(cell_rect.get_center(), cell_rect.size.x * 0.052, line_color)

	if bool(piece.get("locked", false)):
		var lock_center := cell_rect.position + Vector2(cell_rect.size.x * 0.78, cell_rect.size.y * 0.24)
		draw_circle(lock_center, cell_rect.size.x * 0.07, Color("101817"))
		draw_circle(lock_center, cell_rect.size.x * 0.045, Color("d6e3cf"))


func _draw_bloom(cell_rect: Rect2, lit: bool) -> void:
	var center := cell_rect.get_center()
	var radius := cell_rect.size.x * 0.075
	var petal_radius := cell_rect.size.x * (0.13 if lit else 0.09)
	var petal_color := Color("f0f4d6") if lit else Color(0.74, 0.84, 0.76, 0.70)
	var core_color := Color("f6c86a") if lit else Color(0.52, 0.60, 0.55, 0.90)
	var glow_color := Color(1.0, 0.94, 0.72, 0.18) if lit else Color(0.0, 0.0, 0.0, 0.0)

	if lit:
		draw_circle(center, cell_rect.size.x * 0.22, glow_color)

	for angle in [0.0, PI * 0.5, PI, PI * 1.5]:
		var petal_offset := Vector2.RIGHT.rotated(angle) * cell_rect.size.x * 0.12
		draw_circle(center + petal_offset, petal_radius, petal_color)

	draw_circle(center, radius, core_color)


func _draw_sources(board_rect: Rect2) -> void:
	for source in sources:
		var source_data := source as Dictionary
		var start_point := _source_start_point(
			String(source_data.get("side", "top")),
			int(source_data.get("index", 0))
		)
		var center := _grid_to_local(start_point, board_rect)
		var color := source_data.get("color", BEAM_FALLBACK) as Color
		var cell_radius := board_rect.size.x / float(grid_size)
		draw_circle(center, cell_radius * 0.15, Color(color.r, color.g, color.b, 0.24))
		draw_circle(center, cell_radius * 0.09, color)
		draw_circle(center, cell_radius * 0.04, Color(1.0, 0.98, 0.90, 1.0))


func _draw_board_frame(board_rect: Rect2) -> void:
	var accent := Color(0.85, 0.93, 0.84, 0.16)
	var top_left := board_rect.position
	var top_right := board_rect.position + Vector2(board_rect.size.x, 0.0)
	var inset_top := Vector2(0.0, board_rect.size.y * 0.02)
	var inset_left := Vector2(board_rect.size.x * 0.02, 0.0)
	draw_line(top_left + inset_top, top_right + inset_top, accent, 2.0, true)
	draw_line(
		top_left + inset_left,
		top_left + inset_left + Vector2(0.0, board_rect.size.y),
		accent,
		2.0,
		true
	)


func _ensure_styles() -> void:
	if board_style != null:
		return

	board_style = StyleBoxFlat.new()
	board_style.bg_color = Color(0.09, 0.15, 0.14, 0.82)
	board_style.border_color = Color(0.78, 0.88, 0.80, 0.18)
	board_style.border_width_left = 2
	board_style.border_width_top = 2
	board_style.border_width_right = 2
	board_style.border_width_bottom = 2
	board_style.corner_radius_top_left = 28
	board_style.corner_radius_top_right = 28
	board_style.corner_radius_bottom_right = 28
	board_style.corner_radius_bottom_left = 28
	board_style.content_margin_left = 14
	board_style.content_margin_top = 14
	board_style.content_margin_right = 14
	board_style.content_margin_bottom = 14

	cell_style = StyleBoxFlat.new()
	cell_style.bg_color = Color(0.20, 0.30, 0.28, 0.48)
	cell_style.border_color = Color(0.90, 0.97, 0.90, 0.08)
	cell_style.border_width_left = 1
	cell_style.border_width_top = 1
	cell_style.border_width_right = 1
	cell_style.border_width_bottom = 1
	cell_style.corner_radius_top_left = 18
	cell_style.corner_radius_top_right = 18
	cell_style.corner_radius_bottom_right = 18
	cell_style.corner_radius_bottom_left = 18

	lit_cell_style = StyleBoxFlat.new()
	lit_cell_style.bg_color = Color(0.34, 0.44, 0.31, 0.54)
	lit_cell_style.border_color = Color(1.0, 0.95, 0.82, 0.20)
	lit_cell_style.border_width_left = 1
	lit_cell_style.border_width_top = 1
	lit_cell_style.border_width_right = 1
	lit_cell_style.border_width_bottom = 1
	lit_cell_style.corner_radius_top_left = 18
	lit_cell_style.corner_radius_top_right = 18
	lit_cell_style.corner_radius_bottom_right = 18
	lit_cell_style.corner_radius_bottom_left = 18

	blocker_style = StyleBoxFlat.new()
	blocker_style.bg_color = Color(0.14, 0.18, 0.18, 0.92)
	blocker_style.border_color = Color(0.78, 0.88, 0.86, 0.24)
	blocker_style.border_width_left = 1
	blocker_style.border_width_top = 1
	blocker_style.border_width_right = 1
	blocker_style.border_width_bottom = 1
	blocker_style.corner_radius_top_left = 16
	blocker_style.corner_radius_top_right = 16
	blocker_style.corner_radius_bottom_right = 16
	blocker_style.corner_radius_bottom_left = 16


func _board_rect() -> Rect2:
	var side: float = min(size.x, size.y) - board_padding * 2.0
	side = max(side, 120.0)
	return Rect2(Vector2((size.x - side) * 0.5, (size.y - side) * 0.5), Vector2(side, side))


func _cell_rect(board_rect: Rect2, cell: Vector2i) -> Rect2:
	var cell_size := board_rect.size.x / float(grid_size)
	var gap: float = max(4.0, cell_size * 0.08)
	return Rect2(
		board_rect.position + Vector2(cell.x, cell.y) * cell_size + Vector2(gap, gap),
		Vector2(cell_size - gap * 2.0, cell_size - gap * 2.0)
	)


func _cell_from_local(local_pos: Vector2) -> Vector2i:
	var board_rect := _board_rect()
	if not board_rect.has_point(local_pos):
		return INVALID_CELL

	var cell_size := board_rect.size.x / float(grid_size)
	var x := int(floor((local_pos.x - board_rect.position.x) / cell_size))
	var y := int(floor((local_pos.y - board_rect.position.y) / cell_size))
	if x < 0 or x >= grid_size or y < 0 or y >= grid_size:
		return INVALID_CELL
	return Vector2i(x, y)


func _grid_to_local(point: Vector2, board_rect: Rect2) -> Vector2:
	var cell_size := board_rect.size.x / float(grid_size)
	return board_rect.position + point * cell_size


func _cell_center(cell: Vector2i) -> Vector2:
	return Vector2(cell.x + 0.5, cell.y + 0.5)


func _cell_exit_point(cell: Vector2i, direction: Vector2i) -> Vector2:
	if direction == Vector2i.UP:
		return Vector2(cell.x + 0.5, cell.y)
	if direction == Vector2i.RIGHT:
		return Vector2(cell.x + 1.0, cell.y + 0.5)
	if direction == Vector2i.DOWN:
		return Vector2(cell.x + 0.5, cell.y + 1.0)
	return Vector2(cell.x, cell.y + 0.5)


func _source_start_point(side: String, index: int) -> Vector2:
	if side == "top":
		return Vector2(index + 0.5, -0.18)
	if side == "right":
		return Vector2(float(grid_size) + 0.18, index + 0.5)
	if side == "bottom":
		return Vector2(index + 0.5, float(grid_size) + 0.18)
	return Vector2(-0.18, index + 0.5)


func _source_entry_cell(side: String, index: int) -> Vector2i:
	if side == "top":
		return Vector2i(index, 0)
	if side == "right":
		return Vector2i(grid_size - 1, index)
	if side == "bottom":
		return Vector2i(index, grid_size - 1)
	return Vector2i(0, index)


func _direction_from_side(side: String) -> Vector2i:
	if side == "top":
		return Vector2i.DOWN
	if side == "right":
		return Vector2i.LEFT
	if side == "bottom":
		return Vector2i.UP
	return Vector2i.RIGHT


func _reflect(direction: Vector2i, rotation: int) -> Vector2i:
	var backslash_map := {
		Vector2i.UP: Vector2i.LEFT,
		Vector2i.RIGHT: Vector2i.DOWN,
		Vector2i.DOWN: Vector2i.RIGHT,
		Vector2i.LEFT: Vector2i.UP
	}
	var slash_map := {
		Vector2i.UP: Vector2i.RIGHT,
		Vector2i.RIGHT: Vector2i.UP,
		Vector2i.DOWN: Vector2i.LEFT,
		Vector2i.LEFT: Vector2i.DOWN
	}
	var lookup := backslash_map if wrapi(rotation, 0, 4) % 2 == 0 else slash_map
	return lookup.get(direction, Vector2i.DOWN)


func _is_inside(cell: Vector2i) -> bool:
	return cell.x >= 0 and cell.x < grid_size and cell.y >= 0 and cell.y < grid_size


func _cell_key(cell: Vector2i) -> String:
	return "%d:%d" % [cell.x, cell.y]
