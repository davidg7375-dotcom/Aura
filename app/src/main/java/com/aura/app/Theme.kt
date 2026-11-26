package com.aura.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

object AuraColors {
    // פלטה מתוך הלוגו: שקיעה חמה מעל אגם, ירוק מרווה כהה ועלה שקוף
    val Bg = Color(0xFFFBF5E8)
    val BgBottom = Color(0xFFE2EADF)
    val Card = Color(0xFFFFFCF5)
    val Line = Color(0x1F3F5747)
    val Green = Color(0xFF4F6B58)
    val GreenDark = Color(0xFF3F5747)
    val Sage = Color(0xFFDCE6D9)
    val Avatar = Color(0xFFF3E4C9)
    val Peach = Color(0xFFEAD7B3)
    val Sun = Color(0xFFF2C98F)
    val Ink = Color(0xFF2F3B33)
    val Muted = Color(0xFF7C897E)
    val PaidBg = Color(0xFFDCEBDD)
    val PaidFg = Color(0xFF3F7A4E)
    val DueBg = Color(0xFFF6DDD2)
    val DueFg = Color(0xFFB5533C)
    val SoonBg = Color(0xFFF8E6C2)
    val SoonFg = Color(0xFFA9742A)
    val Debt = Color(0xFFB5533C)
}

@Composable
fun AuraTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = AuraColors.Green,
        onPrimary = Color.White,
        background = AuraColors.Bg,
        onBackground = AuraColors.Ink,
        surface = AuraColors.Card,
        onSurface = AuraColors.Ink,
        surfaceVariant = AuraColors.Sage,
        outline = AuraColors.Muted
    )
    MaterialTheme(colorScheme = scheme) {
        // ממשק RTL אמיתי בכל האפליקציה
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            content()
        }
    }
}

@Composable
fun AuraBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AuraColors.Bg, AuraColors.BgBottom))),
        content = content
    )
}
