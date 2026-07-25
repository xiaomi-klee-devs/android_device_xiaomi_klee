/*
 * Copyright (C) 2025 kenway214
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

package com.xiaomi.settings.light

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.xiaomi.settings.utils.dlog

class LightNotificationBootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "LightNotificationBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (Intent.ACTION_BOOT_COMPLETED == action || Intent.ACTION_LOCKED_BOOT_COMPLETED == action) {
            grantListenerAccess(context)
        }
    }

    private fun grantListenerAccess(context: Context) {
        val component = ComponentName(context, LightNotificationService::class.java)
        try {
            val nm = context.getSystemService(NotificationManager::class.java)
            val method = NotificationManager::class.java.getMethod(
                "setNotificationListenerAccessGranted",
                ComponentName::class.java,
                Boolean::class.javaPrimitiveType
            )
            method.invoke(nm, component, true)
            dlog(TAG, "Granted notification listener access for $component")
        } catch (e: Exception) {
            dlog(TAG, "Failed to grant notification listener access: ${e.message}")
        }
    }
}
