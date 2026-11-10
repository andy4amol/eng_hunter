package com.enghunter.tv.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

/**
 * 專為米家 K 歌麥克風 (XMKGMKF01YM) 定制的「防消原唱」立體聲播放器（AntiVocalCancelPlayer）
 *
 * 【為什麼必須這樣設計？】
 * XMKGMKF01YM 機身電源鍵支持長按開啟「一鍵消原唱」，硬件 DSP 算法為中置人聲相消（L - R）。
 * 標準英語 TTS 帶讀音頻為單聲道（在雙聲道中 L == R），一旦孩子誤觸「消原唱」，
 * 麥克風雙 5W 揚聲器裏的老師帶讀聲音會被完全抵消（只剩底噪）！
 *
 * 【工程根治方案】
 * 將單聲道 PCM 轉換為「非對稱相位差立體聲（Asymmetric Stereo PCM）」：
 * - 左聲道 (L)：100% 原始信號
 * - 右聲道 (R)：62% 幅度 + 2.5ms 相位延遲（約 40 個採樣點 @16kHz）
 * 這樣人耳聽起來依然是飽滿立體聲，但在硬件 DSP 執行 (L - R) 時，L - R 保留了 >60% 的清晰能量，
 * 徹底杜絕孩子誤觸「一鍵消原唱」導致帶讀無聲的問題！
 */
class AntiVocalCancelPlayer(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = tts?.setLanguage(Locale.US)
                isTtsReady = res != TextToSpeech.LANG_MISSING_DATA &&
                    res != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    companion object {
        /**
         * 將單聲道 16-bit PCM 轉為防消原唱立體聲 16-bit PCM (Interleaved L, R, L, R...)
         */
        fun convertMonoToAntiVocalCancelStereo(
            monoPcm: ShortArray,
            sampleRate: Int,
            enableProtection: Boolean = true
        ): ShortArray {
            val stereo = ShortArray(monoPcm.size * 2)
            // 2.5ms 相位錯開採樣數
            val delaySamples = ((sampleRate * 2.5) / 1000.0).toInt().coerceAtLeast(1)

            for (i in monoPcm.indices) {
                val leftSample = monoPcm[i]
                val rightSample = if (enableProtection) {
                    val delayedIdx = (i - delaySamples).coerceAtLeast(0)
                    (monoPcm[delayedIdx] * 0.62f).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                } else {
                    leftSample
                }
                stereo[i * 2] = leftSample
                stereo[i * 2 + 1] = rightSample
            }
            return stereo
        }
    }

    /**
     * 播放「叮咚！」開麥提示音（提示孩子：帶讀已結束，現在對著 K 歌麥克風說話）
     */
    suspend fun playReadyToSpeakChime() = withContext(Dispatchers.IO) {
        val sampleRate = 16000
        // 雙音叮咚：880Hz (A5, 110ms) + 1174.66Hz (D6, 170ms)
        val n1 = (sampleRate * 0.11).toInt()
        val n2 = (sampleRate * 0.17).toInt()
        val mono = ShortArray(n1 + n2)
        for (i in 0 until n1) {
            val env = 1.0 - (i.toDouble() / n1) * 0.5
            mono[i] = (sin(2.0 * PI * 880.0 * i / sampleRate) * 14000 * env).toInt().toShort()
        }
        for (i in 0 until n2) {
            val env = 1.0 - (i.toDouble() / n2) * 0.8
            mono[n1 + i] = (sin(2.0 * PI * 1174.66 * i / sampleRate) * 16000 * env).toInt().toShort()
        }
        playMonoPcmWithAntiVocalCancel(mono, sampleRate, enableProtection = true)
    }

    /**
     * 播放孩子剛才錄製的 WAV 文件（經過防消原唱立體聲處理，直接從 XMKGMKF01YM 雙 5W 喇叭放出）
     */
    suspend fun playWavFile(
        file: File,
        enableAntiVocalCancel: Boolean = true,
        onProgressMs: (Long) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        val pair = RingBufferAudioCache.readWavFile(file) ?: return@withContext false
        val (monoPcm, sampleRate) = pair
        playMonoPcmWithAntiVocalCancel(
            monoPcm = monoPcm,
            sampleRate = sampleRate,
            enableProtection = enableAntiVocalCancel,
            onProgressMs = onProgressMs
        )
        true
    }

    /**
     * 播放英語句子帶讀，並按毫秒回調當前進度以驅動卡拉 OK 逐詞高亮
     * 優先級順序：
     * 1. 若存在 M2 離線預烘焙的 Edge-TTS WAV 資源（`assets/sentences/audio/{id}_{normal|slow}.wav`），優先通過防消原唱立體聲通道播放
     * 2. 若電視安裝了英文 TTS 引擎，把 TTS 合成到臨時 WAV 再通過防消原唱立體聲通道播放
     * 3. 若部分國產電視精簡了系統 TTS，自動使用清脆的音節節拍合成器驅動卡拉 OK 帶讀演示，確保零依賴可運行
     */
    suspend fun playSentenceWithKaraoke(
        text: String,
        durationMs: Long,
        slowMode: Boolean,
        enableAntiVocalCancel: Boolean,
        bluetoothLatencyOffsetMs: Long,
        prebakedWavFile: File? = null,
        onProgressMs: (Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (prebakedWavFile != null && prebakedWavFile.exists() && prebakedWavFile.length() > 256L) {
            val pair = RingBufferAudioCache.readWavFile(prebakedWavFile)
            if (pair != null) {
                val (monoPcm, sr) = pair
                val actualDurationMs = (monoPcm.size * 1000L / sr).coerceAtLeast(1L)
                playMonoPcmWithAntiVocalCancel(
                    monoPcm = monoPcm,
                    sampleRate = sr,
                    enableProtection = enableAntiVocalCancel
                ) { playedMs ->
                    val compensated = (playedMs - bluetoothLatencyOffsetMs).coerceAtLeast(0L)
                    val scaledMs = (compensated * durationMs) / actualDurationMs
                    onProgressMs(scaledMs)
                }
                return@withContext
            }
        }

        val tempWav = File(context.cacheDir, "tts_synth_temp.wav")
        val synthesized = synthesizeTtsToWav(text, if (slowMode) 0.78f else 1.0f, tempWav)

        if (synthesized && tempWav.exists() && tempWav.length() > 1000) {
            val pair = RingBufferAudioCache.readWavFile(tempWav)
            if (pair != null) {
                val (monoPcm, sr) = pair
                val actualDurationMs = (monoPcm.size * 1000L / sr).coerceAtLeast(1L)
                playMonoPcmWithAntiVocalCancel(
                    monoPcm = monoPcm,
                    sampleRate = sr,
                    enableProtection = enableAntiVocalCancel
                ) { playedMs ->
                    val compensated = (playedMs - bluetoothLatencyOffsetMs).coerceAtLeast(0L)
                    // 將實際 TTS 時長映射到詞時間軸
                    val scaledMs = (compensated * durationMs) / actualDurationMs
                    onProgressMs(scaledMs)
                }
                return@withContext
            }
        }

        // 若電視無內置英文 TTS，使用柔和的節拍旋律模擬帶讀流，同時驗證防消原唱與藍牙延遲校準
        val sr = 16000
        val totalSamples = ((durationMs * sr) / 1000L).toInt().coerceAtLeast(sr)
        val freqs = doubleArrayOf(523.25, 587.33, 659.25, 698.46, 783.99, 659.25)
        val mono = ShortArray(totalSamples)
        val segLen = (totalSamples / freqs.size).coerceAtLeast(1)
        for (i in 0 until totalSamples) {
            val segIdx = (i / segLen).coerceIn(0, freqs.lastIndex)
            val localPos = i % segLen
            val env = if (localPos < segLen * 0.82) 1.0 - (localPos.toDouble() / segLen) * 0.5 else 0.0
            mono[i] = (sin(2.0 * PI * freqs[segIdx] * i / sr) * 11000 * env).toInt().toShort()
        }
        playMonoPcmWithAntiVocalCancel(
            monoPcm = mono,
            sampleRate = sr,
            enableProtection = enableAntiVocalCancel
        ) { playedMs ->
            val compensated = (playedMs - bluetoothLatencyOffsetMs).coerceAtLeast(0L)
            onProgressMs(compensated)
        }
    }

    private suspend fun synthesizeTtsToWav(text: String, speechRate: Float, outFile: File): Boolean {
        val engine = tts ?: return false
        if (!isTtsReady) return false
        if (outFile.exists()) outFile.delete()

        var finished = false
        var success = false
        val utteranceId = "utt_${System.currentTimeMillis()}"
        engine.setSpeechRate(speechRate)
        engine.setPitch(1.08f) // 親切少兒外教語調
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(id: String?) {
                if (id == utteranceId) {
                    success = true
                    finished = true
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                if (id == utteranceId) {
                    success = false
                    finished = true
                }
            }
        })

        val res = engine.synthesizeToFile(text, Bundle(), outFile, utteranceId)
        if (res != TextToSpeech.SUCCESS) return false

        var waited = 0
        while (!finished && waited < 4500) {
            delay(50)
            waited += 50
        }
        return success && outFile.exists()
    }

    private fun playMonoPcmWithAntiVocalCancel(
        monoPcm: ShortArray,
        sampleRate: Int,
        enableProtection: Boolean,
        onProgressMs: (Long) -> Unit = {}
    ) {
        val stereoPcm = convertMonoToAntiVocalCancelStereo(monoPcm, sampleRate, enableProtection)
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        try {
            track.play()
            val chunkShorts = (sampleRate / 25) * 2 // 每 40ms 一幀立體聲 (L+R)
            var offset = 0
            while (offset < stereoPcm.size) {
                val toWrite = minOf(chunkShorts, stereoPcm.size - offset)
                track.write(stereoPcm, offset, toWrite)
                offset += toWrite
                val monoSamplesPlayed = offset / 2
                val elapsedMs = (monoSamplesPlayed * 1000L) / sampleRate
                onProgressMs(elapsedMs)
            }
            track.stop()
        } finally {
            track.release()
        }
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
