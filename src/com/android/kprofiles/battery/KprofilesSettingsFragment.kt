/*
 * SPDX-FileCopyrightText: CannedShroud
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.kprofiles.battery

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.UserHandle
import android.service.quicksettings.TileService
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.SwitchPreferenceCompat
import com.android.kprofiles.ACTION_KPROFILE_CHANGED
import com.android.kprofiles.R
import com.android.kprofiles.utils.PREFS_NAME
import com.android.kprofiles.utils.getDefaultPrefs
import com.android.kprofiles.utils.getMode
import com.android.kprofiles.utils.isAutoEnabled
import com.android.kprofiles.utils.isAutoSupported
import com.android.kprofiles.utils.isMainSwitchEnabled
import com.android.kprofiles.utils.isModesSupported
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.MainSwitchPreference
import com.android.settingslib.widget.SettingsBasePreferenceFragment

class KprofilesSettingsFragment :
    SettingsBasePreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val prefs by lazy { _context.getDefaultPrefs() }

    private val _context by lazy { requireContext() }
    private val powerManager: PowerManager by lazy {
        _context.getSystemService(PowerManager::class.java)
    }
    private var selfChange: Boolean = false
    private val intentFilter =
        IntentFilter().also {
            it.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            it.addAction(ACTION_KPROFILE_CHANGED)
        }
    private val localIntent =
        Intent().also {
            it.setAction(ACTION_KPROFILE_CHANGED)
            it.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY)
        }
    private val serviceIntent by lazy { Intent(_context, KProfilesService::class.java) }
    private val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (context == null || intent == null) {
                    return
                }

                when (intent.action) {
                    ACTION_KPROFILE_CHANGED -> {
                        if (selfChange) {
                            selfChange = false
                            return
                        }
                        updateModes()
                    }
                    PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                        onPowerSaveStateChanged(powerManager.isPowerSaveMode())
                    }
                }
            }
        }

    // Keys
    private val keyEnabled by lazy { _context.getString(R.string.pref_key_enabled) }
    private val keyAuto by lazy { _context.getString(R.string.pref_key_auto) }
    private val keyModes by lazy { _context.getString(R.string.pref_key_modes) }
    private val keyModesInfo by lazy { _context.getString(R.string.pref_key_modes_info) }

    // Prefs
    private val prefEnabled by lazy { findPreference<MainSwitchPreference>(keyEnabled) }
    private val prefAuto by lazy { findPreference<SwitchPreferenceCompat>(keyAuto) }
    private val prefModes by lazy { findPreference<ListPreference>(keyModes) }
    private val prefModesInfo by lazy { findPreference<FooterPreference>(keyModesInfo) }

    // Modes
    private val modeNone by lazy { _context.getString(R.string.kprofiles_modes_value_none) }
    private val modeBattery by lazy { _context.getString(R.string.kprofiles_modes_value_battery) }
    private val modeBalanced by lazy { _context.getString(R.string.kprofiles_modes_value_balanced) }
    private val modePerformace by lazy {
        _context.getString(R.string.kprofiles_modes_value_performance)
    }

    // Tile
    private val tileComponent by lazy {
        ComponentName(_context, KProfilesModesTileService::class.java)
    }

    private val SERVICE_CONTROL_DELAY_MS = 300L
    private val mainHandler: Handler = Handler(Looper.getMainLooper())

    private val controlServiceRunnable = Runnable {
        if (prefs.isMainSwitchEnabled(_context)) {
            _context.startServiceAsUser(serviceIntent, UserHandle.CURRENT)
        } else {
            _context.stopServiceAsUser(serviceIntent, UserHandle.CURRENT)
        }

        selfChange = true
        _context.sendBroadcastAsUser(localIntent, UserHandle.CURRENT)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.kprofiles_settings, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferenceManager.setStorageDeviceProtected()
        preferenceManager.sharedPreferencesName = PREFS_NAME

        prefs.registerOnSharedPreferenceChangeListener(this)
        _context.registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED)

        updateEnabled()
        updateAutoEnabled()
        updateModes()
        updateTileContent()
    }

    override fun onResume() {
        super.onResume()
        updateEnabled()
        updateAutoEnabled()
        updateModes()
        updateTileContent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _context.unregisterReceiver(receiver)
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        mainHandler.removeCallbacks(controlServiceRunnable)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        when (key) {
            keyEnabled -> {
                updateEnabled()
                updateTileContent()
            }
            keyAuto -> {
                updateAutoEnabled()
                selfChange = true
                _context.sendBroadcastAsUser(localIntent, UserHandle.CURRENT)
            }
            keyModes -> {
                updateModes()
                selfChange = true
                _context.sendBroadcastAsUser(localIntent, UserHandle.CURRENT)
                updateTileContent()
            }
        }
    }

    fun onPowerSaveStateChanged(enabled: Boolean) {
        updateModes()
        updateAutoEnabled()
    }

    private fun updateEnabled() {
        val enabled = prefs.isMainSwitchEnabled(_context)
        prefEnabled?.apply { setChecked(enabled) }

        updateAutoEnabled()
        updateModes()

        mainHandler.removeCallbacks(controlServiceRunnable)
        mainHandler.postDelayed(controlServiceRunnable, SERVICE_CONTROL_DELAY_MS)
    }

    private fun updateAutoEnabled() {
        if (!_context.isAutoSupported()) {
            prefAuto?.let { preferenceScreen?.removePreference(it) }
            return
        }

        val isMainSwitchEnabled = prefs.isMainSwitchEnabled(_context)
        prefAuto?.setEnabled(isMainSwitchEnabled && !powerManager.isPowerSaveMode())

        val enabled = prefs.isAutoEnabled(_context)
        prefAuto?.setChecked(enabled)
    }

    private fun updateModes() {
        if (!_context.isModesSupported()) {
            prefModes?.let { preferenceScreen?.removePreference(it) }
            return
        }

        val isMainSwitchEnabled = prefs.isMainSwitchEnabled(_context)
        prefModes?.setEnabled(isMainSwitchEnabled && !powerManager.isPowerSaveMode())

        val value = prefs.getMode(_context)
        prefModes?.setValue(value)
        updateModesInfo(value)
    }

    private fun modesDesc(mode: String?): String {
        if (!_context.isModesSupported()) {
            return _context.getString(R.string.kprofiles_not_supported)
        }

        val currentMode = mode ?: modeNone

        return when (currentMode) {
            modeNone -> {
                _context.getString(R.string.kprofiles_modes_none_description)
            }
            modeBattery -> {
                _context.getString(R.string.kprofiles_modes_battery_description)
            }
            modeBalanced -> {
                _context.getString(R.string.kprofiles_modes_balanced_description)
            }
            modePerformace -> {
                _context.getString(R.string.kprofiles_modes_performance_description)
            }
            else -> {
                _context.getString(R.string.kprofiles_modes_none_description)
            }
        }
    }

    private fun updateModesInfo(value: String?) {
        if (!_context.isModesSupported()) {
            prefModesInfo?.let { preferenceScreen?.removePreference(it) }
            return
        }

        val isMainSwitchEnabled = prefs.isMainSwitchEnabled(_context)
        val powerSave = powerManager.isPowerSaveMode()
        prefModesInfo?.setEnabled(isMainSwitchEnabled && !powerSave)

        prefModesInfo?.setTitle(
            if (powerSave) {
                String.format(
                    getString(R.string.kprofiles_modes_description),
                    getString(R.string.kprofiles_battery_saver_on),
                )
            } else {
                String.format(getString(R.string.kprofiles_modes_description), modesDesc(value))
            }
        )
    }

    private fun updateTileContent() {
        TileService.requestListeningState(_context, tileComponent)
    }
}
