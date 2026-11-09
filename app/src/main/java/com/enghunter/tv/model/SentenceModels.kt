package com.enghunter.tv.model

/**
 * 標準句式協議（Sentence Card Schema）
 * 統一支持：
 * 1. 本地預置題庫
 * 2. 階段 0 硬件探測與延遲校準測試句
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

data class SentenceCard(
    val id: String,
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
    val rolePlayQuestionZh: String = "你想要点什么？"
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
 * 預置的小學三年級核心開口句式題庫（階段 0 & 階段 1 原型）
 */
object SampleSentenceRepository {
    val builtInSentences: List<SentenceCard> = listOf(
        SentenceCard(
            id = "u01_s01_water",
            unitTitle = "Unit 1 · 礼貌点餐与请求",
            pattern = "Can I have some ___, please?",
            en = "Can I have some water, please?",
            zh = "请给我一些水好吗？",
            emoji = "💧",
            chunks = listOf("Can I have", "some water,", "please?"),
            wordTimingsNormal = listOf(
                WordTiming("Can", 0L, 260L),
                WordTiming("I", 280L, 200L),
                WordTiming("have", 500L, 320L),
                WordTiming("some", 840L, 300L),
                WordTiming("water,", 1160L, 520L),
                WordTiming("please?", 1720L, 540L)
            ),
            wordTimingsSlow = listOf(
                WordTiming("Can", 0L, 380L),
                WordTiming("I", 410L, 300L),
                WordTiming("have", 740L, 450L),
                WordTiming("some", 1220L, 420L),
                WordTiming("water,", 1680L, 740L),
                WordTiming("please?", 2460L, 760L)
            ),
            substitutions = listOf(
                SlotOption(
                    keyword = "some bread",
                    zhHint = "一些面包",
                    emoji = "🍞",
                    fullEn = "Can I have some bread, please?",
                    fullZh = "请给我一些面包好吗？"
                ),
                SlotOption(
                    keyword = "an apple",
                    zhHint = "一个苹果",
                    emoji = "🍎",
                    fullEn = "Can I have an apple, please?",
                    fullZh = "请给我一个苹果好吗？"
                ),
                SlotOption(
                    keyword = "some milk",
                    zhHint = "一些牛奶",
                    emoji = "🥛",
                    fullEn = "Can I have some milk, please?",
                    fullZh = "请给我一些牛奶好吗？"
                )
            ),
            rolePlayQuestion = "Welcome! What would you like?",
            rolePlayQuestionZh = "欢迎光临！你想要点什么？"
        ),
        SentenceCard(
            id = "u01_s02_like",
            unitTitle = "Unit 1 · 表达喜好",
            pattern = "I like ___ very much.",
            en = "I like red apples very much.",
            zh = "我非常喜欢红苹果。",
            emoji = "🍎",
            chunks = listOf("I like", "red apples", "very much."),
            wordTimingsNormal = listOf(
                WordTiming("I", 0L, 240L),
                WordTiming("like", 260L, 340L),
                WordTiming("red", 620L, 340L),
                WordTiming("apples", 980L, 480L),
                WordTiming("very", 1480L, 360L),
                WordTiming("much.", 1860L, 480L)
            ),
            wordTimingsSlow = listOf(
                WordTiming("I", 0L, 360L),
                WordTiming("like", 390L, 480L),
                WordTiming("red", 900L, 480L),
                WordTiming("apples", 1410L, 680L),
                WordTiming("very", 2120L, 500L),
                WordTiming("much.", 2650L, 680L)
            ),
            substitutions = listOf(
                SlotOption(
                    keyword = "cute dogs",
                    zhHint = "可爱的小狗",
                    emoji = "🐶",
                    fullEn = "I like cute dogs very much.",
                    fullZh = "我非常喜欢可爱的小狗。"
                ),
                SlotOption(
                    keyword = "yellow bananas",
                    zhHint = "黄色的香蕉",
                    emoji = "🍌",
                    fullEn = "I like yellow bananas very much.",
                    fullZh = "我非常喜欢黄色的香蕉。"
                )
            ),
            rolePlayQuestion = "What fruit do you like?",
            rolePlayQuestionZh = "你喜欢什么水果？"
        ),
        SentenceCard(
            id = "u02_s01_time",
            unitTitle = "Unit 2 · 日常作息与时间",
            pattern = "It is time to ___.",
            en = "It is time to go to school.",
            zh = "到去上学的时间了。",
            emoji = "🎒",
            chunks = listOf("It is time", "to go", "to school."),
            wordTimingsNormal = listOf(
                WordTiming("It", 0L, 200L),
                WordTiming("is", 220L, 220L),
                WordTiming("time", 460L, 380L),
                WordTiming("to", 860L, 200L),
                WordTiming("go", 1080L, 280L),
                WordTiming("to", 1380L, 200L),
                WordTiming("school.", 1600L, 540L)
            ),
            wordTimingsSlow = listOf(
                WordTiming("It", 0L, 300L),
                WordTiming("is", 320L, 320L),
                WordTiming("time", 660L, 540L),
                WordTiming("to", 1220L, 280L),
                WordTiming("go", 1520L, 400L),
                WordTiming("to", 1940L, 280L),
                WordTiming("school.", 2240L, 760L)
            ),
            substitutions = listOf(
                SlotOption(
                    keyword = "have dinner",
                    zhHint = "吃晚饭",
                    emoji = "🍲",
                    fullEn = "It is time to have dinner.",
                    fullZh = "到吃晚饭的时间了。"
                ),
                SlotOption(
                    keyword = "go to bed",
                    zhHint = "睡觉",
                    emoji = "🛏️",
                    fullEn = "It is time to go to bed.",
                    fullZh = "到睡觉的时间了。"
                )
            ),
            rolePlayQuestion = "Look at the clock! What time is it?",
            rolePlayQuestionZh = "看钟表！现在该做什么啦？"
        )
    )
}
