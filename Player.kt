package com.liquid.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import kotlin.math.absoluteValue

@Composable
fun PlayerScreen(st: AppState, onClose: () -> Unit, onCustomize: () -> Unit, onEq: () -> Unit) {
    val p = st.prefs
    val skin = Skins[p.skin]
    val song = st.current
    val bmp = rememberArt(song)
    val cfg = LocalConfiguration.current
    val screenW = cfg.screenWidthDp.dp
    val coverW = minOf((cfg.screenWidthDp * p.coverFrac).dp, (cfg.screenHeightDp * 0.44f).dp)

    // Giro del vinilo
    var rot by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(st.playing, skin.shape) {
        if (st.playing && skin.shape == 1) {
            var last = withFrameMillis { it }
            while (true) {
                val now = withFrameMillis { it }
                rot = (rot + (now - last) * 0.04f) % 360f
                last = now
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AppBackground(skin, bmp, p.blurBg, p.blurAmount)
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Ib(Icons.Rounded.KeyboardArrowDown, "Cerrar", onClick = onClose)
                Text("Reproduciendo", Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                Ib(Icons.Rounded.Tune, "Personalizar", onClick = onCustomize)
            }
            Spacer(Modifier.weight(1f))

            // ---- Carrusel horizontal de carátulas ----
            if (st.songs.isNotEmpty()) {
                val pager = rememberPagerState(initialPage = st.index.coerceAtLeast(0)) { st.songs.size }
                LaunchedEffect(st.index) {
                    if (st.index >= 0 && pager.currentPage != st.index) pager.animateScrollToPage(st.index)
                }
                LaunchedEffect(pager) {
                    snapshotFlow { pager.settledPage }.collect { if (st.index >= 0 && it != st.index) st.play(it) }
                }
                HorizontalPager(
                    state = pager,
                    modifier = Modifier.fillMaxWidth().height(coverW),
                    contentPadding = PaddingValues(horizontal = ((screenW - coverW) / 2).coerceAtLeast(0.dp)),
                    pageSize = PageSize.Fixed(coverW),
                    pageSpacing = 14.dp,
                    key = { st.songs[it].id }
                ) { page ->
                    val s = st.songs[page]
                    val art = if (page == st.index) bmp else rememberArt(s)
                    Box(
                        Modifier.fillMaxSize().graphicsLayer {
                            val d = (page - pager.currentPage) - pager.currentPageOffsetFraction
                            val off = d.absoluteValue.coerceIn(0f, 1f)
                            val sc = 1f - 0.18f * off
                            scaleX = sc; scaleY = sc
                            alpha = 1f - 0.45f * off
                            rotationY = (-d).coerceIn(-1f, 1f) * 22f
                            cameraDistance = 12f * density
                        }
                    ) { Cover(skin, art, rotation = { if (page == st.index) rot else 0f }) }
                }
            }
            Spacer(Modifier.weight(1f))

            // ---- Título + favorita ----
            Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(song?.title ?: "—", fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song?.artist ?: "", fontSize = 15.sp, color = Color.White.copy(.78f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (p.show("fav") && song != null) {
                    val fav = song.id in p.favs
                    Ib(if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favorita",
                        tint = if (fav) skin.accent else Color.White) { st.toggleFav(song.id) }
                }
            }

            // ---- Barra de progreso ----
            var drag by remember { mutableStateOf<Float?>(null) }
            val frac = if (st.dur > 0) (st.pos.toFloat() / st.dur).coerceIn(0f, 1f) else 0f
            Slider(
                value = drag ?: frac,
                onValueChange = { drag = it },
                onValueChangeFinished = { drag?.let { st.seek((it * st.dur).toLong()) }; drag = null },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 30.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(fmt(((drag ?: frac) * st.dur).toLong()), fontSize = 12.sp, color = Color.White.copy(.78f))
                Text(fmt(st.dur), fontSize = 12.sp, color = Color.White.copy(.78f))
            }
            Spacer(Modifier.height(14.dp))

            // ---- Controles (cada botón se puede quitar/poner en Personalizar) ----
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().glass(RoundedCornerShape(44.dp), skin).padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
            ) {
                if (p.show("shuffle")) Ib(Icons.Rounded.Shuffle, "Aleatorio", if (st.shuffle) skin.accent else Color.White, 24.dp) { st.toggleShuffle() }
                if (p.show("prev")) Ib(Icons.Rounded.SkipPrevious, "Anterior", size = 34.dp) { st.prev() }
                Box(
                    Modifier.size(68.dp).glass(CircleShape, skin, .18f),
                    contentAlignment = Alignment.Center
                ) {
                    Ib(if (st.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (st.playing) "Pausa" else "Reproducir", size = 38.dp) { st.toggle() }
                }
                if (p.show("next")) Ib(Icons.Rounded.SkipNext, "Siguiente", size = 34.dp) { st.next() }
                if (p.show("repeat")) Ib(
                    if (st.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repetir",
                    if (st.repeat != Player.REPEAT_MODE_OFF) skin.accent else Color.White, 24.dp
                ) { st.cycleRepeat() }
                if (p.show("eq")) Ib(Icons.Rounded.GraphicEq, "Ecualizador", if (p.eqOn) skin.accent else Color.White, 24.dp, onClick = onEq)
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}
