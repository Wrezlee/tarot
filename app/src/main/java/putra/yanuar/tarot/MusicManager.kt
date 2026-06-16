package putra.yanuar.tarot

import android.content.Context
import android.media.MediaPlayer

object MusicManager {

    private var mediaPlayer: MediaPlayer? = null
    private var isMuted = false

    fun start(context: Context) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context.applicationContext, R.raw.bgm_mistis)
            mediaPlayer?.isLooping = true
            mediaPlayer?.setVolume(0.4f, 0.4f)
        }
        if (!isMuted && mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
        }
    }

    fun pause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }

    fun resume() {
        if (!isMuted && mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
        }
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        if (isMuted) {
            mediaPlayer?.setVolume(0f, 0f)
        } else {
            mediaPlayer?.setVolume(0.4f, 0.4f)
        }
        return isMuted
    }

    fun isMuted(): Boolean = isMuted

    fun release() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}