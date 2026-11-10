package com.enghunter.tv.model

import kotlin.math.max

/**
 * 標準句式協議（Sentence Card Schema）
 * 統一支持：
 * 1. 里程碑 M2：預置《小學英語核心 60 句高保真卡拉 OK 資源包》（6 大主題單元 × 每單元 10 句）
 * 2. 離線預烘焙資源包（assets/sentences/sentences_manifest.json + Edge-TTS 音頻）
 * 3. 大模型（DeepSeek / Qwen）動態生成的舉一反三拓展句
 */
data class WordTiming(
    val word: String,
    val startMs: Long,
    val durationMs: Long
) {
    val endMs: Long get() = startMs + durationMs
}

data class SlotOption(
    val keyword: String,
    val zhHint: String,
    val emoji: String,
    val fullEn: String,
    val fullZh: String
)

enum class CurriculumUnit(
    val unitId: String,
    val shortTitle: String,
    val fullTitle: String,
    val emoji: String
) {
    ALL("all", "📚 全部 60 句", "小学英语核心 60 句全集", "🌟"),
    UNIT_1("u01", "U1 礼貌交际", "Unit 1 · 礼貌问候与日常交际 (10句)", "👋"),
    UNIT_2("u02", "U2 点餐请求", "Unit 2 · 美食点餐与礼貌请求 (10句)", "🍔"),
    UNIT_3("u03", "U3 校园文具", "Unit 3 · 校园生活与文具物品 (10句)", "🎒"),
    UNIT_4("u04", "U4 家人爱好", "Unit 4 · 家庭成员与兴趣运动 (10句)", "⚽"),
    UNIT_5("u05", "U5 时间天气", "Unit 5 · 日常作息与天气穿搭 (10句)", "⏰"),
    UNIT_6("u06", "U6 动物出行", "Unit 6 · 动物世界与问路出行 (10句)", "🐼")
}

data class SentenceCard(
    val id: String,
    val unitId: String = "u01",
    val unitTitle: String,
    val pattern: String,
    val en: String,
    val zh: String,
    val emoji: String,
    val chunks: List<String>,
    val wordTimingsNormal: List<WordTiming>,
    val wordTimingsSlow: List<WordTiming>,
    val substitutions: List<SlotOption> = emptyList(),
    val rolePlayQuestion: String = "What would you like?",
    val rolePlayQuestionZh: String = "你想要点什么？",
    val prebakedAudioNormalAsset: String? = null,
    val prebakedAudioSlowAsset: String? = null
)

enum class WordFeedbackLevel {
    EXCELLENT, // 綠色：發音飽滿清晰
    KEEP_TRYING // 橙色：需要再練一下（保護孩子自信心，不使用刺眼紅色）
}

data class WordScoreResult(
    val word: String,
    val score: Int, // 0..100
    val level: WordFeedbackLevel
)

data class PronunciationReport(
    val stars: Int, // 1..3 星（只要開口至少 1 顆星）
    val overallScore: Int,
    val completenessScore: Int,
    val fluencyScore: Int,
    val wordResults: List<WordScoreResult>,
    val focusWordToRetry: String?, // 每次最多只挑 1 個核心詞溫和提示重練
    val encouragementMessage: String,
    val unlockMagicSoundEffect: Boolean, // 拿到 3 星解鎖 XMKGMKF01YM 9種搞怪音效獎勵秀
    val engineSource: String // "L0-VAD+Energy" / "L1-Sherpa" / "L2-Cloud"
)

/**
 * 毫秒級卡拉 OK 時間軸自動推導工具（與 Edge-TTS `en-US-AnaNeural` 少兒語速對齊）
 * 當沒有外部 JSON 覆蓋時，根據單詞音節數、元音簇長度及標點停頓精確生成 WordBoundary
 */
object KaraokeTimingEstimator {

    fun buildTimingsForSentence(sentence: String, slowMode: Boolean): List<WordTiming> {
        val words = sentence.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()

        val speedFactor = if (slowMode) 1.38 else 1.0
        val interWordGapMs = if (slowMode) 45L else 28L
        var cursorMs = 0L
        val list = ArrayList<WordTiming>(words.size)

        for (w in words) {
            val clean = w.lowercase().filter { it in 'a'..'z' }
            val syllables = estimateSyllables(clean)
            val hasPunctuationPause = w.endsWith(",") || w.endsWith(".") || w.endsWith("?") || w.endsWith("!")
            val baseMs = 175L + syllables * 95L + (clean.length * 14L) + (if (hasPunctuationPause) 90L else 0L)
            val durationMs = (baseMs * speedFactor).toLong().coerceAtLeast(180L)
            list.add(WordTiming(word = w, startMs = cursorMs, durationMs = durationMs))
            cursorMs += durationMs + interWordGapMs
        }
        return list
    }

    private fun estimateSyllables(word: String): Int {
        if (word.isEmpty()) return 1
        var count = 0
        var prevVowel = false
        for (ch in word) {
            val isVowel = ch in "aeiouy"
            if (isVowel && !prevVowel) count++
            prevVowel = isVowel
        }
        if (word.endsWith("e") && count > 1) count--
        return max(1, count)
    }
}

/**
 * 里程碑 M2：《小學英語核心 60 句高保真卡拉 OK 資源包》（1~6 年級新課標核心口語句式）
 * 共 6 個單元 × 每單元 10 句 = 60 句完整結構化數據（均含意群拆解、3組看圖換詞、雙麥問答、常速/慢速卡拉OK時間軸）
 */
object SampleSentenceRepository {

    private fun card(
        id: String,
        unit: CurriculumUnit,
        pattern: String,
        en: String,
        zh: String,
        emoji: String,
        chunks: List<String>,
        subs: List<Triple<String, String, String>>, // (keyword, zhHint, emoji) -> 自動拼裝完整替換句
        fullSubPairs: List<Pair<String, String>>,
        qEn: String,
        qZh: String
    ): SentenceCard {
        val slotOptions = subs.mapIndexed { idx, (kw, zhHint, em) ->
            val (fEn, fZh) = fullSubPairs.getOrElse(idx) { en to zh }
            SlotOption(
                keyword = kw,
                zhHint = zhHint,
                emoji = em,
                fullEn = fEn,
                fullZh = fZh
            )
        }
        return SentenceCard(
            id = id,
            unitId = unit.unitId,
            unitTitle = unit.fullTitle,
            pattern = pattern,
            en = en,
            zh = zh,
            emoji = emoji,
            chunks = chunks,
            wordTimingsNormal = KaraokeTimingEstimator.buildTimingsForSentence(en, slowMode = false),
            wordTimingsSlow = KaraokeTimingEstimator.buildTimingsForSentence(en, slowMode = true),
            substitutions = slotOptions,
            rolePlayQuestion = qEn,
            rolePlayQuestionZh = qZh,
            prebakedAudioNormalAsset = "sentences/audio/${id}_normal.wav",
            prebakedAudioSlowAsset = "sentences/audio/${id}_slow.wav"
        )
    }

    val builtInSentences: List<SentenceCard> by lazy {
        PrimarySchool60Catalog.all60Sentences
    }

    fun filterByUnit(unit: CurriculumUnit, allCards: List<SentenceCard> = builtInSentences): List<SentenceCard> {
        if (unit == CurriculumUnit.ALL) return allCards
        return allCards.filter { it.unitId == unit.unitId }
    }
}
