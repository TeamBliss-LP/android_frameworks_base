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
import com.android.systemui.statusbar.policy.HotspotController;


public class HotspotButton extends QSButton implements
        HotspotController.Callback {

    private final HotspotController mHotspotController;

    private boolean mEnabled;

    public HotspotButton(Context context, QSBar qsBar, Drawable iconEnabled,
            Drawable iconDisabled) {
        super(context, qsBar, iconEnabled, iconDisabled);

        mHotspotController = mQSBar.getHotspotController();
        mEnabled = mHotspotController.isHotspotEnabled();
        updateState(mEnabled);
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mHotspotController.addCallback(this);
        } else {
            mHotspotController.removeCallback(this);
        }
    }

    @Override
    public void handleClick() {
        mHotspotController.setHotspotEnabled(!mEnabled);
    }

    @Override
    public void handleLongClick() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                "com.android.settings",
                "com.android.settings.Settings$TetherSettingsActivity"));
        mQSBar.startSettingsActivity(intent);
    }

    @Override
    public void onHotspotChanged(boolean enabled) {
        mEnabled = enabled;
        updateState(mEnabled);
    }
}
