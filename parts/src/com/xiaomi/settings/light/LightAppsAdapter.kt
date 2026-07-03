/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.light

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xiaomi.settings.R

class LightAppsAdapter(
    private val packageManager: PackageManager,
    private val apps: List<ApplicationInfo>,
    private val selectedApps: Set<String>,
    private val listener: OnAppToggledListener?
) : RecyclerView.Adapter<LightAppsAdapter.ViewHolder>() {

    interface OnAppToggledListener {
        fun onAppToggled(appInfo: ApplicationInfo, isChecked: Boolean)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.light_app_selector_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appInfo = apps[position]
        holder.appName.text = appInfo.loadLabel(packageManager)
        holder.appPackage.text = appInfo.packageName
        holder.appIcon.setImageDrawable(appInfo.loadIcon(packageManager))

        holder.appSwitch.setOnCheckedChangeListener(null)
        holder.appSwitch.isChecked = selectedApps.contains(appInfo.packageName)

        holder.appSwitch.setOnCheckedChangeListener { _, isChecked ->
            listener?.onAppToggled(appInfo, isChecked)
        }

        holder.itemView.setOnClickListener {
            holder.appSwitch.isChecked = !holder.appSwitch.isChecked
        }
    }

    override fun getItemCount(): Int {
        return apps.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appName: TextView = itemView.findViewById(R.id.app_name)
        val appPackage: TextView = itemView.findViewById(R.id.app_package)
        val appIcon: ImageView = itemView.findViewById(R.id.app_icon)
        val appSwitch: Switch = itemView.findViewById(R.id.app_switch)
    }
}
