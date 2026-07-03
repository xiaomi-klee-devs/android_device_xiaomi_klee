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
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import androidx.preference.SwitchPreferenceCompat
import com.xiaomi.settings.R

class TouchSamplingSettingsFragment : SettingsBasePreferenceFragment(), Preference.OnPreferenceChangeListener {

    private var mHTSRPreference: SwitchPreferenceCompat? = null
    private var mAutoEnablePreference: SwitchPreferenceCompat? = null
    private var mChooseAppsPreference: Preference? = null
    private var mPrefs: SharedPreferences? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.htsr_settings, rootKey)
        activity?.actionBar?.setDisplayHomeAsUpEnabled(true)

        mHTSRPreference = findPreference<SwitchPreferenceCompat>(HTSR_ENABLE_KEY)
        mAutoEnablePreference = findPreference<SwitchPreferenceCompat>(HTSR_AUTO_ENABLE_KEY)
        mChooseAppsPreference = findPreference(HTSR_APP_SELECTOR_KEY)
        mPrefs = activity?.getSharedPreferences(SHAREDHTSR, Context.MODE_PRIVATE)

        // Set the initial state of the switch
        val htsrEnabled = mPrefs!!.getBoolean(HTSR_STATE, false)
        mHTSRPreference?.isChecked = htsrEnabled

        // Enable the switch and set its listener
        mHTSRPreference?.setOnPreferenceChangeListener(this)

        // Start the service if the toggle is enabled
        if (htsrEnabled) {
            startTouchSamplingService(true)
        }

        mChooseAppsPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = Intent(activity, TouchSamplingAppSelectorActivity::class.java)
            startActivity(intent)
            true
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
            activity?.startService(serviceIntent)
        } else {
            activity?.stopService(serviceIntent)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            activity?.onBackPressed()
            return true
        }
        return false
    }

    companion object {
        private const val HTSR_ENABLE_KEY = "htsr_enable"
        const val SHAREDHTSR = "SHAREDHTSR"
        const val HTSR_STATE = "htsr_state"
        const val HTSR_AUTO_ENABLE_KEY = "htsr_auto_enable"
        const val HTSR_APP_SELECTOR_KEY = "htsr_app_selector"
        const val HTSR_APPS_PREF = "htsr_auto_apps"
    }
}
