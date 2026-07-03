/*
 * Copyright (C) 2024 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */

package com.xiaomi.settings.touchsampling

import android.content.Context
import android.util.Log
import com.xiaomi.settings.utils.writeLine

object TouchSamplingUtils {

    private const val TAG = "TouchSamplingUtils"

    const val HTSR_FILE =
        "/sys/devices/platform/goodix_ts.0/switch_report_rate"

    @JvmStatic
    fun restoreSamplingValue(context: Context) {

        val sharedPref = context.getSharedPreferences(
            TouchSamplingSettingsFragment.SHAREDHTSR,
            Context.MODE_PRIVATE
        )

        val htsrState = sharedPref.getInt(
            TouchSamplingSettingsFragment.SHAREDHTSR,
            0
        )

        if (!writeLine(HTSR_FILE, htsrState.toString())) {
            Log.e(TAG, "Failed to restore sampling value")
        }
    }
}