/*
 * Copyright (C) 2012 CyanogenMod
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

package com.android.settings.cyanogenmod;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.IWindowManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class SystemSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SystemSettings";

    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_NOTIFICATION_DRAWER = "notification_drawer";
    private static final String KEY_NOTIFICATION_DRAWER_TABLET = "notification_drawer_tablet";
    private static final String KEY_NAVIGATION_BAR = "navigation_bar";
    private static final String KEY_HARDWARE_KEYS = "hardware_keys";

    private ListPreference mFontSizePref;

    private final Configuration mCurConfig = new Configuration();
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.system_settings);

        mFontSizePref = (ListPreference) findPreference(KEY_FONT_SIZE);
        mFontSizePref.setOnPreferenceChangeListener(this);
        if (Utils.isScreenLarge()) {
            getPreferenceScreen().removePreference(findPreference(KEY_NOTIFICATION_DRAWER));
        } else {
            getPreferenceScreen().removePreference(findPreference(KEY_NOTIFICATION_DRAWER_TABLET));
        }
        IWindowManager windowManager = IWindowManager.Stub.asInterface(ServiceManager.getService(Context.WINDOW_SERVICE));
        try {
            if (windowManager.hasNavigationBar()) {
                getPreferenceScreen().removePreference(findPreference(KEY_HARDWARE_KEYS));
            } else {
                getPreferenceScreen().removePreference(findPreference(KEY_NAVIGATION_BAR));
            }
        } catch (RemoteException e) {
        }

    }

    int floatToIndex(float val) {
        String[] indices = getResources().getStringArray(R.array.entryvalues_font_size);
        float lastVal = Float.parseFloat(indices[0]);
        for (int i=1; i<indices.length; i++) {
            float thisVal = Float.parseFloat(indices[i]);
            if (val < (lastVal + (thisVal-lastVal)*.5f)) {
                return i-1;
            }
            lastVal = thisVal;
        }
        return indices.length-1;
    }
    
    public void readFontSizePreference(ListPreference pref) {
        try {
            mCurConfig.updateFrom(ActivityManagerNative.getDefault().getConfiguration());
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to retrieve font size");
        }

        // mark the appropriate item in the preferences list
        int index = floatToIndex(mCurConfig.fontScale);
        pref.setValueIndex(index);

        // report the current size in the summary text
        final Resources res = getResources();
        String[] fontSizeNames = res.getStringArray(R.array.entries_font_size);
        pref.setSummary(String.format(res.getString(R.string.summary_font_size),
                fontSizeNames[index]));
    }
    
    @Override
    public void onResume() {
        super.onResume();

        updateState();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void updateState() {
        readFontSizePreference(mFontSizePref);
    }

    public void writeFontSizePreference(Object objValue) {
        try {
            mCurConfig.fontScale = Float.parseFloat(objValue.toString());
            ActivityManagerNative.getDefault().updatePersistentConfiguration(mCurConfig);
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to save font size");
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_FONT_SIZE.equals(key)) {
            writeFontSizePreference(objValue);
        }

        return true;
    }
}
