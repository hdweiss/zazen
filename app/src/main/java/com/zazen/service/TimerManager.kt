package com.zazen.service

import android.content.Context
import android.content.Intent
import com.zazen.data.model.TimerConfig
import com.zazen.data.model.TimerState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-scoped façade for controlling the timer.
 * ViewModels use this instead of touching the service directly.
 */
@Singleton
class TimerManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val state: StateFlow<TimerState> get() = TimerService.timerState
    val config: TimerConfig? get() = TimerService.currentConfig
    val vibrateOnly: StateFlow<Boolean> get() = TimerService.vibrateOnly

    fun start(config: TimerConfig) {
        // A second tap on Start (e.g. while the setup screen fades out) must not
        // restart the sit that's already under way.
        val current = state.value
        if (current is TimerState.Running || current is TimerState.Paused) return
        TimerService.markStarting(config)
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_CONFIG, config)
        }
        context.startForegroundService(intent)
    }

    fun pause() = sendCommand(TimerService.ACTION_PAUSE)

    fun resume() = sendCommand(TimerService.ACTION_RESUME)

    fun stop() = sendCommand(TimerService.ACTION_STOP)

    fun toggleVibrateOnly() = TimerService.toggleVibrateOnly()

    /** Move from Finished back to Idle */
    fun reset() {
        TimerService.resetState()
    }

    private fun sendCommand(action: String) {
        context.startService(
            Intent(context, TimerService::class.java).apply { this.action = action }
        )
    }
}
