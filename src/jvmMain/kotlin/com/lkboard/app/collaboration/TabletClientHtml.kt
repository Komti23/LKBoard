package com.lkboard.app.collaboration

/**
 * Генератор мобильного веб-приложения для режима графического планшета (Phone Stylus Digitizer).
 * Превращает любой смартфон или планшет в персональный беспроводной графический планшет:
 * - Исключительно длинные персональные ссылки с токенами (?id=...&name=...&color=...)
 * - Виджет 3 последних действий с возможностью мгновенной отмены прямо с экрана телефона
 * - Режим «Лазерная указка» (🔴) со смартфона для презентаций и объяснений
 * - Режим «Линейка / Прямая линия» (📏) для чертежей
 * - Тактильный Haptic Bumper при приближении к границам монитора
 * - 1:1 Пропорции монитора ПК (Active Digitizer Frame) — окружности без сплющивания
 * - Двухпальцевое перемещение (2-Finger Pan) холста ПК с телефона
 * - Сворачиваемая панель инструментов и плавающие кнопки под большой палец
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
            --bg-color: #0b0d11;
            --surface-color: rgba(20, 24, 32, 0.92);
            --border-color: rgba(255, 255, 255, 0.12);
            --accent: #2563eb;
            --accent-glow: rgba(37, 99, 235, 0.35);
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
            position: absolute;
            top: 0;
            left: 0;
            z-index: 10;
        }

        /* Активная рамка пропорций монитора ПК (1:1 Digitizer Pad) */
        #digitizer-frame {
            position: absolute;
            pointer-events: none;
            border: 1.5px dashed rgba(56, 189, 248, 0.40);
            border-radius: 14px;
            box-shadow: 0 0 24px rgba(56, 189, 248, 0.08);
            z-index: 20;
            transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
        }

        .digitizer-matte {
            position: absolute;
            background: rgba(0, 0, 0, 0.42);
            pointer-events: none;
            z-index: 15;
            backdrop-filter: blur(1px);
        }

        .digitizer-badge {
            position: absolute;
            top: -24px;
            left: 50%;
            transform: translateX(-50%);
            background: rgba(15, 23, 42, 0.85);
            border: 1px solid rgba(56, 189, 248, 0.35);
            color: #38bdf8;
            font-size: 10px;
            font-weight: 700;
            padding: 2px 8px;
            border-radius: 6px;
            letter-spacing: 0.5px;
            white-space: nowrap;
        }

        .corner-tag {
            position: absolute;
            color: rgba(56, 189, 248, 0.55);
            font-size: 14px;
            font-family: monospace;
            font-weight: 900;
            line-height: 1;
        }
        .corner-tag.tl { top: 4px; left: 4px; }
        .corner-tag.tr { top: 4px; right: 4px; }
        .corner-tag.bl { bottom: 4px; left: 4px; }
        .corner-tag.br { bottom: 4px; right: 4px; }

        /* Верхняя панель статуса */
        .top-bar {
            position: fixed;
            top: 10px;
            left: 10px;
            right: 10px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            z-index: 50;
            pointer-events: none;
        }

        .status-badge {
            background: var(--surface-color);
            backdrop-filter: blur(16px);
            -webkit-backdrop-filter: blur(16px);
            border: 1px solid var(--border-color);
            border-radius: 12px;
            padding: 6px 12px;
            font-size: 11px;
            font-weight: 600;
            display: flex;
            align-items: center;
            gap: 7px;
            pointer-events: auto;
            box-shadow: 0 4px 14px rgba(0,0,0,0.4);
        }

        .status-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: #10b981;
            box-shadow: 0 0 8px #10b981;
        }

        .top-actions {
            display: flex;
            align-items: center;
            gap: 6px;
            pointer-events: auto;
        }

        .top-btn {
            background: var(--surface-color);
            backdrop-filter: blur(16px);
            -webkit-backdrop-filter: blur(16px);
            border: 1px solid var(--border-color);
            color: var(--text-main);
            border-radius: 10px;
            padding: 6px 10px;
            font-size: 11px;
            font-weight: 600;
            cursor: pointer;
            display: flex;
            align-items: center;
            gap: 5px;
            box-shadow: 0 4px 14px rgba(0,0,0,0.4);
            transition: all 0.15s ease;
        }

        .top-btn:active {
            transform: scale(0.95);
            background: rgba(37, 99, 235, 0.25);
        }

        /* Виджет 3 последних действий */
        .actions-card {
            position: fixed;
            top: 48px;
            left: 10px;
            background: var(--surface-color);
            backdrop-filter: blur(16px);
            -webkit-backdrop-filter: blur(16px);
            border: 1px solid var(--border-color);
            border-radius: 12px;
            padding: 6px 10px;
            z-index: 45;
            box-shadow: 0 4px 18px rgba(0,0,0,0.45);
            min-width: 190px;
            max-width: 250px;
            pointer-events: auto;
            transition: all 0.2s ease;
        }

        .actions-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            font-size: 11px;
            font-weight: 700;
            color: #e2e8f0;
            cursor: pointer;
        }

        .actions-list {
            margin-top: 5px;
            padding-top: 5px;
            border-top: 1px solid rgba(255, 255, 255, 0.1);
            display: flex;
            flex-direction: column;
            gap: 4px;
        }

        .actions-list.collapsed {
            display: none;
        }

        .action-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 6px;
            font-size: 10px;
            color: #cbd5e1;
        }

        .action-btn-undo {
            background: rgba(255, 255, 255, 0.08);
            border: 1px solid rgba(255, 255, 255, 0.14);
            color: #38bdf8;
            border-radius: 5px;
            padding: 1px 5px;
            font-size: 9px;
            font-weight: bold;
            cursor: pointer;
        }

        .action-btn-undo:active {
            background: rgba(56, 189, 248, 0.3);
        }

        /* Подсказка поворота в портретном режиме */
        .rotate-hint {
            position: fixed;
            top: 48px;
            right: 10px;
            background: rgba(30, 41, 59, 0.95);
            border: 1px solid rgba(56, 189, 248, 0.4);
            color: #e2e8f0;
            padding: 6px 12px;
            border-radius: 12px;
            font-size: 10px;
            font-weight: 600;
            display: none;
            align-items: center;
            gap: 6px;
            z-index: 44;
            box-shadow: 0 8px 20px rgba(0,0,0,0.5);
        }

        /* Плавающий тост-уведомление */
        .toast-pill {
            position: fixed;
            top: 86px;
            left: 50%;
            transform: translateX(-50%);
            background: rgba(15, 23, 42, 0.94);
            border: 1px solid rgba(255, 255, 255, 0.16);
            color: #ffffff;
            padding: 8px 16px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 600;
            z-index: 99;
            pointer-events: none;
            opacity: 0;
            transition: opacity 0.2s ease, transform 0.2s ease;
            box-shadow: 0 8px 24px rgba(0,0,0,0.6);
        }

        .toast-pill.show {
            opacity: 1;
            transform: translateX(-50%) translateY(4px);
        }

        /* Кнопки быстрого действия под большой палец (Thumb Controls) */
        .thumb-controls {
            position: fixed;
            right: 12px;
            bottom: 74px;
            display: flex;
            flex-direction: column;
            gap: 10px;
            z-index: 45;
            pointer-events: auto;
        }

        .thumb-btn {
            width: 44px;
            height: 44px;
            border-radius: 14px;
            background: var(--surface-color);
            backdrop-filter: blur(16px);
            -webkit-backdrop-filter: blur(16px);
            border: 1px solid var(--border-color);
            color: #ffffff;
            font-size: 19px;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            box-shadow: 0 8px 20px rgba(0, 0, 0, 0.5);
            transition: transform 0.1s ease, background 0.15s ease;
        }

        .thumb-btn:active {
            transform: scale(0.92);
            background: rgba(37, 99, 235, 0.35);
        }

        /* Нижняя панель инструментов */
        .toolbar-pill {
            position: fixed;
            bottom: 14px;
            left: 50%;
            transform: translateX(-50%);
            background: var(--surface-color);
            backdrop-filter: blur(20px);
            -webkit-backdrop-filter: blur(20px);
            border: 1px solid var(--border-color);
            border-radius: 20px;
            padding: 6px 12px;
            display: flex;
            align-items: center;
            gap: 7px;
            z-index: 50;
            box-shadow: 0 12px 32px rgba(0, 0, 0, 0.55);
            max-width: 96vw;
            transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.25s ease;
        }

        .toolbar-pill.collapsed {
            transform: translateX(-50%) translateY(85px);
            opacity: 0;
            pointer-events: none;
        }

        .btn-mini-restore {
            position: fixed;
            bottom: 14px;
            left: 14px;
            background: var(--surface-color);
            backdrop-filter: blur(16px);
            border: 1px solid var(--border-color);
            color: #38bdf8;
            border-radius: 12px;
            padding: 8px 12px;
            font-size: 12px;
            font-weight: 700;
            display: none;
            align-items: center;
            gap: 6px;
            cursor: pointer;
            z-index: 50;
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
        }

        .tool-btn {
            background: transparent;
            border: 1px solid transparent;
            color: var(--text-muted);
            width: 36px;
            height: 36px;
            border-radius: 10px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 17px;
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
            width: 20px;
            height: 20px;
            border-radius: 50%;
            border: 2px solid transparent;
            cursor: pointer;
            flex-shrink: 0;
            transition: transform 0.15s ease, border-color 0.15s ease;
        }

        .color-dot.active {
            border-color: #ffffff;
            transform: scale(1.25);
            box-shadow: 0 0 10px rgba(255, 255, 255, 0.45);
        }

        .divider {
            width: 1px;
            height: 20px;
            background: rgba(255, 255, 255, 0.15);
            flex-shrink: 0;
        }

        /* Полноэкранный модальный оверлей */
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
            transition: opacity 0.25s ease;
        }

        .fs-overlay.hidden {
            opacity: 0;
            pointer-events: none;
            display: none !important;
        }

        .fs-card {
            background: #181b20;
            border: 1px solid rgba(255, 255, 255, 0.15);
            border-radius: 24px;
            padding: 26px 20px;
            max-width: 420px;
            width: 100%;
            text-align: center;
            box-shadow: 0 24px 48px rgba(0, 0, 0, 0.6);
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
        }

        .fs-icon {
            font-size: 44px;
            margin-bottom: 10px;
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
            margin-bottom: 16px;
        }

        .student-name-box {
            background: rgba(255, 255, 255, 0.06);
            border: 1px solid rgba(255, 255, 255, 0.14);
            border-radius: 12px;
            padding: 10px 14px;
            margin-bottom: 16px;
            text-align: left;
        }

        .student-name-label {
            font-size: 11px;
            color: #94a3b8;
            font-weight: 600;
            margin-bottom: 6px;
            display: block;
        }

        .student-name-input {
            width: 100%;
            background: transparent;
            border: none;
            outline: none;
            color: #ffffff;
            font-size: 15px;
            font-weight: 700;
        }

        .btn-fullscreen {
            background: linear-gradient(135deg, #2563eb, #1d4ed8);
            color: #ffffff;
            border: none;
            padding: 15px 20px;
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
            transition: transform 0.1s ease;
        }

        .btn-fullscreen:active {
            transform: scale(0.97);
        }

        .btn-fullscreen-skip {
            background: transparent;
            color: var(--text-muted);
            border: 1px solid rgba(255, 255, 255, 0.12);
            padding: 11px 18px;
            border-radius: 12px;
            font-size: 13px;
            font-weight: 600;
            width: 100%;
            cursor: pointer;
            margin-top: 10px;
        }

        .fs-tip {
            margin-top: 14px;
            font-size: 11px;
            color: #64748b;
            line-height: 1.4;
        }
    </style>
</head>
<body>

    <!-- Рамка пропорций монитора ПК (1:1 Active Pad) -->
    <div id="matte-left" class="digitizer-matte"></div>
    <div id="matte-right" class="digitizer-matte"></div>
    <div id="matte-top" class="digitizer-matte"></div>
    <div id="matte-bottom" class="digitizer-matte"></div>

    <div id="digitizer-frame">
        <div class="digitizer-badge" id="frame-badge">📐 1:1 Монитор ПК</div>
        <div class="corner-tag tl">⌜</div>
        <div class="corner-tag tr">⌝</div>
        <div class="corner-tag bl">⌞</div>
        <div class="corner-tag br">⌟</div>
    </div>

    <!-- Основной холст ввода стилуса -->
    <canvas id="canvas"></canvas>

    <!-- Верхняя статусная панель -->
    <div class="top-bar">
        <div class="status-badge" id="status-badge">
            <div class="status-dot" id="status-dot"></div>
            <span id="status-name">📱 Планшет</span>
            <span style="color: #64748b;">·</span>
            <span id="aspect-label" style="color: #38bdf8;">1:1 монитор</span>
        </div>

        <div class="top-actions">
            <button class="top-btn" id="btn-toggle-aspect" type="button" title="Переключить режим пропорций">📐 1:1</button>
            <button class="top-btn" id="btn-re-fs" type="button" title="На весь экран">⛶ Экран</button>
            <button class="top-btn" id="btn-collapse-tb" type="button" title="Свернуть панель инструментов">▼ Меню</button>
        </div>
    </div>

    <!-- Виджет 3 последних действий -->
    <div class="actions-card" id="actions-card">
        <div class="actions-header" id="actions-header" title="Показать / скрыть последние действия">
            <span style="display:flex; align-items:center; gap:5px;">📋 3 действия</span>
            <span id="actions-arrow" style="font-size: 9px; color: #94a3b8;">▼</span>
        </div>
        <div class="actions-list" id="actions-list">
            <div id="actions-items-container">
                <div style="color: #64748b; font-size: 10px; padding: 2px 0;">Действий пока нет</div>
            </div>
        </div>
    </div>

    <!-- Подсказка поворота экрана -->
    <div class="rotate-hint" id="rotate-hint">
        <span>🔄 Поверните телефон горизонтально</span>
    </div>

    <!-- Всплывающий тост обратной связи -->
    <div class="toast-pill" id="toast-pill">✋ Перемещение доски...</div>

    <!-- Кнопки под большой палец (Thumb Quick Actions) -->
    <div class="thumb-controls">
        <button class="thumb-btn" id="btn-thumb-undo" type="button" title="Отменить штрих (Undo)">↩️</button>
        <button class="thumb-btn" id="btn-thumb-clear" type="button" title="Очистить холст">🗑️</button>
        <button class="thumb-btn" id="btn-thumb-toggle-tool" type="button" title="Быстрое перо / ластик">✏️</button>
    </div>

    <!-- Кнопка восстановления свернутого тулбара -->
    <button class="btn-mini-restore" id="btn-mini-restore" type="button">
        <span>✏️ Развернуть меню</span>
    </button>

    <!-- Нижняя панель инструментов -->
    <div class="toolbar-pill" id="toolbar">
        <button class="tool-btn active" id="tool-pen" title="Стилус / Ручка" type="button">🖊️</button>
        <button class="tool-btn" id="tool-highlighter" title="Маркер" type="button">🖍️</button>
        <button class="tool-btn" id="tool-laser" title="Лазерная указка" type="button">🔴</button>
        <button class="tool-btn" id="tool-line" title="Линейка / Прямая линия" type="button">📏</button>
        <button class="tool-btn" id="tool-eraser" title="Ластик" type="button">🧹</button>

        <div class="divider"></div>

        <!-- Выбор цвета пера -->
        <div class="color-dot active" data-color="0xFF2563EB" style="background: #2563eb;" title="Синий"></div>
        <div class="color-dot" data-color="0xFF16A34A" style="background: #16a34a;" title="Зеленый"></div>
        <div class="color-dot" data-color="0xFFDC2626" style="background: #dc2626;" title="Красный"></div>
        <div class="color-dot" data-color="0xFF9333EA" style="background: #9333ea;" title="Фиолетовый"></div>
        <div class="color-dot" data-color="0xFFEA580C" style="background: #ea580c;" title="Оранжевый"></div>
        <div class="color-dot" data-color="0xFFFFFFFF" style="background: #ffffff;" title="Белый"></div>

        <div class="divider"></div>

        <!-- Толщина и опции -->
        <button class="tool-btn" id="btn-width" title="Толщина пера" type="button" style="font-size: 13px; font-weight: bold;">4px</button>
        <button class="tool-btn" id="btn-palm" title="Защита от касаний ладони (Palm Rejection)" type="button">🖐️</button>
        <button class="tool-btn" id="btn-close-tb" title="Скрыть панель" type="button">✕</button>
    </div>

    <!-- Окно приветствия и перехода в полноэкранный режим -->
    <div class="fs-overlay" id="fs-overlay">
        <div class="fs-card" id="fs-card">
            <button class="fs-close-btn" id="btn-fs-close" title="Закрыть" type="button">✕</button>
            <div class="fs-icon">📱✍️</div>
            <h1 class="fs-title" id="welcome-title">Режим графического планшета</h1>
            <p class="fs-desc" id="welcome-desc">
                Телефон подключен к LKBoard на компьютере.<br>
                Пишите стилусом или пальцем — штрихи отображаются на мониторе в реальном времени!
            </p>

            <div class="student-name-box" id="student-name-box">
                <label class="student-name-label" for="input-student-name">Ваше имя / планшет на доске:</label>
                <input class="student-name-input" id="input-student-name" type="text" placeholder="Например: Ученик 1" maxlength="24">
            </div>

            <button class="btn-fullscreen" id="btn-fs-start" type="button">
                <span>⛶ Открыть на полный экран</span>
            </button>
            <button class="btn-fullscreen-skip" id="btn-fs-skip" type="button">
                <span>Начать писать без полного экрана →</span>
            </button>
            <div class="fs-tip">
                Совет: для идеального совпадения с монитором поверните телефон горизонтально.<br>
                2 пальца по экрану — скроллинг холста на ПК!
            </div>
        </div>
    </div>

    <script>
        (function() {
            // 1. Извлечение параметров индивидуальной ссылки (?id=...&name=...&color=...)
            const urlParams = new URLSearchParams(window.location.search);
            let studentId = urlParams.get('id') || ('tablet-' + Math.random().toString(36).substr(2, 6));
            let studentName = urlParams.get('name') || localStorage.getItem('lkboard_tablet_name') || 'Преподаватель';
            let currentColorHex = urlParams.get('color') ? Number(urlParams.get('color')) : 0xFF2563EB;
            let currentCssColor = '#' + (currentColorHex & 0x00FFFFFF).toString(16).padStart(6, '0');

            // Элементы интерфейса
            const canvas = document.getElementById('canvas');
            const ctx = canvas.getContext('2d');
            const digitizerFrame = document.getElementById('digitizer-frame');
            const matteLeft = document.getElementById('matte-left');
            const matteRight = document.getElementById('matte-right');
            const matteTop = document.getElementById('matte-top');
            const matteBottom = document.getElementById('matte-bottom');
            const overlay = document.getElementById('fs-overlay');
            const btnFsStart = document.getElementById('btn-fs-start');
            const btnFsSkip = document.getElementById('btn-fs-skip');
            const btnFsClose = document.getElementById('btn-fs-close');
            const btnReFs = document.getElementById('btn-re-fs');
            const statusName = document.getElementById('status-name');
            const statusDot = document.getElementById('status-dot');
            const inputStudentName = document.getElementById('input-student-name');
            const studentNameBox = document.getElementById('student-name-box');
            const welcomeDesc = document.getElementById('welcome-desc');
            const rotateHint = document.getElementById('rotate-hint');
            const toastPill = document.getElementById('toast-pill');
            const toolbar = document.getElementById('toolbar');
            const btnToggleAspect = document.getElementById('btn-toggle-aspect');
            const btnCollapseTb = document.getElementById('btn-collapse-tb');
            const btnCloseTb = document.getElementById('btn-close-tb');
            const btnMiniRestore = document.getElementById('btn-mini-restore');
            const btnWidth = document.getElementById('btn-width');
            const btnPalm = document.getElementById('btn-palm');
            const btnThumbUndo = document.getElementById('btn-thumb-undo');
            const btnThumbClear = document.getElementById('btn-thumb-clear');
            const btnThumbToggleTool = document.getElementById('btn-thumb-toggle-tool');

            // Виджет 3 последних действий
            const actionsHeader = document.getElementById('actions-header');
            const actionsList = document.getElementById('actions-list');
            const actionsArrow = document.getElementById('actions-arrow');
            const actionsContainer = document.getElementById('actions-items-container');
            let isActionsCollapsed = false;

            // Хранилище 3 последних действий на планшете
            let localRecentActions = [];

            function toggleActions() {
                isActionsCollapsed = !isActionsCollapsed;
                actionsList.classList.toggle('collapsed', isActionsCollapsed);
                actionsArrow.innerText = isActionsCollapsed ? '▶' : '▼';
            }
            if (actionsHeader) actionsHeader.addEventListener('click', toggleActions);

            function addRecentAction(title, icon, elementId) {
                const item = {
                    id: 'act-' + Date.now(),
                    title: title,
                    icon: icon,
                    elementId: elementId || null,
                    timestamp: Date.now()
                };
                localRecentActions.unshift(item);
                if (localRecentActions.length > 3) localRecentActions.pop();
                renderRecentActions();
            }

            function renderRecentActions() {
                if (!actionsContainer) return;
                if (localRecentActions.length === 0) {
                    actionsContainer.innerHTML = '<div style="color: #64748b; font-size: 10px; padding: 2px 0;">Действий пока нет</div>';
                    return;
                }
                const now = Date.now();
                actionsContainer.innerHTML = localRecentActions.map(act => {
                    const sec = Math.max(0, Math.round((now - act.timestamp) / 1000));
                    const timeStr = sec < 4 ? 'только что' : sec + 'с назад';
                    const undoBtn = act.elementId ? ('<button class="action-btn-undo" onclick="window.undoAction(\'' + act.elementId + '\')">↩</button>') : '';
                    return '<div class="action-row">' +
                        '<span>' + act.icon + ' ' + escapeHtml(act.title) + '</span>' +
                        '<div style="display:flex;align-items:center;gap:4px;">' +
                            '<span style="font-size:9px;color:#94a3b8;">' + timeStr + '</span>' +
                            undoBtn +
                        '</div>' +
                    '</div>';
                }).join('');
            }

            window.undoAction = async function(elementId) {
                if (!elementId) return;
                await sendAction({
                    type: 'delete_elements',
                    elementIds: [elementId],
                    senderId: studentId
                });
                strokesHistory = strokesHistory.filter(id => id !== elementId);
                localRecentActions = localRecentActions.filter(act => act.elementId !== elementId);
                addRecentAction('Отмена штриха', '↩️', null);
                haptic(20);
                showToast('↩️ Элемент отменен');
                ctx.clearRect(0, 0, canvas.width, canvas.height);
            };

            // Синхронизация 3 последних действий с сервера ПК
            async function fetchRecentActions() {
                try {
                    const res = await fetch('/api/recent-actions');
                    if (res.ok) {
                        const list = await res.json();
                        if (Array.isArray(list) && list.length > 0) {
                            // Если есть удаленные действия от других, дополняем ленту
                            for (const a of list) {
                                if (!localRecentActions.some(local => local.title === a.actionTitle && Math.abs(local.timestamp - a.timestamp) < 3000)) {
                                    localRecentActions.push({
                                        id: a.id,
                                        title: a.peerName + ': ' + a.actionTitle,
                                        icon: a.icon || '✏️',
                                        elementId: null,
                                        timestamp: a.timestamp
                                    });
                                }
                            }
                            localRecentActions.sort((x, y) => y.timestamp - x.timestamp);
                            localRecentActions = localRecentActions.slice(0, 3);
                            renderRecentActions();
                        }
                    }
                } catch (_) {}
            }
            setInterval(fetchRecentActions, 2000);

            // Инициализация имени и цвета
            if (urlParams.get('name')) {
                studentNameBox.style.display = 'none';
                welcomeDesc.innerHTML = 'Привет, <b>' + escapeHtml(studentName) + '</b>!<br>Ваш персональный планшет подключен к монитору LKBoard.';
            } else {
                inputStudentName.value = studentName;
                inputStudentName.addEventListener('change', () => {
                    studentName = inputStudentName.value.trim() || 'Преподаватель';
                    localStorage.setItem('lkboard_tablet_name', studentName);
                    updateStatusBadge();
                });
            }

            function updateStatusBadge() {
                statusName.innerText = '📱 ' + studentName;
                statusDot.style.background = currentCssColor;
                statusDot.style.boxShadow = '0 0 8px ' + currentCssColor;
            }
            updateStatusBadge();

            function escapeHtml(text) {
                return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
            }

            // Тактильный виброотклик
            function haptic(ms) {
                try {
                    if (navigator.vibrate) navigator.vibrate(ms || 12);
                } catch (_) {}
            }

            // Всплывающее уведомление
            let toastTimer = null;
            function showToast(text) {
                if (!toastPill) return;
                toastPill.innerText = text;
                toastPill.classList.add('show');
                clearTimeout(toastTimer);
                toastTimer = setTimeout(() => {
                    toastPill.classList.remove('show');
                }, 1600);
            }

            // Быстрый клик без задержки 300мс
            function addFastClickListener(element, handler) {
                if (!element) return;
                let touched = false;

                element.addEventListener('pointerdown', (e) => e.stopPropagation());
                element.addEventListener('touchstart', (e) => e.stopPropagation(), { passive: true });
                element.addEventListener('touchend', (e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    touched = true;
                    handler(e);
                    setTimeout(() => { touched = false; }, 350);
                }, { passive: false });
                element.addEventListener('click', (e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    if (!touched) handler(e);
                });
            }

            // Состояние инструментов
            let currentTool = 'pen'; // 'pen', 'highlighter', 'eraser', 'laser', 'line'
            let currentWidth = 4;
            const widths = [2, 4, 8, 14];
            let widthIndex = 1;
            let palmRejection = true;
            let isDrawing = false;
            let currentStrokePoints = [];
            let strokesHistory = [];
            let activePointerId = null;
            let lastPointerMoveTime = 0;
            let activeStrokeId = null;
            let lastLiveStreamTime = 0;
            let lastEdgeHapticTime = 0;

            // Режим пропорций: 'monitor' (1:1 геометрия монитора) или 'fill' (на весь экран)
            let aspectMode = localStorage.getItem('lkboard_aspect_mode') || 'monitor';
            let pcViewport = { panX: 0, panY: 0, scale: 1, width: 1920, height: 1080 };

            // Расчет активной зоны дигитайзера
            function getActiveBounds() {
                const screenW = window.innerWidth;
                const screenH = window.innerHeight;

                if (aspectMode === 'fill') {
                    return { left: 0, top: 0, width: screenW, height: screenH };
                }

                const pcAspect = (pcViewport.width > 50 && pcViewport.height > 50)
                    ? (pcViewport.width / pcViewport.height)
                    : (16 / 9);

                const screenAspect = screenW / screenH;
                const margin = 10;
                const maxW = screenW - margin * 2;
                const maxH = screenH - margin * 2;

                let w, h;
                if (screenAspect > pcAspect) {
                    h = maxH;
                    w = h * pcAspect;
                } else {
                    w = maxW;
                    h = w / pcAspect;
                }

                const left = (screenW - w) / 2;
                const top = (screenH - h) / 2;
                return { left, top, width: w, height: h };
            }

            // Обновление рамки и затемнения краев
            function updateDigitizerFrame() {
                const b = getActiveBounds();
                const screenW = window.innerWidth;
                const screenH = window.innerHeight;

                if (aspectMode === 'fill') {
                    digitizerFrame.style.display = 'none';
                    matteLeft.style.display = 'none';
                    matteRight.style.display = 'none';
                    matteTop.style.display = 'none';
                    matteBottom.style.display = 'none';
                    document.getElementById('aspect-label').innerText = 'весь экран';
                    btnToggleAspect.innerText = '📱 Экран';
                    return;
                }

                digitizerFrame.style.display = 'block';
                digitizerFrame.style.left = b.left + 'px';
                digitizerFrame.style.top = b.top + 'px';
                digitizerFrame.style.width = b.width + 'px';
                digitizerFrame.style.height = b.height + 'px';

                // Затемнение краев экрана (letterboxing)
                matteLeft.style.display = 'block';
                matteLeft.style.left = '0px';
                matteLeft.style.top = '0px';
                matteLeft.style.width = b.left + 'px';
                matteLeft.style.height = screenH + 'px';

                matteRight.style.display = 'block';
                matteRight.style.left = (b.left + b.width) + 'px';
                matteRight.style.top = '0px';
                matteRight.style.width = Math.max(0, screenW - (b.left + b.width)) + 'px';
                matteRight.style.height = screenH + 'px';

                matteTop.style.display = 'block';
                matteTop.style.left = b.left + 'px';
                matteTop.style.top = '0px';
                matteTop.style.width = b.width + 'px';
                matteTop.style.height = b.top + 'px';

                matteBottom.style.display = 'block';
                matteBottom.style.left = b.left + 'px';
                matteBottom.style.top = (b.top + b.height) + 'px';
                matteBottom.style.width = b.width + 'px';
                matteBottom.style.height = Math.max(0, screenH - (b.top + b.height)) + 'px';

                document.getElementById('aspect-label').innerText = '1:1 монитор';
                btnToggleAspect.innerText = '📐 1:1';
            }

            // Переключение режима пропорций
            function toggleAspectMode() {
                aspectMode = (aspectMode === 'monitor') ? 'fill' : 'monitor';
                localStorage.setItem('lkboard_aspect_mode', aspectMode);
                updateDigitizerFrame();
                haptic(15);
                showToast(aspectMode === 'monitor' ? '📐 1:1 монитор (без искажения)' : '📱 На весь экран');
            }
            addFastClickListener(btnToggleAspect, toggleAspectMode);

            // Настройка Canvas под ретину и экран
            function resizeCanvas() {
                const dpr = window.devicePixelRatio || 1;
                canvas.width = window.innerWidth * dpr;
                canvas.height = window.innerHeight * dpr;
                ctx.scale(dpr, dpr);
                updateDigitizerFrame();

                const isPortrait = window.innerHeight > window.innerWidth;
                if (rotateHint) {
                    rotateHint.style.display = isPortrait ? 'flex' : 'none';
                }
            }
            window.addEventListener('resize', resizeCanvas);
            window.addEventListener('orientationchange', () => setTimeout(resizeCanvas, 200));
            resizeCanvas();

            // Полноэкранный режим
            function dismissOverlay() {
                overlay.classList.add('hidden');
                overlay.style.display = 'none';
                resizeCanvas();
            }

            function enterFullscreen() {
                dismissOverlay();
                const el = document.documentElement;
                const rfs = el.requestFullscreen || el.webkitRequestFullscreen || el.mozRequestFullScreen || el.msRequestFullscreen;
                if (rfs) {
                    try {
                        const p = rfs.call(el);
                        if (p && typeof p.catch === 'function') p.catch(() => {});
                    } catch (_) {}
                }
                try { window.scrollTo(0, 1); } catch (_) {}
                try {
                    if (screen.orientation && screen.orientation.lock) {
                        screen.orientation.lock('landscape').catch(() => {});
                    }
                } catch (_) {}
                setTimeout(resizeCanvas, 200);
            }

            addFastClickListener(btnFsStart, enterFullscreen);
            addFastClickListener(btnFsSkip, dismissOverlay);
            addFastClickListener(btnFsClose, dismissOverlay);
            addFastClickListener(btnReFs, enterFullscreen);

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

            // Получение геометрии монитора ПК
            async function fetchViewport() {
                try {
                    const res = await fetch('/api/viewport');
                    if (res.ok) {
                        const vp = await res.json();
                        if (vp && vp.width > 50 && vp.height > 50) {
                            pcViewport = vp;
                            updateDigitizerFrame();
                        }
                    }
                } catch (_) {}
            }
            fetchViewport();
            setInterval(fetchViewport, 1000);

            // Пересчет экранных координат планшета в координаты доски ПК с сохранением 1:1 пропорций
            function phoneToWorld(px, py) {
                const b = getActiveBounds();

                // Тактильный бампер границы: если стилус касается края активной зоны
                const now = performance.now();
                if (now - lastEdgeHapticTime > 350) {
                    const edgeDist = 12;
                    if (Math.abs(px - b.left) < edgeDist || Math.abs(px - (b.left + b.width)) < edgeDist ||
                        Math.abs(py - b.top) < edgeDist || Math.abs(py - (b.top + b.height)) < edgeDist) {
                        lastEdgeHapticTime = now;
                        haptic(22);
                    }
                }

                const worldLeft = (0 - pcViewport.panX) / pcViewport.scale;
                const worldTop = (0 - pcViewport.panY) / pcViewport.scale;
                const worldRight = (pcViewport.width - pcViewport.panX) / pcViewport.scale;
                const worldBottom = (pcViewport.height - pcViewport.panY) / pcViewport.scale;
                const worldW = worldRight - worldLeft;
                const worldH = worldBottom - worldTop;

                const nx = Math.max(0, Math.min(1, (px - b.left) / b.width));
                const ny = Math.max(0, Math.min(1, (py - b.top) / b.height));

                return {
                    x: worldLeft + nx * worldW,
                    y: worldTop + ny * worldH
                };
            }

            // Отправка действий на ПК
            async function sendAction(actionObj) {
                try {
                    await fetch('/api/action', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(actionObj)
                    });
                } catch (_) {}
            }

            // Отправка движения курсора пера / лазера
            function streamPointerMove(worldPos, isLaser) {
                const now = performance.now();
                if (now - lastPointerMoveTime < 24) return;
                lastPointerMoveTime = now;

                sendAction({
                    type: 'pointer_move',
                    senderId: studentId,
                    senderName: studentName,
                    x: worldPos.x,
                    y: worldPos.y,
                    colorHex: currentColorHex,
                    isLaser: !!isLaser
                });
            }

            // Потоковая отправка живого штриха в реальном времени
            async function streamLiveStroke(isFinal) {
                if (currentTool === 'laser') return;
                if (!isDrawing || currentStrokePoints.length < 2) return;
                const now = performance.now();
                if (!isFinal && now - lastLiveStreamTime < 30) return;
                lastLiveStreamTime = now;

                if (!activeStrokeId) {
                    activeStrokeId = studentId + '-' + Date.now() + '-' + Math.random().toString(36).substr(2, 4);
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
                    points: (currentTool === 'line' && currentStrokePoints.length >= 2)
                        ? [currentStrokePoints[0], currentStrokePoints[currentStrokePoints.length - 1]]
                        : currentStrokePoints.slice(),
                    strokeColorHex: currentColorHex,
                    strokeWidth: strokeWidth,
                    isHighlighter: isHighlighter
                };

                await sendAction({
                    type: 'upsert_element',
                    element: drawingPath,
                    senderId: studentId,
                    senderName: studentName
                });
            }

            // ДВУХПАЛЬЦЕВОЕ ПЕРЕМЕЩЕНИЕ ХОЛСТА (2-Finger Pan)
            let isTwoFingerPan = false;
            let prevMidX = 0, prevMidY = 0;

            canvas.addEventListener('touchstart', (e) => {
                if (e.touches.length === 2) {
                    isTwoFingerPan = true;
                    if (isDrawing) {
                        finishStroke();
                    }
                    prevMidX = (e.touches[0].clientX + e.touches[1].clientX) / 2;
                    prevMidY = (e.touches[0].clientY + e.touches[1].clientY) / 2;
                    haptic(15);
                    showToast('✋ Перемещение доски');
                    addRecentAction('Сдвиг холста', '🖐️', null);
                }
            }, { passive: false });

            canvas.addEventListener('touchmove', (e) => {
                if (e.touches.length === 2 && isTwoFingerPan) {
                    e.preventDefault();
                    const currX = (e.touches[0].clientX + e.touches[1].clientX) / 2;
                    const currY = (e.touches[0].clientY + e.touches[1].clientY) / 2;
                    const deltaPxX = currX - prevMidX;
                    const deltaPxY = currY - prevMidY;
                    prevMidX = currX;
                    prevMidY = currY;

                    const b = getActiveBounds();
                    const worldW = pcViewport.width / pcViewport.scale;
                    const worldH = pcViewport.height / pcViewport.scale;
                    const worldDx = deltaPxX * (worldW / b.width);
                    const worldDy = deltaPxY * (worldH / b.height);

                    sendAction({
                        type: 'pan_viewport',
                        dx: -worldDx,
                        dy: -worldDy,
                        senderId: studentId
                    });
                }
            }, { passive: false });

            canvas.addEventListener('touchend', (e) => {
                if (e.touches.length < 2) {
                    isTwoFingerPan = false;
                }
            }, { passive: true });

            // ОБРАБОТКА ВВОДА СТИЛУСА / ПАЛЬЦА
            let lineStartPx = { x: 0, y: 0 };

            canvas.addEventListener('pointerdown', (e) => {
                if (isTwoFingerPan || activePointerId !== null) return;

                // Защита от касания ладонью (Palm Rejection)
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

                lineStartPx = { x: px, y: py };
                activeStrokeId = studentId + '-' + Date.now() + '-' + Math.random().toString(36).substr(2, 4);
                const world = phoneToWorld(px, py);
                currentStrokePoints = [{ x: world.x, y: world.y }];

                ctx.beginPath();
                ctx.lineCap = 'round';
                ctx.lineJoin = 'round';
                ctx.strokeStyle = currentTool === 'highlighter' ? currentCssColor + '70' : currentCssColor;
                ctx.lineWidth = currentWidth * (0.5 + pressure * 0.8);
                ctx.moveTo(px, py);

                if (currentTool === 'laser') {
                    streamPointerMove(world, true);
                    ctx.fillStyle = '#ff1744';
                    ctx.beginPath();
                    ctx.arc(px, py, 6, 0, Math.PI * 2);
                    ctx.fill();
                } else {
                    streamPointerMove(world, false);
                }
            });

            canvas.addEventListener('pointermove', (e) => {
                if (!isDrawing || e.pointerId !== activePointerId || isTwoFingerPan) return;

                const rect = canvas.getBoundingClientRect();
                const events = (e.getCoalescedEvents && e.getCoalescedEvents().length > 0)
                    ? e.getCoalescedEvents()
                    : [e];

                for (const ev of events) {
                    const px = ev.clientX - rect.left;
                    const py = ev.clientY - rect.top;
                    const pressure = ev.pressure > 0 ? ev.pressure : 0.5;
                    const world = phoneToWorld(px, py);

                    if (currentTool === 'laser') {
                        streamPointerMove(world, true);
                        ctx.clearRect(0, 0, canvas.width, canvas.height);
                        ctx.fillStyle = '#ff1744';
                        ctx.beginPath();
                        ctx.arc(px, py, 7, 0, Math.PI * 2);
                        ctx.fill();
                        continue;
                    }

                    if (currentTool === 'line') {
                        ctx.clearRect(0, 0, canvas.width, canvas.height);
                        ctx.beginPath();
                        ctx.strokeStyle = currentCssColor;
                        ctx.lineWidth = currentWidth;
                        ctx.moveTo(lineStartPx.x, lineStartPx.y);
                        ctx.lineTo(px, py);
                        ctx.stroke();
                        currentStrokePoints = [{ x: phoneToWorld(lineStartPx.x, lineStartPx.y).x, y: phoneToWorld(lineStartPx.x, lineStartPx.y).y }, { x: world.x, y: world.y }];
                        streamPointerMove(world, false);
                        continue;
                    }

                    currentStrokePoints.push({ x: world.x, y: world.y });
                    ctx.lineWidth = currentWidth * (0.5 + pressure * 0.8);
                    ctx.lineTo(px, py);
                    ctx.stroke();

                    streamPointerMove(world, false);
                    streamLiveStroke(false);
                }
            });

            async function finishStroke() {
                if (!isDrawing) return;
                isDrawing = false;
                activePointerId = null;

                if (currentTool === 'laser') {
                    ctx.clearRect(0, 0, canvas.width, canvas.height);
                    return;
                }

                if (currentStrokePoints.length >= 2) {
                    await streamLiveStroke(true);
                    if (activeStrokeId) {
                        strokesHistory.push(activeStrokeId);
                        const actTitle = (currentTool === 'line') ? 'Линейка (линия)' : ('Штрих (' + currentWidth + 'px)');
                        const actIcon = (currentTool === 'line') ? '📏' : (currentTool === 'highlighter' ? '🖍️' : '✏️');
                        addRecentAction(actTitle, actIcon, activeStrokeId);
                    }
                }

                activeStrokeId = null;
                currentStrokePoints = [];

                setTimeout(() => {
                    ctx.clearRect(0, 0, canvas.width, canvas.height);
                }, 200);
            }

            canvas.addEventListener('pointerup', finishStroke);
            canvas.addEventListener('pointercancel', finishStroke);

            // СВОРАЧИВАНИЕ И РАЗВОРАЧИВАНИЕ ПАНЕЛИ
            let isToolbarCollapsed = false;
            function setToolbarCollapsed(collapsed) {
                isToolbarCollapsed = collapsed;
                toolbar.classList.toggle('collapsed', isToolbarCollapsed);
                btnMiniRestore.style.display = isToolbarCollapsed ? 'flex' : 'none';
                haptic(10);
            }

            addFastClickListener(btnCollapseTb, () => setToolbarCollapsed(true));
            addFastClickListener(btnCloseTb, () => setToolbarCollapsed(true));
            addFastClickListener(btnMiniRestore, () => setToolbarCollapsed(false));

            // ПЕРЕКЛЮЧЕНИЕ ИНСТРУМЕНТОВ
            function selectTool(toolName) {
                currentTool = toolName;
                document.querySelectorAll('.tool-btn').forEach(b => b.classList.remove('active'));
                const el = document.getElementById('tool-' + toolName);
                if (el) el.classList.add('active');
                haptic(12);

                if (toolName === 'laser') showToast('🔴 Лазерная указка активна');
                else if (toolName === 'line') showToast('📏 Режим линейки (прямая)');
                else if (toolName === 'highlighter') showToast('🖍️ Маркер');
                else if (toolName === 'eraser') showToast('🧹 Ластик');
                else showToast('🖊️ Перо');
            }

            addFastClickListener(document.getElementById('tool-pen'), () => selectTool('pen'));
            addFastClickListener(document.getElementById('tool-highlighter'), () => selectTool('highlighter'));
            addFastClickListener(document.getElementById('tool-laser'), () => selectTool('laser'));
            addFastClickListener(document.getElementById('tool-line'), () => selectTool('line'));
            addFastClickListener(document.getElementById('tool-eraser'), () => selectTool('eraser'));

            // Быстрый переключатель под большой палец
            addFastClickListener(btnThumbToggleTool, () => {
                if (currentTool === 'eraser') {
                    selectTool('pen');
                    btnThumbToggleTool.innerText = '✏️';
                    showToast('✏️ Перо активно');
                } else {
                    selectTool('eraser');
                    btnThumbToggleTool.innerText = '🧹';
                    showToast('🧹 Ластик активен');
                }
            });

            // Выбор цвета
            document.querySelectorAll('.color-dot').forEach(dot => {
                addFastClickListener(dot, () => {
                    document.querySelectorAll('.color-dot').forEach(d => d.classList.remove('active'));
                    dot.classList.add('active');
                    currentColorHex = Number(dot.getAttribute('data-color')) || 0xFF2563EB;
                    currentCssColor = dot.style.background;
                    updateStatusBadge();
                    haptic(10);
                });
            });

            // Толщина пера
            addFastClickListener(btnWidth, () => {
                widthIndex = (widthIndex + 1) % widths.length;
                currentWidth = widths[widthIndex];
                btnWidth.innerText = currentWidth + 'px';
                haptic(10);
            });

            // Palm Rejection
            addFastClickListener(btnPalm, () => {
                palmRejection = !palmRejection;
                btnPalm.style.opacity = palmRejection ? '1.0' : '0.4';
                haptic(10);
                showToast(palmRejection ? '🖐️ Защита ладони включена' : '🖐️ Защита ладони отключена');
            });

            // Отмена последнего штриха (Undo)
            async function triggerUndo() {
                if (strokesHistory.length === 0) return;
                const lastId = strokesHistory.pop();
                await sendAction({
                    type: 'delete_elements',
                    elementIds: [lastId],
                    senderId: studentId
                });
                addRecentAction('Отмена последнего штриха', '↩️', null);
                haptic(20);
                showToast('↩️ Штрих отменен');
                ctx.clearRect(0, 0, canvas.width, canvas.height);
            }
            addFastClickListener(btnThumbUndo, triggerUndo);

            // Очистка планшета
            addFastClickListener(btnThumbClear, () => {
                ctx.clearRect(0, 0, canvas.width, canvas.height);
                haptic(20);
                showToast('🗑️ Экран очищен');
                addRecentAction('Очистка экрана', '🗑️', null);
            });

            // Уведомление ПК о подключении участника
            sendAction({
                type: 'peer_joined',
                peer: {
                    id: studentId,
                    name: studentName,
                    colorHex: currentColorHex,
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
