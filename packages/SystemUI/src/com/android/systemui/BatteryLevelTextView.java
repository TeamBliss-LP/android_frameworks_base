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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

import com.android.internal.util.bliss.ColorHelper;
import com.android.systemui.cm.UserContentObserver;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BatteryStateRegistar;

public class BatteryLevelTextView extends TextView implements
        BatteryController.BatteryStateChangeCallback{

    private static final int DEFAULT_BATTERY_TEXT_COLOR = 0xffffffff;

    private BatteryStateRegistar mBatteryStateRegistar;
    private boolean mBatteryPresent;
    private boolean mBatteryCharging;
    private boolean mForceShow;
    private boolean mAttached;
    private int mRequestedVisibility;
    private int mBatteryLevel = 0;
    private int mNewColor;
    private int mOldColor;
    private Animator mColorTransitionAnimator;

    private int mStyle;
    private int mPercentMode;

    private ContentResolver mResolver;

    private SettingsObserver mObserver;

    private class SettingsObserver extends UserContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        protected void observe() {
            super.observe();

            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY_STATUS_TEXT_COLOR),
                    false, this, UserHandle.USER_ALL);
        }

        @Override
        protected void unobserve() {
            super.unobserve();

            getContext().getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY_STATUS_TEXT_COLOR))) {
                update();
            }
        }

        @Override
        public void update() {
            setTextColor(false);
            updateVisibility();
        }
    };

    public BatteryLevelTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mResolver = context.getContentResolver();
        mRequestedVisibility = getVisibility();

        mNewColor = Settings.System.getInt(mResolver,
            Settings.System.STATUS_BAR_BATTERY_STATUS_TEXT_COLOR,
            DEFAULT_BATTERY_TEXT_COLOR);
        mOldColor = mNewColor;
        mColorTransitionAnimator = createColorTransitionAnimator(0, 1);
        mObserver = new SettingsObserver(new Handler());

        // setBatteryStateRegistar (if called) will made the view visible and ready to be hidden
        // if the view shouldn't be displayed. Otherwise this view should be hidden from start.
        mRequestedVisibility = GONE;
   }

    public void setForceShown(boolean forceShow) {
        mForceShow = forceShow;
        updateVisibility();
    }

    public void setBatteryStateRegistar(BatteryStateRegistar batteryStateRegistar) {
        mRequestedVisibility = VISIBLE;
        mBatteryStateRegistar = batteryStateRegistar;
        if (mAttached) {
            mBatteryStateRegistar.addStateChangedCallback(this);
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
            getResources().getDimensionPixelSize(
                R.dimen.battery_level_text_size));
     }

    @Override
    public void onBatteryLevelChanged(boolean present, int level, boolean pluggedIn,
            boolean charging) {
        mBatteryLevel = level;
        setText(getResources().getString(R.string.battery_level_template, level));
        if (mBatteryPresent != present || mBatteryCharging != charging) {
            mBatteryPresent = present;
            mBatteryCharging = charging;
            updateVisibility();
        }
    }

    @Override
    public void onPowerSaveChanged() {
        // Not used
    }

    @Override
    public void onBatteryStyleChanged(int style, int percentMode) {
        mStyle = style;
        mPercentMode = percentMode;
        updateVisibility();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mBatteryStateRegistar != null) {
            mBatteryStateRegistar.addStateChangedCallback(this);
        }
        mObserver.observe();

        mAttached = true;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttached = false;
        mResolver.unregisterContentObserver(mObserver);

        if (mBatteryStateRegistar != null) {
            mBatteryStateRegistar.removeStateChangedCallback(this);
        }
    }

    public void updateVisibility() {
        boolean showNextPercent = mBatteryPresent &&
            mStyle != BatteryController.STYLE_GONE && (
                (mPercentMode == BatteryController.PERCENTAGE_MODE_OUTSIDE) ||
                (mBatteryCharging && mPercentMode ==
                    BatteryController.PERCENTAGE_MODE_INSIDE));
        if (mStyle == BatteryController.STYLE_TEXT) {
            showNextPercent = true;
        } else if (mPercentMode == BatteryController.PERCENTAGE_MODE_OFF ||
                mStyle == BatteryController.STYLE_GONE) {
            showNextPercent = false;
        }

        if (mBatteryStateRegistar != null && (showNextPercent || mForceShow)) {
            super.setVisibility(mRequestedVisibility);
        } else {
            super.setVisibility(GONE);
        }
    }

    public void setTextColor(boolean isHeader) {
        if (isHeader) {
            int headerColor = Settings.System.getInt(mResolver,
                    Settings.System.STATUS_BAR_EXPANDED_HEADER_TEXT_COLOR,
                    DEFAULT_BATTERY_TEXT_COLOR);
            setTextColor(headerColor);
        } else {
            mNewColor = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_BATTERY_STATUS_TEXT_COLOR,
                DEFAULT_BATTERY_TEXT_COLOR);
            if (!mBatteryCharging && mBatteryLevel > 16) {
                if (mOldColor != mNewColor) {
                    mColorTransitionAnimator.start();
                }
            }
            setTextColor(mNewColor);
        }
    }

    private ValueAnimator createColorTransitionAnimator(float start, float end) {
        ValueAnimator animator = ValueAnimator.ofFloat(start, end);

        animator.setDuration(500);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
            @Override public void onAnimationUpdate(ValueAnimator animation) {
                float position = animation.getAnimatedFraction();
                int blended = ColorHelper.getBlendColor(mOldColor, mNewColor, position);
                setTextColor(blended);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mOldColor = mNewColor;
            }
        });
        return animator;
    }

}
