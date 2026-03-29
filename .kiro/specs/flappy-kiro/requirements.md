# Requirements Document

## Introduction

Flappy Kiro is a retro-style, browser-based endless scroller game inspired by Flappy Bird. The player controls a ghost character (Ghosty) through an infinite series of pipe obstacles. The game runs entirely in the browser using vanilla HTML, CSS, and JavaScript with no external frameworks. Visual style is light blue with a sketchy/hand-drawn aesthetic, green pipes, and floating cloud-like shapes. Sound effects play on jump and game over events.

## Glossary

- **Game**: The Flappy Kiro browser application as a whole
- **Ghosty**: The ghost character sprite controlled by the player, rendered using `assets/ghosty.png`
- **Pipe_Pair**: A pair of green rectangular obstacles — one extending down from the top of the canvas and one extending up from the bottom — with a gap between them
- **Gap**: The vertical opening between the top and bottom pipe of a Pipe_Pair through which Ghosty must pass
- **Score**: The count of Pipe_Pairs Ghosty has successfully passed through in the current session
- **High_Score**: The highest Score achieved across all sessions, persisted in browser local storage
- **Game_Loop**: The continuous update-and-render cycle that drives gameplay
- **Gravity**: The constant downward acceleration applied to Ghosty each frame
- **Flap**: The upward velocity impulse applied to Ghosty when the player triggers an input
- **Game_Over_State**: The state entered when Ghosty collides with a pipe or a canvas boundary
- **Start_State**: The idle state before the first input, showing the game ready to play
- **Playing_State**: The active gameplay state after the first input
- **Score_Bar**: The UI strip at the bottom of the canvas displaying the current Score and High_Score
- **Canvas**: The HTML `<canvas>` element on which the game is rendered

---

## Requirements

### Requirement 1: Game Initialization and Rendering Surface

**User Story:** As a player, I want the game to load instantly in my browser with no install steps, so that I can start playing immediately.

#### Acceptance Criteria

1. THE Game SHALL render entirely within a single HTML `<canvas>` element using the 2D rendering context.
2. THE Game SHALL require no external libraries, build tools, or server-side components — only a browser and the local files.
3. WHEN the page loads, THE Game SHALL display the Start_State with Ghosty centered horizontally and vertically on the Canvas.
4. THE Canvas SHALL have a fixed logical resolution of 480×640 pixels and scale to fit the browser viewport while preserving aspect ratio.

---

### Requirement 2: Visual Style

**User Story:** As a player, I want a charming retro aesthetic, so that the game feels distinct and fun to look at.

#### Acceptance Criteria

1. THE Game SHALL render a light blue background with a sketchy, hand-drawn texture effect (e.g., irregular lines or noise patterns drawn on the canvas).
2. THE Game SHALL render semi-transparent, floating cloud-like shapes in the background that scroll from right to left at multiple independent speeds, so that faster clouds appear closer and slower clouds appear farther away (parallax depth effect).
3. THE Game SHALL render Pipe_Pairs as solid green rectangles with a slightly darker green border/cap on the open end.
4. THE Score_Bar SHALL be rendered as a distinct strip at the bottom of the Canvas, visually separated from the play area.
5. THE Game SHALL render Ghosty using the sprite at `assets/ghosty.png`, scaled to a consistent size throughout gameplay.

---

### Requirement 3: Player Input

**User Story:** As a player, I want to control Ghosty using keyboard or mouse/touch, so that the game is accessible on both desktop and mobile.

#### Acceptance Criteria

1. WHEN the player presses the Spacebar, THE Game SHALL trigger a Flap on Ghosty.
2. WHEN the player clicks or taps the Canvas, THE Game SHALL trigger a Flap on Ghosty.
3. WHEN a Flap is triggered during Start_State, THE Game SHALL transition to Playing_State and begin the Game_Loop.
4. WHEN a Flap is triggered during Game_Over_State, THE Game SHALL restart the game and transition to Playing_State.
5. WHILE in Playing_State, THE Game SHALL apply a single Flap impulse per discrete input event (not continuous while held).

---

### Requirement 4: Physics and Movement

**User Story:** As a player, I want Ghosty to respond to gravity and my inputs with smooth, predictable motion, so that the game feels fair and satisfying.

#### Acceptance Criteria

1. WHILE in Playing_State, THE Game_Loop SHALL apply a constant downward Gravity acceleration to Ghosty each frame.
2. WHEN a Flap is triggered, THE Game SHALL set Ghosty's vertical velocity to a fixed upward value, overriding any current downward velocity.
3. WHILE in Playing_State, THE Game_Loop SHALL update Ghosty's vertical position each frame based on Ghosty's current velocity.
4. THE Game SHALL cap Ghosty's maximum downward velocity to prevent unrealistically fast falling.
5. WHILE in Playing_State, THE Game_Loop SHALL scroll all Pipe_Pairs and background elements from right to left at a constant speed.

---

### Requirement 4b: Physics System Constants and Interpolation

**User Story:** As a player, I want Ghosty's movement to feel physically consistent and smooth, so that the game is predictable and satisfying to control.

#### Acceptance Criteria

1. THE Game SHALL define a named gravity constant (e.g., `GRAVITY = 0.5` pixels/frame²) applied as a fixed downward acceleration to Ghosty's vertical velocity each frame while in Playing_State.
2. WHEN a Flap is triggered, THE Game SHALL set Ghosty's vertical velocity to a fixed named ascent constant (e.g., `FLAP_VELOCITY = -9` pixels/frame), overriding any current velocity regardless of direction.
3. THE Game SHALL define a named terminal velocity constant (e.g., `TERMINAL_VELOCITY = 12` pixels/frame downward) and clamp Ghosty's downward velocity to this limit each frame, preventing unrealistically fast falling.
4. THE Game SHALL preserve momentum between frames — Ghosty's velocity SHALL carry over from the previous frame and only be modified by gravity acceleration or a Flap impulse, never reset arbitrarily.
5. THE Game SHALL compute Ghosty's rendered position using linear interpolation between the previous frame's position and the current frame's position (e.g., `renderY = prevY + (currY - prevY) * alpha`), where `alpha` is the fractional progress within the current frame, so that Ghosty's motion appears smooth even at variable frame rates.
6. ALL physics constants (gravity, flap velocity, terminal velocity) SHALL be defined as named constants at the top of the source file, not as inline magic numbers.

---

### Requirement 5: Pipe Obstacle Generation

**User Story:** As a player, I want a continuous stream of pipe obstacles at varied heights, so that the game remains challenging and unpredictable.

#### Acceptance Criteria

1. WHILE in Playing_State, THE Game SHALL spawn a new Pipe_Pair at a fixed horizontal interval (measured in pixels scrolled).
2. THE Game SHALL randomize the vertical center position of each Pipe_Pair's Gap within a range that keeps the Gap fully within the play area.
3. THE Gap SHALL have a fixed height sufficient to allow Ghosty to pass through with skill.
4. WHEN a Pipe_Pair scrolls fully off the left edge of the Canvas, THE Game SHALL remove it from the active obstacle list.

---

### Requirement 6: Collision Detection

**User Story:** As a player, I want the game to accurately detect when I've hit a pipe or boundary, so that the outcome feels fair.

#### Acceptance Criteria

1. WHEN Ghosty's bounding box overlaps with any Pipe_Pair's bounding box, THE Game SHALL transition to Game_Over_State.
2. WHEN Ghosty's vertical position moves above the top boundary of the play area, THE Game SHALL transition to Game_Over_State.
3. WHEN Ghosty's vertical position moves below the top of the Score_Bar, THE Game SHALL transition to Game_Over_State.
4. THE Game SHALL use axis-aligned bounding box (AABB) collision detection with a small inset margin to avoid pixel-perfect unfairness.

---

### Requirement 7: Scoring

**User Story:** As a player, I want to see my score increase as I pass pipes, so that I have a clear sense of progress.

#### Acceptance Criteria

1. WHEN Ghosty's horizontal center passes the horizontal center of a Pipe_Pair for the first time, THE Game SHALL increment the Score by 1.
2. THE Score_Bar SHALL display the current Score and High_Score in the format `Score: N | High: N` at all times during Playing_State and Game_Over_State.
3. WHEN the Game transitions to Game_Over_State, IF the current Score exceeds the High_Score, THEN THE Game SHALL update the High_Score to the current Score.
4. THE Game SHALL persist the High_Score using the browser's `localStorage` API so that it survives page reloads.
5. WHEN the page loads, THE Game SHALL read the High_Score from `localStorage` and initialize the displayed High_Score accordingly.

---

### Requirement 8: Sound Effects

**User Story:** As a player, I want audio feedback on key events, so that the game feels responsive and alive.

#### Acceptance Criteria

1. WHEN a Flap is triggered during Playing_State, THE Game SHALL play the audio file at `assets/jump.wav`.
2. WHEN the Game transitions to Game_Over_State, THE Game SHALL play the audio file at `assets/game_over.wav`.
3. IF the browser has not yet received a user gesture, THEN THE Game SHALL defer audio playback until after the first user interaction (to comply with browser autoplay policies).

---

### Requirement 9: Game Over Screen

**User Story:** As a player, I want a clear game over screen with my score, so that I know the round ended and can try again.

#### Acceptance Criteria

1. WHEN the Game transitions to Game_Over_State, THE Game SHALL overlay a "Game Over" message on the Canvas.
2. THE Game_Over_State overlay SHALL display the final Score and the High_Score.
3. THE Game_Over_State overlay SHALL display a prompt instructing the player to tap or press Space to restart.
4. WHILE in Game_Over_State, THE Game SHALL keep the final frame (Ghosty and pipes at their last positions) visible beneath the overlay.

---

### Requirement 10: Difficulty Progression

**User Story:** As a player, I want the game to get harder over time, so that there is a long-term challenge.

#### Acceptance Criteria

1. WHILE in Playing_State, THE Game SHALL gradually increase the scroll speed of Pipe_Pairs as the Score increases.
2. THE Game SHALL cap the maximum scroll speed to a fixed upper limit to keep the game playable.
3. THE Game SHALL NOT decrease the Gap height as difficulty increases, so that the challenge comes from speed rather than precision alone.
