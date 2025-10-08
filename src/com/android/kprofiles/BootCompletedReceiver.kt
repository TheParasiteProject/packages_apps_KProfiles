/*
 * SPDX-FileCopyrightText: The CyanogenMod Project
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.kprofiles

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import com.android.kprofiles.battery.KProfilesService
import com.android.kprofiles.utils.getDefaultPrefs
import com.android.kprofiles.utils.isAutoSupported
import com.android.kprofiles.utils.isMainSwitchEnabled
import com.android.kprofiles.utils.isModesSupported
import com.android.kprofiles.utils.writeToAutoNode
import com.android.kprofiles.utils.writeToModesNode

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            return
        }

        dlog(TAG, "Received boot completed intent")

        val prefs = context.getDefaultPrefs()

        val autoSupported = context.isAutoSupported()
        val modeSupported = context.isModesSupported()

        if (!prefs.isMainSwitchEnabled(context)) {
            if (autoSupported) {
                context.writeToAutoNode(false)
            }
            if (modeSupported) {
                context.writeToModesNode(context.getString(R.string.kprofiles_modes_none))
            }
            return
        }

        context.startServiceAsUser(
            Intent(context, KProfilesService::class.java),
            UserHandle.CURRENT,
        )
    }
}
