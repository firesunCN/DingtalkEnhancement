package me.firesun.dingtalk.enhancement.util;

public class HookParams {
    public static final String SAVE_DINGTALK_ENHANCEMENT_CONFIG = "dingtalk.intent.action.SAVE_DINGTALK_ENHANCEMENT_CONFIG";
    public static final String DINGTALK_ENHANCEMENT_CONFIG_NAME = "dingtalk_enhancement_config";
    public static final String DINGTALK_PACKAGE_NAME = "com.alibaba.android.rimet";
    public static final int VERSION_CODE = 10; //大版本变动时
    private static HookParams instance = null;
    public String WukongMessageImplClassName = "com.alibaba.wukong.im.message.MessageImpl";
    public String WukongCallbackClassName = "com.alibaba.wukong.Callback";
    public String UpdateToReadClassName;
    public String UpdateToReadMethod;
    public String UpdateToViewMethod;
    public String MessageDsClassName;
    public String MessageDsProxyClassName;
    public String GetSingletionMethod;
    public String MessageRecallMethod;
    public String MessageHandlerMethod;
    public String MessageUpdateMethod;
    public String MessageGetWritableDatabaseMethod = "getWritableDatabase";
    public String IMDatabaseClassName = "com.alibaba.wukong.im.base.IMDatabase";
    public String versionName;
    public int versionCode;

    private HookParams() {
    }

    public static HookParams getInstance() {
        if (instance == null)
            instance = new HookParams();
        return instance;
    }

    public static void setInstance(HookParams i) {
        instance = i;
    }

    public static boolean hasInstance() {
        return instance != null;
    }

}
