/**
 * LKBoard Flagship Experience:
 * - Scroll-driven 3D window rotation & parallax lighting
 * - Live interactive inking canvas right on the website
 * - Real-time Desmos math curve evaluator & SVG plotter
 * - Interactive sticky note todo state
 */

document.addEventListener('DOMContentLoaded', () => {

  // =========================================================================
  // 1. Scroll-Driven 3D Perspective Stage
  // =========================================================================
  const canvasWindow = document.getElementById('hero-canvas-window');
  const orb1 = document.querySelector('.orb-1');
  const orb2 = document.querySelector('.orb-2');
  const orb3 = document.querySelector('.orb-3');

  function handleScroll() {
    const scrollY = window.scrollY || window.pageYOffset;
    const heroHeight = window.innerHeight;
    const progress = Math.min(Math.max(scrollY / (heroHeight * 0.7), 0), 1);

    if (canvasWindow && window.innerWidth > 768) {
      // 14deg tilt at top -> 0deg perfectly flat when scrolled down
      const rotX = (1 - progress) * 12;
      const scale = 0.94 + progress * 0.06;
      canvasWindow.style.transform = `rotateX(${rotX}deg) scale(${scale})`;

      if (progress > 0.4) {
        canvasWindow.classList.add('scrolled');
      } else {
        canvasWindow.classList.remove('scrolled');
      }
    }

    // Parallax background orbs
    if (orb1) orb1.style.transform = `translateX(-50%) translateY(${scrollY * 0.15}px)`;
    if (orb2) orb2.style.transform = `translateY(${scrollY * -0.1}px)`;
    if (orb3) orb3.style.transform = `translateY(${scrollY * -0.12}px)`;
  }

  window.addEventListener('scroll', handleScroll, { passive: true });
  handleScroll();

  // Mouse move glow on the window
  if (canvasWindow) {
    canvasWindow.addEventListener('mousemove', (e) => {
      const rect = canvasWindow.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;
      canvasWindow.style.setProperty('--mouse-x', `${x}px`);
      canvasWindow.style.setProperty('--mouse-y', `${y}px`);
    });
  }

  // =========================================================================
  // 2. Interactive Live Inking Canvas (Try stylus right on the site)
  // =========================================================================
  const paintCanvas = document.getElementById('live-paint-canvas');
  if (paintCanvas) {
    const ctx = paintCanvas.getContext('2d');
    let isDrawing = false;
    let currentColor = '#00f2fe';
    let currentWidth = 3;
    let isEraser = false;

    function resizeCanvas() {
      const rect = paintCanvas.getBoundingClientRect();
      if (rect.width > 0 && rect.height > 0) {
        const dpr = window.devicePixelRatio || 1;
        paintCanvas.width = rect.width * dpr;
        paintCanvas.height = rect.height * dpr;
        ctx.scale(dpr, dpr);
        ctx.lineCap = 'round';
        ctx.lineJoin = 'round';

        // Initial welcome stroke
        drawSampleStroke();
      }
    }

    function drawSampleStroke() {
      ctx.clearRect(0, 0, paintCanvas.width, paintCanvas.height);
      ctx.strokeStyle = '#00f2fe';
      ctx.lineWidth = 3;
      ctx.beginPath();
      const w = paintCanvas.getBoundingClientRect().width;
      const h = paintCanvas.getBoundingClientRect().height;
      ctx.moveTo(w * 0.15, h * 0.55);
      ctx.bezierCurveTo(w * 0.35, h * 0.15, w * 0.65, h * 0.85, w * 0.85, h * 0.45);
      ctx.stroke();

      ctx.fillStyle = 'rgba(255, 255, 255, 0.4)';
      ctx.font = '12px var(--font-sans)';
      ctx.fillText('Попробуйте нарисовать что-нибудь стилусом или мышью...', w * 0.15, h * 0.85);
    }

    window.addEventListener('resize', resizeCanvas);
    setTimeout(resizeCanvas, 50);

    function getPos(e) {
      const rect = paintCanvas.getBoundingClientRect();
      const clientX = e.touches ? e.touches[0].clientX : e.clientX;
      const clientY = e.touches ? e.touches[0].clientY : e.clientY;
      return {
        x: clientX - rect.left,
        y: clientY - rect.top
      };
    }

    function startDraw(e) {
      isDrawing = true;
      const pos = getPos(e);
      ctx.beginPath();
      ctx.moveTo(pos.x, pos.y);
      if (e.touches) e.preventDefault();
    }

    function draw(e) {
      if (!isDrawing) return;
      const pos = getPos(e);
      ctx.strokeStyle = isEraser ? '#030611' : currentColor;
      ctx.lineWidth = isEraser ? 20 : currentWidth;
      ctx.lineTo(pos.x, pos.y);
      ctx.stroke();
      if (e.touches) e.preventDefault();
    }

    function stopDraw() {
      isDrawing = false;
    }

    paintCanvas.addEventListener('mousedown', startDraw);
    paintCanvas.addEventListener('mousemove', draw);
    window.addEventListener('mouseup', stopDraw);

    paintCanvas.addEventListener('touchstart', startDraw, { passive: false });
    paintCanvas.addEventListener('touchmove', draw, { passive: false });
    window.addEventListener('touchend', stopDraw);

    // Color pickers
    document.querySelectorAll('.color-dot').forEach(dot => {
      dot.addEventListener('click', () => {
        document.querySelectorAll('.color-dot').forEach(d => d.classList.remove('active'));
        dot.classList.add('active');
        if (dot.getAttribute('data-action') === 'eraser') {
          isEraser = true;
        } else {
          isEraser = false;
          currentColor = dot.getAttribute('data-color');
        }
      });
    });

    // Clear button
    const clearBtn = document.getElementById('btn-clear-canvas');
    if (clearBtn) {
      clearBtn.addEventListener('click', () => {
        const rect = paintCanvas.getBoundingClientRect();
        ctx.clearRect(0, 0, rect.width, rect.height);
      });
    }
  }

  // =========================================================================
  // 3. Real-time Desmos Math Evaluator & SVG Plotter
  // =========================================================================
  const calcInput = document.getElementById('live-calc-input');
  const liveSvgPath = document.getElementById('live-graph-path');
  const tracerPoint = document.getElementById('live-graph-tracer');
  const tracerLabel = document.getElementById('live-graph-label');

  function evaluateFormula(expr, x) {
    try {
      let sanitized = expr.toLowerCase()
        .replace(/\^/g, '**')
        .replace(/sin/g, 'Math.sin')
        .replace(/cos/g, 'Math.cos')
        .replace(/tan/g, 'Math.tan')
        .replace(/abs/g, 'Math.abs')
        .replace(/sqrt/g, 'Math.sqrt')
        .replace(/exp/g, 'Math.exp')
        .replace(/pi/g, 'Math.PI')
        .replace(/e/g, 'Math.E');

      // simple implicit multiplication e.g. 2x -> 2*x
      sanitized = sanitized.replace(/(\d)x/g, '$1*x');
      sanitized = sanitized.replace(/(\d)\(/g, '$1*(');

      // Safe evaluation with Function
      const f = new Function('x', `return ${sanitized};`);
      const y = f(x);
      return isFinite(y) ? y : null;
    } catch (_) {
      return null;
    }
  }

  function plotFormula(formula) {
    if (!liveSvgPath) return;

    const width = 400;
    const height = 200;
    const xMin = -8;
    const xMax = 8;
    const yMin = -4;
    const yMax = 4;

    const points = [];
    const steps = 160;
    const dx = (xMax - xMin) / steps;

    for (let i = 0; i <= steps; i++) {
      const x = xMin + i * dx;
      const y = evaluateFormula(formula, x);
      if (y !== null) {
        const svgX = ((x - xMin) / (xMax - xMin)) * width;
        const svgY = height - ((y - yMin) / (yMax - yMin)) * height;
        if (svgY >= -20 && svgY <= height + 20) {
          points.push({ x: svgX, y: svgY, rawX: x, rawY: y });
        }
      }
    }

    if (points.length < 2) return;

    let pathD = `M ${points[0].x.toFixed(1)} ${points[0].y.toFixed(1)}`;
    for (let i = 1; i < points.length; i++) {
      pathD += ` L ${points[i].x.toFixed(1)} ${points[i].y.toFixed(1)}`;
    }

    liveSvgPath.setAttribute('d', pathD);

    // Update animated tracer to midpoint
    const mid = points[Math.floor(points.length / 2)];
    if (tracerPoint && mid) {
      tracerPoint.setAttribute('cx', mid.x);
      tracerPoint.setAttribute('cy', mid.y);
    }
    if (tracerLabel && mid) {
      tracerLabel.textContent = `(${mid.rawX.toFixed(1)}, ${mid.rawY.toFixed(2)})`;
      tracerLabel.setAttribute('x', Math.min(mid.x + 8, width - 60));
      tracerLabel.setAttribute('y', Math.max(mid.y - 8, 20));
    }
  }

  if (calcInput) {
    calcInput.addEventListener('input', (e) => {
      plotFormula(e.target.value);
    });

    // Preset chips
    document.querySelectorAll('.preset-chip').forEach(chip => {
      chip.addEventListener('click', () => {
        const f = chip.getAttribute('data-formula');
        calcInput.value = f;
        plotFormula(f);
      });
    });

    // Initial plot
    plotFormula(calcInput.value || 'sin(x) * 2');
  }

  // =========================================================================
  // 4. Interactive Sticky Note Checkboxes
  // =========================================================================
  document.querySelectorAll('.sim-todo-item').forEach(item => {
    const cb = item.querySelector('input');
    if (cb) {
      cb.addEventListener('change', () => {
        if (cb.checked) {
          item.classList.add('checked');
        } else {
          item.classList.remove('checked');
        }
      });
      item.addEventListener('click', (e) => {
        if (e.target !== cb) {
          cb.checked = !cb.checked;
          cb.dispatchEvent(new Event('change'));
        }
      });
    }
  });

  // =========================================================================
  // 5. Live Pomodoro Countdown in Preview
  // =========================================================================
  const timerDigits = document.getElementById('preview-timer-digits');
  if (timerDigits) {
    let seconds = 24 * 60 + 59;
    setInterval(() => {
      if (seconds > 0) {
        seconds--;
        const m = Math.floor(seconds / 60).toString().padStart(2, '0');
        const s = (seconds % 60).toString().padStart(2, '0');
        timerDigits.textContent = `${m}:${s}`;
      }
    }, 1000);
  }

  // =========================================================================
  // 6. Live Relay Server Status
  // =========================================================================
  fetch('/api/status')
    .then(r => r.json())
    .then(data => {
      const pill = document.getElementById('hero-status-tag');
      if (pill && data.status === 'online') {
        pill.innerHTML = `🟢 Облачная сеть онлайн · ${data.roomsCount || 0} комнат`;
      }
    })
    .catch(() => {});
});
