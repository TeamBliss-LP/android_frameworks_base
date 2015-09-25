/*
 * Copyright (C) 2015 The CyanogenMod Open Source Project
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

package com.android.internal.util.cm;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.PowerManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.SparseArray;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.util.bliss.QsDeviceUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class QSUtils {
    private static boolean sAvailableTilesFiltered;
    private static final SparseArray<Context> sSystemUiContextForUser = new SparseArray<>();

    public interface OnQSChanged {
        void onQSChanged();
    }

    private QSUtils() {}

    public static boolean isStaticQsTile(String tileSpec) {
        return QSConstants.STATIC_TILES_AVAILABLE.contains(tileSpec);
    }

    public static boolean isDynamicQsTile(String tileSpec) {
        return QSConstants.DYNAMIC_TILES_AVAILABLE.contains(tileSpec);
    }

    public static List<String> getAvailableTiles(Context context) {
        filterTiles(context);
        return QSConstants.TILES_AVAILABLE;
    }

    public static List<String> getDefaultTiles(Context context) {
        final List<String> tiles = new ArrayList<>();
        final String defaults = context.getString(
                com.android.internal.R.string.config_defaultQuickSettingsTiles);
        if (!TextUtils.isEmpty(defaults)) {
            final String[] array = TextUtils.split(defaults, Pattern.quote(","));
            for (String item : array) {
                if (TextUtils.isEmpty(item)) {
                    continue;
                }
                tiles.add(item);
            }
            filterTiles(context, tiles);
        }
        return tiles;
    }

    public static String getDefaultTilesAsString(Context context) {
        List<String> list = getDefaultTiles(context);
        return TextUtils.join(",", list);
    }

    private static void filterTiles(Context context, List<String> tiles) {
        boolean deviceSupportsMobile = QsDeviceUtils.deviceSupportsMobileData(context);

        // Tiles that need conditional filtering
        Iterator<String> iterator = tiles.iterator();
        while (iterator.hasNext()) {
            String tileKey = iterator.next();
            boolean removeTile = false;
            switch (tileKey) {
                case QSConstants.TILE_CELLULAR:
                case QSConstants.TILE_HOTSPOT:
                case QSConstants.TILE_ROAMING:
                case QSConstants.TILE_APN:
                    removeTile = !deviceSupportsMobile;
                    break;
                case QSConstants.TILE_DDS:
                    removeTile = !QsDeviceUtils.deviceSupportsDdsSupported(context);
                    break;
                case QSConstants.TILE_CAST:
                    removeTile = !QsDeviceUtils.deviceSupportsRemoteDisplay(context);
                    break;
                case QSConstants.TILE_DATA:
                    removeTile = !QsDeviceUtils.deviceSupportsMobileData(context);
                    break;
                case QSConstants.TILE_FLASHLIGHT:
                    removeTile = !QsDeviceUtils.deviceSupportsFlashLight(context);
                    break;
                case QSConstants.TILE_BLUETOOTH:
                    removeTile = !QsDeviceUtils.deviceSupportsBluetooth();
                    break;
                case QSConstants.TILE_NFC:
                    removeTile = !QsDeviceUtils.deviceSupportsNfc(context);
                    break;
                case QSConstants.TILE_COMPASS:
                    removeTile = !QsDeviceUtils.deviceSupportsCompass(context);
                    break;
                case QSConstants.TILE_LTE:
                    removeTile = !QsDeviceUtils.deviceSupportsLte(context);
                    break;
                case QSConstants.TILE_AMBIENT_DISPLAY:
                    removeTile = !QsDeviceUtils.isDozeAvailable(context);
                    break;
                case QSConstants.TILE_PERFORMANCE:
                    removeTile = !deviceSupportsPowerProfiles(context);
                    break;

                case QSConstants.DYNAMIC_TILE_SU:
                    removeTile = !QsDeviceUtils.supportsRootAccess();
                    break;
            }
            if (removeTile) {
                iterator.remove();
            }
        }
    }

    private static void filterTiles(Context context) {
        if (!sAvailableTilesFiltered) {
            filterTiles(context, QSConstants.TILES_AVAILABLE);
            sAvailableTilesFiltered = true;
        }
    }

    public static int getDynamicQSTileResIconId(Context context, int userId, String tileSpec) {
        Context ctx = getQSTileContext(context, userId);
        int index = translateDynamicQsTileSpecToIndex(ctx, tileSpec);
        if (index == -1) {
            return 0;
        }

        try {
            String resourceName = ctx.getResources().getStringArray(
                    ctx.getResources().getIdentifier("dynamic_qs_tiles_icons_resources_ids",
                            "array", ctx.getPackageName()))[index];
            return ctx.getResources().getIdentifier(
                    resourceName, "drawable", ctx.getPackageName());
        } catch (Exception ex) {
            // Ignore
        }
        return 0;
    }

    public static String getDynamicQSTileLabel(Context context, int userId, String tileSpec) {
        Context ctx = getQSTileContext(context, userId);
        int index = translateDynamicQsTileSpecToIndex(ctx, tileSpec);
        if (index == -1) {
            return null;
        }

        try {
            return ctx.getResources().getStringArray(
                    ctx.getResources().getIdentifier("dynamic_qs_tiles_labels",
                            "array", ctx.getPackageName()))[index];
        } catch (Exception ex) {
            // Ignore
        }
        return null;
    }

    private static int translateDynamicQsTileSpecToIndex(Context context, String tileSpec) {
        String[] keys = context.getResources().getStringArray(context.getResources().getIdentifier(
                "dynamic_qs_tiles_values", "array", context.getPackageName()));
        int count = keys.length;
        for (int i = 0; i < count; i++) {
            if (keys[i].equals(tileSpec)) {
                return i;
            }
        }
        return -1;
    }

    public static Context getQSTileContext(Context context, int userId) {
        Context ctx = sSystemUiContextForUser.get(userId);
        if (ctx == null) {
            try {
                ctx = context.createPackageContextAsUser(
                        "com.android.systemui", 0, new UserHandle(userId));
                sSystemUiContextForUser.put(userId, ctx);
            } catch (NameNotFoundException ex) {
                // We can safely ignore this
            }
        }
        return ctx;
    }

    public static boolean isQSTileEnabledForUser(
            Context context, String tileSpec, int userId) {
        final ContentResolver resolver = context.getContentResolver();
        String order = Settings.Secure.getStringForUser(resolver,
                Settings.Secure.QS_TILES, userId);
        return !TextUtils.isEmpty(order) && Arrays.asList(order.split(",")).contains(tileSpec);
    }

    public static ContentObserver registerObserverForQSChanges(Context ctx, final OnQSChanged cb) {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                cb.onQSChanged();
            }
        };

        ctx.getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.QS_TILES),
                false, observer, UserHandle.USER_ALL);
        return observer;
    }

    public static void unregisterObserverForQSChanges(Context ctx, ContentObserver observer) {
        ctx.getContentResolver().unregisterContentObserver(observer);
    }

    public static boolean deviceSupportsDdsSupported(Context context) {
        TelephonyManager tm = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.isMultiSimEnabled()
                && tm.getMultiSimConfiguration() == TelephonyManager.MultiSimVariants.DSDA;
    }
    private static boolean isDozeAvailable(Context context) {
        String name = Build.IS_DEBUGGABLE ? SystemProperties.get("debug.doze.component") : null;
        if (TextUtils.isEmpty(name)) {
            name = context.getResources().getString(
                    com.android.internal.R.string.config_dozeComponent);
        }
        return !TextUtils.isEmpty(name);
    }

    public static boolean deviceSupportsPowerProfiles(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm.hasPowerProfiles();
    }

    private static boolean supportsRootAccess() {
        return Build.IS_DEBUGGABLE || "eng".equals(Build.TYPE);
    }
}
