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
import android.net.ConnectivityManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.Phone;

import com.android.systemui.R;
import com.android.systemui.qs.QSBar;

public class LteButton extends QSButton {
    private static final Intent DATA_ROAMING_SETTINGS = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);

    public LteButton(Context context, QSBar qsBar, Drawable iconEnabled,
            Drawable iconDisabled) {
        super(context, qsBar, iconEnabled, iconDisabled);
        updateState(isLteEnabled());
    }

    @Override
    public void handleClick() {
        TelephonyManager tm = (TelephonyManager)
                mContext.getSystemService(Context.TELEPHONY_SERVICE);
        tm.toggleLTE(true);
        updateState(isLteEnabled());
    }

    @Override
    public void handleLongClick() {
        mQSBar.startSettingsActivity(DATA_ROAMING_SETTINGS);
    }

    private boolean isLteEnabled() {
        boolean enabled = false;

        switch (getCurrentPreferredNetworkMode()) {
            case Phone.NT_MODE_GLOBAL:
            case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
            case Phone.NT_MODE_LTE_GSM_WCDMA:
            case Phone.NT_MODE_LTE_ONLY:
            case Phone.NT_MODE_LTE_WCDMA:
            case Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                enabled = true;
                break;
            default:
                break;
        }
        return enabled;
    }

    private int getCurrentPreferredNetworkMode() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.PREFERRED_NETWORK_MODE, -1);
    }
}
