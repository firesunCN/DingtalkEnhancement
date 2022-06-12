package me.firesun.dingtalk.enhancement;


import de.robv.android.xposed.XSharedPreferences;

public class PreferencesUtils {

    private static XSharedPreferences instance = null;

    private static XSharedPreferences getInstance() {
        if (instance == null) {
            instance = new XSharedPreferences(PreferencesUtils.class.getPackage().getName());
            instance.makeWorldReadable();
        } else {
            instance.reload();
        }
        return instance;
    }

    public static boolean isMaskReadStatus() {
        return getInstance().getBoolean("is_mask_read_status", false);
    }

    public static boolean isAntiRevoke() {
        return getInstance().getBoolean("is_anti_revoke", false);
    }


}


