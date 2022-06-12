package me.firesun.dingtalk.enhancement.plugin;

import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import me.firesun.dingtalk.enhancement.PreferencesUtils;
import me.firesun.dingtalk.enhancement.util.HookParams;


public class MaskReadStatus implements IPlugin {
    @Override
    public void hook(XC_LoadPackage.LoadPackageParam lpparam) {

        HookParams hp = HookParams.getInstance();
        Class callbackClass = XposedHelpers.findClass(hp.WukongCallbackClassName, lpparam.classLoader);

        XposedHelpers.findAndHookMethod(hp.UpdateToReadClassName, lpparam.classLoader,hp.UpdateToReadMethod, Map.class, List.class,  List.class,callbackClass, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    if (!PreferencesUtils.isMaskReadStatus()) {
                        return;
                    }
                    param.setResult(null);
                } catch (Error | Exception e) {
                }
            }
        });

        XposedHelpers.findAndHookMethod(hp.UpdateToReadClassName, lpparam.classLoader, hp.UpdateToViewMethod, String.class, long.class, String.class, callbackClass, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    if (!PreferencesUtils.isMaskReadStatus()) {
                        return;
                    }
                    param.setResult(null);
                } catch (Error | Exception e) {
                }
            }
        });
    }
}
