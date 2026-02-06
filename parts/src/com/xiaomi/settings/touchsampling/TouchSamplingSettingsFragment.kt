/*
 * Copyright (C) 2025 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaomi.settings.touchsampling

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import androidx.preference.Preference
import androidx.preference.PreferenceFragment
import androidx.preference.SwitchPreference
import com.xiaomi.settings.R

class TouchSamplingSettingsFragment : PreferenceFragment(), Preference.OnPreferenceChangeListener {

    private var mHTSRPreference: SwitchPreference? = null
    private var mPrefs: SharedPreferences? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.htsr_settings)
        activity.actionBar?.setDisplayHomeAsUpEnabled(true)

        mHTSRPreference = findPreference(HTSR_ENABLE_KEY) as SwitchPreference?
        mPrefs = activity.getSharedPreferences(SHAREDHTSR, Context.MODE_PRIVATE)

        // Set the initial state of the switch
        val htsrEnabled = mPrefs!!.getBoolean(HTSR_STATE, false)
        mHTSRPreference?.isChecked = htsrEnabled

        // Enable the switch and set its listener
        mHTSRPreference?.onPreferenceChangeListener = this

        // Start the service if the toggle is enabled
        if (htsrEnabled) {
            startTouchSamplingService(true)
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (HTSR_ENABLE_KEY == preference.key) {
            val isEnabled = newValue as Boolean

            // Save the state in shared preferences
            mPrefs!!.edit().putBoolean(HTSR_STATE, isEnabled).apply()

            // Start or stop the service based on the toggle state
            startTouchSamplingService(isEnabled)
        }
        return true
    }

    private fun startTouchSamplingService(enable: Boolean) {
        val serviceIntent = Intent(activity, TouchSamplingService::class.java)
        if (enable) {
            activity.startService(serviceIntent)
        } else {
            activity.stopService(serviceIntent)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            activity.onBackPressed()
            return true
        }
        return false
    }

    companion object {
        private const val HTSR_ENABLE_KEY = "htsr_enable"
        const val SHAREDHTSR = "SHAREDHTSR"
        const val HTSR_STATE = "htsr_state"
    }
}
