package com.enghunter.tv.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import com.enghunter.tv.audio.XiaomiKaraokeMicManager
import com.enghunter.tv.model.WordFeedbackLevel
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
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF0F172A))
                )
            )
            // 5% 電視安全邊距（防止過掃描裁切）
            .padding(horizontal = 36.dp, vertical = 22.dp)
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

            Spacer(modifier = Modifier.height(14.dp))

            when (uiState.activeTab) {
                ActiveTab.PRACTICE_PROTOTYPE -> {
                    PracticeClosedLoopScreen(
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
            }
        }
    }
}

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
            .background(Color(0xFF1E293B).copy(alpha = 0.92f))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "🦁 EngHunter 少儿英语开口王",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFACC15)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF065F46))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "XMKGMKF01YM · 模式A全无线闭环",
                    fontSize = 13.sp,
                    color = Color(0xFFA7F3D0),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TvFocusableButton(
                text = "🎯 单句跟读闯关原型",
                selected = uiState.activeTab == ActiveTab.PRACTICE_PROTOTYPE,
                accentColor = Color(0xFF38BDF8),
                onClick = { onSwitchTab(ActiveTab.PRACTICE_PROTOTYPE) }
            )

            TvFocusableButton(
                text = "🛠️ 阶段0：K歌麦硬件探测台",
                selected = uiState.activeTab == ActiveTab.PHASE0_DIAGNOSTIC,
                accentColor = Color(0xFFF59E0B),
                onClick = { onSwitchTab(ActiveTab.PHASE0_DIAGNOSTIC) }
            )

            TvFocusableButton(
                text = if (uiState.autoFlowHandsFree) "⚡ 免按键自动轮流说：已开启" else "⚡ 免按键自动轮流说：已关闭",
                selected = uiState.autoFlowHandsFree,
                accentColor = Color(0xFF10B981),
                onClick = onToggleHandsFree
            )

            if (!uiState.hasRecordPermission) {
                TvFocusableButton(
                    text = "⚠️ 点击授权麦克风权限",
                    selected = true,
                    accentColor = Color(0xFFEF4444),
                    onClick = onRequestPermission
                )
            } else {
                Text(
                    text = "⭐ 累计星星: ${uiState.totalStarsEarned}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFDE047)
                )
            }
        }
    }
}

/**
 * 頁面 1：小學生單句開口練習完整閉環原型（模式 A：A2DP 雙 5W 喇叭帶讀 ➔ HFP 16mm 咪芯免按鍵拾音 ➔ 護心打分 ➔ 9種音效獎勵）
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 頂部四種教學子模式切換欄（跟讀 / 意群分段 / 看圖換詞造句 / 雙麥對唱問答）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PracticeSubMode.entries.forEach { mode ->
                    TvFocusableButton(
                        text = mode.title,
                        selected = uiState.practiceSubMode == mode,
                        accentColor = Color(0xFF818CF8),
                        onClick = { viewModel.selectSubMode(mode) }
                    )
                }
            }

            Text(
                text = "${card.unitTitle}  (${uiState.currentSentenceIndex + 1}/${uiState.sentences.size})",
                fontSize = 16.sp,
                color = Color(0xFFCBD5E1),
                fontWeight = FontWeight.SemiBold
            )
        }

        // 2. 中央大字號電視舞台卡片（字號 >= 44sp，支持卡拉 OK 逐詞高亮與綠/橙護心反饋）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF1E293B), Color(0xFF312E81))
                    )
                )
                .border(2.dp, Color(0xFF4F46E5), RoundedCornerShape(24.dp))
                .padding(horizontal = 28.dp, vertical = 22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 句式模板提示與狀態膠囊
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "核心句型：${card.pattern}",
                        fontSize = 18.sp,
                        color = Color(0xFF93C5FD),
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF0F172A))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = uiState.practiceStage.label,
                            fontSize = 16.sp,
                            color = Color(0xFFFDE047),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 雙麥對唱角色提示（當選擇第 4 模式時）
                if (uiState.practiceSubMode == PracticeSubMode.DUAL_MIC_ROLE_PLAY) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A).copy(alpha = 0.7f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(
                            text = "🎤 1号K歌麦 (家长/伙伴问)：\"${card.rolePlayQuestion}\"",
                            fontSize = 18.sp,
                            color = Color(0xFF67E8F9),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "🎤 2号K歌麦 (小朋友答)：\"${card.en}\"",
                            fontSize = 18.sp,
                            color = Color(0xFFFDE047),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 情景大圖標 + 大字號卡拉 OK 單詞流
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = uiState.activeEmoji, fontSize = 52.sp)
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        words.forEachIndexed { idx, word ->
                            val isKaraokeActive = uiState.highlightedWordIndex == idx
                            val scoredWord = uiState.latestReport?.wordResults?.getOrNull(idx)

                            val wordColor by animateColorAsState(
                                targetValue = when {
                                    isKaraokeActive -> Color(0xFFFACC15) // 卡拉OK高亮亮黃色
                                    scoredWord?.level == WordFeedbackLevel.EXCELLENT -> Color(0xFF4ADE80) // 優秀綠色
                                    scoredWord?.level == WordFeedbackLevel.KEEP_TRYING -> Color(0xFFFB923C) // 溫和橙色
                                    else -> Color.White
                                },
                                label = "wordColor"
                            )
                            val wordScale by animateFloatAsState(
                                targetValue = if (isKaraokeActive) 1.18f else 1.0f,
                                label = "wordScale"
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .scale(wordScale)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isKaraokeActive) Color(0xFF4338CA)
                                        else Color.Transparent
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = word,
                                    fontSize = 46.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = wordColor
                                )
                                if (scoredWord != null) {
                                    Text(
                                        text = "${scoredWord.score}分",
                                        fontSize = 13.sp,
                                        color = wordColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = uiState.activeTargetChinese,
                    fontSize = 24.sp,
                    color = Color(0xFFE2E8F0),
                    fontWeight = FontWeight.Medium
                )

                // 實時麥克風聲波能量條（當處於錄音或搞怪音效秀時展示）
                if (uiState.practiceStage == PracticeStage.RECORDING_CHILD ||
                    uiState.practiceStage == PracticeStage.MAGIC_SOUND_SHOW
                ) {
                    Spacer(modifier = Modifier.height(14.dp))
                    LiveVoiceWaveMeter(
                        level = uiState.liveMicLevel,
                        elapsedMs = uiState.recordingElapsedMs,
                        routedDevice = uiState.liveRoutedDevice
                    )
                }
            }
        }

        // 3. 大尺寸遙控器焦點操作按鈕區（不劫持全局方向鍵，符合兒童電視直覺）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvFocusableButton(
                text = "🔊 听外教带读 + 自动开麦",
                selected = true,
                accentColor = Color(0xFF22C55E),
                onClick = {
                    if (!uiState.hasRecordPermission) onRequestPermission()
                    else viewModel.startStandardAudioAndAutoPractice(slowMode = false)
                }
            )

            TvFocusableButton(
                text = "🐢 慢速带读 (0.8x)",
                selected = false,
                accentColor = Color(0xFF38BDF8),
                onClick = {
                    if (!uiState.hasRecordPermission) onRequestPermission()
                    else viewModel.startStandardAudioAndAutoPractice(slowMode = true)
                }
            )

            TvFocusableButton(
                text = "🎤 直接拿K歌麦开口说",
                selected = false,
                accentColor = Color(0xFFF59E0B),
                onClick = {
                    if (!uiState.hasRecordPermission) onRequestPermission()
                    else viewModel.startChildRecording()
                }
            )

            if (uiState.lastSavedRecording != null) {
                TvFocusableButton(
                    text = "🔁 对比回放 (标准音 vs 我)",
                    selected = false,
                    accentColor = Color(0xFFA855F7),
                    onClick = { viewModel.playComparisonReplay() }
                )
            }

            if (uiState.practiceSubMode == PracticeSubMode.CHUNK_READ ||
                uiState.practiceSubMode == PracticeSubMode.SLOT_SUBSTITUTE
            ) {
                TvFocusableButton(
                    text = if (uiState.practiceSubMode == PracticeSubMode.CHUNK_READ) "🧩 切换下一意群" else "🔄 换一张图片造句",
                    selected = false,
                    accentColor = Color(0xFFEC4899),
                    onClick = { viewModel.cycleSlotOrChunk() }
                )
            }

            TvFocusableButton(
                text = "⬅️ 上一句",
                selected = false,
                accentColor = Color(0xFF64748B),
                onClick = { viewModel.prevSentence() }
            )

            TvFocusableButton(
                text = "➡️ 下一句",
                selected = false,
                accentColor = Color(0xFF64748B),
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
            .background(Color(0xFF0F172A).copy(alpha = 0.85f))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "🎙️ 正在通过 [$routedDevice] 收音 (说完停顿 0.8 秒自动结束)",
                fontSize = 15.sp,
                color = Color(0xFF4ADE80),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${String.format("%.1f", elapsedMs / 1000.0)}s / 6.5s",
                fontSize = 15.sp,
                color = Color(0xFFFDE047),
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { level.coerceIn(0.05f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp)),
            color = Color(0xFF22C55E),
            trackColor = Color(0xFF334155)
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
            .background(Color(0xFF1E293B))
            .border(1.5.dp, Color(0xFFFACC15), RoundedCornerShape(18.dp))
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val starStr = "⭐".repeat(report.stars) + "☆".repeat(3 - report.stars)
                Text(
                    text = "$starStr   总评 ${report.overallScore} 分",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFACC15)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "完整度 ${report.completenessScore}% · 流利度 ${report.fluencyScore}% · [${report.engineSource}]",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = report.encouragementMessage,
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }

        if (report.unlockMagicSoundEffect) {
            Spacer(modifier = Modifier.width(16.dp))
            TvFocusableButton(
                text = "🎭 三星奖励：开启怪兽/卡通音效搞怪秀 (5秒)",
                selected = true,
                accentColor = Color(0xFFEC4899),
                onClick = onTriggerMagicSoundEffectShow
            )
        }
    }
}

/**
 * 頁面 2：【階段 0：小米電視 & XMKGMKF01YM 麥克風硬件探測台】
 * 用於 10 分鐘實測驗證：
 * 1. 電視識別到的 A2DP / HFP(SCO) / USB / 內置麥設備清單
 * 2. 模式 A 全無線 HFP 5秒錄音 + A2DP 雙 5W 喇叭回放閉環
 * 3. 防消原唱（Anti-Vocal-Cancel）立體聲保護與藍牙卡拉 OK 延遲毫秒微調
 * 4. 小米遙控器按鍵 KeyCode 實時捕獲與環形緩存狀態
 */
@Composable
private fun Phase0HardwareDiagnosticScreen(
    uiState: EngHunterUiState,
    viewModel: EngHunterViewModel,
    onRequestPermission: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 左側：控制與閉環測試面板
        Column(
            modifier = Modifier
                .weight(1.15f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E293B))
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🛠️ 阶段 0：XMKGMKF01YM 无线 K歌麦探测与校准",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFACC15)
            )

            // 三種模式一鍵切換測試
            XiaomiKaraokeMicManager.PreferredInputMode.entries.forEach { mode ->
                TvFocusableButton(
                    text = if (uiState.selectedInputMode == mode) "✅ ${mode.title}" else mode.title,
                    selected = uiState.selectedInputMode == mode,
                    accentColor = Color(0xFF38BDF8),
                    onClick = { viewModel.selectInputMode(mode) }
                )
            }

            // 階段 0 核心測試按鈕
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TvFocusableButton(
                    text = if (uiState.isRunningProbe) "⏳ 测试进行中..." else "🎙️ ① 测 HFP 无线录音+双5W喇叭回放",
                    selected = true,
                    accentColor = Color(0xFF22C55E),
                    onClick = {
                        if (!uiState.hasRecordPermission) onRequestPermission()
                        else viewModel.runPhase0WirelessHfpLoopbackTest()
                    }
                )

                TvFocusableButton(
                    text = "🔊 ② 测防消原唱+卡拉OK高亮同步",
                    selected = false,
                    accentColor = Color(0xFFF59E0B),
                    onClick = { viewModel.runPhase0AntiVocalCancelKaraokeTest() }
                )
            }

            // 防消原唱開關 & 藍牙延遲微調
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TvFocusableButton(
                    text = if (uiState.enableAntiVocalCancel) "🛡️ 防一键消原唱立体声保护：开启" else "🛡️ 防一键消原唱立体声保护：关闭",
                    selected = uiState.enableAntiVocalCancel,
                    accentColor = Color(0xFF10B981),
                    onClick = { viewModel.toggleAntiVocalCancel() }
                )

                TvFocusableButton(
                    text = "蓝牙延迟 -30ms",
                    selected = false,
                    accentColor = Color(0xFF64748B),
                    onClick = { viewModel.adjustBluetoothLatency(-30L) }
                )

                Text(
                    text = "补偿: ${uiState.bluetoothLatencyOffsetMs}ms",
                    fontSize = 15.sp,
                    color = Color(0xFFFDE047),
                    fontWeight = FontWeight.Bold
                )

                TvFocusableButton(
                    text = "蓝牙延迟 +30ms",
                    selected = false,
                    accentColor = Color(0xFF64748B),
                    onClick = { viewModel.adjustBluetoothLatency(30L) }
                )
            }

            // 實時狀態輸出日誌框
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = uiState.diagnosticStatusText,
                        fontSize = 14.sp,
                        color = Color(0xFFA7F3D0)
                    )
                    Text(
                        text = "🎮 遥控器按键监听：${uiState.lastKeyEventInfo}",
                        fontSize = 13.sp,
                        color = Color(0xFF93C5FD)
                    )
                    Text(
                        text = "💾 环形缓存 (最多20条防撑爆电视闪存)：已存 ${uiState.ringCacheCount}/20 条，共 ${uiState.ringCacheBytes / 1024} KB",
                        fontSize = 13.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }

        // 右側：小米電視當前音頻輸入/輸出硬件設備清單
        Column(
            modifier = Modifier
                .weight(0.85f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E293B))
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📡 小米电视音频设备实时拓扑",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                TvFocusableButton(
                    text = "🔄 刷新设备",
                    selected = false,
                    accentColor = Color(0xFF38BDF8),
                    onClick = { viewModel.refreshAudioDevices() }
                )
            }

            Text(
                text = "【输入设备 INPUTS】(${uiState.inputDevices.size} 个)",
                fontSize = 15.sp,
                color = Color(0xFF4ADE80),
                fontWeight = FontWeight.Bold
            )
            if (uiState.inputDevices.isEmpty()) {
                Text(
                    text = "• 暂无外置输入设备（开启 HFP 时将动态挂载蓝牙 SCO 通道）",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
            } else {
                uiState.inputDevices.forEach { dev ->
                    EndpointCard(dev)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "【输出设备 OUTPUTS】(${uiState.outputDevices.size} 个)",
                fontSize = 15.sp,
                color = Color(0xFF60A5FA),
                fontWeight = FontWeight.Bold
            )
            uiState.outputDevices.forEach { dev ->
                EndpointCard(dev)
            }
        }
    }
}

@Composable
private fun EndpointCard(item: XiaomiKaraokeMicManager.AudioEndpointItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (item.isXiaomiKaraokeMicCandidate) Color(0xFF1E3A8A) else Color(0xFF0F172A))
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = "${if (item.isXiaomiKaraokeMicCandidate) "⭐ " else ""}${item.productName} (id=${item.id})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "${item.typeLabel} | 采样率: ${if (item.sampleRates.isEmpty()) "自适应" else item.sampleRates.joinToString()}",
                fontSize = 12.sp,
                color = Color(0xFFCBD5E1)
            )
        }
    }
}

/**
 * 專為小米電視遙控器 D-Pad 焦點導航設計的大尺寸發光按鈕
 * - 遙控器方向鍵移入焦點時自動放大 1.06 倍並高亮金色外框
 * - 按遙控器【確定/OK鍵】或鼠標點擊均可觸發
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
        targetValue = if (isFocused) 1.06f else 1.0f,
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
            else -> Color(0xFF334155)
        },
        border = BorderStroke(
            width = if (isFocused) 3.dp else 1.dp,
            color = if (isFocused) Color(0xFFFDE047) else accentColor.copy(alpha = 0.65f)
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (isFocused) Color(0xFF0F172A) else Color.White,
            textAlign = TextAlign.Center
        )
    }
}
