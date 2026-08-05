package com.fabricio.corinthianslive.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.fabricio.corinthianslive.BuildConfig
import com.fabricio.corinthianslive.data.DataSettings
import com.fabricio.corinthianslive.notifications.GameNotificationManager
import com.fabricio.corinthianslive.notifications.NotificationPreferences
import com.fabricio.corinthianslive.ui.components.AppCard
import com.fabricio.corinthianslive.ui.components.AppSectionTitle
import com.fabricio.corinthianslive.ui.components.CorinthiansTopBar
import com.fabricio.corinthianslive.ui.theme.CorinthiansColors

@Composable
fun ConfiguracoesScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    var url by remember { mutableStateOf(DataSettings.getBaseUrl(context).ifBlank { BuildConfig.DATA_BASE_URL }) }
    var saved by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(NotificationPreferences.isEnabled(context)) }
    var permissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var notificationMessage by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        notificationMessage = if (granted) "Permissão concedida. Os avisos estão prontos." else "Permissão não concedida. Você pode tentar novamente quando quiser."
        if (granted && notificationsEnabled) GameNotificationManager.scheduleChecks(context)
    }
    val isValid = url.isBlank() || (url.startsWith("https://") && !url.endsWith(".json"))

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 18.dp
        )
    ) {
        item { CorinthiansTopBar("Configurações", "Dados e avisos do aplicativo") }

        item { AppSectionTitle("Notificações") }
        item {
            AppCard(accent = CorinthiansColors.Red) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, null, tint = CorinthiansColors.Red)
                    Spacer(Modifier.width(10.dp))
                    androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                        Text("Avisar nos dias de jogo", fontWeight = FontWeight.ExtraBold)
                        Text(
                            if (permissionGranted) "O app verifica a agenda ao longo do dia." else "Falta permitir notificações no Android.",
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
                    ) { Text("Permitir notificações") }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        notificationMessage = if (GameNotificationManager.showTest(context)) {
                            "Notificação de teste enviada."
                        } else {
                            "O Android está bloqueando os avisos. Verifique a permissão do aplicativo."
                        }
                    },
                    enabled = notificationsEnabled && permissionGranted
                ) { Text("Enviar notificação de teste") }
                notificationMessage?.let { message ->
                    Spacer(Modifier.height(10.dp))
                    Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item { AppSectionTitle("Dados online") }
        item {
            AppCard(accent = CorinthiansColors.Red) {
                Text("Endereço do GitHub Pages", fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(5.dp))
                Text(
                    "Pasta que contém fixtures.json, live.json, standings.json e stats.json.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; saved = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("https://usuario.github.io/projeto") },
                    singleLine = true,
                    isError = !isValid,
                    supportingText = { if (!isValid) Text("Use um endereço HTTPS da pasta, sem o nome do arquivo.") }
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { DataSettings.setBaseUrl(context, url); saved = true },
                    enabled = isValid && url.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = CorinthiansColors.Red)
                ) { Text("Salvar fonte") }
                if (saved) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = CorinthiansColors.Red)
                        Spacer(Modifier.width(7.dp))
                        Text("Salvo. Atualize as telas para carregar os dados.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item { AppSectionTitle("Como funciona") }
        item {
            AppCard(accent = CorinthiansColors.Black) {
                Text("O celular não armazena nenhuma chave de API.", fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(7.dp))
                Text(
                    "O GitHub atualiza a agenda de todas as competições, a tabela do Brasileirão e as estatísticas. O app mantém a última cópia para funcionar mesmo quando a conexão falhar.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
