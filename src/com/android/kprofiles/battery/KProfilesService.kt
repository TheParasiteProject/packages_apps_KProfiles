/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.kprofiles.battery

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.service.quicksettings.TileService
import com.android.kprofiles.R
import com.android.kprofiles.utils.getDefaultPrefs
import com.android.kprofiles.utils.getMode
import com.android.kprofiles.utils.isAutoEnabled
import com.android.kprofiles.utils.isAutoSupported
import com.android.kprofiles.utils.isModesSupported
import com.android.kprofiles.utils.writeToAutoNode
import com.android.kprofiles.utils.writeToModesNode

class KProfilesService :
    Service(),
    PowerSaveStateManager.PowerSaveStateListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    enum class State {
        MAIN_ENABLED,
        MAIN_DISABLED,
        POWERSAVE,
    }

    // Keys
    private val keyAuto by lazy { getString(R.string.pref_key_auto) }
    private val keyModes by lazy { getString(R.string.pref_key_modes) }

    // Modes
    private val modeNone by lazy { getString(R.string.kprofiles_modes_value_none) }
    private val modeBattery by lazy { getString(R.string.kprofiles_modes_value_battery) }

    // Tile
    private val tileComponent by lazy { ComponentName(this, KProfilesModesTileService::class.java) }

    private val prefs: SharedPreferences by lazy { getDefaultPrefs() }
    private val psm by lazy { PowerSaveStateManager.getInstance(this) }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        psm.registerListener(this)
        prefs.registerOnSharedPreferenceChangeListener(this)
        updateTileContent()

        val state = if (psm.isPowerSaveMode()) State.POWERSAVE else State.MAIN_ENABLED
        updateAutoNode(state)
        updateModesNode(state)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        psm.unregisterListener(this)
        updateTileContent()
        updateAutoNode(State.MAIN_DISABLED)
        updateModesNode(State.MAIN_DISABLED)
        super.onDestroy()
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        val state = if (psm.isPowerSaveMode()) State.POWERSAVE else State.MAIN_ENABLED

        when (key) {
            keyAuto -> {
                updateAutoNode(state)
            }
            keyModes -> {
                updateModesNode(state)
            }
        }
    }

    override fun onPowerSaveStateChanged(enabled: Boolean) {
        val state = if (enabled) State.POWERSAVE else State.MAIN_ENABLED
        updateAutoNode(state)
        updateModesNode(state)
    }

    private fun updateAutoNode(state: State) {
        if (!isAutoSupported()) return

        val savedAuto = prefs.isAutoEnabled(this)
        writeToAutoNode(
            when (state) {
                State.MAIN_ENABLED -> {
                    savedAuto
                }
                State.MAIN_DISABLED -> {
                    false
                }
                State.POWERSAVE -> {
                    false
                }
                else -> {
                    // Should not reach here
                    false
                }
            }
        )
    }

    private fun updateModesNode(state: State) {
        if (!isModesSupported()) return

        val savedMode = prefs.getMode(this)
        writeToModesNode(
            when (state) {
                State.MAIN_ENABLED -> {
                    savedMode
                }
                State.MAIN_DISABLED -> {
                    modeNone
                }
                State.POWERSAVE -> {
                    modeBattery
                }
                else -> {
                    // Should not reach here
                    modeNone
                }
            }
        )
        updateTileContent()
    }

    private fun updateTileContent() {
        TileService.requestListeningState(this, tileComponent)
    }
}
