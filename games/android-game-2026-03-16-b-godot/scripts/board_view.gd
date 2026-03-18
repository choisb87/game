extends Control

signal turns_changed(turns_left)
signal blooms_changed(lit_count, total_count)
signal level_completed
signal level_failed
signal interaction_feedback(message, emphasized)

const BEAM_FALLBACK := Color("ffe08a")
const INVALID_CELL := Vector2i(-1, -1)

var grid_size := 4
var max_moves := 0
var turns_used := 0
var board_padding := 24.0
var input_enabled := false

var pieces := {}
var blooms := []
var bloom_lookup := {}
var sources := []
var beam_segments := []
var lit_path_lookup := {}
var lit_bloom_lookup := {}

var completed := false
var failed := false
var pulse_cell := INVALID_CELL
var pulse_color := Color(0.0, 0.0, 0.0, 0.0)
var pulse_time_left := 0.0
var win_flash_time := 0.0
var rotate_flash_time := 0.0
var last_rotated_cell := INVALID_CELL
var ambient_time := 0.0

var palette := {}
var board_style: StyleBoxFlat
var cell_style: StyleBoxFlat
var lit_cell_style: StyleBoxFlat
var mirror_tile_style: StyleBoxFlat
var locked_tile_style: StyleBoxFlat
var blocker_style: StyleBoxFlat


func _ready() -> void:
	mouse_filter = MOUSE_FILTER_STOP
	set_process(true)


func _process(delta: float) -> void:
	ambient_time += delta
	var needs_redraw := true

	if pulse_time_left > 0.0:
		pulse_time_left = max(0.0, pulse_time_left - delta)
		if pulse_time_left == 0.0:
			pulse_cell = INVALID_CELL

	if rotate_flash_time > 0.0:
		rotate_flash_time = max(0.0, rotate_flash_time - delta)
		if rotate_flash_time == 0.0:
			last_rotated_cell = INVALID_CELL

	if win_flash_time > 0.0:
		win_flash_time = max(0.0, win_flash_time - delta)

	if needs_redraw:
		queue_redraw()


func get_turns_left() -> int:
	return max(0, max_moves - turns_used)


func get_total_blooms() -> int:
	return blooms.size()


func get_lit_bloom_count() -> int:
	return lit_bloom_lookup.size()


func get_source_count() -> int:
	return sources.size()


func get_rotatable_count() -> int:
	var count := 0
	for piece_data in pieces.values():
		var piece := piece_data as Dictionary
		if String(piece.get("type", "")) == "mirror" and not bool(piece.get("locked", false)):
			count += 1
	return count


func set_input_enabled(enabled: bool) -> void:
	input_enabled = enabled


func setup_level(level: Dictionary) -> void:
	_apply_theme(level)
	grid_size = int(level.get("grid_size", 4))
	max_moves = int(level.get("moves", 6))
	turns_used = 0
	completed = false
	failed = false
	pulse_cell = INVALID_CELL
	pulse_time_left = 0.0
	win_flash_time = 0.0
	rotate_flash_time = 0.0
	last_rotated_cell = INVALID_CELL

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

	for piece_data in level.get("pieces", []):
		var piece := piece_data as Dictionary
		var piece_pos := piece.get("pos", Vector2i.ZERO) as Vector2i
		pieces[_cell_key(piece_pos)] = {
			"pos": piece_pos,
			"type": String(piece.get("type", "mirror")),
			"rotation": int(piece.get("rotation", 0)),
			"locked": bool(piece.get("locked", false))
		}

	_recalculate()
	turns_changed.emit(get_turns_left())
	blooms_changed.emit(lit_bloom_lookup.size(), blooms.size())
	queue_redraw()


func _gui_input(event: InputEvent) -> void:
	if not input_enabled or completed or failed:
		return

	var local_event := make_input_local(event)
	var pointer_pos := Vector2.ZERO
	var should_handle := false

	if local_event is InputEventMouseButton:
		var mouse_event := local_event as InputEventMouseButton
		if mouse_event.button_index == MOUSE_BUTTON_LEFT and mouse_event.pressed:
			pointer_pos = mouse_event.position
			should_handle = true
	elif local_event is InputEventScreenTouch:
		var touch_event := local_event as InputEventScreenTouch
		if touch_event.pressed:
			pointer_pos = touch_event.position
			should_handle = true

	if not should_handle:
		return

	var cell := _cell_from_local(pointer_pos)
	if cell == INVALID_CELL:
		interaction_feedback.emit("보드 안의 유리 타일을 탭하세요.", false)
		accept_event()
		return

	var key := _cell_key(cell)
	if not pieces.has(key):
		interaction_feedback.emit("빈 칸입니다. 거울 타일이 있는 칸만 회전합니다.", false)
		_pulse(cell, Color(0.67, 0.81, 0.76, 0.50))
		accept_event()
		return

	var piece := pieces[key] as Dictionary
	var piece_type := String(piece.get("type", ""))
	if piece_type == "blocker":
		interaction_feedback.emit("차단 유리는 빛을 흡수합니다. 다른 경로를 찾으세요.", true)
		_pulse(cell, Color(0.97, 0.57, 0.49, 0.60))
		accept_event()
		return

	if bool(piece.get("locked", false)):
		interaction_feedback.emit("잠긴 거울은 회전할 수 없습니다.", true)
		_pulse(cell, Color(0.97, 0.57, 0.49, 0.60))
		accept_event()
		return

	piece["rotation"] = wrapi(int(piece.get("rotation", 0)) + 1, 0, 4)
	pieces[key] = piece
	turns_used += 1
	last_rotated_cell = cell
	rotate_flash_time = 0.28
	_recalculate()
	turns_changed.emit(get_turns_left())
	blooms_changed.emit(lit_bloom_lookup.size(), blooms.size())
	interaction_feedback.emit("거울을 회전했습니다. 새 광선 경로를 확인하세요.", false)
	_pulse(cell, Color(0.98, 0.88, 0.60, 0.62))

	if lit_bloom_lookup.size() == blooms.size():
		completed = true
		win_flash_time = 0.60
		interaction_feedback.emit("모든 꽃이 개화했습니다.", true)
		level_completed.emit()
	elif turns_used >= max_moves:
		failed = true
		interaction_feedback.emit("회전 기회를 모두 사용했습니다.", true)
		level_failed.emit()

	accept_event()
	queue_redraw()


func _draw() -> void:
	_ensure_styles()

	var board_rect := _board_rect()
	draw_style_box(board_style, board_rect)
	_draw_board_glass(board_rect)

	for y in range(grid_size):
		for x in range(grid_size):
			var cell := Vector2i(x, y)
			var key := _cell_key(cell)
			var cell_rect := _cell_rect(board_rect, cell)
			draw_style_box(lit_cell_style if lit_path_lookup.has(key) else cell_style, cell_rect)
			_draw_cell_sheen(cell_rect, lit_path_lookup.has(key))

	for segment in beam_segments:
		var from_point := _grid_to_local(segment["from"] as Vector2, board_rect)
		var to_point := _grid_to_local(segment["to"] as Vector2, board_rect)
		var beam_color := segment["color"] as Color
		draw_line(
			from_point, to_point, Color(beam_color.r, beam_color.g, beam_color.b, 0.12), 26.0, true
		)
		draw_line(
			from_point, to_point, Color(beam_color.r, beam_color.g, beam_color.b, 0.58), 12.0, true
		)
		draw_line(
			from_point, to_point, Color(1.0, 0.99, 0.92, 0.96), 4.0, true
		)
		draw_circle(from_point.lerp(to_point, 0.5), 4.0, Color(1.0, 0.98, 0.90, 0.28))

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
	_draw_rotate_flash(board_rect)
	_draw_pulse(board_rect)
	_draw_win_flash(board_rect)


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
		var outside := travel_point + Vector2(direction.x, direction.y) * 0.24
		segments.append({"from": travel_point, "to": outside, "color": beam_color})

	return {"segments": segments, "path": path_cells, "blooms": lit_blooms}


func _apply_theme(level: Dictionary) -> void:
	palette = (level.get("palette", {}) as Dictionary).duplicate(true)
	board_style = null
	cell_style = null
	lit_cell_style = null
	mirror_tile_style = null
	locked_tile_style = null
	blocker_style = null


func _draw_board_glass(board_rect: Rect2) -> void:
	var center := board_rect.get_center()
	var glow := _theme_color("glow", BEAM_FALLBACK)
	var side_glow := _theme_color("glow_secondary", Color("cdefff"))
	draw_circle(center, board_rect.size.x * 0.50, Color(glow.r, glow.g, glow.b, 0.04))
	draw_circle(
		center + Vector2(board_rect.size.x * 0.18, board_rect.size.y * 0.18),
		board_rect.size.x * 0.38,
		Color(side_glow.r, side_glow.g, side_glow.b, 0.04)
	)
	draw_rect(
		Rect2(board_rect.position + Vector2(22.0, 18.0), Vector2(board_rect.size.x - 44.0, 16.0)),
		Color(1.0, 1.0, 1.0, 0.06),
		true
	)


func _draw_cell_sheen(cell_rect: Rect2, lit: bool) -> void:
	var sheen := Color(1.0, 1.0, 1.0, 0.05 if lit else 0.03)
	draw_rect(
		Rect2(
			cell_rect.position + Vector2(cell_rect.size.x * 0.10, cell_rect.size.y * 0.10),
			Vector2(cell_rect.size.x * 0.80, cell_rect.size.y * 0.16)
		),
		sheen,
		true
	)
	draw_rect(
		Rect2(
			cell_rect.position + Vector2(cell_rect.size.x * 0.14, cell_rect.size.y * 0.18),
			Vector2(cell_rect.size.x * 0.28, cell_rect.size.y * 0.06)
		),
		Color(1.0, 1.0, 1.0, 0.08 if lit else 0.04),
		true
	)


func _draw_piece(cell_rect: Rect2, piece: Dictionary) -> void:
	var piece_type := String(piece.get("type", ""))
	if piece_type == "blocker":
		_draw_blocker(cell_rect)
		return

	var locked := bool(piece.get("locked", false))
	var tile_style := locked_tile_style if locked else mirror_tile_style
	var tile_rect := cell_rect.grow(-cell_rect.size.x * 0.10)
	draw_style_box(tile_style, tile_rect)

	var center := cell_rect.get_center()
	var rotation := wrapi(int(piece.get("rotation", 0)), 0, 4)
	var dir_a := Vector2.RIGHT.rotated(rotation * PI * 0.5)
	var dir_b := Vector2.RIGHT.rotated((rotation + 1) * PI * 0.5)
	var diamond_radius := cell_rect.size.x * 0.23
	var diamond := PackedVector2Array(
		[
			center + Vector2(0.0, -diamond_radius),
			center + Vector2(diamond_radius, 0.0),
			center + Vector2(0.0, diamond_radius),
			center + Vector2(-diamond_radius, 0.0)
		]
	)
	var glass := _theme_color("glass", Color(0.84, 0.96, 0.90, 0.26))
	var edge := _theme_color("board_border", Color(0.86, 0.92, 0.84, 0.22))
	draw_colored_polygon(diamond, Color(glass.r, glass.g, glass.b, 0.18))
	draw_polyline(
		PackedVector2Array([diamond[0], diamond[1], diamond[2], diamond[3], diamond[0]]),
		Color(edge.r, edge.g, edge.b, 0.46),
		2.0,
		true
	)
	draw_line(
		center + Vector2(-diamond_radius * 0.55, -diamond_radius * 0.20),
		center + Vector2(diamond_radius * 0.30, -diamond_radius * 0.64),
		Color(1.0, 1.0, 1.0, 0.18),
		2.0,
		true
	)

	var line_color := _theme_color("glow", Color("f4d08f"))
	var glow_color := Color(line_color.r, line_color.g, line_color.b, 0.24)
	var core_color := Color(1.0, 0.99, 0.94, 0.95)
	var reach := cell_rect.size.x * 0.28
	var point_a := center + dir_a * reach
	var point_b := center + dir_b * reach

	draw_line(center, point_a, glow_color, 18.0, true)
	draw_line(center, point_b, glow_color, 18.0, true)
	draw_line(center, point_a, Color(line_color.r, line_color.g, line_color.b, 0.82), 10.0, true)
	draw_line(center, point_b, Color(line_color.r, line_color.g, line_color.b, 0.82), 10.0, true)
	draw_line(center, point_a, core_color, 3.0, true)
	draw_line(center, point_b, core_color, 3.0, true)
	draw_circle(center, cell_rect.size.x * 0.10, Color(0.06, 0.09, 0.09, 0.95))
	draw_circle(center, cell_rect.size.x * 0.06, Color(line_color.r, line_color.g, line_color.b, 0.96))
	draw_circle(point_a, cell_rect.size.x * 0.034, core_color)
	draw_circle(point_b, cell_rect.size.x * 0.034, core_color)

	if input_enabled and not locked:
		var pulse := 0.10 + (sin(ambient_time * 2.8 + float(rotation)) + 1.0) * 0.03
		draw_arc(
			center,
			cell_rect.size.x * 0.32,
			0.0,
			TAU,
			40,
			Color(line_color.r, line_color.g, line_color.b, pulse),
			2.0,
			true
		)

	if locked:
		var lock_center := center + Vector2(cell_rect.size.x * 0.25, -cell_rect.size.y * 0.22)
		draw_arc(
			lock_center + Vector2(0.0, -cell_rect.size.x * 0.016),
			cell_rect.size.x * 0.05,
			PI,
			TAU,
			18,
			Color(0.95, 0.97, 0.94, 0.92),
			3.0,
			true
		)
		var body_rect := Rect2(
			lock_center + Vector2(-cell_rect.size.x * 0.046, -cell_rect.size.x * 0.02),
			Vector2(cell_rect.size.x * 0.092, cell_rect.size.x * 0.11)
		)
		draw_rect(body_rect, Color(0.09, 0.12, 0.12, 0.94), true)
		draw_rect(body_rect.grow(-1.0), Color(0.93, 0.97, 0.94, 0.18), false, 2.0)


func _draw_blocker(cell_rect: Rect2) -> void:
	var tile_rect := cell_rect.grow(-cell_rect.size.x * 0.10)
	draw_style_box(blocker_style, tile_rect)
	var crack_a := tile_rect.position + Vector2(tile_rect.size.x * 0.18, tile_rect.size.y * 0.24)
	var crack_b := tile_rect.position + Vector2(tile_rect.size.x * 0.62, tile_rect.size.y * 0.48)
	var crack_c := tile_rect.position + Vector2(tile_rect.size.x * 0.34, tile_rect.size.y * 0.76)
	var crack_d := tile_rect.position + Vector2(tile_rect.size.x * 0.82, tile_rect.size.y * 0.20)
	draw_line(crack_a, crack_b, Color(0.78, 0.86, 0.82, 0.28), 3.0, true)
	draw_line(crack_b, crack_c, Color(0.78, 0.86, 0.82, 0.18), 2.0, true)
	draw_line(crack_b, crack_d, Color(0.78, 0.86, 0.82, 0.18), 2.0, true)


func _draw_bloom(cell_rect: Rect2, lit: bool) -> void:
	var center := cell_rect.get_center()
	var stem_color := Color(0.43, 0.60, 0.42, 0.82) if lit else Color(0.30, 0.42, 0.32, 0.70)
	var petal_color := _theme_color("bloom", Color("f0f4d6"))
	var core_color := _theme_color("bloom_core", Color("f6c86a"))

	if lit:
		draw_circle(center, cell_rect.size.x * 0.30, Color(petal_color.r, petal_color.g, petal_color.b, 0.12))
		for angle in [0.0, PI / 3.0, PI * 2.0 / 3.0, PI, PI * 4.0 / 3.0, PI * 5.0 / 3.0]:
			var offset := Vector2.RIGHT.rotated(angle) * cell_rect.size.x * 0.16
			draw_circle(center + offset, cell_rect.size.x * 0.095, petal_color)
		draw_circle(center, cell_rect.size.x * 0.082, core_color)
		draw_circle(center, cell_rect.size.x * 0.036, Color(1.0, 0.96, 0.84, 0.95))
	else:
		var bud_top := center + Vector2(0.0, -cell_rect.size.x * 0.07)
		draw_circle(bud_top, cell_rect.size.x * 0.10, Color(0.68, 0.78, 0.70, 0.84))
		draw_colored_polygon(
			PackedVector2Array(
				[
					center + Vector2(-cell_rect.size.x * 0.09, -cell_rect.size.x * 0.02),
					center + Vector2(0.0, -cell_rect.size.x * 0.17),
					center + Vector2(cell_rect.size.x * 0.09, -cell_rect.size.x * 0.02),
					center + Vector2(0.0, cell_rect.size.x * 0.05)
				]
			),
			Color(0.54, 0.67, 0.58, 0.84)
		)

	draw_line(
		center + Vector2(0.0, cell_rect.size.x * 0.06),
		center + Vector2(0.0, cell_rect.size.x * 0.20),
		stem_color,
		3.0,
		true
	)


func _draw_sources(board_rect: Rect2) -> void:
	for source in sources:
		var source_data := source as Dictionary
		var side := String(source_data.get("side", "top"))
		var start_point := _source_start_point(side, int(source_data.get("index", 0)))
		var center := _grid_to_local(start_point, board_rect)
		var color := source_data.get("color", BEAM_FALLBACK) as Color
		var cell_radius := board_rect.size.x / float(grid_size)
		draw_circle(center, cell_radius * 0.17, Color(color.r, color.g, color.b, 0.18))
		draw_circle(center, cell_radius * 0.11, Color(color.r, color.g, color.b, 0.94))
		draw_circle(center, cell_radius * 0.05, Color(1.0, 0.98, 0.90, 0.96))

		var direction := _direction_from_side(side)
		var dir_vec := Vector2(direction.x, direction.y)
		var normal := Vector2(-dir_vec.y, dir_vec.x)
		var triangle := PackedVector2Array(
			[
				center + dir_vec * cell_radius * 0.30,
				center - dir_vec * cell_radius * 0.06 + normal * cell_radius * 0.10,
				center - dir_vec * cell_radius * 0.06 - normal * cell_radius * 0.10
			]
		)
		draw_colored_polygon(triangle, Color(1.0, 0.98, 0.92, 0.88))


func _draw_board_frame(board_rect: Rect2) -> void:
	var accent := _theme_color("board_border", Color(0.85, 0.93, 0.84, 0.16))
	draw_rect(board_rect, Color(accent.r, accent.g, accent.b, 0.10), false, 3.0)
	draw_rect(board_rect.grow(-12.0), Color(accent.r, accent.g, accent.b, 0.06), false, 1.0)

	var corners := [
		board_rect.position + Vector2(24.0, 0.0),
		board_rect.position + Vector2(board_rect.size.x - 24.0, 0.0),
		board_rect.position + Vector2(0.0, 24.0),
		board_rect.position + Vector2(board_rect.size.x, 24.0),
		board_rect.position + Vector2(24.0, board_rect.size.y),
		board_rect.position + Vector2(board_rect.size.x - 24.0, board_rect.size.y),
		board_rect.position + Vector2(0.0, board_rect.size.y - 24.0),
		board_rect.position + Vector2(board_rect.size.x, board_rect.size.y - 24.0)
	]
	draw_line(corners[0], board_rect.position + Vector2(board_rect.size.x * 0.22, 0.0), accent, 2.0, true)
	draw_line(corners[2], board_rect.position + Vector2(0.0, board_rect.size.y * 0.22), accent, 2.0, true)
	draw_line(
		corners[1], board_rect.position + Vector2(board_rect.size.x * 0.78, 0.0), accent, 2.0, true
	)
	draw_line(
		corners[3],
		board_rect.position + Vector2(board_rect.size.x, board_rect.size.y * 0.22),
		accent,
		2.0,
		true
	)
	draw_line(
		corners[4],
		board_rect.position + Vector2(board_rect.size.x * 0.22, board_rect.size.y),
		accent,
		2.0,
		true
	)
	draw_line(
		corners[6],
		board_rect.position + Vector2(0.0, board_rect.size.y * 0.78),
		accent,
		2.0,
		true
	)
	draw_line(
		corners[5],
		board_rect.position + Vector2(board_rect.size.x * 0.78, board_rect.size.y),
		accent,
		2.0,
		true
	)
	draw_line(
		corners[7],
		board_rect.position + Vector2(board_rect.size.x, board_rect.size.y * 0.78),
		accent,
		2.0,
		true
	)


func _draw_rotate_flash(board_rect: Rect2) -> void:
	if rotate_flash_time <= 0.0 or last_rotated_cell == INVALID_CELL:
		return

	var amount := rotate_flash_time / 0.28
	var cell_rect := _cell_rect(board_rect, last_rotated_cell)
	var center := cell_rect.get_center()
	var color := _theme_color("glow", Color("ffd972"))
	draw_arc(
		center,
		cell_rect.size.x * (0.28 + (1.0 - amount) * 0.12),
		0.0,
		TAU,
		32,
		Color(color.r, color.g, color.b, amount * 0.46),
		4.0,
		true
	)


func _draw_pulse(board_rect: Rect2) -> void:
	if pulse_time_left <= 0.0 or pulse_cell == INVALID_CELL:
		return

	var amount := pulse_time_left / 0.42
	var cell_rect := _cell_rect(board_rect, pulse_cell)
	var center := cell_rect.get_center()
	var radius := cell_rect.size.x * (0.34 + (1.0 - amount) * 0.10)
	var glow := Color(pulse_color.r, pulse_color.g, pulse_color.b, pulse_color.a * amount * 0.26)
	var ring := Color(pulse_color.r, pulse_color.g, pulse_color.b, pulse_color.a * amount)
	draw_circle(center, radius * 1.10, glow)
	draw_arc(center, radius, 0.0, TAU, 48, ring, 4.0, true)


func _draw_win_flash(board_rect: Rect2) -> void:
	if win_flash_time <= 0.0:
		return

	var amount := win_flash_time / 0.60
	draw_rect(board_rect, Color(1.0, 0.96, 0.78, amount * 0.14), true)
	draw_circle(
		board_rect.get_center(),
		board_rect.size.x * (0.34 + (1.0 - amount) * 0.10),
		Color(1.0, 0.94, 0.74, amount * 0.08)
	)


func _pulse(cell: Vector2i, color: Color) -> void:
	pulse_cell = cell
	pulse_color = color
	pulse_time_left = 0.42
	queue_redraw()


func _ensure_styles() -> void:
	if board_style != null:
		return

	var board_bg := _theme_color("board", Color(0.09, 0.15, 0.14, 0.82))
	var board_border := _theme_color("board_border", Color(0.78, 0.88, 0.80, 0.18))
	var cell_bg := _theme_color("cell", Color(0.20, 0.30, 0.28, 0.48))
	var cell_lit_bg := _theme_color("cell_lit", Color(0.34, 0.44, 0.31, 0.54))
	var glass := _theme_color("glass", Color(0.84, 0.96, 0.90, 0.26))

	board_style = StyleBoxFlat.new()
	board_style.bg_color = board_bg
	board_style.border_color = board_border
	board_style.border_width_left = 2
	board_style.border_width_top = 2
	board_style.border_width_right = 2
	board_style.border_width_bottom = 2
	board_style.corner_radius_top_left = 32
	board_style.corner_radius_top_right = 32
	board_style.corner_radius_bottom_right = 32
	board_style.corner_radius_bottom_left = 32
	board_style.shadow_color = Color(0.0, 0.0, 0.0, 0.26)
	board_style.shadow_size = 34
	board_style.shadow_offset = Vector2(0.0, 14.0)

	cell_style = StyleBoxFlat.new()
	cell_style.bg_color = cell_bg
	cell_style.border_color = Color(glass.r, glass.g, glass.b, 0.10)
	cell_style.border_width_left = 1
	cell_style.border_width_top = 1
	cell_style.border_width_right = 1
	cell_style.border_width_bottom = 1
	cell_style.corner_radius_top_left = 20
	cell_style.corner_radius_top_right = 20
	cell_style.corner_radius_bottom_right = 20
	cell_style.corner_radius_bottom_left = 20

	lit_cell_style = StyleBoxFlat.new()
	lit_cell_style.bg_color = cell_lit_bg
	lit_cell_style.border_color = Color(_theme_color("glow", BEAM_FALLBACK).r, _theme_color("glow", BEAM_FALLBACK).g, _theme_color("glow", BEAM_FALLBACK).b, 0.22)
	lit_cell_style.border_width_left = 1
	lit_cell_style.border_width_top = 1
	lit_cell_style.border_width_right = 1
	lit_cell_style.border_width_bottom = 1
	lit_cell_style.corner_radius_top_left = 20
	lit_cell_style.corner_radius_top_right = 20
	lit_cell_style.corner_radius_bottom_right = 20
	lit_cell_style.corner_radius_bottom_left = 20

	mirror_tile_style = StyleBoxFlat.new()
	mirror_tile_style.bg_color = Color(glass.r, glass.g, glass.b, 0.08)
	mirror_tile_style.border_color = Color(glass.r, glass.g, glass.b, 0.22)
	mirror_tile_style.border_width_left = 1
	mirror_tile_style.border_width_top = 1
	mirror_tile_style.border_width_right = 1
	mirror_tile_style.border_width_bottom = 1
	mirror_tile_style.corner_radius_top_left = 18
	mirror_tile_style.corner_radius_top_right = 18
	mirror_tile_style.corner_radius_bottom_right = 18
	mirror_tile_style.corner_radius_bottom_left = 18

	locked_tile_style = mirror_tile_style.duplicate()
	locked_tile_style.bg_color = Color(0.16, 0.20, 0.20, 0.82)
	locked_tile_style.border_color = Color(0.88, 0.92, 0.86, 0.18)

	blocker_style = StyleBoxFlat.new()
	blocker_style.bg_color = Color(0.10, 0.12, 0.12, 0.96)
	blocker_style.border_color = Color(0.74, 0.82, 0.78, 0.16)
	blocker_style.border_width_left = 1
	blocker_style.border_width_top = 1
	blocker_style.border_width_right = 1
	blocker_style.border_width_bottom = 1
	blocker_style.corner_radius_top_left = 16
	blocker_style.corner_radius_top_right = 16
	blocker_style.corner_radius_bottom_right = 16
	blocker_style.corner_radius_bottom_left = 16


func _board_rect() -> Rect2:
	var min_side: float = minf(size.x, size.y)
	board_padding = clampf(min_side * 0.045, 18.0, 34.0)
	var side: float = min_side - board_padding * 2.0
	side = clampf(side, 160.0, 980.0)
	return Rect2(Vector2((size.x - side) * 0.5, (size.y - side) * 0.5), Vector2(side, side))


func _cell_rect(board_rect: Rect2, cell: Vector2i) -> Rect2:
	var cell_size := board_rect.size.x / float(grid_size)
	var gap: float = maxf(5.0, cell_size * 0.08)
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
	var lookup_map := {
		0: {
			Vector2i.LEFT: Vector2i.UP,
			Vector2i.DOWN: Vector2i.RIGHT
		},
		1: {
			Vector2i.UP: Vector2i.RIGHT,
			Vector2i.LEFT: Vector2i.DOWN
		},
		2: {
			Vector2i.RIGHT: Vector2i.DOWN,
			Vector2i.UP: Vector2i.LEFT
		},
		3: {
			Vector2i.RIGHT: Vector2i.UP,
			Vector2i.DOWN: Vector2i.LEFT
		}
	}
	var lookup: Dictionary = lookup_map.get(wrapi(rotation, 0, 4), {}) as Dictionary
	return lookup.get(direction, direction)


func _is_inside(cell: Vector2i) -> bool:
	return cell.x >= 0 and cell.x < grid_size and cell.y >= 0 and cell.y < grid_size


func _cell_key(cell: Vector2i) -> String:
	return "%d:%d" % [cell.x, cell.y]


func _theme_color(key: String, fallback: Color) -> Color:
	return palette.get(key, fallback) as Color
