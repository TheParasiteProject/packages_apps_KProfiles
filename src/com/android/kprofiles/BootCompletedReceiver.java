/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2019 The LineageOS Project
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

package com.android.kprofiles;

import static com.android.kprofiles.Constants.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.kprofiles.utils.Utils;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final boolean DEBUG = false;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (DEBUG) Log.d(TAG, "Received boot completed intent");

        if (!Utils.isMainSwitchEnabled(context)) {
            Utils.writeToAutoNode(context, false);
            Utils.writeToModesNode(context, context.getString(R.string.kprofiles_modes_none));
            return;
        }

        if (Utils.isAutoSupported(context)) {
            Utils.writeToAutoNode(context, Utils.isAutoEnabled(context));
        }
        if (Utils.isModesSupported(context)) {
            Utils.writeToModesNode(context, Utils.getMode(context));
        }
    }
}
