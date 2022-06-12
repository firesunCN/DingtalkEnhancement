package me.firesun.dingtalk.enhancement;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import me.firesun.dingtalk.enhancement.plugin.AntiRevoke;
import me.firesun.dingtalk.enhancement.plugin.HideModule;
import me.firesun.dingtalk.enhancement.plugin.IPlugin;
import me.firesun.dingtalk.enhancement.plugin.MaskReadStatus;
import me.firesun.dingtalk.enhancement.util.HookParams;
import me.firesun.dingtalk.enhancement.util.SearchClasses;

import static de.robv.android.xposed.XposedBridge.log;


public class Main implements IXposedHookLoadPackage {

    private static final IPlugin[] plugins = {
            new MaskReadStatus(),
            new HideModule(),
            new AntiRevoke(),
    };

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) {
        if (lpparam.packageName.equals(HookParams.DINGTALK_PACKAGE_NAME)) {
            try {
                XposedHelpers.findAndHookMethod(ContextWrapper.class, "attachBaseContext", Context.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        Context context = (Context) param.args[0];
//                        String processName = lpparam.processName;

                        String versionName = getVersionName(context, HookParams.DINGTALK_PACKAGE_NAME);
                        log("Found dingtalk version:" + versionName);

                        if (!HookParams.hasInstance()) {
                            SearchClasses.init(context, lpparam, versionName);
                            loadPlugins(lpparam);
                            log("loadPlugins done");
                        }
                    }
                });
            } catch (Error | Exception e) {
            }
        }
    }

    private String getVersionName(Context context, String packageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packInfo = packageManager.getPackageInfo(packageName, 0);
            return packInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return "";
    }


    private void loadPlugins(LoadPackageParam lpparam) {
        for (IPlugin plugin : plugins) {
            try {
                plugin.hook(lpparam);
            } catch (Error | Exception e) {
                log(String.format("%s loaded error, %s", plugin, e));
            }
        }
    }

}
