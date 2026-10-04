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
    val Bg = Color(0xFFF1EFE7)
    val BgBottom = Color(0xFFE5EADD)
    val Card = Color(0xFFF8F6F0)
    val Line = Color(0x14000000)
    val Green = Color(0xFF5B7B5B)
    val GreenDark = Color(0xFF445F46)
    val Sage = Color(0xFFE3E9DA)
    val Avatar = Color(0xFFE9E1D3)
    val Ink = Color(0xFF2C312B)
    val Muted = Color(0xFF8A8F86)
    val PaidBg = Color(0xFFDCEBD6)
    val PaidFg = Color(0xFF4C7A4C)
    val DueBg = Color(0xFFF7DDD9)
    val DueFg = Color(0xFFC0463C)
    val SoonBg = Color(0xFFFBEBC6)
    val SoonFg = Color(0xFFB9811A)
    val Debt = Color(0xFFC0392B)
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
