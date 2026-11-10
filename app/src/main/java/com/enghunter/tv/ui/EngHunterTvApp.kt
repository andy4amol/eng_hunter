package com.enghunter.tv.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enghunter.tv.audio.VirtualKaraokeMicTwin
import com.enghunter.tv.audio.XiaomiKaraokeMicManager
import com.enghunter.tv.llm.LlmVendorPreset
import com.enghunter.tv.model.CurriculumUnit
import com.enghunter.tv.model.WordFeedbackLevel
import com.enghunter.tv.scoring.PronunciationScorer
import com.enghunter.tv.ui.theme.StarStageTokens
import com.enghunter.tv.viewmodel.ActiveTab
import com.enghunter.tv.viewmodel.EngHunterUiState
import com.enghunter.tv.viewmodel.EngHunterViewModel
import com.enghunter.tv.viewmodel.PracticeStage
import com.enghunter.tv.viewmodel.PracticeSubMode

@Composable
fun EngHunterTvApp(
    uiState: EngHunterUiState,
    viewModel: EngHunterViewModel,
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StarStageTokens.StageBackdropBrush)
            // 5% 電視安全邊距（防止過掃描裁切）
            .padding(horizontal = 24.dp, vertical = 10.dp)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    viewModel.onRemoteKeyIntercepted(
                        keyEvent.nativeKeyEvent.keyCode,
                        "KEY_DOWN"
                    )
                }
                false
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopTvNavigationBar(
                uiState = uiState,
                onSwitchTab = { viewModel.switchTab(it) },
                onToggleHandsFree = { viewModel.toggleHandsFreeAutoFlow() },
                onRequestPermission = onRequestPermission
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (uiState.activeTab) {
                    ActiveTab.PRACTICE_PROTOTYPE -> {
                        PracticeClosedLoopScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onRequestPermission = onRequestPermission
                        )
                    }
                    ActiveTab.SETTINGS_DRAWER -> {
                        SettingsHubScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onRequestPermission = onRequestPermission
                        )
                    }
                    ActiveTab.PHASE0_DIAGNOSTIC -> {
                        Phase0HardwareDiagnosticScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onRequestPermission = onRequestPermission
                        )
                    }
                    ActiveTab.AI_MODEL_SETTINGS -> {
                        AiModelProviderSettingsScreen(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

/**
 * 顶部少儿沉浸式导航栏：
 * 隐藏底层硬件与开发者专业词汇，突出品牌吉祥物、累计金星池、免按键开麦与设置中心入口
 */
@Composable
private fun TopTvNavigationBar(
    uiState: EngHunterUiState,
    onSwitchTab: (ActiveTab) -> Unit,
    onToggleHandsFree: () -> Unit,
    onRequestPermission: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1748).copy(alpha = 0.85f))
            .border(1.dp, Color(0xFF6B58C4).copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 左侧少儿品牌 Logo 强化
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFBBF24).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🦁", fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "EngHunter",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = StarStageTokens.GoldStar
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "英语魔法舞台",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = StarStageTokens.TextSecondary
            )
        }

        // 右侧交互小岛
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 返回主舞台
            if (uiState.activeTab != ActiveTab.PRACTICE_PROTOTYPE) {
                TvFocusableButton(
                    text = "🎪 返回练习舞台",
                    selected = true,
                    accentColor = StarStageTokens.GoldStar,
                    onClick = { onSwitchTab(ActiveTab.PRACTICE_PROTOTYPE) }
                )
            }

            // 免按键模式胶囊
            TvFocusableButton(
                text = if (uiState.autoFlowHandsFree) "⚡ 免按键开口: 开" else "⚡ 免按键开口: 关",
                selected = uiState.autoFlowHandsFree,
                accentColor = StarStageTokens.MintGreen,
                onClick = onToggleHandsFree
            )

            // 麦克风权限提示
            if (!uiState.hasRecordPermission) {
                TvFocusableButton(
                    text = "⚠️ 授权麦克风",
                    selected = true,
                    accentColor = Color(0xFFEF4444),
                    onClick = onRequestPermission
                )
            }

            // 金星收集池
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF3B2D0B))
                    .border(1.5.dp, StarStageTokens.GoldStar, RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "⭐", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${uiState.totalStarsEarned} 颗星",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = StarStageTokens.GoldStar
                )
            }

            // 统一设置中心
            TvFocusableButton(
                text = "⚙️ 家长与设置",
                selected = uiState.activeTab == ActiveTab.SETTINGS_DRAWER,
                accentColor = StarStageTokens.LavenderMuted,
                onClick = { onSwitchTab(ActiveTab.SETTINGS_DRAWER) }
            )
        }
    }
}

/**
 * 家长与设置中心 (收纳硬件探测、AI 供应商设置、数字孪生模拟器与高级调优)
 */
@Composable
private fun SettingsHubScreen(
    uiState: EngHunterUiState,
    viewModel: EngHunterViewModel,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚙️ 家长与开发者设置控制中心",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = StarStageTokens.TextPrimary
            )
            TvFocusableButton(
                text = "⬅️ 返回练习舞台",
                selected = true,
                accentColor = StarStageTokens.GoldStar,
                onClick = { viewModel.switchTab(ActiveTab.PRACTICE_PROTOTYPE) }
            )
        }

        // 快捷选项卡
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TvFocusableButton(
                text = "🛠️ 进入硬件探测完整控制台 (XMKGMKF01YM)",
                selected = false,
                accentColor = Color(0xFFF59E0B),
                onClick = { viewModel.switchTab(ActiveTab.PHASE0_DIAGNOSTIC) }
            )

            TvFocusableButton(
                text = "🤖 配置大模型供应商 (${uiState.llmConfig.vendor.badge})",
                selected = false,
                accentColor = StarStageTokens.CandyPink,
                onClick = { viewModel.switchTab(ActiveTab.AI_MODEL_SETTINGS) }
            )
        }

        // 数字孪生与发音评测快捷调控
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(StarStageTokens.GlassStageBrush)
                .border(1.5.dp, StarStageTokens.GlassCardBorder, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "🧪 离线数字孪生麦克风仿真器 (无需硬件即可开发自测)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = StarStageTokens.AuroraBlue
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    VirtualKaraokeMicTwin.SimulatedChildVoiceProfile.entries.forEach { profile ->
                        TvFocusableButton(
                            text = profile.badge,
                            selected = uiState.selectedSimulatedProfile == profile,
                            accentColor = StarStageTokens.AuroraBlue,
                            onClick = { viewModel.selectSimulatedProfile(profile) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🧠 发音评测策略引擎选择",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = StarStageTokens.MintGreen
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PronunciationScorer.ScoringEngineMode.entries.forEach { mode ->
                        TvFocusableButton(
                            text = mode.badge,
                            selected = uiState.selectedScoringEngineMode == mode,
                            accentColor = StarStageTokens.MintGreen,
                            onClick = { viewModel.selectScoringEngineMode(mode) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 頁面 1：StarStage 星空魔法舞台（小學生單句開口練習核心）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PracticeClosedLoopScreen(
    uiState: EngHunterUiState,
    viewModel: EngHunterViewModel,
    onRequestPermission: () -> Unit
) {
    val card = uiState.currentSentence
    val words = uiState.activeWordsList

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 0. 里程碑 M2：6 大主題單元篩選欄（小巧糖果色膠囊）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurriculumUnit.entries.forEach { unit ->
                TvFocusableButton(
                    text = "${unit.emoji} ${unit.shortTitle}",
                    selected = uiState.selectedUnit == unit,
                    accentColor = StarStageTokens.MintGreen,
                    onClick = { viewModel.selectUnit(unit) }
                )
            }
        }

        // 1. 四種趣味練習模式切換欄
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PracticeSubMode.entries.forEach { mode ->
                    TvFocusableButton(
                        text = mode.title,
                        selected = uiState.practiceSubMode == mode,
                        accentColor = StarStageTokens.AuroraBlue,
                        onClick = { viewModel.selectSubMode(mode) }
                    )
                }
            }

            Text(
                text = "${card.unitTitle} · 第 ${uiState.currentSentenceIndex + 1}/${uiState.sentences.size} 关",
                fontSize = 15.sp,
                color = StarStageTokens.TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }

        // 2. 中央大字號魔法舞台卡片（磨砂玻璃拟态 + 卡拉OK逐词音符浮动）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(StarStageTokens.GlassStageBrush)
                .border(2.dp, StarStageTokens.GlassCardBorder, RoundedCornerShape(24.dp))
                .padding(horizontal = 26.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部句型徽章与关卡状态胶囊
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF382B73))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🎯 核心句型：${card.pattern}",
                            fontSize = 15.sp,
                            color = StarStageTokens.AuroraBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF0F0B26))
                            .border(1.dp, StarStageTokens.GoldStar.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = uiState.practiceStage.label,
                            fontSize = 14.sp,
                            color = StarStageTokens.GoldStar,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 雙麥對唱角色提示
                if (uiState.practiceSubMode == PracticeSubMode.DUAL_MIC_ROLE_PLAY) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF130E2E).copy(alpha = 0.85f))
                            .border(1.dp, StarStageTokens.AuroraBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(
                            text = "🎤 1号麦 (家长/伙伴问)：\"${card.rolePlayQuestion}\"",
                            fontSize = 17.sp,
                            color = StarStageTokens.AuroraBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "🎤 2号麦 (小朋友答)：\"${card.en}\"",
                            fontSize = 17.sp,
                            color = StarStageTokens.GoldStar,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 3D 拟物情景大图标 + 逐词音符卡片流 (Word Token Cards)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFF4C3B99), Color(0xFF19133F))
                                )
                            )
                            .border(2.dp, StarStageTokens.GoldStar.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = uiState.activeEmoji, fontSize = 48.sp)
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        words.forEachIndexed { idx, word ->
                            val isKaraokeActive = uiState.highlightedWordIndex == idx
                            val scoredWord = uiState.latestReport?.wordResults?.getOrNull(idx)

                            // 单词卡片背景色与浮动动效
                            val wordColor by animateColorAsState(
                                targetValue = when {
                                    isKaraokeActive -> StarStageTokens.GoldStar // 卡拉OK高亮亮黃色
                                    scoredWord?.level == WordFeedbackLevel.EXCELLENT -> StarStageTokens.MintGreen // 優秀綠色
                                    scoredWord?.level == WordFeedbackLevel.KEEP_TRYING -> StarStageTokens.CoralOrange // 溫和橙色
                                    else -> StarStageTokens.TextPrimary
                                },
                                label = "wordColor"
                            )
                            val wordScale by animateFloatAsState(
                                targetValue = if (isKaraokeActive) 1.15f else 1.0f,
                                animationSpec = spring(dampingRatio = 0.65f),
                                label = "wordScale"
                            )

                            Box(
                                modifier = Modifier
                                    .scale(wordScale)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        when {
                                            isKaraokeActive -> Color(0xFF5A4404)
                                            scoredWord?.level == WordFeedbackLevel.EXCELLENT -> Color(0xFF0F3A24)
                                            scoredWord?.level == WordFeedbackLevel.KEEP_TRYING -> Color(0xFF4A2411)
                                            else -> Color(0xFF1B153D).copy(alpha = 0.65f)
                                        }
                                    )
                                    .border(
                                        width = if (isKaraokeActive) 2.dp else 1.dp,
                                        color = if (isKaraokeActive) StarStageTokens.GoldStar else Color(0xFF59489E).copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = word,
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = wordColor,
                                        letterSpacing = 1.sp
                                    )
                                    if (scoredWord != null) {
                                        Text(
                                            text = "${scoredWord.score}分",
                                            fontSize = 12.sp,
                                            color = wordColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 优雅的中文辅助理解字幕
                Text(
                    text = uiState.activeTargetChinese,
                    fontSize = 22.sp,
                    color = StarStageTokens.TextSecondary,
                    fontWeight = FontWeight.Medium
                )

                // 實時麥克風聲波能量條
                if (uiState.practiceStage == PracticeStage.RECORDING_CHILD ||
                    uiState.practiceStage == PracticeStage.MAGIC_SOUND_SHOW
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LiveVoiceWaveMeter(
                        level = uiState.liveMicLevel,
                        elapsedMs = uiState.recordingElapsedMs,
                        routedDevice = uiState.liveRoutedDevice
                    )
                }
            }
        }

        // 3. 精简、高频的核心操作控制栏（降低认知负荷，专注开口）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 主操作 1：听外教带读 + 自动开麦
            TvFocusableButton(
                text = "🔊 听外教带读 + 自动开麦",
                selected = true,
                accentColor = StarStageTokens.MintGreen,
                onClick = {
                    if (!uiState.hasRecordPermission &&
                        uiState.selectedInputMode != XiaomiKaraokeMicManager.PreferredInputMode.MODE_DEV_SIMULATOR
                    ) onRequestPermission()
                    else viewModel.startStandardAudioAndAutoPractice(slowMode = false)
                }
            )

            // 主操作 2：慢速外教拆解
            TvFocusableButton(
                text = "🐢 慢速示范 (0.8x)",
                selected = false,
                accentColor = StarStageTokens.AuroraBlue,
                onClick = {
                    if (!uiState.hasRecordPermission &&
                        uiState.selectedInputMode != XiaomiKaraokeMicManager.PreferredInputMode.MODE_DEV_SIMULATOR
                    ) onRequestPermission()
                    else viewModel.startStandardAudioAndAutoPractice(slowMode = true)
                }
            )

            // 主操作 3：直接开口
            TvFocusableButton(
                text = "🎤 拿麦直接读",
                selected = false,
                accentColor = StarStageTokens.GoldStar,
                onClick = {
                    if (!uiState.hasRecordPermission &&
                        uiState.selectedInputMode != XiaomiKaraokeMicManager.PreferredInputMode.MODE_DEV_SIMULATOR
                    ) onRequestPermission()
                    else viewModel.startChildRecording()
                }
            )

            // 智能开发自测：数字孪生快速触碰
            TvFocusableButton(
                text = "🧪 仿真模拟开口 (${uiState.selectedSimulatedProfile.badge})",
                selected = false,
                accentColor = Color(0xFFA855F7),
                onClick = {
                    viewModel.triggerSimulatedPractice(uiState.selectedSimulatedProfile)
                }
            )

            // 拓展操作：AI 魔法生题
            TvFocusableButton(
                text = if (uiState.isExpandingAi) "🪄 正在生成..." else "✨ AI魔法换句",
                selected = false,
                accentColor = StarStageTokens.CandyPink,
                onClick = { viewModel.triggerAiMagicExpansion() }
            )

            if (uiState.lastSavedRecording != null) {
                TvFocusableButton(
                    text = "🔁 对比回放",
                    selected = false,
                    accentColor = Color(0xFFA855F7),
                    onClick = { viewModel.playComparisonReplay() }
                )
            }

            if (uiState.practiceSubMode == PracticeSubMode.CHUNK_READ ||
                uiState.practiceSubMode == PracticeSubMode.SLOT_SUBSTITUTE
            ) {
                TvFocusableButton(
                    text = if (uiState.practiceSubMode == PracticeSubMode.CHUNK_READ) "🧩 下一意群" else "🔄 换图造句",
                    selected = false,
                    accentColor = StarStageTokens.CandyPink,
                    onClick = { viewModel.cycleSlotOrChunk() }
                )
            }

            TvFocusableButton(
                text = "⬅️ 上一句",
                selected = false,
                accentColor = StarStageTokens.TextTertiary,
                onClick = { viewModel.prevSentence() }
            )

            TvFocusableButton(
                text = "➡️ 下一句",
                selected = false,
                accentColor = StarStageTokens.TextTertiary,
                onClick = { viewModel.nextSentence() }
            )
        }

        // 4. 發音評測星星卡片 + XMKGMKF01YM 9種音效三星獎勵秀
        uiState.latestReport?.let { report ->
            PronunciationFeedbackBanner(
                report = report,
                onTriggerMagicSoundEffectShow = { viewModel.startMagicSoundEffectShow() }
            )
        }
    }
}

@Composable
private fun LiveVoiceWaveMeter(
    level: Float,
    elapsedMs: Long,
    routedDevice: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF130E2E).copy(alpha = 0.88f))
            .border(1.dp, StarStageTokens.MintGreen.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "🎙️ 正在通过 [$routedDevice] 收音 (说完停顿 0.8 秒自动结束)",
                fontSize = 14.sp,
                color = StarStageTokens.MintGreen,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${String.format("%.1f", elapsedMs / 1000.0)}s / 6.5s",
                fontSize = 14.sp,
                color = StarStageTokens.GoldStar,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { level.coerceIn(0.05f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = StarStageTokens.MintGreen,
            trackColor = Color(0xFF28205C)
        )
    }
}

@Composable
private fun PronunciationFeedbackBanner(
    report: com.enghunter.tv.model.PronunciationReport,
    onTriggerMagicSoundEffectShow: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(StarStageTokens.GlassStageBrush)
            .border(1.5.dp, StarStageTokens.GoldStar, RoundedCornerShape(18.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val starStr = "⭐".repeat(report.stars) + "☆".repeat(3 - report.stars)
                Text(
                    text = "$starStr   总评 ${report.overallScore} 分",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = StarStageTokens.GoldStar
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "完整度 ${report.completenessScore}% · 流利度 ${report.fluencyScore}% · [${report.engineSource}]",
                    fontSize = 13.sp,
                    color = StarStageTokens.TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = report.encouragementMessage,
                fontSize = 17.sp,
                color = StarStageTokens.TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }

        if (report.unlockMagicSoundEffect) {
            Spacer(modifier = Modifier.width(14.dp))
            TvFocusableButton(
                text = "🎭 三星奖励：怪兽/卡通变声音效秀 (5秒)",
                selected = true,
                accentColor = StarStageTokens.CandyPink,
                onClick = onTriggerMagicSoundEffectShow
            )
        }
    }
}

/**
 * 頁面 2：【階段 0：小米電視 & XMKGMKF01YM 麥克風硬件探測台】
 */
@Composable
private fun Phase0HardwareDiagnosticScreen(
    uiState: EngHunterUiState,
    viewModel: EngHunterViewModel,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 頂部診斷控制欄
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🛠️ 阶段 0：XMKGMKF01YM 无线 K歌麦探测与校准",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TvFocusableButton(
                    text = "🔄 重新扫描音频设备",
                    selected = false,
                    accentColor = Color(0xFF38BDF8),
                    onClick = { viewModel.refreshAudioDevices() }
                )
            }
        }

        // 設備探測狀態面板（左右分欄：輸入端拾音 vs 輸出端放音）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 左欄：音頻輸入端（咪芯）
            DiagnosticDeviceCard(
                title = "🎙️ 输入设备 (Microphone Endpoints)",
                devices = uiState.inputDevices,
                highlightType = "BLUETOOTH_SCO (K歌麦咪芯)",
                modifier = Modifier.weight(1f)
            )

            // 右欄：音頻輸出端（喇叭）
            DiagnosticDeviceCard(
                title = "🔊 输出设备 (Speaker Endpoints)",
                devices = uiState.outputDevices,
                highlightType = "BLUETOOTH_A2DP (双5W扬声器)",
                modifier = Modifier.weight(1f)
            )
        }

        // 模式 A 閉環 5 秒全無線實測卡片
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E293B))
                .border(2.dp, Color(0xFF10B981), RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "🧪 模式 A 全无线闭环实测 (5秒极速验收)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF34D399)
                )
                Text(
                    text = "测试说明：点击后系统将通过 Bluetooth SCO 通道录制 5 秒，随后自动切回 A2DP 蓝牙立体声通过麦克风双 5W 扬声器回放。同时将自动激活防消原唱算法与 VAD 静音截断。",
                    fontSize = 14.sp,
                    color = Color(0xFFCBD5E1)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvFocusableButton(
                        text = if (uiState.isRunningProbe) "⏳ 正在探测录音中..." else "🚀 开始 5 秒无线录音回放闭环测试",
                        selected = uiState.isRunningProbe,
                        accentColor = Color(0xFF10B981),
                        onClick = {
                            if (!uiState.hasRecordPermission) {
                                onRequestPermission()
                            } else {
                                viewModel.runPhase0WirelessHfpLoopbackTest()
                            }
                        }
                    )

                    TvFocusableButton(
                        text = if (uiState.enableAntiVocalCancel) "🛡️ 防消原唱立体声保护: 已开启" else "⚠️ 防消原唱: 已关闭",
                        selected = uiState.enableAntiVocalCancel,
                        accentColor = Color(0xFF818CF8),
                        onClick = { viewModel.toggleAntiVocalCancel() }
                    )

                    TvFocusableButton(
                        text = "⏱️ 延迟补偿: ${uiState.bluetoothLatencyOffsetMs}ms (+50ms)",
                        selected = false,
                        accentColor = Color(0xFFFBBF24),
                        onClick = {
                            viewModel.adjustBluetoothLatency(50L)
                        }
                    )
                }

                // 實時聲波電平反饋
                if (uiState.isRunningProbe) {
                    LiveVoiceWaveMeter(
                        level = uiState.liveMicLevel,
                        elapsedMs = 0L,
                        routedDevice = uiState.liveRoutedDevice
                    )
                }
            }
        }

        // 遙控器按鍵 KeyCode 實時捕獲展示
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = "📡 小米遥控器与麦克风按键拦截监视器：",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = uiState.lastKeyEventInfo,
                    fontSize = 16.sp,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * 頁面 3：【里程碑 M4：大模型供應商與靈活參數配置中心】
 */
@Composable
private fun AiModelProviderSettingsScreen(
    uiState: EngHunterUiState,
    viewModel: EngHunterViewModel
) {
    var apiKeyInput by remember(uiState.llmConfig.apiKey) { mutableStateOf(uiState.llmConfig.apiKey) }
    var modelInput by remember(uiState.llmConfig.modelName) { mutableStateOf(uiState.llmConfig.modelName) }
    var baseUrlInput by remember(uiState.llmConfig.baseUrl) { mutableStateOf(uiState.llmConfig.baseUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 頂部狀態欄
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🤖 AI 英语拓展与大模型供应商设置",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "支持 7 大主流服务商预设，电视端输入繁琐时可直接通过 ADB 命令行秒级写入",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF065F46))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "当前: ${uiState.llmConfig.vendor.displayName} · ${uiState.llmConfig.modelName}",
                    fontSize = 14.sp,
                    color = Color(0xFFA7F3D0),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 7 大供應商預設一鍵切換標籤欄
        Text(
            text = "① 选择预设服务商（自动填充最优 BaseURL 与标准 Model）：",
            fontSize = 16.sp,
            color = Color(0xFFE2E8F0),
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LlmVendorPreset.entries.forEach { vendor ->
                TvFocusableButton(
                    text = vendor.badge,
                    selected = uiState.llmConfig.vendor == vendor,
                    accentColor = Color(0xFFEC4899),
                    onClick = {
                        viewModel.selectLlmVendor(vendor)
                        apiKeyInput = ""
                        modelInput = vendor.candidateModels.firstOrNull() ?: ""
                        baseUrlInput = vendor.defaultBaseUrl
                    }
                )
            }
        }

        // 參數編輯與保存卡片
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E293B))
                .border(2.dp, Color(0xFFEC4899), RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "② 核心连接参数配置：",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF472B6)
                )

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("API Key (秘钥，若使用本地 Ollama 或离线模板可留空)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xFFCBD5E1),
                        focusedBorderColor = Color(0xFFEC4899),
                        unfocusedBorderColor = Color(0xFF64748B)
                    ),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = { modelInput = it },
                        label = { Text("模型标识 (Model ID)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFCBD5E1),
                            focusedBorderColor = Color(0xFFEC4899),
                            unfocusedBorderColor = Color(0xFF64748B)
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = baseUrlInput,
                        onValueChange = { baseUrlInput = it },
                        label = { Text("Base URL 接口地址") },
                        modifier = Modifier.weight(1.5f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFCBD5E1),
                            focusedBorderColor = Color(0xFFEC4899),
                            unfocusedBorderColor = Color(0xFF64748B)
                        ),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvFocusableButton(
                        text = "💾 保存配置并激活",
                        selected = true,
                        accentColor = Color(0xFF10B981),
                        onClick = {
                            viewModel.updateLlmCustomEndpointAndKey(
                                baseUrl = baseUrlInput,
                                modelName = modelInput,
                                apiKey = apiKeyInput
                            )
                        }
                    )

                    TvFocusableButton(
                        text = "🧪 测试当前 AI 生题",
                        selected = false,
                        accentColor = Color(0xFF38BDF8),
                        onClick = { viewModel.triggerAiMagicExpansion() }
                    )
                }
            }
        }

        // ADB 快速注入指南卡片
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "💡 极速配置技巧（无需在电视遥控器上痛苦输密码）：",
                    fontSize = 14.sp,
                    color = Color(0xFFFDE047),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "在电脑终端输入以下 ADB 命令即可一键将你的 API Key 发送到小米电视：",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "adb shell am broadcast -a com.enghunter.tv.CONFIG_LLM --es vendor DEEPSEEK --es api_key \"sk-你的密钥\" --es model deepseek-chat",
                    fontSize = 12.sp,
                    color = Color(0xFF67E8F9),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DiagnosticDeviceCard(
    title: String,
    devices: List<XiaomiKaraokeMicManager.AudioEndpointItem>,
    highlightType: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFACC15)
            )

            if (devices.isEmpty()) {
                Text(
                    text = "未检测到外部设备 (请开启蓝牙并配对麦克风)",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B)
                )
            } else {
                devices.forEach { dev ->
                    val isHighlight = dev.typeLabel.contains("BLUETOOTH", ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isHighlight) Color(0xFF0F2E22) else Color(0xFF0F172A))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${dev.productName} (${dev.typeLabel})",
                            fontSize = 13.sp,
                            color = if (isHighlight) Color(0xFF34D399) else Color(0xFFE2E8F0),
                            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = "ID: ${dev.id}",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 专为小米电视遥控器 D-Pad 焦点导航设计的 StarStage 发光按钮
 * - 获得焦点时触发柔和弹性放大与金黄色高斯光环
 * - 按钮材质升级为微立体磨砂触感
 */
@Composable
fun TvFocusableButton(
    text: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f),
        label = "btnScale"
    )

    Surface(
        modifier = Modifier
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                        event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = when {
            isFocused -> accentColor
            selected -> accentColor.copy(alpha = 0.28f)
            else -> Color(0xFF241B4E).copy(alpha = 0.85f)
        },
        border = BorderStroke(
            width = if (isFocused) 2.5.dp else 1.dp,
            color = if (isFocused) StarStageTokens.GoldStar else accentColor.copy(alpha = 0.55f)
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isFocused) Color(0xFF0F0B26) else Color.White,
            textAlign = TextAlign.Center
        )
    }
}
