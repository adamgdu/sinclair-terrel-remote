package pl.sinclair.terrelremote.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.sinclair.terrelremote.model.AcMode
import pl.sinclair.terrelremote.model.AcState
import pl.sinclair.terrelremote.model.FanSpeed
import pl.sinclair.terrelremote.model.HorizontalSwing
import pl.sinclair.terrelremote.model.TransportMode
import pl.sinclair.terrelremote.model.VerticalSwing

private val RemoteBg = Color(0xFFEEF2F6)
private val RemoteShell = Color(0xFFFFFFFF)
private val RemoteBorder = Color(0xFFD8E0E9)
private val LcdBgOn = Color(0xFFD5E2D4)
private val LcdBgOff = Color(0xFFC9D1C9)
private val LcdInk = Color(0xFF1A281E)
private val LcdInkMuted = Color(0xFF8AA08D)
private val AccentBlue = Color(0xFF1F6FEB)
private val AccentRed = Color(0xFFDC3545)
private val AccentGreen = Color(0xFF198754)
private val ButtonSurface = Color(0xFFF7F9FC)
private val ButtonActiveSurface = Color(0xFFE3EEFF)
private val TextPrimary = Color(0xFF1E293B)
private val TextSecondary = Color(0xFF64748B)

@Composable
fun RemoteScreen(viewModel: RemoteViewModel) {
    val ui by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    fun tap(action: () -> Unit) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        action()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = RemoteBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header: Hybrid Switch + Wi-Fi Setup
            TopHeaderBar(
                transportMode = ui.transportMode,
                hasIrBlaster = ui.hasIrBlaster,
                onSelectMode = { tap { viewModel.setTransportMode(it) } },
                onOpenSetup = { tap { viewModel.openSetupDialog() } }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Main White Remote Body (Sinclair Terrel 5.3 / YAC1FB9 style)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = RemoteShell,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, RemoteBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Authentic LCD Display
                    Yac1Fb9LcdDisplay(
                        state = ui.acState,
                        transportMode = ui.transportMode,
                        onSelectMode = { mode -> tap { viewModel.selectMode(mode) } }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 1: Big Power + Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val powerActive = ui.acState.power
                        Button(
                            onClick = { tap { viewModel.togglePower() } },
                            modifier = Modifier
                                .weight(1.15f)
                                .height(58.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (powerActive) AccentRed else AccentGreen,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PowerSettingsNew,
                                contentDescription = "Zasilanie",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (powerActive) "WYŁĄCZ" else "WŁĄCZ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        RemoteActionButton(
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp),
                            title = "TRYB",
                            subtitle = ui.acState.mode.labelPl,
                            icon = modeIcon(ui.acState.mode),
                            isActive = ui.acState.power,
                            onClick = { tap { viewModel.cycleMode() } }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 2: Temperature Rocker (- / +)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = ButtonSurface,
                        border = BorderStroke(1.dp, RemoteBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FilledTempButton(
                                icon = Icons.Rounded.Remove,
                                label = "− 1°C",
                                onClick = { tap { viewModel.changeTemperature(-1) } },
                                modifier = Modifier.weight(1f)
                            )

                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "TEMP",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = if (ui.acState.heating8C) "8°C" else "${ui.acState.temperature}°C",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                            }

                            FilledTempButton(
                                icon = Icons.Rounded.Add,
                                label = "+ 1°C",
                                onClick = { tap { viewModel.changeTemperature(+1) } },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 3: FAN, TURBO, QUIET
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RemoteActionButton(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            title = "FAN",
                            subtitle = ui.acState.fanSpeed.labelPl,
                            icon = Icons.Rounded.Air,
                            isActive = ui.acState.power && ui.acState.fanSpeed != FanSpeed.AUTO,
                            onClick = { tap { viewModel.cycleFanSpeed() } }
                        )
                        RemoteActionButton(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            title = "TURBO",
                            subtitle = if (ui.acState.turbo) "WŁ" else "WYŁ",
                            icon = Icons.Rounded.Speed,
                            isActive = ui.acState.turbo,
                            onClick = { tap { viewModel.toggleTurbo() } }
                        )
                        RemoteActionButton(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            title = "QUIET",
                            subtitle = if (ui.acState.quiet) "CICHY" else "WYŁ",
                            icon = Icons.Rounded.VolumeOff,
                            isActive = ui.acState.quiet,
                            onClick = { tap { viewModel.toggleQuiet() } }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 4: SWING Vertical & Horizontal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RemoteActionButton(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            title = "SWING ↕",
                            subtitle = ui.acState.verticalSwing.shortLabel,
                            icon = Icons.Rounded.SwapVert,
                            isActive = ui.acState.verticalSwing != VerticalSwing.OFF,
                            onClick = { tap { viewModel.cycleVerticalSwing() } }
                        )
                        RemoteActionButton(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            title = "SWING ↔",
                            subtitle = ui.acState.horizontalSwing.shortLabel,
                            icon = Icons.Rounded.SwapHoriz,
                            isActive = ui.acState.horizontalSwing != HorizontalSwing.OFF,
                            onClick = { tap { viewModel.cycleHorizontalSwing() } }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 5: LIGHT, X-FAN, HEALTH
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RemoteActionButton(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            title = "LIGHT",
                            subtitle = if (ui.acState.light) "LED WŁ" else "LED WYŁ",
                            icon = Icons.Rounded.Lightbulb,
                            isActive = ui.acState.light,
                            onClick = { tap { viewModel.toggleLight() } }
                        )
                        RemoteActionButton(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            title = "X-FAN",
                            subtitle = if (ui.acState.xFan) "OSUSZ." else "WYŁ",
                            icon = Icons.Rounded.Waves,
                            isActive = ui.acState.xFan,
                            onClick = { tap { viewModel.toggleXFan() } }
                        )
                        RemoteActionButton(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            title = "HEALTH",
                            subtitle = if (ui.acState.health) "PLASMA" else "WYŁ",
                            icon = Icons.Rounded.Spa,
                            isActive = ui.acState.health,
                            onClick = { tap { viewModel.toggleHealth() } }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 6: SLEEP, 8°C, SYNC/RESEND
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RemoteActionButton(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            title = "SLEEP",
                            subtitle = if (ui.acState.sleep) "NOC WŁ" else "WYŁ",
                            icon = Icons.Rounded.Bedtime,
                            isActive = ui.acState.sleep,
                            onClick = { tap { viewModel.toggleSleep() } }
                        )
                        RemoteActionButton(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            title = "8°C",
                            subtitle = if (ui.acState.heating8C) "DYŻUR" else "WYŁ",
                            icon = Icons.Rounded.Thermostat,
                            isActive = ui.acState.heating8C,
                            onClick = { tap { viewModel.toggleHeating8C() } }
                        )
                        RemoteActionButton(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            title = if (ui.transportMode == TransportMode.WIFI) "ODŚWIEŻ" else "WYŚLIJ",
                            subtitle = if (ui.transportMode == TransportMode.WIFI) "SYNC" else "IR",
                            icon = Icons.Rounded.Refresh,
                            isActive = false,
                            onClick = { tap { viewModel.syncOrResend() } }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Subtle Sinclair logo text at bottom of white shell
                    Text(
                        text = "SINCLAIR TERREL • YAC1FB9",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom status strip
            StatusFooter(
                message = ui.statusMessage,
                isError = ui.isError,
                isBusy = ui.isBusy,
                deviceLabel = if (ui.transportMode == TransportMode.WIFI) {
                    ui.savedDevice?.let { "${it.name} • ${it.ip}" } ?: "Brak sparowanej klimy"
                } else {
                    if (ui.hasIrBlaster) "Nadajnik IR 38 kHz gotowy" else "Brak nadajnika IR"
                }
            )
        }
    }

    if (ui.showSetupDialog) {
        WifiSetupDialog(
            ui = ui,
            onDismiss = { viewModel.closeSetupDialog() },
            onScan = { ip -> viewModel.scanWifiDevices(ip) },
            onSelectDevice = { viewModel.pairDevice(it) },
            onForget = { viewModel.forgetDevice() }
        )
    }
}

@Composable
private fun TopHeaderBar(
    transportMode: TransportMode,
    hasIrBlaster: Boolean,
    onSelectMode: (TransportMode) -> Unit,
    onOpenSetup: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Hybrid Wi-Fi / IR Pill Switch
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xFFE2E8F0),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                HybridSegment(
                    modifier = Modifier.weight(1f),
                    label = "Wi-Fi LAN",
                    icon = Icons.Rounded.Wifi,
                    selected = transportMode == TransportMode.WIFI,
                    onClick = { onSelectMode(TransportMode.WIFI) }
                )
                HybridSegment(
                    modifier = Modifier.weight(1f),
                    label = if (hasIrBlaster) "IR Pilot" else "IR (brak)",
                    icon = Icons.Rounded.SettingsRemote,
                    selected = transportMode == TransportMode.IR,
                    onClick = { onSelectMode(TransportMode.IR) }
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = CircleShape,
            color = RemoteShell,
            border = BorderStroke(1.dp, RemoteBorder),
            shadowElevation = 2.dp
        ) {
            IconButton(onClick = onOpenSetup) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Ustawienia Wi-Fi",
                    tint = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun HybridSegment(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) RemoteShell else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) AccentBlue else TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) TextPrimary else TextSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun Yac1Fb9LcdDisplay(
    state: AcState,
    transportMode: TransportMode,
    onSelectMode: (AcMode) -> Unit
) {
    val bgTop = if (state.power) LcdBgOn else LcdBgOff
    val bgBottom = if (state.power) Color(0xFFC8D9C7) else Color(0xFFBFC8BF)
    val ink = if (state.power) LcdInk else Color(0xFF4F5D51)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
            .border(2.dp, Color(0xFF9EB1A0), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        // Mode strip (clickable directly for quick mode jump!)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AcMode.entries.forEach { mode ->
                val active = state.power && state.mode == mode
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectMode(mode) }
                        .background(if (active) ink.copy(alpha = 0.14f) else Color.Transparent)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = modeIcon(mode),
                        contentDescription = mode.labelPl,
                        tint = if (active) ink else LcdInkMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = mode.shortLabel,
                        fontSize = 9.sp,
                        fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (active) ink else LcdInkMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main center temperature & fan bars
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = when {
                        !state.power -> "WYŁ."
                        state.heating8C -> "8°C"
                        else -> "${state.temperature}°C"
                    },
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = ink,
                    lineHeight = 54.sp
                )
                val roomText = when {
                    transportMode == TransportMode.IR -> "Tryb IR (jednokierunkowy)"
                    state.roomTemperature != null -> "W pokoju: ${state.roomTemperature}°C"
                    else -> "Tryb Wi-Fi • brak odczytu czujnika"
                }
                Text(
                    text = roomText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ink.copy(alpha = 0.78f)
                )
            }

            // Right LCD column: Fan Bars + Turbo / Quiet
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = when {
                        !state.power -> "STANDBY"
                        state.turbo -> "TURBO"
                        state.quiet -> "QUIET"
                        state.fanSpeed == FanSpeed.AUTO -> "FAN AUTO"
                        else -> "FAN ${state.fanSpeed.bars}/5"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ink
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val activeBars = when {
                        !state.power -> 0
                        state.turbo -> 5
                        state.quiet -> 1
                        state.fanSpeed == FanSpeed.AUTO -> 3
                        else -> state.fanSpeed.bars
                    }
                    for (i in 1..5) {
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height((10 + i * 5).dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (i <= activeBars) ink else LcdInkMuted.copy(alpha = 0.45f))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "↕ ${state.verticalSwing.shortLabel}  ↔ ${state.horizontalSwing.shortLabel}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ink.copy(alpha = 0.85f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = ink.copy(alpha = 0.18f))
        Spacer(modifier = Modifier.height(8.dp))

        // Bottom active icons strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LcdBadge("LED", state.light, ink)
            LcdBadge("X-FAN", state.xFan, ink)
            LcdBadge("PLASMA", state.health, ink)
            LcdBadge("SLEEP", state.sleep, ink)
            LcdBadge("8°C", state.heating8C, ink)
        }
    }
}

@Composable
private fun LcdBadge(text: String, active: Boolean, ink: Color) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
        color = if (active) ink else LcdInkMuted.copy(alpha = 0.65f),
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (active) ink.copy(alpha = 0.14f) else Color.Transparent)
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

@Composable
private fun FilledTempButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = RemoteShell,
            contentColor = TextPrimary
        ),
        border = BorderStroke(1.dp, RemoteBorder),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun RemoteActionButton(
    modifier: Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) ButtonActiveSurface else ButtonSurface,
        border = BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) AccentBlue else RemoteBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isActive) AccentBlue else TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) AccentBlue else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = if (isActive) AccentBlue.copy(alpha = 0.85f) else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatusFooter(
    message: String,
    isError: Boolean,
    isBusy: Boolean,
    deviceLabel: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isError) Color(0xFFFDECEC) else RemoteShell,
        border = BorderStroke(1.dp, if (isError) Color(0xFFF5B7B1) else RemoteBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = AccentBlue
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isError) AccentRed else TextPrimary
                )
                Text(
                    text = deviceLabel,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun WifiSetupDialog(
    ui: RemoteUiState,
    onDismiss: () -> Unit,
    onScan: (String?) -> Unit,
    onSelectDevice: (pl.sinclair.terrelremote.model.DiscoveredAcDevice) -> Unit,
    onForget: () -> Unit
) {
    var manualIp by remember { mutableStateOf(ui.savedDevice?.ip ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Połączenie Wi-Fi (Sinclair / Gree)",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (ui.savedDevice != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ButtonActiveSurface,
                        border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sparowany: ${ui.savedDevice.name}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${ui.savedDevice.ip} • ${if (ui.savedDevice.useGcm) "GCM v2" else "ECB v1"}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            TextButton(onClick = onForget) {
                                Text("Usuń", color = AccentRed)
                            }
                        }
                    }
                }

                Button(
                    onClick = { onScan(null) },
                    enabled = !ui.isScanning,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (ui.isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Skanowanie sieci...")
                    } else {
                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Skanuj sieć Wi-Fi automatycznie")
                    }
                }

                // Manual IP option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = manualIp,
                        onValueChange = { manualIp = it },
                        label = { Text("Lub wpisz IP klimy") },
                        placeholder = { Text("np. 192.168.1.45") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = { if (manualIp.isNotBlank()) onScan(manualIp) },
                        enabled = !ui.isScanning && manualIp.isNotBlank()
                    ) {
                        Text("Szukaj")
                    }
                }

                if (ui.discoveredDevices.isNotEmpty()) {
                    Text(
                        text = "Znalezione urządzenia (kliknij, aby sparować):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    ui.discoveredDevices.forEach { dev ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectDevice(dev) },
                            shape = RoundedCornerShape(12.dp),
                            color = ButtonSurface,
                            border = BorderStroke(1.dp, RemoteBorder)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = dev.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "IP: ${dev.ip} • MAC: ${dev.mac} • ${if (dev.useGcm) "v2 GCM" else "v1 ECB"}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zamknij")
            }
        }
    )
}

private fun modeIcon(mode: AcMode): ImageVector = when (mode) {
    AcMode.AUTO -> Icons.Rounded.Autorenew
    AcMode.COOL -> Icons.Rounded.AcUnit
    AcMode.DRY -> Icons.Rounded.WaterDrop
    AcMode.FAN -> Icons.Rounded.Air
    AcMode.HEAT -> Icons.Rounded.WbSunny
}
