package com.android.kprofiles.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.UserHandle;

import androidx.preference.PreferenceManager;

import com.android.kprofiles.R;

public final class Utils {
    public static boolean isAutoSupported(Context ctx) {
        return FileUtils.fileExists(ctx.getString(R.string.kprofiles_auto_node));
    }

    public static boolean isModesSupported(Context ctx) {
        return FileUtils.fileExists(ctx.getString(R.string.kprofiles_modes_node));
    }

    public static String readAutoNode(Context ctx) {
        return FileUtils.readOneLine(ctx.getString(R.string.kprofiles_auto_node));
    }

    public static boolean writeToAutoNode(Context ctx, boolean enabled) {
        return FileUtils.writeLine(
                ctx.getString(R.string.kprofiles_auto_node),
                enabled
                        ? ctx.getString(R.string.kprofiles_auto_on)
                        : ctx.getString(R.string.kprofiles_auto_off));
    }

    public static String readModesNode(Context ctx) {
        return FileUtils.readOneLine(ctx.getString(R.string.kprofiles_modes_node));
    }

    public static boolean writeToModesNode(Context ctx, String modes) {
        return FileUtils.writeLine(ctx.getString(R.string.kprofiles_modes_node), modes);
    }

    public static boolean isMainSwitchEnabled(Context ctx) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(ctx.getString(R.string.pref_key_enabled), false);
    }

    public static void registerReceiver(Context ctx, BroadcastReceiver mServiceStateReceiver) {
        // Registering observers
        IntentFilter filter = new IntentFilter();
        filter.addAction(ctx.getString(R.string.kprofiles_intent_action));
        ctx.registerReceiver(mServiceStateReceiver, filter, Context.RECEIVER_EXPORTED);
    }

    public static void unregisterReceiver(Context ctx, BroadcastReceiver mServiceStateReceiver) {
        ctx.unregisterReceiver(mServiceStateReceiver);
    }

    public static void sendBroadcast(Context ctx) {
        Intent intent = new Intent(ctx.getString(R.string.kprofiles_intent_action));
        intent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        ctx.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }
}
