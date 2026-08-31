#!/usr/bin/env bash
# 构建 LeoDebugger LSPosed 模块
# 依赖: JDK17, d8 (build-tools), aapt2, zipalign, apksigner
#
# Android 11+ 安装要求: resources.arsc 必须 ①未压缩 (STORED) ②在 zip 内 4 字节对齐。
# 因此打包时 arsc 用 zip -0 存入, 打包后 zipalign -f 4 对齐, 最后 apksigner 签名
# (apksigner 只追加签名条目不重排 zip, 对齐得以保留; jarsigner 会破坏对齐, 勿用)。
set -e
cd "$(dirname "$0")"

JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 17)}"
BT="${BT:-$(pwd)/android-build-tools/bt-r34}"
D8_JAR="${D8_JAR:-$BT/lib/d8.jar}"
AAPT2="${AAPT2:-$BT/aapt2}"
ZIPALIGN="${ZIPALIGN:-$BT/zipalign}"
APKSIGNER_JAR="${APKSIGNER_JAR:-$BT/lib/apksigner.jar}"
ANDROID_JAR="${ANDROID_JAR:-$(pwd)/android-build-tools/android-34/android.jar}"
KS="${KS:-$(pwd)/android-build-tools/keys/leo.keystore}"
KS_PASS="${KS_PASS:-leo123456}"

if [ ! -x "$AAPT2" ]; then
  echo "错误: 未找到构建工具 $AAPT2" >&2
  echo "请按 AGENTS.md 第5节下载 build-tools r34 + platform-34, 解压到 ./android-build-tools/" >&2
  exit 1
fi

rm -rf build classes stub_classes *.jar *.dex
mkdir -p build/assets build/classes

echo "[1/5] 编译 stub + 模块..."
"$JAVA_HOME/bin/javac" --release 8 -d stub_classes $(find stub -name '*.java')
jar cf stub.jar -C stub_classes .
"$JAVA_HOME/bin/javac" --release 8 -cp stub.jar -d build/classes src/com/leo/debugger/LeoDebugger.java
jar cf module_classes.jar -C build/classes .

echo "[2/5] d8 转 dex..."
"$JAVA_HOME/bin/java" -cp "$D8_JAR" com.android.tools.r8.D8 --min-api 24 --output build module_classes.jar

echo "[3/5] 打包 APK (resources.arsc 强制 STORED)..."
"$AAPT2" link -o build/compiled.apk --manifest AndroidManifest.xml -I "$ANDROID_JAR" --min-sdk-version 24 --target-sdk-version 34
mkdir -p build/pkg
cd build/pkg
unzip -o -q ../compiled.apk
cp ../classes.dex .
mkdir -p assets
cp ../../assets/xposed_init assets/
rm -f ../../unsigned.apk
# -0 = STORED 不压缩 (arsc 必需); 其余条目正常压缩
zip -q -X -0 ../../unsigned.apk resources.arsc
zip -q -X -9 ../../unsigned.apk AndroidManifest.xml classes.dex assets/xposed_init
cd ../..

echo "[4/5] zipalign 4 字节对齐..."
"$ZIPALIGN" -f 4 unsigned.apk aligned.apk

echo "[5/5] 签名..."
"$JAVA_HOME/bin/java" -jar "$APKSIGNER_JAR" sign --min-sdk-version 24 --ks "$KS" --ks-pass pass:"$KS_PASS" --out leo_debugger.apk aligned.apk

"$ZIPALIGN" -c 4 leo_debugger.apk && echo "对齐校验通过" || { echo "错误: 对齐校验失败" >&2; exit 1; }
echo "完成: leo_debugger.apk"
