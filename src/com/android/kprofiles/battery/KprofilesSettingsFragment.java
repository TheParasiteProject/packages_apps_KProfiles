/*
 * Copyright (C) 2022 CannedShroud
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

package com.android.kprofiles.battery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreferenceCompat;

import com.android.kprofiles.R;
import com.android.kprofiles.utils.Utils;
import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

public class KprofilesSettingsFragment extends SettingsBasePreferenceFragment
        implements OnPreferenceChangeListener {
    private MainSwitchPreference kProfilesEnabledPreference;
    private SwitchPreferenceCompat kProfilesAutoPreference;
    private ListPreference kProfilesModesPreference;
    private Preference kProfilesModesInfo;
    private boolean mSelfChange = false;

    private final BroadcastReceiver mServiceStateReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!getString(R.string.kprofiles_intent_action).equals(intent.getAction()))
                        return;
                    if (mSelfChange) {
                        mSelfChange = false;
                        return;
                    }
                    updateValues(false);
                }
            };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.kprofiles_settings, rootKey);

        kProfilesEnabledPreference =
                (MainSwitchPreference) findPreference(getString(R.string.pref_key_enabled));
        kProfilesEnabledPreference.setOnPreferenceChangeListener(this);

        kProfilesAutoPreference =
                (SwitchPreferenceCompat) findPreference(getString(R.string.pref_key_auto));
        if (Utils.isAutoSupported(requireContext())) {
            kProfilesAutoPreference.setOnPreferenceChangeListener(this);
        } else {
            kProfilesAutoPreference.setSummary(R.string.kprofiles_not_supported);
            kProfilesAutoPreference.setEnabled(false);
        }
        kProfilesModesPreference =
                (ListPreference) findPreference(getString(R.string.pref_key_modes));
        final boolean isModesSupported = Utils.isModesSupported(requireContext());
        if (isModesSupported) {
            kProfilesModesPreference.setOnPreferenceChangeListener(this);
        } else {
            kProfilesModesPreference.setSummary(R.string.kprofiles_not_supported);
            kProfilesModesPreference.setEnabled(false);
        }
        kProfilesModesInfo = (Preference) findPreference(getString(R.string.pref_key_modes_info));
        kProfilesModesInfo.setEnabled(isModesSupported);

        updateValues(true);

        Utils.registerReceiver(requireContext(), mServiceStateReceiver);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view =
                LayoutInflater.from(requireContext()).inflate(R.layout.kprofiles, container, false);
        ((ViewGroup) view).addView(super.onCreateView(inflater, container, savedInstanceState));
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (kProfilesAutoPreference == null
                || kProfilesModesPreference == null
                || kProfilesModesInfo == null) return;
        updateValues(false);
    }

    @Override
    public void onDestroy() {
        Utils.unregisterReceiver(requireContext(), mServiceStateReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        if (key.equals(getString(R.string.pref_key_enabled))) {
            updateValues(true);
            sendBroadcast();
        } else if (key.equals(getString(R.string.pref_key_auto))) {
            final boolean value = (Boolean) newValue;
            Utils.writeToAutoNode(requireContext(), value);
        } else if (key.equals(getString(R.string.pref_key_modes))) {
            final String value = (String) newValue;
            Utils.writeToModesNode(requireContext(), value);
            updateTitle(value);
            sendBroadcast();
        }
        return true;
    }

    private String modesDesc(String mode) {
        if (mode == null) mode = getString(R.string.kprofiles_modes_value_none);
        String descrpition = null;
        if (!Utils.isModesSupported(requireContext())) {
            return getString(R.string.kprofiles_not_supported);
        }

        if (mode.equals(getString(R.string.kprofiles_modes_value_none))) {
            descrpition = getString(R.string.kprofiles_modes_none_description);
        } else if (mode.equals(getString(R.string.kprofiles_modes_value_battery))) {
            descrpition = getString(R.string.kprofiles_modes_battery_description);
        } else if (mode.equals(getString(R.string.kprofiles_modes_value_balanced))) {
            descrpition = getString(R.string.kprofiles_modes_balanced_description);
        } else if (mode.equals(getString(R.string.kprofiles_modes_value_performance))) {
            descrpition = getString(R.string.kprofiles_modes_performance_description);
        } else {
            descrpition = getString(R.string.kprofiles_modes_none_description);
        }

        return descrpition;
    }

    private void updateTitle(String value) {
        Handler.getMain()
                .post(
                        () -> {
                            kProfilesModesInfo.setTitle(
                                    String.format(
                                            getString(R.string.kprofiles_modes_description),
                                            modesDesc(value)));
                        });
    }

    private void sendBroadcast() {
        mSelfChange = true;
        Utils.sendBroadcast(requireContext());
    }

    private void updateValues(boolean updateNodes) {
        final boolean isMainSwitchEnabled = Utils.isMainSwitchEnabled(requireContext());

        if (Utils.isAutoSupported(requireContext())) {
            final boolean enabled = Utils.isAutoEnabled(requireContext());
            kProfilesAutoPreference.setChecked(enabled);
            if (updateNodes) {
                Utils.writeToAutoNode(requireContext(), !isMainSwitchEnabled ? false : enabled);
            }
        }

        if (Utils.isModesSupported(requireContext())) {
            final String value = Utils.getMode(requireContext());
            kProfilesModesPreference.setValue(value);
            if (updateNodes) {
                Utils.writeToModesNode(
                        requireContext(),
                        !isMainSwitchEnabled
                                ? getString(R.string.kprofiles_modes_value_none)
                                : value);
            }
            updateTitle(value);
        }
    }
}
