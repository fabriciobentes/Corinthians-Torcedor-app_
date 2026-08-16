package com.fabricio.corinthianslive.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabricio.corinthianslive.R
import com.fabricio.corinthianslive.data.model.EventType
import com.fabricio.corinthianslive.data.model.MatchEvent
import com.fabricio.corinthianslive.data.model.Standing
import com.fabricio.corinthianslive.ui.theme.CorinthiansColors

@Composable
fun CorinthiansTopBar(title: String, subtitle: String, onRefresh: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(CorinthiansColors.Black, CorinthiansColors.DarkBackground)
                )
            )
            .statusBarsPadding()
            .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(54.dp).clip(RoundedCornerShape(18.dp))
                    .background(CorinthiansColors.DarkSurfaceElevated)
                    .border(1.dp, CorinthiansColors.DarkBorder, RoundedCornerShape(18.dp))
                    .padding(7.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.corinthians_crest),
                    contentDescription = "Corinthians",
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "CORINTHIANS TORCEDOR",
                    color = CorinthiansColors.Red,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.35.sp
                )
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onRefresh != null) {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(46.dp).clip(CircleShape)
                        .background(CorinthiansColors.DarkSurfaceElevated)
                        .border(1.dp, CorinthiansColors.DarkBorder, CircleShape)
                ) {
                    Icon(Icons.Default.Refresh, "Atualizar", tint = Color.White)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            subtitle,
            color = Color.White.copy(alpha = .68f),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(15.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.width(54.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(CorinthiansColors.Red)
            )
            Spacer(Modifier.width(7.dp))
            Box(
                Modifier.width(12.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(CorinthiansColors.Gold)
            )
        }
    }
}

@Composable
fun DataStatus(source: String, generatedAt: String, notice: String?, isDemo: Boolean) {
    if (notice == null && !isDemo) return
    val text = when {
        isDemo -> "Modo demonstração: dados locais em uso."
        notice != null -> notice
        else -> "Atualizado em $generatedAt"
    }
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CorinthiansColors.Yellow.copy(alpha = .1f))
            .border(1.dp, CorinthiansColors.Yellow.copy(alpha = .24f), RoundedCornerShape(16.dp))
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Info, null, tint = CorinthiansColors.Yellow)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun LoadingState() {
    Box(Modifier.fillMaxWidth().padding(54.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = CorinthiansColors.Red, trackColor = CorinthiansColors.DarkSurfaceSoft)
    }
}

@Composable
fun EmptyState(title: String, message: String, onRetry: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 46.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(68.dp).clip(CircleShape)
                .background(CorinthiansColors.Wine)
                .border(1.dp, CorinthiansColors.Red.copy(alpha = .3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SportsSoccer, null, tint = CorinthiansColors.Red, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(7.dp))
        Text(
            message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        if (onRetry != null) {
            Spacer(Modifier.height(18.dp))
            OutlinedButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(7.dp))
                Text("Tentar novamente")
            }
        }
    }
}

@Composable
fun AppSectionTitle(text: String) {
    Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 28.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.width(4.dp).height(27.dp).clip(RoundedCornerShape(3.dp))
                .background(Brush.verticalGradient(listOf(CorinthiansColors.Red, CorinthiansColors.DeepRed)))
        )
        Spacer(Modifier.width(11.dp))
        Text(
            text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Box(Modifier.width(22.dp).height(1.dp).background(CorinthiansColors.Gold.copy(alpha = .55f)))
    }
}

@Composable
fun AppCard(accent: Color, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.padding(horizontal = 16.dp, vertical = 7.dp).fillMaxWidth()
            .shadow(14.dp, shape, ambientColor = Color.Black.copy(alpha = .45f), spotColor = accent.copy(alpha = .12f))
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(colors.surface, colors.surfaceVariant.copy(alpha = .72f))
                )
            )
            .border(1.dp, accent.copy(alpha = .24f), shape)
    ) {
        Box(
            Modifier.fillMaxWidth().height(3.dp)
                .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0f))))
        )
        CompositionLocalProvider(LocalContentColor provides colors.onSurface) {
            Column(Modifier.padding(horizontal = 19.dp, vertical = 20.dp), content = content)
        }
    }
}

@Composable
fun Pill(text: String, container: Color, content: Color) {
    Text(
        text,
        Modifier.clip(RoundedCornerShape(50)).background(container)
            .border(1.dp, content.copy(alpha = .2f), RoundedCornerShape(50))
            .padding(horizontal = 11.dp, vertical = 6.dp),
        color = content,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1
    )
}

@Composable
fun TeamBlock(team: String, highlight: Boolean, alignEnd: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        if (highlight) Pill("TIMÃO", CorinthiansColors.Red, Color.White)
        Spacer(Modifier.height(8.dp))
        Text(
            team,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (highlight) FontWeight.Black else FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun InfoItem(label: String, value: String, modifier: Modifier = Modifier, alignEnd: Boolean = false) {
    Column(modifier, horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = CorinthiansColors.Gold.copy(alpha = .85f),
            letterSpacing = .7.sp
        )
        Spacer(Modifier.height(3.dp))
        Text(
            value,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private data class EventVisual(val label: String, val icon: ImageVector, val color: Color)

private fun eventVisual(type: EventType): EventVisual = when (type) {
    EventType.Goal -> EventVisual("GOL", Icons.Default.SportsSoccer, CorinthiansColors.Red)
    EventType.Penalty -> EventVisual("PÊNALTI", Icons.Default.SportsSoccer, CorinthiansColors.Red)
    EventType.YellowCard -> EventVisual("AMARELO", Icons.Default.Style, CorinthiansColors.Yellow)
    EventType.RedCard -> EventVisual("VERMELHO", Icons.Default.Style, Color(0xFFFF405C))
    EventType.Substitution -> EventVisual("SUBSTITUIÇÃO", Icons.Default.SwapHoriz, Color(0xFF5F9CFF))
    EventType.Shot -> EventVisual("FINALIZAÇÃO", Icons.Default.SportsSoccer, CorinthiansColors.Success)
    EventType.Foul -> EventVisual("FALTA", Icons.Default.Warning, Color(0xFFFF8C52))
    EventType.Corner -> EventVisual("ESCANTEIO", Icons.Default.Flag, Color(0xFFA782FF))
    EventType.Offside -> EventVisual("IMPEDIMENTO", Icons.Default.SyncAlt, Color(0xFF8E95A6))
    EventType.Save -> EventVisual("DEFESA", Icons.Default.PanTool, Color(0xFF35B9C8))
    EventType.Var -> EventVisual("VAR", Icons.Default.LiveTv, Color(0xFFA782FF))
    EventType.Kickoff -> EventVisual("JOGO", Icons.Default.Flag, CorinthiansColors.Success)
    EventType.Other -> EventVisual("LANCE", Icons.Default.Timeline, Color(0xFF8E95A6))
}

private fun eventClock(event: MatchEvent): String =
    listOf(event.clock.ifBlank { event.minute.toString() + "'" }, event.period)
        .filter { it.isNotBlank() }.joinToString(" ")

@Composable
fun EventCard(event: MatchEvent) {
    val visual = eventVisual(event.type)
    Card(
        Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth()
            .border(1.dp, visual.color.copy(alpha = .2f), RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(58.dp)) {
                Text(eventClock(event), color = visual.color, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(13.dp))
                        .background(visual.color.copy(alpha = .12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(visual.icon, null, tint = visual.color, modifier = Modifier.size(21.dp))
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Pill(visual.label, visual.color.copy(alpha = .12f), visual.color)
                if (event.team.isNotBlank()) {
                    Spacer(Modifier.height(7.dp))
                    Text(event.team, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                }
                Text(
                    event.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun CompactEventRow(event: MatchEvent) {
    val visual = eventVisual(event.type)
    Row(
        Modifier.fillMaxWidth().padding(top = 9.dp).clip(RoundedCornerShape(16.dp))
            .background(visual.color.copy(alpha = .075f))
            .border(1.dp, visual.color.copy(alpha = .16f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(visual.color.copy(alpha = .14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(visual.icon, null, tint = visual.color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(visual.label, color = visual.color, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(7.dp))
                Text(eventClock(event), fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium)
            }
            Text(
                listOf(event.team, event.description).filter { it.isNotBlank() }.joinToString(" • "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun ComparisonStat(
    label: String,
    home: Int,
    away: Int,
    suffix: String = "",
    homeIsCorinthians: Boolean = true
) {
    val leftColor = if (homeIsCorinthians) CorinthiansColors.Red else Color(0xFF686B76)
    val rightColor = if (homeIsCorinthians) Color(0xFF686B76) else CorinthiansColors.Red
    val leftWeight = home.coerceAtLeast(0).toFloat() + .35f
    val rightWeight = away.coerceAtLeast(0).toFloat() + .35f
    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("$home$suffix", fontWeight = FontWeight.Black, color = leftColor)
            Text(
                label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
            Text("$away$suffix", fontWeight = FontWeight.Black, color = rightColor)
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().height(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                Modifier.weight(leftWeight).height(5.dp).clip(RoundedCornerShape(4.dp))
                    .background(leftColor)
            )
            Box(
                Modifier.weight(rightWeight).height(5.dp).clip(RoundedCornerShape(4.dp))
                    .background(rightColor)
            )
        }
    }
}

@Composable
fun TableHeader(modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
            .background(Brush.horizontalGradient(listOf(CorinthiansColors.Black, CorinthiansColors.Wine)))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderCell("Pos", Modifier.width(40.dp))
        HeaderCell("Time", Modifier.weight(1f))
        HeaderCell("P", Modifier.width(34.dp))
        HeaderCell("J", Modifier.width(34.dp))
        HeaderCell("SG", Modifier.width(42.dp))
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier) {
    Box(modifier) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White, maxLines = 1)
    }
}

@Composable
fun StandingRow(team: Standing, highlight: Boolean, modifier: Modifier = Modifier) {
    val background = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val border = if (highlight) CorinthiansColors.Red.copy(alpha = .68f) else MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(background)
            .border(1.dp, border, RoundedCornerShape(17.dp))
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(32.dp).clip(CircleShape)
                .background(if (highlight) CorinthiansColors.Red else MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                team.position.toString(),
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.labelMedium,
                color = if (highlight) Color.White else MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                team.teamName,
                fontWeight = if (highlight) FontWeight.Black else FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${team.wins}V • ${team.draws}E • ${team.losses}D",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        BodyCell(team.points.toString(), Modifier.width(34.dp), true)
        BodyCell(team.played.toString(), Modifier.width(34.dp))
        BodyCell(if (team.goalDifference > 0) "+${team.goalDifference}" else team.goalDifference.toString(), Modifier.width(42.dp))
    }
}

@Composable
private fun BodyCell(text: String, modifier: Modifier, bold: Boolean = false) {
    Box(modifier) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Black else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
