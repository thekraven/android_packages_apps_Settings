/*
 * Copyright (C) 2011 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.cyanogenmod;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.ListFragment;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.TextView;
import android.view.View.OnClickListener;

import android.util.Log;
import android.util.TypedValue;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;


import com.android.settings.Utils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;



public class NavBar extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private boolean mEditMode;
    private ViewGroup mContainer;
    private Activity mActivity;
	private MenuItem mEditMenu;
	private static final String PREF_NAVBAR_MENU_DISPLAY = "navbar_menu_display";
    private final static Intent mIntent = new Intent("android.intent.action.NAVBAR_EDIT");
    private static final int MENU_RESET = Menu.FIRST;
    private static final int MENU_EDIT = Menu.FIRST + 1;
	private static final String NAV_BAR_TRANSPARENCY = "nav_bar_transparency";
	public static final String PREFS_NAV_BAR = "navbar";
	
	ListPreference mNavigationBarTransparency;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    // Load the preferences from an XML resource
	
        mNavigationBarTransparency = (ListPreference) findPreference(NAV_BAR_TRANSPARENCY);
 		mNavigationBarTransparency.setOnPreferenceChangeListener(this);
        mNavigationBarTransparency.setValue(Integer.toString(Settings.System.getInt(getActivity() 
            .getContentResolver(), Settings.System.NAV_BAR_TRANSPARENCY, 
            100)));		
         		
	//	if (mTablet) {
    	//	prefs.removePreference(mNavigationBarTransparency);
        //    prefs.removePreference(mNavBarMenuDisplay);
		//}
		
	}	

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.nav_bar, container, false);
        setHasOptionsMenu(true);
        mContainer = container;
        mActivity = getActivity();
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        return view;
    }

    /**
     * Toggles navbar edit mode
     * @param on True to enter edit mode / false to exit
     * @param save True to save changes / false to discard them
     */
    private void toggleEditMode(boolean on, boolean save) {
        mIntent.putExtra("edit", on);
        mIntent.putExtra("save", save);
        mActivity.sendBroadcast(mIntent);
		mEditMenu.setTitle(on ? R.string.navigation_bar_menu_editable :  R.string.navigation_bar_menu_locked);
    }

    @Override
    public void onResume() {
        super.onResume();
        // If running on a phone, remove padding around container
        if (!Utils.isScreenLarge()) {
            mContainer.setPadding(0, 0, 0, 0);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.profile_reset_title)
        .setIcon(R.drawable.ic_settings_backup)
        .setAlphabeticShortcut('r')
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		mEditMenu = menu.add(0, MENU_EDIT, 0, R.string.navigation_bar_menu_locked); 
        mEditMenu.setIcon(R.drawable.stat_navbar_edit_off) 
        .setAlphabeticShortcut('s')
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_RESET:
            new AlertDialog.Builder(mActivity)
            .setTitle(R.string.lockscreen_target_reset_title)
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .setMessage(R.string.navigation_bar_reset_message)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    if (mEditMode) {
                        toggleEditMode(false, false);
                    }
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.NAV_BUTTONS, null);
                    toggleEditMode(true, false);
                    toggleEditMode(false, false);
                    mEditMode = false;
                    Toast.makeText(mActivity, R.string.navigation_bar_reset_toast, Toast.LENGTH_LONG).show();
                }
            }).setNegativeButton(R.string.cancel, null)
            .create().show();
            return true;
        case MENU_EDIT:
            mEditMode = !mEditMode;
            toggleEditMode(mEditMode, true);
            if (!mEditMode) {
                item.setIcon(R.drawable.stat_navbar_edit_off);
                Toast.makeText(mActivity, R.string.navigation_bar_save_message, Toast.LENGTH_LONG).show();
            } else {
                item.setIcon(R.drawable.stat_navbar_edit_on);
            }
            return true;
        default:
            return false;
        }
    }

	public boolean onPreferenceChange(Preference preference, Object newValue) {
	    if (preference == mNavigationBarTransparency) { 
            int val = Integer.parseInt((String) newValue); 
            Settings.System.putInt(getActivity().getContentResolver(), 
                    Settings.System.NAV_BAR_TRANSPARENCY, val); 
			restartSystemUI(); 		
            return true; 

		}
		return false;
	}
	
    @Override
    public void onPause() {
        toggleEditMode(false, false);
        super.onPause();
    }

    @Override
    public void onStop() {
        toggleEditMode(false, false);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        toggleEditMode(false, false);
        super.onDestroy();
    }
	 
	private void restartSystemUI() {
	          try {
	              Runtime.getRuntime().exec("pkill -TERM -f com.android.systemui");
	          } catch (IOException e) {
	              e.printStackTrace();
	          }    
	}
}
