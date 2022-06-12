package me.firesun.dingtalk.enhancement.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.DexClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import me.firesun.dingtalk.enhancement.Main;

import static me.firesun.dingtalk.enhancement.util.ReflectionUtil.findClassIfExists;
import static me.firesun.dingtalk.enhancement.util.ReflectionUtil.log;

public class SearchClasses {
    private static final List<String> apkClasses = new ArrayList<>();
    private static XSharedPreferences preferencesInstance = null;

    public static void init(Context context, XC_LoadPackage.LoadPackageParam lparam, String versionName) {

        if (loadConfig(lparam, versionName))
            return;

        log("failed to load config, start finding...");

        generateConfig(lparam.appInfo.sourceDir, lparam.classLoader, versionName);
        saveConfig(context);
    }

    public static void generateConfig(String dingtalkApk, ClassLoader classLoader, String versionName) {

        HookParams hp = HookParams.getInstance();
        hp.versionName = versionName;
        hp.versionCode = HookParams.VERSION_CODE;
        int versionNum = getVersionNum(versionName);

        ApkFile apkFile = null;
        try {
            apkFile = new ApkFile(dingtalkApk);
            DexClass[] dexClasses = apkFile.getDexClasses();
            apkClasses.clear();

            for (DexClass dexClass : dexClasses) {
                apkClasses.add(ReflectionUtil.getClassName(dexClass));
            }
        } catch (Error | Exception e) {
            log("Open ApkFile Failed!");
        } finally {
            try {
                apkFile.close();
            } catch (Exception e) {
                log("Close ApkFile Failed!");
            }
        }

        log("start search");

        //MaskReadStatus
        try {
            Class messageImplClass = findClassIfExists(hp.WukongMessageImplClassName, classLoader);
            Class callbackClass = findClassIfExists(hp.WukongCallbackClassName, classLoader);

            Class updateToReadClass = ReflectionUtil.findClassesFromPackage(classLoader, apkClasses, "", 0)
                    .filterByMethod(void.class, messageImplClass, callbackClass)
                    .filterByMethod(void.class, long.class, callbackClass)
                    .filterByMethod(void.class, String.class, messageImplClass, boolean.class, int.class, callbackClass)
                    .filterByMethod(void.class, String.class, long.class, callbackClass)
                    .firstOrNull();
            hp.UpdateToReadClassName = updateToReadClass.getName();
            hp.UpdateToReadMethod = ReflectionUtil.findMethodsByExactParameters(updateToReadClass,
                    void.class, Map.class, List.class, List.class, callbackClass)
                    .getName();
            hp.UpdateToViewMethod = ReflectionUtil.findMethodsByExactParameters(updateToReadClass,
                    void.class, String.class, long.class, String.class, callbackClass)
                    .getName();
        } catch (Error | Exception e) {
            log("Search MaskReadStatus Classes Failed!");
        }

        //AntiRevoke
        try {
            Class messageImplClass = findClassIfExists(hp.WukongMessageImplClassName, classLoader);

            Class mDatabaseClass = findClassIfExists(hp.IMDatabaseClassName, classLoader);
            Class messageDsClass = ReflectionUtil.findClassesFromPackage(classLoader, apkClasses, "", 0)
                    .filterBySuperClass(mDatabaseClass)
                    .filterByMethod(long.class, String.class, messageImplClass)
                    .filterByMethod(int.class, String.class, List.class)
                    .filterByMethod(Map.class, String.class, Collection.class, boolean.class)
                    .firstOrNull();
            hp.MessageDsClassName = messageDsClass.getName();

            Class messageDsProxyClass = ReflectionUtil.findClassesFromPackage(classLoader, apkClasses, "", 0)
                    .filterBySuperClass(messageDsClass)
                    .filterByMethod(long.class, String.class, messageImplClass)
                    .filterByMethod(int.class, String.class, long.class)
                    .filterByMethod(Map.class, String.class, Collection.class, boolean.class)
                    .firstOrNull();
            hp.MessageDsProxyClassName = messageDsProxyClass.getName();

            hp.GetSingletionMethod = ReflectionUtil.findMethodsByExactParameters(messageDsProxyClass,
                    messageDsProxyClass)
                    .getName();

            hp.MessageRecallMethod = ReflectionUtil.findMethodsByExactParameters(messageDsClass,
                    int.class, String.class, List.class, ContentValues.class)
                    .getName();
            hp.MessageHandlerMethod = ReflectionUtil.findMethodsByExactParameters(messageDsClass,
                    Map.class, String.class, Collection.class, boolean.class)
                    .getName();
            hp.MessageUpdateMethod = ReflectionUtil.findMethodsByExactParameters(messageDsClass,
                    int.class, String.class, String.class, List.class)
                    .getName();
        } catch (Error | Exception e) {
            log("Search AntiRevoke Classes Failed!");
        }
    }

    public static int getVersionNum(String version) {
        String[] v = version.split("\\.");
        if (v.length == 3)
            return Integer.valueOf(v[0]) * 100 * 100 + Integer.valueOf(v[1]) * 100 + Integer.valueOf(v[2]);
        else
            return 0;
    }

    private static boolean loadConfig(XC_LoadPackage.LoadPackageParam lpparam, String curVersionName) {
        try {
            SharedPreferences pref = getPreferencesInstance();
            HookParams hp = new Gson().fromJson(pref.getString("params", ""), HookParams.class);

            if (hp == null
                    || !hp.versionName.equals(curVersionName)
                    || hp.versionCode != HookParams.VERSION_CODE) {
                return false;
            }

            HookParams.setInstance(hp);
            log("load config successful");
            return true;
        } catch (Error | Exception e) {
            log("load config failed!");
        }
        return false;
    }

    private static void saveConfig(Context context) {
        try {
            Intent saveConfigIntent = new Intent();
            saveConfigIntent.setAction(HookParams.SAVE_DINGTALK_ENHANCEMENT_CONFIG);
            saveConfigIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            saveConfigIntent.putExtra("params", new Gson().toJson(HookParams.getInstance()));
            context.sendBroadcast(saveConfigIntent);
            log("saving config...");
        } catch (Error | Exception e) {
            log("saving config failed!");
        }
    }

    private static XSharedPreferences getPreferencesInstance() {
        if (preferencesInstance == null) {
            preferencesInstance = new XSharedPreferences(Main.class.getPackage().getName(), HookParams.DINGTALK_ENHANCEMENT_CONFIG_NAME);
            preferencesInstance.makeWorldReadable();
        } else {
            preferencesInstance.reload();
        }
        return preferencesInstance;
    }

}
