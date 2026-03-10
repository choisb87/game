# Gravity Pulse — Game Design Plan

## Core Concept

One-touch gravity-flipping arcade game. The player controls an orb that falls
under gravity; tapping the screen reverses gravity direction. Neon barriers
scroll upward with gaps the player must thread through.

The design philosophy: **simple to learn, impossible to master**.

## The Fun Loop (First 30 Seconds)

1. **Instant engagement** (0-3s): No loading screens, no tutorials. Tap the
   screen, gravity flips, the orb moves. The player understands the mechanic
   within their first tap.

2. **First success** (3-10s): Early barriers have wide gaps. The player
   successfully passes 2-3 barriers and sees the score increment with particle
   bursts and combo text. Dopamine hit from immediate positive feedback.

3. **First challenge** (10-20s): Gaps narrow, speed increases. The player
   must time their gravity flips more precisely. The tension between "when
   do I flip?" creates the core decision space.

4. **First death** (15-30s): The orb shatters with screen shake and particles.
   Score is shown. The player taps to retry instantly. The loop restarts.

The 30-second hook works because:
- Zero friction from launch to gameplay
- The gravity-flip mechanic is novel yet instantly intuitive
- Visual feedback (particles, combos, neon glow) makes every action feel impactful
- Death is quick and retry is instant — no punishment screens

## Retention Mechanics

### Short-term (session-to-session)
- **High score chase**: SharedPreferences-persisted best score displayed on menu
- **Combo system**: Consecutive passes build multiplier — creates "one more try" to beat combo record
- **Progressive difficulty**: Speed and gap narrowing create a skill curve that rewards practice
- **Visual escalation**: As speed increases, the neon visuals intensify, creating a flow state

### Medium-term (week-over-week)
- **Skill ceiling**: Physics-based movement means there's always room to improve timing
- **Muscle memory development**: The gravity-flip timing becomes instinctive over sessions
- **Score milestones**: Natural plateaus at ~20, ~50, ~100 that feel like achievements when broken

### Long-term (month-over-month)
- **Zen play**: Once skilled, the game becomes meditative — a quick flow-state break
- **Sharing moments**: Exceptional scores or close calls create shareable moments

## Paid App Rationale

### Why Premium ($2.99-$3.99)
1. **No ads breaking flow**: The game is a flow-state experience. Interstitial ads
   would destroy the core appeal. Banner ads would compromise the full-screen
   neon aesthetic.

2. **Complete experience**: Everything unlocked from the start. No paywalls,
   no currencies, no energy systems. The player gets the full game.

3. **Trust signal**: A paid price says "this is worth your time." Free games
   with ads train users to expect interruption. Premium says quality.

4. **Sustainable without manipulation**: No need to design frustration loops
   to drive ad views or IAP. The game can be purely fun-optimized.

### Premium Positioning
- Clean, ad-free experience
- No internet required (fully offline)
- Small install size (<10 MB)
- Battery efficient (Compose Canvas, no heavy game engine)
- Respects user attention — no notifications, no "come back" nags

## Technical Design Decisions

### Why Jetpack Compose (not a game engine)?
- **Lightweight**: No Unity/Godot overhead for a 2D arcade game
- **Native performance**: Direct Canvas drawing at 60fps
- **Small APK**: Minimal dependencies
- **Modern Android**: Compose is the standard UI toolkit; no legacy View system

### State Architecture
- Immutable `GameState` data class — all game state in one place
- Pure function updates: `GameState.update(dt)` returns new state
- Compose recomposition drives rendering — no manual invalidation
- `LaunchedEffect` + `withFrameNanos` for frame-synced game loop

### Physics
- Constant gravity with direction flip on tap
- Velocity clamping prevents runaway speed
- Wall bouncing with dampening keeps the orb in bounds
- Slight horizontal sine drift adds visual interest without affecting gameplay

## Future Considerations (Post-Launch)

- Color themes / visual skins (unlocked by score milestones, not IAP)
- Daily challenge mode (fixed seed, leaderboard)
- Haptic feedback on tap, collision, and score events
- Accessibility: adjustable game speed, high-contrast mode
