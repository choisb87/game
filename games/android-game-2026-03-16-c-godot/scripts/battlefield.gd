extends Node2D

signal score_changed(score: int)
signal shield_changed(value: float, max_value: float)
signal charge_changed(value: float, max_value: float)
signal time_changed(time_left: float)
signal combo_changed(combo: int, multiplier: int)
signal status_changed(text: String)
signal run_finished(result: Dictionary)

const STAGE_DURATION := 45.0
const MAX_SHIELD := 100.0
const MAX_CHARGE := 100.0
const PLAYER_SPEED := 900.0
const AUTO_FIRE_INTERVAL := 0.14

var rng := RandomNumberGenerator.new()
var view_size := Vector2.ZERO
var play_rect := Rect2()
var control_rect := Rect2()

var player_pos := Vector2.ZERO
var player_target := Vector2.ZERO
var shield := MAX_SHIELD
var charge := 0.0
var score := 0
var combo := 0
var multiplier := 1
var combo_timeout := 0.0
var time_left := STAGE_DURATION
var elapsed := 0.0
var auto_fire_timer := AUTO_FIRE_INTERVAL
var spawn_timer := 0.8
var shake_strength := 0.0
var flash_strength := 0.0
var kills := 0
var overdrives_used := 0
var alive := false
var finished := false
var invincible_timer := 0.0
var drag_pointer := -1
var dragging := false
var mouse_dragging := false
var pointer_target := Vector2.ZERO
var status_text := ""
var status_timeout := 0.0

var enemies := []
var player_bullets := []
var enemy_bullets := []
var particles := []
var pickups := []
var shockwaves := []
var score_popups := []
var stars := []
var skyline := []


func _ready() -> void:
	rng.randomize()
	_refresh_view()
	_build_backdrop()
	set_process(true)
	set_physics_process(true)
	queue_redraw()


func begin() -> void:
	_refresh_view()
	_build_backdrop()

	player_pos = Vector2(play_rect.get_center().x, play_rect.position.y + play_rect.size.y * 0.82)
	player_target = player_pos
	pointer_target = player_pos
	shield = MAX_SHIELD
	charge = 38.0
	score = 0
	combo = 0
	multiplier = 1
	combo_timeout = 0.0
	time_left = STAGE_DURATION
	elapsed = 0.0
	auto_fire_timer = 0.1
	spawn_timer = 0.75
	shake_strength = 0.0
	flash_strength = 0.0
	kills = 0
	overdrives_used = 0
	alive = true
	finished = false
	drag_pointer = -1
	dragging = false
	mouse_dragging = false
	status_timeout = 0.0
	invincible_timer = 1.2

	enemies.clear()
	player_bullets.clear()
	enemy_bullets.clear()
	particles.clear()
	pickups.clear()
	shockwaves.clear()
	score_popups.clear()

	_emit_full_state()
	_set_status("드래그로 기체 이동, 우측 버튼으로 오버드라이브 발동", 2.6)
	queue_redraw()


func request_overdrive() -> void:
	if not alive or finished:
		return

	if charge < MAX_CHARGE:
		_set_status("오버드라이브가 아직 충전되지 않았습니다", 1.2)
		return

	charge = 0.0
	overdrives_used += 1
	shake_strength = max(shake_strength, 26.0)
	flash_strength = max(flash_strength, 0.8)
	shockwaves.append({"radius": 24.0, "life": 0.55, "pos": player_pos})
	charge_changed.emit(charge, MAX_CHARGE)

	var survivors := []
	for enemy in enemies:
		var pos = enemy["pos"]
		var distance: float = pos.distance_to(player_pos)
		if distance < 300.0:
			var reward := int(enemy["score"]) + 18
			_destroy_enemy(enemy, reward, true)
		else:
			enemy["hp"] = float(enemy["hp"]) - clampf(180.0 - distance * 0.22, 22.0, 90.0)
			if float(enemy["hp"]) <= 0.0:
				var reward_far := int(enemy["score"])
				_destroy_enemy(enemy, reward_far, true)
			else:
				survivors.append(enemy)
	enemies = survivors

	var remaining_bullets := []
	for bullet in enemy_bullets:
		var bullet_pos = bullet["pos"]
		if bullet_pos.distance_to(player_pos) > 320.0:
			remaining_bullets.append(bullet)
		else:
			_spawn_ring(bullet_pos, Color(1.0, 0.92, 0.78, 0.7), 8, 140.0)
	enemy_bullets = remaining_bullets

	_set_status("오버드라이브 발동", 1.4)
	queue_redraw()


func _process(delta: float) -> void:
	if get_viewport_rect().size != view_size:
		_refresh_view()
		_build_backdrop()

	shake_strength = move_toward(shake_strength, 0.0, delta * 70.0)
	flash_strength = move_toward(flash_strength, 0.0, delta * 1.8)
	queue_redraw()


func _physics_process(delta: float) -> void:
	if stars.is_empty():
		_build_backdrop()

	_tick_backdrop(delta)
	_tick_particles(delta)
	_tick_shockwaves(delta)

	if not alive or finished:
		queue_redraw()
		return

	elapsed += delta
	time_left = max(STAGE_DURATION - elapsed, 0.0)
	time_changed.emit(time_left)

	if time_left <= 0.0:
		_finish_run(true)
		return

	invincible_timer = max(invincible_timer - delta, 0.0)
	_update_player(delta)
	_handle_auto_fire(delta)
	_handle_spawning(delta)
	_update_bullets(delta)
	_update_enemies(delta)
	_update_pickups(delta)
	_tick_combo(delta)
	_tick_score_popups(delta)
	_check_collisions()
	queue_redraw()


func _input(event: InputEvent) -> void:
	if finished:
		return

	if event is InputEventKey and event.pressed and not event.echo:
		if event.keycode == KEY_SPACE:
			request_overdrive()
			return

	if not alive:
		return

	if event is InputEventScreenTouch:
		if event.pressed:
			drag_pointer = event.index
			dragging = true
			_set_pointer_target(event.position)
		elif drag_pointer == event.index:
			dragging = false
			drag_pointer = -1
	elif event is InputEventScreenDrag:
		if drag_pointer == event.index:
			dragging = true
			_set_pointer_target(event.position)
	elif event is InputEventMouseButton and event.button_index == MOUSE_BUTTON_LEFT:
		mouse_dragging = event.pressed
		if mouse_dragging:
			_set_pointer_target(event.position)
	elif event is InputEventMouseMotion and mouse_dragging:
		_set_pointer_target(event.position)


func _update_player(delta: float) -> void:
	var keyboard := Vector2.ZERO
	if Input.is_key_pressed(KEY_A) or Input.is_key_pressed(KEY_LEFT):
		keyboard.x -= 1.0
	if Input.is_key_pressed(KEY_D) or Input.is_key_pressed(KEY_RIGHT):
		keyboard.x += 1.0
	if Input.is_key_pressed(KEY_W) or Input.is_key_pressed(KEY_UP):
		keyboard.y -= 1.0
	if Input.is_key_pressed(KEY_S) or Input.is_key_pressed(KEY_DOWN):
		keyboard.y += 1.0

	if keyboard.length() > 0.0:
		player_target = player_pos + keyboard.normalized() * PLAYER_SPEED * delta
	elif dragging or mouse_dragging:
		player_target = pointer_target

	player_target = _clamp_to_playfield(player_target)
	player_pos = player_pos.move_toward(player_target, PLAYER_SPEED * delta)


func _handle_auto_fire(delta: float) -> void:
	auto_fire_timer -= delta
	if auto_fire_timer > 0.0:
		return

	auto_fire_timer = AUTO_FIRE_INTERVAL
	_spawn_player_bullet(player_pos + Vector2(-22.0, -36.0), Vector2(-30.0, -1300.0))
	_spawn_player_bullet(player_pos + Vector2(22.0, -36.0), Vector2(30.0, -1300.0))

	if multiplier >= 4:
		_spawn_player_bullet(player_pos + Vector2(0.0, -46.0), Vector2(0.0, -1420.0))

	_spawn_ring(player_pos + Vector2(0.0, -44.0), Color(0.26, 0.92, 0.98, 0.45), 4, 52.0)


func _handle_spawning(delta: float) -> void:
	spawn_timer -= delta
	if spawn_timer > 0.0:
		return

	var wave := _current_wave()
	spawn_timer = clampf(0.92 - float(wave) * 0.08 - elapsed * 0.004, 0.34, 0.92)

	var pattern_roll := rng.randi_range(0, 3)
	if wave >= 3 and rng.randf() < 0.25:
		_spawn_tank(Vector2(play_rect.get_center().x + rng.randf_range(-140.0, 140.0), play_rect.position.y - 90.0))

	match pattern_roll:
		0:
			_spawn_line(wave)
		1:
			_spawn_diagonal(wave, -1 if rng.randf() < 0.5 else 1)
		2:
			_spawn_arc(wave)
		_:
			_spawn_swoopers(wave)


func _update_bullets(delta: float) -> void:
	var updated_player_bullets := []
	for bullet in player_bullets:
		bullet["pos"] += bullet["vel"] * delta
		bullet["life"] = float(bullet["life"]) - delta
		var bullet_pos = bullet["pos"]
		if float(bullet["life"]) > 0.0 and bullet_pos.y > play_rect.position.y - 120.0:
			updated_player_bullets.append(bullet)
	player_bullets = updated_player_bullets

	var updated_enemy_bullets := []
	for bullet in enemy_bullets:
		bullet["pos"] += bullet["vel"] * delta
		bullet["life"] = float(bullet["life"]) - delta
		var bullet_pos = bullet["pos"]
		if float(bullet["life"]) > 0.0 and bullet_pos.y < play_rect.end.y + 120.0:
			updated_enemy_bullets.append(bullet)
	enemy_bullets = updated_enemy_bullets


func _update_enemies(delta: float) -> void:
	var survivors := []
	for enemy in enemies:
		enemy["age"] = float(enemy["age"]) + delta
		enemy["fire_timer"] = float(enemy["fire_timer"]) - delta

		var pos = enemy["pos"]
		var vel = enemy["vel"]
		pos += vel * delta

		match String(enemy["kind"]):
			"swooper":
				pos.x += sin(float(enemy["age"]) * 4.2 + float(enemy["phase"])) * 220.0 * delta
			"tank":
				pos.x += sin(float(enemy["age"]) * 2.1 + float(enemy["phase"])) * 70.0 * delta
			"zigzag":
				pos.x += sin(float(enemy["age"]) * 5.0 + float(enemy["phase"])) * 280.0 * delta

		enemy["pos"] = pos

		if pos.y < play_rect.position.y - 240.0:
			survivors.append(enemy)
			continue

		if pos.y > play_rect.end.y + 140.0 or pos.x < play_rect.position.x - 180.0 or pos.x > play_rect.end.x + 180.0:
			continue

		if float(enemy["fire_timer"]) <= 0.0 and pos.y > play_rect.position.y + 60.0:
			enemy["fire_timer"] = float(enemy["fire_interval"])
			_fire_enemy_shot(enemy)

		survivors.append(enemy)
	enemies = survivors


func _update_pickups(delta: float) -> void:
	var remaining := []
	for pickup in pickups:
		pickup["life"] = float(pickup["life"]) - delta
		pickup["age"] = float(pickup["age"]) + delta
		var pos = pickup["pos"]
		var vel = pickup["vel"]
		var pull: Vector2 = player_pos - pos
		if pull.length() < 320.0:
			vel = vel.lerp(pull.normalized() * 440.0, delta * 2.4)
		pos += vel * delta
		pickup["pos"] = pos
		pickup["vel"] = vel

		if float(pickup["life"]) <= 0.0:
			continue

		if pos.distance_to(player_pos) < 44.0:
			charge = min(charge + float(pickup["value"]), MAX_CHARGE)
			charge_changed.emit(charge, MAX_CHARGE)
			_set_status("에너지 셀 확보", 0.8)
			_spawn_ring(pos, Color(0.94, 0.83, 0.31, 0.58), 6, 72.0)
			continue

		remaining.append(pickup)
	pickups = remaining


func _tick_combo(delta: float) -> void:
	if combo <= 0:
		return

	combo_timeout -= delta
	if combo_timeout > 0.0:
		return

	combo = max(combo - 1, 0)
	multiplier = 1 + min(4, int(combo / 4))
	combo_timeout = 0.72 if combo > 0 else 0.0
	combo_changed.emit(combo, multiplier)


func _check_collisions() -> void:
	var remaining_player_bullets := []
	for bullet in player_bullets:
		var bullet_consumed := false
		for enemy in enemies:
			if _intersects(bullet["pos"], float(bullet["radius"]), enemy["pos"], float(enemy["radius"])):
				bullet_consumed = true
				enemy["hp"] = float(enemy["hp"]) - float(bullet["damage"])
				_spawn_ring(bullet["pos"], Color(0.29, 0.93, 0.98, 0.36), 3, 32.0)
				_spawn_sparks(bullet["pos"], Color(0.86, 0.98, 1.0, 0.95), 4, 120.0)
				if float(enemy["hp"]) <= 0.0:
					enemy["dead"] = true
				break
		if not bullet_consumed:
			remaining_player_bullets.append(bullet)
	player_bullets = remaining_player_bullets

	var living_enemies := []
	for enemy in enemies:
		if bool(enemy.get("dead", false)):
			_destroy_enemy(enemy, int(enemy["score"]), false)
		else:
			living_enemies.append(enemy)
	enemies = living_enemies

	var remaining_enemy_bullets := []
	for bullet in enemy_bullets:
		if _intersects(bullet["pos"], float(bullet["radius"]), player_pos, 30.0):
			_apply_player_hit(float(bullet["damage"]), bullet["pos"])
		else:
			remaining_enemy_bullets.append(bullet)
	enemy_bullets = remaining_enemy_bullets

	var collision_survivors := []
	for enemy in enemies:
		if _intersects(enemy["pos"], float(enemy["radius"]) + 8.0, player_pos, 32.0):
			_apply_player_hit(18.0, enemy["pos"])
			_destroy_enemy(enemy, int(enemy["score"]) / 2, true)
		else:
			collision_survivors.append(enemy)
	enemies = collision_survivors


func _apply_player_hit(amount: float, impact_pos: Vector2) -> void:
	if finished or invincible_timer > 0.0:
		return

	invincible_timer = 0.6
	shield = max(shield - amount, 0.0)
	shake_strength = max(shake_strength, 18.0)
	flash_strength = max(flash_strength, 0.45)
	combo = 0
	multiplier = 1
	combo_timeout = 0.0
	shield_changed.emit(shield, MAX_SHIELD)
	combo_changed.emit(combo, multiplier)
	_set_status("피격! 회피 우선", 1.0)
	_spawn_sparks(impact_pos, Color(1.0, 0.48, 0.54, 0.95), 12, 220.0)

	if shield <= 0.0:
		_finish_run(false)


func _destroy_enemy(enemy: Dictionary, reward: int, silent_status: bool) -> void:
	var pos = enemy["pos"]
	kills += 1
	combo += 1
	combo_timeout = 1.45
	multiplier = 1 + min(4, int(combo / 4))
	var earned := reward * multiplier
	score += earned
	score_changed.emit(score)
	combo_changed.emit(combo, multiplier)
	score_popups.append({"pos": pos, "text": "+%d" % earned if multiplier <= 1 else "+%d x%d" % [earned, multiplier], "life": 0.85, "vel": Vector2(0.0, -180.0)})
	_spawn_sparks(pos, Color(1.0, 0.74, 0.29, 0.95), 16, 260.0)
	_spawn_ring(pos, Color(0.98, 0.52, 0.66, 0.42), 7, 92.0)

	if rng.randf() < 0.42:
		pickups.append(
			{
				"pos": pos,
				"vel": Vector2(rng.randf_range(-80.0, 80.0), rng.randf_range(60.0, 140.0)),
				"life": 5.5,
				"age": 0.0,
				"value": rng.randf_range(10.0, 18.0)
			}
		)

	if not silent_status and combo > 0 and combo % 5 == 0:
		_set_status("%d연쇄 돌입 x%d" % [combo, multiplier], 0.9)


func _spawn_player_bullet(origin: Vector2, velocity: Vector2) -> void:
	player_bullets.append(
		{
			"pos": origin,
			"vel": velocity,
			"life": 1.0,
			"radius": 8.0,
			"damage": 26.0
		}
	)


func _fire_enemy_shot(enemy: Dictionary) -> void:
	var pos = enemy["pos"]
	var target_dir: Vector2 = (player_pos - pos).normalized()
	var speed := float(enemy["bullet_speed"])
	var spread := rng.randf_range(-0.18, 0.18)
	var velocity: Vector2 = target_dir.rotated(spread) * speed
	if velocity.y < 160.0:
		velocity.y = 160.0

	enemy_bullets.append(
		{
			"pos": pos + Vector2(0.0, 18.0),
			"vel": velocity,
			"life": 3.0,
			"radius": 10.0,
			"damage": float(enemy["bullet_damage"])
		}
	)


func _spawn_line(wave: int) -> void:
	var count := 3 + wave
	for index in range(count):
		var ratio := float(index + 1) / float(count + 1)
		var x := lerpf(play_rect.position.x + 40.0, play_rect.end.x - 40.0, ratio)
		var pos := Vector2(x, play_rect.position.y - 100.0 - index * 24.0)
		var vel := Vector2(rng.randf_range(-22.0, 22.0), rng.randf_range(180.0, 260.0))
		_spawn_enemy("scout", pos, vel)


func _spawn_diagonal(wave: int, direction: int) -> void:
	var count := 3 + wave
	for index in range(count):
		var x := play_rect.position.x - 120.0 if direction < 0 else play_rect.end.x + 120.0
		var y := play_rect.position.y - 80.0 - index * 80.0
		var drift := rng.randf_range(90.0, 160.0) * float(-direction)
		var vel := Vector2(drift, rng.randf_range(210.0, 300.0))
		_spawn_enemy("zigzag", Vector2(x, y), vel)


func _spawn_arc(wave: int) -> void:
	var count := 4 + wave
	for index in range(count):
		var ratio: float = float(index) / max(float(count - 1), 1.0)
		var x := lerpf(play_rect.position.x + 70.0, play_rect.end.x - 70.0, ratio)
		var y_offset := sin(ratio * PI) * -110.0
		_spawn_enemy("scout", Vector2(x, play_rect.position.y - 120.0 + y_offset), Vector2(0.0, 200.0 + ratio * 30.0))


func _spawn_swoopers(wave: int) -> void:
	var count: int = 2 + min(3, wave)
	for index in range(count):
		var x := play_rect.position.x + 120.0 if index % 2 == 0 else play_rect.end.x - 120.0
		var vel := Vector2(0.0, 210.0 + wave * 18.0)
		_spawn_enemy("swooper", Vector2(x, play_rect.position.y - 120.0 - index * 80.0), vel)


func _spawn_tank(pos: Vector2) -> void:
	enemies.append(
		{
			"kind": "tank",
			"pos": pos,
			"vel": Vector2(rng.randf_range(-20.0, 20.0), 150.0),
			"radius": 44.0,
			"hp": 220.0,
			"max_hp": 220.0,
			"score": 90,
			"fire_timer": 0.7,
			"fire_interval": 0.7,
			"bullet_speed": 320.0,
			"bullet_damage": 12.0,
			"phase": rng.randf_range(0.0, TAU),
			"age": 0.0
		}
	)


func _spawn_enemy(kind: String, pos: Vector2, vel: Vector2) -> void:
	var enemy := {
		"kind": kind,
		"pos": pos,
		"vel": vel,
		"radius": 26.0,
		"hp": 48.0,
		"max_hp": 48.0,
		"score": 24,
		"fire_timer": rng.randf_range(0.6, 1.4),
		"fire_interval": rng.randf_range(1.2, 2.0),
		"bullet_speed": 260.0,
		"bullet_damage": 8.0,
		"phase": rng.randf_range(0.0, TAU),
		"age": 0.0
	}

	match kind:
		"zigzag":
			enemy["radius"] = 24.0
			enemy["hp"] = 40.0
			enemy["max_hp"] = 40.0
			enemy["score"] = 28
			enemy["bullet_speed"] = 300.0
		"swooper":
			enemy["radius"] = 28.0
			enemy["hp"] = 66.0
			enemy["max_hp"] = 66.0
			enemy["score"] = 40
			enemy["fire_interval"] = 1.0
			enemy["bullet_speed"] = 340.0

	enemies.append(enemy)


func _finish_run(cleared: bool) -> void:
	if finished:
		return

	alive = false
	finished = true
	dragging = false
	mouse_dragging = false

	if not cleared or shield <= 0.0:
		shake_strength = max(shake_strength, 32.0)
		flash_strength = max(flash_strength, 0.9)
		_spawn_sparks(player_pos, Color(1.0, 0.62, 0.22, 0.98), 36, 400.0)
		_spawn_ring(player_pos, Color(1.0, 0.45, 0.50, 0.7), 16, 240.0)
		shockwaves.append({"radius": 18.0, "life": 0.7, "pos": player_pos})

	var result := {
		"score": score,
		"kills": kills,
		"wave": _current_wave(),
		"time_survived": snapped(STAGE_DURATION - time_left, 0.1),
		"cleared": cleared and shield > 0.0,
		"shield_left": int(round(shield)),
		"overdrives_used": overdrives_used,
		"rank": _compute_rank(score, cleared and shield > 0.0)
	}

	run_finished.emit(result)


func _current_wave() -> int:
	return clampi(int(elapsed / 9.0) + 1, 1, 5)


func _compute_rank(final_score: int, cleared: bool) -> String:
	if cleared and final_score >= 5200:
		return "S"
	if cleared and final_score >= 4000:
		return "A"
	if final_score >= 2800:
		return "B"
	if final_score >= 1600:
		return "C"
	return "D"


func _emit_full_state() -> void:
	score_changed.emit(score)
	shield_changed.emit(shield, MAX_SHIELD)
	charge_changed.emit(charge, MAX_CHARGE)
	time_changed.emit(time_left)
	combo_changed.emit(combo, multiplier)
	status_changed.emit(status_text)


func _set_pointer_target(point: Vector2) -> void:
	var target := point
	if point.y < control_rect.position.y:
		target.y = lerpf(player_pos.y, point.y, 0.48)
	pointer_target = _clamp_to_playfield(target)


func _clamp_to_playfield(point: Vector2) -> Vector2:
	return Vector2(
		clampf(point.x, play_rect.position.x + 26.0, play_rect.end.x - 26.0),
		clampf(point.y, play_rect.position.y + 36.0, play_rect.end.y - 36.0)
	)


func _refresh_view() -> void:
	view_size = get_viewport_rect().size
	play_rect = Rect2(Vector2(58.0, 248.0), Vector2(max(view_size.x - 116.0, 200.0), max(view_size.y - 560.0, 760.0)))
	control_rect = Rect2(Vector2(24.0, view_size.y * 0.58), Vector2(max(view_size.x - 48.0, 200.0), max(view_size.y * 0.34, 240.0)))


func _build_backdrop() -> void:
	stars.clear()
	skyline.clear()
	if view_size.x <= 0.0 or view_size.y <= 0.0:
		return

	var local_rng := RandomNumberGenerator.new()
	local_rng.seed = 314159
	for index in range(42):
		stars.append(
			{
				"pos": Vector2(local_rng.randf_range(play_rect.position.x, play_rect.end.x), local_rng.randf_range(play_rect.position.y - 40.0, play_rect.end.y)),
				"speed": local_rng.randf_range(40.0, 140.0),
				"radius": local_rng.randf_range(1.0, 3.2),
				"alpha": local_rng.randf_range(0.18, 0.55)
			}
		)

	var cursor := play_rect.position.x - 18.0
	while cursor < play_rect.end.x + 80.0:
		var width := local_rng.randf_range(46.0, 90.0)
		var height := local_rng.randf_range(70.0, 220.0)
		skyline.append({"x": cursor, "width": width, "height": height})
		cursor += width - local_rng.randf_range(6.0, 18.0)


func _tick_backdrop(delta: float) -> void:
	for star in stars:
		var star_pos: Vector2 = star["pos"]
		star_pos.y += float(star["speed"]) * delta
		if star_pos.y > play_rect.end.y:
			star_pos.y = play_rect.position.y - 20.0
			star_pos.x = rng.randf_range(play_rect.position.x, play_rect.end.x)
		star["pos"] = star_pos

	if status_timeout > 0.0:
		status_timeout -= delta
		if status_timeout <= 0.0 and status_text != "":
			status_text = ""
			status_changed.emit(status_text)


func _tick_particles(delta: float) -> void:
	var alive_particles := []
	for particle in particles:
		particle["life"] = float(particle["life"]) - delta
		if float(particle["life"]) <= 0.0:
			continue
		var particle_velocity: Vector2 = particle["vel"]
		particle["pos"] += particle_velocity * delta
		particle["vel"] = particle_velocity.lerp(Vector2.ZERO, delta * 1.5)
		alive_particles.append(particle)
	particles = alive_particles


func _tick_shockwaves(delta: float) -> void:
	var active_shockwaves := []
	for wave in shockwaves:
		wave["life"] = float(wave["life"]) - delta
		if float(wave["life"]) <= 0.0:
			continue
		wave["radius"] = float(wave["radius"]) + 820.0 * delta
		active_shockwaves.append(wave)
	shockwaves = active_shockwaves


func _tick_score_popups(delta: float) -> void:
	var alive_popups := []
	for popup in score_popups:
		popup["life"] = float(popup["life"]) - delta
		if float(popup["life"]) <= 0.0:
			continue
		popup["pos"] += popup["vel"] * delta
		popup["vel"] = Vector2(popup["vel"].x, popup["vel"].y * 0.96)
		alive_popups.append(popup)
	score_popups = alive_popups


func _spawn_sparks(pos: Vector2, color: Color, count: int, speed: float) -> void:
	for _index in range(count):
		var direction := Vector2.RIGHT.rotated(rng.randf_range(0.0, TAU))
		particles.append(
			{
				"pos": pos,
				"vel": direction * rng.randf_range(speed * 0.25, speed),
				"life": rng.randf_range(0.25, 0.7),
				"size": rng.randf_range(3.0, 8.0),
				"color": color
			}
		)


func _spawn_ring(pos: Vector2, color: Color, count: int, speed: float) -> void:
	for index in range(count):
		var angle: float = TAU * float(index) / max(float(count), 1.0)
		particles.append(
			{
				"pos": pos,
				"vel": Vector2.RIGHT.rotated(angle) * speed,
				"life": 0.24,
				"size": 4.0,
				"color": color
			}
		)


func _set_status(text: String, duration: float) -> void:
	status_text = text
	status_timeout = duration
	status_changed.emit(status_text)


func _intersects(a_pos: Vector2, a_radius: float, b_pos: Vector2, b_radius: float) -> bool:
	var total_radius := a_radius + b_radius
	return a_pos.distance_squared_to(b_pos) <= total_radius * total_radius


func _draw() -> void:
	if view_size.x <= 0.0 or view_size.y <= 0.0:
		return

	var shake_offset := Vector2(
		rng.randf_range(-shake_strength, shake_strength),
		rng.randf_range(-shake_strength, shake_strength)
	)
	draw_set_transform(shake_offset, 0.0, Vector2.ONE)

	_draw_playfield()
	_draw_backdrop_entities()
	_draw_pickups()
	_draw_player_bullets()
	_draw_enemy_bullets()
	_draw_enemies()
	_draw_player()
	_draw_particles()
	_draw_shockwaves()
	_draw_score_popups()
	_draw_touch_zone()

	draw_set_transform(Vector2.ZERO, 0.0, Vector2.ONE)

	if flash_strength > 0.0:
		draw_rect(Rect2(Vector2.ZERO, view_size), Color(1.0, 0.97, 0.82, flash_strength * 0.12), true)


func _draw_playfield() -> void:
	draw_rect(play_rect, Color(0.02, 0.06, 0.11, 0.42), true)
	draw_rect(play_rect.grow(4.0), Color(0.19, 0.86, 0.96, 0.18), false, 4.0)

	var horizon_y := play_rect.position.y + play_rect.size.y * 0.18
	draw_line(Vector2(play_rect.position.x, horizon_y), Vector2(play_rect.end.x, horizon_y), Color(0.16, 0.76, 0.96, 0.2), 2.0)

	for index in range(9):
		var y := horizon_y + pow(float(index) / 9.0, 1.8) * (play_rect.end.y - horizon_y)
		draw_line(Vector2(play_rect.position.x, y), Vector2(play_rect.end.x, y), Color(0.16, 0.56, 0.84, 0.12), 2.0)

	for index in range(-4, 5):
		var t := float(index) / 4.0
		var top := Vector2(play_rect.get_center().x + t * 60.0, horizon_y)
		var bottom := Vector2(play_rect.get_center().x + t * play_rect.size.x * 0.55, play_rect.end.y)
		draw_line(top, bottom, Color(0.14, 0.48, 0.76, 0.12), 2.0)

	for building in skyline:
		var rect := Rect2(
			Vector2(float(building["x"]), play_rect.end.y - float(building["height"])),
			Vector2(float(building["width"]), float(building["height"]))
		)
		draw_rect(rect, Color(0.03, 0.05, 0.10, 0.85), true)


func _draw_backdrop_entities() -> void:
	for star in stars:
		draw_circle(star["pos"], float(star["radius"]), Color(0.78, 0.95, 1.0, float(star["alpha"])))


func _draw_pickups() -> void:
	for pickup in pickups:
		var pos = pickup["pos"]
		var age := float(pickup["age"])
		var radius := 16.0 + sin(age * 6.0) * 3.0
		var diamond := PackedVector2Array(
			[
				pos + Vector2(0.0, -radius),
				pos + Vector2(radius * 0.7, 0.0),
				pos + Vector2(0.0, radius),
				pos + Vector2(-radius * 0.7, 0.0)
			]
		)
		draw_colored_polygon(diamond, Color(0.99, 0.81, 0.30, 0.95))
		draw_polyline(diamond, Color(1.0, 0.95, 0.74, 0.9), 2.0, true)


func _draw_player_bullets() -> void:
	for bullet in player_bullets:
		var pos = bullet["pos"]
		draw_line(pos + Vector2(0.0, 18.0), pos + Vector2(0.0, -24.0), Color(0.31, 0.95, 1.0, 0.92), 6.0)
		draw_circle(pos, 4.0, Color(0.87, 0.98, 1.0, 1.0))


func _draw_enemy_bullets() -> void:
	for bullet in enemy_bullets:
		var pos = bullet["pos"]
		draw_circle(pos, float(bullet["radius"]), Color(1.0, 0.49, 0.54, 0.82))
		draw_circle(pos, max(float(bullet["radius"]) - 4.0, 3.0), Color(1.0, 0.78, 0.82, 0.72))


func _draw_enemies() -> void:
	for enemy in enemies:
		var pos = enemy["pos"]
		match String(enemy["kind"]):
			"tank":
				var body := PackedVector2Array(
					[
						pos + Vector2(0.0, -44.0),
						pos + Vector2(34.0, -8.0),
						pos + Vector2(28.0, 34.0),
						pos + Vector2(0.0, 48.0),
						pos + Vector2(-28.0, 34.0),
						pos + Vector2(-34.0, -8.0)
					]
				)
				draw_colored_polygon(body, Color(0.98, 0.49, 0.60, 0.96))
				draw_polyline(body, Color(1.0, 0.83, 0.88, 0.95), 3.0, true)
			"swooper":
				var wings := PackedVector2Array(
					[
						pos + Vector2(0.0, -32.0),
						pos + Vector2(42.0, 10.0),
						pos + Vector2(14.0, 28.0),
						pos + Vector2(0.0, 14.0),
						pos + Vector2(-14.0, 28.0),
						pos + Vector2(-42.0, 10.0)
					]
				)
				draw_colored_polygon(wings, Color(0.99, 0.72, 0.31, 0.96))
				draw_polyline(wings, Color(1.0, 0.92, 0.74, 0.95), 3.0, true)
			_:
				var scout := PackedVector2Array(
					[
						pos + Vector2(0.0, -26.0),
						pos + Vector2(28.0, 2.0),
						pos + Vector2(0.0, 22.0),
						pos + Vector2(-28.0, 2.0)
					]
				)
				draw_colored_polygon(scout, Color(0.96, 0.42, 0.56, 0.92))
				draw_polyline(scout, Color(1.0, 0.88, 0.92, 0.92), 2.0, true)

		var hp_ratio := clampf(float(enemy["hp"]) / max(float(enemy.get("max_hp", enemy["hp"])), 1.0), 0.0, 1.0)
		var bar_rect := Rect2(pos + Vector2(-24.0, -46.0), Vector2(48.0, 6.0))
		draw_rect(bar_rect, Color(0.07, 0.09, 0.14, 0.8), true)
		draw_rect(Rect2(bar_rect.position, Vector2(bar_rect.size.x * hp_ratio, bar_rect.size.y)), Color(0.26, 0.94, 0.98, 0.9), true)


func _draw_player() -> void:
	if not alive:
		return
	if invincible_timer > 0.0 and int(elapsed * 14.0) % 2 == 0:
		return
	var engine_glow := 22.0 + sin(elapsed * 18.0) * 4.0
	draw_circle(player_pos + Vector2(0.0, 24.0), engine_glow, Color(0.25, 0.92, 0.98, 0.16))
	draw_circle(player_pos + Vector2(-18.0, 24.0), engine_glow * 0.55, Color(0.99, 0.63, 0.28, 0.12))
	draw_circle(player_pos + Vector2(18.0, 24.0), engine_glow * 0.55, Color(0.99, 0.63, 0.28, 0.12))

	var body := PackedVector2Array(
		[
			player_pos + Vector2(0.0, -42.0),
			player_pos + Vector2(28.0, -6.0),
			player_pos + Vector2(34.0, 24.0),
			player_pos + Vector2(0.0, 30.0),
			player_pos + Vector2(-34.0, 24.0),
			player_pos + Vector2(-28.0, -6.0)
		]
	)
	var canopy := PackedVector2Array(
		[
			player_pos + Vector2(0.0, -24.0),
			player_pos + Vector2(12.0, 0.0),
			player_pos + Vector2(0.0, 10.0),
			player_pos + Vector2(-12.0, 0.0)
		]
	)
	draw_colored_polygon(body, Color(0.19, 0.88, 0.98, 0.96))
	draw_polyline(body, Color(0.91, 0.98, 1.0, 0.96), 3.0, true)
	draw_colored_polygon(canopy, Color(0.99, 0.76, 0.33, 0.92))


func _draw_particles() -> void:
	for particle in particles:
		var color: Color = particle["color"]
		var alpha := clampf(float(particle["life"]) * 1.8, 0.0, 1.0)
		draw_circle(particle["pos"], float(particle["size"]), Color(color.r, color.g, color.b, color.a * alpha))


func _draw_shockwaves() -> void:
	for wave in shockwaves:
		var alpha := clampf(float(wave["life"]) * 1.4, 0.0, 1.0)
		draw_arc(wave["pos"], float(wave["radius"]), 0.0, TAU, 64, Color(1.0, 0.92, 0.76, alpha * 0.8), 6.0, true)


func _draw_score_popups() -> void:
	var font := ThemeDB.fallback_font
	var font_size := 28
	for popup in score_popups:
		var alpha := clampf(float(popup["life"]) * 2.0, 0.0, 1.0)
		var color := Color(1.0, 0.92, 0.52, alpha)
		var text: String = popup["text"]
		var pos: Vector2 = popup["pos"]
		draw_string(font, pos, text, HORIZONTAL_ALIGNMENT_CENTER, -1, font_size, color)


func _draw_touch_zone() -> void:
	var color := Color(0.17, 0.88, 0.96, 0.05)
	draw_rect(control_rect, color, true)
	draw_rect(control_rect.grow(2.0), Color(0.17, 0.88, 0.96, 0.12), false, 2.0)
