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

package org.lineageos.settings.touchsampling;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import org.lineageos.settings.utils.FileUtils;
import vendor.xiaomi.hw.touchfeature.ITouchFeature;

public class TouchSamplingService extends Service {
    private static final String TAG = "TouchSamplingService";
    private static int TOUCH_GAME_MODE = 0;
    private ITouchFeature mTouchFeature;

    private BroadcastReceiver mScreenUnlockReceiver;
    private SharedPreferences.OnSharedPreferenceChangeListener mPreferenceChangeListener;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "TouchSamplingService started");
    
        // Initialize the touchfeature
        initTouchFeature();

        // Initialize and register the broadcast receiver
        registerScreenUnlockReceiver();

        // Initialize and register the SharedPreferences listener
        registerPreferenceChangeListener();

        // Apply the touch sampling rate initially
        applyTouchSamplingRateFromPreferences();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "TouchSamplingService stopped");

        // Unregister the broadcast receiver
        if (mScreenUnlockReceiver != null) {
            unregisterReceiver(mScreenUnlockReceiver);
        }

        // Unregister the SharedPreferences change listener
        SharedPreferences sharedPref = getSharedPreferences(
                TouchSamplingSettingsFragment.SHAREDHTSR, Context.MODE_PRIVATE);
        sharedPref.unregisterOnSharedPreferenceChangeListener(mPreferenceChangeListener);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initTouchFeature() {
        try {
            String name = "default";
            String fqName = vendor.xiaomi.hw.touchfeature.ITouchFeature.DESCRIPTOR + "/" + name;
            IBinder binder = android.os.Binder.allowBlocking(
                    android.os.ServiceManager.waitForDeclaredService(fqName));
            mTouchFeature = vendor.xiaomi.hw.touchfeature.ITouchFeature.Stub.asInterface(binder);
        } catch (Exception e) {
            // Silent catch
        }
    }

    /**
     * Registers a BroadcastReceiver to handle screen unlock and screen on events.
     */
    private void registerScreenUnlockReceiver() {
        mScreenUnlockReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_USER_PRESENT.equals(intent.getAction()) ||
                        Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                    Log.d(TAG, "Screen turned on or device unlocked. Reapplying touch sampling rate.");
                    applyTouchSamplingRateFromPreferences();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mScreenUnlockReceiver, filter);
    }

    /**
     * Registers a SharedPreferences.OnSharedPreferenceChangeListener to monitor
     * changes in the touch sampling rate setting.
     */
    private void registerPreferenceChangeListener() {
        SharedPreferences sharedPref = getSharedPreferences(
                TouchSamplingSettingsFragment.SHAREDHTSR, Context.MODE_PRIVATE);

        mPreferenceChangeListener = (sharedPreferences, key) -> {
            if (TouchSamplingSettingsFragment.HTSR_STATE.equals(key)) {
                Log.d(TAG, "Preference changed. Reapplying touch sampling rate.");
                boolean htsrEnabled = sharedPreferences.getBoolean(key, false);
                applyTouchSamplingRate(htsrEnabled ? 1 : 0);
            }
        };

        sharedPref.registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);
    }

    /**
     * Reads the touch sampling rate preference and applies the appropriate state.
     */
    private void applyTouchSamplingRateFromPreferences() {
        SharedPreferences sharedPref = getSharedPreferences(
                TouchSamplingSettingsFragment.SHAREDHTSR, Context.MODE_PRIVATE);
        boolean htsrEnabled = sharedPref.getBoolean(TouchSamplingSettingsFragment.HTSR_STATE, false);
        applyTouchSamplingRate(htsrEnabled ? 1 : 0);
    }

    /**
     * Applies the given touch sampling rate state directly to the hardware file.
     *
     * @param state 1 to enable high touch sampling rate, 0 to disable it.
     */
    private void applyTouchSamplingRate(int state) {
        try {
            if (mTouchFeature != null) {
                mTouchFeature.setTouchMode(0, TOUCH_GAME_MODE, state);
                mTouchFeature.setTouchMode(0, 202, state);
                mTouchFeature.setTouchMode(0, 1, state);
                mTouchFeature.setTouchMode(0, 3, (state == 1) ? 34 : 0);
                mTouchFeature.setTouchMode(0, 2, (state == 1) ? 99 : 0);
                mTouchFeature.setTouchMode(0, 7, (state == 1) ? 0 : 1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to set touch sampling state", e);
        }
    }
}
