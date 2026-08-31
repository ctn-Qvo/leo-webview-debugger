# AGENTS.md — AI 开发指南 (leo-webview-debugger)

> **拼写确认**: 本文件名为 AGENTS.md（不是 AGNETS.md / Agent.md）。
> 本文件为 AI 编码助手（Claude / DeepSeek / Cursor / Codex 等）提供本模块的完整工作上下文。
> **任何修改、扩展、调试本模块前，必须完整阅读本文件。**

---

## 0. 模块一句话

LSPosed/Xposed 模块，**运行时强制开启小猿口算 (com.fenbi.android.leo) 的 WebView 远程调试**，
使主机端脚本（leo-oral-pk-automator）能通过 Chrome DevTools 协议 (CDP) 注入 JS 实现自动答题。

**必须配合主仓库使用**: https://github.com/functy23/leo-oral-pk-automator

---

## 1. 为什么需要这个模块

- 小猿口算 H5 对局页**生产环境不开启** WebView 调试
- 应用代码 (UtilsKt.defaultConfig) 有调用 setWebContentsDebuggingEnabled(true)，但运行时未生效
- **不能用 frida**: 应用有反 Frida 检测（周期性扫描，注入即退出）
- **不能重打包 APK**: 触发崩溃检测（签名/完整性校验）
- → 唯一可靠路线: LSPosed 运行时 hook，不改 APK

---

## 2. 架构总览

```
LSPosed (Zygisk)
  └─ 模块加载 (com.leo.debugger)
       ├─ hook 1: Activity.onCreate → WebView.setWebContentsDebuggingEnabled(true)
       ├─ hook 2: WebView.setWebContentsDebuggingEnabled → 参数强制 true (防被关闭)
       └─ hook 3: com.tencent.smtt.sdk.WebView (X5) 同款 hook (兼容)
  └─ 效果: WebView 创建 devtools 端口
       localabstract:webview_devtools_remote_<pid>
  └─ 主机端: adb forward tcp:9333 → CDP 访问
```

---

## 3. 文件结构

| 文件 | 说明 |
|------|------|
| src/com/leo/debugger/LeoDebugger.java | **模块主逻辑** (所有 hook 在此) |
| stub/ | 编译期 API 桩 (Xposed API + Android 类, 仅编译用) |
| assets/xposed_init | LSPosed 入口声明 (指向 LeoDebugger) |
| AndroidManifest.xml | 模块元数据 (xposedmodule=true 等) |
| build.sh | 构建脚本 (javac + d8 + aapt2 + apksigner) |
| README.md | 用户使用说明 |
| AGENTS.md | 本文件 |

---

## 4. LeoDebugger.java 代码解剖

### 4.1 入口
```java
public class LeoDebugger implements IXposedHookLoadPackage {
    public void handleLoadPackage(LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("com.fenbi.android.leo")) return;  // 只处理小猿口算
        ...
    }
}
```

### 4.2 hook 清单

| # | hook 目标 | 时机 | 作用 |
|---|----------|------|------|
| 1 | Activity.onCreate | after | 每个 Activity 创建后强制开启调试 (最可靠时机) |
| 2 | WebView.setWebContentsDebuggingEnabled | before | 参数强制 true, 防应用关闭调试 |
| 3 | com.tencent.smtt.sdk.WebView.setWebContentsDebuggingEnabled | before | X5 内核兼容 |

### 4.3 hook 实现要点

- **不能用匿名内部类**: d8 8.x 对匿名类 (new XC_MethodHook(){...}) 有 bug (NullPointerException)，必须用具名静态类 (ForceTrueHook / ActivityCreateHook)
- **Activity.onCreate 比 Application.attachBaseContext 可靠**: attachBaseContext hook 可能因 LSPosed hidden api 限制失败
- **X5 兼容**: 应用可能用腾讯 X5 内核, 需同时 hook com.tencent.smtt.sdk.WebView

---

## 5. 构建 (build.sh)

### 依赖 (默认指向 ~/android-build-tools/, 可用环境变量覆盖)
- JDK 17 (实测 Zulu 17: /Library/Java/JavaVirtualMachines/zulu-17.jdk)
- build-tools r34 (含 d8/aapt2/zipalign/apksigner): ~/android-build-tools/bt-r34/
- android.jar (API 34): ~/android-build-tools/android-34/android.jar
- 签名 keystore: ~/android-build-tools/keys/leo.keystore (storepass/keypass: leo123456)

下载方式 (build-tools 解压后目录名 android-14 需改名为 bt-r34):
```bash
curl -L -o bt34.zip https://dl.google.com/android/repository/build-tools_r34-macosx.zip
curl -L -o platform34.zip https://dl.google.com/android/repository/platform-34-ext7_r03.zip
```

### 构建流程 (build.sh 内)
```
1. javac 编译 stub → stub.jar (作为 classpath)
2. javac 编译 LeoDebugger.java (-cp stub.jar, 只输出模块类)
3. d8 转 dex (--min-api 24)
4. aapt2 link 编译 manifest (--min-sdk-version 24 --target-sdk-version 34)
5. 组装 APK: resources.arsc 用 zip -0 STORED 存入 (未压缩), 其余条目 zip -9
6. zipalign -f 4 对齐 (Android 11+ 要求 arsc 未压缩且 4 字节对齐)
7. apksigner 签名 (v2/v3, 不破坏 zipalign; 勿用 jarsigner)
8. zipalign -c 校验
```

### 环境变量覆盖 (build.sh 支持)
```bash
JAVA_HOME=/path/to/jdk BT=/path/bt-r34 ANDROID_JAR=/path/android.jar \
KS=/path/keystore KS_PASS=pass bash build.sh
```

---

## 6. 安装与验证

1. 安装: 从 GitHub Releases 下载 APK (R1.0 tag), 或用 build.sh 自建
2. LSPosed 启用模块, 作用域勾选 com.fenbi.android.leo
3. 重启小猿口算 (必须重启才生效)
4. 验证调试端口:
   ```bash
   adb shell 'cat /proc/net/unix | grep webview'   # 应看到 webview_devtools_remote_<pid>
   adb forward tcp:9333 localabstract:webview_devtools_remote_<pid>
   curl http://127.0.0.1:9333/json                 # 应返回页面列表
   ```
5. 检查 LSPosed 日志: /data/adb/lspd/log/ 应有 "[LeoDebugger] hooks installed"

---

## 7. 常见问题排查

| 问题 | 原因 | 解决 |
|------|------|------|
| LSPosed 报 Cannot load module | Xposed API 类编译进了 APK | 确认 classes 输出不含 de/robv/android/xposed/* |
| d8 崩溃 (NPE in graph.u2) | 匿名内部类 | 用具名静态类 |
| 调试端口不出现 | hook 未生效 / 模块未加载 | 检查 lspd 日志; 确认作用域勾选 |
| err-activity 日志 | attachBaseContext hook 失败 | 用 Activity.onCreate 代替 |

---

## 8. 已知限制

- 模块只做"开调试", 不做任何答题逻辑 (答题在主仓库)
- 前端发版不影响本模块 (hook 的是 Android API, 不是页面 JS)
- resources.arsc 对齐问题已在 build.sh 修复 (zip -0 STORED + zipalign -f 4), Android 11+ 可安装
- keystore 于 2026-08-31 重新生成 (旧版丢失), 新旧 APK 签名不同, 覆盖安装需先卸载旧版

---

## 9. 禁止事项

- 仅限学习研究 WebView 调试 / Xposed 开发
- 禁止用于作弊、牟利、传播; 使用者自行承担账号风险
- 不提供任何账号、token、真实用户数据
