package com.streetlifting.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Mini gráfico de línea (sin dependencias externas). */
@Composable
fun LineChart(
    points: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxWidth().height(120.dp)) {
        if (points.isEmpty()) return@Canvas
        val minV = points.min()
        val maxV = points.max()
        val range = (maxV - minV).takeIf { it > 0f } ?: 1f
        val padX = 12f
        val padY = 12f
        val w = size.width - padX * 2
        val h = size.height - padY * 2

        fun x(i: Int): Float =
            if (points.size == 1) padX + w / 2
            else padX + w * i / (points.size - 1)

        fun y(v: Float): Float = padY + h * (1f - (v - minV) / range)

        // Baseline.
        drawLine(
            color = color.copy(alpha = 0.2f),
            start = Offset(padX, padY + h),
            end = Offset(padX + w, padY + h),
            strokeWidth = 2f,
        )

        if (points.size >= 2) {
            for (i in 0 until points.size - 1) {
                drawLine(
                    color = color,
                    start = Offset(x(i), y(points[i])),
                    end = Offset(x(i + 1), y(points[i + 1])),
                    strokeWidth = 5f,
                )
            }
        }
        points.forEachIndexed { i, v ->
            drawCircle(color = color, radius = 7f, center = Offset(x(i), y(v)))
            drawCircle(color = Color.White, radius = 3f, center = Offset(x(i), y(v)),
                style = Stroke(width = 2f))
        }
    }
}
