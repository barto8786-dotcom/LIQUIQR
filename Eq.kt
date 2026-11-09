package com.liquid.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun EqBand(value: Float, accent: Color, enabled: Boolean, onChange: (Float) -> Unit) {
    val cb by rememberUpdatedState(onChange)
    Canvas(
        Modifier.width(30.dp).height(190.dp).pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                fun set(y: Float) {
                    val v = (12f - 24f * (y / size.height)).coerceIn(-12f, 12f)
                    cb((v * 2).roundToInt() / 2f)
                }
                val down = awaitFirstDown()
                set(down.position.y)
                do {
                    val e = awaitPointerEvent()
                    e.changes.forEach { if (it.pressed) { set(it.position.y); it.consume() } }
                } while (e.changes.any { it.pressed })
            }
        }
    ) {
        val cx = size.width / 2
        val w = 6.dp.toPx()
        val y = size.height * (12f - value) / 24f
        val a = if (enabled) 1f else .4f
        drawLine(Color.White.copy(.2f), Offset(cx, 0f), Offset(cx, size.height), w, StrokeCap.Round)
        drawLine(Color.White.copy(.35f), Offset(cx - 12.dp.toPx(), size.height / 2), Offset(cx + 12.dp.toPx(), size.height / 2), 1.dp.toPx())
        drawLine(accent.copy(a), Offset(cx, size.height / 2), Offset(cx, y), w, StrokeCap.Round)
        drawCircle(Color.White.copy(a), 10.dp.toPx(), Offset(cx, y))
    }
}

@Composable
fun EqScreen(st: AppState, onBack: () -> Unit) {
    val p = st.prefs
    val skin = Skins[p.skin]
    val bmp = rememberArt(st.current)
    Box(Modifier.fillMaxSize()) {
        AppBackground(skin, bmp, p.blurBg, p.blurAmount)
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Ib(Icons.Rounded.ArrowBack, "Volver", onClick = onBack)
                Text("Ecualizador · 10 bandas", Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Switch(p.eqOn, { v -> p.upd { eqOn = v } }, enabled = !p.bitPerfect)
            }
            if (p.bitPerfect) Text(
                "El ecualizador está desactivado mientras Bit-perfect está activo.",
                Modifier.padding(8.dp), fontSize = 13.sp, color = Color.White.copy(.8f)
            )
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EqPresets.forEach { (n, g) -> Pill(n, p.eq == g, skin.accent) { p.upd { eq = g } } }
            }
            GlassCard(skin) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (i in 0 until 10) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("%+.1f".format(p.eq[i]), fontSize = 9.sp, color = Color.White.copy(.8f))
                            EqBand(p.eq[i], skin.accent, p.eqOn && !p.bitPerfect) { v ->
                                p.upd { eq = eq.toMutableList().also { it[i] = v } }
                            }
                            Text(EqProcessor.LABELS[i], fontSize = 10.sp, color = Color.White.copy(.8f))
                        }
                    }
                }
                Text("dB", Modifier.padding(top = 6.dp), fontSize = 11.sp, color = Color.White.copy(.6f))
            }
            Text(
                "Rango ±12 dB. Para ajustar, activa el interruptor de arriba y arrastra cada banda.",
                Modifier.padding(8.dp), fontSize = 12.sp, color = Color.White.copy(.7f)
            )
        }
    }
}
