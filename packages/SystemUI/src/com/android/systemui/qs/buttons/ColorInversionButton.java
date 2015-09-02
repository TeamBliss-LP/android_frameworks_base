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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.provider.Settings;

import com.android.systemui.R;
import com.android.systemui.qs.QSBar;

public class ColorInversionButton extends QSButton {
    private static final Intent ACCESSIBILITY_SETTINGS = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);

    private final ContentObserver mColorInversionObserver;
    private final ContentResolver mResolver;

    private boolean mEnabled;

    public ColorInversionButton(Context context, QSBar qsBar, Drawable iconEnabled,
            Drawable iconDisabled) {
        super(context, qsBar, iconEnabled, iconDisabled);

        mColorInversionObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                mEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 0) == 1;
                updateState(mEnabled);
            }
        };
        mResolver = mContext.getContentResolver();
        mEnabled = Settings.Secure.getInt(mResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 0) == 1;
        updateState(mEnabled);
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mResolver.registerContentObserver(
                    Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED),
                    false, mColorInversionObserver);
        } else {
            mResolver.unregisterContentObserver(mColorInversionObserver);
        }
    }

    @Override
    public void handleClick() {
        Settings.Secure.putInt(mResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, mEnabled ? 0 : 1);
    }

    @Override
    public void handleLongClick() {
        mQSBar.startSettingsActivity(ACCESSIBILITY_SETTINGS);
    }
}
