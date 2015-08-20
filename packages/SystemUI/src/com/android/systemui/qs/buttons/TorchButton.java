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
 * limitations under the License
 */

package com.android.systemui.qs.buttons;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.hardware.TorchManager;

import com.android.systemui.R;
import com.android.systemui.qs.QSBar;

public class TorchButton extends QSButton implements
        TorchManager.TorchCallback {

    private final TorchManager mTorchManager;
    private boolean mTorchAvailable;
    private boolean mOn = false;


    public TorchButton(Context context, QSBar qsBar, Drawable iconEnabled,
            Drawable iconDisabled) {
        super(context, qsBar, iconEnabled, iconDisabled);

        mTorchManager = (TorchManager) mContext.getSystemService(Context.TORCH_SERVICE);
        mTorchAvailable = mTorchManager.isAvailable();
        if (mTorchAvailable) {
            mOn = mTorchManager.isTorchOn();
        }
        updateState(mOn);
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mTorchManager.addListener(this);
        } else {
            mTorchManager.removeListener(this);
        }
    }

    @Override
    public void handleClick() {
        if (mTorchAvailable) {
            mTorchManager.setTorchEnabled(!mOn);
        }
    }

    @Override
    public void onTorchStateChanged(boolean on) {
        mOn = on;
        updateState(mOn);
    }

    @Override
    public void onTorchError() {
        updateState(false);
    }

    @Override
    public void onTorchAvailabilityChanged(boolean available) {
        mTorchAvailable = available;
    }
}
