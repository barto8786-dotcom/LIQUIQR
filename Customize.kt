package com.liquid.player

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun Pill(text: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier.clip(CircleShape)
            .background(if (selected) accent.copy(.35f) else Color.White.copy(.12f))
            .border(1.dp, if (selected) accent else Color.White.copy(.25f), CircleShape)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp)
    ) { Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun CustomizeScreen(st: AppState, onBack: () -> Unit, onEq: () -> Unit) {
    val p = st.prefs
    val skin = Skins[p.skin]
    val bmp = rememberArt(st.current)
    Box(Modifier.fillMaxSize()) {
        AppBackground(skin, bmp, p.blurBg, p.blurAmount)
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Ib(Icons.Rounded.ArrowBack, "Volver", onClick = onBack)
                Text("Personalizar reproductor", Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.size(48.dp))
            }

            SectionLabel("Skin de la pantalla del reproductor")
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Skins.forEachIndexed { i, s ->
                    val sel = p.skin == i
                    val shape = RoundedCornerShape(22.dp)
                    Box(
                        Modifier.size(width = 96.dp, height = 128.dp).clip(shape)
                            .background(Brush.linearGradient(listOf(s.blobA, s.blobB, s.base)))
                            .border(if (sel) 3.dp else 1.dp, if (sel) s.accent else Color.White.copy(.3f), shape)
                            .clickable { p.upd { skin = i } },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(Modifier.align(Alignment.TopCenter).padding(top = 14.dp).size(54.dp).clip(s.shapeOf()).background(Color.White.copy(.35f)))
                        Text(s.name, Modifier.padding(8.dp), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            SectionLabel("Carátula")
            GlassCard(skin) {
                Text("Tamaño: ${(p.coverFrac * 100).roundToInt()}% del ancho", fontSize = 15.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Pequeña" to .55f, "Mediana" to .7f, "Grande" to .85f, "Completa" to 1f).forEach { (n, v) ->
                        Pill(n, kotlin.math.abs(p.coverFrac - v) < .02f, skin.accent) { p.upd { coverFrac = v } }
                    }
                }
                Slider(p.coverFrac, { v -> p.upd { coverFrac = v } }, valueRange = .5f..1f)
            }

            SectionLabel("Fondo")
            GlassCard(skin) {
                SwitchRow("Fondo con la carátula desenfocada", p.blurBg) { v -> p.upd { blurBg = v } }
                Text("Intensidad del desenfoque: ${p.blurAmount.roundToInt()}", fontSize = 15.sp, color = Color.White.copy(if (p.blurBg) 1f else .4f))
                Slider(p.blurAmount, { v -> p.upd { blurAmount = v } }, valueRange = 0f..60f, enabled = p.blurBg)
                if (Build.VERSION.SDK_INT < 31)
                    Text("En Android 11 o anterior el desenfoque es aproximado.", fontSize = 12.sp, color = Color.White.copy(.7f))
            }

            SectionLabel("Botones de la pantalla del reproductor")
            GlassCard(skin) {
                BUTTONS.forEach { (id, name) ->
                    SwitchRow(name, p.show(id)) { on -> p.upd { hidden = if (on) hidden - id else hidden + id } }
                }
            }

            SectionLabel("Audio")
            GlassCard(skin) {
                SwitchRow(
                    "Bit-perfect (USB)", p.bitPerfect,
                    sub = if (p.bitPerfect && st.bpStatus.isNotEmpty()) st.bpStatus
                    else "Android 14+ y DAC/auriculares USB. Desactiva el ecualizador."
                ) { v -> p.upd { bitPerfect = v } }
                SwitchRow("Ecualizador de 10 bandas", p.eqOn, enabled = !p.bitPerfect) { v -> p.upd { eqOn = v } }
                Pill("Abrir ecualizador", false, skin.accent, onEq)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
