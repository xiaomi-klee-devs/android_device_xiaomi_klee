package com.xiaomi.settings.touchsampling

import android.os.Bundle
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.xiaomi.settings.R

class TouchSamplingAppSelectorActivity : CollapsingToolbarBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_bar_app_selector)
        title = getString(R.string.htsr_choose_apps_title)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, TouchSamplingAppSelectorFragment())
                .commit()
        }
    }
}
