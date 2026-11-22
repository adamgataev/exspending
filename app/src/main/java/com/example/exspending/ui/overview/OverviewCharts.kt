package com.example.exspending.ui.overview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.exspending.domain.CategoryTotal
import com.example.exspending.domain.MonthCashflow
import com.example.exspending.ui.theme.AccentBlue
import com.example.exspending.ui.theme.ExpenseRed
import com.example.exspending.ui.theme.IncomeGreen
import com.example.exspending.ui.theme.TextHint
import com.example.exspending.ui.theme.TextSecondary
import com.example.exspending.ui.theme.TransferAmber
import com.example.exspending.ui.util.MoneyFormat
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

private val piePalette = listOf(
    AccentBlue,
    ExpenseRed,
    IncomeGreen,
    TransferAmber,
    Color(0xFFB39DDB),
    Color(0xFF80CBC4),
    Color(0xFFFFCC80),
    Color(0xFF90CAF9),
    Color(0xFFCE93D8),
    Color(0xFFA5D6A7)
)

private const val Y_TICK_COUNT = 4

@Composable
fun MonthlyCashflowChart(
    months: List<MonthCashflow>,
    modifier: Modifier = Modifier
) {
    val rawMax = months.maxOfOrNull { maxOf(it.income, it.expense) }
        ?.coerceAtLeast(1.0) ?: 1.0
    val maxValue = niceCeiling(rawMax)
    val tickStep = maxValue / Y_TICK_COUNT
    val labelColor = android.graphics.Color.parseColor("#CAC4D0")
    val gridColor = TextHint.copy(alpha = 0.35f)

    Column(modifier = modifier) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        val yAxisWidth = 32.dp.toPx()
        val chartLeft = yAxisWidth
        val chartRight = size.width
        val chartWidth = chartRight - chartLeft
        val chartBottom = size.height - 28f
        val chartTop = 8f
        val chartHeight = chartBottom - chartTop
        val groupWidth = chartWidth / 12f
        val barWidth = groupWidth * 0.32f

        val labelPaint = android.graphics.Paint().apply {
            color = labelColor
            textAlign = android.graphics.Paint.Align.RIGHT
            textSize = 26f
            isAntiAlias = true
        }

        for (i in 0..Y_TICK_COUNT) {
            val value = tickStep * i
            val y = chartBottom - (value / maxValue * chartHeight).toFloat()
            drawLine(
                color = gridColor,
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                strokeWidth = 1.dp.toPx()
            )
            drawContext.canvas.nativeCanvas.drawText(
                formatAxisTick(value),
                chartLeft - 4.dp.toPx(),
                y + 8f,
                labelPaint
            )
        }

        val monthPaint = android.graphics.Paint().apply {
            color = labelColor
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 28f
            isAntiAlias = true
        }

        months.forEachIndexed { index, month ->
            val x0 = chartLeft + index * groupWidth + groupWidth * 0.12f
            val incomeH = (month.income / maxValue * chartHeight).toFloat()
            val expenseH = (month.expense / maxValue * chartHeight).toFloat()

            drawRect(
                color = IncomeGreen,
                topLeft = Offset(x0, chartBottom - incomeH),
                size = Size(barWidth, incomeH)
            )
            drawRect(
                color = ExpenseRed,
                topLeft = Offset(x0 + barWidth + 4f, chartBottom - expenseH),
                size = Size(barWidth, expenseH)
            )

            drawContext.canvas.nativeCanvas.drawText(
                month.label,
                x0 + barWidth,
                size.height - 6f,
                monthPaint
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LegendDot(IncomeGreen, "Income")
        LegendDot(ExpenseRed, "Expenses")
    }
    }
}

/** Round max up to a clean tick ceiling for ~[Y_TICK_COUNT] intervals. */
private fun niceCeiling(value: Double): Double {
    if (value <= 0.0) return 1.0
    val roughStep = value / Y_TICK_COUNT
    val magnitude = 10.0.pow(floor(log10(roughStep)))
    val residual = roughStep / magnitude
    val niceResidual = when {
        residual <= 1.0 -> 1.0
        residual <= 2.0 -> 2.0
        residual <= 5.0 -> 5.0
        else -> 10.0
    }
    val niceStep = niceResidual * magnitude
    return ceil(value / niceStep) * niceStep
}

private fun formatAxisTick(value: Double): String {
    if (value == 0.0) return "€0"
    if (value >= 1_000_000) {
        val m = value / 1_000_000
        return if (m % 1.0 == 0.0) "€${m.toInt()}M" else "€${"%.1f".format(m)}M"
    }
    if (value >= 1_000) {
        val k = value / 1_000
        return if (k % 1.0 == 0.0) "€${k.toInt()}k" else "€${"%.1f".format(k)}k"
    }
    return MoneyFormat.format(value)
}

@Composable
fun CategoryPieChart(
    title: String,
    slices: List<CategoryTotal>
) {
    if (slices.isEmpty()) {
        Text("No data", color = TextHint)
        return
    }
    val total = slices.sumOf { it.amount }.coerceAtLeast(0.0001)
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(modifier = Modifier.size(160.dp)) {
                var startAngle = -90f
                slices.forEachIndexed { index, slice ->
                    val sweep = (slice.amount / total * 360f).toFloat()
                    drawArc(
                        color = piePalette[index % piePalette.size],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true,
                        topLeft = Offset.Zero,
                        size = Size(size.width, size.height)
                    )
                    startAngle += sweep
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                slices.take(8).forEachIndexed { index, slice ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(10.dp)) {
                            drawCircle(piePalette[index % piePalette.size])
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${slice.name}: ${MoneyFormat.format(slice.amount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(10.dp)) { drawCircle(color) }
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}
