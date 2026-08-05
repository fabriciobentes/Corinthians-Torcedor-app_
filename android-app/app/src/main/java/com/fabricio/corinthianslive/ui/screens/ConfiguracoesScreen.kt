package com.fabricio.corinthianslive.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fabricio.corinthianslive.BuildConfig
import com.fabricio.corinthianslive.data.DataSettings
import com.fabricio.corinthianslive.ui.components.AppCard
import com.fabricio.corinthianslive.ui.components.AppSectionTitle
import com.fabricio.corinthianslive.ui.components.CorinthiansTopBar
import com.fabricio.corinthianslive.ui.theme.CorinthiansColors

@Composable fun ConfiguracoesScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    var url by remember { mutableStateOf(DataSettings.getBaseUrl(context).ifBlank { BuildConfig.DATA_BASE_URL }) }
    var saved by remember { mutableStateOf(false) }
    val isValid = url.isBlank() || (url.startsWith("https://") && !url.endsWith(".json"))

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding() + 18.dp)) {
        item { CorinthiansTopBar("Configurações", "Fonte dos dados do aplicativo") }
        item { AppSectionTitle("Dados online") }
        item {
            AppCard(accent = CorinthiansColors.Red) {
                Text("Endereço do GitHub Pages", fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(5.dp))
                Text("Cole o endereço da pasta que contém fixtures.json, live.json e standings.json.", color = CorinthiansColors.GrayText, style = MaterialTheme.typography.bodyMedium)
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
                    Spacer(Modifier.height(12.dp)); Row { Icon(Icons.Default.CheckCircle, null, tint = CorinthiansColors.Red); Spacer(Modifier.width(7.dp)); Text("Salvo. Atualize as telas para carregar os dados reais.", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        item { AppSectionTitle("Como funciona") }
        item {
            AppCard(accent = CorinthiansColors.Black) {
                Text("A chave da API nunca fica no celular.", fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(7.dp)); Text("O GitHub atualiza os arquivos a cada hora. O app consulta apenas esses arquivos públicos e conserva uma cópia local para situações sem internet.", color = CorinthiansColors.GrayText)
            }
        }
    }
}
