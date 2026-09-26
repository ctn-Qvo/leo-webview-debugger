package de.robv.android.xposed;

import java.lang.reflect.Constructor;

public final class XposedBridge {

    private XposedBridge() {}

    public static void log(String text) {
        // stub
    }

    public static void log(Throwable t) {
        // stub
    }

    public static XC_MethodHook.Unhook hookAllConstructors(
            Class<?> clazz, XC_MethodHook callback) {
        return new XC_MethodHook.Unhook();
    }

    public static XC_MethodHook.Unhook hookAllMethods(
            Class<?> clazz, String methodName, XC_MethodHook callback) {
        return new XC_MethodHook.Unhook();
    }
}
