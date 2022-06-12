package me.firesun.dingtalk.enhancement.plugin;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public interface IPlugin {
    void hook(XC_LoadPackage.LoadPackageParam lpparam);
}
