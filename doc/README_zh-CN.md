<div align="center">

# 🐞 Leo WebView Debugger

**一个 LSPosed/Xposed 模块，强制开启小猿口算（com.fenbi.android.leo）的 WebView 远程调试。**

[![leo-webview-debugger](https://img.shields.io/badge/leo-webview-debugger-LWD-orange.svg)](https://github.com/functy23/leo-webview-debugger)
[![Java](https://img.shields.io/badge/Java-8%2B-red.svg?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Top Language](https://img.shields.io/github/languages/top/functy23/leo-webview-debugger?style=flat)](https://github.com/functy23/leo-webview-debugger)
[![Platform](https://img.shields.io/badge/platform-Android%20%7C%20LSPosed-lightgrey.svg?logo=android&logoColor=white)](https://github.com/functy23/leo-webview-debugger)

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?logo=opensourceinitiative&logoColor=white)](https://opensource.org/licenses/MIT)

[![Release](https://img.shields.io/github/v/release/functy23/leo-webview-debugger?style=flat&logo=github)](https://github.com/functy23/leo-webview-debugger/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/functy23/leo-webview-debugger/total?label=Downloads&logo=github)](https://github.com/functy23/leo-webview-debugger/releases)
[![Stars](https://img.shields.io/github/stars/functy23/leo-webview-debugger?style=flat&logo=github)](https://github.com/functy23/leo-webview-debugger/stargazers)
[![Repo Size](https://img.shields.io/github/repo-size/functy23/leo-webview-debugger?style=flat&logo=github)](https://github.com/functy23/leo-webview-debugger)
[![Contributors](https://img.shields.io/github/contributors/functy23/leo-webview-debugger?color=ee8449&logo=githubsponsors)](https://github.com/functy23/leo-webview-debugger/graphs/contributors)

[Issues](https://github.com/functy23/leo-webview-debugger/issues) • [AGENTS.md](AGENTS.md) • [Releases](https://github.com/functy23/leo-webview-debugger/releases)

[English](../README.md) | **简体中文**
</div>

---

## 概览

强制开启小猿口算 (`com.fenbi.android.leo`) WebView 远程调试的 LSPosed/Xposed 模块。

> 仅供学习 WebView 调试与 Xposed 模块开发。配合 leo-oral-pk-automator 使用。

## 配合使用

必须与 https://github.com/functy23/leo-oral-pk-automator 配合：

1. 从 **Releases** 下载预构建 APK (`leo_debugger_v1.0.apk`)
2. 安装后 LSPosed 启用模块，作用域勾选 `com.fenbi.android.leo`
3. 运行主仓库的 `pk_auto.py`

## 源码结构

- `src/com/leo/debugger/LeoDebugger.java` — 模块主逻辑
- `stub/` — 编译用 API 桩
- `assets/xposed_init` — 入口声明
- `AndroidManifest.xml` — 模块元数据
- `build.sh` — 构建脚本 (需要 JDK + d8 + aapt2 + apksigner)

## 构建

```bash
bash build.sh
```

## License

MIT
