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
import com.android.systemui.statusbar.policy.RotationLockController;

public class RotationLockButton extends QSButton implements
        RotationLockController.RotationLockControllerCallback {

    private final RotationLockController mRotationLockController;

    private boolean mEnabled;

    public RotationLockButton(Context context, QSBar qsBar, Drawable iconEnabled,
            Drawable iconDisabled) {
        super(context, qsBar, iconEnabled, iconDisabled);

        mRotationLockController = mQSBar.getRotationLockController();
        mEnabled = !mRotationLockController.isRotationLocked();
        updateState(mEnabled);
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mRotationLockController.addRotationLockControllerCallback(this);
        } else {
            mRotationLockController.removeRotationLockControllerCallback(this);
        }
    }

    @Override
    public void handleClick() {
        mRotationLockController.setRotationLocked(mEnabled);
    }

    @Override
    public void handleLongClick() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                "com.android.settings",
                "com.android.settings.Settings$DisplayRotationSettingsActivity"));
        mQSBar.startSettingsActivity(intent);
    }

    @Override
    public void onRotationLockStateChanged(boolean rotationLocked, boolean affordanceVisible) {
        mEnabled = !rotationLocked;
        updateState(mEnabled);
    }
}
