# Leo WebView Debugger (LSPosed 模块)

> ⚠️ **仅供学习 WebView 调试与 Xposed 模块开发**。配合 [leo-oral-pk-automator](https://github.com/functy23/leo-oral-pk-automator) 使用。
> 使用自动化工具参与在线 PK 对战违反平台用户协议，可能导致账号封禁。请勿用于作弊。

强制开启小猿口算 (com.fenbi.android.leo) WebView 远程调试的 **LSPosed/Xposed 模块**。

## 作用

小猿口算的 H5 对局页面 (Vue SPA) 生产环境**默认不开启 WebView 调试**（代码中有调用但运行时未生效）。
本模块通过 hook 强制开启 `WebView.setWebContentsDebuggingEnabled(true)`，使外部可通过
**Chrome DevTools 协议 (CDP)** 访问对局页面（配合主脚本使用）。

## 配合使用

**必须与 [leo-oral-pk-automator](https://github.com/functy23/leo-oral-pk-automator) 配合**：

```
leo-webview-debugger (本仓库)         leo-oral-pk-automator (主脚本)
┌──────────────────────────┐         ┌──────────────────────────────┐
│ 强制开启 WebView 调试      │────┐    │ 通过 CDP 注入 JS               │
│ (LSPosed 运行时 hook)     │    └───▶│ 读答案 → 自动答题 → 自动结算    │
└──────────────────────────┘         └──────────────────────────────┘
```

1. 安装本模块 APK (`release/leo_debugger_v1.0.apk`)
2. LSPosed 中启用模块，作用域勾选 `com.fenbi.android.leo`
3. 运行主仓库的 `pk_auto.py`

## 文件结构

| 文件 | 说明 |
|------|------|
| `src/com/leo/debugger/LeoDebugger.java` | 模块主逻辑 (hook 实现) |
| `stub/` | 编译用最小 API 桩 (Xposed API + Android 类) |
| `assets/xposed_init` | LSPosed 入口声明 |
| `AndroidManifest.xml` | 模块元数据 |
| `release/leo_debugger_v1.0.apk` | 预构建 APK (直接安装) |
| `build.sh` | 从源码构建 |

## Hook 逻辑 (LeoDebugger.java)

```java
1. hook Activity.onCreate → 每个 Activity 创建时强制 WebView.setWebContentsDebuggingEnabled(true)
2. hook WebView.setWebContentsDebuggingEnabled → 参数强制 true (防被关闭)
3. hook com.tencent.smtt.sdk.WebView (X5) 同款 hook (兼容)
```

## 构建

需要: JDK 17+, d8 (Android build-tools), apksigner

```bash
# 1. 编译 (stub 编译成 jar 作为 classpath, 只输出模块类)
javac --release 8 -d stub_classes $(find stub -name '*.java')
jar cf stub.jar -C stub_classes .
javac --release 8 -cp stub.jar -d classes src/com/leo/debugger/LeoDebugger.java

# 2. d8 转 dex
jar cf module_classes.jar -C classes .
java -cp d8.jar com.android.tools.r8.D8 --min-api 24 --output . module_classes.jar

# 3. 打包 APK (需 aapt2 编译 manifest)
aapt2 link -o compiled.apk --manifest AndroidManifest.xml --min-sdk-version 24 --target-sdk-version 34
# 组装: compiled.apk 的 manifest/resources + classes.dex + assets/xposed_init → zip

# 4. 签名
apksigner sign --min-sdk-version 24 --ks your.keystore --out leo_debugger.apk unsigned.apk
```

> 注意: 不能把 `de.robv.android.xposed.*` 类编译进 APK (LSPosed 会拒绝加载),
> stub 仅用于编译期, 运行时由框架提供。

## License

MIT License - 仅限学习研究。
