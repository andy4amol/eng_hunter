package com.enghunter.tv.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * EngHunter "StarStage 星空魔法舞台" 少儿大屏视觉设计规范 (Design Tokens)
 * 专为 6~12 岁小学生客厅大屏沉浸体验设计：
 * - 天鹅绒魔幻夜紫背景，柔和护眼、电影剧场感
 * - 糖果色功能点缀（闪烁金、薄荷绿、晚霞橙、极光蓝）
 * - 磨砂玻璃拟态 (Glassmorphism) 与微立体浮雕
 */
object StarStageTokens {

    // 1. 空间幕布色系 (Space Backdrop)
    val SkyDeepMidnight = Color(0xFF0D0A24)   // 深空天鹅绒夜紫底色
    val SkyDarkPurple = Color(0xFF1B1446)     // 空间纵深过渡紫
    val SkyCosmicBlue = Color(0xFF161E54)     // 极光微蓝边缘

    val StageBackdropBrush = Brush.verticalGradient(
        colors = listOf(SkyDeepMidnight, SkyDarkPurple, SkyCosmicBlue, SkyDeepMidnight)
    )

    // 2. 核心舞台玻璃拟态卡片 (Glassmorphism Stage)
    val GlassCardBgTop = Color(0xFF28205C).copy(alpha = 0.88f)
    val GlassCardBgBottom = Color(0xFF18133E).copy(alpha = 0.94f)
    val GlassCardBorder = Color(0xFF7C66DC).copy(alpha = 0.65f)
    val GlassCardFocusedBorder = Color(0xFFFFD233) // 获得焦点时的黄金高光边框

    val GlassStageBrush = Brush.linearGradient(
        colors = listOf(GlassCardBgTop, GlassCardBgBottom)
    )

    // 3. 少儿糖果色系 (Candy Accent Palette)
    val GoldStar = Color(0xFFFFD233)        // 逐词高亮、闪烁金星、焦点光环
    val GoldStarBg = Color(0xFF5A4404)      // 金色高光背板
    val MintGreen = Color(0xFF2FD58A)       // 发音优秀、绿灯通过、录音波动
    val CoralOrange = Color(0xFFFF7E40)     // 需要加油纠音单词、提示色
    val AuroraBlue = Color(0xFF38B6FF)      // 外教带读流光、标准音标签
    val CandyPink = Color(0xFFFF5CAA)       // AI 举一反三魔法色、特效奖励
    val LavenderMuted = Color(0xFFA594F9)   // 次要文案、音标与意群边框

    // 4. 文字层级规范 (Text Colors)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFD6CFF9)   // 带有一丝淡紫温润感的浅色
    val TextTertiary = Color(0xFF8E82BE)

    // 5. 呼吸光晕微动效 Brush
    @Composable
    fun rememberBreathingGlowBrush(
        accentColor: Color = GoldStar
    ): Brush {
        val infiniteTransition = rememberInfiniteTransition(label = "glowTransition")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )
        return Brush.radialGradient(
            colors = listOf(
                accentColor.copy(alpha = alpha),
                accentColor.copy(alpha = alpha * 0.3f),
                Color.Transparent
            )
        )
    }
}
