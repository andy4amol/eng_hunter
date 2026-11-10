package com.enghunter.tv.audio

import android.content.Context
import com.enghunter.tv.model.KaraokeTimingEstimator
import com.enghunter.tv.model.PrimarySchool60Catalog
import com.enghunter.tv.model.SentenceCard
import com.enghunter.tv.model.SlotOption
import com.enghunter.tv.model.WordTiming
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 【里程碑 M2】《小學英語核心 60 句高保真卡拉 OK 資源包》加載與序列化引擎
 *
 * 支持三種資源層級的無縫融合：
 * 1. 內置 60 句結構化數據集（[PrimarySchool60Catalog.all60Sentences]）
 * 2. `assets/sentences/sentences_manifest.json` 離線預烘焙清單（含 Edge-TTS WordBoundary 精確毫秒時間戳）
 * 3. `assets/sentences/audio/{id}_{normal|slow}.wav` 預烘焙語音文件（自動拷貝至緩存供 [AntiVocalCancelPlayer] 播放）
 */
object OfflineKaraokePackLoader {

    const val MANIFEST_ASSET_PATH = "sentences/sentences_manifest.json"

    /**
     * 從 assets 加載 60 句資源包；若存在預烘焙的 `sentences_manifest.json` 則優先使用其精確時間軸，
     * 否則回退到 [PrimarySchool60Catalog.all60Sentences]。
     */
    fun loadSentenceCatalog(context: Context): List<SentenceCard> {
        return try {
            val jsonText = context.assets.open(MANIFEST_ASSET_PATH).bufferedReader().use { it.readText() }
            val parsed = parseManifestJson(jsonText)
            if (parsed.isNotEmpty()) parsed else PrimarySchool60Catalog.all60Sentences
        } catch (_: Exception) {
            PrimarySchool60Catalog.all60Sentences
        }
    }

    /**
     * 檢查是否存在預烘焙的常速/慢速 WAV 資產，若存在則提取到本地 cacheDir 供播放器直接讀取
     */
    fun resolvePrebakedWavIfPresent(
        context: Context,
        card: SentenceCard,
        slowMode: Boolean
    ): File? {
        val assetPath = if (slowMode) card.prebakedAudioSlowAsset else card.prebakedAudioNormalAsset
        if (assetPath.isNullOrBlank()) return null
        return try {
            val outDir = File(context.cacheDir, "prebaked_wav").apply { mkdirs() }
            val outFile = File(outDir, assetPath.substringAfterLast('/'))
            if (outFile.exists() && outFile.length() > 128L) {
                return outFile
            }
            context.assets.open(assetPath).use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            outFile.takeIf { it.exists() && it.length() > 128L }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 將任意 SentenceCard 列表序列化為標準 JSON 清單（可供離線烘焙腳本與單元測試驗證）
     */
    fun toManifestJson(cards: List<SentenceCard>): String {
        val root = JSONObject()
        root.put("schemaVersion", "enghunter.karaoke.pack.v2")
        root.put("voice", "en-US-AnaNeural")
        root.put("totalCount", cards.size)

        val arr = JSONArray()
        for (c in cards) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("unitId", c.unitId)
            obj.put("unitTitle", c.unitTitle)
            obj.put("pattern", c.pattern)
            obj.put("en", c.en)
            obj.put("zh", c.zh)
            obj.put("emoji", c.emoji)
            obj.put("rolePlayQuestion", c.rolePlayQuestion)
            obj.put("rolePlayQuestionZh", c.rolePlayQuestionZh)
            obj.put("prebakedAudioNormalAsset", c.prebakedAudioNormalAsset ?: "sentences/audio/${c.id}_normal.wav")
            obj.put("prebakedAudioSlowAsset", c.prebakedAudioSlowAsset ?: "sentences/audio/${c.id}_slow.wav")

            val chunksArr = JSONArray()
            c.chunks.forEach { chunksArr.put(it) }
            obj.put("chunks", chunksArr)

            obj.put("wordTimingsNormal", serializeTimings(c.wordTimingsNormal))
            obj.put("wordTimingsSlow", serializeTimings(c.wordTimingsSlow))

            val subsArr = JSONArray()
            c.substitutions.forEach { sub ->
                val sObj = JSONObject()
                sObj.put("keyword", sub.keyword)
                sObj.put("zhHint", sub.zhHint)
                sObj.put("emoji", sub.emoji)
                sObj.put("fullEn", sub.fullEn)
                sObj.put("fullZh", sub.fullZh)
                subsArr.put(sObj)
            }
            obj.put("substitutions", subsArr)
            arr.put(obj)
        }
        root.put("sentences", arr)
        return root.toString(2)
    }

    /**
     * 從 JSON 字符串反序列化為 SentenceCard 列表
     */
    fun parseManifestJson(jsonText: String): List<SentenceCard> {
        val root = JSONObject(jsonText)
        val arr = root.optJSONArray("sentences") ?: return emptyList()
        val result = ArrayList<SentenceCard>(arr.length())

        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val id = obj.getString("id")
            val en = obj.getString("en")

            val chunksArr = obj.optJSONArray("chunks")
            val chunks = if (chunksArr != null && chunksArr.length() > 0) {
                List(chunksArr.length()) { idx -> chunksArr.getString(idx) }
            } else {
                listOf(en)
            }

            val normalTimings = parseTimings(obj.optJSONArray("wordTimingsNormal"))
                .ifEmpty { KaraokeTimingEstimator.buildTimingsForSentence(en, slowMode = false) }
            val slowTimings = parseTimings(obj.optJSONArray("wordTimingsSlow"))
                .ifEmpty { KaraokeTimingEstimator.buildTimingsForSentence(en, slowMode = true) }

            val subsArr = obj.optJSONArray("substitutions")
            val subs = if (subsArr != null) {
                List(subsArr.length()) { idx ->
                    val s = subsArr.getJSONObject(idx)
                    SlotOption(
                        keyword = s.getString("keyword"),
                        zhHint = s.optString("zhHint", ""),
                        emoji = s.optString("emoji", "✨"),
                        fullEn = s.optString("fullEn", en),
                        fullZh = s.optString("fullZh", "")
                    )
                }
            } else emptyList()

            result.add(
                SentenceCard(
                    id = id,
                    unitId = obj.optString("unitId", id.substringBefore('_')),
                    unitTitle = obj.optString("unitTitle", "Unit"),
                    pattern = obj.optString("pattern", en),
                    en = en,
                    zh = obj.optString("zh", ""),
                    emoji = obj.optString("emoji", "🌟"),
                    chunks = chunks,
                    wordTimingsNormal = normalTimings,
                    wordTimingsSlow = slowTimings,
                    substitutions = subs,
                    rolePlayQuestion = obj.optString("rolePlayQuestion", "What would you like?"),
                    rolePlayQuestionZh = obj.optString("rolePlayQuestionZh", "你想要点什么？"),
                    prebakedAudioNormalAsset = obj.optString("prebakedAudioNormalAsset", "sentences/audio/${id}_normal.wav"),
                    prebakedAudioSlowAsset = obj.optString("prebakedAudioSlowAsset", "sentences/audio/${id}_slow.wav")
                )
            )
        }
        return result
    }

    private fun serializeTimings(timings: List<WordTiming>): JSONArray {
        val arr = JSONArray()
        for (t in timings) {
            val o = JSONObject()
            o.put("word", t.word)
            o.put("startMs", t.startMs)
            o.put("durationMs", t.durationMs)
            arr.put(o)
        }
        return arr
    }

    private fun parseTimings(arr: JSONArray?): List<WordTiming> {
        if (arr == null || arr.length() == 0) return emptyList()
        return List(arr.length()) { idx ->
            val o = arr.getJSONObject(idx)
            WordTiming(
                word = o.getString("word"),
                startMs = o.getLong("startMs"),
                durationMs = o.getLong("durationMs")
            )
        }
    }
}
