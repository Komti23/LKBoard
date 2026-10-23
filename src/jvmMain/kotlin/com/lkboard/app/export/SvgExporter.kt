package com.lkboard.app.export

import com.lkboard.app.model.BoardElement
import com.lkboard.app.model.BoardState
import com.lkboard.app.model.ConnectorStyle
import com.lkboard.app.model.GeometryUtils
import com.lkboard.app.model.ShapeType
import com.lkboard.app.model.getAnchorOffset
import com.lkboard.app.util.MathEvaluator
import java.io.File

/**
 * Сервис экспорта доски Lkboard в чистый масштабируемый векторный формат SVG.
 */
object SvgExporter {

    fun exportToSvg(file: File, state: BoardState) {
        val elements = state.sortedElements
        val bounds = GeometryUtils.computeBounds(elements)
            ?: androidx.compose.ui.geometry.Rect(0f, 0f, 1920f, 1080f)

        val padding = 60f
        val viewBoxX = bounds.left - padding
        val viewBoxY = bounds.top - padding
        val viewBoxW = bounds.width + padding * 2
        val viewBoxH = bounds.height + padding * 2

        val bgHex = String.format("#%06X", state.backgroundColorHex and 0xFFFFFFL)

        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="$viewBoxX $viewBoxY $viewBoxW $viewBoxH" width="$viewBoxW" height="$viewBoxH">""")
        sb.appendLine("""  <defs>""")
        sb.appendLine("""    <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">""")
        sb.appendLine("""      <polygon points="0 0, 10 3.5, 0 7" fill="#2563EB" />""")
        sb.appendLine("""    </marker>""")
        sb.appendLine("""  </defs>""")
        sb.appendLine("""  <!-- Фон холста -->""")
        sb.appendLine("""  <rect x="$viewBoxX" y="$viewBoxY" width="$viewBoxW" height="$viewBoxH" fill="$bgHex" />""")

        for (el in elements) {
            when (el) {
                is BoardElement.StickyNote -> {
                    val fill = String.format("#%06X", el.colorHex and 0xFFFFFFL)
                    sb.appendLine("""  <g id="${el.id}">""")
                    sb.appendLine("""    <rect x="${el.x}" y="${el.y}" width="${el.width}" height="${el.height}" rx="10" ry="10" fill="$fill" stroke="#E2E8F0" stroke-width="1"/>""")
                    val escaped = escapeXml(el.text)
                    val lines = escaped.split("\n")
                    var ty = el.y + 24f
                    for (line in lines) {
                        sb.appendLine("""    <text x="${el.x + 12f}" y="$ty" font-family="sans-serif" font-size="${el.fontSize}" fill="#1E293B">$line</text>""")
                        ty += el.fontSize * 1.3f
                    }
                    sb.appendLine("""  </g>""")
                }

                is BoardElement.Shape -> {
                    val stroke = String.format("#%06X", el.strokeColorHex and 0xFFFFFFL)
                    val fill = if (el.fillColorHex != null && el.fillColorHex != 0L) {
                        val colorStr = String.format("#%06X", el.fillColorHex and 0xFFFFFFL)
                        val alpha = ((el.fillColorHex shr 24) and 0xFFL) / 255.0
                        """fill="$colorStr" fill-opacity="$alpha""""
                    } else """fill="none""""

                    when (el.shapeType) {
                        ShapeType.ELLIPSE -> {
                            val cx = el.x + el.width / 2f
                            val cy = el.y + el.height / 2f
                            val rx = el.width / 2f
                            val ry = el.height / 2f
                            sb.appendLine("""  <ellipse cx="$cx" cy="$cy" rx="$rx" ry="$ry" stroke="$stroke" stroke-width="${el.strokeWidth}" $fill />""")
                        }
                        ShapeType.ROUNDED_RECTANGLE -> {
                            sb.appendLine("""  <rect x="${el.x}" y="${el.y}" width="${el.width}" height="${el.height}" rx="${el.cornerRadius}" ry="${el.cornerRadius}" stroke="$stroke" stroke-width="${el.strokeWidth}" $fill />""")
                        }
                        else -> {
                            sb.appendLine("""  <rect x="${el.x}" y="${el.y}" width="${el.width}" height="${el.height}" stroke="$stroke" stroke-width="${el.strokeWidth}" $fill />""")
                        }
                    }
                }

                is BoardElement.Text -> {
                    val color = String.format("#%06X", el.colorHex and 0xFFFFFFL)
                    val escaped = escapeXml(el.text)
                    val lines = escaped.split("\n")
                    var ty = el.y + el.fontSize
                    sb.appendLine("""  <g id="${el.id}">""")
                    for (line in lines) {
                        val boldAttr = if (el.isBold) """font-weight="bold"""" else ""
                        sb.appendLine("""    <text x="${el.x}" y="$ty" font-family="sans-serif" font-size="${el.fontSize}" fill="$color" $boldAttr>$line</text>""")
                        ty += el.fontSize * 1.3f
                    }
                    sb.appendLine("""  </g>""")
                }

                is BoardElement.DrawingPath -> {
                    if (el.points.size >= 2) {
                        val stroke = String.format("#%06X", el.strokeColorHex and 0xFFFFFFL)
                        val d = StringBuilder("M ${el.points.first().x} ${el.points.first().y} ")
                        for (i in 1 until el.points.size) {
                            d.append("L ${el.points[i].x} ${el.points[i].y} ")
                        }
                        sb.appendLine("""  <path d="$d" fill="none" stroke="$stroke" stroke-width="${el.strokeWidth}" stroke-linecap="round" stroke-linejoin="round" />""")
                    }
                }

                is BoardElement.Connector -> {
                    val stroke = String.format("#%06X", el.strokeColorHex and 0xFFFFFFL)
                    val startOffset = el.startElementId?.let { id ->
                        state.elements[id]?.getAnchorOffset(el.startAnchor)
                    } ?: el.startPoint.toOffset()
                    val endOffset = el.endElementId?.let { id ->
                        state.elements[id]?.getAnchorOffset(el.endAnchor)
                    } ?: el.endPoint.toOffset()

                    val markerAttr = if (el.hasEndArrow) """marker-end="url(#arrowhead)"""" else ""
                    val d = when (el.style) {
                        ConnectorStyle.STRAIGHT -> "M ${startOffset.x} ${startOffset.y} L ${endOffset.x} ${endOffset.y}"
                        ConnectorStyle.ORTHOGONAL -> {
                            val midX = (startOffset.x + endOffset.x) / 2f
                            "M ${startOffset.x} ${startOffset.y} L $midX ${startOffset.y} L $midX ${endOffset.y} L ${endOffset.x} ${endOffset.y}"
                        }
                        ConnectorStyle.BEZIER -> {
                            val dx = (endOffset.x - startOffset.x) * 0.5f
                            "M ${startOffset.x} ${startOffset.y} C ${startOffset.x + dx} ${startOffset.y}, ${endOffset.x - dx} ${endOffset.y}, ${endOffset.x} ${endOffset.y}"
                        }
                    }
                    sb.appendLine("""  <path d="$d" fill="none" stroke="$stroke" stroke-width="${el.strokeWidth}" stroke-linecap="round" stroke-linejoin="round" $markerAttr />""")
                }

                is BoardElement.MilestoneCard -> {
                    val statusColor = String.format("#%06X", el.status.colorHex and 0xFFFFFFL)
                    sb.appendLine("""  <g id="${el.id}">""")
                    sb.appendLine("""    <rect x="${el.x}" y="${el.y}" width="${el.width}" height="${el.height}" rx="14" ry="14" fill="#FFFFFF" stroke="#E2E8F0" stroke-width="1.2"/>""")
                    sb.appendLine("""    <rect x="${el.x}" y="${el.y}" width="6" height="${el.height}" rx="3" ry="3" fill="$statusColor"/>""")
                    sb.appendLine("""    <text x="${el.x + 16f}" y="${el.y + 26f}" font-family="sans-serif" font-size="11" font-weight="bold" fill="$statusColor">${escapeXml(el.status.label)}</text>""")
                    sb.appendLine("""    <text x="${el.x + 16f}" y="${el.y + 54f}" font-family="sans-serif" font-size="14" font-weight="bold" fill="#0F172A">${escapeXml(el.title)}</text>""")
                    sb.appendLine("""    <text x="${el.x + 16f}" y="${el.y + 76f}" font-family="sans-serif" font-size="11" fill="#64748B">${escapeXml(el.description)}</text>""")
                    // Прогресс бар
                    val barW = el.width - 48f
                    sb.appendLine("""    <rect x="${el.x + 16f}" y="${el.y + el.height - 30f}" width="$barW" height="5" rx="2.5" fill="#E2E8F0"/>""")
                    val fillW = barW * el.progress.coerceIn(0f, 1f)
                    if (fillW > 0) {
                        sb.appendLine("""    <rect x="${el.x + 16f}" y="${el.y + el.height - 30f}" width="$fillW" height="5" rx="2.5" fill="$statusColor"/>""")
                    }
                    sb.appendLine("""  </g>""")
                }

                is BoardElement.FunctionGraph -> {
                    sb.appendLine("""  <g id="${el.id}">""")
                    sb.appendLine("""    <rect x="${el.x}" y="${el.y}" width="${el.width}" height="${el.height}" rx="12" ry="12" fill="#FFFFFF" stroke="#CBD5E1" stroke-width="1.2"/>""")
                    sb.appendLine("""    <text x="${el.x + 12f}" y="${el.y + 22f}" font-family="sans-serif" font-size="11" font-weight="bold" fill="#1E293B">${escapeXml(el.title)}: ${escapeXml(el.expressions.joinToString(", "))}</text>""")
                    // Сэмпл кривой
                    val pad = 12f
                    val plotL = el.x + pad
                    val plotT = el.y + 34f + pad
                    val plotW = el.width - pad * 2
                    val plotH = el.height - 34f - pad * 2
                    for (expr in el.expressions) {
                        val samples = MathEvaluator.sampleFunction(expr, el.xMin.toDouble(), el.xMax.toDouble(), 180)
                        val curveD = StringBuilder()
                        var first = true
                        for ((sx, sy) in samples) {
                            if (sy == null || sy.isNaN() || sy.isInfinite()) continue
                            val px = plotL + ((sx - el.xMin) / (el.xMax - el.xMin)) * plotW
                            val py = plotT + ((el.yMax - sy) / (el.yMax - el.yMin)) * plotH
                            if (first) {
                                curveD.append("M $px $py ")
                                first = false
                            } else {
                                curveD.append("L $px $py ")
                            }
                        }
                        if (curveD.isNotBlank()) {
                            sb.appendLine("""    <path d="$curveD" fill="none" stroke="#2563EB" stroke-width="2" stroke-linecap="round"/>""")
                        }
                    }
                    sb.appendLine("""  </g>""")
                }

                is BoardElement.KanbanBoard -> {
                    sb.appendLine("""  <g id="${el.id}">""")
                    sb.appendLine("""    <rect x="${el.x}" y="${el.y}" width="${el.width}" height="${el.height}" rx="16" ry="16" fill="#F8FAFC" stroke="#E2E8F0" stroke-width="1.5"/>""")
                    val headH = 44f
                    sb.appendLine("""    <rect x="${el.x}" y="${el.y}" width="${el.width}" height="$headH" rx="16" ry="16" fill="#F1F5F9"/>""")
                    sb.appendLine("""    <text x="${el.x + 18f}" y="${el.y + 28f}" font-family="sans-serif" font-size="15" font-weight="bold" fill="#0F172A">📋 ${escapeXml(el.title)}</text>""")
                    val cols = listOf("🔴 Бэклог", "🟡 В работе", "🟣 На ревью", "🟢 Готово")
                    val colW = el.width / 4f
                    for (i in 0 until 4) {
                        val cx = el.x + i * colW
                        if (i > 0) {
                            sb.appendLine("""    <line x1="$cx" y1="${el.y + headH}" x2="$cx" y2="${el.y + el.height}" stroke="#E2E8F0" stroke-width="1"/>""")
                        }
                        sb.appendLine("""    <text x="${cx + 14f}" y="${el.y + headH + 26f}" font-family="sans-serif" font-size="12" font-weight="bold" fill="#334155">${cols[i]}</text>""")
                    }
                    sb.appendLine("""  </g>""")
                }

                is BoardElement.DocumentFrame -> {
                    sb.appendLine("""  <g id="${el.id}">""")
                    sb.appendLine("""    <rect x="${el.x}" y="${el.y}" width="${el.width}" height="${el.height}" rx="10" ry="10" fill="#FFFFFF" stroke="#CBD5E1" stroke-width="1.2"/>""")
                    sb.appendLine("""    <rect x="${el.x}" y="${el.y}" width="${el.width}" height="38" rx="10" ry="10" fill="#F1F5F9"/>""")
                    sb.appendLine("""    <text x="${el.x + 14f}" y="${el.y + 24f}" font-family="sans-serif" font-size="13" font-weight="bold" fill="#1E293B">${escapeXml(el.title)}</text>""")
                    val pageTxt = if (el.totalPages > 0) "${el.currentPageIndex + 1} / ${el.totalPages}" else "Слайды"
                    sb.appendLine("""    <text x="${el.x + el.width - 80f}" y="${el.y + 24f}" font-family="sans-serif" font-size="12" fill="#0284C7">$pageTxt</text>""")
                    sb.appendLine("""  </g>""")
                }

                else -> {}
            }
        }

        sb.appendLine("""</svg>""")
        file.writeText(sb.toString(), Charsets.UTF_8)
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
