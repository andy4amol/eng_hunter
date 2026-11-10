package com.enghunter.tv.scoring

import com.enghunter.tv.model.PronunciationReport
import com.enghunter.tv.model.SentenceCard
import com.enghunter.tv.model.WordFeedbackLevel
import com.enghunter.tv.model.WordScoreResult
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 少兒英語三級可插拔發音評測引擎（PronunciationScorer）
 *
 * 【里程碑 M3】支持四種評測引擎路由模式（[ScoringEngineMode]）：
 * 1. `AUTO_CLOUD_WITH_LOCAL_FALLBACK`（智能混合模式，默認）：
 *    - 若已配置 Azure Speech F0 免費層密鑰，優先發起 2.5s 超時保護的音素級（Phoneme）雲端評測；
 *    - 若未配置或網絡超時，0 報錯無縫回退本地 L0 聲學包絡評測。
 * 2. `MOCK_AZURE_PHONEME_SIMULATOR`（開發機音素級仿真模式）：
 *    - 無需聯網與雲端 Key，利用本地聲學包絡實時生成標準 Azure Detailed JSON（含音標級評分如 `/w/`, `/ɔː/`, `/θ/`），並走完整 Azure 解析器出分。
 * 3. `AZURE_CLOUD_PREFER`（強制優先雲端 Azure F0）：
 *    - 調用微軟 Azure Pronunciation Assessment REST API，失敗時回退 L0 並標註原因。
 * 4. `LOCAL_L0_ENVELOPE`（純離線 L0 聲學包絡評測）：
 *    - 零網絡依賴，基於 XMKGMKF01YM 16mm 心形指向咪芯 16kHz PCM 能量包絡切分單詞打分。
 *
 * 小學生友好的「護心評分法則」：
 * - 只要敢對著麥克風開口說話，保底給 1 顆星！
 * - 不使用刺眼紅色，僅區分「綠色（太棒了）」與「橙色（再試一次會更好）」。
 * - 每次最多只挑 1 個核心單詞（或音標）提示重讀，拿滿 3 顆星即解鎖 XMKGMKF01YM 9種搞怪音效獎勵秀！
 */
class PronunciationScorer(
    private val azureClient: AzureSpeechAssessmentClient = AzureSpeechAssessmentClient()
) {

    enum class ScoringEngineMode(val title: String, val badge: String) {
        AUTO_CLOUD_WITH_LOCAL_FALLBACK(
            title = "智能混合：Azure 音素级优先 + 本地 L0 自动降级",
            badge = "智能混合(Cloud+L0)"
        ),
        MOCK_AZURE_PHONEME_SIMULATOR(
            title = "音素级仿真：离线模拟 Azure 音标纠音 (/θ/ /w/ /r/)",
            badge = "音素仿真(Phoneme)"
        ),
        LOCAL_L0_ENVELOPE(
            title = "纯离线 L0：16mm 咪芯声学包络评测 (零网络)",
            badge = "纯离线(L0包络)"
        )
    }

    /**
     * 異步可插拔評測入口（支持雲端 Azure Speech F0 + 離線音素仿真 + 本地 L0 自動降級）
     */
    suspend fun evaluateWithStrategy(
        sentence: SentenceCard,
        targetWords: List<String>,
        pcm16k: ShortArray,
        mode: ScoringEngineMode = ScoringEngineMode.AUTO_CLOUD_WITH_LOCAL_FALLBACK,
        credentials: AzureSpeechAssessmentClient.CloudCredentials = AzureSpeechAssessmentClient.CloudCredentials()
    ): PronunciationReport {
        val localReport = evaluate(sentence, targetWords, pcm16k)
        if (pcm16k.isEmpty()) return localReport

        return when (mode) {
            ScoringEngineMode.LOCAL_L0_ENVELOPE -> localReport

            ScoringEngineMode.MOCK_AZURE_PHONEME_SIMULATOR -> {
                val simulatedAzureJson = AzureSpeechAssessmentClient.simulatePhonemeLevelResponseJson(
                    targetWords = targetWords,
                    localFallbackReport = localReport
                )
                AzureSpeechAssessmentClient.parseAzurePronunciationJson(
                    rawJson = simulatedAzureJson,
                    targetWords = targetWords,
                    engineLabel = "L2-Azure音素级仿真 (Phoneme Mock)"
                ) ?: localReport
            }

            ScoringEngineMode.AUTO_CLOUD_WITH_LOCAL_FALLBACK -> {
                if (credentials.isConfigured) {
                    val cloudReport = azureClient.evaluateOnCloudOrNull(
                        credentials = credentials,
                        sentence = sentence,
                        targetWords = targetWords,
                        pcm16k = pcm16k
                    )
                    cloudReport ?: localReport.copy(
                        engineSource = "${localReport.engineSource} [云端超时自动降级]"
                    )
                } else {
                    localReport
                }
            }
        }
    }

    /**
     * 同步本地 L0 聲學包絡評測（零延遲、100% 離線可用）
     */
    fun evaluate(
        sentence: SentenceCard,
        targetWords: List<String>,
        pcm16k: ShortArray
    ): PronunciationReport {
        if (pcm16k.isEmpty()) {
            return buildSilentFallback(targetWords)
        }

        // 1. 裁剪前後靜音區，提取孩子的有效發聲區間（Active Speech Span）
        val frameSize = 320 // 20ms @ 16kHz
        val numFrames = pcm16k.size / frameSize
        if (numFrames < 5) {
            return buildSilentFallback(targetWords)
        }

        val frameRms = DoubleArray(numFrames)
        var maxFrameRms = 1.0
        for (f in 0 until numFrames) {
            var sum = 0.0
            val base = f * frameSize
            for (i in 0 until frameSize) {
                val v = pcm16k[base + i].toDouble()
                sum += v * v
            }
            val r = sqrt(sum / frameSize)
            frameRms[f] = r
            if (r > maxFrameRms) maxFrameRms = r
        }

        // 判斷是否根本沒說話（峰值能量過低）
        if (maxFrameRms < 320.0) {
            return PronunciationReport(
                stars = 1,
                overallScore = 55,
                completenessScore = 40,
                fluencyScore = 50,
                wordResults = targetWords.map { WordScoreResult(it, 55, WordFeedbackLevel.KEEP_TRYING) },
                focusWordToRetry = targetWords.firstOrNull(),
                encouragementMessage = "声音有点轻哦～拿起米家 K 歌麦克风靠近嘴巴，大声再试一次吧！🎤",
                unlockMagicSoundEffect = false,
                engineSource = "L0-本地声学包络评测"
            )
        }

        val activeThreshold = (maxFrameRms * 0.16).coerceAtLeast(260.0)
        val firstActiveFrame = frameRms.indexOfFirst { it >= activeThreshold }.coerceAtLeast(0)
        val lastActiveFrame = frameRms.indexOfLast { it >= activeThreshold }.coerceAtLeast(firstActiveFrame + 1)
        val activeFramesCount = (lastActiveFrame - firstActiveFrame + 1).coerceAtLeast(1)
        val activeDurationMs = activeFramesCount * 20L

        // 計算預期句子時長（每個詞約 330ms）
        val expectedDurationMs = (targetWords.size * 340L).coerceAtLeast(900L)
        val durationRatio = (activeDurationMs.toDouble() / expectedDurationMs.toDouble()).coerceIn(0.35, 1.45)

        // 2. 將有效發聲區間按單詞長度比例切分，檢測每個單詞時間窗內的發音飽滿度
        val totalChars = targetWords.sumOf { it.length.coerceAtLeast(2) }.coerceAtLeast(1)
        var cursorFrame = firstActiveFrame
        val wordResults = mutableListOf<WordScoreResult>()

        for ((idx, w) in targetWords.withIndex()) {
            val weight = w.length.coerceAtLeast(2).toDouble() / totalChars
            val spanFrames = (activeFramesCount * weight).toInt().coerceAtLeast(2)
            val startF = cursorFrame.coerceIn(0, numFrames - 1)
            val endF = if (idx == targetWords.lastIndex) {
                lastActiveFrame.coerceIn(startF + 1, numFrames)
            } else {
                (startF + spanFrames).coerceIn(startF + 1, numFrames)
            }
            cursorFrame = endF

            var voicedFramesInWord = 0
            var wordEnergySum = 0.0
            for (f in startF until endF) {
                wordEnergySum += frameRms[f]
                if (frameRms[f] >= activeThreshold * 0.85) {
                    voicedFramesInWord++
                }
            }
            val wordSpan = (endF - startF).coerceAtLeast(1)
            val voicedRatio = voicedFramesInWord.toDouble() / wordSpan
            val avgWordEnergy = wordEnergySum / wordSpan
            val energyNorm = (avgWordEnergy / (maxFrameRms * 0.56)).coerceIn(0.3, 1.10)

            val rawWordScore = ((voicedRatio * 58.0 + energyNorm * 34.0) * min(1.06, durationRatio + 0.22))
                .toInt()
                .coerceIn(58, 99)

            val level = if (rawWordScore >= 76) {
                WordFeedbackLevel.EXCELLENT
            } else {
                WordFeedbackLevel.KEEP_TRYING
            }
            wordResults.add(WordScoreResult(word = w, score = rawWordScore, level = level))
        }

        val avgWordScore = wordResults.map { it.score }.average().toInt()
        val completeness = ((activeDurationMs.toDouble() / expectedDurationMs) * 100.0)
            .toInt()
            .coerceIn(60, 100)
        val fluency = (100 - abs(1.0 - durationRatio) * 35.0).toInt().coerceIn(65, 99)
        val overall = ((avgWordScore * 0.6) + (completeness * 0.25) + (fluency * 0.15)).toInt().coerceIn(60, 99)

        val stars = when {
            overall >= 82 -> 3
            overall >= 68 -> 2
            else -> 1
        }

        // 只挑出分數最低且未達優秀的 1 個核心詞（不讓滿屏報錯打擊孩子）
        val lowestWord = wordResults
            .filter { it.level == WordFeedbackLevel.KEEP_TRYING }
            .minByOrNull { it.score }
            ?.word
            ?.replace(Regex("[^a-zA-Z']"), "")

        val message = when (stars) {
            3 -> "🌟 太棒啦！声音洪亮又流利，拿下 3 颗星！快按麦克风切个【卡通/怪兽音效】搞怪读一遍吧！"
            2 -> if (lowestWord != null) {
                "👏 读得很不错！如果把「$lowestWord」读得再饱满一点就能拿满 3 颗星啦！"
            } else {
                "👏 读得很棒！再来一次冲击 3 颗星吧！"
            }
            else -> "💪 勇敢开口奖励 1 颗星！跟着慢速带读再试一遍，你一定行！"
        }

        return PronunciationReport(
            stars = stars,
            overallScore = overall,
            completenessScore = completeness,
            fluencyScore = fluency,
            wordResults = wordResults,
            focusWordToRetry = lowestWord,
            encouragementMessage = message,
            unlockMagicSoundEffect = stars == 3,
            engineSource = "L0-本地声学包络评测 (XMKGMKF01YM 16mm咪芯优化)"
        )
    }

    private fun buildSilentFallback(targetWords: List<String>): PronunciationReport {
        return PronunciationReport(
            stars = 1,
            overallScore = 50,
            completenessScore = 30,
            fluencyScore = 50,
            wordResults = targetWords.map { WordScoreResult(it, 50, WordFeedbackLevel.KEEP_TRYING) },
            focusWordToRetry = targetWords.firstOrNull(),
            encouragementMessage = "还没有听到你的声音哦～请确认 K 歌麦克风已开机并调到【原声】模式！",
            unlockMagicSoundEffect = false,
            engineSource = "L0-静音检测"
        )
    }
}
