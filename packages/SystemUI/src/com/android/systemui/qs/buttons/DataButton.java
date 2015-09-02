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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.provider.Settings;

import com.android.systemui.R;
import com.android.systemui.qs.QSBar;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.MobileDataController;


public class DataButton extends QSButton implements
        NetworkController.NetworkSignalChangedCallback {
    private static final Intent WIRELESS_SETTINGS = new Intent(Settings.ACTION_WIRELESS_SETTINGS);

    private final NetworkController mNetworkController;
    private final MobileDataController mMobileDataController;

    private boolean mEnabled;
    private boolean mAirplaneModeEnabled;

    public DataButton(Context context, QSBar qsBar, Drawable iconEnabled,
            Drawable iconDisabled) {
        super(context, qsBar, iconEnabled, iconDisabled);

        mNetworkController = mQSBar.getNetworkController();
        mMobileDataController = mNetworkController.getMobileDataController();
        mAirplaneModeEnabled = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        mEnabled = !mAirplaneModeEnabled && mMobileDataController.isMobileDataEnabled();
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
        if (!mAirplaneModeEnabled) {
            mMobileDataController.setMobileDataEnabled(!mEnabled);
        } else {
            mQSBar.startSettingsActivity(WIRELESS_SETTINGS);
        }
    }

    @Override
    public void handleLongClick() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                "com.android.settings",
                "com.android.settings.Settings$DataUsageSummaryActivity"));
        mQSBar.startSettingsActivity(intent);
    }

    @Override
    public void onWifiSignalChanged(boolean enabled, boolean connected,
                int wifiSignalIconId, boolean activityIn, boolean activityOut,
                String wifiSignalContentDescriptionId, String description) {
    }

    @Override
    public void onMobileDataSignalChanged(boolean enabled, int mobileSignalIconId,
                String mobileSignalContentDescriptionId, int dataTypeIconId,
                boolean activityIn, boolean activityOut,
                String dataTypeContentDescriptionId, String description,
                boolean isDataTypeIconWide) {
        mEnabled = !mAirplaneModeEnabled && mMobileDataController.isMobileDataEnabled();
        updateState(mEnabled);
    }

    @Override
    public  void onNoSimVisibleChanged(boolean visible) {
    }

    @Override
    public void onAirplaneModeChanged(boolean enabled) {
        mAirplaneModeEnabled = enabled;
        mEnabled = !mAirplaneModeEnabled && mMobileDataController.isMobileDataEnabled();
        updateState(mEnabled);
    }

    @Override
    public  void onMobileDataEnabled(boolean visible) {
    }
}
