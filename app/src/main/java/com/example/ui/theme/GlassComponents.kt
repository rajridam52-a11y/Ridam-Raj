package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.prefs.BackgroundStyle
import com.example.data.prefs.CardStylePreset
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NebulaBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val design = LocalRrDesign.current
    val ext = LocalRrColors.current
    val durationMs = (16000f / design.animationSpeed.coerceIn(0.5f, 2.0f)).toInt()
    val infinite = rememberInfiniteTransition(label = "ambientBg")
    val drift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(durationMs, easing = LinearEasing)),
        label = "ambientDrift"
    )
    val driftX = if (design.animationsEnabled) cos(drift) * 0.045f else 0f
    val driftY = if (design.animationsEnabled) sin(drift) * 0.035f else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ext.gradientStart, ext.gradientMid, ext.gradientEnd)
                )
            )
    ) {
        when {
            design.backgroundStyle == BackgroundStyle.NEBULA_ART && design.isDarkMode -> {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_nebula),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.22f)
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c1 = Offset(size.width * (0.84f + driftX), size.height * (0.14f + driftY))
                    val c2 = Offset(size.width * (0.16f - driftX), size.height * (0.76f - driftY))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(ext.glowPrimary.copy(alpha = 0.24f), Color.Transparent),
                            center = c1,
                            radius = size.width * 0.68f
                        ),
                        radius = size.width * 0.68f,
                        center = c1
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(ext.glowSecondary.copy(alpha = 0.16f), Color.Transparent),
                            center = c2,
                            radius = size.width * 0.62f
                        ),
                        radius = size.width * 0.62f,
                        center = c2
                    )
                }
            }

            design.backgroundStyle == BackgroundStyle.AURORA_GRADIENT -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c1 = Offset(size.width * (0.85f + driftX), size.height * (0.14f + driftY))
                    val c2 = Offset(size.width * (0.15f - driftX), size.height * (0.78f - driftY))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(ext.glowPrimary.copy(alpha = 0.28f), Color.Transparent),
                            center = c1,
                            radius = size.width * 0.70f
                        ),
                        radius = size.width * 0.70f,
                        center = c1
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(ext.glowSecondary.copy(alpha = 0.24f), Color.Transparent),
                            center = c2,
                            radius = size.width * 0.72f
                        ),
                        radius = size.width * 0.72f,
                        center = c2
                    )
                }
            }

            design.backgroundStyle == BackgroundStyle.STARFIELD_GRID -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = 36.dp.toPx()
                    val gridColor = ext.glowPrimary.copy(alpha = 0.08f)
                    var x = 0f
                    while (x <= size.width) {
                        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                        x += step
                    }
                    var y = 0f
                    while (y <= size.height) {
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                        y += step
                    }
                    val center = Offset(size.width * (0.5f + driftX), size.height * (0.18f + driftY))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(ext.glowPrimary.copy(alpha = 0.24f), Color.Transparent),
                            center = center,
                            radius = size.width * 0.75f
                        ),
                        radius = size.width * 0.75f,
                        center = center
                    )
                }
            }
        }
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp? = null,
    glowBorder: Boolean = true,
    accentStripeColor: Color? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val ext = LocalRrColors.current
    val design = LocalRrDesign.current
    val resolvedRadius = cornerRadius ?: design.cardCornerRadiusDp.dp
    val shape = RoundedCornerShape(resolvedRadius)
    val effectiveGlow = glowBorder && design.borderGlowEnabled

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null && design.animationsEnabled) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardPressScale"
    )

    val borderBrush = when (design.cardStyle) {
        CardStylePreset.NOTHING_OUTLINE -> Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
        )
        CardStylePreset.IOS_ELEVATED -> Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = if (design.isDarkMode) 0.14f else 0.85f),
                Color.White.copy(alpha = if (design.isDarkMode) 0.05f else 0.45f)
            )
        )
        CardStylePreset.NEON_GRADIENT -> Brush.linearGradient(
            listOf(
                ext.glowPrimary.copy(alpha = if (effectiveGlow) 0.85f else 0.45f),
                ext.glowSecondary.copy(alpha = if (effectiveGlow) 0.85f else 0.45f)
            )
        )
        CardStylePreset.GLASS_FROST -> if (effectiveGlow) {
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = if (design.isDarkMode) 0.30f else 0.72f),
                    ext.glassBorder,
                    ext.glowSecondary.copy(alpha = if (design.blurEffectsEnabled) 0.36f else 0.18f),
                    ext.glassBorder.copy(alpha = 0.45f)
                )
            )
        } else {
            Brush.linearGradient(listOf(ext.glassBorder.copy(alpha = 0.22f), ext.glassBorder.copy(alpha = 0.14f)))
        }
    }

    val interiorBrush = when (design.cardStyle) {
        CardStylePreset.IOS_ELEVATED -> Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = if (design.isDarkMode) 0.90f else 0.96f),
                MaterialTheme.colorScheme.surface.copy(alpha = if (design.isDarkMode) 0.82f else 0.92f)
            )
        )
        CardStylePreset.NOTHING_OUTLINE -> Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = if (design.isDarkMode) 0.76f else 0.90f),
                MaterialTheme.colorScheme.surface.copy(alpha = if (design.isDarkMode) 0.72f else 0.86f)
            )
        )
        else -> Brush.linearGradient(
            colors = listOf(
                ext.glassHighlight.copy(alpha = (ext.glassHighlight.alpha + 0.08f).coerceAtMost(0.50f)),
                ext.glassSurface,
                ext.glassSurface.copy(alpha = (ext.glassSurface.alpha + 0.06f).coerceAtMost(0.95f))
            )
        )
    }

    val elevation = when (design.cardStyle) {
        CardStylePreset.IOS_ELEVATED -> 6.dp
        else -> 0.dp
    }

    Surface(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(interiorBrush, shape)
            .border(
                width = if (design.cardStyle == CardStylePreset.NEON_GRADIENT || design.cardStyle == CardStylePreset.NOTHING_OUTLINE) 1.25.dp else 1.dp,
                brush = borderBrush,
                shape = shape
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = androidx.compose.material3.ripple(),
                        onClick = onClick
                    )
                } else Modifier
            ),
        shape = shape,
        color = Color.Transparent,
        shadowElevation = elevation,
        tonalElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (design.cardStyle == CardStylePreset.GLASS_FROST || design.cardStyle == CardStylePreset.NEON_GRADIENT) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = if (design.isDarkMode) 0.24f else 0.70f),
                                    ext.glowPrimary.copy(alpha = if (effectiveGlow) 0.45f else 0.16f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
            if (accentStripeColor != null) {
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .padding(vertical = 10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(accentStripeColor, accentStripeColor.copy(alpha = 0.55f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier.weight(1f).padding(contentPadding),
                        content = content
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(contentPadding),
                    content = content
                )
            }
        }
    }
}

@Composable
fun NeonGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val ext = LocalRrColors.current
    val design = LocalRrDesign.current
    val shape = RoundedCornerShape(50)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && design.animationsEnabled) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btnPressScale"
    )
    val brush = if (enabled) {
        Brush.horizontalGradient(listOf(ext.glowPrimary, ext.glowSecondary))
    } else {
        Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
    val contentTint = if (!enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else if ((ext.glowPrimary.luminance() + ext.glowSecondary.luminance()) / 2f > 0.62f) {
        Color(0xFF0A0E17)
    } else {
        Color.White
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(brush, shape)
            .border(1.dp, Color.White.copy(alpha = 0.34f), shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentTint,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = contentTint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GlassSegmentedTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "seg_tab"
) {
    val ext = LocalRrColors.current
    val design = LocalRrDesign.current
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        contentPadding = 5.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { idx, label ->
                val selected = idx == selectedIndex
                val bgColor by animateColorAsState(
                    targetValue = if (selected) ext.glowPrimary.copy(alpha = 0.22f) else Color.Transparent,
                    animationSpec = tween(if (design.animationsEnabled) 220 else 0),
                    label = "segBg"
                )
                val borderColor = if (selected) ext.glowPrimary.copy(alpha = 0.65f) else Color.Transparent
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(15.dp))
                        .background(bgColor)
                        .border(1.dp, borderColor, RoundedCornerShape(15.dp))
                        .clickable { onSelect(idx) }
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ScreenOverlineHeader(
    overline: String,
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    val ext = LocalRrColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(ext.glowPrimary, ext.glowSecondary)))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = overline.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        trailing?.invoke()
    }
}

@Composable
fun RadialProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    strokeWidth: Dp = 8.dp,
    primaryColor: Color = LocalRrColors.current.glowPrimary,
    secondaryColor: Color = LocalRrColors.current.glowSecondary,
    centerContent: @Composable BoxScope.() -> Unit
) {
    val design = LocalRrDesign.current
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(if (design.animationsEnabled) 550 else 0),
        label = "ringProgress"
    )
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(
                color = primaryColor.copy(alpha = 0.16f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            if (animatedProgress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(primaryColor, secondaryColor, primaryColor)),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        centerContent()
    }
}

@Composable
fun PremiumEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val ext = LocalRrColors.current
    GlassCard(modifier = modifier.fillMaxWidth(), contentPadding = 24.dp) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                ext.glowPrimary.copy(alpha = 0.26f),
                                ext.glowSecondary.copy(alpha = 0.10f)
                            )
                        )
                    )
                    .border(1.dp, ext.glowPrimary.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(4.dp))
                NeonGradientButton(
                    text = actionLabel,
                    onClick = onAction
                )
            }
        }
    }
}

@Composable
fun PremiumLoadingCard(
    message: String,
    modifier: Modifier = Modifier
) {
    val design = LocalRrDesign.current
    val ext = LocalRrColors.current
    val transition = rememberInfiniteTransition(label = "pulseLoad")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (design.animationsEnabled) 900 else 1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ext.glowPrimary.copy(alpha = 0.18f * pulse))
                    .border(2.dp, ext.glowPrimary.copy(alpha = pulse), CircleShape)
            )
            Column {
                Text(message, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Syncing real-time telemetry...",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val ext = LocalRrColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ext.glowPrimary.copy(alpha = 0.16f))
                        .border(0.75.dp, ext.glowPrimary.copy(alpha = 0.36f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        trailing?.invoke()
    }
}

@Composable
fun CreatorSignatureFooter(
    modifier: Modifier = Modifier
) {
    val ext = LocalRrColors.current
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            ext.glowPrimary.copy(alpha = 0.40f),
                            ext.glowSecondary.copy(alpha = 0.40f),
                            Color.Transparent
                        )
                    )
                )
        )
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.linearGradient(listOf(ext.glowPrimary, ext.glowSecondary)))
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = "Created by ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Ridam Raj",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    color: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector? = null
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.48f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
    }
}

@Composable
fun WaveformVisualizerBar(
    isActive: Boolean,
    rmsDb: Float = 0f,
    barCount: Int = 14,
    modifier: Modifier = Modifier
) {
    val design = LocalRrDesign.current
    val ext = LocalRrColors.current
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isActive) 900 else 2400, easing = LinearEasing)
        ),
        label = "wavePhase"
    )

    Canvas(modifier = modifier.height(34.dp)) {
        val barWidth = (size.width / (barCount * 1.9f)).coerceIn(2.dp.toPx(), 5.dp.toPx())
        val gap = (size.width - barWidth * barCount) / (barCount - 1).coerceAtLeast(1)
        val ampBoost = if (isActive) (0.45f + (rmsDb / 24f).coerceIn(0f, 0.55f)) else 0.22f

        for (i in 0 until barCount) {
            val factor = if (design.animationsEnabled) {
                val raw = sin(phase + i * 0.65f)
                (0.25f + 0.75f * kotlin.math.abs(raw)) * ampBoost
            } else 0.35f
            val h = (size.height * factor).coerceAtLeast(4.dp.toPx())
            val x = i * (barWidth + gap)
            val y = (size.height - h) / 2f
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(ext.glowPrimary, ext.glowSecondary)),
                topLeft = Offset(x, y),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
