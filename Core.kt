package com.liquid.player

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ---------- Datos ----------
data class Song(
    val id: Long, val title: String, val artist: String, val album: String,
    val albumId: Long, val durationMs: Long, val uri: Uri
) {
    val artUri: Uri get() = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
}

fun Song.toItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString()).setUri(uri)
    .setMediaMetadata(
        MediaMetadata.Builder().setTitle(title).setArtist(artist).setAlbumTitle(album).setArtworkUri(artUri).build()
    ).build()

object Library {
    fun load(ctx: Context): List<Song> {
        val out = ArrayList<Song>()
        val coll = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val proj = arrayOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DURATION
        )
        val sel = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 30000"
        ctx.contentResolver.query(coll, proj, sel, null, "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC")?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                fun clean(s: String?, d: String) = if (s.isNullOrBlank() || s == "<unknown>") d else s
                out += Song(
                    id, clean(c.getString(1), "Sin título"), clean(c.getString(2), "Artista desconocido"),
                    clean(c.getString(3), "Álbum desconocido"), c.getLong(4), c.getLong(5),
                    ContentUris.withAppendedId(coll, id)
                )
            }
        }
        return out
    }
}

object Art {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    suspend fun load(ctx: Context, s: Song, px: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = "${s.id}_$px"
        cache.get(key)?.let { return@withContext it }
        val bmp = try {
            if (Build.VERSION.SDK_INT >= 29) ctx.contentResolver.loadThumbnail(s.uri, Size(px, px), null)
            else ctx.contentResolver.openInputStream(s.artUri)?.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) { null }
        if (bmp != null) cache.put(key, bmp)
        bmp
    }
}

@Composable
fun rememberArt(s: Song?, px: Int = 600): Bitmap? {
    val ctx = LocalContext.current
    val state = produceState<Bitmap?>(null, s?.id, px) { value = if (s == null) null else Art.load(ctx, s, px) }
    return state.value
}

fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

// ---------- Skins ----------
/** shape: 0 = redondeada, 1 = vinilo (círculo giratorio), 2 = cuadrada */
data class Skin(
    val name: String, val base: Color, val blobA: Color, val blobB: Color, val accent: Color,
    val glass: Float, val border: Color, val shape: Int, val corner: Int, val glow: Boolean = false
)

val Skins = listOf(
    Skin("Cristal", Color(0xFF0B0D1A), Color(0xFFFF5E8A), Color(0xFF4F8CFF), Color.White, .14f, Color.White, 0, 36),
    Skin("Vinilo", Color(0xFF14100C), Color(0xFFB8793A), Color(0xFF5A3A1E), Color(0xFFF2B35E), .10f, Color(0xFFF2B35E), 1, 0),
    Skin("Neón", Color(0xFF05030F), Color(0xFFFF2BD6), Color(0xFF00E5FF), Color(0xFF00E5FF), .05f, Color(0xFF00E5FF), 0, 22, true),
    Skin("Aurora", Color(0xFF04141A), Color(0xFF1DE9B6), Color(0xFF7C4DFF), Color(0xFF69F0AE), .12f, Color(0xFFB9F6CA), 0, 56),
    Skin("Minimal", Color(0xFF0A0A0A), Color(0xFF2A2A2A), Color(0xFF151515), Color.White, .05f, Color.White, 2, 0),
    Skin("Ámbar", Color(0xFF160B05), Color(0xFFFF7A18), Color(0xFFFF3D00), Color(0xFFFFB74D), .13f, Color(0xFFFFE0B2), 0, 28)
)

val BUTTONS = listOf(
    "shuffle" to "Aleatorio", "prev" to "Anterior", "next" to "Siguiente",
    "repeat" to "Repetir", "fav" to "Favorita", "eq" to "Ecualizador"
)

val EqPresets: List<Pair<String, List<Float>>> = listOf(
    "Plano" to List(10) { 0f },
    "Bajos" to listOf(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f),
    "Agudos" to listOf(0f, 0f, 0f, 0f, 0f, 1f, 3f, 4f, 5f, 6f),
    "Rock" to listOf(5f, 4f, 2f, 0f, -1f, 0f, 2f, 4f, 5f, 5f),
    "Pop" to listOf(-1f, 1f, 3f, 4f, 3f, 0f, -1f, -1f, 1f, 2f),
    "Vocal" to listOf(-2f, -2f, -1f, 2f, 4f, 4f, 3f, 1f, 0f, -1f),
    "Electrónica" to listOf(6f, 5f, 2f, 0f, -2f, 2f, 1f, 3f, 5f, 5f),
    "Clásica" to listOf(0f, 0f, 0f, 0f, 0f, 0f, -2f, -2f, -2f, -4f)
)

// ---------- Preferencias ----------
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("liquid", Context.MODE_PRIVATE)
    var skin by mutableIntStateOf(sp.getInt("skin", 0).coerceIn(0, Skins.lastIndex))
    var coverFrac by mutableFloatStateOf(sp.getFloat("coverFrac", 0.78f))
    var blurBg by mutableStateOf(sp.getBoolean("blurBg", true))
    var blurAmount by mutableFloatStateOf(sp.getFloat("blurAmount", 28f))
    var bitPerfect by mutableStateOf(sp.getBoolean("bitPerfect", false))
    var eqOn by mutableStateOf(sp.getBoolean("eqOn", false))
    var eq by mutableStateOf(
        sp.getString("eq", null)?.split(",")?.mapNotNull { it.toFloatOrNull() }?.takeIf { it.size == 10 }
            ?: List(10) { 0f }
    )
    var hidden by mutableStateOf(sp.getStringSet("hidden", emptySet())!!.toSet())
    var favs by mutableStateOf(sp.getString("favs", "")!!.split(",").mapNotNull { it.toLongOrNull() }.toSet())

    fun show(b: String) = b !in hidden

    fun upd(block: Prefs.() -> Unit) {
        block()
        sp.edit()
            .putInt("skin", skin).putFloat("coverFrac", coverFrac).putBoolean("blurBg", blurBg)
            .putFloat("blurAmount", blurAmount).putBoolean("bitPerfect", bitPerfect).putBoolean("eqOn", eqOn)
            .putString("eq", eq.joinToString(",")).putStringSet("hidden", hidden)
            .putString("favs", favs.joinToString(","))
            .apply()
    }
}

// ---------- Estado de la app / control del reproductor ----------
class AppState(val ctx: Context) {
    val prefs = Prefs(ctx)
    val player: ExoPlayer = PlayerHolder.get(ctx)
    var songs by mutableStateOf<List<Song>>(emptyList())
    var index by mutableIntStateOf(-1)
    var playing by mutableStateOf(false)
    var shuffle by mutableStateOf(false)
    var repeat by mutableIntStateOf(Player.REPEAT_MODE_OFF)
    var pos by mutableLongStateOf(0L)
    var dur by mutableLongStateOf(0L)
    var bpStatus by mutableStateOf("")
    val current: Song? get() = songs.getOrNull(index)

    init {
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                    index = player.currentMediaItemIndex
                    pos = 0
                    dur = player.duration.coerceAtLeast(0)
                }
            }
            override fun onPlaybackStateChanged(playbackState: Int) { dur = player.duration.coerceAtLeast(0) }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
                if (isPlaying && prefs.bitPerfect) bpStatus = BitPerfect.apply(ctx, true)
            }
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) { shuffle = shuffleModeEnabled }
            override fun onRepeatModeChanged(repeatMode: Int) { repeat = repeatMode }
        })
        try { ctx.startService(Intent(ctx, PlaybackService::class.java)) } catch (_: Exception) {}
    }

    fun setSongs(list: List<Song>) {
        songs = list
        if (player.mediaItemCount != list.size) {
            player.setMediaItems(list.map { it.toItem() })
            player.prepare()
        }
        index = if (list.isNotEmpty() && player.mediaItemCount == list.size &&
            (player.isPlaying || player.currentPosition > 0)) player.currentMediaItemIndex else -1
        shuffle = player.shuffleModeEnabled
        repeat = player.repeatMode
        playing = player.isPlaying
    }

    fun play(i: Int) {
        if (i !in songs.indices) return
        player.seekTo(i, 0)
        player.play()
        index = i
        pos = 0
    }
    fun toggle() { if (player.isPlaying) player.pause() else if (index < 0) play(0) else player.play() }
    fun next() { if (player.hasNextMediaItem()) player.seekToNextMediaItem() else player.seekTo(0, 0) }
    fun prev() { if (player.currentPosition > 3000) player.seekTo(0) else player.seekToPreviousMediaItem() }
    fun seek(ms: Long) { player.seekTo(ms); pos = ms }
    fun toggleShuffle() { player.shuffleModeEnabled = !player.shuffleModeEnabled }
    fun cycleRepeat() {
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }
    fun toggleFav(id: Long) = prefs.upd { favs = if (id in favs) favs - id else favs + id }
}
