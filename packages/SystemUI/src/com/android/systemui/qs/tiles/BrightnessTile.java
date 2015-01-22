/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2013-2015 The CyanogenMod Project
 * Copyright (C) 2015 The Euphoria-OS Project
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

package com.android.systemui.qs.tiles;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.settings.BrightnessController.BrightnessStateChangeCallback;
import com.android.systemui.qs.QSTile;

/** Quick settings tile: Brightness **/
public class BrightnessTile extends QSTile<QSTile.BooleanState> {

    private static final Intent DISPLAY_SETTINGS = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
    private boolean mListening;

    public BrightnessTile(Host host) {
        super(host);
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    public void setListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
    }

    @Override
    public void handleClick() {
        mHost.collapsePanels();
        mContext.startActivityAsUser(new Intent(
            Intent.ACTION_SHOW_BRIGHTNESS_DIALOG), UserHandle.CURRENT_OR_SELF);
        refreshState();
    }

    @Override
    protected void handleSecondaryClick() {
        mHost.startSettingsActivity(DISPLAY_SETTINGS);
    }

    @Override
    public void handleLongClick() {
        mHost.startSettingsActivity(DISPLAY_SETTINGS);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        int mode = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL, UserHandle.USER_CURRENT);
        boolean autoBrightness = mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;

        state.visible = true;
        state.label = mContext.getString(R.string.quick_settings_brightness_label);
        state.iconId = autoBrightness
                ? R.drawable.ic_qs_brightness_auto_on
                : R.drawable.ic_qs_brightness_auto_off_alpha;
    }
}
