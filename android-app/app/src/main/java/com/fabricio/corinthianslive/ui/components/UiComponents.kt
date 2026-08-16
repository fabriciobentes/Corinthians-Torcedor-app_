package com.fabricio.corinthianslive.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fabricio.corinthianslive.data.model.*
import com.fabricio.corinthianslive.ui.theme.CorinthiansColors

@Composable fun CorinthiansTopBar(title: String, subtitle: String, onRefresh: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().background(CorinthiansColors.Black).padding(horizontal = 20.dp, vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(Modifier.height(3.dp)); Text(subtitle, color = Color.White.copy(alpha = .72f))
            }
            if (onRefresh != null) IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Atualizar", tint = Color.White) }
        }
        Spacer(Modifier.height(12.dp)); Box(Modifier.width(46.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(CorinthiansColors.Red))
    }
}

@Composable fun DataStatus(source: String, generatedAt: String, notice: String?, isDemo: Boolean) {
    if (notice == null && !isDemo) return
    val text = when {
        isDemo -> "Demonstração — conecte a fonte online para ver dados reais."
        notice != null -> notice
        else -> "Atualizado em $generatedAt"
    }
    Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CorinthiansColors.Yellow.copy(alpha = .13f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Info, null, tint = CorinthiansColors.Yellow); Spacer(Modifier.width(9.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable fun LoadingState() { Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = CorinthiansColors.Red) } }

@Composable fun EmptyState(title: String, message: String, onRetry: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.SportsSoccer, null, tint = CorinthiansColors.Red, modifier = Modifier.size(42.dp)); Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp)); Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (onRetry != null) { Spacer(Modifier.height(16.dp)); OutlinedButton(onClick = onRetry) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Tentar novamente") } }
    }
}

@Composable fun AppSectionTitle(text: String) { Text(text, Modifier.padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 10.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold) }

@Composable fun AppCard(accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, accent.copy(alpha = .18f), RoundedCornerShape(18.dp)).padding(18.dp), content = content)
}

@Composable fun Pill(text: String, container: Color, content: Color) { Text(text, Modifier.clip(RoundedCornerShape(50)).background(container).padding(horizontal = 10.dp, vertical = 5.dp), color = content, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }

@Composable fun TeamBlock(team: String, highlight: Boolean, alignEnd: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        if (highlight) Pill("TIMÃO", CorinthiansColors.Red, Color.White)
        Spacer(Modifier.height(6.dp)); Text(team, textAlign = if (alignEnd) TextAlign.End else TextAlign.Start, fontWeight = if (highlight) FontWeight.Black else FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable fun InfoItem(label: String, value: String, modifier: Modifier = Modifier, alignEnd: Boolean = false) {
    Column(modifier, horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        Text(value, textAlign = if (alignEnd) TextAlign.End else TextAlign.Start, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable fun EventCard(event: MatchEvent) {
    val icon = when (event.type) {
        EventType.Goal, EventType.Shot, EventType.Penalty -> Icons.Default.SportsSoccer
        EventType.YellowCard, EventType.RedCard -> Icons.Default.Style
        EventType.Substitution -> Icons.Default.SwapHoriz
        EventType.Var -> Icons.Default.LiveTv
        EventType.Kickoff, EventType.Corner -> Icons.Default.Flag
        EventType.Foul -> Icons.Default.Warning
        EventType.Offside -> Icons.Default.SyncAlt
        EventType.Save -> Icons.Default.PanTool
        EventType.Other -> Icons.Default.Timeline
    }
    val color = when (event.type) {
        EventType.Goal, EventType.RedCard, EventType.Penalty -> CorinthiansColors.Red
        EventType.YellowCard -> CorinthiansColors.Yellow
        EventType.Shot, EventType.Save -> Color(0xFF198754)
        else -> CorinthiansColors.Black
    }
    val clock = listOf(event.clock.ifBlank { event.minute.toString() }, event.period)
        .filter { it.isNotBlank() }
        .joinToString(" ")
    Card(
        Modifier.padding(horizontal = 16.dp, vertical = 5.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = .12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    listOf(clock, event.team).filter { it.isNotBlank() }.joinToString(" • "),
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    event.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable fun TableHeader(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CorinthiansColors.Black).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        HeaderCell("Pos", Modifier.width(40.dp)); HeaderCell("Time", Modifier.weight(1f)); HeaderCell("P", Modifier.width(34.dp)); HeaderCell("J", Modifier.width(34.dp)); HeaderCell("SG", Modifier.width(42.dp))
    }
}
@Composable private fun HeaderCell(text: String, modifier: Modifier) { Box(modifier) { Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1) } }

@Composable fun StandingRow(team: Standing, highlight: Boolean, modifier: Modifier = Modifier) {
    val bg = if (highlight) CorinthiansColors.Red.copy(alpha = .12f) else MaterialTheme.colorScheme.surfaceVariant
    val border = if (highlight) CorinthiansColors.Red.copy(alpha = .72f) else MaterialTheme.colorScheme.outlineVariant
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg).border(1.dp, border, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        BodyCell(team.position.toString(), Modifier.width(40.dp), highlight)
        Column(Modifier.weight(1f)) {
            Text(team.teamName, fontWeight = if (highlight) FontWeight.Black else FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${team.wins}V • ${team.draws}E • ${team.losses}D", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BodyCell(team.points.toString(), Modifier.width(34.dp), true)
        BodyCell(team.played.toString(), Modifier.width(34.dp))
        BodyCell(if (team.goalDifference > 0) "+${team.goalDifference}" else team.goalDifference.toString(), Modifier.width(42.dp))
    }
}
@Composable private fun BodyCell(text: String, modifier: Modifier, bold: Boolean = false) { Box(modifier) { Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
