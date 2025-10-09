/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.kprofiles.battery

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.os.PowerManager
import android.os.UserHandle
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.android.kprofiles.ACTION_KPROFILE_CHANGED
import com.android.kprofiles.R
import com.android.kprofiles.utils.getDefaultPrefs
import com.android.kprofiles.utils.getMode
import com.android.kprofiles.utils.isMainSwitchEnabled
import com.android.kprofiles.utils.isModesSupported
import com.android.kprofiles.utils.setMode

class KProfilesModesTileService : TileService() {

    private lateinit var prefs: SharedPreferences
    private lateinit var powerManager: PowerManager
    private val localIntent =
        Intent().also {
            it.setAction(ACTION_KPROFILE_CHANGED)
            it.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY)
        }

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
        powerManager = getSystemService(PowerManager::class.java)
        prefs = getDefaultPrefs()

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
        updateTileContent()
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    override fun onClick() {
        if (!isModesSupported()) return
        if (!prefs.isMainSwitchEnabled(this) || powerManager.isPowerSaveMode()) {
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
        sendBroadcastAsUser(localIntent, UserHandle.CURRENT)
        updateTileContent(nextMode)
        super.onClick()
    }

    private fun updateTileContent(mode: String? = null) {
        val tile: Tile = getQsTile()
        val powerSave = powerManager.isPowerSaveMode()

        if (powerSave || !prefs.isMainSwitchEnabled(this)) {
            tile.setState(Tile.STATE_UNAVAILABLE)
            tile.setSubtitle(
                if (powerSave) {
                    getString(R.string.kprofiles_battery_saver_on)
                } else {
                    null
                }
            )
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
