package com.lkboard.app.collaboration

object WebClientHtml {

    fun generateHtml(boardTitle: String, hostName: String, port: Int): String {
        return """
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>$boardTitle — LKBoard Live Collaboration</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body, html {
            width: 100%; height: 100%;
            overflow: hidden;
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            background-color: #f1f5f9;
            user-select: none;
            -webkit-user-select: none;
        }

        #canvas {
            display: block;
            width: 100%;
            height: 100%;
            cursor: crosshair;
        }

        /* Apple Liquid Glass Panels */
        .glass-panel {
            background: rgba(255, 255, 255, 0.78);
            backdrop-filter: blur(24px);
            -webkit-backdrop-filter: blur(24px);
            border: 1px solid rgba(255, 255, 255, 0.85);
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08), 0 1px 3px rgba(0, 0, 0, 0.04);
            border-radius: 16px;
        }

        /* Top Header */
        .header {
            position: fixed;
            top: 16px;
            left: 16px;
            right: 16px;
            height: 52px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0 18px;
            z-index: 100;
        }

        .header-title {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .logo-badge {
            width: 32px;
            height: 32px;
            background: linear-gradient(135deg, #0284c7, #3b82f6);
            border-radius: 8px;
            display: flex;
            align-items: center;
            justify-content: center;
            color: #fff;
            font-weight: 700;
            font-size: 14px;
            box-shadow: 0 2px 8px rgba(2, 132, 199, 0.3);
        }

        .board-title {
            font-weight: 700;
            font-size: 16px;
            color: #0f172a;
            letter-spacing: -0.2px;
        }

        .status-pill {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 6px 14px;
            border-radius: 20px;
            font-size: 13px;
            font-weight: 600;
            background: rgba(16, 185, 129, 0.12);
            color: #059669;
            border: 1px solid rgba(16, 185, 129, 0.25);
        }

        .status-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: #10b981;
            animation: pulse 2s infinite;
        }

        @keyframes pulse {
            0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7); }
            70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(16, 185, 129, 0); }
            100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
        }

        /* Floating Toolbar */
        .toolbar {
            position: fixed;
            bottom: 24px;
            left: 50%;
            transform: translateX(-50%);
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 8px 12px;
            z-index: 100;
        }

        .tool-btn {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            width: 44px;
            height: 44px;
            border-radius: 12px;
            border: none;
            background: transparent;
            color: #475569;
            font-size: 18px;
            cursor: pointer;
            transition: all 0.18s ease;
        }

        .tool-btn:hover {
            background: rgba(0, 0, 0, 0.05);
            color: #0f172a;
        }

        .tool-btn.active {
            background: #0284c7;
            color: #ffffff;
            box-shadow: 0 4px 12px rgba(2, 132, 199, 0.35);
        }

        .tool-divider {
            width: 1px;
            height: 24px;
            background: rgba(0, 0, 0, 0.1);
            margin: 0 4px;
        }

        /* Color Picker Pill */
        .color-dot {
            width: 18px;
            height: 18px;
            border-radius: 50%;
            cursor: pointer;
            border: 2px solid #ffffff;
            box-shadow: 0 1px 3px rgba(0,0,0,0.2);
            transition: transform 0.15s;
        }
        .color-dot:hover { transform: scale(1.2); }
        .color-dot.active { border-color: #0f172a; transform: scale(1.15); }

        /* Notification Toast */
        .toast {
            position: fixed;
            bottom: 84px;
            left: 50%;
            transform: translateX(-50%);
            padding: 8px 16px;
            border-radius: 20px;
            font-size: 13px;
            font-weight: 500;
            background: rgba(15, 23, 42, 0.88);
            color: #ffffff;
            backdrop-filter: blur(12px);
            z-index: 150;
            opacity: 0;
            pointer-events: none;
            transition: opacity 0.25s ease;
        }
        .toast.show { opacity: 1; }
    </style>
</head>
<body>

    <!-- Header -->
    <header class="header glass-panel">
        <div class="header-title">
            <div class="logo-badge">LK</div>
            <div>
                <div class="board-title">$boardTitle</div>
                <div style="font-size: 11px; color: #64748b;">Хост: $hostName • Совместный доступ</div>
            </div>
        </div>
        <div class="status-pill" id="statusPill">
            <div class="status-dot"></div>
            <span id="statusText">Подключено (Live)</span>
        </div>
    </header>

    <!-- Canvas -->
    <canvas id="canvas"></canvas>

    <!-- Toolbar -->
    <div class="toolbar glass-panel">
        <button class="tool-btn active" id="toolHand" title="Панорамирование (Рука)">✋</button>
        <button class="tool-btn" id="toolPen" title="Маркер / Рисование">✏️</button>
        <button class="tool-btn" id="toolSticky" title="Добавить стикер">🗂️</button>
        <button class="tool-btn" id="toolLaser" title="Лазерная указка">🎯</button>
        
        <div class="tool-divider"></div>

        <div style="display: flex; gap: 6px; align-items: center; padding: 0 4px;">
            <div class="color-dot active" data-color="#0284c7" style="background: #0284c7;"></div>
            <div class="color-dot" data-color="#ef4444" style="background: #ef4444;"></div>
            <div class="color-dot" data-color="#10b981" style="background: #10b981;"></div>
            <div class="color-dot" data-color="#8b5cf6" style="background: #8b5cf6;"></div>
            <div class="color-dot" data-color="#0f172a" style="background: #0f172a;"></div>
        </div>

        <div class="tool-divider"></div>

        <button class="tool-btn" id="toolResetZoom" title="Сбросить масштаб">🔍</button>
    </div>

    <div class="toast" id="toast">Стикер создан</div>

    <script>
        // State
        let elements = {};
        let activeTool = 'hand'; // 'hand', 'pen', 'sticky', 'laser'
        let currentColor = '#0284c7';
        let panX = 0, panY = 0;
        let scale = 1.0;
        let isDragging = false;
        let dragStartX = 0, dragStartY = 0;
        let currentStroke = null;
        let remotePointers = {};
        const clientId = 'web-' + Math.random().toString(36).substring(2, 9);
        const clientName = 'Гость ' + Math.floor(100 + Math.random() * 900);

        const canvas = document.getElementById('canvas');
        const ctx = canvas.getContext('2d');

        function resizeCanvas() {
            canvas.width = window.innerWidth * window.devicePixelRatio;
            canvas.height = window.innerHeight * window.devicePixelRatio;
            canvas.style.width = window.innerWidth + 'px';
            canvas.style.height = window.innerHeight + 'px';
            render();
        }
        window.addEventListener('resize', resizeCanvas);
        resizeCanvas();

        function worldToScreen(wx, wy) {
            return {
                x: (wx * scale + panX) * window.devicePixelRatio,
                y: (wy * scale + panY) * window.devicePixelRatio
            };
        }

        function screenToWorld(sx, sy) {
            return {
                x: (sx - panX) / scale,
                y: (sy - panY) / scale
            };
        }

        // Render Canvas
        function render() {
            const dpr = window.devicePixelRatio;
            ctx.save();
            ctx.clearRect(0, 0, canvas.width, canvas.height);

            // Draw Subtle Grid
            drawGrid();

            // Apply Camera Transform
            ctx.scale(dpr, dpr);
            ctx.translate(panX, panY);
            ctx.scale(scale, scale);

            // Sort elements by zIndex
            const sorted = Object.values(elements).sort((a, b) => (a.zIndex || 0) - (b.zIndex || 0));

            for (const el of sorted) {
                drawElement(el);
            }

            // Draw local stroke in progress
            if (currentStroke && currentStroke.points && currentStroke.points.length > 1) {
                drawStroke(currentStroke);
            }

            // Draw remote pointers
            for (const pid in remotePointers) {
                drawPointer(remotePointers[pid]);
            }

            ctx.restore();
        }

        function drawGrid() {
            const dpr = window.devicePixelRatio;
            ctx.save();
            ctx.scale(dpr, dpr);
            ctx.fillStyle = '#cbd5e1';
            const step = 32 * scale;
            if (step > 8) {
                const startX = (panX % step + step) % step;
                const startY = (panY % step + step) % step;
                for (let x = startX; x < window.innerWidth; x += step) {
                    for (let y = startY; y < window.innerHeight; y += step) {
                        ctx.beginPath();
                        ctx.arc(x, y, 1.2, 0, Math.PI * 2);
                        ctx.fill();
                    }
                }
            }
            ctx.restore();
        }

        function drawElement(el) {
            ctx.save();
            if (el.type === 'drawing_path' || el.points) {
                drawStroke(el);
            } else if (el.type === 'sticky_note' || (el.text !== undefined && el.colorHex !== undefined && el.type !== 'text')) {
                drawSticky(el);
            } else if (el.type === 'milestone_card' || el.status !== undefined) {
                drawMilestone(el);
            } else if (el.type === 'kanban_board') {
                drawKanban(el);
            } else if (el.type === 'document_frame' || el.totalPages !== undefined || el.currentPageIndex !== undefined) {
                drawDocumentFrame(el);
            } else if (el.type === 'function_graph' || el.expressions !== undefined) {
                drawFunctionGraph(el);
            } else if (el.type === 'shape' || el.shapeType !== undefined) {
                drawShape(el);
            } else if (el.type === 'text') {
                drawText(el);
            }
            ctx.restore();
        }

        function drawStroke(s) {
            if (!s.points || s.points.length < 2) return;
            ctx.beginPath();
            ctx.strokeStyle = s.strokeColorHex ? hexToColor(s.strokeColorHex) : (s.color || '#0284c7');
            ctx.lineWidth = s.strokeWidth || 3;
            ctx.lineCap = 'round';
            ctx.lineJoin = 'round';

            ctx.moveTo(s.points[0].x, s.points[0].y);
            for (let i = 1; i < s.points.length; i++) {
                ctx.lineTo(s.points[i].x, s.points[i].y);
            }
            ctx.stroke();
        }

        function drawSticky(note) {
            const x = note.x, y = note.y, w = note.width || 210, h = note.height || 210;
            const bg = note.colorHex ? hexToColor(note.colorHex) : '#fef08a';

            // Shadow
            ctx.shadowColor = 'rgba(0, 0, 0, 0.08)';
            ctx.shadowBlur = 12;
            ctx.shadowOffsetY = 4;

            // Card Body
            ctx.fillStyle = bg;
            roundRect(ctx, x, y, w, h, 14);
            ctx.fill();

            // Paper peel fold at bottom right
            ctx.shadowColor = 'transparent';
            const foldSize = 22;
            ctx.beginPath();
            ctx.moveTo(x + w - foldSize, y + h);
            ctx.lineTo(x + w, y + h - foldSize);
            ctx.lineTo(x + w - foldSize, y + h - foldSize);
            ctx.closePath();
            ctx.fillStyle = 'rgba(0, 0, 0, 0.07)';
            ctx.fill();

            // Border
            ctx.strokeStyle = 'rgba(0, 0, 0, 0.06)';
            ctx.lineWidth = 1;
            roundRect(ctx, x, y, w, h, 14);
            ctx.stroke();

            // Content
            ctx.fillStyle = '#1e293b';
            ctx.font = '14px Inter, sans-serif';
            const lines = (note.text || '').split('\n');
            let curY = y + 26;
            for (let i = 0; i < lines.length && curY < y + h - 16; i++) {
                const line = lines[i];
                if (line.startsWith('- [x] ') || line.startsWith('- [X] ')) {
                    // Checked box
                    drawCheckbox(x + 14, curY - 10, true);
                    ctx.save();
                    ctx.fillStyle = '#94a3b8';
                    ctx.fillText(line.substring(6), x + 34, curY);
                    ctx.restore();
                } else if (line.startsWith('- [ ] ')) {
                    // Unchecked box
                    drawCheckbox(x + 14, curY - 10, false);
                    ctx.fillText(line.substring(6), x + 34, curY);
                } else {
                    ctx.fillText(line, x + 14, curY);
                }
                curY += 20;
            }
        }

        function drawCheckbox(x, y, checked) {
            ctx.save();
            ctx.strokeStyle = checked ? '#10b981' : '#94a3b8';
            ctx.fillStyle = checked ? '#10b981' : '#ffffff';
            ctx.lineWidth = 1.5;
            roundRect(ctx, x, y, 14, 14, 3);
            ctx.fill();
            ctx.stroke();
            if (checked) {
                ctx.strokeStyle = '#ffffff';
                ctx.beginPath();
                ctx.moveTo(x + 3, y + 7);
                ctx.lineTo(x + 6, y + 10);
                ctx.lineTo(x + 11, y + 4);
                ctx.stroke();
            }
            ctx.restore();
        }

        function drawMilestone(card) {
            const x = card.x, y = card.y, w = card.width || 280, h = card.height || 160;
            ctx.fillStyle = '#ffffff';
            ctx.shadowColor = 'rgba(0, 0, 0, 0.06)';
            ctx.shadowBlur = 10;
            roundRect(ctx, x, y, w, h, 14);
            ctx.fill();
            ctx.shadowColor = 'transparent';
            ctx.strokeStyle = '#e2e8f0';
            ctx.stroke();

            // Title
            ctx.fillStyle = '#0f172a';
            ctx.font = 'bold 15px Inter, sans-serif';
            ctx.fillText(card.title || 'Веха', x + 16, y + 30);

            // Status Badge
            const statusColor = card.status === 'DONE' ? '#10b981' : card.status === 'IN_PROGRESS' ? '#f59e0b' : '#3b82f6';
            ctx.fillStyle = statusColor;
            roundRect(ctx, x + 16, y + 42, 80, 20, 10);
            ctx.fill();
            ctx.fillStyle = '#ffffff';
            ctx.font = 'bold 10px Inter, sans-serif';
            ctx.fillText(card.status || 'В работе', x + 24, y + 56);
        }

        function drawKanban(k) {
            const x = k.x, y = k.y, w = k.width || 1000, h = k.height || 620;
            const kScale = Math.min(3.5, Math.max(0.6, w / 1000));
            const headH = Math.max(36, 46 * kScale);
            const colHeadH = Math.max(28, 38 * kScale);

            ctx.fillStyle = 'rgba(248, 250, 252, 0.95)';
            roundRect(ctx, x, y, w, h, 16 * kScale);
            ctx.fill();
            ctx.strokeStyle = '#cbd5e1';
            ctx.lineWidth = 1.5;
            ctx.stroke();

            // Header
            ctx.fillStyle = '#0f172a';
            ctx.font = 'bold ' + Math.round(15 * kScale) + 'px Inter, sans-serif';
            ctx.fillText('📋 ' + (k.title || 'Канбан-доска'), x + 18 * kScale, y + headH * 0.65);

            const cols = ['🔴 Бэклог', '🟡 В работе', '🟣 На ревью', '🟢 Готово'];
            const colW = w / 4;
            for (let i = 0; i < 4; i++) {
                const cx = x + i * colW;
                const cy = y + headH;
                const cw = colW;
                const ch = h - headH;

                if (i > 0) {
                    ctx.beginPath();
                    ctx.moveTo(cx, cy);
                    ctx.lineTo(cx, cy + ch);
                    ctx.strokeStyle = '#e2e8f0';
                    ctx.lineWidth = 1;
                    ctx.stroke();
                }

                ctx.fillStyle = '#334155';
                ctx.font = 'bold ' + Math.round(12 * kScale) + 'px Inter, sans-serif';
                ctx.fillText(cols[i], cx + 14 * kScale, cy + colHeadH * 0.65);
            }
        }

        function drawDocumentFrame(f) {
            const x = f.x, y = f.y, w = f.width || 800, h = f.height || 450;
            const fScale = Math.min(3.5, Math.max(0.6, w / 800));
            const headH = Math.max(28, 38 * fScale);
            const cr = 10 * fScale;

            ctx.fillStyle = '#ffffff';
            roundRect(ctx, x, y, w, h, cr);
            ctx.fill();
            ctx.strokeStyle = '#cbd5e1';
            ctx.lineWidth = 1.2 * fScale;
            ctx.stroke();

            // Header
            ctx.fillStyle = '#f1f5f9';
            roundRect(ctx, x, y, w, headH, cr);
            ctx.fill();
            ctx.strokeStyle = '#e2e8f0';
            ctx.beginPath();
            ctx.moveTo(x, y + headH); ctx.lineTo(x + w, y + headH);
            ctx.stroke();

            // Title
            ctx.fillStyle = '#1e293b';
            ctx.font = 'bold ' + Math.round(13 * fScale) + 'px Inter, sans-serif';
            ctx.fillText(f.title || 'Презентация / PDF', x + 14 * fScale, y + headH * 0.65);

            // Badge
            const pageTxt = (f.totalPages && f.totalPages > 0) ? ((f.currentPageIndex || 0) + 1) + ' / ' + f.totalPages : 'Нет файла';
            ctx.fillStyle = '#e0f2fe';
            const badgeW = 75 * fScale, badgeH = 22 * fScale;
            roundRect(ctx, x + w - badgeW - 12 * fScale, y + (headH - badgeH) / 2, badgeW, badgeH, badgeH / 2);
            ctx.fill();
            ctx.fillStyle = '#0284c7';
            ctx.font = '600 ' + Math.round(11 * fScale) + 'px Inter, sans-serif';
            ctx.fillText(pageTxt, x + w - badgeW + 8 * fScale, y + headH * 0.62);

            // Placeholder content if no image
            ctx.fillStyle = '#f8fafc';
            ctx.fillRect(x, y + headH, w, h - headH);
            ctx.fillStyle = '#64748b';
            ctx.font = '600 ' + Math.round(15 * fScale) + 'px Inter, sans-serif';
            ctx.fillText('Область презентации / PDF', x + w / 2 - 95 * fScale, y + headH + (h - headH) / 2);
        }

        function drawFunctionGraph(g) {
            const x = g.x, y = g.y, w = g.width || 440, h = g.height || 280;
            const gScale = Math.min(3.5, Math.max(0.6, w / 440));
            const headH = Math.max(28, 34 * gScale);

            ctx.fillStyle = '#ffffff';
            roundRect(ctx, x, y, w, h, 12 * gScale);
            ctx.fill();
            ctx.strokeStyle = '#cbd5e1';
            ctx.lineWidth = 1.2;
            ctx.stroke();

            // Header
            ctx.fillStyle = '#f8fafc';
            roundRect(ctx, x, y, w, headH, 12 * gScale);
            ctx.fill();
            ctx.fillStyle = '#1e293b';
            ctx.font = 'bold ' + Math.round(12 * gScale) + 'px Inter, sans-serif';
            const fTitle = (g.expressions && g.expressions.length > 0) ? g.expressions.map(e => 'f(x)=' + e).join(', ') : (g.title || 'График');
            ctx.fillText(fTitle, x + 12 * gScale, y + headH * 0.65);

            // Inner plot clipping
            const pad = 12 * gScale;
            const px = x + pad, py = y + headH + pad, pw = w - pad * 2, ph = h - headH - pad * 2;
            if (pw <= 10 || ph <= 10) return;

            ctx.save();
            ctx.beginPath();
            ctx.rect(px, py, pw, ph);
            ctx.clip();

            // Background of plot
            ctx.fillStyle = '#f8fafc';
            ctx.fillRect(px, py, pw, ph);

            const xMin = g.xMin !== undefined ? g.xMin : -10;
            const xMax = g.xMax !== undefined ? g.xMax : 10;
            const yMin = g.yMin !== undefined ? g.yMin : -5;
            const yMax = g.yMax !== undefined ? g.yMax : 5;

            const toScreenX = (val) => px + ((val - xMin) / (xMax - xMin)) * pw;
            const toScreenY = (val) => py + ((yMax - val) / (yMax - yMin)) * ph;

            const originX = Math.max(px, Math.min(px + pw, toScreenX(0)));
            const originY = Math.max(py, Math.min(py + ph, toScreenY(0)));

            // Grid lines and numbers
            ctx.font = '600 ' + Math.round(9 * gScale) + 'px Inter, sans-serif';
            ctx.fillStyle = '#64748b';
            ctx.strokeStyle = '#e2e8f0';
            ctx.lineWidth = 1;

            const xStep = Math.max(1, Math.round((xMax - xMin) / 8));
            for (let gx = Math.ceil(xMin); gx <= Math.floor(xMax); gx += xStep) {
                const sx = toScreenX(gx);
                ctx.beginPath();
                ctx.moveTo(sx, py); ctx.lineTo(sx, py + ph);
                ctx.stroke();
                if (gx !== 0) {
                    ctx.fillText(gx.toString(), sx - 6, Math.min(py + ph - 4, Math.max(py + 12, originY + 12)));
                }
            }

            const yStep = Math.max(1, Math.round((yMax - yMin) / 6));
            for (let gy = Math.ceil(yMin); gy <= Math.floor(yMax); gy += yStep) {
                const sy = toScreenY(gy);
                ctx.beginPath();
                ctx.moveTo(px, sy); ctx.lineTo(px + pw, sy);
                ctx.stroke();
                if (gy !== 0) {
                    ctx.fillText(gy.toString(), Math.min(px + pw - 18, Math.max(px + 4, originX + 4)), sy + 3);
                }
            }

            // Axes
            ctx.strokeStyle = '#94a3b8';
            ctx.lineWidth = 1.5;
            ctx.beginPath();
            ctx.moveTo(px, originY); ctx.lineTo(px + pw, originY);
            ctx.moveTo(originX, py); ctx.lineTo(originX, py + ph);
            ctx.stroke();

            // Zero label
            ctx.fillText('0', originX + 4, originY + 12);

            // Simple parser/evaluator for graph curves f(x)
            if (g.expressions && g.expressions.length > 0) {
                const colors = ['#2563eb', '#dc2626', '#16a34a', '#9333ea'];
                g.expressions.forEach((expr, idx) => {
                    ctx.strokeStyle = colors[idx % colors.length];
                    ctx.lineWidth = 2.4;
                    ctx.beginPath();
                    let started = false;
                    const steps = 180;
                    for (let s = 0; s <= steps; s++) {
                        const xv = xMin + (s / steps) * (xMax - xMin);
                        let yv = null;
                        try {
                            let jsExpr = expr.replace(/\^/g, '**')
                                             .replace(/sin\(/g, 'Math.sin(')
                                             .replace(/cos\(/g, 'Math.cos(')
                                             .replace(/tan\(/g, 'Math.tan(')
                                             .replace(/sqrt\(/g, 'Math.sqrt(')
                                             .replace(/abs\(/g, 'Math.abs(')
                                             .replace(/pi/gi, 'Math.PI')
                                             .replace(/e/gi, 'Math.E');
                            const fn = new Function('x', 'return ' + jsExpr);
                            yv = fn(xv);
                        } catch(e) {}

                        if (yv !== null && !isNaN(yv) && isFinite(yv) && yv >= yMin - 50 && yv <= yMax + 50) {
                            const sx = toScreenX(xv);
                            const sy = toScreenY(yv);
                            if (!started) {
                                ctx.moveTo(sx, sy);
                                started = true;
                            } else {
                                ctx.lineTo(sx, sy);
                            }
                        } else {
                            started = false;
                        }
                    }
                    ctx.stroke();
                });
            }
            ctx.restore();
        }

        function drawShape(s) {
            const x = s.x, y = s.y, w = s.width || 120, h = s.height || 80;
            ctx.fillStyle = s.fillColorHex ? hexToColor(s.fillColorHex) : 'rgba(255, 255, 255, 0.85)';
            ctx.strokeStyle = s.strokeColorHex ? hexToColor(s.strokeColorHex) : '#0284c7';
            ctx.lineWidth = s.strokeWidth || 2;
            roundRect(ctx, x, y, w, h, 8);
            ctx.fill();
            ctx.stroke();
        }

        function drawText(t) {
            ctx.fillStyle = t.colorHex ? hexToColor(t.colorHex) : '#0f172a';
            const fSize = Math.round(t.fontSize || 18);
            const fWeight = t.isBold ? 'bold ' : '';
            const fStyle = t.isItalic ? 'italic ' : '';
            ctx.font = fStyle + fWeight + fSize + 'px Inter, sans-serif';
            ctx.fillText(t.text || '', t.x, t.y + fSize);
        }

        function drawPointer(p) {
            if (Date.now() - p.lastUpdated > 4000) return;
            const x = p.x, y = p.y;
            ctx.fillStyle = p.colorHex ? hexToColor(p.colorHex) : '#ef4444';
            ctx.beginPath();
            ctx.arc(x, y, p.isLaser ? 8 : 5, 0, Math.PI * 2);
            ctx.fill();

            // Name pill
            ctx.font = '11px Inter, sans-serif';
            const name = p.peerName || 'Участник';
            const nw = ctx.measureText(name).width;
            ctx.fillStyle = 'rgba(15, 23, 42, 0.85)';
            roundRect(ctx, x + 8, y - 10, nw + 12, 20, 6);
            ctx.fill();
            ctx.fillStyle = '#ffffff';
            ctx.fillText(name, x + 14, y + 4);
        }

        function roundRect(c, x, y, w, h, r) {
            if (w < 2 * r) r = w / 2;
            if (h < 2 * r) r = h / 2;
            c.beginPath();
            c.moveTo(x + r, y);
            c.arcTo(x + w, y, x + w, y + h, r);
            c.arcTo(x + w, y + h, x, y + h, r);
            c.arcTo(x, y + h, x, y, r);
            c.arcTo(x, y, x + w, y, r);
            c.closePath();
        }

        function hexToColor(hex) {
            if (typeof hex === 'string') return hex;
            const num = (Number(hex) >>> 0) & 0xFFFFFF;
            return '#' + num.toString(16).padStart(6, '0');
        }

        // Pointer Events
        canvas.addEventListener('pointerdown', (e) => {
            const world = screenToWorld(e.clientX, e.clientY);
            isDragging = true;
            dragStartX = e.clientX;
            dragStartY = e.clientY;

            if (activeTool === 'pen') {
                currentStroke = {
                    id: 'path-' + Math.random().toString(36).substring(2, 9),
                    type: 'drawing_path',
                    points: [{ x: world.x, y: world.y }],
                    strokeColorHex: parseInt(currentColor.replace('#', '0xFF'), 16),
                    strokeWidth: 3.5,
                    zIndex: 100
                };
            } else if (activeTool === 'sticky') {
                createStickyAt(world.x, world.y);
            } else if (activeTool === 'laser') {
                sendPointer(world.x, world.y, true);
            } else if (activeTool === 'hand') {
                // Check if user clicked a To-Do checkbox on a sticky note
                checkStickyClick(world.x, world.y);
            }
        });

        canvas.addEventListener('pointermove', (e) => {
            const world = screenToWorld(e.clientX, e.clientY);

            if (activeTool === 'laser') {
                sendPointer(world.x, world.y, true);
            }

            if (!isDragging) return;

            if (activeTool === 'hand') {
                const dx = e.clientX - dragStartX;
                const dy = e.clientY - dragStartY;
                panX += dx;
                panY += dy;
                dragStartX = e.clientX;
                dragStartY = e.clientY;
                render();
            } else if (activeTool === 'pen' && currentStroke) {
                currentStroke.points.push({ x: world.x, y: world.y });
                render();
            }
        });

        canvas.addEventListener('pointerup', () => {
            if (activeTool === 'pen' && currentStroke && currentStroke.points.length > 1) {
                elements[currentStroke.id] = currentStroke;
                sendAction({
                    type: 'upsert_element',
                    element: currentStroke,
                    senderId: clientId,
                    senderName: clientName
                });
                currentStroke = null;
                render();
            }
            isDragging = false;
        });

        canvas.addEventListener('wheel', (e) => {
            e.preventDefault();
            const zoomFactor = e.deltaY < 0 ? 1.1 : 0.9;
            const newScale = Math.max(0.1, Math.min(5.0, scale * zoomFactor));

            const mouseX = e.clientX;
            const mouseY = e.clientY;

            panX = mouseX - (mouseX - panX) * (newScale / scale);
            panY = mouseY - (mouseY - panY) * (newScale / scale);
            scale = newScale;
            render();
        }, { passive: false });

        function checkStickyClick(wx, wy) {
            for (const id in elements) {
                const el = elements[id];
                if ((el.type === 'sticky_note' || el.text) && wx >= el.x && wx <= el.x + (el.width || 210) && wy >= el.y && wy <= el.y + (el.height || 210)) {
                    // Check if clicked checkbox lines
                    const lines = (el.text || '').split('\n');
                    let cy = el.y + 26;
                    for (let i = 0; i < lines.length; i++) {
                        if (lines[i].startsWith('- [ ] ') || lines[i].startsWith('- [x] ') || lines[i].startsWith('- [X] ')) {
                            if (wx >= el.x + 10 && wx <= el.x + 34 && wy >= cy - 14 && wy <= cy + 6) {
                                // Toggle!
                                sendAction({
                                    type: 'toggle_sticky_todo',
                                    noteId: el.id,
                                    lineIndex: i,
                                    senderId: clientId
                                });
                                showToast('Статус задачи изменен');
                                return;
                            }
                        }
                        cy += 20;
                    }
                }
            }
        }

        function createStickyAt(x, y) {
            const note = {
                id: 'note-' + Math.random().toString(36).substring(2, 9),
                type: 'sticky_note',
                x: x - 105,
                y: y - 105,
                width: 210,
                height: 210,
                text: 'Заметка от ' + clientName + '\n- [ ] Задача 1\n- [ ] Задача 2',
                colorHex: 0xFFFEF08A,
                zIndex: 50
            };
            elements[note.id] = note;
            sendAction({
                type: 'upsert_element',
                element: note,
                senderId: clientId,
                senderName: clientName
            });
            render();
            showToast('Стикер добавлен на доску');
        }

        function sendPointer(x, y, isLaser) {
            sendAction({
                type: 'pointer_move',
                senderId: clientId,
                senderName: clientName,
                x: x,
                y: y,
                colorHex: 0xFF0284C7,
                isLaser: isLaser
            });
        }

        function sendAction(action) {
            fetch('/api/action', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(action)
            }).catch(() => {});
        }

        function showToast(msg) {
            const toast = document.getElementById('toast');
            toast.textContent = msg;
            toast.classList.add('show');
            setTimeout(() => toast.classList.remove('show'), 2200);
        }

        // Toolbar UI Events
        document.getElementById('toolHand').onclick = () => setTool('hand');
        document.getElementById('toolPen').onclick = () => setTool('pen');
        document.getElementById('toolSticky').onclick = () => setTool('sticky');
        document.getElementById('toolLaser').onclick = () => setTool('laser');
        document.getElementById('toolResetZoom').onclick = () => {
            panX = 0; panY = 0; scale = 1.0; render();
        };

        function setTool(tool) {
            activeTool = tool;
            document.querySelectorAll('.tool-btn').forEach(b => b.classList.remove('active'));
            if (tool === 'hand') document.getElementById('toolHand').classList.add('active');
            if (tool === 'pen') document.getElementById('toolPen').classList.add('active');
            if (tool === 'sticky') document.getElementById('toolSticky').classList.add('active');
            if (tool === 'laser') document.getElementById('toolLaser').classList.add('active');
        }

        document.querySelectorAll('.color-dot').forEach(dot => {
            dot.onclick = () => {
                document.querySelectorAll('.color-dot').forEach(d => d.classList.remove('active'));
                dot.classList.add('active');
                currentColor = dot.dataset.color;
            };
        });

        // Server-Sent Events (SSE) Live Sync + Periodic Delta Sync
        let evtSource = null;
        let lastSyncVersion = 0;
        let seenActionIds = new Set();

        function connectLiveStream() {
            if (evtSource) {
                try { evtSource.close(); } catch(e) {}
            }
            evtSource = new EventSource('/api/events');
            
            evtSource.onopen = () => {
                document.getElementById('statusText').textContent = 'Подключено (Live)';
                document.getElementById('statusPill').style.background = 'rgba(16, 185, 129, 0.12)';
                document.getElementById('statusPill').style.color = '#059669';
            };

            evtSource.onmessage = (e) => {
                try {
                    const action = JSON.parse(e.data);
                    handleRemoteAction(action);
                } catch (err) {
                    console.error('Invalid SSE action', err);
                }
            };

            evtSource.onerror = () => {
                document.getElementById('statusText').textContent = 'Синхронизация...';
                document.getElementById('statusPill').style.background = 'rgba(245, 158, 11, 0.12)';
                document.getElementById('statusPill').style.color = '#d97706';
                setTimeout(connectLiveStream, 3000);
            };
        }

        function pollDeltaSync() {
            fetch('/api/sync?since=' + lastSyncVersion)
                .then(r => r.json())
                .then(data => {
                    if (data && data.actions) {
                        lastSyncVersion = Math.max(lastSyncVersion, data.currentVersion || 0);
                        let hasChanges = false;
                        for (const item of data.actions) {
                            if (!seenActionIds.has(item.id)) {
                                seenActionIds.add(item.id);
                                if (seenActionIds.size > 1000) {
                                    const first = seenActionIds.values().next().value;
                                    seenActionIds.delete(first);
                                }
                                handleRemoteAction(item.action);
                                hasChanges = true;
                            }
                        }
                    }
                })
                .catch(() => {});
        }
        setInterval(pollDeltaSync, 1200);

        function handleRemoteAction(action) {
            if (!action) return;
            if (action.senderId === clientId) return;

            if (action.type === 'snapshot') {
                elements = {};
                if (action.document && action.document.elements) {
                    for (const el of action.document.elements) {
                        elements[el.id] = el;
                    }
                }
                render();
            } else if (action.type === 'upsert_element') {
                if (action.element) {
                    elements[action.element.id] = action.element;
                    render();
                }
            } else if (action.type === 'delete_elements') {
                if (action.elementIds) {
                    for (const id of action.elementIds) {
                        delete elements[id];
                    }
                    render();
                }
            } else if (action.type === 'pointer_move') {
                if (action.senderId !== clientId) {
                    remotePointers[action.senderId] = {
                        peerName: action.senderName,
                        x: action.x,
                        y: action.y,
                        colorHex: action.colorHex,
                        isLaser: action.isLaser,
                        lastUpdated: Date.now()
                    };
                    render();
                }
            } else if (action.type === 'toggle_sticky_todo') {
                const note = elements[action.noteId];
                if (note && note.text) {
                    const lines = note.text.split('\n');
                    const idx = action.lineIndex;
                    if (idx >= 0 && idx < lines.length) {
                        if (lines[idx].startsWith('- [ ] ')) {
                            lines[idx] = '- [x] ' + lines[idx].substring(6);
                        } else if (lines[idx].startsWith('- [x] ') || lines[idx].startsWith('- [X] ')) {
                            lines[idx] = '- [ ] ' + lines[idx].substring(6);
                        }
                        note.text = lines.join('\n');
                        render();
                    }
                }
            }
        }

        // Initial snapshot
        fetch('/api/snapshot')
            .then(res => res.json())
            .then(doc => {
                if (doc && doc.elements) {
                    for (const el of doc.elements) {
                        elements[el.id] = el;
                    }
                    render();
                }
            })
            .catch(() => {});

        // Connect Realtime
        connectLiveStream();
    </script>
</body>
</html>
        """.trimIndent()
    }
}
