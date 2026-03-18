extends Control

const LEAF_TEXTURE := preload("res://assets/art/leaf_tile.svg")

var pulse := 0.0
var drift_particles := []
var palette := {
	"sky_top": Color("132623"),
	"sky_mid": Color("17312d"),
	"sky_bottom": Color("091214"),
	"mist": Color(0.52, 0.73, 0.61, 0.22),
	"glow": Color("ffd972"),
	"glow_secondary": Color("cdefff")
}


func _ready() -> void:
	randomize()
	mouse_filter = MOUSE_FILTER_IGNORE
	for index in range(34):
		drift_particles.append(
			{
				"seed": randf_range(0.0, TAU),
				"radius": randf_range(16.0, 58.0),
				"speed": randf_range(0.010, 0.040),
				"position": Vector2(randf(), randf()),
				"alpha": randf_range(0.025, 0.10),
				"bias": float(index % 3) / 2.0
			}
		)
	set_process(true)


func configure_level(level: Dictionary) -> void:
	palette = (level.get("palette", {}) as Dictionary).duplicate(true)
	queue_redraw()


func _process(delta: float) -> void:
	pulse += delta
	queue_redraw()


func _draw() -> void:
	_draw_vertical_gradient()
	_draw_glow_fields()
	_draw_greenhouse_arches()
	_draw_light_shafts()
	_draw_leaf_pattern()
	_draw_drift_particles()
	_draw_vignette()


func _draw_vertical_gradient() -> void:
	var top := _color("sky_top", Color("132623"))
	var mid := _color("sky_mid", Color("17312d"))
	var bottom := _color("sky_bottom", Color("091214"))
	var bands := 26
	for index in range(bands):
		var t0 := float(index) / float(bands)
		var t1 := float(index + 1) / float(bands)
		var y := size.y * t0
		var band_height := size.y * (t1 - t0) + 1.0
		var base := top.lerp(mid, min(t0 * 1.55, 1.0))
		var color := base.lerp(bottom, pow(t0, 1.35))
		draw_rect(Rect2(0.0, y, size.x, band_height), color, true)


func _draw_glow_fields() -> void:
	var glow := _color("glow", Color("ffd972"))
	var side_glow := _color("glow_secondary", Color("cdefff"))
	var mist := _color("mist", Color(0.52, 0.73, 0.61, 0.22))

	draw_circle(Vector2(size.x * 0.18, size.y * 0.14), size.x * 0.30, Color(glow.r, glow.g, glow.b, 0.10))
	draw_circle(Vector2(size.x * 0.84, size.y * 0.24), size.x * 0.22, Color(side_glow.r, side_glow.g, side_glow.b, 0.08))
	draw_circle(Vector2(size.x * 0.46, size.y * 0.82), size.x * 0.44, Color(mist.r, mist.g, mist.b, 0.12))
	draw_circle(Vector2(size.x * 0.50, size.y * 0.52), size.x * 0.36, Color(1.0, 1.0, 1.0, 0.03))


func _draw_greenhouse_arches() -> void:
	var line_color := Color(0.82, 0.90, 0.86, 0.08)
	for index in range(5):
		var scale := 0.48 + float(index) * 0.12
		var center := Vector2(size.x * 0.50, size.y * 1.02)
		var radius := size.x * scale
		var alpha := 0.05 + float(index) * 0.012
		var arch_color := Color(line_color.r, line_color.g, line_color.b, alpha)
		draw_arc(center, radius, PI, TAU, 72, arch_color, 2.0, true)
		draw_line(
			Vector2(center.x - radius, size.y * 0.12),
			Vector2(center.x - radius, size.y),
			Color(arch_color.r, arch_color.g, arch_color.b, alpha * 0.65),
			2.0,
			true
		)
		draw_line(
			Vector2(center.x + radius, size.y * 0.12),
			Vector2(center.x + radius, size.y),
			Color(arch_color.r, arch_color.g, arch_color.b, alpha * 0.65),
			2.0,
			true
		)


func _draw_light_shafts() -> void:
	var glow := _color("glow", Color("ffd972"))
	var side_glow := _color("glow_secondary", Color("cdefff"))
	var shafts := [
		{
			"points": PackedVector2Array(
				[
					Vector2(size.x * 0.10, 0.0),
					Vector2(size.x * 0.24, 0.0),
					Vector2(size.x * 0.52, size.y),
					Vector2(size.x * 0.34, size.y)
				]
			),
			"color": Color(glow.r, glow.g, glow.b, 0.05)
		},
		{
			"points": PackedVector2Array(
				[
					Vector2(size.x * 0.72, 0.0),
					Vector2(size.x * 0.84, 0.0),
					Vector2(size.x * 1.00, size.y),
					Vector2(size.x * 0.86, size.y)
				]
			),
			"color": Color(side_glow.r, side_glow.g, side_glow.b, 0.05)
		}
	]

	for shaft in shafts:
		draw_colored_polygon(shaft["points"] as PackedVector2Array, shaft["color"] as Color)


func _draw_leaf_pattern() -> void:
	var tint := _color("mist", Color(0.52, 0.73, 0.61, 0.22))
	draw_texture_rect(
		LEAF_TEXTURE,
		Rect2(Vector2(-128.0, -96.0), size + Vector2(256.0, 192.0)),
		true,
		Color(tint.r, tint.g, tint.b, 0.12)
	)


func _draw_drift_particles() -> void:
	var glow := _color("glow", Color("ffd972"))
	var side_glow := _color("glow_secondary", Color("cdefff"))
	for particle in drift_particles:
		var seed := float(particle["seed"])
		var alpha := float(particle["alpha"])
		var speed := float(particle["speed"])
		var radius := float(particle["radius"])
		var base := particle["position"] as Vector2
		var bias := float(particle["bias"])
		var tint := glow.lerp(side_glow, bias)

		var x := fposmod(base.x + sin(pulse * 0.20 + seed) * 0.04, 1.20)
		var y := fposmod(base.y + pulse * speed, 1.24)
		var wobble := sin(pulse * 0.75 + seed) * 8.0
		var center := Vector2(x * size.x, y * size.y + wobble)
		draw_circle(center, radius + sin(pulse + seed) * 4.0, Color(tint.r, tint.g, tint.b, alpha))


func _draw_vignette() -> void:
	draw_circle(Vector2(0.0, 0.0), size.x * 0.44, Color(0.0, 0.0, 0.0, 0.16))
	draw_circle(Vector2(size.x, 0.0), size.x * 0.40, Color(0.0, 0.0, 0.0, 0.14))
	draw_circle(Vector2(0.0, size.y), size.x * 0.46, Color(0.0, 0.0, 0.0, 0.18))
	draw_circle(Vector2(size.x, size.y), size.x * 0.42, Color(0.0, 0.0, 0.0, 0.18))
	draw_rect(Rect2(0.0, size.y * 0.92, size.x, size.y * 0.10), Color(0.0, 0.0, 0.0, 0.16), true)


func _color(key: String, fallback: Color) -> Color:
	return palette.get(key, fallback) as Color
