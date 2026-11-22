package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.example.data.prefs.DesignSettings
import com.example.data.prefs.ThemePreset

data class RrExtendedColors(
    val glassSurface: Color,
    val glassHighlight: Color,
    val glassBorder: Color,
    val glowPrimary: Color,
    val glowSecondary: Color,
    val gradientStart: Color,
    val gradientMid: Color,
    val gradientEnd: Color,
    val success: Color,
    val warning: Color
)

val LocalRrDesign = staticCompositionLocalOf { DesignSettings() }
val LocalRrColors = staticCompositionLocalOf {
    RrExtendedColors(
        glassSurface = Color(0x3D131D42),
        glassHighlight = Color(0x18FFFFFF),
        glassBorder = Color(0x5500E5FF),
        glowPrimary = ElectricCyan,
        glowSecondary = CosmicViolet,
        gradientStart = MidnightBlack,
        gradientMid = DeepNavy,
        gradientEnd = Color(0xFF141136),
        success = SuccessMint,
        warning = WarningAmber
    )
}

@Composable
fun RrReminderTheme(
    design: DesignSettings = DesignSettings(),
    content: @Composable () -> Unit
) {
    val accent = Color(design.customAccentHex)
    val secondaryAccent = Color(design.customSecondaryHex)
    val effectiveDark = when {
        !design.isDarkMode || design.themePreset == ThemePreset.LIGHT_FROST -> false
        else -> true
    }

    val (colorScheme, extended) = resolveThemeTokens(design, accent, secondaryAccent, effectiveDark)
    val corner = design.cardCornerRadiusDp.coerceIn(10, 32)
    val shapes = Shapes(
        small = RoundedCornerShape((corner * 0.55f).dp),
        medium = RoundedCornerShape(corner.dp),
        large = RoundedCornerShape((corner + 6).dp)
    )

    CompositionLocalProvider(
        LocalRrDesign provides design,
        LocalRrColors provides extended
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildRrTypography(design.fontScale, design.fontStyle),
            shapes = shapes,
            content = content
        )
    }
}

private fun resolveThemeTokens(
    design: DesignSettings,
    customAccent: Color,
    customSecondary: Color,
    isDark: Boolean
): Pair<ColorScheme, RrExtendedColors> {
    val alpha = design.cardTransparency.coerceIn(0.10f, 0.90f)
    val glowAlphaFactor = if (design.blurEffectsEnabled) 1f else 0.55f

    if (!isDark) {
        val safeAccent = if (customAccent.luminance() > 0.78f) NeonBlue else customAccent
        val safeSec = if (customSecondary.luminance() > 0.78f) CosmicViolet else customSecondary
        val (primary, secondary, gradEnd) = when (design.themePreset) {
            ThemePreset.COSMIC_PURPLE -> Triple(if (safeAccent == ElectricCyan) CosmicViolet else safeAccent, safeSec, Color(0xFFEDE9FE))
            ThemePreset.BLACK_GOLD -> Triple(AmberGold, WarmGold, Color(0xFFFEF3C7))
            ThemePreset.NEON_CYBER -> Triple(if (safeAccent == ElectricCyan) CyberPink else safeAccent, safeSec, Color(0xFFFCE7F3))
            ThemePreset.EMERALD_AURORA -> Triple(SuccessMint, EmeraldTeal, Color(0xFFD1FAE5))
            ThemePreset.CRIMSON_PULSE -> Triple(CrimsonRose, SunsetCoral, Color(0xFFFFE4E6))
            ThemePreset.NOTHING_MONO -> Triple(if (customAccent == Color.White || customAccent.luminance() > 0.78f) Color(0xFF111827) else customAccent, NothingRed, Color(0xFFE5E7EB))
            ThemePreset.IOS_TITANIUM -> Triple(if (safeAccent == ElectricCyan) AppleBlue else safeAccent, AppleIndigo, Color(0xFFE2E8F0))
            else -> Triple(if (safeAccent == ElectricCyan) NeonBlue else safeAccent, safeSec, Color(0xFFE0ECFF))
        }
        val onPrim = if (primary.luminance() > 0.52f) Color(0xFF0A0E17) else Color.White
        val onSec = if (secondary.luminance() > 0.52f) Color(0xFF0A0E17) else Color.White
        val scheme = lightColorScheme(
            primary = primary,
            onPrimary = onPrim,
            secondary = secondary,
            onSecondary = onSec,
            tertiary = ElectricCyan,
            background = LightFrostBg,
            onBackground = LightNavyText,
            surface = Color.White.copy(alpha = (alpha + 0.55f).coerceAtMost(0.95f)),
            onSurface = LightNavyText,
            surfaceVariant = Color(0xFFE2E8F5),
            onSurfaceVariant = Color(0xFF475569),
            outline = primary.copy(alpha = 0.35f),
            error = DangerCoral
        )
        val ext = RrExtendedColors(
            glassSurface = Color.White.copy(alpha = (alpha + 0.45f).coerceAtMost(0.92f)),
            glassHighlight = Color.White.copy(alpha = 0.65f),
            glassBorder = primary.copy(alpha = 0.30f * glowAlphaFactor),
            glowPrimary = primary,
            glowSecondary = secondary,
            gradientStart = Color(0xFFEFF4FF),
            gradientMid = Color(0xFFF7FAFF),
            gradientEnd = gradEnd,
            success = SuccessMint,
            warning = WarningAmber
        )
        return scheme to ext
    }

    return when (design.themePreset) {
        ThemePreset.IOS_TITANIUM -> {
            val primary = if (customAccent == ElectricCyan) AppleBlue else customAccent
            val sec = if (customSecondary == CosmicViolet) AppleIndigo else customSecondary
            val scheme = darkColorScheme(
                primary = primary,
                onPrimary = Color.White,
                secondary = sec,
                onSecondary = Color.White,
                tertiary = SkyGlow,
                background = TitaniumBg,
                onBackground = Color(0xFFF5F7FA),
                surface = TitaniumSurface.copy(alpha = (alpha + 0.34f).coerceAtMost(0.94f)),
                onSurface = Color(0xFFF5F7FA),
                surfaceVariant = Color(0xFF1F2430),
                onSurfaceVariant = Color(0xFFA1AAB8),
                outline = Color.White.copy(alpha = 0.16f),
                error = DangerCoral
            )
            val ext = RrExtendedColors(
                glassSurface = TitaniumSurface.copy(alpha = (alpha + 0.28f).coerceAtMost(0.92f)),
                glassHighlight = Color.White.copy(alpha = 0.10f * glowAlphaFactor),
                glassBorder = Color.White.copy(alpha = 0.18f * glowAlphaFactor),
                glowPrimary = primary,
                glowSecondary = sec,
                gradientStart = Color(0xFF05070B),
                gradientMid = TitaniumBg,
                gradientEnd = Color(0xFF121828),
                success = SuccessMint,
                warning = WarningAmber
            )
            scheme to ext
        }

        ThemePreset.NOTHING_MONO -> {
            val primary = customAccent
            val sec = if (customSecondary == CosmicViolet) NothingRed else customSecondary
            val onPrim = if (primary == Color.White) Color.Black else Color.White
            val scheme = darkColorScheme(
                primary = primary,
                onPrimary = onPrim,
                secondary = sec,
                onSecondary = Color.White,
                tertiary = Color(0xFFD4D4D8),
                background = NothingBlack,
                onBackground = Color(0xFFFAFAFA),
                surface = NothingSurface.copy(alpha = (alpha + 0.36f).coerceAtMost(0.95f)),
                onSurface = Color(0xFFFAFAFA),
                surfaceVariant = Color(0xFF1C1C1F),
                onSurfaceVariant = Color(0xFFA1A1AA),
                outline = Color.White.copy(alpha = 0.26f),
                error = NothingRed
            )
            val ext = RrExtendedColors(
                glassSurface = NothingSurface.copy(alpha = (alpha + 0.30f).coerceAtMost(0.92f)),
                glassHighlight = Color.White.copy(alpha = 0.06f * glowAlphaFactor),
                glassBorder = Color.White.copy(alpha = 0.28f * glowAlphaFactor),
                glowPrimary = primary,
                glowSecondary = sec,
                gradientStart = NothingBlack,
                gradientMid = Color(0xFF09090B),
                gradientEnd = Color(0xFF131316),
                success = SuccessMint,
                warning = WarningAmber
            )
            scheme to ext
        }

        ThemePreset.BLACK_GOLD -> {
            val gold = if (customAccent == ElectricCyan) WarmGold else customAccent
            val sec = if (customSecondary == CosmicViolet) AmberGold else customSecondary
            val scheme = darkColorScheme(
                primary = gold,
                onPrimary = ObsidianBlack,
                secondary = sec,
                onSecondary = ObsidianBlack,
                tertiary = Color(0xFFFDE68A),
                background = ObsidianBlack,
                onBackground = Color(0xFFF8FAFC),
                surface = Color(0xFF15151C).copy(alpha = (alpha + 0.25f).coerceAtMost(0.92f)),
                onSurface = Color(0xFFF8FAFC),
                surfaceVariant = Color(0xFF21202A),
                onSurfaceVariant = Color(0xFFCBD5E1),
                outline = gold.copy(alpha = 0.45f),
                error = DangerCoral
            )
            val ext = RrExtendedColors(
                glassSurface = Color(0xFF171720).copy(alpha = (alpha + 0.20f).coerceAtMost(0.90f)),
                glassHighlight = gold.copy(alpha = 0.10f * glowAlphaFactor),
                glassBorder = gold.copy(alpha = 0.48f * glowAlphaFactor),
                glowPrimary = gold,
                glowSecondary = sec,
                gradientStart = Color(0xFF060609),
                gradientMid = Color(0xFF111017),
                gradientEnd = Color(0xFF231B0A),
                success = SuccessMint,
                warning = WarningAmber
            )
            scheme to ext
        }

        ThemePreset.COSMIC_PURPLE -> {
            val primary = if (customAccent == ElectricCyan) CosmicViolet else customAccent
            val sec = if (customSecondary == CosmicViolet) AuroraMagenta else customSecondary
            val scheme = darkColorScheme(
                primary = primary,
                onPrimary = Color.White,
                secondary = sec,
                onSecondary = Color.White,
                tertiary = ElectricCyan,
                background = Color(0xFF090617),
                onBackground = Color(0xFFF8FAFC),
                surface = Color(0xFF181136).copy(alpha = (alpha + 0.25f).coerceAtMost(0.92f)),
                onSurface = Color(0xFFF8FAFC),
                surfaceVariant = Color(0xFF241B4A),
                onSurfaceVariant = Color(0xFFC4B5FD),
                outline = primary.copy(alpha = 0.45f),
                error = DangerCoral
            )
            val ext = RrExtendedColors(
                glassSurface = Color(0xFF1C1440).copy(alpha = (alpha + 0.18f).coerceAtMost(0.88f)),
                glassHighlight = sec.copy(alpha = 0.12f * glowAlphaFactor),
                glassBorder = primary.copy(alpha = 0.52f * glowAlphaFactor),
                glowPrimary = primary,
                glowSecondary = sec,
                gradientStart = Color(0xFF060411),
                gradientMid = Color(0xFF140D33),
                gradientEnd = Color(0xFF2A124D),
                success = SuccessMint,
                warning = WarningAmber
            )
            scheme to ext
        }

        ThemePreset.NEON_CYBER -> {
            val primary = if (customAccent == ElectricCyan) CyberPink else customAccent
            val sec = customSecondary
            val scheme = darkColorScheme(
                primary = primary,
                onPrimary = Color.White,
                secondary = sec,
                onSecondary = MidnightBlack,
                tertiary = AuroraMagenta,
                background = Color(0xFF08041A),
                onBackground = Color(0xFFF8FAFC),
                surface = Color(0xFF160B35).copy(alpha = (alpha + 0.24f).coerceAtMost(0.92f)),
                onSurface = Color(0xFFF8FAFC),
                surfaceVariant = Color(0xFF23134F),
                onSurfaceVariant = Color(0xFFE9D5FF),
                outline = primary.copy(alpha = 0.52f),
                error = DangerCoral
            )
            val ext = RrExtendedColors(
                glassSurface = Color(0xFF160A36).copy(alpha = (alpha + 0.18f).coerceAtMost(0.88f)),
                glassHighlight = sec.copy(alpha = 0.12f * glowAlphaFactor),
                glassBorder = primary.copy(alpha = 0.58f * glowAlphaFactor),
                glowPrimary = primary,
                glowSecondary = sec,
                gradientStart = Color(0xFF070316),
                gradientMid = Color(0xFF140832),
                gradientEnd = Color(0xFF2B0B42),
                success = SuccessMint,
                warning = WarningAmber
            )
            scheme to ext
        }

        ThemePreset.EMERALD_AURORA -> {
            val primary = if (customAccent == ElectricCyan) SuccessMint else customAccent
            val sec = if (customSecondary == CosmicViolet) EmeraldTeal else customSecondary
            val scheme = darkColorScheme(
                primary = primary,
                onPrimary = MidnightBlack,
                secondary = sec,
                onSecondary = MidnightBlack,
                tertiary = SkyGlow,
                background = EmeraldBg,
                onBackground = Color(0xFFF0FDF4),
                surface = EmeraldSurface.copy(alpha = (alpha + 0.24f).coerceAtMost(0.92f)),
                onSurface = Color(0xFFF0FDF4),
                surfaceVariant = Color(0xFF113830),
                onSurfaceVariant = Color(0xFFA7F3D0),
                outline = primary.copy(alpha = 0.45f),
                error = DangerCoral
            )
            val ext = RrExtendedColors(
                glassSurface = EmeraldSurface.copy(alpha = (alpha + 0.16f).coerceAtMost(0.88f)),
                glassHighlight = sec.copy(alpha = 0.10f * glowAlphaFactor),
                glassBorder = primary.copy(alpha = 0.48f * glowAlphaFactor),
                glowPrimary = primary,
                glowSecondary = sec,
                gradientStart = Color(0xFF020E0D),
                gradientMid = Color(0xFF06201C),
                gradientEnd = Color(0xFF0C312B),
                success = SuccessMint,
                warning = WarningAmber
            )
            scheme to ext
        }

        ThemePreset.CRIMSON_PULSE -> {
            val primary = if (customAccent == ElectricCyan) CrimsonRose else customAccent
            val sec = if (customSecondary == CosmicViolet) SunsetCoral else customSecondary
            val scheme = darkColorScheme(
                primary = primary,
                onPrimary = Color.White,
                secondary = sec,
                onSecondary = MidnightBlack,
                tertiary = WarmGold,
                background = CrimsonBg,
                onBackground = Color(0xFFFFF1F2),
                surface = CrimsonSurface.copy(alpha = (alpha + 0.24f).coerceAtMost(0.92f)),
                onSurface = Color(0xFFFFF1F2),
                surfaceVariant = Color(0xFF3D1322),
                onSurfaceVariant = Color(0xFFFECDD3),
                outline = primary.copy(alpha = 0.48f),
                error = DangerCoral
            )
            val ext = RrExtendedColors(
                glassSurface = CrimsonSurface.copy(alpha = (alpha + 0.16f).coerceAtMost(0.88f)),
                glassHighlight = sec.copy(alpha = 0.10f * glowAlphaFactor),
                glassBorder = primary.copy(alpha = 0.50f * glowAlphaFactor),
                glowPrimary = primary,
                glowSecondary = sec,
                gradientStart = Color(0xFF0D0307),
                gradientMid = Color(0xFF1F0812),
                gradientEnd = Color(0xFF330F1D),
                success = SuccessMint,
                warning = WarningAmber
            )
            scheme to ext
        }

        ThemePreset.GLASSMORPHISM -> {
            val onPrim = if (customAccent.luminance() > 0.52f) MidnightBlack else Color.White
            val scheme = darkColorScheme(
                primary = customAccent,
                onPrimary = onPrim,
                secondary = customSecondary,
                onSecondary = Color.White,
                tertiary = SkyGlow,
                background = DeepNavy,
                onBackground = Color(0xFFF8FAFC),
                surface = Color(0xFF162447).copy(alpha = (alpha * 0.85f).coerceAtLeast(0.12f)),
                onSurface = Color(0xFFF8FAFC),
                surfaceVariant = Color(0xFF1E2E58),
                onSurfaceVariant = Color(0xFFCBD5E1),
                outline = Color.White.copy(alpha = 0.28f),
                error = DangerCoral
            )
            val ext = RrExtendedColors(
                glassSurface = Color.White.copy(alpha = (alpha * 0.42f).coerceIn(0.08f, 0.38f)),
                glassHighlight = Color.White.copy(alpha = 0.18f * glowAlphaFactor),
                glassBorder = Color.White.copy(alpha = 0.34f * glowAlphaFactor),
                glowPrimary = customAccent,
                glowSecondary = customSecondary,
                gradientStart = Color(0xFF060D21),
                gradientMid = Color(0xFF0F1B3D),
                gradientEnd = Color(0xFF1B1947),
                success = SuccessMint,
                warning = WarningAmber
            )
            scheme to ext
        }

        else -> {
            val onPrim = if (customAccent.luminance() > 0.52f) MidnightBlack else Color.White
            val scheme = darkColorScheme(
                primary = customAccent,
                onPrimary = onPrim,
                secondary = customSecondary,
                onSecondary = Color.White,
                tertiary = NeonBlue,
                background = MidnightBlack,
                onBackground = Color(0xFFF8FAFC),
                surface = IndigoSurface.copy(alpha = (alpha + 0.22f).coerceAtMost(0.90f)),
                onSurface = Color(0xFFF8FAFC),
                surfaceVariant = Color(0xFF18244D),
                onSurfaceVariant = Color(0xFF94A3B8),
                outline = customAccent.copy(alpha = 0.40f),
                error = DangerCoral
            )
            val ext = RrExtendedColors(
                glassSurface = IndigoSurface.copy(alpha = (alpha + 0.16f).coerceAtMost(0.88f)),
                glassHighlight = customAccent.copy(alpha = 0.10f * glowAlphaFactor),
                glassBorder = customAccent.copy(alpha = 0.46f * glowAlphaFactor),
                glowPrimary = customAccent,
                glowSecondary = customSecondary,
                gradientStart = MidnightBlack,
                gradientMid = DeepNavy,
                gradientEnd = Color(0xFF15133A),
                success = SuccessMint,
                warning = WarningAmber
            )
            scheme to ext
        }
    }
}
