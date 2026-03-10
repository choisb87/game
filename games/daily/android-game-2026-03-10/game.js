const canvas = document.getElementById('game');
const ctx = canvas.getContext('2d');
const scoreEl = document.getElementById('score');
const livesEl = document.getElementById('lives');
const overlay = document.getElementById('overlay');
const overlayTitle = document.getElementById('overlayTitle');
const overlayMsg = document.getElementById('overlayMsg');
const startBtn = document.getElementById('startBtn');

const COLORS = ['#e94560','#0f3460','#16c79a','#f5a623','#8b5cf6','#ec4899','#06b6d4'];
let W, H, dpr;
let bubbles = [];
let particles = [];
let score, lives, spawnTimer, spawnInterval, running, combo, lastPopTime;

function resize() {
  dpr = window.devicePixelRatio || 1;
  W = window.innerWidth;
  H = window.innerHeight;
  canvas.width = W * dpr;
  canvas.height = H * dpr;
  canvas.style.width = W + 'px';
  canvas.style.height = H + 'px';
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
}

function init() {
  score = 0; lives = 3; combo = 0; lastPopTime = 0;
  spawnInterval = 800;
  spawnTimer = 0;
  bubbles = []; particles = [];
  updateUI();
}

function updateUI() {
  scoreEl.textContent = 'Score: ' + score;
  livesEl.textContent = '\u2764'.repeat(lives);
}

function spawnBubble() {
  const r = 20 + Math.random() * 25;
  bubbles.push({
    x: r + Math.random() * (W - r * 2),
    y: H + r,
    r: r,
    speed: 1 + Math.random() * 1.5 + score * 0.005,
    color: COLORS[Math.floor(Math.random() * COLORS.length)],
    wobble: Math.random() * Math.PI * 2,
    wobbleSpeed: 0.02 + Math.random() * 0.02
  });
}

function spawnParticles(x, y, color) {
  for (let i = 0; i < 8; i++) {
    const angle = (Math.PI * 2 / 8) * i;
    particles.push({
      x, y,
      vx: Math.cos(angle) * (2 + Math.random() * 3),
      vy: Math.sin(angle) * (2 + Math.random() * 3),
      r: 3 + Math.random() * 3,
      color,
      life: 1
    });
  }
}

function popBubble(index) {
  const b = bubbles[index];
  const now = Date.now();
  combo = (now - lastPopTime < 500) ? combo + 1 : 0;
  lastPopTime = now;
  const points = 10 + combo * 5;
  score += points;
  spawnParticles(b.x, b.y, b.color);
  bubbles.splice(index, 1);
  spawnInterval = Math.max(300, 800 - score * 2);
  updateUI();
}

function handleTap(px, py) {
  if (!running) return;
  for (let i = bubbles.length - 1; i >= 0; i--) {
    const b = bubbles[i];
    const dx = px - b.x, dy = py - b.y;
    if (dx * dx + dy * dy <= b.r * b.r) {
      popBubble(i);
      return;
    }
  }
}

canvas.addEventListener('pointerdown', e => {
  e.preventDefault();
  handleTap(e.clientX, e.clientY);
});

canvas.addEventListener('contextmenu', e => e.preventDefault());

function update(dt) {
  const factor = dt / 16.667; // normalize to 60fps baseline

  spawnTimer += dt;
  if (spawnTimer >= spawnInterval) {
    spawnTimer = 0;
    spawnBubble();
  }

  for (let i = bubbles.length - 1; i >= 0; i--) {
    const b = bubbles[i];
    b.y -= b.speed * factor;
    b.wobble += b.wobbleSpeed * factor;
    b.x += Math.sin(b.wobble) * 0.5 * factor;
    if (b.y + b.r < 0) {
      bubbles.splice(i, 1);
      lives--;
      updateUI();
      if (lives <= 0) { gameOver(); return; }
    }
  }

  for (let i = particles.length - 1; i >= 0; i--) {
    const p = particles[i];
    p.x += p.vx * factor;
    p.y += p.vy * factor;
    p.life -= 0.03 * factor;
    if (p.life <= 0) particles.splice(i, 1);
  }
}

function draw() {
  ctx.clearRect(0, 0, W, H);

  for (const b of bubbles) {
    ctx.beginPath();
    ctx.arc(b.x, b.y, b.r, 0, Math.PI * 2);
    ctx.fillStyle = b.color;
    ctx.globalAlpha = 0.85;
    ctx.fill();
    ctx.globalAlpha = 0.3;
    ctx.beginPath();
    ctx.arc(b.x - b.r * 0.3, b.y - b.r * 0.3, b.r * 0.25, 0, Math.PI * 2);
    ctx.fillStyle = '#fff';
    ctx.fill();
    ctx.globalAlpha = 1;
  }

  for (const p of particles) {
    ctx.globalAlpha = p.life;
    ctx.beginPath();
    ctx.arc(p.x, p.y, p.r * p.life, 0, Math.PI * 2);
    ctx.fillStyle = p.color;
    ctx.fill();
  }
  ctx.globalAlpha = 1;

  if (combo > 0 && Date.now() - lastPopTime < 800) {
    ctx.fillStyle = '#f5a623';
    ctx.font = 'bold 24px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('Combo x' + (combo + 1) + '!', W / 2, H / 2 - 40);
  }
}

let lastTime = 0;
function loop(time) {
  if (!running) return;
  const dt = lastTime ? time - lastTime : 16;
  lastTime = time;
  update(dt);
  draw();
  requestAnimationFrame(loop);
}

function startGame() {
  overlay.classList.add('hidden');
  init();
  running = true;
  lastTime = 0;
  requestAnimationFrame(loop);
}

function gameOver() {
  running = false;
  overlayTitle.textContent = 'Game Over';
  overlayMsg.textContent = 'Score: ' + score;
  startBtn.textContent = 'Play Again';
  overlay.classList.remove('hidden');
}

startBtn.addEventListener('click', startGame);
window.addEventListener('resize', resize);
resize();
