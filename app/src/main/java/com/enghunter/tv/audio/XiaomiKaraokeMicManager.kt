package com.enghunter.tv.audio

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 專為小米米家 K 歌麥克風 (XMKGMKF01YM) 打造的【模式 A：全無線 K 歌麥閉環】路由與錄音引擎
 *
 * 硬件協同機制：
 * 1. 待機與帶讀播放時（A2DP 高清音箱模式）：
 *    保持 AudioManager.MODE_NORMAL，電視通過 Bluetooth A2DP 將防消原唱立體聲發送到 XMKGMKF01YM 的雙 5W 對置揚聲器。
 * 2. 輪到孩子開口跟讀時（HFP/SCO 無線拾音模式）：
 *    - 自動切換到 AudioManager.MODE_IN_COMMUNICATION
 *    - 啟動藍牙 SCO / HFP 通道（兼容 Android 6~11 的 startBluetoothSco 與 Android 12+ 的 setCommunicationDevice）
 *    - 優先綁定 TYPE_BLUETOOTH_SCO（若同時插了 USB 聲卡或選了其他路由也可一鍵切換）
 *    - 實時計算 RMS 音量波形與 VAD（人聲起止檢測：開口激活 + 停頓 850ms 自動結束）
 * 3. 錄音結束瞬間：
 *    - 立即釋放 HFP/SCO 通道並恢復 AudioManager.MODE_NORMAL，讓 XMKGMKF01YM 無縫切回 A2DP 雙 5W 喇叭播放星星獎勵音效與對比回放！
 */
class XiaomiKaraokeMicManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    data class AudioEndpointItem(
        val id: Int,
        val productName: String,
        val typeCode: Int,
        val typeLabel: String,
        val isSource: Boolean,
        val sampleRates: List<Int>,
        val isXiaomiKaraokeMicCandidate: Boolean
    )

    data class LiveRecordingFrame(
        val elapsedMs: Long,
        val currentRms: Float, // 0f..1f 歸一化音量，用於電視大屏實時聲波動畫
        val peakRaw: Int,
        val hasSpeechStarted: Boolean,
        val activeDeviceSummary: String,
        val actualSampleRate: Int,
        val scoStateLabel: String
    )

    data class RecordingSessionResult(
        val pcm16kMono: ShortArray,
        val rawSampleRate: Int,
        val durationMs: Long,
        val peakAmplitude: Int,
        val averageRms: Double,
        val stoppedByVadSilence: Boolean,
        val usedBluetoothHfp: Boolean,
        val deviceNameUsed: String,
        val diagnosticLog: String
    )

    enum class PreferredInputMode(val title: String, val subtitle: String) {
        MODE_A_WIRELESS_HFP(
            "模式 A：全无线 K歌麦闭环 (A2DP 播 + HFP 录)",
            "首选方案：自动切换 XMKGMKF01YM 的 HFP 无线拾音与双 5W 喇叭播放"
        ),
        MODE_B_USB_LINE_IN(
            "模式 B：主麦有线底座 + 副麦无线连主麦",
            "备选方案：优先选择 USB 声卡/音频线输入，孩子手持副麦无线对讲"
        ),
        MODE_C_REMOTE_OR_BUILTIN(
            "模式 C：K歌麦外放 + 遥控器/电视内置麦收音",
            "保底方案：不切换蓝牙 HFP，保持 A2DP 常开，用遥控器或电视麦录音"
        ),
        MODE_DEV_SIMULATOR(
            "模式 D：开发机数字孪生模拟 (零真机/零麦克风)",
            "办公开发专用：仿真 450ms HFP 握手 + 自动注入 4 种小学生发音样本"
        )
    }

    /**
     * 掃描當前小米電視識別到的所有音頻輸入與輸出設備
     */
    fun scanAudioEndpoints(): Pair<List<AudioEndpointItem>, List<AudioEndpointItem>> {
        val inputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).map { it.toEndpointItem(true) }
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).map { it.toEndpointItem(false) }
        return inputs to outputs
    }

    private fun AudioDeviceInfo.toEndpointItem(isInput: Boolean): AudioEndpointItem {
        val name = productName?.toString()?.ifBlank { "AndroidTVAudioDevice" } ?: "AndroidTVAudioDevice"
        val label = deviceTypeToReadable(type)
        val isKaraokeCandidate = name.contains("Xiaomi", ignoreCase = true) ||
            name.contains("MIJIA", ignoreCase = true) ||
            name.contains("K歌", ignoreCase = true) ||
            name.contains("Karaoke", ignoreCase = true) ||
            name.contains("XMKGMKF", ignoreCase = true) ||
            type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            type == AudioDeviceInfo.TYPE_USB_DEVICE ||
            type == AudioDeviceInfo.TYPE_USB_HEADSET

        return AudioEndpointItem(
            id = id,
            productName = name,
            typeCode = type,
            typeLabel = label,
            isSource = isInput,
            sampleRates = sampleRates.toList(),
            isXiaomiKaraokeMicCandidate = isKaraokeCandidate
        )
    }

    /**
     * 開啟模式 A 的藍牙 HFP/SCO 上行語音通道，並等待連接就緒（最多等待 1600ms）
     */
    suspend fun engageWirelessHfpChannel(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val logs = StringBuilder()
        logs.append("正在请求开启 XMKGMKF01YM 蓝牙 HFP/SCO 无线麦克风通道...\n")

        var scoConnected = false
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED) {
                    val state = intent.getIntExtra(
                        AudioManager.EXTRA_SCO_AUDIO_STATE,
                        AudioManager.SCO_AUDIO_STATE_ERROR
                    )
                    if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                        scoConnected = true
                    }
                }
            }
        }

        try {
            context.registerReceiver(
                receiver,
                IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
            )
        } catch (_: Exception) {
        }

        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val scoTarget = audioManager.availableCommunicationDevices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
                }
                if (scoTarget != null) {
                    val ok = audioManager.setCommunicationDevice(scoTarget)
                    logs.append("Android 12+ setCommunicationDevice(${scoTarget.productName}): $ok\n")
                    if (ok) scoConnected = true
                }
            }

            @Suppress("DEPRECATION")
            if (audioManager.isBluetoothScoAvailableOffCall) {
                audioManager.startBluetoothSco()
                audioManager.isBluetoothScoOn = true
                logs.append("已调用 startBluetoothSco() (isBluetoothScoAvailableOffCall=true)\n")
            } else {
                logs.append("注意：系统报告 isBluetoothScoAvailableOffCall=false，仍尝试请求 SCO...\n")
                @Suppress("DEPRECATION")
                audioManager.startBluetoothSco()
            }

            var waitedMs = 0
            while (!scoConnected && waitedMs < 1500) {
                delay(100)
                waitedMs += 100
                @Suppress("DEPRECATION")
                if (audioManager.isBluetoothScoOn) {
                    val hasScoInput = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
                        .any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
                    if (hasScoInput) {
                        scoConnected = true
                        break
                    }
                }
            }

            val scoInputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
                .filter { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
            if (scoInputs.isNotEmpty()) {
                scoConnected = true
                logs.append("成功发现蓝牙 HFP 输入设备：${scoInputs.joinToString { "${it.productName}(id=${it.id})" }}\n")
            } else {
                logs.append("未在 INPUTS 列表中看到独立 TYPE_BLUETOOTH_SCO，将尝试由 VOICE_COMMUNICATION / VOICE_RECOGNITION 音源采集（等待耗时 ${waitedMs}ms）\n")
            }
        } catch (e: Exception) {
            logs.append("开启 HFP 异常: ${e.message}\n")
        } finally {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }

        return@withContext scoConnected to logs.toString()
    }

    /**
     * 關閉藍牙 HFP/SCO，切回 A2DP 立體聲高清播放模式（讓 XMKGMKF01YM 雙 5W 喇叭恢復工作）
     */
    suspend fun releaseWirelessHfpAndRestoreA2dp() = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            }
            @Suppress("DEPRECATION")
            audioManager.isBluetoothScoOn = false
            @Suppress("DEPRECATION")
            audioManager.stopBluetoothSco()
            audioManager.mode = AudioManager.MODE_NORMAL
            // 留出 250ms 給 XMKGMKF01YM 從 HFP 切回 A2DP 雙喇叭模式
            delay(250)
        } catch (_: Exception) {
        }
    }

    /**
     * 執行一次完整的人聲採集（支持 VAD 說完自動停止 + 8kHz/16kHz 自適應重採樣到 16kHz + 開發機數字孿生模擬）
     */
    @SuppressLint("MissingPermission")
    suspend fun recordWithAutoVad(
        mode: PreferredInputMode,
        maxDurationMs: Long = 6500L,
        enableVadAutoStop: Boolean = true,
        silenceHangoverMs: Long = 850L,
        simulatedProfile: VirtualKaraokeMicTwin.SimulatedChildVoiceProfile =
            VirtualKaraokeMicTwin.SimulatedChildVoiceProfile.PERFECT_3_STARS,
        expectedWordCount: Int = 6,
        onLiveFrame: (LiveRecordingFrame) -> Unit
    ): RecordingSessionResult = withContext(Dispatchers.IO) {
        if (mode == PreferredInputMode.MODE_DEV_SIMULATOR) {
            return@withContext runDigitalTwinSimulation(
                profile = simulatedProfile,
                wordCount = expectedWordCount,
                maxDurationMs = maxDurationMs,
                enableVadAutoStop = enableVadAutoStop,
                silenceHangoverMs = silenceHangoverMs,
                onLiveFrame = onLiveFrame
            )
        }

        val diag = StringBuilder()
        var hfpEngaged = false

        if (mode == PreferredInputMode.MODE_A_WIRELESS_HFP) {
            val (ok, hfpLog) = engageWirelessHfpChannel()
            hfpEngaged = ok
            diag.append(hfpLog)
        } else {
            audioManager.mode = AudioManager.MODE_NORMAL
        }

        // HFP 藍牙通道可能是 16000Hz（mSBC 寬帶語音）或 8000Hz（CVSD 窄帶語音），自動按優先級探測
        val candidateRates = intArrayOf(16000, 8000, 44100, 48000)
        val audioSource = when (mode) {
            PreferredInputMode.MODE_A_WIRELESS_HFP -> MediaRecorder.AudioSource.VOICE_COMMUNICATION
            PreferredInputMode.MODE_B_USB_LINE_IN -> MediaRecorder.AudioSource.VOICE_RECOGNITION
            PreferredInputMode.MODE_C_REMOTE_OR_BUILTIN -> MediaRecorder.AudioSource.VOICE_RECOGNITION
            PreferredInputMode.MODE_DEV_SIMULATOR -> MediaRecorder.AudioSource.VOICE_RECOGNITION
        }

        var chosenRecorder: AudioRecord? = null
        var chosenRate = 16000
        var chosenBufBytes = 4096

        for (rate in candidateRates) {
            val minBuf = AudioRecord.getMinBufferSize(
                rate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuf <= 0) continue
            val bufSize = (minBuf * 2).coerceAtLeast(rate / 5 * 2)
            try {
                val rec = AudioRecord(
                    audioSource,
                    rate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufSize
                )
                if (rec.state == AudioRecord.STATE_INITIALIZED) {
                    chosenRecorder = rec
                    chosenRate = rate
                    chosenBufBytes = bufSize
                    break
                } else {
                    rec.release()
                }
            } catch (_: Exception) {
            }
        }

        // 若 VOICE_COMMUNICATION 初始化失敗，回退到 MIC
        if (chosenRecorder == null) {
            val minBuf = AudioRecord.getMinBufferSize(
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(3200)
            chosenRecorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf * 2
            )
            chosenRate = 16000
            chosenBufBytes = minBuf * 2
        }

        val recorder = chosenRecorder!!
        val targetDevice = pickPreferredInputDevice(mode)
        if (targetDevice != null) {
            recorder.setPreferredDevice(targetDevice)
            diag.append("首选绑定输入设备: ${targetDevice.productName} (${deviceTypeToReadable(targetDevice.type)})\n")
        }

        val rawPcmBuffer = ByteArrayOutputStream()
        var peakAmp = 0
        var sumSquares = 0.0
        var totalSamples = 0L
        var speechDetected = false
        var lastVoiceTimestamp = 0L
        var stoppedByVad = false
        var activeDeviceName = targetDevice?.productName?.toString() ?: "系统默认麦克风"

        try {
            recorder.startRecording()
            val startTime = System.currentTimeMillis()
            lastVoiceTimestamp = startTime

            val frameSamples = (chosenRate / 20).coerceAtLeast(160) // 50ms 一幀
            val shortChunk = ShortArray(frameSamples)

            while (isActive) {
                val now = System.currentTimeMillis()
                val elapsed = now - startTime
                if (elapsed >= maxDurationMs) break

                val readCount = recorder.read(shortChunk, 0, shortChunk.size)
                if (readCount > 0) {
                    val byteChunk = ByteBuffer.allocate(readCount * 2).order(ByteOrder.LITTLE_ENDIAN)
                    var framePeak = 0
                    var frameSumSq = 0.0
                    for (i in 0 until readCount) {
                        val s = shortChunk[i]
                        byteChunk.putShort(s)
                        val absV = abs(s.toInt())
                        if (absV > framePeak) framePeak = absV
                        if (absV > peakAmp) peakAmp = absV
                        frameSumSq += s.toDouble() * s.toDouble()
                    }
                    rawPcmBuffer.write(byteChunk.array())
                    sumSquares += frameSumSq
                    totalSamples += readCount

                    val frameRms = sqrt(frameSumSq / readCount)
                    val normLevel = (frameRms / 4500.0).toFloat().coerceIn(0f, 1f)

                    // 讀取實際路由到的硬件設備名稱
                    recorder.routedDevice?.let { routed ->
                        activeDeviceName = "${routed.productName} (${deviceTypeToReadable(routed.type)})"
                    }

                    // VAD 門限：XMKGMKF01YM 16mm 心形指向咪芯 + DRC，人聲開口時 RMS 通常 > 550
                    if (frameRms > 520.0 || framePeak > 2200) {
                        if (!speechDetected) {
                            speechDetected = true
                        }
                        lastVoiceTimestamp = now
                    }

                    onLiveFrame(
                        LiveRecordingFrame(
                            elapsedMs = elapsed,
                            currentRms = normLevel,
                            peakRaw = peakAmp,
                            hasSpeechStarted = speechDetected,
                            activeDeviceSummary = activeDeviceName,
                            actualSampleRate = chosenRate,
                            scoStateLabel = if (hfpEngaged) "HFP 无线已激活" else "标准采集"
                        )
                    )

                    // 如果孩子已經開口說了至少 600ms，並且隨後連續安靜超過 silenceHangoverMs (850ms)，自動結束錄音！
                    if (enableVadAutoStop && speechDetected && elapsed > 1100L) {
                        val silenceMs = now - lastVoiceTimestamp
                        if (silenceMs >= silenceHangoverMs) {
                            stoppedByVad = true
                            diag.append("VAD 自动检测到孩子说完停顿 (${silenceMs}ms)，自动截断录音。\n")
                            break
                        }
                    }
                }
            }
        } finally {
            try {
                recorder.stop()
            } catch (_: Exception) {
            }
            recorder.release()
            if (mode == PreferredInputMode.MODE_A_WIRELESS_HFP) {
                releaseWirelessHfpAndRestoreA2dp()
                diag.append("已自动关闭 HFP 并恢复 XMKGMKF01YM A2DP 双 5W 扬声器输出通道。\n")
            }
        }

        val rawBytes = rawPcmBuffer.toByteArray()
        val rawShorts = ShortArray(rawBytes.size / 2)
        ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(rawShorts)

        // 若藍牙 HFP 返回的是 8000Hz，線性插值重採樣至標準 16000Hz 供評測與緩存使用
        val pcm16k = if (chosenRate == 16000) {
            rawShorts
        } else {
            resampleTo16kHz(rawShorts, chosenRate)
        }

        val durationMs = if (chosenRate > 0) (rawShorts.size * 1000L) / chosenRate else 0L
        val avgRms = if (totalSamples > 0) sqrt(sumSquares / totalSamples) else 0.0
        diag.append("采集完成：原始采样率=${chosenRate}Hz, 时长=${durationMs}ms, 峰值=${peakAmp}, RMS=${avgRms.toInt()}")

        RecordingSessionResult(
            pcm16kMono = pcm16k,
            rawSampleRate = chosenRate,
            durationMs = durationMs,
            peakAmplitude = peakAmp,
            averageRms = avgRms,
            stoppedByVadSilence = stoppedByVad,
            usedBluetoothHfp = hfpEngaged,
            deviceNameUsed = activeDeviceName,
            diagnosticLog = diag.toString()
        )
    }

    private suspend fun runDigitalTwinSimulation(
        profile: VirtualKaraokeMicTwin.SimulatedChildVoiceProfile,
        wordCount: Int,
        maxDurationMs: Long,
        enableVadAutoStop: Boolean,
        silenceHangoverMs: Long,
        onLiveFrame: (LiveRecordingFrame) -> Unit
    ): RecordingSessionResult {
        // 1. 模擬真實 XMKGMKF01YM 藍牙 5.1 HFP/SCO 通道切換握手延遲 (450ms)
        delay(450L)

        // 2. 生成高仿真兒童跟讀語音包絡 PCM
        val simulatedRaw = VirtualKaraokeMicTwin.synthesizeChildSpeechPcm(
            profile = profile,
            wordCount = wordCount
        )

        // 3. 按真實時間節奏回放 50ms 幀流並執行 VAD 自動截斷
        return VirtualKaraokeMicTwin.runVadStreamOverPcm(
            rawPcm = simulatedRaw,
            sampleRate = profile.simulatedSampleRate,
            maxDurationMs = maxDurationMs,
            enableVadAutoStop = enableVadAutoStop,
            silenceHangoverMs = silenceHangoverMs
        ) { elapsedMs, frameRmsNorm, peakSoFar, speechStarted ->
            onLiveFrame(
                LiveRecordingFrame(
                    elapsedMs = elapsedMs,
                    currentRms = frameRmsNorm,
                    peakRaw = peakSoFar,
                    hasSpeechStarted = speechStarted,
                    activeDeviceSummary = "数字孪生::XMKGMKF01YM [${profile.badge}]",
                    actualSampleRate = profile.simulatedSampleRate,
                    scoStateLabel = "数字孪生 HFP 仿真中"
                )
            )
        }
    }

    private fun pickPreferredInputDevice(mode: PreferredInputMode): AudioDeviceInfo? {
        val inputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        return when (mode) {
            PreferredInputMode.MODE_A_WIRELESS_HFP -> {
                inputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
                    ?: inputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLE_HEADSET }
                    ?: inputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_USB_DEVICE || it.type == AudioDeviceInfo.TYPE_USB_HEADSET }
            }
            PreferredInputMode.MODE_B_USB_LINE_IN -> {
                inputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_USB_DEVICE || it.type == AudioDeviceInfo.TYPE_USB_HEADSET }
                    ?: inputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET }
            }
            PreferredInputMode.MODE_C_REMOTE_OR_BUILTIN -> {
                inputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
                    ?: inputs.firstOrNull()
            }
            PreferredInputMode.MODE_DEV_SIMULATOR -> null
        }
    }

    private fun resampleTo16kHz(input: ShortArray, srcRate: Int): ShortArray {
        return VirtualKaraokeMicTwin.resampleTo16kHz(input, srcRate)
    }

    companion object {
        fun deviceTypeToReadable(type: Int): String = when (type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "蓝牙立体声音箱 (A2DP)"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "蓝牙无线麦克风通话通道 (HFP/SCO)"
            AudioDeviceInfo.TYPE_USB_DEVICE -> "USB 音频设备/声卡"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB 耳麦声卡"
            AudioDeviceInfo.TYPE_BUILTIN_MIC -> "电视/遥控器内置麦克风"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "小米电视内置扬声器"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "3.5mm 有线耳麦接口"
            AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_HDMI_ARC -> "HDMI 音频通道"
            else -> "音频设备(Type=$type)"
        }
    }
}
