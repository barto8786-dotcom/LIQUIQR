package com.liquid.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LibraryScreen(st: AppState, onOpenPlayer: () -> Unit, onCustomize: () -> Unit, onEq: () -> Unit) {
    val skin = Skins[st.prefs.skin]
    val bmp = rememberArt(st.current)
    var tab by remember { mutableIntStateOf(0) }
    var group by remember { mutableStateOf<String?>(null) }
    var q by remember { mutableStateOf("") }

    val groups = remember(st.songs, tab, q) {
        if (tab == 0) emptyList() else st.songs.groupBy { if (tab == 1) it.album else it.artist }
            .toList().filter { q.isBlank() || it.first.contains(q, true) }.sortedBy { it.first.lowercase() }
    }
    val shown = remember(st.songs, tab, group, q) {
        st.songs.filter { s ->
            (tab == 0 || group == null || (if (tab == 1) s.album else s.artist) == group) &&
                (q.isBlank() || s.title.contains(q, true) || s.artist.contains(q, true))
        }
    }

    Box(Modifier.fillMaxSize()) {
        AppBackground(skin, bmp, st.prefs.blurBg, st.prefs.blurAmount)
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Biblioteca", Modifier.weight(1f), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Ib(Icons.Rounded.GraphicEq, "Ecualizador", onClick = onEq)
                Ib(Icons.Rounded.Tune, "Personalizar", onClick = onCustomize)
            }
            // Búsqueda
            BasicTextField(
                value = q, onValueChange = { q = it }, singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp).fillMaxWidth(),
                decorationBox = { inner ->
                    Row(
                        Modifier.fillMaxWidth().height(46.dp).glass(CircleShape, skin).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Search, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Box { if (q.isEmpty()) Text("Buscar", color = Color.White.copy(.6f), fontSize = 15.sp); inner() }
                    }
                }
            )
            // Pestañas
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 6.dp).fillMaxWidth().height(44.dp).glass(CircleShape, skin).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Canciones", "Álbumes", "Artistas").forEachIndexed { i, t ->
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clip(CircleShape)
                            .background(if (tab == i) Color.White.copy(.26f) else Color.Transparent)
                            .clickable { tab = i; group = null },
                        contentAlignment = Alignment.Center
                    ) { Text(t, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
            if (tab != 0 && group != null) {
                Row(
                    Modifier.padding(horizontal = 24.dp, vertical = 4.dp).clickable { group = null },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.size(18.dp))
                    Text("  $group  ·  toca para volver", fontSize = 13.sp, color = Color.White.copy(.8f))
                }
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 130.dp)) {
                if (tab != 0 && group == null) {
                    items(groups, key = { it.first }) { (name, list) ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp).clip(RoundedCornerShape(20.dp))
                                .clickable { group = name }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${list.size}", fontSize = 13.sp, color = Color.White.copy(.7f))
                        }
                    }
                } else {
                    items(shown, key = { it.id }) { SongRow(st, it) }
                }
            }
        }
        // Mini reproductor
        val cur = st.current
        if (cur != null) {
            Row(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp).fillMaxWidth().height(64.dp)
                    .glass(CircleShape, skin, .04f).clickable { onOpenPlayer() }.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val art = rememberArt(cur, 128)
                Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(.15f))) {
                    if (art != null) Image(art.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(cur.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(cur.artist, fontSize = 12.sp, color = Color.White.copy(.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Ib(if (st.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (st.playing) "Pausa" else "Reproducir") { st.toggle() }
            }
        }
    }
}

@Composable
fun SongRow(st: AppState, s: Song) {
    val art = rememberArt(s, 128)
    val sel = st.current?.id == s.id
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp).clip(RoundedCornerShape(20.dp))
            .background(if (sel) Color.White.copy(.16f) else Color.Transparent)
            .clickable { st.play(st.songs.indexOfFirst { it.id == s.id }) }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(.12f))) {
            if (art != null) Image(art.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(s.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(s.artist, fontSize = 13.sp, color = Color.White.copy(.72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(fmt(s.durationMs), fontSize = 13.sp, color = Color.White.copy(.72f))
    }
}
