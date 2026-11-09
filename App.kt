package com.liquid.player

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT)
        )
        setContent { LiquidApp() }
    }
}

@Composable
fun LiquidApp() {
    val ctx = LocalContext.current
    val st = remember { AppState(ctx) }
    val p = st.prefs
    val skin = Skins[p.skin]
    var showPlayer by remember { mutableStateOf(false) }
    var overlay by remember { mutableStateOf<String?>(null) }
    var lastOverlay by remember { mutableStateOf("custom") }
    SideEffect { overlay?.let { lastOverlay = it } }

    val perm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    val perms = remember {
        buildList { add(perm); if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS) }.toTypedArray()
    }
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        granted = r[perm] == true
    }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(perms) }
    LaunchedEffect(granted) {
        if (granted) st.setSongs(withContext(Dispatchers.IO) { Library.load(ctx) })
    }
    LaunchedEffect(st.playing) {
        while (st.playing) {
            st.pos = st.player.currentPosition
            st.dur = st.player.duration.coerceAtLeast(0L)
            delay(250)
        }
    }
    LaunchedEffect(p.eqOn, p.eq, p.bitPerfect) {
        PlayerHolder.eq.enabled = p.eqOn && !p.bitPerfect
        PlayerHolder.eq.setGains(p.eq)
    }
    LaunchedEffect(p.bitPerfect) { st.bpStatus = BitPerfect.apply(ctx, p.bitPerfect) }

    BackHandler(enabled = overlay != null) { overlay = null }
    BackHandler(enabled = overlay == null && showPlayer) { showPlayer = false }

    MaterialTheme(colorScheme = darkColorScheme(primary = skin.accent, onPrimary = Color.Black)) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Box(Modifier.fillMaxSize().background(skin.base)) {
                if (!granted) {
                    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Liquid Player", fontSize = 30.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Necesito permiso para leer la música de tu móvil.", textAlign = TextAlign.Center)
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = { launcher.launch(perms) }, shape = CircleShape) { Text("Dar permiso") }
                    }
                } else {
                    LibraryScreen(st, { showPlayer = true }, { overlay = "custom" }, { overlay = "eq" })
                    AnimatedVisibility(showPlayer, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                        PlayerScreen(st, { showPlayer = false }, { overlay = "custom" }, { overlay = "eq" })
                    }
                    AnimatedVisibility(overlay != null, enter = slideInHorizontally { it }, exit = slideOutHorizontally { it }) {
                        if ((overlay ?: lastOverlay) == "eq") EqScreen(st) { overlay = null }
                        else CustomizeScreen(st, { overlay = null }, { overlay = "eq" })
                    }
                }
            }
        }
    }
}
