/*
 * Copyright (C) 2015 DarkKat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.qs;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

public class QSBarContainer extends HorizontalScrollView {

    enum EventStates {
        SCROLLING,
        FLING
    }

    private EventStates systemState = EventStates.SCROLLING;

    public QSBarContainer(Context context) {
        super(context);
    }

    public QSBarContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public QSBarContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    Runnable mSnapRunnable = new Runnable(){
        @Override
        public void run() {
            snapItems();
            systemState = EventStates.SCROLLING;
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            systemState = EventStates.FLING;
        } else if (action == MotionEvent.ACTION_DOWN) {
            systemState = EventStates.SCROLLING;
            removeCallbacks(mSnapRunnable);
        }
        return super.onTouchEvent(ev);
    }

    private void snapItems() {
        Rect parentBounds = new Rect();
        getDrawingRect(parentBounds);
        Rect childBounds = new Rect();
        ViewGroup parent = (ViewGroup) getChildAt(0);
        for (int i = 0; i < parent.getChildCount(); i++) {
            View view = parent.getChildAt(i);
            view.getHitRect(childBounds);
            if (childBounds.right >= parentBounds.left && childBounds.left <= parentBounds.left) {
                // First partially visible child
                if ((childBounds.right - parentBounds.left) >= (parentBounds.left - childBounds.left)) {
                    smoothScrollTo(Math.abs(childBounds.left), 0);
                } else {
                    smoothScrollTo(Math.abs(childBounds.right), 0);
                }
                break;
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        post(new Runnable() {
            public void run() {
                requestLayout();
            }
        });
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        if (systemState == EventStates.SCROLLING) {
            return;
        }
        if (Math.abs(l - oldl) <= 1 && systemState == EventStates.FLING) {
            removeCallbacks(mSnapRunnable);
            postDelayed(mSnapRunnable, 100);
        }
    }
}
