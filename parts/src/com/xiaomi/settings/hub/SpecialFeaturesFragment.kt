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

package com.xiaomi.settings.hub

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import com.xiaomi.settings.R
import com.xiaomi.settings.corecontrol.CoreControlActivity
import com.xiaomi.settings.gamebar.GameBarSettingsActivity
import com.xiaomi.settings.light.LightSettingsActivity
import com.xiaomi.settings.touchsampling.TouchSamplingSettingsActivity

class SpecialFeaturesFragment : SettingsBasePreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.special_features, rootKey)

        findPreference<Preference>(KEY_BACKLIGHT)?.setOnPreferenceClickListener {
            startActivity(Intent(context, LightSettingsActivity::class.java))
            true
        }

        findPreference<Preference>(KEY_GAME_BAR)?.setOnPreferenceClickListener {
            startActivity(Intent(context, GameBarSettingsActivity::class.java))
            true
        }

        findPreference<Preference>(KEY_TOUCH_SAMPLING)?.setOnPreferenceClickListener {
            startActivity(Intent(context, TouchSamplingSettingsActivity::class.java))
            true
        }

        findPreference<Preference>(KEY_CORE_CONTROL)?.setOnPreferenceClickListener {
            startActivity(Intent(context, CoreControlActivity::class.java))
            true
        }
    }

    companion object {
        private const val KEY_BACKLIGHT = "special_backlight"
        private const val KEY_GAME_BAR = "special_game_bar"
        private const val KEY_TOUCH_SAMPLING = "special_touch_sampling"
        private const val KEY_CORE_CONTROL = "special_core_control"
    }
}
