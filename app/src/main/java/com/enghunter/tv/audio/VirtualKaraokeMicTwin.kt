package com.enghunter.tv.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 【里程碑 M1】米家 K 歌麥克風 (XMKGMKF01YM) 硬件數字孿生模擬器（VirtualKaraokeMicTwin）
 *
 * 解決痛點：
 * 當開發電腦不在客廳電視旁、沒有物理連接 XMKGMKF01YM 麥克風時，也不需要對著電腦真開口說話，
 * 即可在本地開發機 / Android TV 模擬器 / JUnit 測試中 100% 仿真：
 * 1. XMKGMKF01YM 藍牙 5.1 HFP/SCO 通道握手延遲（默認 450ms）
 * 2. 4 種典型小學生跟讀聲學表現（3星滿分 / 2星漏讀第3個詞 / 1星害羞小聲 / 8kHz CVSD 藍牙窄帶降級）
 * 3. VAD（語音活動檢測）在孩子說完停頓 850ms 後自動截斷的真實幀流時序
 */
object VirtualKaraokeMicTwin {

    enum class SimulatedChildVoiceProfile(
        val title: String,
        val badge: String,
        val description: String,
        val simulatedSampleRate: Int,
        val expectedStarsHint: Int
    ) {
        PERFECT_3_STARS(
            title = "🌟 样本A：3星洪亮标准跟读",
            badge = "模拟 3星",
            description = "模拟小朋友拿着 XMKGMKF01YM 洪亮流利读完全句，说完停顿 0.85s 自动触发 VAD 截断并解锁 9 种音效秀",
            simulatedSampleRate = 16000,
            expectedStarsHint = 3
        ),
        HESITANT_MISSED_WORD_2_STARS(
            title = "🟠 样本B：2星中间漏读/卡顿",
            badge = "模拟 2星",
            description = "模拟小朋友中间第 3 个单词卡壳漏读，验证 UI 能否用温和橙色标出该词并给出单点鼓励提示",
            simulatedSampleRate = 16000,
            expectedStarsHint = 2
        ),
        SHY_LOW_VOICE_1_STAR(
            title = "🌱 样本C：1星害羞微弱声音",
            badge = "模拟 1星保底",
            description = "模拟小朋友声音极小且只说了前半句，验证“只要敢开口就保底 1 颗星”的小学生护心评分机制",
            simulatedSampleRate = 16000,
            expectedStarsHint = 1
        ),
        NARROWBAND_8KHZ_CVSD(
            title = "📡 样本D：8kHz CVSD 蓝牙窄带重采样",
            badge = "模拟 8kHz->16kHz",
            description = "模拟部分老款小米电视 HFP 降级到 8000Hz 采样率，验证线性插值重采样至 16000Hz 与打分兼容性",
            simulatedSampleRate = 8000,
            expectedStarsHint = 3
        )
    }

    /**
     * 根據目標單詞數與選擇的數字孿生畫像，生成高仿真兒童語音包絡 PCM（含尾部 1100ms 靜音供 VAD 自動截斷）
     */
    fun synthesizeChildSpeechPcm(
        profile: SimulatedChildVoiceProfile,
        wordCount: Int = 6
    ): ShortArray {
        val sampleRate = profile.simulatedSampleRate
        val safeWords = wordCount.coerceIn(2, 12)
        // 每個單詞約 340ms（含詞間微停頓），外加開頭 180ms 吸氣準備 + 結尾 1150ms 靜音（觸發 850ms VAD 截斷）
        val leadInMs = 180
        val perWordMs = 340
        val tailSilenceMs = 1150
        val totalMs = leadInMs + safeWords * perWordMs + tailSilenceMs
        val totalSamples = (sampleRate * totalMs) / 1000
        val pcm = ShortArray(totalSamples)

        val leadInSamples = (sampleRate * leadInMs) / 1000
        val wordSamples = (sampleRate * perWordMs) / 1000
        val activeVocalRatio = when (profile) {
            SimulatedChildVoiceProfile.PERFECT_3_STARS,
            SimulatedChildVoiceProfile.NARROWBAND_8KHZ_CVSD -> 0.88 // 3星样本饱满连贯发音
            else -> 0.76
        }

        for (w in 0 until safeWords) {
            val isSkippedWord = when (profile) {
                SimulatedChildVoiceProfile.HESITANT_MISSED_WORD_2_STARS -> (w == 2.coerceAtMost(safeWords - 1))
                SimulatedChildVoiceProfile.SHY_LOW_VOICE_1_STAR -> (w >= (safeWords + 1) / 2)
                else -> false
            }
            if (isSkippedWord) continue

            val peakAmplitude = when (profile) {
                SimulatedChildVoiceProfile.PERFECT_3_STARS -> 8200.0
                SimulatedChildVoiceProfile.HESITANT_MISSED_WORD_2_STARS -> if (w == 1) 2400.0 else 7200.0
                SimulatedChildVoiceProfile.SHY_LOW_VOICE_1_STAR -> 1650.0
                SimulatedChildVoiceProfile.NARROWBAND_8KHZ_CVSD -> 7800.0
            }

            val wordStart = leadInSamples + w * wordSamples
            val vocalLen = (wordSamples * activeVocalRatio).toInt()
            // 模擬兒童基頻 (240Hz ~ 295Hz) + 第一共振峰諧波
            val f0 = 245.0 + (w % 4) * 16.0

            for (i in 0 until vocalLen) {
                val sampleIdx = wordStart + i
                if (sampleIdx >= totalSamples) break
                val progress = i.toDouble() / vocalLen
                // 模擬真實元音起振與收尾包絡 (Attack-Sustain-Release)
                val envelope = sin(PI * progress)
                val t = i.toDouble() / sampleRate
                val wave = 0.65 * sin(2.0 * PI * f0 * t) +
                    0.25 * sin(2.0 * PI * (f0 * 2.0) * t) +
                    0.10 * sin(2.0 * PI * (f0 * 3.0) * t)
                val v = (wave * peakAmplitude * envelope).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                pcm[sampleIdx] = v.toShort()
            }
        }

        return pcm
    }

    /**
     * 純函數重採樣：將任意採樣率（如 8000Hz 藍牙 CVSD）線性插值轉換為標準 16000Hz PCM
     */
    fun resampleTo16kHz(input: ShortArray, srcRate: Int): ShortArray {
        if (input.isEmpty() || srcRate <= 0 || srcRate == 16000) return input
        val outLen = ((input.size.toLong() * 16000L) / srcRate).toInt().coerceAtLeast(1)
        val output = ShortArray(outLen)
        val ratio = srcRate.toDouble() / 16000.0
        for (i in 0 until outLen) {
            val srcPos = i * ratio
            val idx = srcPos.toInt().coerceIn(0, input.lastIndex)
            val nextIdx = (idx + 1).coerceAtMost(input.lastIndex)
            val frac = srcPos - idx
            val interpolated = input[idx] * (1.0 - frac) + input[nextIdx] * frac
            output[i] = interpolated.toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return output
    }

    /**
     * 在純內存中按 50ms 幀流執行 VAD 掃描與截斷（可同時用於實時模擬器與 JUnit 秒級自動化測試）
     */
    fun runVadStreamOverPcm(
        rawPcm: ShortArray,
        sampleRate: Int,
        maxDurationMs: Long = 6500L,
        enableVadAutoStop: Boolean = true,
        silenceHangoverMs: Long = 850L,
        onFrame: (elapsedMs: Long, frameRmsNorm: Float, peakSoFar: Int, speechStarted: Boolean) -> Unit = { _, _, _, _ -> }
    ): XiaomiKaraokeMicManager.RecordingSessionResult {
        val frameSamples = (sampleRate / 20).coerceAtLeast(80) // 50ms per frame
        var cursor = 0
        var elapsedMs = 0L
        var peakAmp = 0
        var sumSquares = 0.0
        var consumedSamples = 0
        var speechDetected = false
        var lastVoiceMs = 0L
        var stoppedByVad = false

        while (cursor < rawPcm.size && elapsedMs < maxDurationMs) {
            val end = (cursor + frameSamples).coerceAtMost(rawPcm.size)
            val count = end - cursor
            var framePeak = 0
            var frameSumSq = 0.0
            for (i in cursor until end) {
                val s = rawPcm[i]
                val absV = abs(s.toInt())
                if (absV > framePeak) framePeak = absV
                if (absV > peakAmp) peakAmp = absV
                frameSumSq += s.toDouble() * s.toDouble()
            }
            sumSquares += frameSumSq
            consumedSamples += count
            cursor = end
            elapsedMs = (consumedSamples * 1000L) / sampleRate

            val frameRms = sqrt(frameSumSq / count)
            val normLevel = (frameRms / 4500.0).toFloat().coerceIn(0f, 1f)

            // 與真機一致的 VAD 激活閾值
            if (frameRms > 520.0 || framePeak > 2000 || (speechDetected && frameRms > 320.0)) {
                speechDetected = true
                lastVoiceMs = elapsedMs
            } else if (!speechDetected && framePeak > 1100) {
                // 兼容 1 星害羞微弱聲音樣本的激活
                speechDetected = true
                lastVoiceMs = elapsedMs
            }

            onFrame(elapsedMs, normLevel, peakAmp, speechDetected)

            if (enableVadAutoStop && speechDetected && elapsedMs > 900L) {
                val silenceDuration = elapsedMs - lastVoiceMs
                if (silenceDuration >= silenceHangoverMs) {
                    stoppedByVad = true
                    break
                }
            }
        }

        val capturedSlice = rawPcm.copyOfRange(0, cursor)
        val pcm16k = resampleTo16kHz(capturedSlice, sampleRate)
        val avgRms = if (consumedSamples > 0) sqrt(sumSquares / consumedSamples) else 0.0

        return XiaomiKaraokeMicManager.RecordingSessionResult(
            pcm16kMono = pcm16k,
            rawSampleRate = sampleRate,
            durationMs = elapsedMs,
            peakAmplitude = peakAmp,
            averageRms = avgRms,
            stoppedByVadSilence = stoppedByVad,
            usedBluetoothHfp = true,
            deviceNameUsed = "DigitalTwin::XMKGMKF01YM(${sampleRate}Hz)",
            diagnosticLog = "[开发机数字孪生模拟器] 握手延迟=450ms | 原始采样率=${sampleRate}Hz -> 16000Hz | VAD自动截断=$stoppedByVad | 有效时长=${elapsedMs}ms"
        )
    }
}
