package com.lkboard.app.collaboration

/**
 * Генератор мобильного веб-приложения для режима графического планшета (Phone Stylus Digitizer).
 * Превращает любой смартфон или планшет в беспроводной графический планшет:
 * - Полноэкранный режим без адресной строки (Fullscreen API)
 * - Поддержка стилусов с силой нажатия (Pointer Events & Pressure API)
 * - Palm Rejection (защита от случайных прикосновений ладонью)
 * - Мгновенная передача штрихов и координат пера на экран компьютера через локальную сеть Wi-Fi.
 */
object TabletClientHtml {

    fun generateHtml(boardTitle: String, port: Int, serverIp: String): String {
        return """
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
    <title>📱 Графический планшет — $boardTitle</title>
    <style>
        :root {
            --bg-color: #0f1115;
            --surface-color: rgba(24, 27, 32, 0.90);
            --border-color: rgba(255, 255, 255, 0.12);
            --accent: #2563eb;
            --accent-hover: #3b82f6;
            --text-main: #f4f4f5;
            --text-muted: #94a3b8;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            user-select: none;
            -webkit-user-select: none;
            -webkit-touch-callout: none;
        }

        html, body {
            width: 100%;
            height: 100%;
            overflow: hidden;
            background-color: var(--bg-color);
            color: var(--text-main);
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
            touch-action: none;
            overscroll-behavior: none;
        }

        #canvas {
            display: block;
            width: 100%;
            height: 100%;
            background-color: var(--bg-color);
            cursor: crosshair;
            touch-action: none;
        }

        /* Полноэкранный модальный запрос */
        .fs-overlay {
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(10, 12, 16, 0.96);
            backdrop-filter: blur(20px);
            -webkit-backdrop-filter: blur(20px);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 1000;
            padding: 20px;
            touch-action: auto !important;
            transition: opacity 0.25s ease, visibility 0.25s ease;
        }

        .fs-overlay.hidden {
            opacity: 0;
            visibility: hidden;
            pointer-events: none;
            display: none !important;
        }

        .fs-card {
            background: #181b20;
            border: 1px solid rgba(255, 255, 255, 0.15);
            border-radius: 24px;
            padding: 28px 22px;
            max-width: 420px;
            width: 100%;
            text-align: center;
            box-shadow: 0 24px 48px rgba(0, 0, 0, 0.6);
            touch-action: auto !important;
            position: relative;
        }

        .fs-close-btn {
            position: absolute;
            top: 14px;
            right: 14px;
            width: 32px;
            height: 32px;
            border-radius: 50%;
            background: rgba(255, 255, 255, 0.08);
            border: 1px solid rgba(255, 255, 255, 0.12);
            color: #94a3b8;
            font-size: 16px;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            touch-action: auto !important;
        }

        .fs-icon {
            font-size: 48px;
            margin-bottom: 12px;
            filter: drop-shadow(0 4px 12px rgba(37, 99, 235, 0.4));
        }

        .fs-title {
            font-size: 20px;
            font-weight: 800;
            margin-bottom: 8px;
            color: #ffffff;
            letter-spacing: -0.5px;
        }

        .fs-desc {
            font-size: 13px;
            color: var(--text-muted);
            line-height: 1.5;
            margin-bottom: 20px;
        }

        .btn-fullscreen {
            background: linear-gradient(135deg, #2563eb, #1d4ed8);
            color: #ffffff;
            border: none;
            padding: 16px 20px;
            border-radius: 14px;
            font-size: 15px;
            font-weight: 700;
            width: 100%;
            cursor: pointer;
            box-shadow: 0 8px 20px rgba(37, 99, 235, 0.35);
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 10px;
            touch-action: auto !important;
            -webkit-tap-highlight-color: transparent;
            transition: transform 0.1s ease, background 0.2s ease;
        }

        .btn-fullscreen:active {
            transform: scale(0.97);
        }

        .btn-fullscreen-skip {
            background: transparent;
            color: var(--text-muted);
            border: 1px solid rgba(255, 255, 255, 0.12);
            padding: 12px 18px;
            border-radius: 12px;
            font-size: 13px;
            font-weight: 600;
            width: 100%;
            cursor: pointer;
            margin-top: 10px;
            touch-action: auto !important;
            -webkit-tap-highlight-color: transparent;
            transition: all 0.15s ease;
        }

        .btn-fullscreen-skip:active {
            background: rgba(255, 255, 255, 0.08);
            color: #ffffff;
        }

        .fs-tip {
            margin-top: 14px;
            font-size: 11px;
            color: #64748b;
            line-height: 1.4;
        }

        /* Плавающая панель инструментов */
        .toolbar-pill {
            position: fixed;
            bottom: 16px;
            left: 50%;
            transform: translateX(-50%);
            background: var(--surface-color);
            backdrop-filter: blur(20px);
            -webkit-backdrop-filter: blur(20px);
            border: 1px solid var(--border-color);
            border-radius: 20px;
            padding: 8px 14px;
            display: flex;
            align-items: center;
            gap: 10px;
            z-index: 100;
            box-shadow: 0 12px 32px rgba(0, 0, 0, 0.5);
            max-width: 95vw;
            overflow-x: auto;
        }

        .tool-btn {
            background: transparent;
            border: 1px solid transparent;
            color: var(--text-muted);
            width: 38px;
            height: 38px;
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 18px;
            cursor: pointer;
            transition: all 0.15s ease;
            flex-shrink: 0;
        }

        .tool-btn.active {
            background: rgba(37, 99, 235, 0.25);
            color: #38bdf8;
            border-color: rgba(56, 189, 248, 0.4);
            transform: scale(1.05);
        }

        .color-dot {
            width: 22px;
            height: 22px;
            border-radius: 50%;
            border: 2px solid transparent;
            cursor: pointer;
            flex-shrink: 0;
            transition: transform 0.15s ease, border-color 0.15s ease;
        }

        .color-dot.active {
            border-color: #ffffff;
            transform: scale(1.22);
            box-shadow: 0 0 10px rgba(255, 255, 255, 0.4);
        }

        .divider {
            width: 1px;
            height: 24px;
            background: rgba(255, 255, 255, 0.15);
            flex-shrink: 0;
        }

        /* Индикатор подключения в углу */
        .status-badge {
            position: fixed;
            top: 12px;
            left: 12px;
            background: rgba(24, 27, 32, 0.85);
            backdrop-filter: blur(12px);
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 10px;
            padding: 5px 10px;
            font-size: 11px;
            font-weight: 600;
            display: flex;
            align-items: center;
            gap: 6px;
            z-index: 90;
            pointer-events: none;
        }

        .status-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: #10b981;
            box-shadow: 0 0 8px #10b981;
        }

        /* Кнопка вернуть полноэкранный режим */
        .btn-re-fs {
            position: fixed;
            top: 12px;
            right: 12px;
            background: rgba(24, 27, 32, 0.85);
            backdrop-filter: blur(12px);
            border: 1px solid rgba(255, 255, 255, 0.15);
            color: #ffffff;
            border-radius: 10px;
            padding: 6px 10px;
            font-size: 12px;
            font-weight: 600;
            cursor: pointer;
            z-index: 90;
            display: flex;
            align-items: center;
            gap: 5px;
        }

        /* Сетка планшета */
        .tablet-grid {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            pointer-events: none;
            background-image: radial-gradient(rgba(255, 255, 255, 0.10) 1px, transparent 1px);
            background-size: 24px 24px;
        }

        /* Угловые маркеры активной зоны дигитайзера (соответствие 4 углам монитора ПК) */
        .digitizer-corner {
            position: fixed;
            color: rgba(255, 255, 255, 0.35);
            font-size: 18px;
            font-family: monospace;
            font-weight: 900;
            pointer-events: none;
            user-select: none;
            z-index: 85;
            line-height: 1;
        }
        .corner-tl { top: 10px; left: 10px; }
        .corner-tr { top: 10px; right: 10px; }
        .corner-bl { bottom: 68px; left: 10px; }
        .corner-br { bottom: 68px; right: 10px; }
    </style>
</head>
<body>

    <div class="tablet-grid"></div>
    <div class="digitizer-corner corner-tl" title="Верхний левый угол монитора">⌜</div>
    <div class="digitizer-corner corner-tr" title="Верхний правый угол монитора">⌝</div>
    <div class="digitizer-corner corner-bl" title="Нижний левый угол монитора">⌞</div>
    <div class="digitizer-corner corner-br" title="Нижний правый угол монитора">⌟</div>

    <canvas id="canvas"></canvas>

    <!-- Индикатор подключения -->
    <div class="status-badge">
        <div class="status-dot" id="status-dot"></div>
        <span id="status-text">Графический планшет · 1:1 монитор</span>
    </div>

    <!-- Кнопка повторного входа в полный экран -->
    <button class="btn-re-fs" id="btn-re-fs" title="Развернуть во весь экран" type="button" onclick="window.enterFullscreenTablet()" ontouchend="window.enterFullscreenTablet()">⛶ Полный экран</button>

    <!-- Окно приглашения перейти в полный экран -->
    <div class="fs-overlay" id="fs-overlay" onclick="if(event.target===this) window.dismissTabletOverlay()">
        <div class="fs-card" id="fs-card">
            <button class="fs-close-btn" id="btn-fs-close" title="Закрыть" type="button" onclick="window.dismissTabletOverlay()" ontouchend="window.dismissTabletOverlay()">✕</button>
            <div class="fs-icon">📱✍️</div>
            <h1 class="fs-title">Режим графического планшета</h1>
            <p class="fs-desc">
                Телефон подключен к LKBoard на компьютере.<br>
                Разверните холст на весь экран, чтобы панели браузера не мешали писать стилусом или пальцем.
            </p>
            <button class="btn-fullscreen" id="btn-fs-start" type="button" onclick="window.enterFullscreenTablet()" ontouchend="window.enterFullscreenTablet()">
                <span>⛶ Открыть на полный экран</span>
            </button>
            <button class="btn-fullscreen-skip" id="btn-fs-skip" type="button" onclick="window.dismissTabletOverlay()" ontouchend="window.dismissTabletOverlay()">
                <span>Начать писать без полного экрана →</span>
            </button>
            <div class="fs-tip">
                Совет: поверните телефон горизонтально.<br>
                На iPhone: «Поделиться» → «На экран «Домой»» для режима без адресной строки.
            </div>
        </div>
    </div>

    <!-- Нижняя плавающая панель инструментов -->
    <div class="toolbar-pill">
        <button class="tool-btn active" id="tool-pen" title="Стилус / Ручка" type="button">🖊️</button>
        <button class="tool-btn" id="tool-highlighter" title="Маркер" type="button">🖍️</button>
        <button class="tool-btn" id="tool-eraser" title="Ластик" type="button">🧹</button>

        <div class="divider"></div>

        <!-- Цвета -->
        <div class="color-dot active" data-color="0xFF2563EB" style="background: #2563eb;" title="Синий"></div>
        <div class="color-dot" data-color="0xFF0F172A" style="background: #ffffff;" title="Белый"></div>
        <div class="color-dot" data-color="0xFFDC2626" style="background: #dc2626;" title="Красный"></div>
        <div class="color-dot" data-color="0xFF16A34A" style="background: #16a34a;" title="Зеленый"></div>
        <div class="color-dot" data-color="0xFFEAB308" style="background: #eab308;" title="Желтый"></div>

        <div class="divider"></div>

        <!-- Толщина -->
        <button class="tool-btn" id="btn-width" title="Толщина пера" type="button" style="font-size: 13px; font-weight: bold;">4px</button>
        <button class="tool-btn" id="btn-undo" title="Отменить последний штрих" type="button">↩️</button>
        <button class="tool-btn" id="btn-clear" title="Очистить холст" type="button">🗑️</button>
        <button class="tool-btn" id="btn-palm" title="Palm Rejection (защита ладони)" type="button">🖐️</button>
    </div>

    <script>
        // Глобальные функции управления экраном (гарантированная работа при нажатии в любой точке)
        window.dismissTabletOverlay = function() {
            var o = document.getElementById('fs-overlay');
            if (o) {
                o.classList.add('hidden');
                o.style.display = 'none';
            }
            if (typeof window.triggerResize === 'function') window.triggerResize();
        };

        window.enterFullscreenTablet = function() {
            window.dismissTabletOverlay();
            var el = document.documentElement;
            var rfs = el.requestFullscreen ||
                      el.webkitRequestFullscreen ||
                      el.webkitRequestFullScreen ||
                      el.mozRequestFullScreen ||
                      el.msRequestFullscreen;

            if (rfs) {
                try {
                    var p = rfs.call(el);
                    if (p && typeof p.catch === 'function') {
                        p.catch(function(){});
                    }
                } catch (_) {}
            }

            try { window.scrollTo(0, 1); } catch (_) {}
            try {
                if (screen.orientation && screen.orientation.lock) {
                    screen.orientation.lock('landscape').catch(function() {});
                }
            } catch (_) {}

            setTimeout(function() {
                if (typeof window.triggerResize === 'function') window.triggerResize();
            }, 150);
        };

        (function() {
            const canvas = document.getElementById('canvas');
            const ctx = canvas.getContext('2d');
            const overlay = document.getElementById('fs-overlay');
            const btnFsStart = document.getElementById('btn-fs-start');
            const btnReFs = document.getElementById('btn-re-fs');
            const btnWidth = document.getElementById('btn-width');
            const btnUndo = document.getElementById('btn-undo');
            const btnClear = document.getElementById('btn-clear');
            const btnPalm = document.getElementById('btn-palm');
            const btnFsClose = document.getElementById('btn-fs-close');
            const btnFsSkip = document.getElementById('btn-fs-skip');
            const toolbar = document.querySelector('.toolbar-pill');

            let currentTool = 'pen'; // 'pen', 'highlighter', 'eraser'
            let currentColorHex = 0xFF2563EB; // default blue (валидное число в JS, без суффикса L)
            let currentCssColor = '#2563eb';
            let currentWidth = 4;
            const widths = [2, 4, 8, 14];
            let widthIndex = 1;
            let palmRejection = true;

            let isDrawing = false;
            let currentStrokePoints = [];
            let strokesHistory = []; // list of elementIds for undo
            let activePointerId = null;
            let lastPointerMoveTime = 0;

            // Вьюпорт ПК
            let pcViewport = { panX: 0, panY: 0, scale: 1, width: 1366, height: 850 };

            // Предотвращение захвата касаний холстом при кликах по тулбару или оверлею
            if (toolbar) {
                toolbar.addEventListener('pointerdown', (e) => e.stopPropagation());
                toolbar.addEventListener('touchstart', (e) => e.stopPropagation(), { passive: true });
            }
            if (overlay) {
                overlay.addEventListener('pointerdown', (e) => e.stopPropagation());
                overlay.addEventListener('touchstart', (e) => e.stopPropagation(), { passive: true });
            }

            // Быстрый обработчик нажатия без 300мс мобильной задержки
            function addFastClickListener(element, handler) {
                if (!element) return;
                let touched = false;

                element.addEventListener('pointerdown', function(e) {
                    e.stopPropagation();
                });

                element.addEventListener('touchstart', function(e) {
                    e.stopPropagation();
                }, { passive: true });

                element.addEventListener('touchend', function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    touched = true;
                    handler(e);
                    setTimeout(function() { touched = false; }, 350);
                }, { passive: false });

                element.addEventListener('click', function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    if (!touched) {
                        handler(e);
                    }
                });
            }

            addFastClickListener(btnFsStart, window.enterFullscreenTablet);
            addFastClickListener(btnFsSkip, window.dismissTabletOverlay);
            addFastClickListener(btnFsClose, window.dismissTabletOverlay);
            addFastClickListener(btnReFs, window.enterFullscreenTablet);

            // Закрытие при клике по фону
            overlay.addEventListener('click', function(e) {
                if (e.target === overlay) {
                    dismissOverlay();
                }
            });

            function onFsChange() {
                const isFs = !!(document.fullscreenElement || document.webkitFullscreenElement || document.mozFullScreenElement);
                if (isFs) {
                    dismissOverlay();
                    btnReFs.style.display = 'none';
                } else {
                    btnReFs.style.display = 'flex';
                }
                resizeCanvas();
            }

            document.addEventListener('fullscreenchange', onFsChange);
            document.addEventListener('webkitfullscreenchange', onFsChange);
            document.addEventListener('mozfullscreenchange', onFsChange);

            // Настройка Canvas под ретину и экран
            function resizeCanvas() {
                const dpr = window.devicePixelRatio || 1;
                canvas.width = window.innerWidth * dpr;
                canvas.height = window.innerHeight * dpr;
                ctx.scale(dpr, dpr);
                redrawLocalCanvas();
            }
            window.triggerResize = resizeCanvas;

            window.addEventListener('resize', resizeCanvas);
            window.addEventListener('orientationchange', () => setTimeout(resizeCanvas, 200));
            resizeCanvas();

            // Запрос актуальной геометрии экрана и масштаба с ПК
            async function fetchViewport() {
                try {
                    const res = await fetch('/api/viewport');
                    if (res.ok) {
                        const vp = await res.json();
                        if (vp && vp.width > 50 && vp.height > 50) {
                            pcViewport = vp;
                        }
                    }
                } catch (_) {}
            }
            fetchViewport();
            setInterval(fetchViewport, 800);

            // Пересчет экранных координат планшета (0..phoneW, 0..phoneH) в мировые координаты доски ПК
            // Гарантирует абсолютное 1:1 соответствие: угол телефона = угол экрана монитора!
            function phoneToWorld(px, py) {
                const phoneW = window.innerWidth;
                const phoneH = window.innerHeight;

                const worldLeft = (0 - pcViewport.panX) / pcViewport.scale;
                const worldTop = (0 - pcViewport.panY) / pcViewport.scale;
                const worldRight = (pcViewport.width - pcViewport.panX) / pcViewport.scale;
                const worldBottom = (pcViewport.height - pcViewport.panY) / pcViewport.scale;

                const worldW = worldRight - worldLeft;
                const worldH = worldBottom - worldTop;

                // Клампинг координат строго внутри экрана планшета
                const cx = Math.max(0, Math.min(phoneW, px));
                const cy = Math.max(0, Math.min(phoneH, py));

                return {
                    x: worldLeft + (cx / phoneW) * worldW,
                    y: worldTop + (cy / phoneH) * worldH
                };
            }

            // Отправка действий на ПК через /api/action
            async function sendAction(actionObj) {
                try {
                    await fetch('/api/action', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(actionObj)
                    });
                } catch (e) {
                    console.error('Failed to send action:', e);
                }
            }

            // Отправка движения курсора стилуса на ПК
            function streamPointerMove(worldPos) {
                const now = performance.now();
                if (now - lastPointerMoveTime < 24) return; // ~40 FPS throttle
                lastPointerMoveTime = now;

                sendAction({
                    type: 'pointer_move',
                    senderId: 'phone-tablet-stylus',
                    senderName: 'Стилус (Телефон)',
                    x: worldPos.x,
                    y: worldPos.y,
                    colorHex: currentColorHex,
                    isLaser: false
                });
            }

            // Потоковая передача рисуемого штриха в реальном времени на монитор ПК
            let activeStrokeId = null;
            let lastLiveStreamTime = 0;

            async function streamLiveStroke(isFinal) {
                if (!isDrawing || currentStrokePoints.length < 2) return;
                const now = performance.now();
                if (!isFinal && now - lastLiveStreamTime < 32) return; // ~30 fps
                lastLiveStreamTime = now;

                if (!activeStrokeId) {
                    activeStrokeId = 'tablet-' + Date.now() + '-' + Math.random().toString(36).substr(2, 4);
                }

                const isHighlighter = currentTool === 'highlighter';
                const strokeWidth = currentWidth;

                const drawingPath = {
                    type: 'drawing_path',
                    id: activeStrokeId,
                    x: 0,
                    y: 0,
                    width: 0,
                    height: 0,
                    zIndex: 10,
                    isLocked: false,
                    rotation: 0,
                    points: currentStrokePoints.slice(),
                    strokeColorHex: currentColorHex,
                    strokeWidth: strokeWidth,
                    isHighlighter: isHighlighter
                };

                await sendAction({
                    type: 'upsert_element',
                    element: drawingPath,
                    senderId: 'phone-tablet-stylus',
                    senderName: 'Стилус (Телефон)'
                });
            }

            // Обработка касаний стилуса / пальца
            canvas.addEventListener('pointerdown', (e) => {
                if (activePointerId !== null) return;

                // Защита от касаний ладони: если касание пальцем при активном Palm Rejection и был стилус
                if (palmRejection && e.pointerType === 'touch' && window.hasUsedPen) {
                    return;
                }
                if (e.pointerType === 'pen') {
                    window.hasUsedPen = true;
                }

                activePointerId = e.pointerId;
                canvas.setPointerCapture(e.pointerId);
                isDrawing = true;

                const rect = canvas.getBoundingClientRect();
                const px = e.clientX - rect.left;
                const py = e.clientY - rect.top;
                const pressure = e.pressure > 0 ? e.pressure : 0.5;

                activeStrokeId = 'tablet-' + Date.now() + '-' + Math.random().toString(36).substr(2, 4);
                const world = phoneToWorld(px, py);
                currentStrokePoints = [{ x: world.x, y: world.y }];

                ctx.beginPath();
                ctx.lineCap = 'round';
                ctx.lineJoin = 'round';
                ctx.strokeStyle = currentTool === 'highlighter' ? currentCssColor + '70' : currentCssColor;
                ctx.lineWidth = currentWidth * (0.5 + pressure * 0.8);
                ctx.moveTo(px, py);

                streamPointerMove(world);
            });

            canvas.addEventListener('pointermove', (e) => {
                if (!isDrawing || e.pointerId !== activePointerId) return;

                const rect = canvas.getBoundingClientRect();
                const events = (e.getCoalescedEvents && e.getCoalescedEvents().length > 0)
                    ? e.getCoalescedEvents()
                    : [e];

                for (const ev of events) {
                    const px = ev.clientX - rect.left;
                    const py = ev.clientY - rect.top;
                    const pressure = ev.pressure > 0 ? ev.pressure : 0.5;
                    const world = phoneToWorld(px, py);

                    currentStrokePoints.push({ x: world.x, y: world.y });

                    ctx.lineWidth = currentWidth * (0.5 + pressure * 0.8);
                    ctx.lineTo(px, py);
                    ctx.stroke();

                    streamPointerMove(world);
                    streamLiveStroke(false);
                }
            });

            async function finishStroke() {
                if (!isDrawing) return;
                isDrawing = false;
                activePointerId = null;

                if (currentStrokePoints.length >= 2) {
                    await streamLiveStroke(true);
                    if (activeStrokeId) {
                        strokesHistory.push(activeStrokeId);
                    }
                }

                activeStrokeId = null;
                currentStrokePoints = [];

                // Планшет очищает локальный экран после завершения штриха (режим чистого дигитайзера),
                // а постоянный рисунок в высоком разрешении сохраняется на мониторе ПК!
                setTimeout(redrawLocalCanvas, 250);
            }

            canvas.addEventListener('pointerup', finishStroke);
            canvas.addEventListener('pointercancel', finishStroke);

            // Переключение инструментов с мгновенным откликом
            addFastClickListener(document.getElementById('tool-pen'), () => {
                currentTool = 'pen';
                document.querySelectorAll('.tool-btn').forEach(b => b.classList.remove('active'));
                document.getElementById('tool-pen').classList.add('active');
            });

            addFastClickListener(document.getElementById('tool-highlighter'), () => {
                currentTool = 'highlighter';
                document.querySelectorAll('.tool-btn').forEach(b => b.classList.remove('active'));
                document.getElementById('tool-highlighter').classList.add('active');
            });

            addFastClickListener(document.getElementById('tool-eraser'), () => {
                currentTool = 'eraser';
                document.querySelectorAll('.tool-btn').forEach(b => b.classList.remove('active'));
                document.getElementById('tool-eraser').classList.add('active');
            });

            // Выбор цвета
            document.querySelectorAll('.color-dot').forEach(dot => {
                addFastClickListener(dot, () => {
                    document.querySelectorAll('.color-dot').forEach(d => d.classList.remove('active'));
                    dot.classList.add('active');
                    currentColorHex = Number(dot.getAttribute('data-color')) || 0xFF2563EB;
                    currentCssColor = dot.style.background;
                });
            });

            // Толщина пера
            addFastClickListener(btnWidth, () => {
                widthIndex = (widthIndex + 1) % widths.length;
                currentWidth = widths[widthIndex];
                btnWidth.innerText = currentWidth + 'px';
            });

            // Отмена штриха
            addFastClickListener(btnUndo, async () => {
                if (strokesHistory.length === 0) return;
                const lastId = strokesHistory.pop();
                await sendAction({
                    type: 'delete_elements',
                    elementIds: [lastId],
                    senderId: 'phone-tablet-stylus'
                });
                resizeCanvas();
            });

            // Очистка экрана планшета
            addFastClickListener(btnClear, () => {
                resizeCanvas();
            });

            // Palm Rejection
            addFastClickListener(btnPalm, () => {
                palmRejection = !palmRejection;
                btnPalm.style.opacity = palmRejection ? '1.0' : '0.4';
            });

            function redrawLocalCanvas() {
                ctx.clearRect(0, 0, canvas.width, canvas.height);
            }

            // Уведомление ПК о подключении планшета (число 0xFF2563EB без L)
            sendAction({
                type: 'peer_joined',
                peer: {
                    id: 'phone-tablet-stylus',
                    name: 'Стилус (Телефон)',
                    colorHex: 0xFF2563EB,
                    isHost: false,
                    clientType: 'Tablet'
                }
            });

        })();
    </script>
</body>
</html>
        """.trimIndent()
    }
}
