package org.treeo.treeo.ui.treemeasurement.screens

import android.content.Context
import android.media.MediaPlayer
import android.widget.ImageView
import android.widget.SeekBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.treeo.treeo.R
import org.treeo.treeo.databinding.AudioLayoutBinding
import java.util.concurrent.TimeUnit

class AudioGuideUtils(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val audioLayout: AudioLayoutBinding
) {

    private val mediaPlayer by lazy {
        MediaPlayer.create(context, R.raw.treeo_camera_guide)
    }
    private var updateSeekBarWithAudioProgress = false

    fun setupAudioGuide() {
        mediaPlayer.run {
            displayAudioInfo()
            handlePlayPause()
            setupSeekBar()
        }
    }

    private fun MediaPlayer.displayAudioInfo() {
        audioLayout.apply {
            progressSeekBar.max = duration
            progressSeekBar.progress = currentPosition

            val minutes =
                TimeUnit.MILLISECONDS.toMinutes(duration.toLong())
            val seconds =
                TimeUnit.MILLISECONDS.toSeconds(duration.toLong()) - TimeUnit.MINUTES.toSeconds(
                    minutes
                )
            endTime.text = String.format(
                context.getString(R.string.audio_end_time_placeholder),
                minutes,
                seconds
            )
        }
    }

    private fun MediaPlayer.handlePlayPause() {
        audioLayout.buttonPlayPause.setOnClickListener {
            val imageView = it as ImageView
            if (isPlaying) {
                pauseAudioGuide()
            } else {
                imageView.setImageResource(R.drawable.ic_baseline_pause_circle_filled_24)
                start()
                updateSeekBarBasedOnAudioProgress()
            }
        }
    }

    fun pauseAudioGuide() {
        updateSeekBarWithAudioProgress = false
        val imageView = audioLayout.buttonPlayPause
        imageView.setImageResource(R.drawable.ic_baseline_play_circle_filled_24)
        mediaPlayer.pause()
    }

    fun updateSeekBarBasedOnAudioProgress() {
        coroutineScope.launch {
            try {
                updateSeekBarWithAudioProgress = true
                while (updateSeekBarWithAudioProgress) {
                    delay(300)
                    audioLayout.progressSeekBar.progress =
                        mediaPlayer.currentPosition

                    if (!mediaPlayer.isPlaying) {
                        updateSeekBarWithAudioProgress = false
                        val imageView = audioLayout.buttonPlayPause
                        imageView.setImageResource(R.drawable.ic_baseline_play_circle_filled_24)
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun MediaPlayer.setupSeekBar() {
        audioLayout.progressSeekBar.setOnSeekBarChangeListener(
            object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    updateSeekBarWithAudioProgress = false
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    if (seekBar != null) {
                        seekTo(seekBar.progress)
                    }
                    updateSeekBarBasedOnAudioProgress()
                }
            })
    }
}
