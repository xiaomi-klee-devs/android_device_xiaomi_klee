package com.xiaomi.settings.light

import android.util.Log
import com.xiaomi.settings.utils.writeLine

object LedManager {
    private const val TAG = "LedManager"
    
    private const val LED_PATH = "/sys/class/leds/aw21024_led"
    private const val HWEN_NODE = "$LED_PATH/hwen"
    private const val RUN_NODE = "$LED_PATH/run"
    private const val REPEAT_NODE = "$LED_PATH/repeat"
    private const val PERIOD_NODE = "$LED_PATH/period"
    private const val BRIGHTNESS_NODE = "$LED_PATH/brightness"
    private const val RGBCOLOR_NODE = "$LED_PATH/rgbcolor"
    private const val GRADIENT_NODE = "$LED_PATH/gradient"

    private var isActive = false
    private var lastRunMode = -1
    private var lastColorHex = ""
    private var lastRiseMs = -1
    private var lastOnMs = -1
    private var lastFallMs = -1
    private var lastOffMs = -1
    private var lastRepeat = false
    private var lastOffTimeMs = 0L

    private fun powerOnIfNeeded(runMode: Int) {
        if (!isActive) {
            val now = System.currentTimeMillis()
            val elapsed = now - lastOffTimeMs
            if (elapsed < 15) {
                Thread.sleep(15 - elapsed)
            }
            Log.i(TAG, "powerOnIfNeeded: Turning ON with runMode=$runMode")
            writeLine(HWEN_NODE, "1")
            isActive = true
            writeLine(RUN_NODE, runMode.toString())
            lastRunMode = runMode
        } else if (lastRunMode != runMode) {
            Log.i(TAG, "powerOnIfNeeded: Changing runMode from $lastRunMode to $runMode")
            writeLine(RUN_NODE, "0")
            writeLine(HWEN_NODE, "0")
            Thread.sleep(15) // Settle time
            writeLine(HWEN_NODE, "1")
            writeLine(RUN_NODE, runMode.toString())
            lastRunMode = runMode
        }
    }

    private val dynamicColors = arrayOf("ff0000", "ff7f00", "ffff00", "00ff00", "00ffff", "0000ff", "800080", "ff00ff")

    private fun setDeviceColor(colorHex: String) {
        writeLine(RGBCOLOR_NODE, "0x04 0x$colorHex")
        writeLine(RGBCOLOR_NODE, "0x03 0x$colorHex")
        writeLine(BRIGHTNESS_NODE, "80")
    }

    private fun setDeviceDualColor(topColorHex: String, bottomColorHex: String) {
        writeLine(RGBCOLOR_NODE, "0x04 0x$bottomColorHex")
        writeLine(RGBCOLOR_NODE, "0x03 0x$topColorHex")
        writeLine(BRIGHTNESS_NODE, "80")
    }

    fun turnOff() {
        if (!isActive) return
        Log.i(TAG, "turnOff: Turning OFF LEDs")
        writeLine(RUN_NODE, "0")
        writeLine(GRADIENT_NODE, "0")
        writeLine(REPEAT_NODE, "0")
        writeLine(HWEN_NODE, "0")
        isActive = false
        lastRunMode = -1
        lastColorHex = ""
        lastOffTimeMs = System.currentTimeMillis()
    }

    fun setVisualizerActive() {
        Log.i(TAG, "setVisualizerActive")
        powerOnIfNeeded(1) // 1 = always-on (dynamic brightness)
        writeLine(GRADIENT_NODE, "0")
    }

    fun setStaticColor(colorHex: String) {
        if (isActive && lastRunMode == 1 && lastColorHex == colorHex) {
            return
        }
        Log.i(TAG, "setStaticColor: color=$colorHex")
        powerOnIfNeeded(1) // 1 = always-on
        setDeviceColor(colorHex)
        lastColorHex = colorHex
    }

    fun setBlink(colorHex: String, riseMs: Int, onMs: Int, fallMs: Int, offMs: Int, repeat: Boolean) {
        if (isActive && lastRunMode == 2 && lastColorHex == colorHex &&
            lastRiseMs == riseMs && lastOnMs == onMs && lastFallMs == fallMs &&
            lastOffMs == offMs && lastRepeat == repeat) {
            return
        }
        Log.i(TAG, "setBlink: color=$colorHex, repeat=$repeat")
        powerOnIfNeeded(2) // 2 = blink/breathe
        if (repeat) {
            writeLine(REPEAT_NODE, "1")
        } else {
            writeLine(REPEAT_NODE, "0")
        }
        writeLine(PERIOD_NODE, "$riseMs $onMs $fallMs $offMs")
        setDeviceColor(colorHex)
        
        lastColorHex = colorHex
        lastRiseMs = riseMs
        lastOnMs = onMs
        lastFallMs = fallMs
        lastOffMs = offMs
        lastRepeat = repeat
    }

    fun setDynamicBlink(riseMs: Int, onMs: Int, fallMs: Int, offMs: Int, repeat: Boolean) {
        Log.i(TAG, "setDynamicBlink")
        powerOnIfNeeded(2) // 2 = blink/breathe
        if (repeat) {
            writeLine(REPEAT_NODE, "1")
        } else {
            writeLine(REPEAT_NODE, "0")
        }
        writeLine(PERIOD_NODE, "$riseMs $onMs $fallMs $offMs")

        val topColor = dynamicColors.random()
        var bottomColor = dynamicColors.random()
        while (bottomColor == topColor) {
            bottomColor = dynamicColors.random()
        }
        setDeviceDualColor(topColor, bottomColor)

        // "dynamic" is never a real hex value, so the equality checks in
        // setBlink/setStaticColor never accidentally short-circuit this mode,
        // and a fresh random pair is written on every call.
        lastColorHex = "dynamic"
        lastRiseMs = riseMs
        lastOnMs = onMs
        lastFallMs = fallMs
        lastOffMs = offMs
        lastRepeat = repeat
    }

    fun setGradientSweep(enable: Boolean) {
        if (enable) {
            if (isActive && lastRunMode == 3) {
                return
            }
            Log.i(TAG, "setGradientSweep: enable=$enable")
            powerOnIfNeeded(3) // 3 = gradient
            writeLine(GRADIENT_NODE, "1")
        } else {
            turnOff()
        }
    }
}
