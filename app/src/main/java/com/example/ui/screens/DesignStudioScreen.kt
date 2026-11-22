package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.prefs.BackgroundStyle
import com.example.data.prefs.CardStylePreset
import com.example.data.prefs.DashboardCardType
import com.example.data.prefs.DesignSettings
import com.example.data.prefs.FontStylePreset
import com.example.data.prefs.IconStyle
import com.example.data.prefs.ThemePreset
import com.example.ui.theme.AccentPalette
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassSegmentedTabs
import com.example.ui.theme.LocalRrColors
import com.example.ui.theme.NebulaBackground
import com.example.ui.theme.NeonGradientButton
import com.example.ui.theme.RrReminderTheme
import com.example.ui.theme.StatusPill
import java.util.Locale

private data class ThemeVisualSwatch(
    val preset: ThemePreset,
    val shortName: String,
    val topBg: Color,
    val bottomBg: Color,
    val glowColor: Color,
    val secondaryGlow: Color,
    val isLight: Boolean = false
)

private val ThemeVisualSwatches = listOf(
    ThemeVisualSwatch(ThemePreset.FUTURISTIC_BLUE, "Blue", Color(0xFF061333), Color(0xFF0B2559), Color(0xFF00E5FF), Color(0xFF8B5CF6)),
    ThemeVisualSwatch(ThemePreset.IOS_TITANIUM, "Titanium", Color(0xFF0A0E17), Color(0xFF1A2234), Color(0xFF0A84FF), Color(0xFF5E5CE6)),
    ThemeVisualSwatch(ThemePreset.NOTHING_MONO, "Nothing", Color(0xFF060606), Color(0xFF19191B), Color(0xFFFFFFFF), Color(0xFFFF3B30)),
    ThemeVisualSwatch(ThemePreset.COSMIC_PURPLE, "Purple", Color(0xFF13082A), Color(0xFF2B1254), Color(0xFF8B5CF6), Color(0xFFD946EF)),
    ThemeVisualSwatch(ThemePreset.BLACK_GOLD, "Gold", Color(0xFF0D0C10), Color(0xFF2B200B), Color(0xFFFFD166), Color(0xFFF59E0B)),
    ThemeVisualSwatch(ThemePreset.GLASSMORPHISM, "Glass", Color(0xFF0E1D3D), Color(0xFF20386B), Color(0xFF38BDF8), Color(0xFFA5F3FC)),
    ThemeVisualSwatch(ThemePreset.NEON_CYBER, "Neon", Color(0xFF12062B), Color(0xFF360B4A), Color(0xFFFF2E93), Color(0xFF00E5FF)),
    ThemeVisualSwatch(ThemePreset.EMERALD_AURORA, "Emerald", Color(0xFF041C18), Color(0xFF0D3B32), Color(0xFF10B981), Color(0xFF2DD4BF)),
    ThemeVisualSwatch(ThemePreset.CRIMSON_PULSE, "Crimson", Color(0xFF1B0710), Color(0xFF421022), Color(0xFFF43F5E), Color(0xFFFB923C)),
    ThemeVisualSwatch(ThemePreset.LIGHT_FROST, "Light", Color(0xFFE5EEFF), Color(0xFFFFFFFF), Color(0xFF2563EB), Color(0xFF8B5CF6), isLight = true)
)

private val DualToneCombos = listOf(
    Triple("Cyan × Violet", 0xFF00E5FFL, 0xFF8B5CF6L),
    Triple("Titanium × Indigo", 0xFF0A84FFL, 0xFF5E5CE6L),
    Triple("Pink × Cyan", 0xFFFF2E93L, 0xFF00E5FFL),
    Triple("Gold × Amber", 0xFFFFD166L, 0xFFF59E0BL),
    Triple("Emerald × Teal", 0xFF10B981L, 0xFF00E5FFL),
    Triple("Nothing × Red", 0xFFFFFFFFL, 0xFFFF3B30L)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesignStudioScreen(
    currentDesign: DesignSettings,
    onApplyDesign: (DesignSettings) -> Unit,
    onResetDefault: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    if (onBack != null) {
        BackHandler(onBack = onBack)
    }

    var draft by remember(currentDesign) { mutableStateOf(currentDesign) }
    var activeTab by remember { mutableIntStateOf(0) } // 0=Themes, 1=Colors, 2=Surface, 3=Layout
    val ext = LocalRrColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("design_back_button")) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(ext.glassSurface)
                                    .border(1.dp, ext.glassBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(ext.glowPrimary, ext.glowSecondary)))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "DESIGN DASHBOARD",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text("Colour & Style Studio", style = MaterialTheme.typography.headlineMedium)
                    }
                }
                IconButton(
                    onClick = {
                        draft = DesignSettings()
                        onResetDefault()
                    },
                    modifier = Modifier.size(48.dp).testTag("design_reset_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ext.glassSurface)
                            .border(1.dp, ext.glassBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset to default", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Live Interactive Multi-Element Preview
        item {
            RrReminderTheme(design = draft) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(184.dp)
                        .border(
                            1.dp,
                            Color(draft.customAccentHex).copy(alpha = 0.55f),
                            MaterialTheme.shapes.large
                        )
                ) {
                    NebulaBackground {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = 12.dp,
                                accentStripeColor = Color(draft.customAccentHex)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        StatusPill(text = "${draft.themePreset.label} • ${draft.cardStyle.label}", icon = Icons.Rounded.Palette)
                                        Spacer(Modifier.height(4.dp))
                                        Text("RR Reminder Live Preview", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            draft.fontStyle.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(draft.customAccentHex), Color(draft.customSecondaryHex))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.ColorLens, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Alpha ${(draft.cardTransparency * 100).toInt()}% • ${draft.cardCornerRadiusDp}dp • ${draft.backgroundStyle.label}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                NeonGradientButton(
                                    text = "Preview",
                                    onClick = {}
                                )
                            }
                        }
                    }
                }
            }
        }

        // Segmented Pill Studio Selector
        item {
            GlassSegmentedTabs(
                tabs = listOf("Themes", "Colors", "Surface", "Layout"),
                selectedIndex = activeTab,
                onSelect = { activeTab = it }
            )
        }

        when (activeTab) {
            0 -> {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Theme Presets", style = MaterialTheme.typography.titleLarge)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (draft.isDarkMode) "Dark" else "Light",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                Switch(
                                    checked = draft.isDarkMode,
                                    onCheckedChange = { dark ->
                                        draft = draft.copy(
                                            isDarkMode = dark,
                                            themePreset = if (!dark && draft.themePreset != ThemePreset.LIGHT_FROST) ThemePreset.LIGHT_FROST
                                            else if (dark && draft.themePreset == ThemePreset.LIGHT_FROST) ThemePreset.FUTURISTIC_BLUE
                                            else draft.themePreset
                                        )
                                    },
                                    modifier = Modifier.testTag("design_dark_mode_switch")
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))

                        val rows = ThemeVisualSwatches.chunked(2)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            rows.forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    pair.forEach { swatch ->
                                        val isSelected = draft.themePreset == swatch.preset
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(92.dp)
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(Brush.linearGradient(listOf(swatch.topBg, swatch.bottomBg)))
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) swatch.glowColor else Color.White.copy(alpha = 0.20f),
                                                    shape = RoundedCornerShape(18.dp)
                                                )
                                                .clickable {
                                                    draft = draft.copy(
                                                        themePreset = swatch.preset,
                                                        isDarkMode = !swatch.isLight,
                                                        customAccentHex = swatch.preset.defaultAccentHex,
                                                        customSecondaryHex = swatch.preset.defaultSecondaryHex
                                                    )
                                                }
                                                .testTag("theme_tile_${swatch.preset.name}"),
                                            contentAlignment = Alignment.BottomStart
                                        ) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                drawCircle(
                                                    brush = Brush.radialGradient(
                                                        colors = listOf(swatch.glowColor.copy(alpha = 0.65f), Color.Transparent),
                                                        center = Offset(size.width * 0.72f, size.height * 0.34f),
                                                        radius = size.minDimension * 0.62f
                                                    ),
                                                    radius = size.minDimension * 0.62f,
                                                    center = Offset(size.width * 0.72f, size.height * 0.34f)
                                                )
                                                drawCircle(
                                                    brush = Brush.radialGradient(
                                                        colors = listOf(swatch.secondaryGlow.copy(alpha = 0.45f), Color.Transparent),
                                                        center = Offset(size.width * 0.25f, size.height * 0.22f),
                                                        radius = size.minDimension * 0.48f
                                                    ),
                                                    radius = size.minDimension * 0.48f,
                                                    center = Offset(size.width * 0.25f, size.height * 0.22f)
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = swatch.shortName,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = if (swatch.isLight) Color(0xFF0F172A) else Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (isSelected) {
                                                    Surface(
                                                        shape = RoundedCornerShape(50),
                                                        color = swatch.glowColor.copy(alpha = 0.25f),
                                                        border = BorderStroke(1.dp, swatch.glowColor)
                                                    ) {
                                                        Text(
                                                            "Active",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (swatch.isLight) swatch.glowColor else Color.White,
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Primary Accent Colour", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Controls buttons, icons, highlights, and active borders",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            AccentPalette.forEach { (hex, label) ->
                                val selected = draft.customAccentHex == hex
                                Surface(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .clickable { draft = draft.copy(customAccentHex = hex) },
                                    shape = CircleShape,
                                    color = Color(hex),
                                    border = if (selected) BorderStroke(3.dp, Color.White) else BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                                ) {
                                    if (selected) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Check, contentDescription = label, tint = Color.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Secondary Gradient Glow", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Pairs with your primary accent across gradients and card rims",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            AccentPalette.forEach { (hex, label) ->
                                val selected = draft.customSecondaryHex == hex
                                Surface(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .clickable { draft = draft.copy(customSecondaryHex = hex) },
                                    shape = CircleShape,
                                    color = Color(hex),
                                    border = if (selected) BorderStroke(3.dp, Color.White) else BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                                ) {
                                    if (selected) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Check, contentDescription = label, tint = Color.Black)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("Dual-Tone Gradient Combos", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DualToneCombos.forEach { (name, pHex, sHex) ->
                                val active = draft.customAccentHex == pHex && draft.customSecondaryHex == sHex
                                FilterChip(
                                    selected = active,
                                    onClick = { draft = draft.copy(customAccentHex = pHex, customSecondaryHex = sHex) },
                                    label = { Text(name) }
                                )
                            }
                        }
                    }
                }
            }

            2 -> {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Card Surface Style", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CardStylePreset.entries.forEach { cs ->
                                FilterChip(
                                    selected = draft.cardStyle == cs,
                                    onClick = { draft = draft.copy(cardStyle = cs) },
                                    label = { Text(cs.label) }
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Text("Wallpaper Style", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BackgroundStyle.entries.forEach { bg ->
                                FilterChip(
                                    selected = draft.backgroundStyle == bg,
                                    onClick = { draft = draft.copy(backgroundStyle = bg) },
                                    label = { Text(bg.label) }
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Glass Transparency", style = MaterialTheme.typography.titleMedium)
                            Text("${(draft.cardTransparency * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = draft.cardTransparency,
                            onValueChange = { draft = draft.copy(cardTransparency = it) },
                            valueRange = 0.10f..0.85f
                        )

                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Card Corner Roundness", style = MaterialTheme.typography.titleMedium)
                            Text("${draft.cardCornerRadiusDp} dp", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = draft.cardCornerRadiusDp.toFloat(),
                            onValueChange = { draft = draft.copy(cardCornerRadiusDp = it.toInt()) },
                            valueRange = 10f..32f
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Glowing Card Borders", style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = draft.borderGlowEnabled,
                                onCheckedChange = { draft = draft.copy(borderGlowEnabled = it) }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Blur & Glow Effects", style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = draft.blurEffectsEnabled,
                                onCheckedChange = { draft = draft.copy(blurEffectsEnabled = it) }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Animations", style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = draft.animationsEnabled,
                                onCheckedChange = { draft = draft.copy(animationsEnabled = it) }
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Animation Speed (${String.format(Locale.US, "%.1fx", draft.animationSpeed)})")
                        Slider(
                            value = draft.animationSpeed,
                            onValueChange = { draft = draft.copy(animationSpeed = it) },
                            valueRange = 0.5f..2.0f
                        )
                    }
                }
            }

            3 -> {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Typography & Font Family", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FontStylePreset.entries.forEach { font ->
                                FilterChip(
                                    selected = draft.fontStyle == font,
                                    onClick = { draft = draft.copy(fontStyle = font) },
                                    label = { Text(font.label) }
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Font Size Scale (${(draft.fontScale * 100).toInt()}%)")
                        Slider(
                            value = draft.fontScale,
                            onValueChange = { draft = draft.copy(fontScale = it) },
                            valueRange = 0.85f..1.30f
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Compact Dashboard Widgets")
                            Switch(
                                checked = draft.compactWidgets,
                                onCheckedChange = { draft = draft.copy(compactWidgets = it) }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Icon Style", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconStyle.entries.forEach { style ->
                                FilterChip(
                                    selected = draft.iconStyle == style,
                                    onClick = { draft = draft.copy(iconStyle = style) },
                                    label = { Text(style.label) }
                                )
                            }
                        }
                    }
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Dashboard Card Order & Visibility", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(6.dp))
                        draft.dashboardOrder.forEachIndexed { index, cardId ->
                            val card = DashboardCardType.entries.firstOrNull { it.id == cardId } ?: return@forEachIndexed
                            val visible = !draft.hiddenCards.contains(card.id)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(card.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                IconButton(
                                    enabled = index > 0,
                                    onClick = {
                                        val mutable = draft.dashboardOrder.toMutableList()
                                        val moved = mutable.removeAt(index)
                                        mutable.add(index - 1, moved)
                                        draft = draft.copy(dashboardOrder = mutable)
                                    }
                                ) { Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up") }
                                IconButton(
                                    enabled = index < draft.dashboardOrder.lastIndex,
                                    onClick = {
                                        val mutable = draft.dashboardOrder.toMutableList()
                                        val moved = mutable.removeAt(index)
                                        mutable.add(index + 1, moved)
                                        draft = draft.copy(dashboardOrder = mutable)
                                    }
                                ) { Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down") }
                                Switch(
                                    checked = visible,
                                    onCheckedChange = { show ->
                                        val next = if (show) draft.hiddenCards - card.id else draft.hiddenCards + card.id
                                        draft = draft.copy(hiddenCards = next)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Apply / Reset CTA bar
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeonGradientButton(
                    text = "Apply Design",
                    icon = Icons.Rounded.Check,
                    onClick = { onApplyDesign(draft) },
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("design_apply_button")
                )
                OutlinedButton(
                    onClick = {
                        draft = DesignSettings()
                        onResetDefault()
                    },
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Text("Reset")
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}
