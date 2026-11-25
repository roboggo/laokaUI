#!/bin/bash
# 墨屏桌面 —— 离线构建脚本（仅依赖 Android SDK + JDK，不联网、不需要 Gradle）
set -e

ROOT="$(cd "$(dirname "$0")" && pwd)"
SDK="$HOME/Library/Android/sdk"
BT="$SDK/build-tools/36.0.0"
AJAR="$SDK/platforms/android-34/android.jar"
JDK=""

# 定位 JDK（brew openjdk@17）
for c in \
  "$(dirname "$0")/../.toolchain/jdk-17.0.21.1+2/Contents/Home/bin" \
  "/opt/homebrew/opt/openjdk@17/bin" \
  "/usr/local/opt/openjdk@17/bin" \
  "$HOME/.jdks"; do
  if [ -x "$c/javac" ]; then JDK="$c"; break; fi
done
if [ -z "$JDK" ]; then
  echo "ERROR: 找不到 JDK javac，请先 brew install openjdk@17"
  exit 1
fi
JAVAC="$JDK/javac"; JAVA="$JDK/java"; KEYTOOL="$JDK/keytool"
export JAVA_HOME="$(cd "$JDK/.." && pwd)"

echo "==> 使用 JDK: $JDK  (JAVA_HOME=$JAVA_HOME)"
cd "$ROOT"
rm -rf build gen classes dex && mkdir -p build gen classes dex

echo "==> 1/7 编译资源 (aapt2 compile)"
"$BT/aapt2" compile --dir res -o build/res.zip

echo "==> 2/7 链接资源与 Manifest (aapt2 link)"
"$BT/aapt2" link \
  -I "$AJAR" \
  --manifest AndroidManifest.xml \
  --java gen \
  --min-sdk-version 24 \
  --target-sdk-version 34 \
  --version-code 1 --version-name 1.0 \
  -o build/base.apk \
  build/res.zip

echo "==> 3/7 编译 Java"
"$JAVAC" -source 8 -target 8 -encoding UTF-8 \
  -bootclasspath "$AJAR" -cp "$AJAR" \
  -d classes \
  src/com/inklauncher/minimal/*.java \
  $(find gen -name "*.java")

echo "==> 4/7 生成 DEX"
"$BT/d8" --lib "$AJAR" --min-api 24 --output dex $(find classes -name "*.class")

echo "==> 5/7 打包进 APK"
cp build/base.apk build/unsigned.apk
cd dex && zip -q ../build/unsigned.apk classes.dex && cd ..

echo "==> 6/7 对齐 (zipalign)"
"$BT/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk

echo "==> 7/7 签名 (apksigner)"
# keystore 放在项目根目录（不随 build 清理），保证多次构建签名一致、可覆盖安装
if [ ! -f debug.jks ]; then
  "$KEYTOOL" -genkeypair -v -keystore debug.jks \
    -alias inkdesktop -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass 123456 -keypass 123456 \
    -dname "CN=InkDesktop, OU=Personal, O=Personal, L=NA, ST=NA, C=CN" >/dev/null 2>&1
fi
"$BT/apksigner" sign --ks debug.jks \
  --ks-key-alias inkdesktop --ks-pass pass:123456 --key-pass pass:123456 \
  --v1-signing-enabled true --v2-signing-enabled true \
  --out build/InkDesktop-v1.6.apk build/aligned.apk

export JAVA_HOME="$(cd "$JDK/.." && pwd)"
"$BT/apksigner" verify --print-certs build/InkDesktop-v1.6.apk | head -6
echo ""
echo "==> 完成"
ls -lh build/InkDesktop-v1.6.apk
echo "路径: $ROOT/build/InkDesktop-v1.6.apk"
