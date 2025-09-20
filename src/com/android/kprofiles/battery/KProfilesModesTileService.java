package com.android.kprofiles.battery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.android.kprofiles.R;
import com.android.kprofiles.utils.Utils;

public class KProfilesModesTileService extends TileService {

    private Context mContext;
    private boolean mSelfChange = false;

    private Icon mNoneIcon;
    private Icon mBatteryIcon;
    private Icon mBalancedIcon;
    private Icon mPerformanceIcon;

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
                    updateTileContent();
                }
            };

    @Override
    public void onCreate() {
        mContext = getApplicationContext();
        if (Utils.isModesSupported(mContext)) {
            super.onCreate();
            mNoneIcon = Icon.createWithResource(mContext, R.drawable.ic_kprofiles);
            mBatteryIcon = Icon.createWithResource(mContext, R.drawable.ic_battery);
            mBalancedIcon = Icon.createWithResource(mContext, R.drawable.ic_balanced);
            mPerformanceIcon = Icon.createWithResource(mContext, R.drawable.ic_performance);
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
            updateTileContent();
            super.onClick();
            return;
        }

        String mode = Utils.getMode(mContext);
        if (mode.equals(getString(R.string.kprofiles_modes_value_none))) {
            mode = getString(R.string.kprofiles_modes_value_battery);
        } else if (mode.equals(getString(R.string.kprofiles_modes_value_battery))) {
            mode = getString(R.string.kprofiles_modes_value_balanced);
        } else if (mode.equals(getString(R.string.kprofiles_modes_value_balanced))) {
            mode = getString(R.string.kprofiles_modes_value_performance);
        } else if (mode.equals(getString(R.string.kprofiles_modes_value_performance))) {
            mode = getString(R.string.kprofiles_modes_value_none);
        }
        setMode(mode);
        updateTileContent(mode);
        super.onClick();
    }

    private void setMode(String mode) {
        if (!Utils.isMainSwitchEnabled(mContext)) return;
        Utils.writeToModesNode(mContext, mode);
        Utils.setMode(mContext, mode);
        sendBroadcast();
    }

    private void sendBroadcast() {
        mSelfChange = true;
        Utils.sendBroadcast(mContext);
    }

    private void updateTileContent() {
        updateTileContent(null);
    }

    private void updateTileContent(String mode) {
        Tile tile = getQsTile();

        if (!Utils.isMainSwitchEnabled(mContext)) {
            tile.setState(Tile.STATE_UNAVAILABLE);
            tile.updateTile();
            return;
        }

        if (mode == null) mode = Utils.getMode(mContext);

        tile.setState(
                !mode.equals(getString(R.string.kprofiles_modes_value_none))
                        ? Tile.STATE_ACTIVE
                        : Tile.STATE_INACTIVE);

        if (mode.equals(getString(R.string.kprofiles_modes_value_none))) {
            tile.setIcon(mNoneIcon);
            tile.setContentDescription(getString(R.string.kprofiles_modes_none));
            tile.setSubtitle(getString(R.string.kprofiles_modes_none));
        } else if (mode.equals(getString(R.string.kprofiles_modes_value_battery))) {
            tile.setIcon(mBatteryIcon);
            tile.setContentDescription(getString(R.string.kprofiles_modes_battery));
            tile.setSubtitle(getString(R.string.kprofiles_modes_battery));
        } else if (mode.equals(getString(R.string.kprofiles_modes_value_balanced))) {
            tile.setIcon(mBalancedIcon);
            tile.setContentDescription(getString(R.string.kprofiles_modes_balanced));
            tile.setSubtitle(getString(R.string.kprofiles_modes_balanced));
        } else if (mode.equals(getString(R.string.kprofiles_modes_value_performance))) {
            tile.setIcon(mPerformanceIcon);
            tile.setContentDescription(getString(R.string.kprofiles_modes_performance));
            tile.setSubtitle(getString(R.string.kprofiles_modes_performance));
        }

        tile.updateTile();
    }
}
