/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.kprofiles.battery

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import com.android.kprofiles.R
import com.android.kprofiles.utils.getDefaultPrefs
import com.android.kprofiles.utils.getMode
import com.android.kprofiles.utils.isAutoEnabled
import com.android.kprofiles.utils.isAutoSupported
import com.android.kprofiles.utils.isMainSwitchEnabled
import com.android.kprofiles.utils.isModesSupported
import com.android.kprofiles.utils.writeToAutoNode
import com.android.kprofiles.utils.writeToModesNode

class KProfilesService : Service(), PowerSaveStateManager.PowerSaveStateListener {

    // Modes
    private val modeNone by lazy { getString(R.string.kprofiles_modes_value_none) }
    private val modeBattery by lazy { getString(R.string.kprofiles_modes_value_battery) }

    private val prefs: SharedPreferences by lazy { applicationContext.getDefaultPrefs() }
    private val psm by lazy { PowerSaveStateManager.getInstance(applicationContext) }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        psm.registerListener(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        psm.unregisterListener(this)
    }

    override fun onPowerSaveStateChanged(enabled: Boolean) {
        if (!prefs.isMainSwitchEnabled(applicationContext)) {
            return
        }
        if (applicationContext.isAutoSupported()) {
            // Enforce disabled always when battery saver mode enabled
            val savedAuto = prefs.isAutoEnabled(applicationContext)
            writeToAutoNode(
                if (enabled) {
                    false
                } else {
                    savedAuto
                }
            )
        }
        if (applicationContext.isModesSupported()) {
            val savedMode = prefs.getMode(applicationContext)
            writeToModesNode(
                if (enabled) {
                    modeBattery
                } else {
                    savedMode
                }
            )
        }
    }
}
