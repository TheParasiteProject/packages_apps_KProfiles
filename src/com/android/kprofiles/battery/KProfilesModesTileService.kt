/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.kprofiles.battery

import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.android.kprofiles.R
import com.android.kprofiles.utils.getDefaultPrefs
import com.android.kprofiles.utils.getMode
import com.android.kprofiles.utils.isMainSwitchEnabled
import com.android.kprofiles.utils.isModesSupported
import com.android.kprofiles.utils.setMode
import com.android.kprofiles.utils.writeToModesNode

class KProfilesModesTileService :
    TileService(),
    SharedPreferences.OnSharedPreferenceChangeListener,
    PowerSaveStateManager.PowerSaveStateListener {

    private lateinit var prefs: SharedPreferences
    private lateinit var psm: PowerSaveStateManager

    // Keys
    private val keyEnabled by lazy { getString(R.string.pref_key_enabled) }
    private val keyModes by lazy { getString(R.string.pref_key_modes) }

    // Modes
    private val modeNone by lazy { getString(R.string.kprofiles_modes_value_none) }
    private val modeBattery by lazy { getString(R.string.kprofiles_modes_value_battery) }
    private val modeBalanced by lazy { getString(R.string.kprofiles_modes_value_balanced) }
    private val modePerformace by lazy { getString(R.string.kprofiles_modes_value_performance) }

    // Icons
    private val noneIcon: Icon by lazy { Icon.createWithResource(this, R.drawable.ic_kprofiles) }
    private val batteryIcon: Icon by lazy { Icon.createWithResource(this, R.drawable.ic_battery) }
    private val balancedIcon: Icon by lazy { Icon.createWithResource(this, R.drawable.ic_balanced) }
    private val performanceIcon: Icon by lazy {
        Icon.createWithResource(this, R.drawable.ic_performance)
    }

    override fun onCreate() {
        if (isModesSupported()) {
            super.onCreate()
            return
        }

        qsTile.apply {
            setState(Tile.STATE_UNAVAILABLE)
            updateTile()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onTileAdded() {
        super.onTileAdded()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
    }

    override fun onStartListening() {
        if (!isModesSupported()) return
        super.onStartListening()
        psm = PowerSaveStateManager.getInstance(this)
        prefs = getDefaultPrefs()
        prefs.registerOnSharedPreferenceChangeListener(this)
        psm.registerListener(this)
        updateTileContent()
    }

    override fun onStopListening() {
        psm.unregisterListener(this)
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        super.onStopListening()
    }

    override fun onClick() {
        if (!isModesSupported()) return
        if (!prefs.isMainSwitchEnabled(this) || psm.isPowerSaveMode()) {
            updateTileContent()
            super.onClick()
            return
        }

        val nextMode =
            when (prefs.getMode(this)) {
                modeNone -> modeBattery
                modeBattery -> modeBalanced
                modeBalanced -> modePerformace
                modePerformace -> modeNone
                else -> getString(R.string.kprofiles_modes_default)
            }

        prefs.setMode(this, nextMode)
        writeToModesNode(nextMode)
        updateTileContent(nextMode)
        super.onClick()
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        if (key == keyEnabled || key == keyModes) {
            updateTileContent()
        }
    }

    override fun onPowerSaveStateChanged(enabled: Boolean) {
        updateTileContent()
    }

    private fun updateTileContent(mode: String? = null) {
        val tile: Tile = getQsTile()

        if (psm.isPowerSaveMode()) {
            tile.setState(Tile.STATE_UNAVAILABLE)
            tile.setSubtitle(getString(R.string.kprofiles_battery_saver_on))
            tile.updateTile()
            return
        }

        if (!prefs.isMainSwitchEnabled(this)) {
            tile.setState(Tile.STATE_UNAVAILABLE)
            tile.updateTile()
            return
        }

        val currentMode = mode ?: prefs.getMode(this)

        tile.setState(
            if (currentMode != modeNone) {
                Tile.STATE_ACTIVE
            } else {
                Tile.STATE_INACTIVE
            }
        )

        when (currentMode) {
            modeNone -> {
                tile.setIcon(noneIcon)
                tile.setContentDescription(getString(R.string.kprofiles_modes_none))
                tile.setSubtitle(getString(R.string.kprofiles_modes_none))
            }
            modeBattery -> {
                tile.setIcon(batteryIcon)
                tile.setContentDescription(getString(R.string.kprofiles_modes_battery))
                tile.setSubtitle(getString(R.string.kprofiles_modes_battery))
            }
            modeBalanced -> {
                tile.setIcon(balancedIcon)
                tile.setContentDescription(getString(R.string.kprofiles_modes_balanced))
                tile.setSubtitle(getString(R.string.kprofiles_modes_balanced))
            }
            modePerformace -> {
                tile.setIcon(performanceIcon)
                tile.setContentDescription(getString(R.string.kprofiles_modes_performance))
                tile.setSubtitle(getString(R.string.kprofiles_modes_performance))
            }
        }

        tile.updateTile()
    }
}
