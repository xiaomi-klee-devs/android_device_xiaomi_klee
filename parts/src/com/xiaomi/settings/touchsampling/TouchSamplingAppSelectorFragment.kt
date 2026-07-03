package com.xiaomi.settings.touchsampling

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import com.xiaomi.settings.R
import java.util.HashSet

class TouchSamplingAppSelectorFragment : SettingsBasePreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val screen = preferenceManager.createPreferenceScreen(requireContext())
        preferenceScreen = screen
        loadApps(screen)
    }

    private fun loadApps(screen: PreferenceScreen) {
        val packageManager = requireContext().packageManager
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val autoApps = savedAutoApps

        val sortedApps = installedApps.filter { appInfo ->
            appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0 &&
            appInfo.packageName != requireContext().packageName
        }.sortedWith(
            compareByDescending<ApplicationInfo> { autoApps.contains(it.packageName) }
            .thenBy { it.loadLabel(packageManager).toString().lowercase() }
        )

        for (appInfo in sortedApps) {
            val pref = SwitchPreferenceCompat(requireContext()).apply {
                title = appInfo.loadLabel(packageManager)
                summary = appInfo.packageName
                icon = appInfo.loadIcon(packageManager)
                isChecked = autoApps.contains(appInfo.packageName)
                isPersistent = false
                setOnPreferenceChangeListener { _, newValue ->
                    val isEnabled = newValue as Boolean
                    updateAutoApp(appInfo.packageName, isEnabled)
                    true
                }
            }
            screen.addPreference(pref)
        }
    }

    private val savedAutoApps: Set<String>
        get() = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getStringSet(TouchSamplingSettingsFragment.HTSR_APPS_PREF, HashSet()) ?: HashSet()

    private fun updateAutoApp(packageName: String, add: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val currentSet = prefs.getStringSet(TouchSamplingSettingsFragment.HTSR_APPS_PREF, HashSet()) ?: HashSet()

        val newSet = HashSet(currentSet)
        if (add) {
            newSet.add(packageName)
        } else {
            newSet.remove(packageName)
        }
        prefs.edit().putStringSet(TouchSamplingSettingsFragment.HTSR_APPS_PREF, newSet).apply()
    }
}
