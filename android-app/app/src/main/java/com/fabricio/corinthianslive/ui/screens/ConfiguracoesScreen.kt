package com.fabricio.corinthianslive.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.fabricio.corinthianslive.data.AppThemeMode
import com.fabricio.corinthianslive.data.CorinthiansRepository
import com.fabricio.corinthianslive.notifications.GameNotificationManager
import com.fabricio.corinthianslive.notifications.NotificationPreferences
import com.fabricio.corinthianslive.ui.components.AppCard
import com.fabricio.corinthianslive.ui.components.AppSectionTitle
import com.fabricio.corinthianslive.ui.components.CorinthiansTopBar
import com.fabricio.corinthianslive.ui.theme.CorinthiansColors
import kotlinx.coroutines.launch

@Composable
fun ConfiguracoesScreen(
    contentPadding: PaddingValues,
    themeMode: AppThemeMode,
    onThemeModeChanged: (AppThemeMode) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { CorinthiansRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var notificationsEnabled by remember { mutableStateOf(NotificationPreferences.isEnabled(context)) }
    var permissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var notificationMessage by remember { mutableStateOf<String?>(null) }
    var connectionMessage by remember { mutableStateOf<String?>(null) }
    var testingConnection by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        notificationMessage = if (granted) {
            "Permissão concedida. Todos os avisos de jogo estão ativos."
        } else {
            "Permissão não concedida. Os avisos permanecem bloqueados pelo Android."
        }
        if (granted && notificationsEnabled) GameNotificationManager.scheduleChecks(context)
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 18.dp)
    ) {
        item { CorinthiansTopBar("Configurações", "Aparência, avisos e conexão automática") }

        item { AppSectionTitle("Aparência") }
        item {
            AppCard(accent = CorinthiansColors.Gold) {
                Text("MODO DO APLICATIVO", color = CorinthiansColors.Gold, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(4.dp))
                Text("Escolha como deseja visualizar", style = MaterialTheme.typography.titleMedium)
                Text(
                    "O contraste dos textos e dos cartões é ajustado automaticamente.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(13.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ThemeOption(
                        label = "Sistema",
                        icon = Icons.Default.PhoneAndroid,
                        selected = themeMode == AppThemeMode.System,
                        modifier = Modifier.weight(1f)
                    ) { onThemeModeChanged(AppThemeMode.System) }
                    ThemeOption(
                        label = "Claro",
                        icon = Icons.Default.LightMode,
                        selected = themeMode == AppThemeMode.Light,
                        modifier = Modifier.weight(1f)
                    ) { onThemeModeChanged(AppThemeMode.Light) }
                    ThemeOption(
                        label = "Escuro",
                        icon = Icons.Default.DarkMode,
                        selected = themeMode == AppThemeMode.Dark,
                        modifier = Modifier.weight(1f)
                    ) { onThemeModeChanged(AppThemeMode.Dark) }
                }
            }
        }

        item { AppSectionTitle("Notificações") }
        item {
            AppCard(accent = CorinthiansColors.Red) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, null, tint = CorinthiansColors.Red)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Notificações do Corinthians", fontWeight = FontWeight.ExtraBold)
                        Text(
                            if (permissionGranted) {
                                "30 minutos antes, início, escalações, gols, cartões, chutes, faltas e substituições."
                            } else {
                                "Falta permitir notificações no Android."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { enabled ->
                            notificationsEnabled = enabled
                            NotificationPreferences.setEnabled(context, enabled)
                            notificationMessage = null
                            if (enabled) {
                                GameNotificationManager.scheduleChecks(context)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionGranted) {
                                    NotificationPreferences.markPermissionAsked(context)
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                GameNotificationManager.cancelChecks(context)
                            }
                        }
                    )
                }
                if (notificationsEnabled && !permissionGranted) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            NotificationPreferences.markPermissionAsked(context)
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CorinthiansColors.Red)
                    ) {
                        Text("Permitir notificações")
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        notificationMessage = if (GameNotificationManager.showTest(context)) {
                            "Notificação de teste enviada."
                        } else {
                            "O Android está bloqueando os avisos."
                        }
                    },
                    enabled = notificationsEnabled && permissionGranted
                ) {
                    Text("Enviar notificação de teste")
                }
                notificationMessage?.let { message ->
                    Spacer(Modifier.height(10.dp))
                    Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item { AppSectionTitle("GitHub") }
        item {
            AppCard(accent = CorinthiansColors.Red) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDone, null, tint = CorinthiansColors.Red)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Dados vinculados automaticamente", fontWeight = FontWeight.ExtraBold)
                        Text(
                            "O app usa o repositório configurado e exibe os horários convertidos para Manaus.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        testingConnection = true
                        connectionMessage = null
                        scope.launch {
                            connectionMessage = runCatching { repository.testConnection() }
                                .getOrElse { "Falha na conexão com o GitHub: " + (it.message ?: "erro desconhecido") }
                            testingConnection = false
                        }
                    },
                    enabled = !testingConnection,
                    colors = ButtonDefaults.buttonColors(containerColor = CorinthiansColors.Red)
                ) {
                    if (testingConnection) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(18.dp).height(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (testingConnection) "Testando..." else "Testar conexão com o GitHub")
                }
                connectionMessage?.let { message ->
                    Spacer(Modifier.height(10.dp))
                    Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    FilterChip(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        leadingIcon = { Icon(icon, null) },
        label = { Text(label, fontWeight = FontWeight.ExtraBold) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
        )
    )
}
