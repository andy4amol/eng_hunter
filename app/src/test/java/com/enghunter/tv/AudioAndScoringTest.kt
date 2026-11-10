package com.enghunter.tv

import com.enghunter.tv.audio.AntiVocalCancelPlayer
import com.enghunter.tv.audio.OfflineKaraokePackLoader
import com.enghunter.tv.audio.VirtualKaraokeMicTwin
import com.enghunter.tv.llm.LlmActiveConfig
import com.enghunter.tv.llm.LlmSentenceExpansionEngine
import com.enghunter.tv.llm.LlmVendorPreset
import com.enghunter.tv.model.CurriculumUnit
import com.enghunter.tv.model.PrimarySchool60Catalog
import com.enghunter.tv.model.SampleSentenceRepository
import com.enghunter.tv.model.WordFeedbackLevel
import com.enghunter.tv.scoring.AzureSpeechAssessmentClient
import com.enghunter.tv.scoring.PronunciationScorer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class AudioAndScoringTest {

    @Test
    fun `anti vocal cancel stereo conversion produces asymmetric L and R channels`() {
        // 構造 1000 個採樣點的單聲道正弦波
        val mono = ShortArray(1000) { idx ->
            (sin(idx * 0.1) * 12000).toInt().toShort()
        }
        val stereo = AntiVocalCancelPlayer.convertMonoToAntiVocalCancelStereo(
            monoPcm = mono,
            sampleRate = 16000
        )

        // 立體聲長度應為單聲道的 2 倍
        assertEquals(mono.size * 2, stereo.size)

        // 驗證經過 L - R 相減後不會歸零（防 XMKGMKF01YM 硬件一鍵消原唱抹除老師帶讀聲）
        var residualEnergy = 0L
        for (i in 100 until mono.size) {
            val left = stereo[i * 2].toInt()
            val right = stereo[i * 2 + 1].toInt()
            residualEnergy += kotlin.math.abs(left - right)
        }
        assertTrue("Anti-vocal-cancel L-R residual energy should be strictly positive", residualEnergy > 100_000L)
    }

    @Test
    fun `child friendly pronunciation scorer guarantees at least 1 star when child speaks`() {
        val scorer = PronunciationScorer()
        val card = SampleSentenceRepository.builtInSentences.first()
        val targetWords = card.en.split(" ")

        // 模擬 2 秒有語音包絡的 16kHz PCM
        val pcm = ShortArray(16000 * 2) { idx ->
            val envelope = if ((idx / 2500) % 2 == 0) 6000 else 1800
            (sin(idx * 0.15) * envelope).toInt().toShort()
        }

        val report = scorer.evaluate(card, targetWords, pcm)
        assertTrue("Speaking child should earn at least 1 star", report.stars in 1..3)
        assertEquals(targetWords.size, report.wordResults.size)
        assertNotEquals(0, report.overallScore)
    }

    @Test
    fun `digital twin PERFECT_3_STARS triggers VAD auto stop and unlocks 3-star magic sound effect`() {
        val scorer = PronunciationScorer()
        val card = SampleSentenceRepository.builtInSentences.first()
        val targetWords = card.en.split(" ")

        val rawPcm = VirtualKaraokeMicTwin.synthesizeChildSpeechPcm(
            profile = VirtualKaraokeMicTwin.SimulatedChildVoiceProfile.PERFECT_3_STARS,
            wordCount = targetWords.size
        )
        val vadResult = VirtualKaraokeMicTwin.runVadStreamOverPcm(
            rawPcm = rawPcm,
            sampleRate = 16000
        )

        assertTrue("VAD should auto-stop after trailing silence", vadResult.stoppedByVadSilence)
        assertTrue("Captured duration should be trimmed before raw PCM length", vadResult.pcm16kMono.size < rawPcm.size)

        val report = scorer.evaluate(card, targetWords, vadResult.pcm16kMono)
        assertEquals(3, report.stars)
        assertTrue("3-star result should unlock XMKGMKF01YM 9-mode magic sound effect", report.unlockMagicSoundEffect)
    }

    @Test
    fun `digital twin HESITANT_MISSED_WORD_2_STARS highlights skipped word in gentle orange`() {
        val scorer = PronunciationScorer()
        val card = SampleSentenceRepository.builtInSentences.first()
        val targetWords = card.en.split(" ")

        val rawPcm = VirtualKaraokeMicTwin.synthesizeChildSpeechPcm(
            profile = VirtualKaraokeMicTwin.SimulatedChildVoiceProfile.HESITANT_MISSED_WORD_2_STARS,
            wordCount = targetWords.size
        )
        val vadResult = VirtualKaraokeMicTwin.runVadStreamOverPcm(
            rawPcm = rawPcm,
            sampleRate = 16000
        )
        val report = scorer.evaluate(card, targetWords, vadResult.pcm16kMono)

        assertEquals(2, report.stars)
        assertFalse(report.unlockMagicSoundEffect)
        assertTrue(
            "At least one missed word should be marked KEEP_TRYING",
            report.wordResults.any { it.level == WordFeedbackLevel.KEEP_TRYING }
        )
    }

    @Test
    fun `digital twin SHY_LOW_VOICE_1_STAR still earns 1 star floor protection`() {
        val scorer = PronunciationScorer()
        val card = SampleSentenceRepository.builtInSentences.first()
        val targetWords = card.en.split(" ")

        val rawPcm = VirtualKaraokeMicTwin.synthesizeChildSpeechPcm(
            profile = VirtualKaraokeMicTwin.SimulatedChildVoiceProfile.SHY_LOW_VOICE_1_STAR,
            wordCount = targetWords.size
        )
        val vadResult = VirtualKaraokeMicTwin.runVadStreamOverPcm(
            rawPcm = rawPcm,
            sampleRate = 16000
        )
        val report = scorer.evaluate(card, targetWords, vadResult.pcm16kMono)

        assertTrue("Shy low voice should still get at least 1 star", report.stars >= 1)
    }

    @Test
    fun `digital twin NARROWBAND_8KHZ_CVSD resamples 8000Hz to 16000Hz accurately`() {
        val scorer = PronunciationScorer()
        val card = SampleSentenceRepository.builtInSentences.first()
        val targetWords = card.en.split(" ")

        val raw8k = VirtualKaraokeMicTwin.synthesizeChildSpeechPcm(
            profile = VirtualKaraokeMicTwin.SimulatedChildVoiceProfile.NARROWBAND_8KHZ_CVSD,
            wordCount = targetWords.size
        )
        val vadResult = VirtualKaraokeMicTwin.runVadStreamOverPcm(
            rawPcm = raw8k,
            sampleRate = 8000
        )

        assertEquals(8000, vadResult.rawSampleRate)
        // Resampled 16kHz array should match 16 samples per ms of duration
        val expectedSamples16k = (vadResult.durationMs * 16).toInt()
        assertTrue(kotlin.math.abs(vadResult.pcm16kMono.size - expectedSamples16k) <= 32)

        val report = scorer.evaluate(card, targetWords, vadResult.pcm16kMono)
        assertEquals(3, report.stars)
    }

    @Test
    fun `M2 catalog contains exactly 60 primary school sentences across 6 units with complete karaoke timings`() {
        val all = PrimarySchool60Catalog.all60Sentences
        assertEquals("Catalog must contain exactly 60 core sentences", 60, all.size)

        // 驗證 6 個主題單元，每個單元 10 句
        val units = listOf(
            CurriculumUnit.UNIT_1,
            CurriculumUnit.UNIT_2,
            CurriculumUnit.UNIT_3,
            CurriculumUnit.UNIT_4,
            CurriculumUnit.UNIT_5,
            CurriculumUnit.UNIT_6
        )
        for (unit in units) {
            val filtered = SampleSentenceRepository.filterByUnit(unit, all)
            assertEquals("Unit ${unit.unitId} should have 10 sentences", 10, filtered.size)
        }

        // 驗證每一句的卡拉 OK 逐詞時間軸與單詞數量 100% 對齊，且慢速總時長大於常速總時長
        for (card in all) {
            val words = card.en.trim().split(Regex("\\s+"))
            assertEquals("Normal timings count must match words for ${card.id}", words.size, card.wordTimingsNormal.size)
            assertEquals("Slow timings count must match words for ${card.id}", words.size, card.wordTimingsSlow.size)

            val normalEnd = card.wordTimingsNormal.last().endMs
            val slowEnd = card.wordTimingsSlow.last().endMs
            assertTrue("Slow karaoke duration ($slowEnd) must exceed normal ($normalEnd) for ${card.id}", slowEnd > normalEnd)
            assertTrue("Chunks should not be empty for ${card.id}", card.chunks.isNotEmpty())
            assertTrue("Substitutions should have at least 2 options for ${card.id}", card.substitutions.size >= 2)
        }
    }

    @Test
    fun `M2 manifest JSON file in assets parses all 60 sentences losslessly`() {
        val manifestFile = java.io.File("src/main/assets/sentences/sentences_manifest.json")
            .takeIf { it.exists() }
            ?: java.io.File("app/src/main/assets/sentences/sentences_manifest.json")

        assertTrue("Prebaked sentences_manifest.json must exist in assets", manifestFile.exists())
        val jsonText = manifestFile.readText(Charsets.UTF_8)
        val parsed = OfflineKaraokePackLoader.parseManifestJson(jsonText)

        assertEquals(60, parsed.size)
        assertEquals("u01_s01", parsed.first().id)
        assertEquals("u06_s10", parsed.last().id)

        // Round-trip serialization verification
        val reserialized = OfflineKaraokePackLoader.toManifestJson(parsed)
        val reparsed = OfflineKaraokePackLoader.parseManifestJson(reserialized)
        assertEquals(60, reparsed.size)
    }

    @Test
    fun `M3 Azure phoneme-level JSON parser and MOCK_AZURE_PHONEME_SIMULATOR produce phoneme hints`() = runBlocking {
        val scorer = PronunciationScorer()
        val card = SampleSentenceRepository.builtInSentences.first()
        val targetWords = card.en.split(" ")

        // 使用样本 B（第 3 个单词漏读/卡顿）测试音素级纠音反馈
        val rawPcm = VirtualKaraokeMicTwin.synthesizeChildSpeechPcm(
            profile = VirtualKaraokeMicTwin.SimulatedChildVoiceProfile.HESITANT_MISSED_WORD_2_STARS,
            wordCount = targetWords.size
        )
        val vadResult = VirtualKaraokeMicTwin.runVadStreamOverPcm(rawPcm, 16000)

        val phonemeReport = scorer.evaluateWithStrategy(
            sentence = card,
            targetWords = targetWords,
            pcm16k = vadResult.pcm16kMono,
            mode = PronunciationScorer.ScoringEngineMode.MOCK_AZURE_PHONEME_SIMULATOR
        )

        assertEquals(2, phonemeReport.stars)
        assertTrue(phonemeReport.engineSource.contains("Azure音素级仿真"))
        assertTrue("Encouragement should include phoneme IPA slash notation", phonemeReport.encouragementMessage.contains("/"))
    }

    @Test
    fun `M3 AUTO_CLOUD_WITH_LOCAL_FALLBACK gracefully falls back to L0 when cloud credentials are empty or unreachable`() = runBlocking {
        val scorer = PronunciationScorer()
        val card = SampleSentenceRepository.builtInSentences.first()
        val targetWords = card.en.split(" ")

        val rawPcm = VirtualKaraokeMicTwin.synthesizeChildSpeechPcm(
            profile = VirtualKaraokeMicTwin.SimulatedChildVoiceProfile.PERFECT_3_STARS,
            wordCount = targetWords.size
        )
        val vadResult = VirtualKaraokeMicTwin.runVadStreamOverPcm(rawPcm, 16000)

        // 1. 未配置 Key 时直接走本地 L0
        val reportNoKey = scorer.evaluateWithStrategy(
            sentence = card,
            targetWords = targetWords,
            pcm16k = vadResult.pcm16kMono,
            mode = PronunciationScorer.ScoringEngineMode.AUTO_CLOUD_WITH_LOCAL_FALLBACK,
            credentials = AzureSpeechAssessmentClient.CloudCredentials(subscriptionKey = "", region = "eastasia")
        )
        assertEquals(3, reportNoKey.stars)
        assertTrue(reportNoKey.engineSource.startsWith("L0-本地声学包络评测"))

        // 2. WAV 编码头校验 (RIFF + WAVE)
        val wavBytes = AzureSpeechAssessmentClient.encodePcm16kToWavBytes(vadResult.pcm16kMono, 16000)
        assertEquals(44 + vadResult.pcm16kMono.size * 2, wavBytes.size)
        assertEquals("RIFF", String(wavBytes.copyOfRange(0, 4), Charsets.US_ASCII))
        assertEquals("WAVE", String(wavBytes.copyOfRange(8, 12), Charsets.US_ASCII))
    }

    @Test
    fun `M4 LlmVendorPreset provides 7 preconfigured vendors and parses OpenAI compatible JSON`() = runBlocking {
        // 1. 验证 7 大预置供应商与默认模型齐全
        assertEquals(7, LlmVendorPreset.entries.size)
        val deepseek = LlmVendorPreset.fromId("DEEPSEEK")
        assertEquals("https://api.deepseek.com/v1", deepseek.defaultBaseUrl)
        assertTrue(deepseek.candidateModels.contains("deepseek-chat"))

        val qwen = LlmVendorPreset.fromId("ALIYUN_QWEN")
        assertTrue(qwen.candidateModels.contains("qwen-plus"))

        // 2. 验证 OpenAI / DeepSeek /v1/chat/completions 响应解析（支持带 markdown 代码块包裹的 JSON）
        val seedCard = SampleSentenceRepository.builtInSentences.first()
        val mockOpenAiResponse = """
            {
              "id": "chatcmpl-123",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "```json\n{\n  \"items\": [\n    {\n      \"en\": \"Can I have a mango pudding, please?\",\n      \"zh\": \"请给我一份芒果布丁好吗？\",\n      \"emoji\": \"🍮\",\n      \"chunks\": [\"Can I have\", \"a mango pudding,\", \"please?\"],\n      \"rolePlayQuestion\": \"Welcome! What dessert would you like?\",\n      \"rolePlayQuestionZh\": \"欢迎！你想吃什么甜点？\"\n    }\n  ]\n}\n```"
                  }
                }
              ]
            }
        """.trimIndent()

        val parsedCards = LlmSentenceExpansionEngine.parseOpenAiChatCompletionResponse(
            rawResponse = mockOpenAiResponse,
            seedCard = seedCard
        )
        assertEquals(1, parsedCards.size)
        assertEquals("Can I have a mango pudding, please?", parsedCards.first().en)
        assertEquals(7, parsedCards.first().wordTimingsNormal.size)
        assertEquals(7, parsedCards.first().wordTimingsSlow.size)

        // 3. 验证无 Key 时自动回退到内置离线 AI 魔法裂变引擎，生成 3 句全新拓展句
        val engine = LlmSentenceExpansionEngine()
        val offlineExpansion = engine.expandFromSeedSentence(
            seedCard = seedCard,
            config = LlmActiveConfig(vendor = LlmVendorPreset.DEEPSEEK, apiKey = ""),
            existingSentences = SampleSentenceRepository.builtInSentences
        )
        assertEquals(3, offlineExpansion.generatedCards.size)
    }
}
