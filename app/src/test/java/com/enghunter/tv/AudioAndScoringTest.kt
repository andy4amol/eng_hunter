package com.enghunter.tv

import com.enghunter.tv.audio.AntiVocalCancelPlayer
import com.enghunter.tv.model.SampleSentenceRepository
import com.enghunter.tv.scoring.PronunciationScorer
import org.junit.Assert.assertEquals
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
}
