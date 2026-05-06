package com.xiaomi.settings.light

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.audiofx.Visualizer
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.BatteryManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.preference.PreferenceManager
import com.xiaomi.settings.gamebar.ForegroundAppDetector
import com.xiaomi.settings.utils.writeLine
import java.util.HashSet
import kotlin.math.abs

class LightService : Service() {

    companion object {
        private const val TAG = "LightService"
    }

    private lateinit var telephonyManager: TelephonyManager
    private lateinit var audioManager: AudioManager
    private lateinit var sharedPreferences: SharedPreferences
    private var mediaSessionManager: MediaSessionManager? = null

    @Volatile private var isRinging = false
    @Volatile private var isMusicActive = false
    @Volatile private var isGameModeActive = false
    @Volatile private var isCharging = false
    @Volatile private var chargingColorHex: String? = null
    @Volatile private var isServiceRunning = false
    @Volatile private var isNotificationPulsing = false

    private var visualizer: Visualizer? = null
    private var isVisualizerActive = false

    private lateinit var powerManager: PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val notificationTimeoutRunnable = Runnable {
        Log.i(TAG, "Notification pulse timeout reached")
        isNotificationPulsing = false
        postUpdateLedState()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    private var visualizerHandlerThread: HandlerThread? = null
    private var visualizerHandler: Handler? = null

    private var ledHandlerThread: HandlerThread? = null
    private var ledHandler: Handler? = null

    private val activeMediaControllers = mutableListOf<MediaController>()

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "light_enable",
            "light_music_enable",
            "light_music_style",
            "light_game_mode_enable",
            "light_charging_enable",
            "light_incoming_call_enable",
            "light_incoming_call_color",
            "light_standalone_enable",
            "light_standalone_color",
            "light_notifications_enable",
            "light_notifications_color" -> {
                Log.i(TAG, "Preference changed: $key")
                if (key == "light_music_enable" || key == "light_music_apps") {
                    checkMusicState()
                }
                postUpdateLedState()
            }
        }
    }

    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            super.onCallStateChanged(state, phoneNumber)
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    Log.i(TAG, "Phone ringing")
                    isRinging = true
                    postUpdateLedState()
                }
                TelephonyManager.CALL_STATE_IDLE, TelephonyManager.CALL_STATE_OFFHOOK -> {
                    Log.i(TAG, "Phone idle/offhook")
                    isRinging = false
                    postUpdateLedState()
                }
            }
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCurrentlyCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                      status == BatteryManager.BATTERY_STATUS_FULL

            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = level * 100 / scale.toFloat()

            val newChargingColorHex = when {
                batteryPct <= 25 -> "ff0000" // Red
                batteryPct <= 50 -> "ff7f00" // Orange
                batteryPct <= 75 -> "0000ff" // Blue
                else -> "800080"             // Purple
            }

            if (isCharging != isCurrentlyCharging || chargingColorHex != newChargingColorHex) {
                Log.i(TAG, "Battery state changed: charging=$isCurrentlyCharging, level=$batteryPct%")
            }

            isCharging = isCurrentlyCharging
            chargingColorHex = newChargingColorHex
            postUpdateLedState()
        }
    }

    private val mediaControllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            Log.i(TAG, "MediaController.Callback: onPlaybackStateChanged state=${state?.state}")
            checkMusicState()
        }
    }

    private val activeSessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        Log.i(TAG, "MediaSessionManager: OnActiveSessionsChangedListener triggered")
        updateMediaControllers(controllers)
    }

    private fun updateMediaControllers(controllers: List<MediaController>?) {
        // unregister from old
        for (controller in activeMediaControllers) {
            controller.unregisterCallback(mediaControllerCallback)
        }
        activeMediaControllers.clear()
        
        // register to new
        controllers?.forEach { controller ->
            controller.registerCallback(mediaControllerCallback)
            activeMediaControllers.add(controller)
        }
        checkMusicState()
    }

    private fun checkMusicState() {
        val musicEnabled = sharedPreferences.getBoolean("light_music_enable", false)
        if (!musicEnabled) {
            if (isMusicActive) {
                Log.i(TAG, "checkMusicState: Music disabled, but isMusicActive was true")
                isMusicActive = false
                postUpdateLedState()
            }
            return
        }

        var isValidMusicPlaying = false
        val musicApps = sharedPreferences.getStringSet("light_music_apps", HashSet()) ?: HashSet()

        try {
            for (controller in activeMediaControllers) {
                if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
                    if (musicApps.contains(controller.packageName)) {
                        isValidMusicPlaying = true
                        Log.i(TAG, "checkMusicState: Valid music playing from ${controller.packageName}")
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "checkMusicState: Error checking media controllers", e)
            val foreground = ForegroundAppDetector.getForegroundPackageName(this)
            isValidMusicPlaying = musicApps.contains(foreground) && audioManager.isMusicActive
        }

        if (isValidMusicPlaying != isMusicActive) {
            Log.i(TAG, "checkMusicState: isMusicActive changed from $isMusicActive to $isValidMusicPlaying")
            isMusicActive = isValidMusicPlaying
            postUpdateLedState()
        }
    }

    private fun postUpdateLedState() {
        ledHandler?.post { updateLedState() }
    }

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LightService:NotificationPulse")

        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)

        sharedPreferences.registerOnSharedPreferenceChangeListener(prefListener)
        visualizerHandlerThread = HandlerThread("VisualizerThread").apply { start() }
        visualizerHandler = Handler(visualizerHandlerThread!!.looper)

        ledHandlerThread = HandlerThread("LedHandlerThread").apply { start() }
        ledHandler = Handler(ledHandlerThread!!.looper)

        isServiceRunning = true

        try {
            val sessions = mediaSessionManager?.getActiveSessions(null)
            updateMediaControllers(sessions)
            mediaSessionManager?.addOnActiveSessionsChangedListener(activeSessionsListener, null)
            Log.i(TAG, "Registered MediaSessionManager listener")
        } catch (e: SecurityException) {
            Log.w(TAG, "Failed to register MediaSessionManager listener, missing permissions", e)
        }

        ledHandler?.postDelayed(backgroundStateRunnable, 2000)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_PULSE_NOTIFICATION") {
            Log.i(TAG, "Received ACTION_PULSE_NOTIFICATION")
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(6000)
            }
            if (!isNotificationPulsing) {
                isNotificationPulsing = true
                postUpdateLedState()
            }
            mainHandler.removeCallbacks(notificationTimeoutRunnable)
            mainHandler.postDelayed(notificationTimeoutRunnable, 5000)
        } else {
            postUpdateLedState()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy: stopping service")
        isServiceRunning = false
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        unregisterReceiver(batteryReceiver)
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(prefListener)
        
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(activeSessionsListener)
        } catch (e: Exception) {
            Log.w(TAG, "Error removing MediaSessionManager listener", e)
        }
        updateMediaControllers(emptyList())

        visualizerHandlerThread?.quitSafely()
        ledHandlerThread?.quitSafely()
        stopVisualizer()
        LedManager.turnOff()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private val backgroundStateRunnable = object : Runnable {
        override fun run() {
            if (!isServiceRunning) return

            var stateChanged = false

            // Check Game Mode
            val gameEnabled = sharedPreferences.getBoolean("light_game_mode_enable", false)
            if (gameEnabled) {
                val foreground = ForegroundAppDetector.getForegroundPackageName(this@LightService)
                val gameApps = sharedPreferences.getStringSet("light_game_mode_apps", HashSet())!!
                val currentlyInGame = gameApps.contains(foreground)
                if (currentlyInGame != isGameModeActive) {
                    Log.i(TAG, "Game mode active changed from $isGameModeActive to $currentlyInGame")
                    isGameModeActive = currentlyInGame
                    stateChanged = true
                }
            } else if (isGameModeActive) {
                Log.i(TAG, "Game mode disabled, disabling game mode state")
                isGameModeActive = false
                stateChanged = true
            }

            // Fallback for music checking if media session manager didn't catch it
            val musicEnabled = sharedPreferences.getBoolean("light_music_enable", false)
            if (musicEnabled && activeMediaControllers.isEmpty() && audioManager.isMusicActive) {
                 val foreground = ForegroundAppDetector.getForegroundPackageName(this@LightService)
                 val musicApps = sharedPreferences.getStringSet("light_music_apps", HashSet()) ?: HashSet()
                 val isValidFallback = musicApps.contains(foreground)
                 if (isValidFallback != isMusicActive) {
                     Log.i(TAG, "Music active (fallback) changed from $isMusicActive to $isValidFallback")
                     isMusicActive = isValidFallback
                     stateChanged = true
                 }
            }

            if (stateChanged) {
                updateLedState() // since this runnable runs on ledHandler
            }

            ledHandler?.postDelayed(this, 2000)
        }
    }

    private val availableColors = arrayOf("ff0000", "ff7f00", "ffff00", "00ff00", "00ffff", "0000ff", "800080")
    private var lastColorChangeTime = 0L

    private fun startVisualizer() {
        if (isVisualizerActive) return
        Log.i(TAG, "startVisualizer")
        try {
            visualizer = Visualizer(0)
            visualizer?.captureSize = Visualizer.getCaptureSizeRange()[1]
            visualizer?.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {}

                override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                    if (fft == null || fft.isEmpty()) return
                    
                    val magnitudes = FloatArray(fft.size / 2)
                    var maxIdx = 0
                    for (i in magnitudes.indices) {
                        magnitudes[i] = Math.hypot(fft[i * 2].toDouble(), fft[i * 2 + 1].toDouble()).toFloat()
                        if (magnitudes[maxIdx] < magnitudes[i]) {
                            maxIdx = i
                        }
                    }

                    // Map the dominant magnitude to brightness 0..80.
                    // Magnitudes typically range from 0 to ~128+.
                    var brightness = (magnitudes[maxIdx] * 0.8f).toInt()
                    if (brightness > 80) brightness = 80
                    if (brightness < 0) brightness = 0

                    visualizerHandler?.removeCallbacksAndMessages(null)
                    visualizerHandler?.post {
                        LedManager.setVisualizerActive()
                        if (brightness > 20) {
                            val now = System.currentTimeMillis()
                            // Debounce color changes
                            if (now - lastColorChangeTime > 200) {
                                val randomColor = availableColors.random()
                                writeLine("/sys/class/leds/aw21024_led/rgbcolor", "0x04 0x$randomColor")
                                writeLine("/sys/class/leds/aw21024_led/rgbcolor", "0x03 0x$randomColor")
                                lastColorChangeTime = now
                            }
                        }

                        writeLine("/sys/class/leds/aw21024_led/brightness", brightness.toString())
                    }
                }
            }, Visualizer.getMaxCaptureRate() / 2, false, true)
            visualizer?.enabled = true
            isVisualizerActive = true
        } catch (e: Exception) {
            e.printStackTrace()
            isVisualizerActive = false
        }
    }

    private fun stopVisualizer() {
        if (!isVisualizerActive) return
        Log.i(TAG, "stopVisualizer")
        try {
            visualizer?.enabled = false
            visualizer?.release()
            visualizer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isVisualizerActive = false
    }

    private fun stopAllEffects() {
        stopVisualizer()
        LedManager.turnOff()
    }

    private fun updateLedState() {
        val masterEnabled = sharedPreferences.getBoolean("light_enable", false)
        if (!masterEnabled) {
            Log.i(TAG, "updateLedState: Master toggle disabled, stopping all effects")
            stopAllEffects()
            return
        }

        // Priority 1: Incoming Call
        if (isRinging) {
            val callEnabled = sharedPreferences.getBoolean("light_incoming_call_enable", false)
            if (callEnabled) {
                Log.i(TAG, "updateLedState: Incoming call priority")
                stopAllEffects()
                val color = sharedPreferences.getString("light_incoming_call_color", "ff0000") ?: "ff0000"
                if (color == "gradient") LedManager.setGradientSweep(true)
                else LedManager.setBlink(color, 1000, 1000, 1000, 1000, true)
                return
            }
        }

        // Priority 2: Notification Pulse
        if (isNotificationPulsing) {
            val notifEnabled = sharedPreferences.getBoolean("light_notifications_enable", false)
            if (notifEnabled) {
                Log.i(TAG, "updateLedState: Notification pulse priority")
                stopAllEffects()
                val color = sharedPreferences.getString("light_notifications_color", "ff0000") ?: "ff0000"
                if (color == "gradient") LedManager.setGradientSweep(true)
                else LedManager.setBlink(color, 1000, 1000, 1000, 1000, true)
                return
            }
        }

        // Priority 3: Game Mode
        if (isGameModeActive) {
            val gameEnabled = sharedPreferences.getBoolean("light_game_mode_enable", false)
            if (gameEnabled) {
                Log.i(TAG, "updateLedState: Game mode priority")
                stopAllEffects()
                LedManager.setGradientSweep(true)
                return
            }
        }

        // Priority 4: Charging
        if (isCharging) {
            val chargingEnabled = sharedPreferences.getBoolean("light_charging_enable", false)
            if (chargingEnabled && chargingColorHex != null) {
                Log.i(TAG, "updateLedState: Charging priority")
                stopAllEffects()
                LedManager.setBlink(chargingColorHex!!, 2000, 1000, 2000, 1000, true)
                return
            }
        }

        // Priority 5: Music Visualizer
        if (isMusicActive) {
            val musicEnabled = sharedPreferences.getBoolean("light_music_enable", false)
            if (musicEnabled) {
                Log.i(TAG, "updateLedState: Music visualizer priority")
                // Stop gradient/blink if standalone or game was previously active
                if (!isVisualizerActive) {
                    LedManager.turnOff()
                    startVisualizer()
                }
                return
            }
        }

        // Priority 6: Standalone Color
        val standaloneEnabled = sharedPreferences.getBoolean("light_standalone_enable", false)
        if (standaloneEnabled) {
            Log.i(TAG, "updateLedState: Standalone color priority")
            // Stop visualizer if music mode was previously active
            stopAllEffects()
            val color = sharedPreferences.getString("light_standalone_color", "ff0000") ?: "ff0000"
            if (color == "gradient") LedManager.setGradientSweep(true)
            else LedManager.setStaticColor(color)
            return
        }

        // Default: everything off
        stopAllEffects()
    }
}
