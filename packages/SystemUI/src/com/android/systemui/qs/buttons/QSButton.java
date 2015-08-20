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
 * limitations under the License
 */

package com.android.systemui.qs.buttons;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.android.systemui.qs.QSBar;

import com.android.internal.util.bliss.ImageHelper;

public class QSButton extends ImageView {
    protected final Context mContext;
    protected final QSBar mQSBar;
    private final Drawable mIconEnabled;
    private final Drawable mIconDisabled;

    public QSButton(Context context, QSBar qsBar, Drawable iconEnabled,
            Drawable iconDisabled) {
        super(context);

        mContext = context;
        mQSBar = qsBar;
        mIconEnabled = iconEnabled;
        mIconDisabled = iconDisabled;
    }

    public void setListening(boolean listening) {
    }

    public void handleClick() {
    }

    public void handleLongClick() {
    }

    protected void updateState(boolean enabled) {
        setImageDrawable(ImageHelper.resize(mContext, enabled ?
                mIconEnabled : mIconDisabled, 32));
        setAlpha(enabled ? 255 : 77);
    }
}
