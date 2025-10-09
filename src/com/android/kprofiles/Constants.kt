/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.kprofiles

import android.util.Log

const val TAG: String = "KProfiles/Service"

const val ACTION_KPROFILE_CHANGED: String = "com.android.kprofiles.battery.KPROFILE_CHANGED"

fun dlog(tag: String, msg: String) {
    val DEBUG = Log.isLoggable(TAG, Log.DEBUG)
    if (DEBUG) Log.d(tag, msg)
}
