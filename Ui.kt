package com.liquid.player

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun Skin.shapeOf(): Shape = when (shape) {
    1 -> CircleShape
    2 -> RectangleShape
    else -> RoundedCornerShape(corner.dp)
}

/** Panel de "cristal": relleno translúcido con degradado + borde con brillo. */
fun Modifier.glass(shape: Shape, skin: Skin, boost: Float = 0f): Modifier {
    val a = (skin.glass + boost).coerceIn(0f, 0.9f)
    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(Color.White.copy((a + .08f).coerceAtMost(1f)), Color.White.copy(a * .6f))))
        .border(1.dp, Brush.verticalGradient(listOf(skin.border.copy(.6f), skin.border.copy(.08f))), shape)
}

@Composable
fun AppBackground(skin: Skin, bmp: Bitmap?, useCover: Boolean, blurDp: Float) {
    Box(Modifier.fillMaxSize().background(skin.base)) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.width * 0.95f
            val c1 = Offset(0f, 0f)
            val c2 = Offset(size.width, size.height * .55f)
            val c3 = Offset(0f, size.height)
            drawCircle(Brush.radialGradient(listOf(skin.blobA.copy(.75f), Color.Transparent), c1, r), r, c1)
            drawCircle(Brush.radialGradient(listOf(skin.blobB.copy(.7f), Color.Transparent), c2, r), r, c2)
            drawCircle(Brush.radialGradient(listOf(skin.blobA.copy(.5f), Color.Transparent), c3, r), r, c3)
        }
        if (useCover) {
            Crossfade(targetState = bmp, animationSpec = tween(500), label = "bg") { b ->
                if (b != null) {
                    val modern = Build.VERSION.SDK_INT >= 31
                    val shown = remember(b, blurDp) {
                        if (modern) b else {
                            val s = (64 - blurDp * 1.0f).toInt().coerceAtLeast(6)
                            Bitmap.createScaledBitmap(b, s, s, true)
                        }
                    }
                    Box(Modifier.fillMaxSize()) {
                        Image(
                            shown.asImageBitmap(), null,
                            Modifier.fillMaxSize()
                                .graphicsLayer { scaleX = 1.3f; scaleY = 1.3f }
                                .blur(if (modern) blurDp.dp else 0.dp),
                            contentScale = ContentScale.Crop,
                            filterQuality = FilterQuality.Low
                        )
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(.35f)))
                    }
                }
            }
        }
    }
}

@Composable
fun Cover(skin: Skin, bmp: Bitmap?, rotation: () -> Float = { 0f }) {
    val shape = skin.shapeOf()
    val sh = if (skin.glow) Modifier.shadow(28.dp, shape, ambientColor = skin.accent, spotColor = skin.accent)
    else Modifier.shadow(18.dp, shape)
    Box(
        Modifier.fillMaxSize().then(sh).clip(shape).background(Color.White.copy(.1f))
            .border(if (skin.glow) 2.dp else 1.dp, skin.border.copy(.6f), shape),
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) Image(
            bmp.asImageBitmap(), null,
            Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation() },
            contentScale = ContentScale.Crop
        ) else Icon(Icons.Rounded.MusicNote, null, Modifier.fillMaxSize(.4f), tint = Color.White.copy(.6f))
        if (skin.shape == 1) Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            for (k in 1..6) drawCircle(Color.Black.copy(.16f), r * (0.35f + k * 0.1f), center, style = Stroke(1.dp.toPx()))
            drawCircle(skin.base, r * 0.09f, center)
            drawCircle(Color.White.copy(.3f), r * 0.09f, center, style = Stroke(2.dp.toPx()))
        }
    }
}

@Composable
fun Ib(icon: ImageVector, desc: String, tint: Color = Color.White, size: Dp = 28.dp, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(size))
    }
}

@Composable
fun GlassCard(skin: Skin, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 8.dp).glass(RoundedCornerShape(26.dp), skin).padding(16.dp),
        content = content
    )
}

@Composable
fun SwitchRow(label: String, checked: Boolean, sub: String? = null, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, fontSize = 15.sp)
            if (sub != null) Text(sub, fontSize = 12.sp, color = Color.White.copy(.7f))
        }
        Switch(checked, onChange, enabled = enabled)
    }
}

@Composable
fun SectionLabel(t: String) {
    Text(t, Modifier.padding(start = 8.dp, top = 12.dp), fontSize = 13.sp, color = Color.White.copy(.78f))
}
