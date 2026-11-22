package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.security.SecurityManager
import com.example.ui.theme.CreatorSignatureFooter
import com.example.ui.theme.GlassCard
import com.example.ui.theme.LocalRrColors
import com.example.ui.theme.LocalRrDesign
import com.example.ui.theme.NebulaBackground
import com.example.ui.theme.StatusPill

@Composable
fun AppLockScreen(
    biometricEnabled: Boolean,
    onVerifyPin: (String) -> Boolean,
    onBiometricSuccess: () -> Unit
) {
    val context = LocalContext.current
    val ext = LocalRrColors.current
    val design = LocalRrDesign.current
    var enteredPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val pulseTransition = rememberInfiniteTransition(label = "lockHalo")
    val haloScale by pulseTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "haloScale"
    )

    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled && SecurityManager.canUseBiometrics(context)) {
            SecurityManager.authenticateBiometric(
                context = context,
                onSuccess = onBiometricSuccess,
                onError = { msg ->
                    if (!msg.contains("PIN", ignoreCase = true)) {
                        errorMsg = msg
                    }
                }
            )
        }
    }

    NebulaBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(8.dp))

            GlassCard(
                modifier = Modifier.widthIn(max = 420.dp),
                contentPadding = 24.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .scale(if (design.animationsEnabled) haloScale else 1f)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        ext.glowPrimary.copy(alpha = 0.28f),
                                        ext.glowSecondary.copy(alpha = 0.12f)
                                    )
                                )
                            )
                            .border(1.5.dp, Brush.linearGradient(listOf(ext.glowPrimary, ext.glowSecondary)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Locked",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    StatusPill(
                        text = "SECURITY VAULT",
                        color = MaterialTheme.colorScheme.primary,
                        icon = Icons.Rounded.VerifiedUser
                    )

                    Text(
                        text = "RR Reminder Locked",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Enter your PIN to unlock RR Reminder",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        repeat(6) { idx ->
                            val filled = idx < enteredPin.length
                            val dotColor by animateColorAsState(
                                targetValue = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                label = "pinDot"
                            )
                            val dotScale by animateFloatAsState(
                                targetValue = if (filled) 1.15f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "pinDotScale"
                            )
                            Surface(
                                modifier = Modifier
                                    .size(16.dp)
                                    .scale(dotScale),
                                shape = CircleShape,
                                color = dotColor,
                                border = BorderStroke(
                                    1.dp,
                                    if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            ) {}
                        }
                    }

                    AnimatedVisibility(visible = errorMsg != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = errorMsg.orEmpty(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("BIO", "0", "DEL")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        rows.forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                row.forEach { key ->
                                    when (key) {
                                        "BIO" -> {
                                            if (biometricEnabled && SecurityManager.canUseBiometrics(context)) {
                                                KeypadActionCircle(
                                                    icon = Icons.Rounded.Fingerprint,
                                                    contentDescription = "Biometric unlock",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    testTag = "lock_biometric_button"
                                                ) {
                                                    SecurityManager.authenticateBiometric(
                                                        context = context,
                                                        onSuccess = onBiometricSuccess,
                                                        onError = { errorMsg = it }
                                                    )
                                                }
                                            } else {
                                                Spacer(Modifier.size(66.dp))
                                            }
                                        }
                                        "DEL" -> {
                                            KeypadActionCircle(
                                                icon = Icons.AutoMirrored.Rounded.Backspace,
                                                contentDescription = "Backspace",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                testTag = "lock_backspace_button"
                                            ) {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                    errorMsg = null
                                                }
                                            }
                                        }
                                        else -> {
                                            KeypadDigitCircle(
                                                digit = key,
                                                testTag = "lock_digit_$key"
                                            ) {
                                                if (enteredPin.length < 6) {
                                                    enteredPin += key
                                                    errorMsg = null
                                                    if (enteredPin.length >= 4 && onVerifyPin(enteredPin)) {
                                                        errorMsg = null
                                                    } else if (enteredPin.length == 6) {
                                                        errorMsg = "Incorrect PIN. Try again."
                                                        enteredPin = ""
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (enteredPin.length in 4..5) {
                        TextButton(
                            onClick = {
                                if (!onVerifyPin(enteredPin)) {
                                    errorMsg = "Incorrect PIN"
                                    enteredPin = ""
                                }
                            },
                            modifier = Modifier.testTag("lock_submit_pin")
                        ) {
                            Text("Verify PIN", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            CreatorSignatureFooter()
        }
    }
}

@Composable
private fun KeypadDigitCircle(
    digit: String,
    testTag: String,
    onClick: () -> Unit
) {
    val ext = LocalRrColors.current
    val design = LocalRrDesign.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && design.animationsEnabled) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "digitScale"
    )
    Box(
        modifier = Modifier
            .size(66.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(ext.glassHighlight.copy(alpha = 0.22f), ext.glassSurface)
                )
            )
            .border(1.dp, ext.glassBorder.copy(alpha = 0.7f), CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun KeypadActionCircle(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val ext = LocalRrColors.current
    val design = LocalRrDesign.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && design.animationsEnabled) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "actionScale"
    )
    Box(
        modifier = Modifier
            .size(66.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(ext.glassSurface.copy(alpha = 0.65f))
            .border(1.dp, ext.glassBorder.copy(alpha = 0.45f), CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(26.dp))
    }
}
