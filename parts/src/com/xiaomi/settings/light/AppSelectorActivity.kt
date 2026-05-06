package com.xiaomi.settings.light

import android.os.Bundle
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.xiaomi.settings.R

class AppSelectorActivity : CollapsingToolbarBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_bar_app_selector)
        val titleExtra = intent.getStringExtra("title") ?: "Select Apps"
        val prefKey = intent.getStringExtra("prefKey") ?: AppSelectorFragment.PREF_AUTO_APPS

        title = titleExtra

        if (savedInstanceState == null) {
            val fragment = AppSelectorFragment()
            val args = Bundle()
            args.putString("prefKey", prefKey)
            fragment.arguments = args

            supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit()
        }
    }
}
