/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.corecontrol

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import com.xiaomi.settings.R
import java.io.File
import java.nio.file.Files

class CoreControlFragment : SettingsBasePreferenceFragment(), Preference.OnPreferenceChangeListener {

    companion object {
        private const val TAG = "CoreControlFragment"
        private const val NUM_CORES = 8
    }

    private val mCorePrefs = arrayOfNulls<SwitchPreferenceCompat>(NUM_CORES)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.core_control_settings, rootKey)

        for (i in 0 until NUM_CORES) {
            val key = "core_$i"
            mCorePrefs[i] = findPreference(key)
            mCorePrefs[i]?.apply {
                onPreferenceChangeListener = this@CoreControlFragment
                isChecked = isCoreOnline(i)
            }
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val requestedState = newValue as Boolean

        for (i in 0 until NUM_CORES) {
            if (preference === mCorePrefs[i]) {
                if (!requestedState && !canOffline(i)) {
                    Toast.makeText(
                        context,
                        "At least 2 little cores must remain online",
                        Toast.LENGTH_SHORT
                    ).show()
                    return false
                }
                setCoreState(i, requestedState)
                return true
            }
        }
        return false
    }

    private fun isCoreOnline(core: Int): Boolean {
        val path = "/sys/devices/system/cpu/cpu$core/online"
        return File(path).exists() && readFile(path) == "1"
    }

    private fun setCoreState(core: Int, online: Boolean) {
        writeFile("/sys/devices/system/cpu/cpu$core/online", if (online) "1" else "0")
    }

    private fun canOffline(core: Int): Boolean {
        if (core in 0..3) {
            var onlineCount = 0
            for (i in 0..3) {
                if (i != core && isCoreOnline(i)) onlineCount++
            }
            return onlineCount >= 2
        }
        return true
    }

    private fun readFile(path: String): String {
        return try {
            String(Files.readAllBytes(File(path).toPath())).trim()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read $path", e)
            ""
        }
    }

    private fun writeFile(path: String, value: String) {
        try {
            Files.write(File(path).toPath(), value.toByteArray())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write $path", e)
        }
    }
}
