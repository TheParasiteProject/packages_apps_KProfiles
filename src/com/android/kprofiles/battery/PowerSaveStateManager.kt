/*
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.kprofiles.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager

internal class PowerSaveStateManager private constructor(private val context: Context) {

    interface PowerSaveStateListener {
        fun onPowerSaveStateChanged(enabled: Boolean)
    }

    private val listeners: HashSet<PowerSaveStateListener> = HashSet()
    private val listenersLock = Any()

    private var isReceiverRegistered = false
    private val powerManager: PowerManager = context.getSystemService(PowerManager::class.java)

    private val intentFilter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
    private val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                notifyStateChanges()
            }
        }

    fun onCreate() {
        if (!isReceiverRegistered) {
            context.registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED)
            isReceiverRegistered = true
            notifyStateChanges()
        }
    }

    fun onDestroy() {
        listeners.clear()
        if (isReceiverRegistered) {
            context.unregisterReceiver(receiver)
            isReceiverRegistered = false
        }
    }

    fun notifyStateChanges() {
        val isPowerSave = powerManager.isPowerSaveMode()
        val listenersCopy: HashSet<PowerSaveStateListener>
        synchronized(listenersLock) { listenersCopy = HashSet(listeners) }
        listenersCopy.forEach { listener -> listener.onPowerSaveStateChanged(isPowerSave) }
    }

    fun isPowerSaveMode(): Boolean = powerManager.isPowerSaveMode()

    fun registerListener(listener: PowerSaveStateListener) {
        synchronized(listenersLock) { listeners.add(listener) }
        val isPowerSave = powerManager.isPowerSaveMode()
        listener.onPowerSaveStateChanged(isPowerSave)
    }

    fun unregisterListener(listener: PowerSaveStateListener) {
        synchronized(listenersLock) { listeners.remove(listener) }
    }

    companion object {
        @Volatile private var instance: PowerSaveStateManager? = null

        fun getInstance(context: Context) =
            instance
                ?: synchronized(this) {
                    instance
                        ?: PowerSaveStateManager(context.applicationContext).also {
                            it.onCreate()
                            instance = it
                        }
                }
    }
}
