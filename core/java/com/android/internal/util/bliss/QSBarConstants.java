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

public class QSBarConstants {

    public static final String BUTTON_AIRPLANE   = "airplane";
    public static final String BUTTON_BLUETOOTH  = "bt";
    public static final String BUTTON_DATA       = "data";
    public static final String BUTTON_FLASHLIGHT = "flashlight";
    public static final String BUTTON_HOTSPOT    = "hotspot";
    public static final String BUTTON_INVERSION  = "inversion";
    public static final String BUTTON_LOCATION   = "location";
    public static final String BUTTON_LTE        = "lte";
    public static final String BUTTON_NFC        = "nfc";
    public static final String BUTTON_ROTATION   = "rotation";
    public static final String BUTTON_WIFI       = "wifi";

    public static final String BUTTON_DELIMITER  = "|";
    public static final String ICON_EMPTY        = "empty";

    public static final String QUICK_SETTINGS_BUTTONS_DEFAULT =
          BUTTON_WIFI       + BUTTON_DELIMITER
        + ICON_EMPTY        + BUTTON_DELIMITER
        + BUTTON_BLUETOOTH  + BUTTON_DELIMITER
        + ICON_EMPTY        + BUTTON_DELIMITER
        + BUTTON_AIRPLANE   + BUTTON_DELIMITER
        + ICON_EMPTY        + BUTTON_DELIMITER
        + BUTTON_ROTATION   + BUTTON_DELIMITER
        + ICON_EMPTY        + BUTTON_DELIMITER
        + BUTTON_LOCATION   + BUTTON_DELIMITER
        + ICON_EMPTY;
}
