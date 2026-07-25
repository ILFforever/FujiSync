package com.ilfforever.fujisync.ui.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.components.IconCamera
import com.ilfforever.fujisync.ui.components.IconEdit
import com.ilfforever.fujisync.ui.components.IconImage
import com.ilfforever.fujisync.ui.components.IconQrCode
import com.ilfforever.fujisync.ui.components.IconScan
import com.ilfforever.fujisync.ui.components.IconSort
import com.ilfforever.fujisync.ui.components.Wordmark
import com.ilfforever.fujisync.ui.detail.RecipeQrSheet
import com.ilfforever.fujisync.ui.model.LibraryRecipeUiModel
import com.ilfforever.fujisync.ui.model.RecipeUiModel
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.Metal
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextPrimary
import com.ilfforever.fujisync.ui.transfer.components.TransferActionRow
import com.ilfforever.fujisync.ui.transfer.components.TransferCreditModal
import com.ilfforever.fujisync.ui.transfer.components.TransferDivider
import com.ilfforever.fujisync.ui.transfer.components.TransferSection
import com.ilfforever.fujisync.ui.transfer.components.TransferSharePicker

/**
 * Recipe movement hub — every way a recipe gets into the library, and every way one leaves.
 * Replaces the Discover tab in lean builds; the Library "add" drawer still offers the same
 * import actions for people who start from the library.
 */
@Composable
fun TransferScreen(
    recipes: List<LibraryRecipeUiModel>,
    creditNoticeSeen: Boolean,
    onCreditNoticeSeen: () -> Unit,
    onCreateRecipe: () -> Unit,
    onImportFromPhoto: () -> Unit,
    onImportFromScreenshot: () -> Unit,
    onImportFromQr: () -> Unit,
    onScanTileGuide: () -> Unit,
    onComposeSet: () -> Unit,
) {
    var showSharePicker by remember { mutableStateOf(false) }
    var qrRecipe by remember { mutableStateOf<RecipeUiModel?>(null) }
    val hasRecipes = recipes.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Bg)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.height(28.dp), contentAlignment = Alignment.CenterStart) {
                    Wordmark()
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Text(
                    text = "TRANSFER",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    letterSpacing = 0.4.sp,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Move recipes in and out of your library.",
                    fontFamily = SansFamily,
                    fontSize = 13.sp,
                    color = TextDim,
                )

                Spacer(Modifier.height(26.dp))

                TransferSection(
                    label = "BRING IN",
                    caption = "Five ways to add a recipe.",
                ) {
                    TransferActionRow(
                        label = "Create manually",
                        subtitle = "Build a clean preset from scratch",
                        icon = IconEdit,
                        iconTint = Gold,
                        onClick = onCreateRecipe,
                    )
                    TransferDivider()
                    TransferActionRow(
                        label = "Scan screenshot",
                        subtitle = "Extract settings from a recipe image",
                        icon = IconImage,
                        iconTint = Gold,
                        tag = "OCR",
                        onClick = onImportFromScreenshot,
                    )
                    TransferDivider()
                    TransferActionRow(
                        label = "Scan QR code",
                        subtitle = "Import a shared FujiSync recipe",
                        icon = IconQrCode,
                        iconTint = Gold,
                        onClick = onImportFromQr,
                    )
                    TransferDivider()
                    TransferActionRow(
                        label = "From JPEG",
                        subtitle = "Read settings out of a camera photo",
                        icon = IconCamera,
                        iconTint = Gold,
                        tag = "EXIF",
                        onClick = onImportFromPhoto,
                    )
                    TransferDivider()
                    TransferActionRow(
                        label = "Scan tile",
                        subtitle = "Capture from another app's recipe tile",
                        icon = IconScan,
                        iconTint = Gold,
                        onClick = onScanTileGuide,
                    )
                }

                Spacer(Modifier.height(28.dp))

                TransferSection(
                    label = "SEND OUT",
                    caption = "Hand a recipe to someone, or stage a set for the camera.",
                ) {
                    TransferActionRow(
                        label = "Share a recipe",
                        subtitle = if (hasRecipes) {
                            "QR code or share card"
                        } else {
                            "Add a recipe to your library first"
                        },
                        icon = IconQrCode,
                        iconTint = Gold,
                        enabled = hasRecipes,
                        onClick = { showSharePicker = true },
                    )
                    TransferDivider()
                    TransferActionRow(
                        label = "Compose camera set",
                        subtitle = "Arrange C1–C7 and save it — restore from Camera",
                        icon = IconSort,
                        iconTint = Metal,
                        onClick = onComposeSet,
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }

        if (showSharePicker) {
            TransferSharePicker(
                recipes = recipes,
                onPick = { picked ->
                    showSharePicker = false
                    qrRecipe = picked.toShareRecipe()
                },
                onDismiss = { showSharePicker = false },
            )
        }

        qrRecipe?.let { recipe ->
            RecipeQrSheet(
                recipe = recipe,
                onDismiss = { qrRecipe = null },
            )
        }

        if (!creditNoticeSeen) {
            TransferCreditModal(onDismiss = onCreditNoticeSeen)
        }
    }
}

/** [RecipeQr] ignores the slot label, so library recipes share with an empty slot. */
private fun LibraryRecipeUiModel.toShareRecipe() = RecipeUiModel(
    libraryId = id,
    slot = "",
    name = name,
    sim = sim,
    pills = pills,
    description = description,
    effects = effects,
    tone = tone,
    wb = wb,
    saved = saved,
    sourceCameraName = sourceCameraName,
    sourceCameraModel = sourceCameraModel,
    sourceUsbId = sourceUsbId,
    sourceUrl = sourceUrl,
    sourceLabel = sourceLabel,
    referenceImageUris = referenceImageUris,
    groupIds = groupIds,
    favorite = favorite,
    isoMin = isoMin,
    isoMax = isoMax,
    exposureCompMin = exposureCompMin,
    exposureCompMax = exposureCompMax,
    sensorGens = sensorGens,
)
