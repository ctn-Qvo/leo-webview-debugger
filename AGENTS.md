# AGENTS.md — AI 开发指南

本文件为 AI 编码助手提供本项目工作上下文。修改前请完整阅读。

## 项目概述

LSPosed 模块，强制开启小猿口算 (com.fenbi.android.leo) 的 WebView 远程调试。
**必须配合 leo-oral-pk-automator 使用**（主脚本通过 CDP 注入 JS 实现自动答题）。

## 为什么需要这个模块

- 小猿口算 H5 对局页生产环境**不开启** WebView 调试
- 应用代码 (UtilsKt.defaultConfig) 有无条件 setWebContentsDebuggingEnabled(true)，
  但运行时未生效 → 需要 hook 强制
- **不能用 frida**: 应用有反 Frida 检测（注入即退出）

## 架构

LSPosed (Zygisk) → 加载模块 → hook:
  1. Activity.onCreate → WebView.setWebContentsDebuggingEnabled(true)
  2. WebView.setWebContentsDebuggingEnabled → 强制参数 true
  3. com.tencent.smtt.sdk.WebView (X5) 同款
→ WebView 远程调试端口开启 (webview_devtools_remote_PID)
→ 主脚本通过 adb forward + CDP 访问

## 关键文件

| 文件 | 说明 |
|------|------|
| src/com/leo/debugger/LeoDebugger.java | **模块主逻辑** (hook 点都在这里) |
| stub/ | 编译期 API 桩 (Xposed + Android) |
| assets/xposed_init | 入口类声明 |
| release/leo_debugger_v1.0.apk | 预构建 APK |

## 开发注意事项

1. **不能把 de.robv.android.xposed.* 编译进 APK** — LSPosed 检测到会拒绝加载
   (报错: "The Xposed API classes are compiled into the module's APK")
   → stub 只做编译期 classpath，输出目录不能包含它们
2. **d8 匿名类 bug**: 匿名内部类 (new XC_MethodHook(){...}) 会导致 d8 8.x 崩溃
   (NullPointerException in graph.u2) → **必须用具名静态类** (如 ForceTrueHook)
3. **hook 时机**: Application.attachBaseContext hook 可能失败 (LSPosed hidden api 限制)，
   用 Activity.onCreate 更可靠 (每个 Activity 创建都会触发)
4. **X5 兼容**: 应用可能用腾讯 X5 内核，需同时 hook com.tencent.smtt.sdk.WebView

## 验证流程

1. 安装模块 APK
2. LSPosed 启用 + 作用域勾选 com.fenbi.android.leo
3. 重启小猿口算
4. 检查: adb forward tcp:9333 localabstract:webview_devtools_remote_PID
   然后 curl http://127.0.0.1:9333/json 应返回页面列表
5. LSPosed 日志 (/data/adb/lspd/log/) 应有 "[LeoDebugger] hooks installed"

## 常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| Cannot load module | API 类编译进 APK | 检查 classes 输出不含 de/robv |
| d8 崩溃 | 匿名内部类 | 用具名静态类 |
| 调试端口不出现 | hook 未生效 | 检查 lspd 日志, 确认模块加载 |
| err-activity | attachBaseContext hook 失败 | 用 Activity.onCreate 代替 |

## 禁止事项
- 仅限学习研究, 禁止作弊/牟利/传播
