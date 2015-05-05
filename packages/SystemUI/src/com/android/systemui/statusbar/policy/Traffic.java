package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff.Mode;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.UserHandle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import com.android.systemui.R;

public class Traffic extends TextView {
    public static final String TAG = "Traffic";
    private static final int TRAFFIC_UP_DOWN = 0;
    private static final int TRAFFIC_DOWN = 1;
    private static final int TRAFFIC_UP = 2;
    private static final int NO_TRAFFIC = 3;

    private boolean mAttached;
    private boolean mEnabled;
    private boolean mShowIcon;
    private boolean mHide;
    private boolean mShowDl;
    private boolean mShowUl;
    private int mState = NO_TRAFFIC;

    private int mIconColor;
    private boolean mIsBit;
    private int mSummaryTime;

    long totalRxBytes;
    long totalTxBytes;
    long lastUpdateTime;
    long trafficBurstStartTime;
    long trafficBurstStartRxBytes;
    long trafficBurstStartTxBytes;
    long keepOnUntil = Long.MIN_VALUE;
    NumberFormat decimalFormat = new DecimalFormat("##0.0");
    NumberFormat integerFormat = NumberFormat.getIntegerInstance();

    private int txtSizeSingle;
    private int txtSizeMulti;

    Resources mResources;

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();

            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_ENABLE_NETWORK_SPEED_INDICATOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_SPEED_HIDE_TRAFFIC),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_TRAFFIC_SUMMARY),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_SPEED_INDICATOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_SPEED_SHOW_ICON),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_SPEED_BIT_BYTE),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_SPEED_TEXT_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NETWORK_SPEED_ICON_COLOR),
                    false, this, UserHandle.USER_ALL);

            updateSettings();
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }

    }

    public Traffic(Context context) {
        this(context, null);
    }

    public Traffic(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Traffic(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mResources = getResources();
        txtSizeSingle = mResources.getDimensionPixelSize(R.dimen.net_traffic_single_text_size);
        txtSizeMulti = mResources.getDimensionPixelSize(R.dimen.net_traffic_multi_text_size);

        updateSettings();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            getContext().registerReceiver(mIntentReceiver, filter, null,
                    getHandler());

            SettingsObserver settingsObserver = new SettingsObserver(getHandler());
            settingsObserver.observe();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            getContext().unregisterReceiver(mIntentReceiver);
            mAttached = false;
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                updateSettings();
            }
        }
    };

    @Override
    public void onScreenStateChanged(int screenState) {
        if (screenState == SCREEN_STATE_OFF) {
            stopTrafficUpdates();
        } else {
            startTrafficUpdates();
        }
        super.onScreenStateChanged(screenState);
    }

    private void stopTrafficUpdates() {
        getHandler().removeCallbacks(mRunnable);
        setText("");
    }

    public void startTrafficUpdates() {

        if (getConnectAvailable()) {
            totalRxBytes = TrafficStats.getTotalRxBytes();
            totalTxBytes = TrafficStats.getTotalTxBytes();
            lastUpdateTime = SystemClock.elapsedRealtime();
            trafficBurstStartTime = Long.MIN_VALUE;

            getHandler().removeCallbacks(mRunnable);
            getHandler().post(mRunnable);
        }
    }

    private String formatTraffic(long trafffic, boolean speed) {
        if (trafffic > 10485760) { // 1024 * 1024 * 10
            return (speed ? "" : "(")
                    + integerFormat.format(trafffic / 1048576)
                    + (speed ? (mIsBit ? "Mbit/s" : "MB/s") : "MB)");
        } else if (trafffic > 1048576) { // 1024 * 1024
            return (speed ? "" : "(")
                    + decimalFormat.format(((float) trafffic) / 1048576f)
                    + (speed ? (mIsBit ? "Mbit/s" : "MB/s") : "MB)");
        } else if (trafffic > 10240) { // 1024 * 10
            return (speed ? "" : "(")
                    + integerFormat.format(trafffic / 1024)
                    + (speed ? (mIsBit ? "Kbit/s" : "KB/s") : "KB)");
        } else if (trafffic > 1024) { // 1024
            return (speed ? "" : "(")
                    + decimalFormat.format(((float) trafffic) / 1024f)
                    + (speed ? (mIsBit ? "Kbit/s" : "KB/s") : "KB)");
        } else {
            return (speed ? "" : "(")
                    + integerFormat.format(trafffic)
                    + (speed ? (mIsBit ? "bit/s" : "B/s") : "B)");
        }
    }

    private boolean getConnectAvailable() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) mContext
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

            return connectivityManager.getActiveNetworkInfo().isConnected();
        } catch (Exception ignored) {
        }
        return false;
    }

    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            long td = SystemClock.elapsedRealtime() - lastUpdateTime;

            if (td == 0 || !mEnabled) {
                // we just updated the view, nothing further to do
                return;
            }

            long currentRxBytes = TrafficStats.getTotalRxBytes();
            long currentTxBytes = TrafficStats.getTotalTxBytes();
            long newRxBytes = currentRxBytes - totalRxBytes;
            long newTxBytes = currentTxBytes - totalTxBytes;

            String output = "";
            int textSize = txtSizeSingle;
            int state = NO_TRAFFIC;

            if (mHide && newTxBytes == 0) {
                long trafficBurstTxBytes = currentTxBytes - trafficBurstStartTxBytes;

                if (trafficBurstTxBytes != 0 && mSummaryTime != 0) {
                    if (mShowUl) {
                        output = formatTraffic(trafficBurstTxBytes, false);
                        state = TRAFFIC_UP;
                    }

                    Log.i(TAG,
                            "Traffic burst tx ended: " + trafficBurstTxBytes + "B in "
                                    + (SystemClock.elapsedRealtime() - trafficBurstStartTime)
                                    / 1000 + "s");
                    keepOnUntil = SystemClock.elapsedRealtime() + mSummaryTime;
                    trafficBurstStartTime = Long.MIN_VALUE;
                    trafficBurstStartTxBytes = currentTxBytes;
                }
            } else {
                if (mHide && trafficBurstStartTime == Long.MIN_VALUE) {
                    trafficBurstStartTime = lastUpdateTime;
                    trafficBurstStartTxBytes = totalTxBytes;
                }
                if (mShowUl) {
                    output = formatTraffic(mIsBit ? newTxBytes * 8000 / td : newTxBytes * 1000 / td, true);
                    state = TRAFFIC_UP;
                }
            }

            if (mHide && newRxBytes == 0) {
                long trafficBurstRxBytes = currentRxBytes - trafficBurstStartRxBytes;

                if (trafficBurstRxBytes != 0 && mSummaryTime != 0) {
                    if (mShowDl) {
                        if (output != "") {
                            output += "\n";
                            textSize = txtSizeMulti;
                            state = TRAFFIC_UP_DOWN;
                        } else {
                            state = TRAFFIC_DOWN;
                        }
                        output += formatTraffic(trafficBurstRxBytes, false);
                    }

                    Log.i(TAG,
                            "Traffic burst rx ended: " + trafficBurstRxBytes + "B in "
                                    + (SystemClock.elapsedRealtime() - trafficBurstStartTime)
                                    / 1000 + "s");
                    keepOnUntil = SystemClock.elapsedRealtime() + mSummaryTime;
                    trafficBurstStartTime = Long.MIN_VALUE;
                    trafficBurstStartRxBytes = currentRxBytes;
                }
            } else {
                if (mHide && trafficBurstStartTime == Long.MIN_VALUE) {
                    trafficBurstStartTime = lastUpdateTime;
                    trafficBurstStartRxBytes = totalRxBytes;
                }
                if (mShowDl) {
                    if (output != "") {
                        output += "\n";
                        textSize = txtSizeMulti;
                        state = TRAFFIC_UP_DOWN;
                    } else {
                        state = TRAFFIC_DOWN;
                    }
                    output += formatTraffic(mIsBit ? newRxBytes * 8000 / td : newRxBytes * 1000 / td, true);
                }
            }

            setTextSize(TypedValue.COMPLEX_UNIT_PX, (float)textSize);
            setText(output);
            if (mState != state) {
                mState = state;
                updateDrawable(mIconColor, mState);
            }

            // Hide if there is no traffic
            if (mShowDl || mShowUl) {
                if (mHide && newRxBytes == 0 && newTxBytes == 0) {
                    if (getVisibility() != GONE
                            && keepOnUntil < SystemClock.elapsedRealtime()) {
                        setText("");
                        setVisibility(View.GONE);
                    }
                } else {
                    if (getVisibility() != VISIBLE) {
                        setVisibility(View.VISIBLE);
                    }
                }
            } else {
                setText("");
                setVisibility(View.GONE);
            }

            totalRxBytes = currentRxBytes;
            totalTxBytes = currentTxBytes;
            lastUpdateTime = SystemClock.elapsedRealtime();
            if (getHandler() != null) {
                getHandler().postDelayed(mRunnable, 500);
            }
        }
    };

    public void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();

        mEnabled = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_NETWORK_SPEED_INDICATOR, 3,
                UserHandle.USER_CURRENT) != 3;
        int indicator = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_NETWORK_SPEED_INDICATOR, 3,
                UserHandle.USER_CURRENT);
        mHide = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_NETWORK_SPEED_HIDE_TRAFFIC, 1,
                UserHandle.USER_CURRENT) == 1;
        mSummaryTime = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_TRAFFIC_SUMMARY, 3000,
                UserHandle.USER_CURRENT);
        mShowIcon = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_NETWORK_SPEED_SHOW_ICON, 1,
                UserHandle.USER_CURRENT) == 1;
        mIsBit = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_NETWORK_SPEED_BIT_BYTE, 0,
                UserHandle.USER_CURRENT) == 1;
        int textColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_NETWORK_SPEED_TEXT_COLOR,
                0xffffffff, UserHandle.USER_CURRENT);
        mIconColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_NETWORK_SPEED_ICON_COLOR,
                0xffffffff, UserHandle.USER_CURRENT);

        mShowDl = (indicator == 1 || indicator == 0) ? true : false;
        mShowUl = (indicator == 2 || indicator == 0) ? true : false;

        if (mEnabled && getConnectAvailable()) {
            setVisibility(View.VISIBLE);
            if (mAttached) {
                startTrafficUpdates();
            }
        } else {
            setVisibility(View.GONE);
            setText("");
        }

        setTextColor(textColor);
        updateDrawable(mIconColor, mState);
    }

    private void updateDrawable(int color, int state) {
        Drawable drawable = null;

        if (mShowIcon) {
            if (state == TRAFFIC_UP) {
                drawable = mResources.getDrawable(R.drawable.stat_sys_network_traffic_up);
            } else if (state == TRAFFIC_DOWN) {
                drawable = mResources.getDrawable(R.drawable.stat_sys_network_traffic_down);
            } else if (state == TRAFFIC_UP_DOWN) {
                drawable = mResources.getDrawable(R.drawable.stat_sys_network_traffic_updown);
            }

            if (drawable != null) {
                drawable.setColorFilter(color, Mode.MULTIPLY);
            }
        }
        setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                mShowIcon ? drawable : null,
                null);
    }
}
