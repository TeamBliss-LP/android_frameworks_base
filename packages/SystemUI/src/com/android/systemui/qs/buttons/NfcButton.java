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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;

import com.android.systemui.R;
import com.android.systemui.qs.QSBar;

public class NfcButton extends QSButton {
    private static final Intent NFC_SETTINGS = new Intent("android.settings.NFC_SETTINGS");

    private NfcAdapter mNfcAdapter;

    private boolean mEnabled;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mEnabled = isNfcEnabled();
            updateState(mEnabled);
        }
    };

    public NfcButton(Context context, QSBar qsBar, Drawable iconEnabled,
            Drawable iconDisabled) {
        super(context, qsBar, iconEnabled, iconDisabled);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(mContext);
        mEnabled = isNfcEnabled();
        updateState(mEnabled);
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mContext.registerReceiver(mReceiver,
                    new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED));
        } else {
            mContext.unregisterReceiver(mReceiver);
        }
    }

    @Override
    public void handleClick() {
        if (mEnabled) {
            mNfcAdapter.disable();
        } else {
            mNfcAdapter.enable();
        }
    }

    @Override
    public void handleLongClick() {
        mQSBar.startSettingsActivity(NFC_SETTINGS);
    }

    private boolean isNfcEnabled() {
        int state = mNfcAdapter.getAdapterState();
        if (state == NfcAdapter.STATE_TURNING_ON
                || state == NfcAdapter.STATE_ON) {
            return true;
        } else {
            return false;
        }
    }
}
