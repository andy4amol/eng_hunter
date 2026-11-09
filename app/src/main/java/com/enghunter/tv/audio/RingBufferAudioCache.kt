package com.enghunter.tv.audio

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 環形錄音緩存管理器（RingBufferAudioCache）
 * 1. 解決小米電視存儲空間小（通常僅 2~4GB 剩餘）的問題：
 *    孩子練習的錄音保存在 context.cacheDir，最多保留最近 20 條 WAV（總共 < 4MB），自動循環覆蓋。
 * 2. 支持將 16bit PCM ShortArray 保存為標準 WAV 文件或從 WAV 讀取 PCM。
 */
class RingBufferAudioCache(
    private val context: Context,
    private val maxRingFiles: Int = 20
) {
    private val ringDir: File = File(context.cacheDir, "child_recordings_ring").apply {
        if (!exists()) mkdirs()
    }
    private val bestDir: File = File(context.filesDir, "best_sentence_recordings").apply {
        if (!exists()) mkdirs()
    }

    private var currentRingIndex: Int = 0

    data class SavedRecording(
        val file: File,
        val sampleRate: Int,
        val durationMs: Long,
        val peakAmplitude: Int,
        val rmsAmplitude: Double
    )

    /**
     * 寫入一條新的練習錄音到環形緩存（slot_00.wav ~ slot_19.wav）
     */
    @Synchronized
    fun saveToRingBuffer(pcmData: ShortArray, sampleRate: Int): SavedRecording {
        val slotName = String.format("slot_%02d.wav", currentRingIndex % maxRingFiles)
        currentRingIndex = (currentRingIndex + 1) % maxRingFiles
        val targetFile = File(ringDir, slotName)
        writeWavFile(targetFile, pcmData, sampleRate)
        return analyzePcm(targetFile, pcmData, sampleRate)
    }

    /**
     * 若本句得分破紀錄，保存為該句式的「最佳對比錄音」
     */
    @Synchronized
    fun saveBestForSentence(sentenceId: String, pcmData: ShortArray, sampleRate: Int): SavedRecording {
        val safeId = sentenceId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val targetFile = File(bestDir, "best_${safeId}.wav")
        writeWavFile(targetFile, pcmData, sampleRate)
        return analyzePcm(targetFile, pcmData, sampleRate)
    }

    fun getRingFilesInfo(): List<File> {
        return ringDir.listFiles { f -> f.extension == "wav" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun getTotalCacheSizeBytes(): Long {
        val ringBytes = ringDir.listFiles()?.sumOf { it.length() } ?: 0L
        val bestBytes = bestDir.listFiles()?.sumOf { it.length() } ?: 0L
        return ringBytes + bestBytes
    }

    private fun analyzePcm(file: File, pcmData: ShortArray, sampleRate: Int): SavedRecording {
        var peak = 0
        var sumSq = 0.0
        for (sample in pcmData) {
            val absVal = kotlin.math.abs(sample.toInt())
            if (absVal > peak) peak = absVal
            sumSq += sample.toDouble() * sample.toDouble()
        }
        val rms = if (pcmData.isNotEmpty()) kotlin.math.sqrt(sumSq / pcmData.size) else 0.0
        val durationMs = if (sampleRate > 0) (pcmData.size * 1000L) / sampleRate else 0L
        return SavedRecording(
            file = file,
            sampleRate = sampleRate,
            durationMs = durationMs,
            peakAmplitude = peak,
            rmsAmplitude = rms
        )
    }

    companion object {
        fun writeWavFile(file: File, pcmData: ShortArray, sampleRate: Int) {
            val byteData = ByteBuffer.allocate(pcmData.size * 2)
                .order(ByteOrder.LITTLE_ENDIAN)
            for (s in pcmData) {
                byteData.putShort(s)
            }
            val pcmBytes = byteData.array()
            val totalDataLen = pcmBytes.size + 36
            val byteRate = sampleRate * 2 // 16-bit mono

            FileOutputStream(file).use { out ->
                val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
                header.put("RIFF".toByteArray(Charsets.US_ASCII))
                header.putInt(totalDataLen)
                header.put("WAVE".toByteArray(Charsets.US_ASCII))
                header.put("fmt ".toByteArray(Charsets.US_ASCII))
                header.putInt(16) // Subchunk1Size for PCM
                header.putShort(1) // AudioFormat = 1 (PCM)
                header.putShort(1) // NumChannels = 1 (Mono)
                header.putInt(sampleRate)
                header.putInt(byteRate)
                header.putShort(2) // BlockAlign = 2 bytes
                header.putShort(16) // BitsPerSample = 16
                header.put("data".toByteArray(Charsets.US_ASCII))
                header.putInt(pcmBytes.size)
                out.write(header.array())
                out.write(pcmBytes)
            }
        }

        fun readWavFile(file: File): Pair<ShortArray, Int>? {
            if (!file.exists() || file.length() <= 44) return null
            FileInputStream(file).use { input ->
                val header = ByteArray(44)
                if (input.read(header) != 44) return null
                val headerBuf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
                val sampleRate = headerBuf.getInt(24)
                val pcmBytes = input.readBytes()
                val shortCount = pcmBytes.size / 2
                val shorts = ShortArray(shortCount)
                ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer()
                    .get(shorts)
                return shorts to sampleRate
            }
        }
    }
}
