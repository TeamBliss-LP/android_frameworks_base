/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.systemui.qs;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.android.systemui.R;

/**
 * Wrapper view with background which contains {@link QSPanel}
 */
public class QSContainer extends FrameLayout {

    private static final int QS_TYPE_PANEL = 0;
    private static final int QS_TYPE_BAR   = 1;

    private int mHeightOverride = -1;
    private QSBarContainer mQSBarContainer;
    private QSBar mQSBar;
    private QSPanel mQSPanel;
    private int mQSType;

    public QSContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mQSBarContainer =
                (QSBarContainer) findViewById(R.id.quick_settings_bar_container);
        mQSBar = (QSBar) findViewById(R.id.quick_settings_bar);
        mQSPanel = (QSPanel) findViewById(R.id.quick_settings_panel);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateBottom();
    }

    public void setQSType(int qsType) {
        mQSType = qsType;
        if (mQSType == QS_TYPE_PANEL) {
            mQSBarContainer.setVisibility(View.GONE);
            mQSBar.setVisibility(View.GONE);
            mQSPanel.setVisibility(View.INVISIBLE);
            setVisibility(View.INVISIBLE);
        } else if (mQSType == QS_TYPE_BAR) {
            mQSPanel.setVisibility(View.GONE);
            mQSBarContainer.setVisibility(View.INVISIBLE);
            mQSBar.setVisibility(View.INVISIBLE);
            setVisibility(View.INVISIBLE);
        } else {
            mQSBarContainer.setVisibility(View.GONE);
            mQSBar.setVisibility(View.GONE);
            mQSPanel.setVisibility(View.GONE);
            setVisibility(View.GONE);
        }
    }

    public void setQSTypeVisibility(boolean visible) {
        if (mQSType == QS_TYPE_PANEL) {
            mQSPanel.setVisibility(visible ?  View.VISIBLE : View.INVISIBLE);
        } else if (mQSType == QS_TYPE_BAR) {
            mQSBarContainer.setVisibility(visible ?  View.VISIBLE : View.INVISIBLE);
            mQSBar.setVisibility(visible ?  View.VISIBLE : View.INVISIBLE);
        }
    }

    public void setListening(boolean listening) {
        mQSPanel.setListening(listening && mQSType == QS_TYPE_PANEL);
        mQSBar.setListening(listening && mQSType == QS_TYPE_BAR);
    }
        
    /**
     * Overrides the height of this view (post-layout), so that the content is clipped to that
     * height and the background is set to that height.
     *
     * @param heightOverride the overridden height
     */
    public void setHeightOverride(int heightOverride) {
        mHeightOverride = heightOverride;
        updateBottom();
    }

    /**
     * The height this view wants to be. This is different from {@link #getMeasuredHeight} such that
     * during closing the detail panel, this already returns the smaller height.
     */
    public int getDesiredHeight() {
        if (mQSPanel.isClosingDetail()) {
            return mQSPanel.getGridHeight() + getPaddingTop() + getPaddingBottom();
        } else {
            return getMeasuredHeight();
        }
    }

    private void updateBottom() {
        int height = mHeightOverride != -1 ? mHeightOverride : getMeasuredHeight();
        setBottom(getTop() + height);
    }
}
