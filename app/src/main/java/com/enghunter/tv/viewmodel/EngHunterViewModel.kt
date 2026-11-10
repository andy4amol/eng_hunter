package com.enghunter.tv.viewmodel

import android.app.Application
import android.content.Intent
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enghunter.tv.audio.AntiVocalCancelPlayer
import com.enghunter.tv.audio.OfflineKaraokePackLoader
import com.enghunter.tv.audio.RingBufferAudioCache
import com.enghunter.tv.audio.VirtualKaraokeMicTwin
import com.enghunter.tv.audio.XiaomiKaraokeMicManager
import com.enghunter.tv.llm.LlmActiveConfig
import com.enghunter.tv.llm.LlmConfigStore
import com.enghunter.tv.llm.LlmSentenceExpansionEngine
import com.enghunter.tv.llm.LlmVendorPreset
import com.enghunter.tv.llm.PersistedExpansionStore
import com.enghunter.tv.model.CurriculumUnit
import com.enghunter.tv.model.PronunciationReport
import com.enghunter.tv.model.SampleSentenceRepository
import com.enghunter.tv.model.SentenceCard
import com.enghunter.tv.scoring.AzureSpeechAssessmentClient
import com.enghunter.tv.scoring.PronunciationScorer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ActiveTab {
    PRACTICE_PROTOTYPE, // 小學生單句開口跟讀原型（模式 A 全無線閉環）
    PHASE0_DIAGNOSTIC,  // 階段 0：小米電視 & XMKGMKF01YM 麥克風硬件探測台
    AI_MODEL_SETTINGS   // 里程碑 M4：大模型供應商與模型靈活配置中心
}

enum class PracticeStage(val label: String) {
    READY("准备开始"),
    PLAYING_STANDARD_NORMAL("🔊 正在播放外教常速带读 (A2DP 双5W喇叭)"),
    PLAYING_STANDARD_SLOW("🐢 正在播放外教慢速拆解带读 (0.8x)"),
    SWITCHING_TO_HFP("⚡ 正在唤醒 K歌麦 16mm 咪芯无线通道 (HFP)..."),
    RECORDING_CHILD("🎤 轮到你啦！请对着 K歌麦克风大声读（说完自动停）"),
    SCORING("✨ 正在评测你的发音..."),
    FEEDBACK("🏆 跟读完成！查看星星与发音反馈"),
    PLAYING_COMPARISON("🔁 正在对比回放：先听标准音 ➔ 再听你的声音"),
    MAGIC_SOUND_SHOW("🎭 三星奖励：切换麦克风【怪兽/卡通音效】自由搞怪配音！"),
    EXPANDING_WITH_AI("🪄 AI 老师正在为你生成 3 句全新魔法挑战句...")
}

enum class PracticeSubMode(val title: String) {
    FOLLOW_READ("① 标准整句跟读"),
    CHUNK_READ("② 意群分段拆解"),
    SLOT_SUBSTITUTE("③ 看图换词造句"),
    DUAL_MIC_ROLE_PLAY("④ 双麦对唱问答 (2支XMKGMKF01YM)")
}

data class EngHunterUiState(
    val activeTab: ActiveTab = ActiveTab.PRACTICE_PROTOTYPE,
    val hasRecordPermission: Boolean = false,
    val selectedInputMode: XiaomiKaraokeMicManager.PreferredInputMode =
        XiaomiKaraokeMicManager.PreferredInputMode.MODE_A_WIRELESS_HFP,
    val selectedSimulatedProfile: VirtualKaraokeMicTwin.SimulatedChildVoiceProfile =
        VirtualKaraokeMicTwin.SimulatedChildVoiceProfile.PERFECT_3_STARS,
    val selectedScoringEngineMode: PronunciationScorer.ScoringEngineMode =
        PronunciationScorer.ScoringEngineMode.AUTO_CLOUD_WITH_LOCAL_FALLBACK,
    val azureSubscriptionKey: String = "",
    val azureRegion: String = "eastasia",
    val llmConfig: LlmActiveConfig = LlmActiveConfig(),
    val aiStatusMessage: String = "支持 7 大主流供应商一键切换，也可通过 ADB 命令一行设置 API Key",
    val isExpandingAi: Boolean = false,
    val aiExpandedCount: Int = 0,
    val enableAntiVocalCancel: Boolean = true,
    val bluetoothLatencyOffsetMs: Long = 180L,
    val autoFlowHandsFree: Boolean = true, // 模式 A 核心：播完自動叮一聲開麥，說完 VAD 自動停

    // 階段 0 硬件探測數據
    val inputDevices: List<XiaomiKaraokeMicManager.AudioEndpointItem> = emptyList(),
    val outputDevices: List<XiaomiKaraokeMicManager.AudioEndpointItem> = emptyList(),
    val diagnosticStatusText: String = "等待测试：请开启米家 K 歌麦克风 (XMKGMKF01YM) 并连接小米电视蓝牙",
    val lastKeyEventInfo: String = "按下小米遥控器任意按键或语音键可在此查看 KeyCode",
    val isRunningProbe: Boolean = false,
    val liveMicLevel: Float = 0f,
    val livePeakRaw: Int = 0,
    val liveRoutedDevice: String = "未开始采集",
    val liveSampleRate: Int = 16000,
    val lastSavedRecording: RingBufferAudioCache.SavedRecording? = null,
    val ringCacheCount: Int = 0,
    val ringCacheBytes: Long = 0L,

    // 單句跟讀閉環狀態（里程碑 M2：支持 6 大主題單元 × 60 句核心卡拉 OK 題庫篩選）
    val allCatalogSentences: List<SentenceCard> = SampleSentenceRepository.builtInSentences,
    val selectedUnit: CurriculumUnit = CurriculumUnit.ALL,
    val sentences: List<SentenceCard> = SampleSentenceRepository.builtInSentences,
    val currentSentenceIndex: Int = 0,
    val practiceStage: PracticeStage = PracticeStage.READY,
    val practiceSubMode: PracticeSubMode = PracticeSubMode.FOLLOW_READ,
    val selectedSlotIndex: Int = 0,
    val activeChunkIndex: Int = 0,
    val highlightedWordIndex: Int = -1,
    val recordingElapsedMs: Long = 0L,
    val latestReport: PronunciationReport? = null,
    val totalStarsEarned: Int = 0
) {
    val currentSentence: SentenceCard
        get() = sentences.getOrElse(currentSentenceIndex) { sentences.first() }

    /**
     * 當前子模式下孩子實際需要朗讀的英文句子
     */
    val activeTargetEnglish: String
        get() = when (practiceSubMode) {
            PracticeSubMode.FOLLOW_READ -> currentSentence.en
            PracticeSubMode.CHUNK_READ -> currentSentence.chunks.getOrElse(activeChunkIndex) { currentSentence.en }
            PracticeSubMode.SLOT_SUBSTITUTE -> {
                currentSentence.substitutions.getOrNull(selectedSlotIndex)?.fullEn ?: currentSentence.en
            }
            PracticeSubMode.DUAL_MIC_ROLE_PLAY -> currentSentence.en
        }

    val activeTargetChinese: String
        get() = when (practiceSubMode) {
            PracticeSubMode.FOLLOW_READ -> currentSentence.zh
            PracticeSubMode.CHUNK_READ -> "分段练习 (${activeChunkIndex + 1}/${currentSentence.chunks.size})：${currentSentence.zh}"
            PracticeSubMode.SLOT_SUBSTITUTE -> {
                currentSentence.substitutions.getOrNull(selectedSlotIndex)?.fullZh ?: currentSentence.zh
            }
            PracticeSubMode.DUAL_MIC_ROLE_PLAY -> "【1号麦提问】${currentSentence.rolePlayQuestion} ➔ 【2号麦回答】${currentSentence.en}"
        }

    val activeEmoji: String
        get() = when (practiceSubMode) {
            PracticeSubMode.SLOT_SUBSTITUTE -> {
                currentSentence.substitutions.getOrNull(selectedSlotIndex)?.emoji ?: currentSentence.emoji
            }
            else -> currentSentence.emoji
        }

    val activeWordsList: List<String>
        get() = activeTargetEnglish.split(" ").filter { it.isNotBlank() }
}

class EngHunterViewModel(application: Application) : AndroidViewModel(application) {

    private val micManager = XiaomiKaraokeMicManager(application)
    private val player = AntiVocalCancelPlayer(application)
    private val ringCache = RingBufferAudioCache(application, maxRingFiles = 20)
    private val scorer = PronunciationScorer()
    private val llmConfigStore = LlmConfigStore(application)
    private val llmExpansionEngine = LlmSentenceExpansionEngine()
    private val persistedExpansionStore = PersistedExpansionStore(application)

    private val _uiState = MutableStateFlow(EngHunterUiState())
    val uiState: StateFlow<EngHunterUiState> = _uiState.asStateFlow()

    private var activeTaskJob: Job? = null

    init {
        val baseCatalog = OfflineKaraokePackLoader.loadSentenceCatalog(application)
        val savedExpansions = persistedExpansionStore.loadSavedExpansions()
        val combinedCatalog = mergeCatalogWithExpansions(baseCatalog, savedExpansions)
        val savedLlmConfig = llmConfigStore.load()

        _uiState.update {
            it.copy(
                llmConfig = savedLlmConfig,
                aiExpandedCount = savedExpansions.size,
                allCatalogSentences = combinedCatalog,
                sentences = SampleSentenceRepository.filterByUnit(it.selectedUnit, combinedCatalog),
                currentSentenceIndex = 0
            )
        }
        refreshAudioDevices()
    }

    private fun mergeCatalogWithExpansions(
        base: List<SentenceCard>,
        expansions: List<SentenceCard>
    ): List<SentenceCard> {
        val seenEn = base.map { it.en.lowercase() }.toMutableSet()
        val extra = expansions.filter { seenEn.add(it.en.lowercase()) }
        return base + extra
    }

    fun selectUnit(unit: CurriculumUnit) {
        cancelActiveJob()
        _uiState.update { state ->
            val filtered = SampleSentenceRepository.filterByUnit(unit, state.allCatalogSentences)
            state.copy(
                selectedUnit = unit,
                sentences = filtered.ifEmpty { state.allCatalogSentences },
                currentSentenceIndex = 0,
                selectedSlotIndex = 0,
                activeChunkIndex = 0,
                practiceStage = PracticeStage.READY,
                highlightedWordIndex = -1,
                latestReport = null
            )
        }
    }

    fun updatePermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(hasRecordPermission = granted) }
        refreshAudioDevices()
    }

    fun switchTab(tab: ActiveTab) {
        cancelActiveJob()
        refreshAudioDevices()
        _uiState.update { it.copy(activeTab = tab, practiceStage = PracticeStage.READY) }
    }

    fun selectInputMode(mode: XiaomiKaraokeMicManager.PreferredInputMode) {
        _uiState.update { it.copy(selectedInputMode = mode) }
        refreshAudioDevices()
    }

    fun selectSimulatedProfile(profile: VirtualKaraokeMicTwin.SimulatedChildVoiceProfile) {
        _uiState.update {
            it.copy(
                selectedSimulatedProfile = profile,
                selectedInputMode = XiaomiKaraokeMicManager.PreferredInputMode.MODE_DEV_SIMULATOR
            )
        }
    }

    fun selectScoringEngineMode(mode: PronunciationScorer.ScoringEngineMode) {
        _uiState.update { it.copy(selectedScoringEngineMode = mode) }
    }

    fun updateAzureCredentials(key: String, region: String) {
        _uiState.update {
            it.copy(
                azureSubscriptionKey = key.trim(),
                azureRegion = region.trim().ifEmpty { "eastasia" }
            )
        }
    }

    // =========================================================================
    // 里程碑 M4：大模型供應商與模型靈活配置 + AI 魔法舉一反三生題
    // =========================================================================

    fun selectLlmVendor(vendor: LlmVendorPreset) {
        _uiState.update { state ->
            val updated = state.llmConfig.copy(
                vendor = vendor,
                baseUrl = vendor.defaultBaseUrl,
                modelName = vendor.candidateModels.first()
            )
            llmConfigStore.save(updated)
            state.copy(
                llmConfig = updated,
                aiStatusMessage = "✅ 已切换供应商为【${vendor.displayName}】，默认模型：${updated.modelName}"
            )
        }
    }

    fun selectLlmModel(modelName: String) {
        _uiState.update { state ->
            val updated = state.llmConfig.copy(modelName = modelName.trim())
            llmConfigStore.save(updated)
            state.copy(
                llmConfig = updated,
                aiStatusMessage = "✅ 已切换模型为：$modelName"
            )
        }
    }

    fun updateLlmCustomEndpointAndKey(baseUrl: String, modelName: String, apiKey: String) {
        _uiState.update { state ->
            val updated = state.llmConfig.copy(
                baseUrl = baseUrl.trim().ifEmpty { state.llmConfig.vendor.defaultBaseUrl },
                modelName = modelName.trim().ifEmpty { state.llmConfig.vendor.candidateModels.first() },
                apiKey = apiKey.trim()
            )
            llmConfigStore.save(updated)
            state.copy(
                llmConfig = updated,
                aiStatusMessage = "✅ 已保存自定义配置：${updated.toSummaryBadge()} | Key: ${updated.maskedApiKey}"
            )
        }
    }

    fun toggleLlmAutoFallback() {
        _uiState.update { state ->
            val updated = state.llmConfig.copy(autoFallbackToOffline = !state.llmConfig.autoFallbackToOffline)
            llmConfigStore.save(updated)
            state.copy(llmConfig = updated)
        }
    }

    /**
     * 接收來自 ADB 廣播或啟動 Intent 的一鍵配置命令
     */
    fun applyAdbLlmConfigIntent(intent: Intent?) {
        val merged = LlmConfigStore.mergeFromIntent(_uiState.value.llmConfig, intent) ?: return
        llmConfigStore.save(merged)
        _uiState.update {
            it.copy(
                llmConfig = merged,
                aiStatusMessage = "📡 已通过 ADB 命令动态更新配置：${merged.toSummaryBadge()} (${merged.baseUrl})"
            )
        }
    }

    /**
     * 一鍵觸發「✨ AI 魔法挑戰」：基於當前句式動態生成 3 句全新拓展句，並自動跳轉到第一句新題
     */
    fun triggerAiMagicExpansion() {
        cancelActiveJob()
        activeTaskJob = viewModelScope.launch {
            val seedCard = _uiState.value.currentSentence
            val config = _uiState.value.llmConfig
            _uiState.update {
                it.copy(
                    isExpandingAi = true,
                    practiceStage = PracticeStage.EXPANDING_WITH_AI,
                    aiStatusMessage = "🪄 正在通过 ${config.toSummaryBadge()} 为「${seedCard.pattern}」生成 3 句新挑战..."
                )
            }

            val result = llmExpansionEngine.expandFromSeedSentence(
                seedCard = seedCard,
                config = config,
                existingSentences = _uiState.value.allCatalogSentences
            )

            if (result.generatedCards.isNotEmpty()) {
                val allSavedExpansions = persistedExpansionStore.appendAndSaveExpansions(result.generatedCards)
                val baseCatalog = OfflineKaraokePackLoader.loadSentenceCatalog(getApplication())
                val mergedAll = mergeCatalogWithExpansions(baseCatalog, allSavedExpansions)
                val firstNewId = result.generatedCards.first().id

                _uiState.update { state ->
                    val filtered = SampleSentenceRepository.filterByUnit(state.selectedUnit, mergedAll)
                    val jumpIdx = filtered.indexOfFirst { it.id == firstNewId }.coerceAtLeast(0)
                    state.copy(
                        isExpandingAi = false,
                        practiceStage = PracticeStage.READY,
                        allCatalogSentences = mergedAll,
                        sentences = filtered,
                        currentSentenceIndex = jumpIdx,
                        aiExpandedCount = allSavedExpansions.size,
                        aiStatusMessage = result.statusMessage,
                        latestReport = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isExpandingAi = false,
                        practiceStage = PracticeStage.READY,
                        aiStatusMessage = result.statusMessage
                    )
                }
            }
        }
    }

    /**
     * 在單句跟讀界面一鍵觸發指定數字孿生畫像的跟讀與評測（無需真機麥克風權限）
     */
    fun triggerSimulatedPractice(profile: VirtualKaraokeMicTwin.SimulatedChildVoiceProfile) {
        cancelActiveJob()
        _uiState.update {
            it.copy(
                selectedInputMode = XiaomiKaraokeMicManager.PreferredInputMode.MODE_DEV_SIMULATOR,
                selectedSimulatedProfile = profile
            )
        }
        activeTaskJob = viewModelScope.launch {
            startChildRecordingInternal()
        }
    }

    fun toggleAntiVocalCancel() {
        _uiState.update { it.copy(enableAntiVocalCancel = !it.enableAntiVocalCancel) }
    }

    fun toggleHandsFreeAutoFlow() {
        _uiState.update { it.copy(autoFlowHandsFree = !it.autoFlowHandsFree) }
    }

    fun adjustBluetoothLatency(deltaMs: Long) {
        _uiState.update {
            it.copy(bluetoothLatencyOffsetMs = (it.bluetoothLatencyOffsetMs + deltaMs).coerceIn(0L, 600L))
        }
    }

    fun refreshAudioDevices() {
        val (inputs, outputs) = micManager.scanAudioEndpoints()
        val files = ringCache.getRingFilesInfo()
        _uiState.update {
            it.copy(
                inputDevices = inputs,
                outputDevices = outputs,
                ringCacheCount = files.size,
                ringCacheBytes = ringCache.getTotalCacheSizeBytes()
            )
        }
    }

    /**
     * 記錄遙控器按鍵事件，用於階段 0 驗證小米遙控器按鍵與語音鍵 KeyCode
     */
    fun onRemoteKeyIntercepted(keyCode: Int, actionName: String): Boolean {
        val keyName = KeyEvent.keyCodeToString(keyCode)
        _uiState.update {
            it.copy(lastKeyEventInfo = "$actionName : $keyName (keyCode=$keyCode)")
        }
        return false
    }

    // =========================================================================
    // 階段 0：硬件探測台專屬測試動作
    // =========================================================================

    /**
     * 測試 1：模式 A 全無線 HFP 5秒拾音 + 自動切回 A2DP 雙 5W 喇叭回放
     */
    fun runPhase0WirelessHfpLoopbackTest() {
        cancelActiveJob()
        activeTaskJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRunningProbe = true,
                    diagnosticStatusText = "① 正在切换 XMKGMKF01YM 到 HFP 无线收音模式，请对着麦克风说话（最长 5 秒，说完自动停）..."
                )
            }
            player.playReadyToSpeakChime()

            val result = micManager.recordWithAutoVad(
                mode = _uiState.value.selectedInputMode,
                maxDurationMs = 5000L,
                enableVadAutoStop = true,
                simulatedProfile = _uiState.value.selectedSimulatedProfile,
                expectedWordCount = _uiState.value.activeWordsList.size
            ) { frame ->
                _uiState.update {
                    it.copy(
                        liveMicLevel = frame.currentRms,
                        livePeakRaw = frame.peakRaw,
                        liveRoutedDevice = frame.activeDeviceSummary,
                        liveSampleRate = frame.actualSampleRate,
                        recordingElapsedMs = frame.elapsedMs
                    )
                }
            }

            val saved = ringCache.saveToRingBuffer(result.pcm16kMono, 16000)
            refreshAudioDevices()

            _uiState.update {
                it.copy(
                    lastSavedRecording = saved,
                    liveMicLevel = 0f,
                    diagnosticStatusText = buildString {
                        appendLine("✅ 采集完成！设备：${result.deviceNameUsed}")
                        appendLine("• 原始采样率：${result.rawSampleRate}Hz ➔ 标准化 16000Hz | 时长：${result.durationMs}ms")
                        appendLine("• 峰值振幅：${result.peakAmplitude} | 平均 RMS：${result.averageRms.toInt()} | VAD自动截断：${result.stoppedByVadSilence}")
                        appendLine("② 正在通过 A2DP 双 5W 扬声器回放刚才的录音（已开启防消原唱保护）...")
                        append(result.diagnosticLog)
                    }
                )
            }

            player.playWavFile(saved.file, enableAntiVocalCancel = _uiState.value.enableAntiVocalCancel)

            _uiState.update {
                it.copy(
                    isRunningProbe = false,
                    diagnosticStatusText = it.diagnosticStatusText + "\n🎉 闭环测试完成！如果刚才从麦克风喇叭听到了你自己的声音，说明【模式 A 全无线闭环】100% 跑通！"
                )
            }
        }
    }

    /**
     * 測試 2：防消原唱（Anti-Vocal-Cancel）與卡拉 OK 逐詞高亮延遲校準測試
     */
    fun runPhase0AntiVocalCancelKaraokeTest() {
        cancelActiveJob()
        activeTaskJob = viewModelScope.launch {
            val card = _uiState.value.currentSentence
            _uiState.update {
                it.copy(
                    isRunningProbe = true,
                    diagnosticStatusText = "🔊 正在播放防消原唱测试句（可长按 XMKGMKF01YM 电源键开启【消原唱】对比：开启保护时老师声音绝不消失！）"
                )
            }
            playCardAudioWithHighlight(card, slowMode = false)
            _uiState.update {
                it.copy(
                    isRunningProbe = false,
                    highlightedWordIndex = -1,
                    diagnosticStatusText = "✅ 防消原唱与卡拉 OK 逐词高亮测试完成（当前蓝牙延迟补偿：${it.bluetoothLatencyOffsetMs}ms）。"
                )
            }
        }
    }

    // =========================================================================
    // 單句跟讀完整閉環原型（模式 A：聽外教 ➔ 叮咚自動開麥 ➔ VAD自動截斷 ➔ 打分 ➔ 9種音效獎勵）
    // =========================================================================

    fun selectSubMode(subMode: PracticeSubMode) {
        cancelActiveJob()
        _uiState.update {
            it.copy(
                practiceSubMode = subMode,
                practiceStage = PracticeStage.READY,
                highlightedWordIndex = -1,
                latestReport = null
            )
        }
    }

    fun nextSentence() {
        cancelActiveJob()
        _uiState.update { state ->
            val nextIdx = (state.currentSentenceIndex + 1) % state.sentences.size
            state.copy(
                currentSentenceIndex = nextIdx,
                selectedSlotIndex = 0,
                activeChunkIndex = 0,
                practiceStage = PracticeStage.READY,
                highlightedWordIndex = -1,
                latestReport = null
            )
        }
    }

    fun prevSentence() {
        cancelActiveJob()
        _uiState.update { state ->
            val prevIdx = if (state.currentSentenceIndex - 1 < 0) state.sentences.lastIndex else state.currentSentenceIndex - 1
            state.copy(
                currentSentenceIndex = prevIdx,
                selectedSlotIndex = 0,
                activeChunkIndex = 0,
                practiceStage = PracticeStage.READY,
                highlightedWordIndex = -1,
                latestReport = null
            )
        }
    }

    fun cycleSlotOrChunk() {
        _uiState.update { state ->
            when (state.practiceSubMode) {
                PracticeSubMode.CHUNK_READ -> {
                    val nextC = (state.activeChunkIndex + 1) % state.currentSentence.chunks.size.coerceAtLeast(1)
                    state.copy(activeChunkIndex = nextC, latestReport = null)
                }
                PracticeSubMode.SLOT_SUBSTITUTE -> {
                    val count = state.currentSentence.substitutions.size.coerceAtLeast(1)
                    state.copy(selectedSlotIndex = (state.selectedSlotIndex + 1) % count, latestReport = null)
                }
                else -> state
            }
        }
    }

    /**
     * 一鍵啟動「模式 A 免按鍵自動跟讀閉環」：
     * 1. A2DP 播放外教帶讀 + 屏幕卡拉 OK 逐詞高亮
     * 2. 若開啟了自動輪流說（autoFlowHandsFree），播完自動「叮咚」一聲切換 HFP 開麥
     * 3. 孩子拿著 XMKGMKF01YM 說完停頓 0.85 秒自動結束錄音、切回 A2DP、出星星與單詞反饋
     */
    fun startStandardAudioAndAutoPractice(slowMode: Boolean = false) {
        cancelActiveJob()
        activeTaskJob = viewModelScope.launch {
            val state = _uiState.value
            val card = state.currentSentence

            _uiState.update {
                it.copy(
                    practiceStage = if (slowMode) PracticeStage.PLAYING_STANDARD_SLOW else PracticeStage.PLAYING_STANDARD_NORMAL,
                    highlightedWordIndex = 0
                )
            }

            playCardAudioWithHighlight(card, slowMode = slowMode)

            _uiState.update { it.copy(highlightedWordIndex = -1) }

            if (_uiState.value.autoFlowHandsFree &&
                (_uiState.value.hasRecordPermission ||
                    _uiState.value.selectedInputMode == XiaomiKaraokeMicManager.PreferredInputMode.MODE_DEV_SIMULATOR)
            ) {
                startChildRecordingInternal()
            } else {
                _uiState.update { it.copy(practiceStage = PracticeStage.READY) }
            }
        }
    }

    /**
     * 手動或自動啟動孩子開口錄音與評測
     */
    fun startChildRecording() {
        cancelActiveJob()
        activeTaskJob = viewModelScope.launch {
            startChildRecordingInternal()
        }
    }

    private suspend fun startChildRecordingInternal() {
        _uiState.update {
            it.copy(
                practiceStage = PracticeStage.SWITCHING_TO_HFP,
                liveMicLevel = 0f,
                recordingElapsedMs = 0L
            )
        }

        // 先在 A2DP 下播放「叮咚！」開麥提示音
        player.playReadyToSpeakChime()

        _uiState.update { it.copy(practiceStage = PracticeStage.RECORDING_CHILD) }

        val recResult = micManager.recordWithAutoVad(
            mode = _uiState.value.selectedInputMode,
            maxDurationMs = 6500L,
            enableVadAutoStop = true,
            silenceHangoverMs = 850L,
            simulatedProfile = _uiState.value.selectedSimulatedProfile,
            expectedWordCount = _uiState.value.activeWordsList.size
        ) { frame ->
            _uiState.update {
                it.copy(
                    liveMicLevel = frame.currentRms,
                    livePeakRaw = frame.peakRaw,
                    liveRoutedDevice = frame.activeDeviceSummary,
                    liveSampleRate = frame.actualSampleRate,
                    recordingElapsedMs = frame.elapsedMs
                )
            }
        }

        _uiState.update {
            it.copy(
                practiceStage = PracticeStage.SCORING,
                liveMicLevel = 0f
            )
        }

        val saved = ringCache.saveToRingBuffer(recResult.pcm16kMono, 16000)
        val currentState = _uiState.value
        val report = scorer.evaluateWithStrategy(
            sentence = currentState.currentSentence,
            targetWords = currentState.activeWordsList,
            pcm16k = recResult.pcm16kMono,
            mode = currentState.selectedScoringEngineMode,
            credentials = AzureSpeechAssessmentClient.CloudCredentials(
                subscriptionKey = currentState.azureSubscriptionKey,
                region = currentState.azureRegion
            )
        )

        if (report.stars == 3) {
            ringCache.saveBestForSentence(currentState.currentSentence.id, recResult.pcm16kMono, 16000)
        }
        refreshAudioDevices()

        _uiState.update {
            it.copy(
                practiceStage = PracticeStage.FEEDBACK,
                lastSavedRecording = saved,
                latestReport = report,
                totalStarsEarned = it.totalStarsEarned + report.stars
            )
        }
    }

    /**
     * 對比回放：先播放一次標準外教音，再播放孩子剛才在 XMKGMKF01YM 上的錄音
     */
    fun playComparisonReplay() {
        val saved = _uiState.value.lastSavedRecording ?: return
        cancelActiveJob()
        activeTaskJob = viewModelScope.launch {
            val card = _uiState.value.currentSentence
            _uiState.update { it.copy(practiceStage = PracticeStage.PLAYING_COMPARISON) }

            // 1. 先播外教標準音
            playCardAudioWithHighlight(card, slowMode = false)
            _uiState.update { it.copy(highlightedWordIndex = -1) }
            delay(350)

            // 2. 再播孩子的錄音
            player.playWavFile(
                file = saved.file,
                enableAntiVocalCancel = _uiState.value.enableAntiVocalCancel
            )

            _uiState.update { it.copy(practiceStage = PracticeStage.FEEDBACK) }
        }
    }

    /**
     * 三星專屬獎勵：開啟 XMKGMKF01YM 9種趣味音效（怪獸/卡通/KTV）5秒搞怪秀（只錄音回放娛樂、不扣分）
     */
    fun startMagicSoundEffectShow() {
        cancelActiveJob()
        activeTaskJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    practiceStage = PracticeStage.MAGIC_SOUND_SHOW,
                    recordingElapsedMs = 0L
                )
            }
            player.playReadyToSpeakChime()

            val result = micManager.recordWithAutoVad(
                mode = _uiState.value.selectedInputMode,
                maxDurationMs = 5000L,
                enableVadAutoStop = true,
                simulatedProfile = _uiState.value.selectedSimulatedProfile,
                expectedWordCount = _uiState.value.activeWordsList.size
            ) { frame ->
                _uiState.update {
                    it.copy(
                        liveMicLevel = frame.currentRms,
                        recordingElapsedMs = frame.elapsedMs
                    )
                }
            }
            val saved = ringCache.saveToRingBuffer(result.pcm16kMono, 16000)
            player.playWavFile(saved.file, enableAntiVocalCancel = _uiState.value.enableAntiVocalCancel)
            _uiState.update {
                it.copy(
                    practiceStage = PracticeStage.FEEDBACK,
                    lastSavedRecording = saved,
                    liveMicLevel = 0f
                )
            }
        }
    }

    private suspend fun playCardAudioWithHighlight(card: SentenceCard, slowMode: Boolean) {
        val state = _uiState.value
        val timings = if (slowMode) card.wordTimingsSlow else card.wordTimingsNormal
        val totalDurationMs = timings.lastOrNull()?.endMs ?: 2200L
        val spokenText = state.activeTargetEnglish
        val prebakedWav = if (state.practiceSubMode == PracticeSubMode.FOLLOW_READ ||
            state.practiceSubMode == PracticeSubMode.DUAL_MIC_ROLE_PLAY
        ) {
            OfflineKaraokePackLoader.resolvePrebakedWavIfPresent(getApplication(), card, slowMode)
        } else null

        player.playSentenceWithKaraoke(
            text = spokenText,
            durationMs = totalDurationMs,
            slowMode = slowMode,
            enableAntiVocalCancel = state.enableAntiVocalCancel,
            bluetoothLatencyOffsetMs = state.bluetoothLatencyOffsetMs,
            prebakedWavFile = prebakedWav
        ) { currentMs ->
            val words = state.activeWordsList
            if (words.isNotEmpty()) {
                val idx = if (timings.size == words.size) {
                    timings.indexOfFirst { currentMs >= it.startMs && currentMs <= it.endMs }
                        .takeIf { it >= 0 }
                        ?: ((currentMs * words.size) / totalDurationMs.coerceAtLeast(1L)).toInt()
                            .coerceIn(0, words.lastIndex)
                } else {
                    ((currentMs * words.size) / totalDurationMs.coerceAtLeast(1L)).toInt()
                        .coerceIn(0, words.lastIndex)
                }
                _uiState.update { it.copy(highlightedWordIndex = idx) }
            }
        }
    }

    fun cancelActiveJob() {
        activeTaskJob?.cancel()
        activeTaskJob = null
        _uiState.update {
            it.copy(
                isRunningProbe = false,
                liveMicLevel = 0f,
                highlightedWordIndex = -1
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelActiveJob()
        player.release()
    }
}
