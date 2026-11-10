package com.enghunter.tv

import com.enghunter.tv.audio.AntiVocalCancelPlayer
import com.enghunter.tv.audio.VirtualKaraokeMicTwin
import com.enghunter.tv.model.SampleSentenceRepository
import com.enghunter.tv.model.WordFeedbackLevel
import com.enghunter.tv.scoring.PronunciationScorer
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
}
