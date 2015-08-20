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
import android.provider.Settings;

import com.android.systemui.R;
import com.android.systemui.qs.QSBar;
import com.android.systemui.statusbar.policy.LocationController;


public class LocationButton extends QSButton implements
        LocationController.LocationSettingsChangeCallback {
    private static final Intent LOCATION_SETTINGS = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);

    private final LocationController mLocationController;

    private boolean mEnabled;

    public LocationButton(Context context, QSBar qsBar, Drawable iconEnabled,
            Drawable iconDisabled) {
        super(context, qsBar, iconEnabled, iconDisabled);

        mLocationController = mQSBar.getLocationController();
        mEnabled = mLocationController.isLocationEnabled();
        updateState(mEnabled);
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mLocationController.addSettingsChangedCallback(this);
        } else {
            mLocationController.removeSettingsChangedCallback(this);
        }
    }

    @Override
    public void handleClick() {
        mLocationController.setLocationEnabled(!mEnabled);
    }

    @Override
    public void handleLongClick() {
        mQSBar.startSettingsActivity(LOCATION_SETTINGS);
    }

    @Override
    public void onLocationSettingsChanged(boolean locationEnabled) {
        mEnabled = locationEnabled;
        updateState(mEnabled);
    }
}
