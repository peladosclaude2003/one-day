package com.oneday.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.oneday.app.R
import com.oneday.app.data.*
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Lo que se dibuja en la imagen para compartir. */
data class ShareSpec(
    val number: String, val unit: String, val tier: Tier, val title: String,
    val quote: String? = null, val stats: List<Pair<String, String>> = emptyList(),
)

fun shareDate(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM yyyy", ES)).replace(".", "")

fun streakShare(s: Streak): ShareSpec {
    val days = streakDays(s)
    val got = MEDAL_DAYS.lastOrNull { days >= it }
    return ShareSpec("$days", "DÍAS", got?.let { tierFor(it) } ?: Tier.Gris, "$days días\nsin ${s.name.lowercase()}", s.reason.ifBlank { null },
        listOf("RÉCORD" to "${maxOf(s.bestDays, days)} d", "ANTOJOS SUPERADOS" to "${Repo.data.cravings.count { it.streakId == s.id }}", "FECHA" to shareDate()))
}

fun habitShare(h: Habit): ShareSpec {
    val st = habitStreak(h).toLong()
    val got = MEDAL_DAYS.lastOrNull { st >= it }
    return ShareSpec("$st", "DÍAS", got?.let { tierFor(it) } ?: Tier.Gris, "$st días seguidos\n${h.name.lowercase()}", null,
        listOf("MEJOR RACHA" to "${maxOf(habitBest(h).toLong(), st)} d", "DÍAS CUMPLIDOS" to "${h.doneDays.size}", "FECHA" to shareDate()))
}

/** Dibuja la imagen con el estilo de la app (formato historia 1080×1920 o cuadrado 1080×1080). */
fun renderShare(ctx: Context, spec: ShareSpec, square: Boolean, withQuote: Boolean): Bitmap {
    val w = 1080; val h = if (square) 1080 else 1920
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val display = ResourcesCompat.getFont(ctx, R.font.space_grotesk_bold) ?: Typeface.DEFAULT_BOLD
    val inter = ResourcesCompat.getFont(ctx, R.font.inter_regular) ?: Typeface.DEFAULT
    val interBold = ResourcesCompat.getFont(ctx, R.font.inter_semibold) ?: Typeface.DEFAULT_BOLD
    c.drawColor(android.graphics.Color.BLACK)
    val accent = Accent.toArgb()

    val medalR = if (square) 190f else 250f
    val mcx = w / 2f; val mcy = if (square) 400f else 720f
    // halo naranja
    c.drawCircle(mcx, mcy, medalR * 2.4f, Paint().apply {
        shader = RadialGradient(mcx, mcy, medalR * 2.4f, intArrayOf(android.graphics.Color.argb(60, 255, 122, 26), android.graphics.Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
    })
    // logo
    val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; style = Paint.Style.STROKE; strokeWidth = 11f; strokeCap = Paint.Cap.ROUND }
    val lx = 110f; val ly = 120f; val lr = 30f
    c.drawArc(RectF(lx - lr, ly - lr, lx + lr, ly + lr), -90f, 300f, false, lp)
    c.drawCircle(lx + lr * Math.cos(Math.toRadians(240.0)).toFloat(), ly + lr * Math.sin(Math.toRadians(240.0)).toFloat(), 7f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent })
    c.drawText("One Day", lx + 55f, ly + 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Ink.toArgb(); textSize = 40f; typeface = interBold })
    // medalla
    val t = spec.tier
    val sw = medalR / 30f
    c.drawCircle(mcx, mcy, medalR - sw, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = t.fill.toArgb() })
    c.drawCircle(mcx, mcy, medalR - sw, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = t.edge.toArgb(); style = Paint.Style.STROKE; strokeWidth = sw * 1.4f })
    c.drawCircle(mcx, mcy, medalR * 0.76f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = t.inner.toArgb(); style = Paint.Style.STROKE; strokeWidth = if (t == Tier.Pleno) sw * 2.6f else sw })
    val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = t.inner.toArgb(); strokeWidth = sw; strokeCap = Paint.Cap.ROUND }
    listOf(0f to -1f, 1f to 0f, 0f to 1f, -1f to 0f).forEach { (dx, dy) ->
        c.drawLine(mcx + dx * (medalR - sw * 1.5f), mcy + dy * (medalR - sw * 1.5f), mcx + dx * medalR * 0.88f, mcy + dy * medalR * 0.88f, tick)
    }
    val numP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = t.num.toArgb(); typeface = display; textAlign = Paint.Align.CENTER; textSize = medalR * if (spec.number.length >= 3) 0.62f else 0.78f; letterSpacing = -0.03f }
    c.drawText(spec.number, mcx, mcy + numP.textSize * 0.30f, numP)
    c.drawText(spec.unit, mcx, mcy + medalR * 0.52f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = t.num.toArgb(); typeface = interBold; textAlign = Paint.Align.CENTER; textSize = medalR * 0.13f; letterSpacing = 0.15f })
    // título
    val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Ink.toArgb(); typeface = display; textSize = if (square) 84f else 108f; letterSpacing = -0.02f }
    val title = StaticLayout.Builder.obtain(spec.title, 0, spec.title.length, tp, w - 160).setAlignment(Layout.Alignment.ALIGN_CENTER).setLineSpacing(0f, 0.95f).build()
    var y = mcy + medalR + if (square) 60f else 110f
    c.save(); c.translate(80f, y); title.draw(c); c.restore()
    y += title.height + 40f
    if (withQuote && spec.quote != null) {
        val qp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Ink2.toArgb(); typeface = Typeface.create(inter, Typeface.ITALIC); textSize = if (square) 40f else 48f }
        val q = StaticLayout.Builder.obtain("“${spec.quote}”", 0, spec.quote.length + 2, qp, w - 200).setAlignment(Layout.Alignment.ALIGN_CENTER).build()
        c.save(); c.translate(100f, y); q.draw(c); c.restore()
    }
    // pie con 3 datos
    if (spec.stats.isNotEmpty()) {
        val fy = h - (if (square) 150f else 220f)
        c.drawLine(80f, fy, w - 80f, fy, Paint().apply { color = android.graphics.Color.argb(40, 255, 255, 255); strokeWidth = 2f })
        val colW = (w - 160f) / spec.stats.size
        spec.stats.forEachIndexed { i, (label, value) ->
            val x = 80f + colW * i + if (i == spec.stats.lastIndex && spec.stats.size > 1) colW else 0f
            val align = if (i == spec.stats.lastIndex && spec.stats.size > 1) Paint.Align.RIGHT else Paint.Align.LEFT
            c.drawText(label, x, fy + 62f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Muted.toArgb(); typeface = interBold; textSize = 26f; letterSpacing = 0.12f; textAlign = align })
            c.drawText(value, x, fy + 120f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Ink.toArgb(); typeface = display; textSize = 50f; textAlign = align })
        }
    }
    return bmp
}

fun shareImage(ctx: Context, bmp: Bitmap) {
    val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
    val f = File(dir, "one-day-logro.png")
    f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
    val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    ctx.startActivity(Intent.createChooser(send, "Compartir logro").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** Vista previa con formato (historia o cuadrado) y opción de incluir tu motivo. */
@Composable
fun ShareSheet(spec: ShareSpec, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var square by remember { mutableStateOf(false) }
    var quote by remember { mutableStateOf(true) }
    val bmp = remember(spec, square, quote) { renderShare(ctx, spec, square, quote) }
    GlassSheet(onDismiss) {
        Text("COMPARTIR", color = Muted, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(12.dp))
        Image(bmp.asImageBitmap(), "Vista previa de la imagen", Modifier.fillMaxWidth(if (square) 0.8f else 0.62f).aspectRatio(if (square) 1f else 9f / 16f)
            .clip(RoundedCornerShape(22.dp)).align(androidx.compose.ui.Alignment.CenterHorizontally))
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoicePill("Historia", !square, Modifier.weight(1f)) { square = false }
            ChoicePill("Cuadrado", square, Modifier.weight(1f)) { square = true }
            if (spec.quote != null) ChoicePill(if (quote) "Con motivo" else "Sin motivo", quote, Modifier.weight(1.1f)) { quote = !quote }
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton("Compartir", glyph = "compartir") { Fx.success(); shareImage(ctx, bmp); onDismiss() }
    }
}
