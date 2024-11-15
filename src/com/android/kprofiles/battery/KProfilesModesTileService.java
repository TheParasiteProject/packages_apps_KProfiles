package com.android.kprofiles.battery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.preference.PreferenceManager;

import com.android.kprofiles.R;
import com.android.kprofiles.utils.Utils;

public class KProfilesModesTileService extends TileService {

    private Context mContext;
    private boolean mSelfChange = false;

    @Override
    public void onCreate() {
        mContext = getApplicationContext();
        if (Utils.isModesSupported(mContext)) {
            super.onCreate();
            return;
        }
        Tile tile = getQsTile();
        tile.setState(Tile.STATE_UNAVAILABLE);
        tile.updateTile();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
    }

    @Override
    public void onStartListening() {
        if (!Utils.isModesSupported(mContext)) return;
        super.onStartListening();

        Utils.registerReceiver(mContext, mServiceStateReceiver);

        updateTileContent();
    }

    @Override
    public void onStopListening() {
        Utils.unregisterReceiver(mContext, mServiceStateReceiver);
        super.onStopListening();
    }

    @Override
    public void onClick() {
        if (!Utils.isModesSupported(mContext)) return;
        if (!Utils.isMainSwitchEnabled(mContext)) {
            updateTileContent(null);
            super.onClick();
            return;
        }

        String mode = getMode();
        if (mode.equals(getString(R.string.kprofiles_modes_none))) {
            mode = getString(R.string.kprofiles_modes_battery);
        } else if (mode.equals(getString(R.string.kprofiles_modes_battery))) {
            mode = getString(R.string.kprofiles_modes_balanced);
        } else if (mode.equals(getString(R.string.kprofiles_modes_balanced))) {
            mode = getString(R.string.kprofiles_modes_performance);
        } else if (mode.equals(getString(R.string.kprofiles_modes_performance))) {
            mode = getString(R.string.kprofiles_modes_none);
        }
        setMode(mode);
        updateTileContent(mode);
        super.onClick();
    }

    private void setMode(String mode) {
        Utils.writeToModesNode(mContext, mode);
        mSelfChange = true;
        Utils.sendBroadcast(mContext);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        prefs.edit().putString(getString(R.string.pref_key_modes), mode).apply();
    }

    private String getMode() {
        final String value = Utils.readModesNode(mContext);
        return value != null ? value : getString(R.string.kprofiles_modes_none);
    }

    private void updateTileContent() {
        updateTileContent(null);
    }

    private void updateTileContent(String mode) {
        Tile tile = getQsTile();
        if (mode == null) mode = getMode();

        if (!Utils.isMainSwitchEnabled(mContext)) {
            tile.setState(Tile.STATE_UNAVAILABLE);
        } else {
            tile.setState(
                    !mode.equals(getString(R.string.kprofiles_modes_none))
                            ? Tile.STATE_ACTIVE
                            : Tile.STATE_INACTIVE);
        }

        if (mode.equals(getString(R.string.kprofiles_modes_none))) {
            tile.setContentDescription(getString(R.string.kprofiles_modes_none));
            tile.setSubtitle(getString(R.string.kprofiles_modes_none));
        } else if (mode.equals(getString(R.string.kprofiles_modes_battery))) {
            tile.setContentDescription(getString(R.string.kprofiles_modes_battery));
            tile.setSubtitle(getString(R.string.kprofiles_modes_battery));
        } else if (mode.equals(getString(R.string.kprofiles_modes_balanced))) {
            tile.setContentDescription(getString(R.string.kprofiles_modes_balanced));
            tile.setSubtitle(getString(R.string.kprofiles_modes_balanced));
        } else if (mode.equals(getString(R.string.kprofiles_modes_performance))) {
            tile.setContentDescription(getString(R.string.kprofiles_modes_performance));
            tile.setSubtitle(getString(R.string.kprofiles_modes_performance));
        }

        tile.updateTile();
    }

    private final BroadcastReceiver mServiceStateReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!getResources()
                            .getString(R.string.kprofiles_intent_action)
                            .equals(intent.getAction())) return;
                    if (mSelfChange) {
                        mSelfChange = false;
                        return;
                    }
                    updateTileContent();
                }
            };
}
