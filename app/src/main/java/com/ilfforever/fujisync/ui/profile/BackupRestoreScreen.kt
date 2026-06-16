package com.ilfforever.fujisync.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.BackupUiState
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary

@Composable
internal fun BackupRestoreScreen(
    backup: BackupUiState,
    onExportBackup: () -> Unit,
    onImportBackupMerge: () -> Unit,
    onImportBackupReplace: () -> Unit,
    onDismissBackupMessage: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "‹",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Gold,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(end = 12.dp, top = 2.dp, bottom = 2.dp),
            )
            Text(
                text = "BACKUP & RESTORE",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                letterSpacing = 0.4.sp,
                color = TextPrimary,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "DATA",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                color = TextDim,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PanelLow)
                    .border(1.dp, Border, RoundedCornerShape(14.dp)),
            ) {
                BackupActionRow(
                    label = if (backup.exporting) "Exporting..." else "Export backup",
                    description = "Save recipes, settings, and reference images to a backup file.",
                    enabled = !backup.exporting && !backup.importing,
                    onClick = onExportBackup,
                )
                ProfileDivider()
                BackupActionRow(
                    label = if (backup.importing) "Importing..." else "Merge backup",
                    description = "Add cameras, groups, and film simulation recipes from a backup. Settings are updated.",
                    enabled = !backup.exporting && !backup.importing,
                    onClick = onImportBackupMerge,
                )
                ProfileDivider()
                BackupActionRow(
                    label = if (backup.importing) "Importing..." else "Replace all",
                    description = "Replace current settings, cameras, groups, and film simulation recipes from a backup file.",
                    enabled = !backup.exporting && !backup.importing,
                    destructive = true,
                    onClick = onImportBackupReplace,
                )
            }

            if (backup.exporting && backup.exportTotal > 0) {
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PanelLow)
                        .border(1.dp, Border, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                ) {
                    Text(
                        text = "Packing ${backup.exportTotal - 1} images…",
                        fontFamily = SansFamily,
                        fontSize = 13.sp,
                        color = TextMuted,
                    )
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { backup.exportProgress },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = Gold,
                        trackColor = Border,
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun BackupActionRow(
    label: String,
    description: String,
    enabled: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = SansFamily,
                fontSize = 14.5.sp,
                color = when {
                    !enabled -> TextDim
                    destructive -> Color(0xFFE05A4F)
                    else -> TextPrimary
                },
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = description,
                fontFamily = SansFamily,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = TextDim,
            )
        }
        Text(
            text = "›",
            fontFamily = MonoFamily,
            fontSize = 14.sp,
            color = if (enabled) TextMuted else TextDim,
        )
    }
}
