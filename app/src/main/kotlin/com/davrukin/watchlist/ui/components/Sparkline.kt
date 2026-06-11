package com.davrukin.watchlist.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.davrukin.watchlist.ui.theme.WatchlistTheme

/**
 * Minimal polyline of recent prices. Renders nothing with fewer than two points; a flat series
 * draws a midline so a quiet market still reads as "alive".
 */
@Composable
fun Sparkline(
    prices: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier,
        onDraw = {
            if (prices.size < 2) {
                return@Canvas
            }
            val min: Double = prices.min()
            val max: Double = prices.max()
            val range: Double = max - min
            val stepX: Float = size.width / (prices.size - 1)
            val path = Path()
            prices.forEachIndexed { index: Int, price: Double ->
                val x: Float = index * stepX
                val y: Float =
                    if (range == 0.0) {
                        size.height / 2
                    } else {
                        (size.height * (1 - ((price - min) / range))).toFloat()
                    }
                if (index == 0) {
                    path.moveTo(x = x, y = y)
                } else {
                    path.lineTo(x = x, y = y)
                }
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 1.5.dp.toPx()),
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun SparklineRisingPreview() {
    WatchlistTheme {
        Sparkline(
            prices = listOf(100.0, 100.4, 100.2, 100.9, 101.3, 101.1, 101.8),
            color = Color(color = 0xFF1B873B),
            modifier = Modifier.size(width = 64.dp, height = 24.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SparklineFallingPreview() {
    WatchlistTheme {
        Sparkline(
            prices = listOf(101.8, 101.2, 101.4, 100.7, 100.9, 100.1),
            color = Color(color = 0xFFB3261E),
            modifier = Modifier.size(width = 64.dp, height = 24.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SparklineFlatPreview() {
    WatchlistTheme {
        Sparkline(
            prices = listOf(100.0, 100.0, 100.0),
            color = Color.Gray,
            modifier = Modifier.size(width = 64.dp, height = 24.dp),
        )
    }
}
