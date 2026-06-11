package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ChartCogs
import com.example.ui.theme.ChartExpense
import com.example.ui.theme.ChartProfit
import com.example.ui.theme.ChartWaste
import kotlin.math.max

/**
 * Gráfica de barras apiladas "¿a dónde se va el dinero?": por día, la venta
 * se descompone en costo de mercancía (azul), gastos (naranja), merma (rojo)
 * y ganancia real (verde). Implementada con primitivas de Compose para no
 * depender de librerías externas; la estructura permite migrar a Vico luego.
 */
@Composable
fun LeakageStackedBarChart(
    days: List<DailyBreakdown>,
    modifier: Modifier = Modifier,
    barAreaHeight: Dp = 160.dp
) {
    if (days.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(barAreaHeight),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Aún no hay historial para graficar",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Si la ganancia es positiva la pila mide exactamente las ventas; si es
    // negativa, los egresos superan la venta y la pila los muestra completos.
    val maxTotal = days
        .maxOf { max(it.sales, it.cogs + it.expenses + it.waste) }
        .takeIf { it > 0.0 } ?: 1.0

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEach { day ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barAreaHeight),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        val hasData = (day.cogs + day.expenses + day.waste + max(day.profit, 0.0)) > 0.0
                        if (hasData) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(0.62f)
                                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                            ) {
                                // Orden visual de arriba hacia abajo:
                                // ganancia, merma, gastos, costo de mercancía.
                                BarSegment(day.profit, maxTotal, barAreaHeight, ChartProfit)
                                BarSegment(day.waste, maxTotal, barAreaHeight, ChartWaste)
                                BarSegment(day.expenses, maxTotal, barAreaHeight, ChartExpense)
                                BarSegment(day.cogs, maxTotal, barAreaHeight, ChartCogs)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.62f)
                                    .height(2.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        day.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LegendItem(color = ChartProfit, label = "Ganancia")
            LegendItem(color = ChartCogs, label = "Costo")
            LegendItem(color = ChartExpense, label = "Gastos")
            LegendItem(color = ChartWaste, label = "Merma")
        }
    }
}

@Composable
private fun BarSegment(value: Double, maxTotal: Double, areaHeight: Dp, color: Color) {
    if (value <= 0.0) return
    val fraction = (value / maxTotal).toFloat().coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(areaHeight * fraction)
            .background(color)
    )
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Barra horizontal del Leakage Score: porción roja = capital de inventario
 * perdido en merma; porción verde = capital que salió vendido.
 */
@Composable
fun LeakageBar(leakagePercent: Double, modifier: Modifier = Modifier) {
    val fraction = (leakagePercent / 100.0).toFloat().coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(ChartProfit.copy(alpha = 0.35f))
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(10.dp)
                    .background(ChartWaste)
            )
        }
    }
}
