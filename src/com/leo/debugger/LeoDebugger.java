package com.leo.debugger;

import android.app.Activity;
import android.webkit.WebView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class LeoDebugger implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.fenbi.android.leo")) {
            return;
        }
        XposedBridge.log("[LeoDebugger] hooked: " + lpparam.packageName);

        try {
            XposedHelpers.findAndHookMethod(
                    Activity.class,
                    "onCreate",
                    android.os.Bundle.class,
                    new ActivityCreateHook());
            XposedBridge.log("[LeoDebugger] Activity.onCreate hooked");
        } catch (Throwable t) {
            XposedBridge.log("[LeoDebugger] err-activity: " + t);
        }

        try {
            XposedHelpers.findAndHookMethod(
                    WebView.class,
                    "setWebContentsDebuggingEnabled",
                    boolean.class,
                    new ForceTrueHook());
            XposedBridge.log("[LeoDebugger] WebView hook ok");
        } catch (Throwable t) {
            XposedBridge.log("[LeoDebugger] err-webview: " + t);
        }

        try {
            XposedHelpers.findAndHookMethod(
                    "com.tencent.smtt.sdk.WebView",
                    lpparam.classLoader,
                    "setWebContentsDebuggingEnabled",
                    boolean.class,
                    new ForceTrueHook());
            XposedBridge.log("[LeoDebugger] X5 hook ok");
        } catch (Throwable t) {
            XposedBridge.log("[LeoDebugger] err-x5: " + t);
        }

        XposedBridge.log("[LeoDebugger] all hooks installed");
    }

    static class ActivityCreateHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            try {
                WebView.setWebContentsDebuggingEnabled(true);
                XposedBridge.log("[LeoDebugger] WebView debug ENABLED (Activity.onCreate)");
            } catch (Throwable t) {
                XposedBridge.log("[LeoDebugger] enable err: " + t);
            }
        }
    }

    static class ForceTrueHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            param.args[0] = true;
            XposedBridge.log("[LeoDebugger] forced debug(true)");
        }
    }
}
