package com.enghunter.tv.scoring

import com.enghunter.tv.audio.RingBufferAudioCache
import com.enghunter.tv.model.PronunciationReport
import com.enghunter.tv.model.SentenceCard
import com.enghunter.tv.model.WordFeedbackLevel
import com.enghunter.tv.model.WordScoreResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util. Base64
import java.util.concurrent.TimeUnit

/**
 * 【里程碑 M3】微軟 Azure Speech 免費層 (F0) 音素級發音評測客戶端 + 離線音素仿真器
 *
 * 1. 支持標準 Azure REST Pronunciation Assessment 接口：
 *    `https://{region}.stt.speech.microsoft.com/speech/recognition/conversation/cognitiveservices/v1?language=en-US`
 *    （每月免費 5 小時 = 18,000 秒，足夠小學生免費評測 4,500 次）
 * 2. 超時保護：連接 1800ms + 讀取 2500ms，一旦超時或未配置 Key 自動返回 null，由上層無縫回退本地 L0 包絡評測。
 * 3. 內置音素級 Mock 仿真器（[simulatePhonemeLevelResponseJson]）：
 *    即使開發機不填寫真實雲端 Key，也能完整驗證 Azure 音素級 JSON 解析、漏讀/錯讀音素提示（如 `/θ/`、`/r/`、`/v/`）與護心反饋映射。
 */
class AzureSpeechAssessmentClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(1800, TimeUnit.MILLISECONDS)
        .readTimeout(2500, TimeUnit.MILLISECONDS)
        .writeTimeout(2000, TimeUnit.MILLISECONDS)
        .build()
) {

    data class CloudCredentials(
        val subscriptionKey: String = "",
        val region: String = "eastasia"
    ) {
        val isConfigured: Boolean
            get() = subscriptionKey.isNotBlank() && region.isNotBlank()
    }

    /**
     * 調用 Azure Speech Pronunciation Assessment REST API
     * 若未配置憑證、網絡不可達或非 200 響應則返回 null（觸發本地 L0 自動降級）
     */
    suspend fun evaluateOnCloudOrNull(
        credentials: CloudCredentials,
        sentence: SentenceCard,
        targetWords: List<String>,
        pcm16k: ShortArray
    ): PronunciationReport? = withContext(Dispatchers.IO) {
        if (!credentials.isConfigured || pcm16k.isEmpty()) return@withContext null

        return@withContext try {
            val referenceText = targetWords.joinToString(" ")
            val pronConfigJson = JSONObject().apply {
                put("ReferenceText", referenceText)
                put("GradingSystem", "HundredMark")
                put("Granularity", "Phoneme")
                put("Dimension", "Comprehensive")
                put("EnableMiscue", true)
            }.toString()

            val pronHeaderBase64 = Base64.getEncoder()
                .encodeToString(pronConfigJson.toByteArray(Charsets.UTF_8))

            val wavBytes = encodePcm16kToWavBytes(pcm16k, 16000)
            val url = "https://${credentials.region.trim()}.stt.speech.microsoft.com/" +
                "speech/recognition/conversation/cognitiveservices/v1?language=en-US&format=detailed"

            val request = Request.Builder()
                .url(url)
                .addHeader("Ocp-Apim-Subscription-Key", credentials.subscriptionKey.trim())
                .addHeader("Pronunciation-Assessment", pronHeaderBase64)
                .addHeader("Accept", "application/json;text/xml")
                .post(wavBytes.toRequestBody("audio/wav; codecs=audio/pcm; samplerate=16000".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val bodyStr = resp.body?.string() ?: return@use null
                parseAzurePronunciationJson(
                    rawJson = bodyStr,
                    targetWords = targetWords,
                    engineLabel = "L2-Azure音素级评测 (${credentials.region})"
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {

        /**
         * 將 16kHz 單聲道 ShortArray 封裝為內存 WAV 字節流（44 字節標準 RIFF 頭）
         */
        fun encodePcm16kToWavBytes(pcm16k: ShortArray, sampleRate: Int = 16000): ByteArray {
            val dataSize = pcm16k.size * 2
            val out = ByteArrayOutputStream(44 + dataSize)
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(36 + dataSize)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16) // PCM chunk size
            header.putShort(1) // AudioFormat = 1 (PCM)
            header.putShort(1) // Mono
            header.putInt(sampleRate)
            header.putInt(sampleRate * 2)
            header.putShort(2) // BlockAlign
            header.putShort(16) // BitsPerSample
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(dataSize)
            out.write(header.array())

            val pcmBuf = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
            for (s in pcm16k) {
                pcmBuf.putShort(s)
            }
            out.write(pcmBuf.array())
            return out.toByteArray()
        }

        /**
         * 解析 Azure Speech Pronunciation Assessment 返回的 Detailed JSON，
         * 並轉換為符合小學生「護心評分法則」（保底1星、僅綠/橙雙色、單點音素糾音提示）的 [PronunciationReport]
         */
        fun parseAzurePronunciationJson(
            rawJson: String,
            targetWords: List<String>,
            engineLabel: String = "L2-Azure音素级评测"
        ): PronunciationReport? {
            val root = JSONObject(rawJson)
            val status = root.optString("RecognitionStatus", "")
            if (status != "Success") return null

            val nBest = root.optJSONArray("NBest")
            if (nBest == null || nBest.length() == 0) return null
            val best = nBest.getJSONObject(0)

            val accuracy = best.optDouble("AccuracyScore", best.optJSONObject("PronunciationAssessment")?.optDouble("AccuracyScore", 80.0) ?: 80.0)
            val fluency = best.optDouble("FluencyScore", best.optJSONObject("PronunciationAssessment")?.optDouble("FluencyScore", 82.0) ?: 82.0)
            val completeness = best.optDouble("CompletenessScore", best.optJSONObject("PronunciationAssessment")?.optDouble("CompletenessScore", 85.0) ?: 85.0)
            val pronScore = best.optDouble("PronScore", best.optJSONObject("PronunciationAssessment")?.optDouble("PronScore", accuracy) ?: accuracy)

            val wordsArr = best.optJSONArray("Words") ?: JSONArray()
            val parsedWordsMap = mutableMapOf<Int, Triple<Int, String, String?>>() // index -> (score, errorType, weakestPhoneme)

            for (i in 0 until wordsArr.length()) {
                val wObj = wordsArr.getJSONObject(i)
                val wScore = wObj.optDouble(
                    "AccuracyScore",
                    wObj.optJSONObject("PronunciationAssessment")?.optDouble("AccuracyScore", 80.0) ?: 80.0
                ).toInt().coerceIn(55, 100)
                val errorType = wObj.optString(
                    "ErrorType",
                    wObj.optJSONObject("PronunciationAssessment")?.optString("ErrorType", "None") ?: "None"
                )

                var weakestPhoneme: String? = null
                var minPhonemeScore = 101
                val phonemesArr = wObj.optJSONArray("Phonemes")
                if (phonemesArr != null) {
                    for (p in 0 until phonemesArr.length()) {
                        val pObj = phonemesArr.getJSONObject(p)
                        val ph = pObj.optString("Phoneme", "")
                        val pScore = pObj.optDouble(
                            "AccuracyScore",
                            pObj.optJSONObject("PronunciationAssessment")?.optDouble("AccuracyScore", 85.0) ?: 85.0
                        ).toInt()
                        if (ph.isNotBlank() && pScore < minPhonemeScore) {
                            minPhonemeScore = pScore
                            weakestPhoneme = "/$ph/ (${pScore}分)"
                        }
                    }
                }
                parsedWordsMap[i] = Triple(wScore, errorType, weakestPhoneme)
            }

            var weakestPhonemeHint: String? = null
            var weakestWordName: String? = null
            var lowestScoreSeen = 101

            val wordResults = targetWords.mapIndexed { idx, targetWord ->
                val triple = parsedWordsMap[idx]
                val score = triple?.first ?: pronScore.toInt().coerceIn(60, 95)
                val errorType = triple?.second ?: "None"
                val level = if (score >= 76 && errorType != "Omission" && errorType != "Mispronunciation") {
                    WordFeedbackLevel.EXCELLENT
                } else {
                    WordFeedbackLevel.KEEP_TRYING
                }
                if (level == WordFeedbackLevel.KEEP_TRYING && score < lowestScoreSeen) {
                    lowestScoreSeen = score
                    weakestWordName = targetWord
                    weakestPhonemeHint = triple?.third
                }
                WordScoreResult(word = targetWord, score = score, level = level)
            }

            val overall = pronScore.toInt().coerceIn(58, 99)
            val stars = when {
                overall >= 82 -> 3
                overall >= 68 -> 2
                else -> 1 // 護心保底 1 顆星
            }

            val encouragement = when {
                stars == 3 -> "🎉 太惊艳了！音素级发音超标准，解锁 K 歌麦【怪兽/卡通音效】5秒自由搞怪秀！"
                weakestWordName != null && weakestPhonemeHint != null ->
                    "👏 读得很棒！如果把单词「$weakestWordName」里的音标 $weakestPhonemeHint 再读饱满一点就是 3 颗星啦！"
                weakestWordName != null ->
                    "👏 声音真好听！单词「$weakestWordName」再慢速跟读一次就能拿满 3 颗星！"
                else -> "🌱 勇敢开口就是最棒的英语小猎手！再来一次冲击 3 颗星吧！"
            }

            return PronunciationReport(
                stars = stars,
                overallScore = overall,
                completenessScore = completeness.toInt().coerceIn(50, 100),
                fluencyScore = fluency.toInt().coerceIn(55, 100),
                wordResults = wordResults,
                focusWordToRetry = weakestWordName,
                encouragementMessage = encouragement,
                unlockMagicSoundEffect = (stars == 3),
                engineSource = engineLabel
            )
        }

        /**
         * 根據本地聲學包絡能量動態生成一份標準 Azure Speech Pronunciation Assessment 格式的音素級 JSON，
         * 用於開發機離線仿真音素級糾音（例如提示 `/w/`, `/ɔː/`, `/θ/`, `/r/` 等小學常見易錯音標）
         */
        fun simulatePhonemeLevelResponseJson(
            targetWords: List<String>,
            localFallbackReport: PronunciationReport
        ): String {
            val wordsArray = JSONArray()
            for ((idx, wr) in localFallbackReport.wordResults.withIndex()) {
                val clean = wr.word.lowercase().filter { it in 'a'..'z' }
                val phonemes = guessPhonemesForWord(clean)
                val phonemesJson = JSONArray()
                for ((pIdx, ph) in phonemes.withIndex()) {
                    val pScore = if (wr.level == WordFeedbackLevel.KEEP_TRYING && pIdx == 0) {
                        (wr.score - 8).coerceAtLeast(58)
                    } else {
                        (wr.score + 3).coerceAtMost(98)
                    }
                    phonemesJson.put(
                        JSONObject().apply {
                            put("Phoneme", ph)
                            put("AccuracyScore", pScore)
                        }
                    )
                }

                val errType = if (wr.level == WordFeedbackLevel.EXCELLENT) "None" else "Mispronunciation"
                wordsArray.put(
                    JSONObject().apply {
                        put("Word", targetWords.getOrElse(idx) { wr.word })
                        put("AccuracyScore", wr.score)
                        put("ErrorType", errType)
                        put("Phonemes", phonemesJson)
                    }
                )
            }

            val bestObj = JSONObject().apply {
                put("AccuracyScore", localFallbackReport.overallScore)
                put("FluencyScore", localFallbackReport.fluencyScore)
                put("CompletenessScore", localFallbackReport.completenessScore)
                put("PronScore", localFallbackReport.overallScore)
                put("Words", wordsArray)
            }

            return JSONObject().apply {
                put("RecognitionStatus", "Success")
                put("NBest", JSONArray().put(bestObj))
            }.toString()
        }

        private fun guessPhonemesForWord(cleanWord: String): List<String> {
            return when (cleanWord) {
                "water" -> listOf("w", "ɔː", "t", "ər")
                "please" -> listOf("p", "l", "iː", "z")
                "thank" -> listOf("θ", "æ", "ŋ", "k")
                "three" -> listOf("θ", "r", "iː")
                "this" -> listOf("ð", "ɪ", "s")
                "that" -> listOf("ð", "æ", "t")
                "school" -> listOf("s", "k", "uː", "l")
                "apple", "apples" -> listOf("æ", "p", "əl")
                "hello" -> listOf("h", "ə", "l", "oʊ")
                "morning" -> listOf("m", "ɔːr", "n", "ɪŋ")
                "father" -> listOf("f", "ɑː", "ð", "ər")
                "mother" -> listOf("m", "ʌ", "ð", "ər")
                "library" -> listOf("l", "aɪ", "b", "r", "er", "i")
                else -> {
                    val first = cleanWord.take(2).ifEmpty { "ə" }
                    val rest = cleanWord.drop(2).take(2).ifEmpty { "n" }
                    listOf(first, rest)
                }
            }
        }
    }
}
