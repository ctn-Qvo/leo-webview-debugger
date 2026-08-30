# Leo WebView Debugger (LSPosed 模块)

> 仅供学习 WebView 调试与 Xposed 模块开发。配合 leo-oral-pk-automator 使用。

强制开启小猿口算 (com.fenbi.android.leo) WebView 远程调试的 LSPosed/Xposed 模块。

## 配合使用

必须与 https://github.com/functy23/leo-oral-pk-automator 配合：

1. 从 **Releases** 下载预构建 APK (leo_debugger_v1.0.apk)
2. 安装后 LSPosed 启用模块, 作用域勾选 com.fenbi.android.leo
3. 运行主仓库的 pk_auto.py

## 源码结构

- src/com/leo/debugger/LeoDebugger.java 模块主逻辑
- stub/ 编译用 API 桩
- assets/xposed_init 入口声明
- AndroidManifest.xml 模块元数据
- build.sh 构建脚本 (需要 JDK + d8 + aapt2 + apksigner)

## 构建

bash build.sh

## License

MIT
