/*
 * SPDX-FileCopyrightText: CannedShroud
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.kprofiles.battery

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.SwitchPreferenceCompat
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
    SettingsBasePreferenceFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener,
    PowerSaveStateManager.PowerSaveStateListener {

    private val prefs by lazy { _context.getDefaultPrefs() }
    private val psm by lazy { PowerSaveStateManager.getInstance(_context) }

    private val _context by lazy { requireContext() }

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

    private val SERVICE_CONTROL_DELAY_MS = 500L
    private val mainHandler: Handler = Handler(Looper.getMainLooper())

    private val controlServiceRunnable = Runnable {
        val enabled = prefs.isMainSwitchEnabled(_context)
        prefEnabled?.apply { setChecked(enabled) }
        val serviceIntent = Intent(_context, KProfilesService::class.java)

        if (enabled) {
            _context.startServiceAsUser(serviceIntent, UserHandle.CURRENT)
        } else {
            _context.stopServiceAsUser(serviceIntent, UserHandle.CURRENT)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.kprofiles_settings, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferenceManager.setStorageDeviceProtected()
        preferenceManager.sharedPreferencesName = PREFS_NAME

        psm.registerListener(this)
        prefs.registerOnSharedPreferenceChangeListener(this)

        updateEnabled()
        updateAutoEnabled()
        updateModes()
    }

    override fun onResume() {
        super.onResume()
        updateEnabled()
        updateAutoEnabled()
        updateModes()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        psm.unregisterListener(this)
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        mainHandler.removeCallbacks(controlServiceRunnable)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        when (key) {
            keyEnabled -> {
                updateEnabled()
                updateAutoEnabled()
                updateModes()
            }
            keyAuto -> updateAutoEnabled()
            keyModes -> updateModes()
        }
    }

    override fun onPowerSaveStateChanged(enabled: Boolean) {
        updateModes()
        updateAutoEnabled()
    }

    private fun updateEnabled() {
        mainHandler.removeCallbacks(controlServiceRunnable)
        mainHandler.postDelayed(controlServiceRunnable, SERVICE_CONTROL_DELAY_MS)
    }

    private fun updateAutoEnabled() {
        if (!_context.isAutoSupported()) {
            prefAuto?.let { preferenceScreen?.removePreference(it) }
            return
        }

        if (psm.isPowerSaveMode()) {
            prefAuto?.setEnabled(false)
            return
        }

        val isMainSwitchEnabled = prefs.isMainSwitchEnabled(_context)
        prefAuto?.setEnabled(isMainSwitchEnabled)

        val enabled = prefs.isAutoEnabled(_context)
        prefAuto?.setChecked(enabled)
    }

    private fun updateModes() {
        if (!_context.isModesSupported()) {
            prefModes?.let { preferenceScreen?.removePreference(it) }
            return
        }

        if (psm.isPowerSaveMode()) {
            prefModes?.setEnabled(false)
            return
        }

        val isMainSwitchEnabled = prefs.isMainSwitchEnabled(_context)
        prefModes?.setEnabled(isMainSwitchEnabled)

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

        if (psm.isPowerSaveMode()) {
            mainHandler.post {
                prefModesInfo?.setTitle(
                    String.format(
                        getString(R.string.kprofiles_modes_description),
                        getString(R.string.kprofiles_battery_saver_on),
                    )
                )
            }
            prefModesInfo?.setEnabled(false)
            return
        }

        val isMainSwitchEnabled = prefs.isMainSwitchEnabled(_context)
        prefModesInfo?.setEnabled(isMainSwitchEnabled)

        mainHandler.post {
            prefModesInfo?.setTitle(
                String.format(getString(R.string.kprofiles_modes_description), modesDesc(value))
            )
        }
    }
}
