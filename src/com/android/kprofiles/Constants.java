package com.android.kprofiles;

import com.android.kprofiles.utils.FileUtils;

public final class Constants {
    public static final String INTENT_ACTION = "com.android.kprofiles.battery.KPROFILE_CHANGED";
    public static final String KPROFILES_AUTO_KEY = "kprofiles_auto";
    public static final String KPROFILES_AUTO_NODE = "/sys/module/kprofiles/parameters/auto_kp";
    public static final String KPROFILES_MODES_KEY = "kprofiles_modes";
    public static final String KPROFILES_MODES_NODE = "/sys/kernel/kprofiles/kp_mode";
    public static final String KPROFILES_MODES_INFO = "pref_kprofiles_modes_info";
    public static final String ON = "Y";
    public static final String OFF = "N";
    public static final boolean IS_SUPPORTED = FileUtils.fileExists(KPROFILES_MODES_NODE);
}
