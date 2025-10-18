package org.lineageos.minrefreshrate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "MinRefreshRateCtrl";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            try {
                Settings.System.putFloat(context.getContentResolver(), "min_refresh_rate", 60.0f);
            } catch (Exception e) {
                Log.e(TAG, "Failed to set min_refresh_rate", e);
            }
        }
    }
}
