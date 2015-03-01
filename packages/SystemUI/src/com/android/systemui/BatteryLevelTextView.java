/*
 * Copyright (C) 2014 The CyanogenMod Project
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

package com.android.systemui;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

import com.android.systemui.cm.UserContentObserver;
import com.android.systemui.statusbar.policy.BatteryController;

public class BatteryLevelTextView extends TextView implements
        BatteryController.BatteryStateChangeCallback{

    private static final String STATUS_BAR_BATTERY_STATUS_STYLE =
            "status_bar_battery_status_style";
    private static final String STATUS_BAR_BATTERY_STATUS_PERCENT_STYLE =
            "status_bar_battery_status_percent_style";

    private BatteryController mBatteryController;
    private boolean mBatteryCharging;
    private boolean mShow;
    private boolean mForceShow;
    private boolean mAttached;
    private int mRequestedVisibility;
	
	private ContentResolver mResolver;

    private SettingsObserver mObserver = new SettingsObserver(new Handler());

    private class SettingsObserver extends UserContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        protected void observe() {
            super.observe();

            mResolver.registerContentObserver(Settings.System.getUriFor(
                    STATUS_BAR_BATTERY_STATUS_STYLE), false, this, UserHandle.USER_ALL);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    STATUS_BAR_BATTERY_STATUS_PERCENT_STYLE), false, this, UserHandle.USER_ALL);
        }

        @Override
        protected void unobserve() {
            super.unobserve();

            getContext().getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void update() {
            loadShowBatteryTextSetting();
        }
    };

    public BatteryLevelTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mResolver = context.getContentResolver();
        mRequestedVisibility = getVisibility();
        loadShowBatteryTextSetting();
    }

    public void setForceShown(boolean forceShow) {
        mForceShow = forceShow;
        updateVisibility();
    }

    public void setBatteryController(BatteryController batteryController) {
        mBatteryController = batteryController;
        if (mAttached) {
            mBatteryController.addStateChangedCallback(this);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        mRequestedVisibility = visibility;
        updateVisibility();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Respect font size setting.
        setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.battery_level_text_size));
     }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        setText(getResources().getString(R.string.battery_level_template, level));
        boolean changed = mBatteryCharging != charging;
        mBatteryCharging = charging;
        if (changed) {
            loadShowBatteryTextSetting();
        }
    }

    @Override
    public void onPowerSaveChanged() {
        // Not used
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mBatteryController != null) {
            mBatteryController.addStateChangedCallback(this);
        }
        mObserver.observe();

        mAttached = true;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttached = false;
        mResolver.unregisterContentObserver(mObserver);

        if (mBatteryController != null) {
            mBatteryController.removeStateChangedCallback(this);
        }
    }

    private void updateVisibility() {
        if (mShow || mForceShow) {
            super.setVisibility(mRequestedVisibility);
        } else {
            super.setVisibility(GONE);
        }
    }

    public void setTextColor(boolean isHeader) {
        int headerColor = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_EXPANDED_HEADER_TEXT_COLOR, 0xffffffff);
        int color = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_BATTERY_STATUS_TEXT_COLOR, 0xff000000);

        super.setTextColor(isHeader ? headerColor : color);
    }


    private void loadShowBatteryTextSetting() {
        int currentUserId = ActivityManager.getCurrentUser();
        int mode = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_BATTERY_STATUS_PERCENT_STYLE, 2, currentUserId);

        boolean showNextPercent = mode == 1;
        int batteryStyle = Settings.System.getIntForUser(mResolver,
                Settings.System.STATUS_BAR_BATTERY_STATUS_STYLE, 0, currentUserId);

        switch (batteryStyle) {
            case 3: //BATTERY_METER_TEXT
                showNextPercent = true;
                break;
            case 4: //BATTERY_METER_GONE
                showNextPercent = false;
                break;
            default:
                break;
        }

        mShow = showNextPercent;
        updateVisibility();
    }
}
