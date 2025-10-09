/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.kprofiles.utils

import android.content.Context
import android.content.SharedPreferences
import com.android.kprofiles.R

const val PREFS_NAME = "kprofiles_preferences"

fun Context.getDefaultPrefs(): SharedPreferences {
    return createDeviceProtectedStorageContext()
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

fun Context.isAutoSupported(): Boolean {
    return FileUtils.fileExists(getString(R.string.kprofiles_auto_node))
}

fun Context.isModesSupported(): Boolean {
    return FileUtils.fileExists(getString(R.string.kprofiles_modes_node))
}

fun Context.readAutoNode(): String? {
    return FileUtils.readOneLine(getString(R.string.kprofiles_auto_node))
}

fun Context.writeToAutoNode(enabled: Boolean): Boolean {
    return FileUtils.writeLine(
        getString(R.string.kprofiles_auto_node),
        if (enabled) {
            getString(R.string.kprofiles_auto_on)
        } else {
            getString(R.string.kprofiles_auto_off)
        },
    )
}

fun Context.readModesNode(): String? {
    return FileUtils.readOneLine(getString(R.string.kprofiles_modes_node))
}

fun Context.writeToModesNode(mode: String): Boolean {
    return FileUtils.writeLine(getString(R.string.kprofiles_modes_node), mode)
}

fun SharedPreferences.isMainSwitchEnabled(context: Context): Boolean {
    return getBoolean(
        context.getString(R.string.pref_key_enabled),
        context.resources.getBoolean(R.bool.kprofiles_enabled_default),
    )
}

fun SharedPreferences.isAutoEnabled(context: Context): Boolean {
    return getBoolean(
        context.getString(R.string.pref_key_auto),
        context.resources.getBoolean(R.bool.kprofiles_auto_default),
    )
}

fun SharedPreferences.setMode(context: Context, mode: String) {
    edit().putString(context.getString(R.string.pref_key_modes), mode).apply()
}

fun SharedPreferences.getMode(context: Context): String {
    return getString(
        context.getString(R.string.pref_key_modes),
        context.getString(R.string.kprofiles_modes_default),
    ) ?: context.getString(R.string.kprofiles_modes_default)
}
