/*
 * SPDX-FileCopyrightText: The CyanogenMod Project
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
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
