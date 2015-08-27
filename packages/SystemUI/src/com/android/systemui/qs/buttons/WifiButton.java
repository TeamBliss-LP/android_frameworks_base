/*
 * Copyright (C) 2015 DarkKat
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

package com.android.systemui.qs.buttons;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiManager;
import android.provider.Settings;

import com.android.systemui.R;
import com.android.systemui.qs.QSBar;
import com.android.systemui.statusbar.policy.NetworkController;


public class WifiButton extends QSButton implements
        NetworkController.NetworkSignalChangedCallback {
    private static final Intent WIFI_SETTINGS = new Intent(Settings.ACTION_WIFI_SETTINGS);

    private final NetworkController mNetworkController;

    private boolean mEnabled;

    public WifiButton(Context context, QSBar qsBar, Drawable iconEnabled,
            Drawable iconDisabled) {
        super(context, qsBar, iconEnabled, iconDisabled);

        mNetworkController = mQSBar.getNetworkController();
        final WifiManager wifiManager = (WifiManager)  mContext.getSystemService(Context.WIFI_SERVICE);
        mEnabled = wifiManager.isWifiEnabled();
        updateState(mEnabled);
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mNetworkController.addNetworkSignalChangedCallback(this);
        } else {
            mNetworkController.removeNetworkSignalChangedCallback(this);
        }
    }

    @Override
    public void handleClick() {
        mNetworkController.setWifiEnabled(!mEnabled);
    }

    @Override
    public void handleLongClick() {
        mQSBar.startSettingsActivity(WIFI_SETTINGS);
    }

    @Override
    public void onWifiSignalChanged(boolean enabled, boolean connected,
                int wifiSignalIconId, boolean activityIn, boolean activityOut,
                String wifiSignalContentDescriptionId, String description) {
        mEnabled = enabled;
        updateState(mEnabled);
    }

    @Override
    public void onMobileDataSignalChanged(boolean enabled, int mobileSignalIconId,
                String mobileSignalContentDescriptionId, int dataTypeIconId,
                boolean activityIn, boolean activityOut,
                String dataTypeContentDescriptionId, String description,
                boolean isDataTypeIconWide) {
    }

    @Override
    public  void onNoSimVisibleChanged(boolean visible) {
    }

    @Override
    public void onAirplaneModeChanged(boolean enabled) {
    }

    @Override
    public  void onMobileDataEnabled(boolean visible) {
    }
}
