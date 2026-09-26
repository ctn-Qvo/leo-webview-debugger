package com.leo.debugger;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * 为小猿口算（com.fenbi.android.leo）的 WebView 注入 remote-allow-origins，
 * 解决 Chrome/WebView 111+ 版本建立 WebSocket 调试连接时的 403 Forbidden 问题。
 */
public class LeoDebugger implements IXposedHookLoadPackage {

    private static final String TARGET_PACKAGE = "com.fenbi.android.leo";
    private static final String TAG = "[LeoDebugger] ";

    /** 允许的来源；生产环境建议改为 http://127.0.0.1:9333 */
    private static final String ALLOW_ORIGIN = "*";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) return;

        log("hooked package: " + lpparam.packageName);

        // 1. 尝试在进程启动早期就注入
        injectAllowOrigins(lpparam);

        // 2. 强制开启 WebView 调试
        hookDebugEnabled(lpparam);

        // 3. 核心：在 WebViewChromium.init() 之前再次注入
        hookWebViewChromiumInit(lpparam);

        // 4. Hook CommandLine 构造函数，兜底注入
        hookCommandLineCtor(lpparam);

        log("all hooks installed");
    }

    // ============================================================
    // 1. 开启 WebView 调试
    // ============================================================
    private void hookDebugEnabled(LoadPackageParam lpparam) {
        // Activity.onCreate
        try {
            XposedHelpers.findAndHookMethod(Activity.class, "onCreate",
                    Bundle.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                WebView.setWebContentsDebuggingEnabled(true);
                            } catch (Throwable ignored) {}
                        }
                    });
            log("Activity.onCreate hooked");
        } catch (Throwable t) {
            log("activity hook err: " + t);
        }

        // 原生 WebView
        try {
            XposedHelpers.findAndHookMethod(WebView.class,
                    "setWebContentsDebuggingEnabled", boolean.class,
                    new ForceTrueHook());
            log("WebView.setWebContentsDebuggingEnabled hooked");
        } catch (Throwable t) {
            log("webview hook err: " + t);
        }

        // 腾讯 X5 内核（如果使用）
        try {
            XposedHelpers.findAndHookMethod("com.tencent.smtt.sdk.WebView",
                    lpparam.classLoader, "setWebContentsDebuggingEnabled",
                    boolean.class, new ForceTrueHook());
            log("X5 WebView hooked");
        } catch (Throwable ignored) {
            // 未使用 X5 内核
        }
    }

    // ============================================================
    // 2. 核心：Hook WebViewChromium.init(Map, boolean)
    // ============================================================
    private void hookWebViewChromiumInit(LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                    "com.android.webview.chromium.WebViewChromium",
                    lpparam.classLoader,
                    "init",
                    Map.class,
                    boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            log("WebViewChromium.init() intercepted");
                            injectAllowOrigins(lpparam);
                        }
                    });
            log("WebViewChromium.init hooked");
        } catch (Throwable t) {
            log("hookWebViewChromiumInit fail: " + t);
        }
    }

    // ============================================================
    // 3. 兜底：Hook CommandLine 构造函数
    // ============================================================
    private void hookCommandLineCtor(LoadPackageParam lpparam) {
        try {
            Class<?> cmdClass = XposedHelpers.findClass(
                    "org.chromium.base.CommandLine", lpparam.classLoader);
            XposedBridge.hookAllConstructors(cmdClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        XposedHelpers.callMethod(param.thisObject,
                                "a", "remote-allow-origins", ALLOW_ORIGIN);
                        log("CommandLine ctor inject ok");
                    } catch (Throwable t) {
                        log("CommandLine ctor inject fail: " + t);
                    }
                }
            });
            log("CommandLine constructor hooked");
        } catch (Throwable t) {
            log("hookCommandLineCtor fail: " + t);
        }
    }

    // ============================================================
    // 4. 注入 remote-allow-origins
    //    对应关系（反编译确认）：
    //      静态字段 d          = 单例 sInstance
    //      方法 a(String,String) = appendSwitchWithValue
    //      方法 c(String)       = hasSwitch
    // ============================================================
    private void injectAllowOrigins(LoadPackageParam lpparam) {
        try {
            Class<?> cmdClass = XposedHelpers.findClass(
                    "org.chromium.base.CommandLine", lpparam.classLoader);

            Object cmd = XposedHelpers.getStaticObjectField(cmdClass, "d");
            if (cmd == null) {
                log("CommandLine.d is null, skip");
                return;
            }

            // 先检查是否已经注入过，避免重复
            boolean alreadyHas = false;
            try {
                alreadyHas = (boolean) XposedHelpers.callMethod(
                        cmd, "c", "remote-allow-origins");
            } catch (Throwable ignored) {}

            if (alreadyHas) {
                log("remote-allow-origins already set, skip");
                return;
            }

            // 调用 a(String, String) = appendSwitchWithValue
            XposedHelpers.callMethod(cmd, "a",
                    "remote-allow-origins", ALLOW_ORIGIN);

            // 验证
            boolean has = (boolean) XposedHelpers.callMethod(
                    cmd, "c", "remote-allow-origins");
            log("remote-allow-origins=" + ALLOW_ORIGIN
                    + " injected, hasSwitch=" + has);

        } catch (Throwable t) {
            log("injectAllowOrigins fail: " + t);
        }
    }

    // ============================================================
    // 工具类
    // ============================================================
    private static void log(String msg) {
        XposedBridge.log(TAG + msg);
    }

    /** 强制把 boolean 参数改为 true */
    static class ForceTrueHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            if (param.args != null && param.args.length > 0) {
                param.args[0] = true;
            }
        }
    }
}
