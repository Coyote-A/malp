/*
 * Copyright (C) 2016 Team Gateship-One
 * (Hendrik Borghorst & Frederik Luetkes)
 *
 * The AUTHORS.md file contains a detailed contributors list:
 * <https://github.com/gateship-one/malp/blob/master/AUTHORS.md>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.gateshipone.malp.application.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.background.BackgroundService;
import org.gateshipone.malp.application.utils.HardwareKeyHandler;
import org.gateshipone.malp.mpdservice.ConnectionManager;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDStateMonitoringHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDCurrentStatus;


public abstract class GenericActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private boolean mHardwareControls;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Read theme preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String themePref = sharedPref.getString(getString(R.string.pref_theme_key), getString(R.string.pref_theme_default));
        boolean darkTheme = sharedPref.getBoolean(getString(R.string.pref_dark_theme_key), getResources().getBoolean(R.bool.pref_theme_dark_default));
        if (darkTheme) {
            if (themePref.equals(getString(R.string.pref_indigo_key))) {
                setTheme(R.style.AppTheme_indigo);
            } else if (themePref.equals(getString(R.string.pref_orange_key))) {
                setTheme(R.style.AppTheme_orange);
            } else if (themePref.equals(getString(R.string.pref_deeporange_key))) {
                setTheme(R.style.AppTheme_deepOrange);
            } else if (themePref.equals(getString(R.string.pref_blue_key))) {
                setTheme(R.style.AppTheme_blue);
            } else if (themePref.equals(getString(R.string.pref_darkgrey_key))) {
                setTheme(R.style.AppTheme_darkGrey);
            } else if (themePref.equals(getString(R.string.pref_brown_key))) {
                setTheme(R.style.AppTheme_brown);
            } else if (themePref.equals(getString(R.string.pref_lightgreen_key))) {
                setTheme(R.style.AppTheme_lightGreen);
            } else if (themePref.equals(getString(R.string.pref_red_key))) {
                setTheme(R.style.AppTheme_red);
            }
        } else {
            if (themePref.equals(getString(R.string.pref_indigo_key))) {
                setTheme(R.style.AppTheme_indigo_light);
            } else if (themePref.equals(getString(R.string.pref_orange_key))) {
                setTheme(R.style.AppTheme_orange_light);
            } else if (themePref.equals(getString(R.string.pref_deeporange_key))) {
                setTheme(R.style.AppTheme_deepOrange_light);
            } else if (themePref.equals(getString(R.string.pref_blue_key))) {
                setTheme(R.style.AppTheme_blue_light);
            } else if (themePref.equals(getString(R.string.pref_darkgrey_key))) {
                setTheme(R.style.AppTheme_darkGrey_light);
            } else if (themePref.equals(getString(R.string.pref_brown_key))) {
                setTheme(R.style.AppTheme_brown_light);
            } else if (themePref.equals(getString(R.string.pref_lightgreen_key))) {
                setTheme(R.style.AppTheme_lightGreen_light);
            } else if (themePref.equals(getString(R.string.pref_red_key))) {
                setTheme(R.style.AppTheme_red_light);
            }
        }
        if (themePref.equals(getString(R.string.pref_oleddark_key))) {
            setTheme(R.style.AppTheme_oledDark);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ConnectionManager.reconnectLastServer(getApplicationContext());

        // Check if hardware key control is enabled by the user
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(this);
        mHardwareControls = sharedPref.getBoolean(getString(R.string.pref_hardware_controls_key), getResources().getBoolean(R.bool.pref_hardware_controls_default));

        boolean showNotification = sharedPref.getBoolean(getString(R.string.pref_show_notification_key), getResources().getBoolean(R.bool.pref_show_notification_default));
        if (showNotification) {
            Intent showNotificationIntent = new Intent(this, BackgroundService.class);
            showNotificationIntent.setAction(BackgroundService.ACTION_QUIT_BACKGROUND_SERVICE);
            startService(showNotificationIntent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        MPDCurrentStatus status = MPDStateMonitoringHandler.getLastStatus();
        if (!isChangingConfigurations()) {
            // Disconnect from MPD server
            ConnectionManager.disconnectFromServer();
        }
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.unregisterOnSharedPreferenceChangeListener(this);

        // Notify the widget to also connect if possible
        Intent connectIntent = new Intent(this, BackgroundService.class);
        connectIntent.setAction(BackgroundService.ACTION_CONNECT);
        startService(connectIntent);

        boolean showNotification = sharedPref.getBoolean(getString(R.string.pref_show_notification_key), getResources().getBoolean(R.bool.pref_show_notification_default));
        if (showNotification && status.getPlaybackState() != MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_STOPPED) {
            Intent showNotificationIntent = new Intent(this, BackgroundService.class);
            showNotificationIntent.setAction(BackgroundService.ACTION_SHOW_NOTIFICATION);
            startService(showNotificationIntent);
        }
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_hardware_controls_key))) {
            mHardwareControls = sharedPreferences.getBoolean(getString(R.string.pref_hardware_controls_key), getResources().getBoolean(R.bool.pref_hardware_controls_default));
        }
    }

    /**
     * Handles the volume keys of the device to control MPDs volume.
     *
     * @param event KeyEvent that was pressed by the user.
     * @return True if handled by MALP
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mHardwareControls) {
            if (!HardwareKeyHandler.getInstance().handleKeyEvent(event)) {
                return super.dispatchKeyEvent(event);
            } else return true;
        } else {
            return super.dispatchKeyEvent(event);
        }
    }
}
