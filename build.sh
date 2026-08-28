#!/usr/bin/env bash
# 构建 LeoDebugger LSPosed 模块
# 依赖: JDK, d8 (build-tools), aapt2, apksigner
set -e
cd "$(dirname "$0")"

JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk}"
D8_JAR="${D8_JAR:-/tmp/build-tools/android-14/lib/d8.jar}"
AAPT2="${AAPT2:-/tmp/bt_mac/android-14/aapt2}"
APKSIGNER_JAR="${APKSIGNER_JAR:-/tmp/build-tools/android-14/lib/apksigner.jar}"
ANDROID_JAR="${ANDROID_JAR:-/tmp/android_34.jar}"
KS="${KS:-/tmp/leo_apk/keys/leo.keystore}"
KS_PASS="${KS_PASS:-leo123456}"

rm -rf build classes stub_classes *.jar *.dex
mkdir -p build/assets build/classes

echo "[1/4] 编译 stub + 模块..."
"$JAVA_HOME/bin/javac" --release 8 -d stub_classes $(find stub -name '*.java')
jar cf stub.jar -C stub_classes .
"$JAVA_HOME/bin/javac" --release 8 -cp stub.jar -d build/classes src/com/leo/debugger/LeoDebugger.java
jar cf module_classes.jar -C build/classes .

echo "[2/4] d8 转 dex..."
"$JAVA_HOME/bin/java" -cp "$D8_JAR" com.android.tools.r8.D8 --min-api 24 --output build module_classes.jar

echo "[3/4] 打包 APK..."
"$AAPT2" link -o build/compiled.apk --manifest AndroidManifest.xml -I "$ANDROID_JAR" --min-sdk-version 24 --target-sdk-version 34
mkdir -p build/pkg
cd build/pkg
unzip -o -q ../compiled.apk
cp ../classes.dex .
mkdir -p assets
cp ../../assets/xposed_init assets/
zip -r ../../unsigned.apk AndroidManifest.xml resources.arsc classes.dex assets/
cd ../..

echo "[4/4] 签名..."
"$JAVA_HOME/bin/java" -jar "$APKSIGNER_JAR" sign --min-sdk-version 24 --ks "$KS" --ks-pass pass:"$KS_PASS" --out leo_debugger.apk unsigned.apk

echo "完成: leo_debugger.apk"
