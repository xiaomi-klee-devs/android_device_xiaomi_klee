package com.xiaomi.settings.light

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xiaomi.settings.R
import com.xiaomi.settings.gamebar.GameBarAppsAdapter

class AppSelectorFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private var adapter: GameBarAppsAdapter? = null
    private lateinit var packageManager: PackageManager
    private var allApps: MutableList<ApplicationInfo>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.game_bar_app_selector, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.app_list)
        packageManager = requireContext().packageManager
        recyclerView.layoutManager = LinearLayoutManager(context)
        loadApps()
    }

    private fun loadApps() {
        allApps = ArrayList()
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        for (appInfo in installedApps) {
            if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0 &&
                appInfo.packageName != requireContext().packageName
            ) {
                allApps!!.add(appInfo)
            }
        }
        val listener = object : GameBarAppsAdapter.OnAppToggledListener {
            override fun onAppToggled(appInfo: ApplicationInfo, isChecked: Boolean) {
                if (isChecked) {
                    addAppToAutoList(appInfo.packageName)
                } else {
                    removeAppFromAutoList(appInfo.packageName)
                }
            }
        }
        adapter = GameBarAppsAdapter(packageManager, allApps!!, savedAutoApps, listener)
        recyclerView.adapter = adapter
    }

    private val savedAutoApps: Set<String>
        get() {
            val prefKey = arguments?.getString("prefKey") ?: PREF_AUTO_APPS
            return PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getStringSet(prefKey, HashSet())!!
        }

    private fun addAppToAutoList(packageName: String) {
        val autoApps = HashSet(savedAutoApps)
        autoApps.add(packageName)
        val prefKey = arguments?.getString("prefKey") ?: PREF_AUTO_APPS
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .edit().putStringSet(prefKey, autoApps).apply()
    }

    private fun removeAppFromAutoList(packageName: String) {
        val autoApps = HashSet(savedAutoApps)
        autoApps.remove(packageName)
        val prefKey = arguments?.getString("prefKey") ?: PREF_AUTO_APPS
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .edit().putStringSet(prefKey, autoApps).apply()
    }

    companion object {
        const val PREF_AUTO_APPS = "light_notifications_apps"
    }
}
