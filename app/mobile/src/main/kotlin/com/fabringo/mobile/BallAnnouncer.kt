package com.fabringo.mobile

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log

internal enum class Voice(val label: String, val assetDirectory: String) {
    Feminine("Femenina", "female"),
    Masculine("Masculina", "male"),
}

internal class BallAnnouncer(context: Context, private val onPlaying: (Boolean) -> Unit) {
    private val assets = context.assets
    private var active: MediaPlayer? = null

    fun announce(number: Int, voice: Voice) {
        stop()
        val player = MediaPlayer()
        active = player
        onPlaying(true)
        try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            assets.openFd("calls/${voice.assetDirectory}/${number.toString().padStart(2, '0')}.ogg").use {
                player.setDataSource(it.fileDescriptor, it.startOffset, it.length)
            }
            player.setOnPreparedListener { prepared ->
                if (active === prepared) {
                    prepared.start()
                    Log.d("FabringoAudio", "Reproduciendo bola $number (${voice.name})")
                }
            }
            player.setOnCompletionListener { completed ->
                if (active === completed) stop()
            }
            player.setOnErrorListener { failed, error, detail ->
                Log.w("FabringoAudio", "No se pudo reproducir la bola $number ($error, $detail)")
                if (active === failed) stop()
                true
            }
            player.prepareAsync()
        } catch (error: Exception) {
            stop()
            Log.w("FabringoAudio", "No se pudo abrir la locución de la bola $number", error)
        }
    }

    fun stop() {
        val player = active ?: return
        active = null
        player.release()
        onPlaying(false)
    }
}
