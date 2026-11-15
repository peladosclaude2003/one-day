package com.oneday.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneday.app.data.*

val SET_KINDS = listOf("Normal", "Calent.", "Al fallo", "Drop")
fun setTag(kind: Int) = when (kind) { SET_WARMUP -> "C"; SET_FAILURE -> "F"; SET_DROP -> "D"; else -> null }

/** Editor de una serie, según el tipo de ejercicio (peso × reps, tiempo, o tiempo y distancia). */
@Composable
fun SetDialog(ex: Exercise?, base: WorkSet?, canDelete: Boolean, onDismiss: () -> Unit, onDelete: () -> Unit, onSave: (WorkSet) -> Unit) {
    val kind = ex?.kind ?: 0
    var weight by remember { mutableStateOf(base?.let { fmtWeight(it.weight) } ?: "") }
    var reps by remember { mutableStateOf(base?.reps?.takeIf { it > 0 }?.toString() ?: "") }
    var secs by remember { mutableStateOf((base?.seconds ?: if (kind == 1) 30 else 0).toString()) }
    var mins by remember { mutableStateOf(((base?.seconds ?: 1200) / 60).toString()) }
    var km by remember { mutableStateOf(base?.km?.let { fmtWeight(it) } ?: "2") }
    var setKind by remember { mutableIntStateOf(base?.kind ?: SET_NORMAL) }
    var rpe by remember { mutableStateOf(base?.rpe) }
    var plates by remember { mutableStateOf(false) }

    val result: WorkSet? = when (kind) {
        1 -> secs.toIntOrNull()?.takeIf { it > 0 }?.let { WorkSet(0.0, 0, setKind, rpe, seconds = it) }
        2 -> {
            val m = mins.toIntOrNull(); val k = km.replace(',', '.').toDoubleOrNull()
            if (m != null && k != null && (m > 0 || k > 0)) WorkSet(0.0, 0, setKind, rpe, seconds = m * 60, km = k) else null
        }
        else -> {
            val w = weight.replace(',', '.').toDoubleOrNull(); val r = reps.toIntOrNull()
            if (w != null && r != null && r > 0) WorkSet(w, r, setKind, rpe) else null
        }
    }

    OdDialog(
        onDismissRequest = onDismiss,
        title = { Text(ex?.name ?: "Serie") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (kind) {
                    1 -> Stepper("Segundos", secs, { secs = it }, step = 5.0, decimal = false)
                    2 -> {
                        Stepper("Minutos", mins, { mins = it }, step = 1.0, decimal = false)
                        Stepper("Distancia (km)", km, { km = it }, step = 0.1, decimal = true)
                    }
                    else -> {
                        Stepper("Peso (kg)", weight, { weight = it }, step = 2.5, decimal = true)
                        Stepper("Repeticiones", reps, { reps = it }, step = 1.0, decimal = false)
                        val total = weight.replace(',', '.').toDoubleOrNull()
                        if (total != null && total > Repo.data.settings.barKg) {
                            TextButton(onClick = { plates = !plates }, contentPadding = PaddingValues(0.dp)) {
                                Text(if (plates) "Ocultar discos" else "¿Qué discos pongo?", color = Ink2)
                            }
                            if (plates) PlateView(total)
                        }
                    }
                }
                Text("Tipo de serie", color = Muted, style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SET_KINDS.forEachIndexed { i, k -> ChoicePill(k, setKind == i, Modifier.weight(1f)) { setKind = i } }
                }
                if (setKind == SET_WARMUP) Text("El calentamiento no suma al volumen ni cuenta para récords.", color = Faint, style = MaterialTheme.typography.bodySmall)
                Text("Esfuerzo (1 = muy fácil · 10 = no podías más)", color = Muted, style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    (1..10).forEach { n ->
                        val sel = rpe == n
                        Box(
                            Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(10.dp))
                                .background(if (sel) Ink else Color(0xFF2A2A2E)).clickable { rpe = if (sel) null else n },
                            contentAlignment = Alignment.Center,
                        ) { Text("$n", color = if (sel) Bg else Ink2, style = MaterialTheme.typography.labelMedium, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) }
                    }
                }
                if (canDelete) TextButton(onClick = { onDelete(); onDismiss() }, contentPadding = PaddingValues(0.dp)) { Text("Eliminar serie", color = Bad) }
            }
        },
        confirmButton = { TextButton(enabled = result != null, onClick = { onSave(result!!); onDismiss() }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Muted) } },
    )
}

/** Dibujo de la barra con los discos de un lado y la lista por lado. */
@Composable
fun PlateView(total: Double) {
    val s = Repo.data.settings
    val (side, left) = platesFor(total, s.barKg, s.plates)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF26262A)).padding(14.dp)) {
        val maxP = (s.plates.maxOrNull() ?: 25.0)
        Canvas(Modifier.fillMaxWidth().height(70.dp)) {
            val cy = size.height / 2
            // barra
            drawRoundRect(Faint, Offset(0f, cy - 4.dp.toPx()), Size(size.width, 8.dp.toPx()), CornerRadius(4.dp.toPx()))
            drawRoundRect(Muted, Offset(size.width * 0.18f, cy - 7.dp.toPx()), Size(6.dp.toPx(), 14.dp.toPx()), CornerRadius(2.dp.toPx()))
            var x = size.width * 0.18f + 8.dp.toPx()
            side.forEach { p ->
                val h = size.height * (0.35f + 0.65f * (p / maxP).toFloat())
                val w = (6 + 10 * (p / maxP)).dp.toPx()
                drawRoundRect(if (p == side.first()) Ink2 else Ink2.copy(alpha = 0.75f), Offset(x, cy - h / 2), Size(w, h), CornerRadius(3.dp.toPx()))
                x += w + 2.dp.toPx()
            }
        }
        Text(
            if (side.isEmpty()) "Solo la barra (${fmtWeight(s.barKg)} kg)" else "Por lado: " + side.joinToString(" + ") { fmtWeight(it) } + " kg",
            color = Ink, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Text("Barra de ${fmtWeight(s.barKg)} kg" + if (left > 0) " · faltan ${fmtWeight(left)} kg (no hay discos para eso)" else "",
            color = Muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

/** Fila de una serie en el entrenamiento en curso. */
@Composable
fun SetRow(index: Int, s: WorkSet, pr: Boolean, onClick: () -> Unit) {
    val warm = s.kind == SET_WARMUP
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(50)).background(CardHigh)
            .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 12.dp).alpha(if (warm) 0.5f else 1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tag = setTag(s.kind)
        Box(Modifier.width(28.dp)) {
            if (tag != null) Text(tag, color = if (s.kind == SET_FAILURE) Accent else Ink2, fontWeight = FontWeight.Bold)
            else Text("$index", color = Faint)
        }
        Text(
            when {
                s.km != null -> "${fmtWeight(round1(s.km))} km"
                s.seconds != null -> fmtSecs(s.seconds)
                else -> "${fmtWeight(s.weight)} kg"
            }, fontWeight = FontWeight.SemiBold, color = Ink,
        )
        if (pr) { Spacer(Modifier.width(10.dp)); Glyph("record", Accent, 15.dp); Spacer(Modifier.width(4.dp)); Text("Récord", color = Accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.weight(1f))
        s.rpe?.let { Text("@$it", color = Muted, style = MaterialTheme.typography.bodySmall); Spacer(Modifier.width(10.dp)) }
        when {
            s.km != null -> Text(fmtSecs(s.seconds ?: 0), fontWeight = FontWeight.SemiBold, color = Ink2)
            s.seconds != null -> {}
            else -> Text("× ${s.reps}", fontWeight = FontWeight.SemiBold, color = Ink2)
        }
    }
}
