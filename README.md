# EngHunter 🦁 — 小米电视少儿英语开口王 (Android TV)

专为小学生设计的电视端英语常用句式听读与开口训练应用，针对 **小米电视 (MIUI TV / PatchWall)** 与 **米家 K 歌麦克风 (`XMKGMKF01YM`)** 深度定制 **模式 A 全无线音频闭环 (`A2DP` + `HFP/SCO`)**。

---

## ✨ 核心特性

1. **米家 K 歌麦克风 (`XMKGMKF01YM`) 模式 A 全无线闭环**
   - **听外教带读 (`A2DP`)**：处于 `AudioManager.MODE_NORMAL`，通过麦克风内置的 **双 5W 对置扬声器 + 双无源辐射盆** 播放高保真外教带读与回放对比音。
   - **免按键自动开麦跟读 (`HFP/SCO`)**：带读播放完毕后自动发出“叮咚”提示音，切换 `AudioManager.MODE_IN_COMMUNICATION` 并唤醒 K 歌麦克风 **16mm 心形指向动圈咪芯**（兼容 Android 6~11 `startBluetoothSco()` 与 Android 12+ `setCommunicationDevice()`）。
   - **VAD 静音检测自动截断**：孩子开口说完后停顿 `850ms` 自动停止录音并切回 `A2DP`，无需一边按遥控器一边说话。

2. **防“一键消原唱”非对称立体声保护 (`AntiVocalCancelPlayer`)**
   - `XMKGMKF01YM` 机身电源键支持开启硬件“一键消原唱”（DSP 中置相消 `L - R`）。
   - 本项目将单声道 TTS 与回放音频实时转换为 **非对称相位差立体声 (`L = 100%`, `R = 62% + 2.5ms 延迟`)**，确保即使孩子误触“消原唱”，外教带读音也绝不消失。

3. **小学生护心评测与趣味激励闭环**
   - **4 种单句训练子模式**：① 标准整句跟读、② 意群分段拆解 (Chunking)、③ 看图换词造句 (Slot Substitution)、④ 双支 `XMKGMKF01YM` 双麦对唱问答。
   - **护心反馈原则**：孩子只要开口即保底 1 颗星；仅使用绿色与温暖橙色高亮，每次最多指出 1 个重点改进词。
   - **3 星专属奖励**：拿满 3 星自动解锁 **`XMKGMKF01YM` 9 种 DSP 音效（怪兽/卡通/KTV）5 秒自由搞怪配音秀**。

4. **阶段 0 硬件探测与校准工作台**
   - 实时枚举小米电视底层 `AudioDeviceInfo` 输入/输出拓扑、采样率与设备 ID。
   - 一键测试 HFP 无线拾音 + 双 5W 喇叭回放、防消原唱开关对比、蓝牙延迟毫秒级 (`±30ms`) 同步微调、遥控器按键 `KeyCode` 监控。
   - **20 槽位环形 WAV 缓存 (`RingBufferAudioCache`)**：严格限制缓存体积 `< 4MB`，防撑爆小米电视闪存。

5. **里程碑 M1：开发机数字孪生模拟器 (`VirtualKaraokeMicTwin`) + GitHub Actions 自动打包**
   - **零真机/零麦克风闭环开发**：当开发电脑不在客厅电视旁、不方便手持麦克风说话时，支持一键切换 **“模式 D：开发机数字孪生模拟”** 或点击跟读页的 **“🧪 数字孪生模拟”** 按钮。
   - **4 种小学生声学画像仿真**：
     - `🌟 样本A：3星洪亮标准跟读`（验证 VAD 0.85s 自动截断与 3 星解锁 9 种音效秀）
     - `🟠 样本B：2星中间漏读/卡顿`（验证第 3 个词漏读时的温和橙色高亮与单点提示）
     - `🌱 样本C：1星害羞微弱声音`（验证“只要敢开口即保底 1 星”护心机制）
     - `📡 样本D：8kHz CVSD 蓝牙窄带重采样`（验证老款电视 HFP 8000Hz 自动线性插值重采样至 16000Hz）
   - **GitHub Actions 自动构建 (`android-ci.yml`)**：每次 `git push` 自动运行全部 JVM 单元测试并构建可安装的 `enghunter-tv-debug-apk` 产物。

6. **里程碑 M2：《小学英语核心 60 句高保真卡拉 OK 资源包》 (`PrimarySchool60Catalog` + `OfflineKaraokePackLoader`)**
   - **6 大主题单元 × 每单元 10 句 = 60 句完整课程体系**：
     - `U1 礼貌交际` · `U2 点餐请求` · `U3 校园文具` · `U4 家人爱好` · `U5 时间天气` · `U6 动物出行`
   - **四维结构化标注**：每句均内置 **意群拆解 (`chunks`)**、**看图换词造句 (`substitutions`)**、**双麦对唱问答 (`rolePlayQuestion`)** 以及 **常速 (1.0x) / 慢速 (0.78x) 毫秒级卡拉 OK 逐词时间轴 (`WordBoundary`)**。
   - **离线烘焙脚本 (`tools/bake_edge_tts_pack.py`)**：支持零网络导出 `app/src/main/assets/sentences/sentences_manifest.json` 清单，或联网通过 `edge-tts` (`en-US-AnaNeural` 美式少儿音色) 批量烘焙高保真音频包。

---

## 📂 项目目录结构

```text
EngHunter/
├── .github/workflows/
│   └── android-ci.yml                               # GitHub Actions 自动测试与 APK 打包流水线
├── tools/
│   └── bake_edge_tts_pack.py                        # M2 离线预烘焙 60 句卡拉 OK 资源包脚本 (支持 Edge-TTS)
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/sentences/
│       │   │   └── sentences_manifest.json          # M2 预烘焙的小学英语核心 60 句卡拉 OK 词级时间戳清单
│       │   ├── java/com/enghunter/tv/
│       │   │   ├── MainActivity.kt                  # TV 入口与录音/蓝牙权限管理
│       │   │   ├── audio/
│       │   │   │   ├── AntiVocalCancelPlayer.kt     # 防一键消原唱立体声播放器 & 卡拉OK高亮同步
│       │   │   │   ├── OfflineKaraokePackLoader.kt  # M2 离线 60 句卡拉 OK 资源包加载与 JSON 解析器
│       │   │   │   ├── RingBufferAudioCache.kt      # 20槽位环形 WAV 缓存管理
│       │   │   │   ├── VirtualKaraokeMicTwin.kt     # M1 开发机硬件数字孪生模拟器 (4种儿童语音样本)
│       │   │   │   └── XiaomiKaraokeMicManager.kt   # XMKGMKF01YM A2DP/HFP 路由与 VAD 录音引擎
│       │   │   ├── model/
│       │   │   │   ├── PrimarySchool60Catalog.kt    # M2 小学英语 1~6 年级 6 大单元 60 句完整题库
│       │   │   │   └── SentenceModels.kt            # 句式卡片、逐词时间戳与双麦问答模型
│       │   │   ├── scoring/
│       │   │   │   └── PronunciationScorer.kt       # 小学生护心发音评测器
│       │   │   ├── ui/
│       │   │   │   └── EngHunterTvApp.kt            # Jetpack Compose for TV 双页面大屏 UI (含6单元筛选栏)
│       │   │   └── viewmodel/
│       │   │       └── EngHunterViewModel.kt        # 模式 A 全无线闭环 & 数字孪生状态机
│       │   └── res/
│       └── test/java/com/enghunter/tv/
│           └── AudioAndScoringTest.kt               # 防消原唱、数字孪生4样本、M2 60句题库与清单解析单元测试
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew
```

---

## 🚀 编译与安装到小米电视

### 1. 本地编译 Debug APK 与运行单元测试
```bash
./gradlew testDebugUnitTest assembleDebug
```
编译产物路径：`app/build/outputs/apk/debug/app-debug.apk`

### 2. 通过 ADB 安装到小米电视
```bash
adb connect <小米电视局域网IP>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
