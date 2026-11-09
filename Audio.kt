package com.liquid.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioMixerAttributes
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/** Ecualizador paramétrico de 10 bandas (filtros peaking biquad), procesa PCM 16 bits. */
class EqProcessor : BaseAudioProcessor() {
    companion object {
        val FREQS = floatArrayOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
        val LABELS = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
    }

    @Volatile var enabled = false
    @Volatile private var gains = FloatArray(10)
    @Volatile private var dirty = true
    private var rate = 44100
    private var ch = 2
    private val b0 = FloatArray(10)
    private val b1 = FloatArray(10)
    private val b2 = FloatArray(10)
    private val a1 = FloatArray(10)
    private val a2 = FloatArray(10)
    private val on = BooleanArray(10)
    private var z1 = FloatArray(20)
    private var z2 = FloatArray(20)
    private var pre = 1f

    fun setGains(g: List<Float>) { gains = g.toFloatArray(); dirty = true }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) return AudioProcessor.AudioFormat.NOT_SET
        rate = inputAudioFormat.sampleRate
        ch = inputAudioFormat.channelCount
        z1 = FloatArray(ch * 10)
        z2 = FloatArray(ch * 10)
        dirty = true
        return inputAudioFormat
    }

    override fun onFlush() { z1.fill(0f); z2.fill(0f) }

    private fun recompute() {
        dirty = false
        val g = gains
        var maxBoost = 0f
        for (i in 0 until 10) {
            val f = FREQS[i]
            on[i] = abs(g[i]) > 0.05f && f < rate * 0.45f
            if (g[i] > maxBoost) maxBoost = g[i]
            if (!on[i]) continue
            val amp = 10.0.pow(g[i] / 40.0)
            val w0 = 2.0 * PI * f / rate
            val alpha = sin(w0) / (2.0 * 1.41)
            val a0 = 1.0 + alpha / amp
            b0[i] = ((1.0 + alpha * amp) / a0).toFloat()
            b1[i] = ((-2.0 * cos(w0)) / a0).toFloat()
            b2[i] = ((1.0 - alpha * amp) / a0).toFloat()
            a1[i] = b1[i]
            a2[i] = ((1.0 - alpha / amp) / a0).toFloat()
        }
        pre = 10f.pow(-maxBoost / 20f) // evita saturación al subir bandas
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        val out = replaceOutputBuffer(size)
        if (!enabled) {
            out.put(inputBuffer)
            out.flip()
            return
        }
        if (dirty) recompute()
        var idx = 0
        while (inputBuffer.hasRemaining()) {
            var x = inputBuffer.short.toFloat() * pre
            val base = (idx % ch) * 10
            for (b in 0 until 10) {
                if (!on[b]) continue
                val y = b0[b] * x + z1[base + b]
                z1[base + b] = b1[b] * x - a1[b] * y + z2[base + b]
                z2[base + b] = b2[b] * x - a2[b] * y
                x = y
            }
            out.putShort(x.roundToInt().coerceIn(-32768, 32767).toShort())
            idx++
        }
        out.flip()
    }
}

/** Bit-perfect real: Android 14+ con DAC/auriculares USB (mezclador bit-perfect del sistema). */
object BitPerfect {
    fun apply(ctx: Context, enable: Boolean): String {
        if (Build.VERSION.SDK_INT < 34) return "Requiere Android 14 o superior"
        val am = ctx.getSystemService(AudioManager::class.java)
        val attrs = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val dev: AudioDeviceInfo = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull {
            it.type == AudioDeviceInfo.TYPE_USB_DEVICE || it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        } ?: return if (enable) "Conecta un DAC o auriculares USB" else "Desactivado"
        return try {
            if (!enable) {
                am.clearPreferredMixerAttributes(attrs, dev)
                "Desactivado"
            } else {
                val mixer = am.getSupportedMixerAttributes(dev)
                    .firstOrNull { it.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT }
                    ?: return "Tu DAC no soporta bit-perfect"
                if (am.setPreferredMixerAttributes(attrs, dev, mixer))
                    "Bit-perfect activo (${mixer.format.sampleRate} Hz)"
                else "No se pudo activar"
            }
        } catch (e: Exception) { "Error: ${e.message}" }
    }
}

object PlayerHolder {
    lateinit var eq: EqProcessor
    private var player: ExoPlayer? = null

    fun get(context: Context): ExoPlayer {
        player?.let { return it }
        val app = context.applicationContext
        eq = EqProcessor()
        val factory = object : DefaultRenderersFactory(app) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink = DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(false)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessors(arrayOf<AudioProcessor>(eq))
                .build()
        }
        return ExoPlayer.Builder(app, factory)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build().also { player = it }
    }
}

class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        session = MediaSession.Builder(this, PlayerHolder.get(this)).setSessionActivity(open).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.release()
        session = null
        super.onDestroy()
    }
}
