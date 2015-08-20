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

package com.android.internal.util.bliss;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.util.bliss.ActionConfig;
import com.android.internal.util.bliss.ConfigSplitHelper;

import java.util.ArrayList;

public class QSBarHelper {

    private static final String SYSTEMUI_METADATA_NAME = "com.android.systemui";

    // get and set the quick settings bar configs from provider and return propper arraylist objects
    // @ActionConfig
    public static ArrayList<ActionConfig> getQSBarConfig(Context context) {
        return (ConfigSplitHelper.getActionConfigValues(context,
            getQSBarProvider(context), null, null, true));
    }

    // get @ActionConfig with description if needed and other then an app description
    public static ArrayList<ActionConfig> getQSBarConfigWithDescription(
            Context context, String values, String entries) {
        return (ConfigSplitHelper.getActionConfigValues(context,
            getQSBarProvider(context), values, entries, true));
    }

    private static String getQSBarProvider(Context context) {
        String config = Settings.System.getStringForUser(
                    context.getContentResolver(),
                    Settings.System.QS_BUTTONS,
                    UserHandle.USER_CURRENT);
        if (config == null) {
            config = QSBarConstants.QUICK_SETTINGS_BUTTONS_DEFAULT;
        }
        return config;
    }

    public static void setQSBarConfig(Context context,
            ArrayList<ActionConfig> actionConfig, boolean reset) {
        String config;
        if (reset) {
            config = QSBarConstants.QUICK_SETTINGS_BUTTONS_DEFAULT;
        } else {
            config = ConfigSplitHelper.setActionConfig(actionConfig, true);
        }
        Settings.System.putString(context.getContentResolver(),
                    Settings.System.QS_BUTTONS,
                    config);
    }

    public static Drawable getQSBarIconImage(Context context, String clickAction) {
        int resId = -1;
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return null;
        }

        Resources systemUiResources;
        try {
            systemUiResources = pm.getResourcesForApplication(SYSTEMUI_METADATA_NAME);
        } catch (Exception e) {
            Log.e("QSBarHelper:", "can't access systemui resources",e);
            return null;
        }


        if (clickAction.equals(QSBarConstants.BUTTON_AIRPLANE)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_qs_button_airplane", null, null);
        } else if (clickAction.equals(QSBarConstants.BUTTON_BLUETOOTH)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_qs_button_bt", null, null);
        } else if (clickAction.equals(QSBarConstants.BUTTON_DATA)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_qs_button_data", null, null);
        } else if (clickAction.equals(QSBarConstants.BUTTON_FLASHLIGHT)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_qs_button_torch", null, null);
        } else if (clickAction.equals(QSBarConstants.BUTTON_HOTSPOT)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_qs_button_hotspot", null, null);
        } else if (clickAction.equals(QSBarConstants.BUTTON_INVERSION)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_qs_button_inversion", null, null);
        } else if (clickAction.equals(QSBarConstants.BUTTON_LOCATION)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_qs_button_location", null, null);
        } else if (clickAction.equals(QSBarConstants.BUTTON_LTE)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_qs_button_lte", null, null);
        } else if (clickAction.equals(QSBarConstants.BUTTON_NFC)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_qs_button_nfc", null, null);
        } else if (clickAction.equals(QSBarConstants.BUTTON_ROTATION)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_qs_button_rotation", null, null);
        } else if (clickAction.equals(QSBarConstants.BUTTON_WIFI)) {
            resId = systemUiResources.getIdentifier(
                        SYSTEMUI_METADATA_NAME + ":drawable/ic_qs_button_wifi", null, null);
        }

        if (resId > 0) {
            return systemUiResources.getDrawable(resId);
        } else {
            return null;
        }
    }
}
