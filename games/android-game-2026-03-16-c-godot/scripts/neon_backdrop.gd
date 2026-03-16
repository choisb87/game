extends Control

@export var scene_kind := "menu"

var _time := 0.0
var _cached_size := Vector2.ZERO
var _buildings := []
var _beams := []
var _sparkles := []


func _ready() -> void:
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	_rebuild()
	queue_redraw()


func _process(delta: float) -> void:
	_time += delta
	if size != _cached_size:
		_rebuild()
	queue_redraw()


func _rebuild() -> void:
	_cached_size = size
	_buildings.clear()
	_beams.clear()
	_sparkles.clear()

	if _cached_size.x <= 0.0 or _cached_size.y <= 0.0:
		return

	var rng := RandomNumberGenerator.new()
	rng.seed = int(String(scene_kind).hash())

	var base_y := _cached_size.y * 0.78
	var cursor := 0.0
	while cursor < _cached_size.x + 80.0:
		var width := rng.randf_range(64.0, 176.0)
		var height := rng.randf_range(160.0, 460.0)
		var hue_shift := rng.randf_range(-0.08, 0.08)
		_buildings.append(
			{
				"x": cursor,
				"width": width,
				"height": height,
				"hue_shift": hue_shift,
				"window_stride": rng.randi_range(18, 28)
			}
		)
		cursor += width - rng.randf_range(10.0, 26.0)

	for index in range(4):
		var start_x := rng.randf_range(-_cached_size.x * 0.2, _cached_size.x * 0.8)
		var end_x := start_x + rng.randf_range(160.0, 420.0)
		var end_y := rng.randf_range(_cached_size.y * 0.18, _cached_size.y * 0.58)
		_beams.append(
			{
				"start_x": start_x,
				"end_x": end_x,
				"end_y": end_y,
				"thickness": rng.randf_range(110.0, 220.0),
				"speed": rng.randf_range(0.2, 0.55)
			}
		)

	for _index in range(32):
		_sparkles.append(
			{
				"pos": Vector2(rng.randf_range(0.0, _cached_size.x), rng.randf_range(0.0, _cached_size.y * 0.7)),
				"radius": rng.randf_range(1.0, 3.4),
				"phase": rng.randf_range(0.0, TAU),
				"speed": rng.randf_range(0.6, 1.6)
			}
		)


func _draw() -> void:
	if _cached_size.x <= 0.0 or _cached_size.y <= 0.0:
		return

	var palette := _get_palette()
	var rect := Rect2(Vector2.ZERO, _cached_size)

	_draw_gradient(rect, palette)
	_draw_light_beams(rect, palette)
	_draw_halo(rect, palette)
	_draw_grid(rect, palette)
	_draw_city(rect, palette)
	_draw_scanlines(rect, palette)
	_draw_sparkles(palette)


func _draw_gradient(rect: Rect2, palette: Dictionary) -> void:
	var top: Color = palette["top"]
	var mid: Color = palette["mid"]
	var bottom: Color = palette["bottom"]
	var steps := 24

	for index in range(steps):
		var t0 := float(index) / float(steps - 1)
		var blend := top.lerp(mid, min(t0 * 1.5, 1.0))
		var final_color := blend.lerp(bottom, max((t0 - 0.45) * 1.8, 0.0))
		var y := rect.size.y * t0
		var band_height := rect.size.y / float(steps) + 2.0
		draw_rect(Rect2(0.0, y, rect.size.x, band_height), final_color, true)


func _draw_light_beams(rect: Rect2, palette: Dictionary) -> void:
	var beam_color: Color = palette["beam"]

	for beam in _beams:
		var drift := sin(_time * beam["speed"] + float(beam["start_x"]) * 0.01) * 42.0
		var start_a := Vector2(float(beam["start_x"]) + drift, rect.size.y)
		var start_b := Vector2(float(beam["start_x"]) + float(beam["thickness"]) + drift, rect.size.y)
		var end_b := Vector2(float(beam["end_x"]) + float(beam["thickness"]) * 0.38 + drift, float(beam["end_y"]))
		var end_a := Vector2(float(beam["end_x"]) + drift, float(beam["end_y"]))
		draw_colored_polygon(PackedVector2Array([start_a, start_b, end_b, end_a]), beam_color)


func _draw_halo(rect: Rect2, palette: Dictionary) -> void:
	var center := Vector2(rect.size.x * 0.5, rect.size.y * 0.28)
	var base_radius := rect.size.x * 0.18
	var pulse := sin(_time * 1.5) * 18.0
	var halo_color: Color = palette["halo"]
	var ring_color: Color = palette["ring"]

	draw_circle(center, base_radius + 36.0 + pulse, Color(halo_color.r, halo_color.g, halo_color.b, 0.08))
	draw_circle(center, base_radius + pulse, Color(halo_color.r, halo_color.g, halo_color.b, 0.22))
	draw_arc(center, base_radius + 56.0 + pulse, PI * 0.15, PI * 0.85, 64, ring_color, 4.0, true)
	draw_arc(center, base_radius + 56.0 + pulse, PI * 1.15, PI * 1.85, 64, ring_color, 4.0, true)


func _draw_grid(rect: Rect2, palette: Dictionary) -> void:
	var grid_color: Color = palette["grid"]
	var horizon_y := rect.size.y * 0.62

	for index in range(12):
		var y := horizon_y + pow(float(index) / 12.0, 1.6) * rect.size.y * 0.44
		draw_line(Vector2(0.0, y), Vector2(rect.size.x, y), grid_color, 2.0)

	for index in range(-6, 7):
		var t := float(index) / 6.0
		var top := Vector2(rect.size.x * 0.5 + t * 80.0, horizon_y)
		var bottom := Vector2(rect.size.x * 0.5 + t * rect.size.x * 0.62, rect.size.y)
		draw_line(top, bottom, grid_color, 2.0)


func _draw_city(rect: Rect2, palette: Dictionary) -> void:
	var base_y := rect.size.y * 0.78
	var building_color: Color = palette["building"]
	var window_color: Color = palette["window"]

	for building in _buildings:
		var pos := Vector2(float(building["x"]), base_y - float(building["height"]))
		var size_local := Vector2(float(building["width"]), float(building["height"]))
		var tint := clampf(0.12 + float(building["hue_shift"]), -0.08, 0.18)
		var color := Color(
			clampf(building_color.r + tint * 0.2, 0.0, 1.0),
			clampf(building_color.g + tint * 0.35, 0.0, 1.0),
			clampf(building_color.b + tint * 0.5, 0.0, 1.0),
			building_color.a
		)
		draw_rect(Rect2(pos, size_local), color, true)
		draw_rect(Rect2(Vector2(pos.x, base_y), Vector2(size_local.x, rect.size.y - base_y)), Color(0.01, 0.03, 0.07, 0.95), true)

		var window_stride := int(building["window_stride"])
		var x := pos.x + 10.0
		while x < pos.x + size_local.x - 14.0:
			var y := pos.y + 16.0
			while y < pos.y + size_local.y - 18.0:
				var alpha := 0.08 + 0.16 * max(sin(_time * 1.1 + x * 0.02 + y * 0.01), 0.0)
				draw_rect(Rect2(Vector2(x, y), Vector2(8.0, 14.0)), Color(window_color.r, window_color.g, window_color.b, alpha), true)
				y += 24.0
			x += float(window_stride)


func _draw_scanlines(rect: Rect2, palette: Dictionary) -> void:
	var line_color: Color = palette["scanline"]
	var y := 0.0
	while y < rect.size.y:
		draw_line(Vector2(0.0, y), Vector2(rect.size.x, y), line_color, 1.0)
		y += 5.0


func _draw_sparkles(palette: Dictionary) -> void:
	var sparkle_color: Color = palette["sparkle"]
	for sparkle in _sparkles:
		var pos: Vector2 = sparkle["pos"]
		var alpha := 0.35 + max(sin(_time * sparkle["speed"] + sparkle["phase"]), 0.0) * 0.45
		draw_circle(pos, sparkle["radius"], Color(sparkle_color.r, sparkle_color.g, sparkle_color.b, alpha))


func _get_palette() -> Dictionary:
	match scene_kind:
		"game":
			return {
				"top": Color(0.01, 0.05, 0.11, 1.0),
				"mid": Color(0.08, 0.10, 0.20, 1.0),
				"bottom": Color(0.01, 0.02, 0.06, 1.0),
				"beam": Color(0.14, 0.82, 0.98, 0.06),
				"halo": Color(0.17, 0.87, 0.95, 1.0),
				"ring": Color(0.96, 0.47, 0.65, 0.52),
				"grid": Color(0.10, 0.46, 0.68, 0.28),
				"building": Color(0.04, 0.08, 0.14, 0.96),
				"window": Color(0.97, 0.77, 0.33, 1.0),
				"scanline": Color(0.88, 0.95, 1.0, 0.018),
				"sparkle": Color(0.78, 0.95, 1.0, 1.0)
			}
		"result":
			return {
				"top": Color(0.05, 0.04, 0.12, 1.0),
				"mid": Color(0.12, 0.08, 0.20, 1.0),
				"bottom": Color(0.03, 0.02, 0.07, 1.0),
				"beam": Color(0.99, 0.44, 0.60, 0.06),
				"halo": Color(0.99, 0.74, 0.29, 1.0),
				"ring": Color(0.18, 0.84, 0.96, 0.48),
				"grid": Color(0.31, 0.22, 0.62, 0.24),
				"building": Color(0.06, 0.05, 0.12, 0.96),
				"window": Color(0.99, 0.74, 0.29, 1.0),
				"scanline": Color(0.95, 0.92, 1.0, 0.018),
				"sparkle": Color(1.0, 0.92, 0.84, 1.0)
			}
		_:
			return {
				"top": Color(0.02, 0.03, 0.09, 1.0),
				"mid": Color(0.09, 0.06, 0.18, 1.0),
				"bottom": Color(0.02, 0.02, 0.06, 1.0),
				"beam": Color(0.12, 0.84, 0.98, 0.06),
				"halo": Color(0.98, 0.49, 0.66, 1.0),
				"ring": Color(0.15, 0.86, 0.94, 0.46),
				"grid": Color(0.22, 0.18, 0.52, 0.22),
				"building": Color(0.05, 0.06, 0.13, 0.96),
				"window": Color(0.96, 0.80, 0.35, 1.0),
				"scanline": Color(0.96, 0.94, 1.0, 0.018),
				"sparkle": Color(0.86, 0.98, 1.0, 1.0)
			}

