extends Control

var pulse := 0.0
var drift_particles := []


func _ready() -> void:
	randomize()
	mouse_filter = MOUSE_FILTER_IGNORE
	for index in range(28):
		drift_particles.append(
			{
				"seed": randf_range(0.0, TAU),
				"radius": randf_range(18.0, 58.0),
				"speed": randf_range(0.012, 0.045),
				"position": Vector2(randf(), randf()),
				"alpha": randf_range(0.035, 0.12),
				"tint": Color(0.84, 0.97 - float(index % 5) * 0.03, 0.82, 1.0)
			}
		)
	set_process(true)


func _process(delta: float) -> void:
	pulse += delta
	queue_redraw()


func _draw() -> void:
	draw_rect(Rect2(Vector2.ZERO, size), Color("0d1514"), true)

	draw_circle(Vector2(size.x * 0.18, size.y * 0.12), size.x * 0.30, Color(0.18, 0.27, 0.24, 0.28))
	draw_circle(Vector2(size.x * 0.84, size.y * 0.24), size.x * 0.24, Color(0.25, 0.18, 0.14, 0.18))
	draw_circle(Vector2(size.x * 0.50, size.y * 0.82), size.x * 0.42, Color(0.09, 0.18, 0.16, 0.30))

	for particle in drift_particles:
		var seed := float(particle["seed"])
		var alpha := float(particle["alpha"])
		var speed := float(particle["speed"])
		var radius := float(particle["radius"])
		var base := particle["position"] as Vector2
		var tint := particle["tint"] as Color

		var x := fposmod(base.x + sin(pulse * 0.20 + seed) * 0.04, 1.15)
		var y := fposmod(base.y + pulse * speed, 1.20)
		var wobble := sin(pulse * 0.75 + seed) * 8.0
		var center := Vector2(x * size.x, y * size.y + wobble)
		draw_circle(center, radius + sin(pulse + seed) * 4.0, Color(tint.r, tint.g, tint.b, alpha))

