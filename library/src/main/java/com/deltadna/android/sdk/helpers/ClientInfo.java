/*
 * Copyright (c) 2016 deltaDNA Ltd. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.deltadna.android.sdk.helpers;

import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public final class ClientInfo {
    
    public static String platform() {
        return "ANDROID";
    }
    
    public static String deviceName() {
        return Build.PRODUCT;
    }
    
    public static String deviceModel() {
        return Build.MODEL;
    }
    
    public static String deviceType() {
        return "UNKNOWN";
    }
    
    public static String operatingSystem() {
        return "ANDROID";
    }
    
    public static String operatingSystemVersion() {
        return Build.VERSION.RELEASE;
    }
    
    public static String manufacturer() {
        return Build.MANUFACTURER;
    }
    
    public static String timezoneOffset() {
        final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
        final String tz = new SimpleDateFormat("Z", Locale.US).format(c.getTime());
        return tz.substring(0, 3) + ':' + tz.substring(3, 5);
    }
    
    public static String countryCode() {
        return Locale.getDefault().getCountry();
    }
    
    public static String languageCode() {
        return Locale.getDefault().getLanguage();
    }
    
    public static String locale() {
        return Locale.getDefault().toString();
    }
}

