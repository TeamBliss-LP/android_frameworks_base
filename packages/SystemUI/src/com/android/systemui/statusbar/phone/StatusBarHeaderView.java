/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.database.ContentObserver;
import android.graphics.Outline;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.MathUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.util.cm.WeatherController;
import com.android.internal.util.cm.WeatherControllerImpl;
import com.android.keyguard.KeyguardStatusView;
import com.android.systemui.BatteryMeterView;
import com.android.systemui.BatteryLevelTextView;
import com.android.systemui.DockBatteryMeterView;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.cm.UserContentObserver;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.DockBatteryController;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.UserInfoController;

/**
 * The view to manage the header area in the expanded status bar.
 */
public class StatusBarHeaderView extends RelativeLayout implements View.OnClickListener, View.OnLongClickListener,
        NextAlarmController.NextAlarmChangeCallback, WeatherController.Callback {

    private static final int STATUS_BAR_POWER_MENU_OFF = 0;
    private static final int STATUS_BAR_POWER_MENU_DEFAULT = 1;
    private static final int STATUS_BAR_POWER_MENU_INVERTED = 2;

    private static final int DEFAULT_HEADER_COLOR = 0xffffffff;

    private boolean mExpanded;
    private boolean mListening;

    private ViewGroup mSystemIconsContainer;
    private ViewGroup mWeatherContainer;
    private View mSystemIconsSuperContainer;
    private View mDateGroup;
    private View mClock;
    private TextView mTime;
    private TextView mAmPm;
    private MultiUserSwitch mMultiUserSwitch;
    private ImageView mMultiUserAvatar;
    private TextView mDateCollapsed;
    private TextView mDateExpanded;
    private LinearLayout mSystemIcons;
    private View mSignalCluster;
    private View mSettingsButton;
    private View mQsDetailHeader;
    private TextView mQsDetailHeaderTitle;
    private Switch mQsDetailHeaderSwitch;
    private ImageView mQsDetailHeaderProgress;
    private TextView mEmergencyCallsOnly;
    private BatteryLevelTextView mBatteryLevel;
    private BatteryLevelTextView mDockBatteryLevel;
    private TextView mAlarmStatus;
    private TextView mWeatherLine1, mWeatherLine2;
    private ImageView mStatusBarPowerMenu;

    private boolean mShowEmergencyCallsOnly;
    private boolean mAlarmShowing;
    private AlarmManager.AlarmClockInfo mNextAlarm;

    private int mCollapsedHeight;
    private int mExpandedHeight;

    private int mMultiUserExpandedMargin;
    private int mMultiUserCollapsedMargin;

    private int mClockMarginBottomExpanded;
    private int mClockMarginBottomCollapsed;
    private int mMultiUserSwitchWidthCollapsed;
    private int mMultiUserSwitchWidthExpanded;

    private int mClockCollapsedSize;
    private int mClockExpandedSize;

    // HeadsUp button
    protected int mDrawable;
    private View mHeadsUpButton;
    private boolean mShowHeadsUpButton;

    // Task manager
    private boolean mShowTaskManager;
    private View mTaskManagerButton;

    /**
     * In collapsed QS, the clock and avatar are scaled down a bit post-layout to allow for a nice
     * transition. These values determine that factor.
     */
    private float mClockCollapsedScaleFactor;
    private float mAvatarCollapsedScaleFactor;

    private ActivityStarter mActivityStarter;
    private NextAlarmController mNextAlarmController;
    private WeatherController mWeatherController;
    private QSPanel mQSPanel;

    private final Rect mClipBounds = new Rect();

    private boolean mCaptureValues;
    private boolean mSignalClusterDetached;
    private final LayoutValues mCollapsedValues = new LayoutValues();
    private final LayoutValues mExpandedValues = new LayoutValues();
    private final LayoutValues mCurrentValues = new LayoutValues();

    private float mCurrentT;
    private boolean mShowingDetail;

    private SettingsObserver mSettingsObserver;
    private boolean mShowWeather;
    private boolean mShowWeatherLocation;
    private boolean mShowBatteryTextExpanded;

    protected Vibrator mVibrator;

    private boolean mQSCSwitch = false;

    private int mTextColor;
    private int mIconColor;
    private int mStatusBarPowerMenuStyle;

    public StatusBarHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mSystemIconsSuperContainer = findViewById(R.id.system_icons_super_container);
        mSystemIconsContainer = (ViewGroup) findViewById(R.id.system_icons_container);
        mSystemIconsSuperContainer.setOnClickListener(this);
        mSystemIconsSuperContainer.setOnLongClickListener(this);
        mDateGroup = findViewById(R.id.date_group);
        mDateGroup.setOnClickListener(this);
        mDateGroup.setOnLongClickListener(this);
        mClock = findViewById(R.id.clock);
        mClock.setOnClickListener(this);
        mClock.setOnLongClickListener(this);
        mTime = (TextView) findViewById(R.id.time_view);
        mAmPm = (TextView) findViewById(R.id.am_pm_view);
        mMultiUserSwitch = (MultiUserSwitch) findViewById(R.id.multi_user_switch);
        mMultiUserAvatar = (ImageView) findViewById(R.id.multi_user_avatar);
        mDateCollapsed = (TextView) findViewById(R.id.date_collapsed);
        mDateExpanded = (TextView) findViewById(R.id.date_expanded);
        mSettingsButton = findViewById(R.id.settings_button);
        mSettingsButton.setOnClickListener(this);
        mQsDetailHeader = findViewById(R.id.qs_detail_header);
        mQsDetailHeader.setAlpha(0);
        mQsDetailHeaderTitle = (TextView) mQsDetailHeader.findViewById(android.R.id.title);
        mQsDetailHeaderSwitch = (Switch) mQsDetailHeader.findViewById(android.R.id.toggle);
        mQsDetailHeaderProgress = (ImageView) findViewById(R.id.qs_detail_header_progress);
        mEmergencyCallsOnly = (TextView) findViewById(R.id.header_emergency_calls_only);
        mBatteryLevel = (BatteryLevelTextView) findViewById(R.id.battery_level_text);

        mHeadsUpButton = findViewById(R.id.heads_up_button);
        if (mHeadsUpButton != null) {
            mHeadsUpButton.setOnClickListener(this);
            mHeadsUpButton.setOnLongClickListener(this);
        }
        mTaskManagerButton = findViewById(R.id.task_manager_button);
        if (mTaskManagerButton != null) {
            mTaskManagerButton.setOnLongClickListener(this);
        }
        mStatusBarPowerMenu = (ImageView) findViewById(R.id.status_bar_power_menu);
        if (mStatusBarPowerMenu != null) {
            mStatusBarPowerMenu.setOnClickListener(this);
            mStatusBarPowerMenu.setLongClickable(true);
            mStatusBarPowerMenu.setOnLongClickListener(this);
        }

        mDockBatteryLevel = (BatteryLevelTextView) findViewById(R.id.dock_battery_level_text);
        mAlarmStatus = (TextView) findViewById(R.id.alarm_status);
        mAlarmStatus.setOnClickListener(this);
        mAlarmStatus.setOnLongClickListener(this);
        mSignalCluster = findViewById(R.id.signal_cluster);
        mSystemIcons = (LinearLayout) findViewById(R.id.system_icons);
        mWeatherContainer = (LinearLayout) findViewById(R.id.weather_container);
        mWeatherContainer.setOnClickListener(this);
        mWeatherContainer.setOnLongClickListener(this);
        mWeatherLine1 = (TextView) findViewById(R.id.weather_line_1);
        mWeatherLine2 = (TextView) findViewById(R.id.weather_line_2);
        mSettingsObserver = new SettingsObserver(new Handler());
        loadDimens();
        updateEverything();
        addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right,
                    int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if ((right - left) != (oldRight - oldLeft)) {
                    // width changed, update clipping
                    setClipping(getHeight());
                }
                boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
                mTime.setPivotX(rtl ? mTime.getWidth() : 0);
                mTime.setPivotY(mTime.getBaseline());
                updateAmPmTranslation();
            }
        });
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRect(mClipBounds);
            }
        });
        requestCaptureValues();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mCaptureValues) {
            if (mExpanded) {
                captureLayoutValues(mExpandedValues);
            } else {
                captureLayoutValues(mCollapsedValues);
            }
            mCaptureValues = false;
            updateLayoutValues(mCurrentT);
        }
        mAlarmStatus.setX(mDateGroup.getLeft() + mDateCollapsed.getRight());
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        FontSizeUtils.updateFontSize(mEmergencyCallsOnly,
                R.dimen.qs_emergency_calls_only_text_size);
        FontSizeUtils.updateFontSize(mDateCollapsed, R.dimen.qs_date_collapsed_size);
        FontSizeUtils.updateFontSize(mDateExpanded, R.dimen.qs_date_collapsed_size);
        FontSizeUtils.updateFontSize(mAlarmStatus, R.dimen.qs_date_collapsed_size);
        FontSizeUtils.updateFontSize(this, android.R.id.title, R.dimen.qs_detail_header_text_size);
        FontSizeUtils.updateFontSize(this, android.R.id.toggle, R.dimen.qs_detail_header_text_size);
        FontSizeUtils.updateFontSize(mAmPm, R.dimen.qs_time_collapsed_size);
        FontSizeUtils.updateFontSize(this, R.id.empty_time_view, R.dimen.qs_time_expanded_size);

        mEmergencyCallsOnly.setText(com.android.internal.R.string.emergency_calls_only);

        mClockCollapsedSize = getResources().getDimensionPixelSize(R.dimen.qs_time_collapsed_size);
        mClockExpandedSize = getResources().getDimensionPixelSize(R.dimen.qs_time_expanded_size);
        mClockCollapsedScaleFactor = (float) mClockCollapsedSize / (float) mClockExpandedSize;

        updateClockScale();
        updateClockCollapsedMargin();
    }

    private void updateClockCollapsedMargin() {
        Resources res = getResources();
        int padding = res.getDimensionPixelSize(R.dimen.clock_collapsed_bottom_margin);
        int largePadding = res.getDimensionPixelSize(
                R.dimen.clock_collapsed_bottom_margin_large_text);
        float largeFactor = (MathUtils.constrain(getResources().getConfiguration().fontScale, 1.0f,
                FontSizeUtils.LARGE_TEXT_SCALE) - 1f) / (FontSizeUtils.LARGE_TEXT_SCALE - 1f);
        mClockMarginBottomCollapsed = Math.round((1 - largeFactor) * padding + largeFactor * largePadding);
        requestLayout();
    }

    private void requestCaptureValues() {
        mCaptureValues = true;
        requestLayout();
    }

    private void loadDimens() {
        mCollapsedHeight = getResources().getDimensionPixelSize(R.dimen.status_bar_header_height);
        mExpandedHeight = getResources().getDimensionPixelSize(
                R.dimen.status_bar_header_height_expanded);
        mMultiUserExpandedMargin =
                getResources().getDimensionPixelSize(R.dimen.multi_user_switch_expanded_margin);
        mMultiUserCollapsedMargin =
                getResources().getDimensionPixelSize(R.dimen.multi_user_switch_collapsed_margin);
        mClockMarginBottomExpanded =
                getResources().getDimensionPixelSize(R.dimen.clock_expanded_bottom_margin);
        updateClockCollapsedMargin();
        mMultiUserSwitchWidthCollapsed =
                getResources().getDimensionPixelSize(R.dimen.multi_user_switch_width_collapsed);
        mMultiUserSwitchWidthExpanded =
                getResources().getDimensionPixelSize(R.dimen.multi_user_switch_width_expanded);
        mAvatarCollapsedScaleFactor =
                getResources().getDimensionPixelSize(R.dimen.multi_user_avatar_collapsed_size)
                / (float) mMultiUserAvatar.getLayoutParams().width;
        mClockCollapsedSize = getResources().getDimensionPixelSize(R.dimen.qs_time_collapsed_size);
        mClockExpandedSize = getResources().getDimensionPixelSize(R.dimen.qs_time_expanded_size);
        mClockCollapsedScaleFactor = (float) mClockCollapsedSize / (float) mClockExpandedSize;
    }

    public void setActivityStarter(ActivityStarter activityStarter) {
        mActivityStarter = activityStarter;
        if (mMultiUserSwitch != null) {
            mMultiUserSwitch.setActivityStarter(activityStarter);
        }
    }

    public void setBatteryController(BatteryController batteryController) {
        BatteryMeterView v = ((BatteryMeterView) findViewById(R.id.battery));
        v.setBatteryStateRegistar(batteryController);
        v.setBatteryController(batteryController);
        mBatteryLevel.setBatteryStateRegistar(batteryController);
    }

    public void setDockBatteryController(DockBatteryController dockBatteryController) {
        DockBatteryMeterView v = ((DockBatteryMeterView) findViewById(R.id.dock_battery));
        if (dockBatteryController != null) {
            v.setBatteryStateRegistar(dockBatteryController);
            if (mDockBatteryLevel != null) {
                mDockBatteryLevel.setBatteryStateRegistar(dockBatteryController);
            }
        } else {
            if (v != null) {
                removeView(v);
            }
            if (mDockBatteryLevel != null) {
                removeView(mDockBatteryLevel);
                mDockBatteryLevel = null;
            }
        }
    }

    public void setNextAlarmController(NextAlarmController nextAlarmController) {
        mNextAlarmController = nextAlarmController;
    }

    public void setWeatherController(WeatherController weatherController) {
        mWeatherController = weatherController;
    }

    public int getCollapsedHeight() {
        return mCollapsedHeight;
    }

    public int getExpandedHeight() {
        return mExpandedHeight;
    }

    public void setListening(boolean listening) {
        if (listening == mListening) {
            return;
        }
        mListening = listening;
        updateListeners();
    }

    public void setExpanded(boolean expanded) {
        boolean changed = expanded != mExpanded;
        mExpanded = expanded;
        if (changed) {
            updateEverything();
        }
    }

    public void updateEverything() {
        updateHeights();
        updateVisibilities();
        updateSystemIconsLayoutParams();
        updateClickTargets();
        updateMultiUserSwitch();
        if (mQSPanel != null) {
            mQSPanel.setExpanded(mExpanded);
        }
        updateClockScale();
        updateAvatarScale();
        updateClockLp();
        requestCaptureValues();
        updateHeaderElements();
    }

    private void updateHeaderElements() {
        updateBackgroundColor();
        updateTextColorSettings();
        updateIconColorSettings();
        updateHeadsUpButton();
        updateStatusBarPowerMenuVisibility();
        updateBatteryPercentageSettings();
        updateWeatherSettings();
    }

    private void updateHeights() {
        int height = mExpanded ? mExpandedHeight : mCollapsedHeight;
        ViewGroup.LayoutParams lp = getLayoutParams();
        if (lp.height != height) {
            lp.height = height;
            setLayoutParams(lp);
        }
    }

    private void updateVisibilities() {
        mDateCollapsed.setVisibility(mExpanded && mAlarmShowing ? View.VISIBLE : View.INVISIBLE);
        mDateExpanded.setVisibility(mExpanded && mAlarmShowing ? View.INVISIBLE : View.VISIBLE);
        mAlarmStatus.setVisibility(mExpanded && mAlarmShowing ? View.VISIBLE : View.INVISIBLE);
        mSettingsButton.setVisibility(mExpanded ? View.VISIBLE : View.INVISIBLE);

        if (mHeadsUpButton != null) {
            mHeadsUpButton.setVisibility(mExpanded && mShowHeadsUpButton ? View.VISIBLE : View.GONE);
        }
        if (mTaskManagerButton != null) {
            mTaskManagerButton.setVisibility(mExpanded && mShowTaskManager ? View.VISIBLE : View.GONE);
        }
        updateStatusBarPowerMenuVisibility();

        mQsDetailHeader.setVisibility(mExpanded && mShowingDetail ? View.VISIBLE : View.GONE);
        if (mSignalCluster != null) {
            updateSignalClusterDetachment();
        }
        mEmergencyCallsOnly.setVisibility(mExpanded && mShowEmergencyCallsOnly ? VISIBLE : GONE);
        updateWeatherVisibility();
        updateBatteryLevelVisibility();
    }

    private void updateWeatherVisibility() {
        mWeatherContainer.setVisibility(mExpanded && mShowWeather ? View.VISIBLE : View.GONE);
        mWeatherLine2.setVisibility(mExpanded && mShowWeather && mShowWeatherLocation ? View.VISIBLE : View.GONE);
    }

    private void updateBatteryLevelVisibility() {
        mBatteryLevel.setForceShown(mExpanded && mShowBatteryTextExpanded);
        mBatteryLevel.setVisibility(View.VISIBLE);
        if (mDockBatteryLevel != null) {
            mDockBatteryLevel.setForceShown(mExpanded && mShowBatteryTextExpanded);
            mDockBatteryLevel.setVisibility(View.VISIBLE);
        }
    }

    private void updateSignalClusterDetachment() {
        boolean detached = mExpanded;
        if (detached != mSignalClusterDetached) {
            if (detached) {
                getOverlay().add(mSignalCluster);
            } else {
                reattachSignalCluster();
            }
        }
        mSignalClusterDetached = detached;
    }

    private void reattachSignalCluster() {
        getOverlay().remove(mSignalCluster);
        mSystemIcons.addView(mSignalCluster, 1);
    }

    private void updateSystemIconsLayoutParams() {
        RelativeLayout.LayoutParams lp =
            (LayoutParams) mSystemIconsSuperContainer.getLayoutParams();
        final int settingsButtonId = mSettingsButton.getId();
        final int mMultiUserSwitchId = mMultiUserSwitch.getId();
        int newRule = mExpanded ? settingsButtonId : mMultiUserSwitchId;
        int taskManager = mTaskManagerButton != null
                ? mTaskManagerButton.getId()
                : settingsButtonId;
        int headsUp = mHeadsUpButton != null
                ? mHeadsUpButton.getId()
                : settingsButtonId;
        int powerMenu = mStatusBarPowerMenu != null
                ? mStatusBarPowerMenu.getId()
                : settingsButtonId;
        int rule  = mExpanded ? taskManager : newRule;
        int ruleh = mExpanded ? headsUp     : newRule;
        int rulep = mExpanded ? powerMenu   : newRule;

        if (mStatusBarPowerMenuStyle != STATUS_BAR_POWER_MENU_OFF) {
            newRule = rulep;
        } else if (mShowHeadsUpButton) {
            newRule = ruleh;
        } else if (mShowTaskManager) {
            newRule = rule;
        }
        if (newRule != lp.getRules()[RelativeLayout.START_OF]) {
            lp.addRule(RelativeLayout.START_OF, newRule);
            mSystemIconsSuperContainer.setLayoutParams(lp);
        }
    }

    private void updateListeners() {
        if (mListening) {
            mSettingsObserver.observe();
            mNextAlarmController.addStateChangedCallback(this);
            mWeatherController.addCallback(this);
        } else {
            mNextAlarmController.removeStateChangedCallback(this);
            mWeatherController.removeCallback(this);
            mSettingsObserver.unobserve();
        }
    }

    private void updateAvatarScale() {
        if (mExpanded) {
            mMultiUserAvatar.setScaleX(1f);
            mMultiUserAvatar.setScaleY(1f);
        } else {
            mMultiUserAvatar.setScaleX(mAvatarCollapsedScaleFactor);
            mMultiUserAvatar.setScaleY(mAvatarCollapsedScaleFactor);
        }
    }

    private void updateClockScale() {
        mTime.setTextSize(TypedValue.COMPLEX_UNIT_PX, mExpanded
                ? mClockExpandedSize
                : mClockCollapsedSize);
        mTime.setScaleX(1f);
        mTime.setScaleY(1f);
        updateAmPmTranslation();
    }

    private void updateAmPmTranslation() {
        boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        mAmPm.setTranslationX((rtl ? 1 : -1) * mTime.getWidth() * (1 - mTime.getScaleX()));
    }

    private boolean isStatusBarPowerMenuVisible() {
        return mStatusBarPowerMenu != null
            && mExpanded
            && (mStatusBarPowerMenuStyle != STATUS_BAR_POWER_MENU_OFF);
    }

    private void updateStatusBarPowerMenuVisibility() {
        if (mStatusBarPowerMenu != null) {
            mStatusBarPowerMenu.setVisibility(isStatusBarPowerMenuVisible()
                ? View.VISIBLE
                : View.GONE);
        }
    }

    @Override
    public void onNextAlarmChanged(AlarmManager.AlarmClockInfo nextAlarm) {
        mNextAlarm = nextAlarm;
        if (nextAlarm != null) {
            mAlarmStatus.setText(KeyguardStatusView.formatNextAlarm(getContext(), nextAlarm));
        }
        mAlarmShowing = nextAlarm != null;
        updateEverything();
        requestCaptureValues();
    }

    @Override
    public void onWeatherChanged(WeatherController.WeatherInfo info) {
        if (info.temp == null || info.condition == null) {
            mWeatherLine1.setText(mContext.getString(R.string.weather_info_not_available));
            mWeatherLine2.setText(null);
        } else {
            mWeatherLine1.setText(mContext.getString(
                    R.string.status_bar_expanded_header_weather_format,
                    info.temp,
                    info.condition));
            mWeatherLine2.setText(info.city);
        }

    }

    private void updateClickTargets() {
        mMultiUserSwitch.setClickable(mExpanded);
        mMultiUserSwitch.setFocusable(mExpanded);
        mSystemIconsSuperContainer.setClickable(mExpanded);
        mSystemIconsSuperContainer.setFocusable(mExpanded);
        mAlarmStatus.setClickable(mNextAlarm != null && mNextAlarm.getShowIntent() != null);
    }

    private void updateClockLp() {
        int marginBottom = mExpanded
                ? mClockMarginBottomExpanded
                : mClockMarginBottomCollapsed;
        LayoutParams lp = (LayoutParams) mDateGroup.getLayoutParams();
        if (marginBottom != lp.bottomMargin) {
            lp.bottomMargin = marginBottom;
            mDateGroup.setLayoutParams(lp);
        }
    }

    private void updateMultiUserSwitch() {
        int marginEnd;
        int width;
        if (mExpanded) {
            marginEnd = mMultiUserExpandedMargin;
            width = mMultiUserSwitchWidthExpanded;
        } else {
            marginEnd = mMultiUserCollapsedMargin;
            width = mMultiUserSwitchWidthCollapsed;
        }
        MarginLayoutParams lp = (MarginLayoutParams) mMultiUserSwitch.getLayoutParams();
        if (marginEnd != lp.getMarginEnd() || lp.width != width) {
            lp.setMarginEnd(marginEnd);
            lp.width = width;
            mMultiUserSwitch.setLayoutParams(lp);
        }
    }

    public void setExpansion(float t) {
        if (!mExpanded) {
            t = 0f;
        }
        mCurrentT = t;
        float height = mCollapsedHeight + t * (mExpandedHeight - mCollapsedHeight);
        if (height < mCollapsedHeight) {
            height = mCollapsedHeight;
        }
        if (height > mExpandedHeight) {
            height = mExpandedHeight;
        }
        setClipping(height);
        updateLayoutValues(t);
    }

    private void updateLayoutValues(float t) {
        if (mCaptureValues) {
            return;
        }
        mCurrentValues.interpolate(mCollapsedValues, mExpandedValues, t);
        applyLayoutValues(mCurrentValues);
    }

    private void setClipping(float height) {
        mClipBounds.set(getPaddingLeft(), 0, getWidth() - getPaddingRight(), (int) height);
        setClipBounds(mClipBounds);
        invalidateOutline();
    }

    public void setUserInfoController(UserInfoController userInfoController) {
        userInfoController.addListener(new UserInfoController.OnUserInfoChangedListener() {
            @Override
            public void onUserInfoChanged(String name, Drawable picture) {
                mMultiUserAvatar.setImageDrawable(picture);
            }
        });
    }

    public void vibrate (int duration) {
        if (mVibrator != null && mVibrator.hasVibrator()) {
            mVibrator.vibrate(duration);
        }
    }

    @Override
    public void onClick(View v) {
        boolean handledOnClick = true;

        if (v == mSettingsButton) {
            startSettingsActivity();
        } else if (v == mSystemIconsSuperContainer) {
            startBatteryActivity();
        } else if (v == mAlarmStatus && mNextAlarm != null) {
            PendingIntent showIntent = mNextAlarm.getShowIntent();
            if (showIntent != null && showIntent.isActivity()) {
                mActivityStarter.startActivity(showIntent.getIntent(), true /* dismissShade */);
            }
        } else if (v == mClock) {
            startClockActivity();
        } else if (v == mDateGroup) {
            startDateActivity();
        } else if (mStatusBarPowerMenu != null && v == mStatusBarPowerMenu) {
            statusBarPowerMenuAction();
        } else if (mHeadsUpButton != null && v == mHeadsUpButton) {
            startHeadsUpActivity();
        } else if (v == mWeatherContainer) {
            startForecastActivity();
        } else {
            handledOnClick = false;
        }

        if (handledOnClick) {
            mQSPanel.vibrateTile(QSPanel.VIBRATION_DURATION_SHORT);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v == mSettingsButton) {
            startSettingsLongClickActivity();
            vibrate(20);
        } else if (v == mSystemIconsSuperContainer) {
            startBatteryLongClickActivity();
        } else if (v == mClock) {
            startClockLongClickActivity();
        } else if (v == mDateGroup) {
            startDateLongClickActivity();
        } else if (v == mWeatherContainer) {
            startForecastLongClickActivity();
        } else if (mStatusBarPowerMenu != null && v == mStatusBarPowerMenu) {
            statusBarPowerMenuAction();
        } else if (mHeadsUpButton != null && v == mHeadsUpButton) {
            startHeadsUpLongClickActivity();
        } else if (mTaskManagerButton != null && v == mTaskManagerButton) {
            startTaskManagerLongClickActivity();
        } else {
          return false;
        }
        return true;
    }

    private void startSettingsActivity() {
        mActivityStarter.startActivity(new Intent(
            android.provider.Settings.ACTION_SETTINGS),
            true /* dismissShade */);
    }

    private void startSettingsLongClickActivity() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.android.settings",
            "com.android.settings.Settings$QSTilesSettingsActivity");
        mActivityStarter.startActivity(intent, true /* dismissShade */);
    }

    private void startBatteryActivity() {
        mActivityStarter.startActivity(new Intent(
            Intent.ACTION_POWER_USAGE_SUMMARY),
            true /* dismissShade */);
    }

    private void startBatteryLongClickActivity() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.android.settings",
            "com.android.settings.Settings$BatterySaverSettingsActivity");
        mActivityStarter.startActivity(intent, true /* dismissShade */);
    }

    private void startClockActivity() {
        mActivityStarter.startActivity(new Intent(
            AlarmClock.ACTION_SHOW_ALARMS),
            true /* dismissShade */);
    }

    private void startClockLongClickActivity() {
        mActivityStarter.startActivity(new Intent(
            AlarmClock.ACTION_SET_ALARM),
            true /* dismissShade */);
    }

    private void startDateActivity() {
        Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
        builder.appendPath("time");
        ContentUris.appendId(builder, System.currentTimeMillis());
        Intent intent = new Intent(Intent.ACTION_VIEW).setData(builder.build());
        mActivityStarter.startActivity(intent, true /* dismissShade */);
    }

    private void triggerPowerMenuDialog() {
        Intent intent = new Intent(Intent.ACTION_POWERMENU);
        mContext.sendBroadcast(intent); /* broadcast action */
        mActivityStarter.startActivity(intent,
                true /* dismissShade */);
    }

    private void statusBarPowerMenuAction() {
        if (isStatusBarPowerMenuVisible()) {
            if (mStatusBarPowerMenuStyle == STATUS_BAR_POWER_MENU_DEFAULT) {
                goToSleep();
            } else if (mStatusBarPowerMenuStyle == STATUS_BAR_POWER_MENU_INVERTED) {
                triggerPowerMenuDialog();
            }
        }
    }

    private void startDateLongClickActivity() {
        Intent intent = new Intent(Intent.ACTION_INSERT);
            intent.setData(Events.CONTENT_URI);
        mActivityStarter.startActivity(intent, true /* dismissShade */);
    }

    private void startForecastActivity() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setComponent(WeatherControllerImpl.COMPONENT_WEATHER_FORECAST);
        mActivityStarter.startActivity(intent, true /* dismissShade */);
    }

    private void startForecastLongClickActivity() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.cyanogenmod.lockclock",
            "com.cyanogenmod.lockclock.preference.Preferences");
        mActivityStarter.startActivity(intent, true /* dismissShade */);
    }

    private void startHeadsUpActivity() {
        Settings.System.putIntForUser(mContext.getContentResolver(),
            Settings.System.HEADS_UP_USER_ENABLED,
            getUserHeadsUpState() ? 0 : 1, UserHandle.USER_CURRENT);
        mActivityStarter.startAction(true /* dismissShade */);

        /* show a toast */
        String enabled = mContext.getString(R.string.heads_up_enabled);
        String disabled = mContext.getString(R.string.heads_up_disabled);
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(mContext,
            getUserHeadsUpState() ? enabled : disabled, duration);
        toast.show();
    }

    private void startHeadsUpLongClickActivity() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.android.settings",
            "com.android.settings.Settings$HeadsUpSettingsActivity");
        mActivityStarter.startActivity(intent, true /* dismissShade */);
    }

    private void startTaskManagerLongClickActivity() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.android.settings",
            "com.android.settings.Settings$RunningServicesActivity");
        mActivityStarter.startActivity(intent, true /* dismissShade */);
    }

    public void setQSPanel(QSPanel qsp) {
        mQSPanel = qsp;
        if (mQSPanel != null) {
            mQSPanel.setCallback(mQsPanelCallback);
        }
        mMultiUserSwitch.setQsPanel(qsp);
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return true;
    }

    public void setShowEmergencyCallsOnly(boolean show) {
        boolean changed = show != mShowEmergencyCallsOnly;
        if (changed) {
            mShowEmergencyCallsOnly = show;
            if (mExpanded) {
                updateEverything();
                requestCaptureValues();
            }
        }
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {
        // We don't want that everything lights up when we click on the header,
        // so block the request here.
    }

    private void captureLayoutValues(LayoutValues target) {
        target.timeScale = mExpanded ? 1f : mClockCollapsedScaleFactor;
        target.clockY = mClock.getBottom();
        target.dateY = mDateGroup.getTop();
        target.emergencyCallsOnlyAlpha = getAlphaForVisibility(mEmergencyCallsOnly);
        target.alarmStatusAlpha = getAlphaForVisibility(mAlarmStatus);
        target.dateCollapsedAlpha = getAlphaForVisibility(mDateCollapsed);
        target.dateExpandedAlpha = getAlphaForVisibility(mDateExpanded);
        target.avatarScale = mMultiUserAvatar.getScaleX();
        target.avatarX = mMultiUserSwitch.getLeft() + mMultiUserAvatar.getLeft();
        target.avatarY = mMultiUserSwitch.getTop() + mMultiUserAvatar.getTop();
        target.weatherY = mClock.getBottom() - mWeatherLine1.getHeight();
        if (getLayoutDirection() == LAYOUT_DIRECTION_LTR) {
            target.batteryX = mSystemIconsSuperContainer.getLeft()
                    + mSystemIconsContainer.getRight();
        } else {
            target.batteryX = mSystemIconsSuperContainer.getLeft()
                    + mSystemIconsContainer.getLeft();
        }
        target.batteryY = mSystemIconsSuperContainer.getTop() + mSystemIconsContainer.getTop();
        target.batteryLevelAlpha = getAlphaForVisibility(mBatteryLevel);
        target.statusBarPowerMenuAlpha = getAlphaForVisibility(mStatusBarPowerMenu);
        target.taskManagerAlpha = getAlphaForVisibility(mTaskManagerButton);
        target.headsUpAlpha = getAlphaForVisibility(mHeadsUpButton);
        target.settingsAlpha = getAlphaForVisibility(mSettingsButton);
        target.settingsTranslation = (mExpanded
                ? 0
                : mMultiUserSwitch.getLeft() - mSettingsButton.getLeft());
        if (mTaskManagerButton != null) {
            target.taskManagerTranslation = (mExpanded
                    ? 0
                    : mMultiUserSwitch.getLeft() - mTaskManagerButton.getLeft());
        }
        if (mHeadsUpButton != null) {
            target.headsUpTranslation = (mExpanded
                    ? 0
                    : mSettingsButton.getLeft()  - mHeadsUpButton.getLeft());
        }
        if (mStatusBarPowerMenu != null) {
            target.statusBarPowerMenuTranslation = (mExpanded
                    ? 0
                    : mSettingsButton.getLeft()  - mStatusBarPowerMenu.getLeft());
        }
        target.signalClusterAlpha = (mSignalClusterDetached ? 0f : 1f);
        target.settingsRotation = (mExpanded ? 0f : 180f);
    }

    private float getAlphaForVisibility(View v) {
        return v == null || v.getVisibility() == View.VISIBLE ? 1f : 0f;
    }

    private void applyAlpha(View v, float alpha) {
        if (v == null || v.getVisibility() == View.GONE) {
            return;
        }
        if (alpha == 0f) {
            v.setVisibility(View.INVISIBLE);
        } else {
            v.setVisibility(View.VISIBLE);
            v.setAlpha(alpha);
        }
    }

    private void applyLayoutValues(LayoutValues values) {
        mTime.setScaleX(values.timeScale);
        mTime.setScaleY(values.timeScale);
        mClock.setY(values.clockY - mClock.getHeight());
        mDateGroup.setY(values.dateY);
        mWeatherContainer.setY(values.weatherY);
        mAlarmStatus.setY(values.dateY - mAlarmStatus.getPaddingTop());
        mMultiUserAvatar.setScaleX(values.avatarScale);
        mMultiUserAvatar.setScaleY(values.avatarScale);
        mMultiUserAvatar.setX(values.avatarX - mMultiUserSwitch.getLeft());
        mMultiUserAvatar.setY(values.avatarY - mMultiUserSwitch.getTop());
        if (getLayoutDirection() == LAYOUT_DIRECTION_LTR) {
            mSystemIconsSuperContainer.setX(values.batteryX - mSystemIconsContainer.getRight());
        } else {
            mSystemIconsSuperContainer.setX(values.batteryX - mSystemIconsContainer.getLeft());
        }
        mSystemIconsSuperContainer.setY(values.batteryY - mSystemIconsContainer.getTop());
        if (mSignalCluster != null) {
            if (mExpanded) {
                if (getLayoutDirection() == LAYOUT_DIRECTION_LTR) {
                    mSignalCluster.setX(mSystemIconsSuperContainer.getX()
                            - mSignalCluster.getWidth());
                } else {
                    mSignalCluster.setX(mSystemIconsSuperContainer.getX()
                            + mSystemIconsSuperContainer.getWidth());
                }
                mSignalCluster.setY(
                        mSystemIconsSuperContainer.getY() + mSystemIconsSuperContainer.getHeight()/2
                                - mSignalCluster.getHeight()/2);
            } else {
                mSignalCluster.setTranslationX(0f);
                mSignalCluster.setTranslationY(0f);
            }
        }
        mSettingsButton.setTranslationY(mSystemIconsSuperContainer.getTranslationY());
        mSettingsButton.setTranslationX(values.settingsTranslation);
        mSettingsButton.setRotation(values.settingsRotation);

        if (mShowHeadsUpButton && mHeadsUpButton != null) {
            mHeadsUpButton.setRotation(values.settingsRotation);
            mHeadsUpButton.setTranslationX(values.settingsTranslation+values.headsUpTranslation);
            mHeadsUpButton.setTranslationY(mSystemIconsSuperContainer.getTranslationY());
        }
        if (mStatusBarPowerMenu != null &&
            mStatusBarPowerMenuStyle != STATUS_BAR_POWER_MENU_OFF) {
            mStatusBarPowerMenu.setRotation(values.settingsRotation);
            mStatusBarPowerMenu.setTranslationX(values.settingsTranslation+values.statusBarPowerMenuTranslation);
            mStatusBarPowerMenu.setTranslationY(mSystemIconsSuperContainer.getTranslationY());
        }
        if (mShowTaskManager && mTaskManagerButton != null) {
            mTaskManagerButton.setRotation(values.settingsRotation);
            mTaskManagerButton.setTranslationX(values.settingsTranslation+values.taskManagerTranslation);
            mTaskManagerButton.setTranslationY(mSystemIconsSuperContainer.getTranslationY());
        }

        applyAlpha(mEmergencyCallsOnly, values.emergencyCallsOnlyAlpha);
        if (!mShowingDetail) {
            // Otherwise it needs to stay invisible
            applyAlpha(mAlarmStatus, values.alarmStatusAlpha);
        }
        applyAlpha(mDateCollapsed, values.dateCollapsedAlpha);
        applyAlpha(mDateExpanded, values.dateExpandedAlpha);
        applyAlpha(mBatteryLevel, values.batteryLevelAlpha);
        if (mDockBatteryLevel != null) {
            applyAlpha(mDockBatteryLevel, values.batteryLevelAlpha);
        }
        applyAlpha(mSettingsButton, values.settingsAlpha);
        applyAlpha(mWeatherLine1, values.settingsAlpha);
        applyAlpha(mWeatherLine2, values.settingsAlpha);
        applyAlpha(mSignalCluster, values.signalClusterAlpha);

        applyAlpha(mHeadsUpButton, values.headsUpAlpha);
        applyAlpha(mStatusBarPowerMenu, values.statusBarPowerMenuAlpha);
        applyAlpha(mTaskManagerButton, values.taskManagerAlpha);

        if (!mExpanded) {
            mTime.setScaleX(1f);
            mTime.setScaleY(1f);
        }
        updateAmPmTranslation();
    }

    /**
     * Captures all layout values (position, visibility) for a certain state. This is used for
     * animations.
     */
    private static final class LayoutValues {

        float dateExpandedAlpha;
        float dateCollapsedAlpha;
        float emergencyCallsOnlyAlpha;
        float alarmStatusAlpha;
        float timeScale = 1f;
        float clockY;
        float dateY;
        float avatarScale;
        float avatarX;
        float avatarY;
        float batteryX;
        float batteryY;
        float batteryLevelAlpha;
        float batteryLevelExpandedAlpha;
        float taskManagerAlpha;
        float taskManagerTranslation;
        float headsUpAlpha;
        float headsUpTranslation;
        float settingsAlpha;
        float settingsTranslation;
        float signalClusterAlpha;
        float settingsRotation;
        float weatherY;
        float statusBarPowerMenuTranslation;
        float statusBarPowerMenuAlpha;

        public void interpolate(LayoutValues v1, LayoutValues v2, float t) {
            float factor = 1 - t;
            timeScale = v1.timeScale * factor + v2.timeScale * t;
            clockY = v1.clockY * factor + v2.clockY * t;
            dateY = v1.dateY * factor + v2.dateY * t;
            avatarScale = v1.avatarScale * factor + v2.avatarScale * t;
            avatarX = v1.avatarX * factor + v2.avatarX * t;
            avatarY = v1.avatarY * factor + v2.avatarY * t;
            batteryX = v1.batteryX * factor + v2.batteryX * t;
            batteryY = v1.batteryY * factor + v2.batteryY * t;

            statusBarPowerMenuTranslation = v1.statusBarPowerMenuTranslation * factor
                    + v2.statusBarPowerMenuTranslation * t;
            taskManagerTranslation = v1.taskManagerTranslation * factor
                    + v2.taskManagerTranslation * t;
            headsUpTranslation = v1.headsUpTranslation * factor
                    + v2.headsUpTranslation * t;
            settingsTranslation = v1.settingsTranslation * factor
                    + v2.settingsTranslation * t;
            weatherY = v1.weatherY * factor
                    + v2.weatherY * t;

            float t1 = Math.max(0, t - 0.5f) * 2;
            factor = 1 - t1;
            settingsRotation = v1.settingsRotation * factor
                    + v2.settingsRotation * t1;
            emergencyCallsOnlyAlpha = v1.emergencyCallsOnlyAlpha * factor
                    + v2.emergencyCallsOnlyAlpha * t1;

            float t2 = Math.min(1, 2 * t);
            factor = 1 - t2;
            signalClusterAlpha = v1.signalClusterAlpha * factor
                    + v2.signalClusterAlpha * t2;

            float t3 = Math.max(0, t - 0.7f) / 0.3f;
            factor = 1 - t3;
            batteryLevelAlpha = v1.batteryLevelAlpha * factor
                    + v2.batteryLevelAlpha * t3;
            batteryLevelExpandedAlpha = v1.batteryLevelExpandedAlpha * factor
                    + v2.batteryLevelExpandedAlpha * t3;
            settingsAlpha = v1.settingsAlpha * factor
                    + v2.settingsAlpha * t3;
            dateExpandedAlpha = v1.dateExpandedAlpha * factor
                    + v2.dateExpandedAlpha * t3;
            dateCollapsedAlpha = v1.dateCollapsedAlpha * factor
                    + v2.dateCollapsedAlpha * t3;
            alarmStatusAlpha = v1.alarmStatusAlpha * factor
                    + v2.alarmStatusAlpha * t3;

            statusBarPowerMenuAlpha = v1.statusBarPowerMenuAlpha * factor
                    + v2.statusBarPowerMenuAlpha * t3;
            headsUpAlpha = v1.headsUpAlpha * factor
                    + v2.headsUpAlpha * t3;
            taskManagerAlpha = v1.taskManagerAlpha * factor
                    + v2.taskManagerAlpha * t3;
        }
    }

    private final QSPanel.Callback mQsPanelCallback = new QSPanel.Callback() {
        private boolean mScanState;

        @Override
        public void onToggleStateChanged(final boolean state) {
            post(new Runnable() {
                @Override
                public void run() {
                    handleToggleStateChanged(state);
                }
            });
        }

        @Override
        public void onShowingDetail(final QSTile.DetailAdapter detail) {
            post(new Runnable() {
                @Override
                public void run() {
                    handleShowingDetail(detail);
                }
            });
        }

        @Override
        public void onScanStateChanged(final boolean state) {
            post(new Runnable() {
                @Override
                public void run() {
                    handleScanStateChanged(state);
                }
            });
        }

        private void handleToggleStateChanged(boolean state) {
            mQsDetailHeaderSwitch.setChecked(state);
        }

        private void handleScanStateChanged(boolean state) {
            if (mScanState == state) return;
            mScanState = state;
            final Animatable anim = (Animatable) mQsDetailHeaderProgress.getDrawable();
            if (state) {
                mQsDetailHeaderProgress.animate().alpha(1f);
                anim.start();
            } else {
                mQsDetailHeaderProgress.animate().alpha(0f);
                anim.stop();
            }
        }

        private void handleShowingDetail(final QSTile.DetailAdapter detail) {
            final boolean showingDetail = detail != null;
            transition(mClock, !showingDetail);
            transition(mDateGroup, !showingDetail);

            if (mExpanded && mShowHeadsUpButton) {
                transition(mHeadsUpButton, !showingDetail);
            }
            if (mExpanded && mShowTaskManager) {
                transition(mTaskManagerButton, !showingDetail);
            }
            if (isStatusBarPowerMenuVisible()) {
                transition(mStatusBarPowerMenu, !showingDetail);
            }
            updateStatusBarPowerMenuVisibility();

            if (mShowWeather) {
                transition(mWeatherContainer, !showingDetail);
            }
            if (mAlarmShowing) {
                transition(mAlarmStatus, !showingDetail);
            }
            transition(mQsDetailHeader, showingDetail);
            mShowingDetail = showingDetail;
            if (showingDetail) {
                mQsDetailHeaderTitle.setText(detail.getTitle());
                if (mQSCSwitch) {
                    int color = Settings.System.getInt(
                            getContext().getContentResolver(),
                            Settings.System.QS_TEXT_COLOR,
                            DEFAULT_HEADER_COLOR);
                    mQsDetailHeaderTitle.setTextColor(color);
                }
                final Boolean toggleState = detail.getToggleState();
                if (toggleState == null) {
                    mQsDetailHeaderSwitch.setVisibility(INVISIBLE);
                    mQsDetailHeader.setClickable(false);
                } else {
                    mQsDetailHeaderSwitch.setVisibility(VISIBLE);
                    mQsDetailHeaderSwitch.setChecked(toggleState);
                    mQsDetailHeader.setClickable(true);
                    mQsDetailHeader.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            detail.setToggleState(!mQsDetailHeaderSwitch.isChecked());
                        }
                    });
                }
            } else {
                mQsDetailHeader.setClickable(false);
            }
        }

        private void transition(final View v, final boolean in) {
            if (v == null) return;
            if (in) {
                v.bringToFront();
                v.setVisibility(VISIBLE);
            }
            v.animate()
                    .alpha(in ? 1 : 0)
                    .withLayer()
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            if (!in) {
                                v.setVisibility(INVISIBLE);
                            }
                        }
                    })
                    .start();
        }
    };

    class SettingsObserver extends UserContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        protected void observe() {
            super.observe();

            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY_STYLE),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_HEADER_SHOW_WEATHER),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_HEADER_SHOW_WEATHER_LOCATION),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_HEADER_TEXT_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_HEADER_ICON_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QS_COLOR_SWITCH),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.HEADS_UP_SHOW_STATUS_BUTTON),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.ENABLE_TASK_MANAGER),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_POWER_MENU),
                    false, this);

            update();
        }

        @Override
        protected void unobserve() {
            super.unobserve();

            ContentResolver resolver = mContext.getContentResolver();
            resolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateWithUri(uri);
        }

        public void update() {
            updateWithUri(null);
        }

        public void updateWithUri(Uri uri) {
            ContentResolver resolver = mContext.getContentResolver();
            boolean updateAll = (uri == null);

            if (updateAll ||
                uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY_STYLE))
                || uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT))) {
                updateBatteryPercentageSettings();
            }
            if (updateAll ||
                uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_HEADER_SHOW_WEATHER))
                || uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_HEADER_SHOW_WEATHER_LOCATION))) {
                updateWeatherSettings();
            }
            if (updateAll ||
                uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_HEADER_BG_COLOR))) {
                updateBackgroundColor();
            }
            if (updateAll ||
                uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_HEADER_TEXT_COLOR))) {
                updateTextColorSettings();
            }
            if (updateAll ||
                uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_EXPANDED_HEADER_ICON_COLOR))) {
                updateIconColorSettings();
            }
            if (updateAll ||
                uri.equals(Settings.System.getUriFor(
                    Settings.System.QS_COLOR_SWITCH))) {
                mQSCSwitch = Settings.System.getInt(resolver,
                    Settings.System.QS_COLOR_SWITCH, 0) == 1;
                if (mQSCSwitch) {
                    int color = Settings.System.getInt(
                            getContext().getContentResolver(),
                            Settings.System.QS_TEXT_COLOR,
                            DEFAULT_HEADER_COLOR);
                    mQsDetailHeaderTitle.setTextColor(color);
                }
            }

            boolean updateLayout = false;
            if (updateAll ||
                uri.equals(Settings.System.getUriFor(
                    Settings.System.ENABLE_TASK_MANAGER))) {
                mShowTaskManager = Settings.System.getInt(resolver,
                    Settings.System.ENABLE_TASK_MANAGER, 0) == 1;
                updateLayout = true;
            }
            if (updateAll ||
                uri.equals(Settings.System.getUriFor(
                    Settings.System.HEADS_UP_SHOW_STATUS_BUTTON))) {
                mShowHeadsUpButton = Settings.System.getIntForUser(resolver,
                    Settings.System.HEADS_UP_SHOW_STATUS_BUTTON,
                    0, UserHandle.USER_CURRENT) == 1;
                updateHeadsUpButton();
                updateLayout = true;
            }
            if (updateAll ||
                uri.equals(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_POWER_MENU))) {
                mStatusBarPowerMenuStyle = Settings.System.getInt(resolver,
                        Settings.System.STATUS_BAR_POWER_MENU, 0);
                updateStatusBarPowerMenuVisibility();
                updateLayout = true;
            }
            if (updateLayout) {
                updateSystemIconsLayoutParams();
            }
        }
    }

    private void updateBatteryPercentageSettings() {
        updateBatteryLevelVisibility();
    }

    private void updateWeatherSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        mShowWeather = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_EXPANDED_HEADER_SHOW_WEATHER, 0) == 1;
        mShowWeatherLocation = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_EXPANDED_HEADER_SHOW_WEATHER_LOCATION, 1) == 1;
        updateWeatherVisibility();
    }

    private void updateBackgroundColor() {
        ContentResolver resolver = mContext.getContentResolver();
        int backgroundColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_EXPANDED_HEADER_BG_COLOR,
                0xff384248);

        getBackground().setColorFilter(backgroundColor, Mode.MULTIPLY);
    }

    public void updateBatteryColorSettings(boolean isHeader) {
        mBatteryLevel.setTextColor(isHeader);
    }

    private void updateTextColorSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        mTextColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_EXPANDED_HEADER_TEXT_COLOR,
                DEFAULT_HEADER_COLOR);

        mTime.setTextColor(mTextColor);
        mAmPm.setTextColor(mTextColor);
        mDateCollapsed.setTextColor(
                getTransparentColor(mTextColor, 178));
        mDateExpanded.setTextColor(
                getTransparentColor(mTextColor, 178));
        updateBatteryColorSettings(mExpanded);
        mAlarmStatus.setTextColor(
                getTransparentColor(mTextColor, 100));
        mWeatherLine1.setTextColor(mTextColor);
        mWeatherLine2.setTextColor(mTextColor);
    }

    private void updateIconColorSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        mIconColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_EXPANDED_HEADER_ICON_COLOR,
                DEFAULT_HEADER_COLOR);

        ((ImageView)mSettingsButton).setColorFilter(mIconColor, Mode.MULTIPLY);
        if (mStatusBarPowerMenu != null) {
            ((ImageView)mStatusBarPowerMenu).setColorFilter(mIconColor, Mode.MULTIPLY);
        }
        if (mHeadsUpButton != null) {
            ((ImageView)mHeadsUpButton).setColorFilter(mIconColor, Mode.MULTIPLY);
        }
        if (mTaskManagerButton != null) {
            ((ImageView)mTaskManagerButton).setColorFilter(mIconColor, Mode.MULTIPLY);
        }
        Drawable alarmIcon = getResources().getDrawable(R.drawable.ic_access_alarms_small);
        alarmIcon.setColorFilter(mIconColor, Mode.MULTIPLY);
        mAlarmStatus.setCompoundDrawablesWithIntrinsicBounds(alarmIcon, null, null, null);
    }

    private int getTransparentColor(int color, int alpha) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        int transparentColor = (alpha << 24) + (r << 16) + (g << 8) + b;
        return transparentColor;
    }

    private void goToSleep() {
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        pm.goToSleep(SystemClock.uptimeMillis());
    }

    private boolean getUserHeadsUpState() {
         return Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.HEADS_UP_USER_ENABLED,
                Settings.System.HEADS_UP_USER_ON,
                UserHandle.USER_CURRENT) != 0;
    }

    private void updateHeadsUpButton() {
        ImageView iv = (ImageView)findViewById(R.id.heads_up_button);
        if (iv == null) return;
        if (mShowHeadsUpButton) {
            if (getUserHeadsUpState()) {
                iv.setImageDrawable(
                    getResources().getDrawable(R.drawable.ic_heads_up_status_on));
            } else {
                iv.setImageDrawable(
                    getResources().getDrawable(R.drawable.ic_heads_up_status_off));
            }
        }
        mHeadsUpButton.setVisibility(mExpanded && mShowHeadsUpButton ? View.VISIBLE : View.GONE);
    }
}
