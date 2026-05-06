package com.xiaomi.settings.light

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragment
import androidx.preference.SwitchPreference
import androidx.preference.ListPreference
import com.xiaomi.settings.R

class LightSettingsFragment : PreferenceFragment(), Preference.OnPreferenceChangeListener {

    private lateinit var enablePref: SwitchPreference
    private lateinit var standaloneEnablePref: SwitchPreference
    private lateinit var standaloneColorPref: ListPreference
    private lateinit var notificationsAppsPref: Preference
    private lateinit var gameModeAppsPref: Preference
    private lateinit var musicAppsPref: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.light_settings, rootKey)

        enablePref = findPreference("light_enable")!!
        enablePref.onPreferenceChangeListener = this

        standaloneEnablePref = findPreference("light_standalone_enable")!!
        standaloneEnablePref.onPreferenceChangeListener = this

        standaloneColorPref = findPreference("light_standalone_color")!!
        standaloneColorPref.onPreferenceChangeListener = this

        notificationsAppsPref = findPreference("light_notifications_apps")!!
        notificationsAppsPref.setOnPreferenceClickListener {
            val intent = Intent(context, AppSelectorActivity::class.java)
            intent.putExtra("title", "Select Apps for Notifications")
            intent.putExtra("prefKey", "light_notifications_apps")
            startActivity(intent)
            true
        }

        musicAppsPref = findPreference("light_music_apps")!!
        musicAppsPref.setOnPreferenceClickListener {
            val intent = Intent(context, AppSelectorActivity::class.java)
            intent.putExtra("title", "Select Music Apps")
            intent.putExtra("prefKey", "light_music_apps")
            startActivity(intent)
            true
        }

        gameModeAppsPref = findPreference("light_game_mode_apps")!!
        gameModeAppsPref.setOnPreferenceClickListener {
            val intent = Intent(context, AppSelectorActivity::class.java)
            intent.putExtra("title", "Select Games for LED")
            intent.putExtra("prefKey", "light_game_mode_apps")
            startActivity(intent)
            true
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        when (preference.key) {
            "light_enable" -> {
                val enabled = newValue as Boolean
                if (enabled) {
                    context?.startService(Intent(context, LightService::class.java))
                } else {
                    context?.stopService(Intent(context, LightService::class.java))
                    LedManager.turnOff()
                }
            }
            "light_standalone_enable", "light_standalone_color" -> {
                // Restart service to apply the standalone state change
                val enabled = enablePref.isChecked
                if (enabled) {
                    context?.startService(Intent(context, LightService::class.java))
                }
            }
        }
        return true
    }
}
