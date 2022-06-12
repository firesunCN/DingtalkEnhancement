package me.firesun.dingtalk.enhancement.plugin;

import android.content.ContentValues;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import me.firesun.dingtalk.enhancement.PreferencesUtils;
import me.firesun.dingtalk.enhancement.util.HookParams;

import static me.firesun.dingtalk.enhancement.util.ReflectionUtil.log;


public class AntiRevoke implements IPlugin {
    private static final Map<Long, Object> msgCacheMap = new HashMap<>();
    private static final List<Long> recalledMid = new ArrayList<Long>();

    @Override
    public void hook(final XC_LoadPackage.LoadPackageParam lpparam) {
        HookParams hp = HookParams.getInstance();

        XposedHelpers.findAndHookMethod(hp.MessageDsClassName, lpparam.classLoader, hp.MessageRecallMethod, String.class, List.class, ContentValues.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    if (!PreferencesUtils.isAntiRevoke()) {
                        return;
                    }
                    if (isRecallMessage((ContentValues) param.args[2])) {
                        // 直接拦截撤回动作
                        param.setResult(0);
                    }
                } catch (Error | Exception e) {
                    log("anti recall error");
                }
            }
        });

        XposedHelpers.findAndHookMethod(hp.MessageDsClassName, lpparam.classLoader, hp.MessageHandlerMethod, String.class, Collection.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    if (!PreferencesUtils.isAntiRevoke()) {
                        return;
                    }
                    handleRecallMessage((String) param.args[0], (Collection) param.args[1], lpparam.classLoader);
                } catch (Error | Exception e) {
                    log("handle message error");
                }
            }
        });


    }

    private void handleRecallMessage(String cid, Collection messages, ClassLoader classLoader) {
        if (messages == null || messages.isEmpty() || !PreferencesUtils.isMaskReadStatus()) {
            return;
        }

        for (Object message : messages) {
            if (message == null) return;

            int mRecallStatus = XposedHelpers.getIntField(message, "mRecallStatus");
            long mMid = XposedHelpers.getLongField(message, "mMid");

            if (1 == mRecallStatus) {
                // 收到撤回指令后，需要提示这条消息被撤回了
                Object conversation = XposedHelpers.getObjectField(message, "mConversation");
                if (conversation == null) return;

                if (recalledMid.contains(mMid))
                    return;
                else
                    recalledMid.add(mMid);

                Object recallMessage = msgCacheMap.get(mMid);
                if (recallMessage == null) return;

                // 获取消息类型
                int msgType = getMsgType(recallMessage);

                try {
                    Object messageContent = XposedHelpers.callMethod(recallMessage,
                            "messageContent");
                    if (messageContent == null) return;

                    switch (msgType) {
                        case 1: // 文本消息
                            String msgText = (String) XposedHelpers.callMethod(messageContent,
                                    "text");
                            XposedHelpers.callMethod(messageContent,
                                    "setText", msgText + " [已撤回]");
                            break;
                        case 1205: // 回复消息
                            String replyText = (String) XposedHelpers.callMethod(messageContent,
                                    "getReplyText");
                            XposedHelpers.callMethod(messageContent,
                                    "setReplyText", replyText + " [已撤回]");
                            break;
                        default:
                            // 其他消息不处理了，处理起来太复杂了
                            return;
                    }


                    HookParams hp = HookParams.getInstance();
                    Class classIMDatabase = XposedHelpers.findClass(hp.IMDatabaseClassName, classLoader);
                    Class messageDs = XposedHelpers.findClass(hp.MessageDsClassName, classLoader);
                    String dbName = (String) XposedHelpers.callStaticMethod(classIMDatabase,
                            hp.MessageGetWritableDatabaseMethod);
                    Method methodMessageUpdate = XposedHelpers.findMethodExact(
                            messageDs, hp.MessageUpdateMethod,
                            String.class, String.class, List.class);
                    Class messageDsProxy = XposedHelpers.findClass(hp.MessageDsProxyClassName, classLoader);
                    Object messageDsProxyInstance = XposedHelpers.callStaticMethod(messageDsProxy, hp.GetSingletionMethod);

                    methodMessageUpdate.invoke(messageDsProxyInstance, dbName, cid, Collections.singletonList(recallMessage));
                } catch (Error | Exception e) {
                    log("handle recall message error");
                }
            } else {
                int msgType = getMsgType(message);
                if (msgType == 1 || msgType == 1205) {
                    msgCacheMap.put(mMid, message);
                }
            }
        }

    }

    private int getMsgType(Object message) {
        Object mMessageContent = XposedHelpers.getObjectField(message,
                "mMessageContent");

        return (int) XposedHelpers.callMethod(mMessageContent, "type");
    }


    private boolean isRecallMessage(ContentValues contentValues) {
        if (contentValues == null) {
            return false;
        }
        Integer integer = contentValues.getAsInteger("recall");
        return integer != null && integer == 1;
    }
}
