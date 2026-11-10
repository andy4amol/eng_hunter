#!/usr/bin/env bash
# =============================================================================
# EngHunter — 电脑端 Android 模拟器一键安装、启动与测试辅助脚本 (run_on_emulator.sh)
# =============================================================================
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_HOME="${ANDROID_HOME:-/Users/lulei/Library/Android/sdk}"
JAVA_HOME="${JAVA_HOME:-/Users/lulei/tools/jdk21/Contents/Home}"
ADB="$ANDROID_HOME/platform-tools/adb"
EMULATOR="$ANDROID_HOME/emulator/emulator"
APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"

export ANDROID_HOME JAVA_HOME

echo "🦁 [EngHunter] 检查开发机环境..."
echo "   • ANDROID_HOME = $ANDROID_HOME"
echo "   • APK_PATH     = $APK_PATH"

# 1. 确保最新 APK 已编译
if [[ ! -f "$APK_PATH" ]] || [[ "${1:-}" == "--rebuild" ]]; then
  echo "🔨 正在编译最新 Debug APK..."
  (cd "$PROJECT_DIR" && ./gradlew assembleDebug)
fi

# 2. 检查当前是否已有运行中的模拟器或连接设备
ONLINE_DEVICES=$("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}')

if [[ -z "$ONLINE_DEVICES" ]]; then
  echo "🔍 当前没有正在运行的模拟器，正在检查已创建的 AVD..."
  AVD_LIST=$("$EMULATOR" -list-avds || true)
  if [[ -z "$AVD_LIST" ]]; then
    echo "⚠️  检测到你的电脑尚未创建 Android 虚拟设备 (AVD) 系统镜像。"
    echo ""
    echo "👉 只需在 Android Studio 中花 2 分钟创建一次："
    echo "   1. 打开已为你唤起的 Android Studio -> 点击顶部菜单 Tools -> Device Manager"
    echo "   2. 点击 '+' (Create Virtual Device) -> 选择 TV -> Android TV (1080p)（或任意 Tablet 横屏设备）"
    echo "   3. 选择 API 34 / API 35 (arm64-v8a) 镜像点击下载并 Finish，然后点击 ▶️ 启动模拟器"
    echo "   4. 模拟器启动后，再次运行本脚本： ./tools/run_on_emulator.sh"
    open -a "Android Studio" "$PROJECT_DIR" || true
    exit 0
  fi

  FIRST_AVD=$(echo "$AVD_LIST" | head -n 1)
  echo "🚀 正在后台启动模拟器: $FIRST_AVD ..."
  nohup "$EMULATOR" -avd "$FIRST_AVD" -netdelay none -netspeed full >/tmp/enghunter_emulator.log 2>&1 &

  echo "⏳ 等待模拟器系统启动完成..."
  "$ADB" wait-for-device
  while [[ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]]; do
    sleep 2
  done
  echo "✅ 模拟器启动完成！"
fi

# 3. 安装 APK 并自动授予麦克风权限
echo "📦 正在安装 EngHunter Debug APK 到模拟器..."
"$ADB" install -r "$APK_PATH"

echo "🔑 自动授予模拟器 RECORD_AUDIO / MODIFY_AUDIO_SETTINGS 权限..."
"$ADB" shell pm grant com.enghunter.tv android.permission.RECORD_AUDIO 2>/dev/null || true

# 4. 启动 MainActivity
echo "🎬 正在启动 EngHunter 应用界面..."
"$ADB" shell am start -n com.enghunter.tv/.MainActivity

echo ""
echo "========================================================================="
echo "🎉 EngHunter 已在模拟器启动！电脑键盘操作指南（100% 对应小米电视遥控器）："
echo "   • 方向键 ⬆️ ⬇️ ⬅️ ➡️  ：移动电视金色焦点框"
echo "   • 回车键 Enter        ：对应小米遥控器【确定/OK键】"
echo "   • Esc / Backspace    ：对应小米遥控器【返回键】"
echo "   • 鼠标左键点击        ：所有大屏按钮同样支持直接用鼠标点击！"
echo ""
echo "🧪 模拟器免麦克风测试技巧："
echo "   • 直接点击底部紫色的【🧪 数字孪生模拟: 模拟 3星】按钮，无需对着电脑说话，"
echo "     即可完整体验：语音合成流 -> 实时声波跳动 -> 0.85s VAD 自动停止 -> 三星评分！"
echo "   • 切换到顶部【② 阶段0：K歌麦硬件探测台】，可切换 4 种小学生发音样本（3星/2星漏词/1星害羞/8kHz重采样）。"
echo "   • 切换到顶部【🤖 AI供应商设置】，可一键切换 DeepSeek / 千问 / 硅基流动 / 本地 Ollama，"
echo "     或在电脑终端用一行命令直接注入 API Key："
echo "     $ADB shell am broadcast -a com.enghunter.tv.CONFIG_LLM --es provider DEEPSEEK --es model deepseek-chat --es api_key sk-你的密钥"
echo "========================================================================="
