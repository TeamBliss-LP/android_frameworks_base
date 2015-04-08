/*
 * Copyright (C) 2015 BOSP
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

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.android.systemui.R;
import com.android.systemui.qs.QSTile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HWKeysTile extends QSTile<QSTile.BooleanState> {
    private boolean mListening;
    private HWKeysObserver mObserver;

    public HWKeysTile(Host host) {
        super(host);
        mObserver = new HWKeysObserver(mHandler);
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleClick() {
        toggleState();
        refreshState();
    }

     @Override
    protected void handleSecondaryClick() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.android.settings",
            "com.android.settings.Settings$ButtonSettings");
        mHost.startSettingsActivity(intent);
    }

    @Override
    public void handleLongClick() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.android.settings",
            "com.android.settings.Settings$ButtonSettings");
        mHost.startSettingsActivity(intent);
    }

 protected void toggleState() {
         Settings.Secure.putInt(mContext.getContentResolver(),
                        Settings.Secure.ENABLE_HW_KEYS, !hwkeysEnabled() ? 1 : 0);
    }


    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.visible = true;
	if (hwkeysEnabled()) {
        state.iconId = R.drawable.ic_qs_buttons_on;
        state.label = mContext.getString(R.string.quick_settings_hwkeys_on);
	} else {
        state.iconId = R.drawable.ic_qs_buttons_off;
	state.label = mContext.getString(R.string.quick_settings_hwkeys_off);
	    }
	}

    private boolean hwkeysEnabled() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ENABLE_HW_KEYS, 1) == 1;
    }

    @Override
    public void setListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
        if (listening) {
            mObserver.startObserving();
        } else {
            mObserver.endObserving();
        }
    }

    private class HWKeysObserver extends ContentObserver {
        public HWKeysObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            refreshState();
        }

        public void startObserving() {
            mContext.getContentResolver().registerContentObserver(
                    Settings.Secure.getUriFor(Settings.System.ENABLE_HW_KEYS),
                    false, this);
        }

        public void endObserving() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }
    }
}

