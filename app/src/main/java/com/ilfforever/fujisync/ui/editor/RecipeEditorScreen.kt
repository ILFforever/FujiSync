package com.ilfforever.fujisync.ui.editor

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.data.ptp.CameraPresetName
import com.ilfforever.fujisync.domain.model.FujiFilmSimulation
import com.ilfforever.fujisync.domain.model.canonicalFilmSimLabel
import com.ilfforever.fujisync.ui.components.decodeSampledBitmap
import com.ilfforever.fujisync.ui.components.FilmSimLabel
import com.ilfforever.fujisync.ui.components.IconCamera
import com.ilfforever.fujisync.ui.components.IconCC
import com.ilfforever.fujisync.ui.components.IconCCFXBlue
import com.ilfforever.fujisync.ui.components.IconCheck
import com.ilfforever.fujisync.ui.components.IconClarity
import com.ilfforever.fujisync.ui.components.IconClose
import com.ilfforever.fujisync.ui.components.IconColor
import com.ilfforever.fujisync.ui.components.IconDR
import com.ilfforever.fujisync.ui.components.IconExposureComp
import com.ilfforever.fujisync.ui.components.IconGrain
import com.ilfforever.fujisync.ui.components.IconHighlight
import com.ilfforever.fujisync.ui.components.IconISO
import com.ilfforever.fujisync.ui.components.IconNR
import com.ilfforever.fujisync.ui.components.IconShadow
import com.ilfforever.fujisync.ui.components.IconSharpness
import com.ilfforever.fujisync.ui.components.IconSmoothSkin
import com.ilfforever.fujisync.ui.components.IconWB
import com.ilfforever.fujisync.ui.components.IconWBShift
import com.ilfforever.fujisync.ui.components.Pill
import com.ilfforever.fujisync.ui.library.normalizedDRangePriorityLabel
import com.ilfforever.fujisync.ui.library.normalizedDynamicRangeLabel
import com.ilfforever.fujisync.ui.model.RecipeUiModel
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelHigh
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.haptics.FujiHapticEffect
import com.ilfforever.fujisync.ui.haptics.FujiHaptics
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val monoSims = setOf(
    "Monochrome",
    "Monochrome + Y",
    "Monochrome + R",
    "Monochrome + G",
    "Sepia",
    "Acros",
    "Acros + Y",
    "Acros + R",
    "Acros + G",
)

private const val RECIPE_NAME_FALLBACK = "Untitled Recipe"

private val dRangePriorityOptions = listOf("Off", "Strong", "Weak", "Auto")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeEditorScreen(
    initialRecipe: RecipeUiModel?,
    referenceImageUris: List<String>,
    cameraModel: String,
    maxReferenceImages: Int = 20,
    onClose: () -> Unit,
    onDirtyChange: (Boolean) -> Unit,
    onAddReferenceImage: () -> Unit,
    onRemoveReferenceImage: (String) -> Unit,
    onSave: (RecipeUiModel) -> Unit,
) {
    val seed = initialRecipe ?: defaultRecipe()
    var name by remember(seed) {
        mutableStateOf(
            seed.name
                .takeUnless { it == "New Recipe" }
                ?.let(CameraPresetName::sanitizeForEditing)
                .orEmpty(),
        )
    }
    var description by remember(seed) { mutableStateOf(seed.description) }
    var sim by remember(seed) { mutableStateOf(seed.sim.canonicalFilmSimLabel().ifBlank { FujiFilmSimulation.Provia.label }) }
    var simFamily by remember(seed) { mutableStateOf(filmSimFamilyFor(seed.sim.canonicalFilmSimLabel().ifBlank { FujiFilmSimulation.Provia.label }).label) }
    var dRangePriority by remember(seed) { mutableStateOf((seed.effects["D Range Priority"] ?: "Off").normalizedDRangePriorityLabel()) }
    var dynamicRange by remember(seed) { mutableStateOf((seed.effects["Dynamic Range"] ?: "DR Auto").normalizedDynamicRangeLabel()) }
    var grain by remember(seed) { mutableStateOf(seed.effects["Grain Effect"] ?: "Off") }
    var colorChrome by remember(seed) { mutableStateOf(seed.effects["Color Chrome"]?.takeUnless { it == "—" } ?: "Off") }
    var colorChromeBlue by remember(seed) { mutableStateOf(seed.effects["Color Chrome FX Blue"]?.takeUnless { it == "—" } ?: "Off") }
    var smoothSkin by remember(seed) { mutableStateOf(seed.effects["Smooth Skin"] ?: "Off") }
    var whiteBalance by remember(seed) { mutableStateOf(seed.wb["White Balance"].editorWhiteBalanceMode()) }
    var colorTemp by remember(seed) { mutableIntStateOf(seed.wb["White Balance"]?.removeSuffix("K")?.toIntOrNull() ?: 5600) }
    var wbRed by remember(seed) { mutableIntStateOf(seed.wb["WB Shift R"].signedIntOrZero()) }
    var wbBlue by remember(seed) { mutableIntStateOf(seed.wb["WB Shift B"].signedIntOrZero()) }
    var highlight by remember(seed) { mutableStateOf(seed.tone["Highlight Tone"].signedFloatOrZero()) }
    var shadow by remember(seed) { mutableStateOf(seed.tone["Shadow Tone"].signedFloatOrZero()) }
    var color by remember(seed) { mutableIntStateOf(seed.tone["Color"].signedIntOrZero()) }
    var sharpness by remember(seed) { mutableIntStateOf(seed.tone["Sharpness"].signedIntOrZero()) }
    var highIsoNr by remember(seed) { mutableIntStateOf(seed.tone["High ISO NR"].signedIntOrZero()) }
    var clarity by remember(seed) { mutableIntStateOf(seed.tone["Clarity"].signedIntOrZero()) }
    var monoWc by remember(seed) { mutableIntStateOf(seed.tone["Mono WC"].signedIntOrZero()) }
    var monoMg by remember(seed) { mutableIntStateOf(seed.tone["Mono MG"].signedIntOrZero()) }
    var isoMin by remember(seed) { mutableStateOf(seed.isoMin) }
    var isoMax by remember(seed) { mutableStateOf(seed.isoMax) }
    var exposureCompMin by remember(seed) { mutableStateOf(seed.exposureCompMin) }
    var exposureCompMax by remember(seed) { mutableStateOf(seed.exposureCompMax) }
    var sensorGens by remember(seed) { mutableStateOf(seed.sensorGens) }

    val isMono = sim in monoSims
    val supportsSmoothSkin = !cameraModel.contains("X-Pro3", ignoreCase = true)
    val supportsClarity = !cameraModel.contains("X-T30", ignoreCase = true)
    val actualWhiteBalance = if (whiteBalance == "Color Temperature") "${colorTemp}K" else whiteBalance
    val draft = remember(
        seed,
        name,
        description,
        sim,
        dRangePriority,
        dynamicRange,
        grain,
        colorChrome,
        colorChromeBlue,
        smoothSkin,
        actualWhiteBalance,
        wbRed,
        wbBlue,
        highlight,
        shadow,
        color,
        sharpness,
        highIsoNr,
        clarity,
        monoWc,
        monoMg,
        referenceImageUris,
        isMono,
        supportsSmoothSkin,
        supportsClarity,
        isoMin,
        isoMax,
        exposureCompMin,
        exposureCompMax,
        sensorGens,
    ) {
        buildRecipe(
            base = seed,
            name = CameraPresetName.sanitizeOrFallback(name, RECIPE_NAME_FALLBACK),
            description = description.trim(),
            sim = sim,
            dRangePriority = dRangePriority,
            dynamicRange = dynamicRange,
            grain = grain,
            colorChrome = colorChrome,
            colorChromeBlue = colorChromeBlue,
            smoothSkin = if (supportsSmoothSkin) smoothSkin else "Off",
            whiteBalance = actualWhiteBalance,
            wbRed = wbRed,
            wbBlue = wbBlue,
            highlight = highlight,
            shadow = shadow,
            color = color,
            sharpness = sharpness,
            highIsoNr = highIsoNr,
            clarity = if (supportsClarity) clarity else 0,
            monoWc = monoWc,
            monoMg = monoMg,
            referenceImageUris = referenceImageUris,
            isMono = isMono,
            isoMin = isoMin,
            isoMax = isoMax,
            exposureCompMin = exposureCompMin,
            exposureCompMax = exposureCompMax,
            sensorGens = sensorGens,
        )
    }
    val cleanSeed = remember(seed, initialRecipe) {
        val cleanName = if (initialRecipe?.libraryId == null) {
            RECIPE_NAME_FALLBACK
        } else {
            CameraPresetName.sanitizeOrFallback(seed.name, RECIPE_NAME_FALLBACK)
        }
        seed.copy(name = cleanName)
    }
    val dirty = draft != cleanSeed
    val canSave = CameraPresetName.sanitize(name).isNotBlank()

    LaunchedEffect(dirty) {
        onDirtyChange(dirty)
    }

    BackHandler(onBack = onClose)

    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            EditorHeader(
                title = if (initialRecipe?.libraryId == null) "CREATE RECIPE" else "EDIT RECIPE",
                onClose = onClose,
                canSave = canSave,
                onSave = { if (canSave) onSave(draft) },
            )

            Box(Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 14.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(PanelLow)
                            .border(1.dp, Border, RoundedCornerShape(16.dp))
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                    ) {
                        FilmSimLabel(sim = sim, imageSize = 28.dp)
                        Spacer(Modifier.height(12.dp))
                        EditorTextField(
                            value = name,
                            onValueChange = { name = CameraPresetName.sanitizeForEditing(it) },
                            placeholder = "Recipe name",
                            textStyle = TextStyle(
                                fontFamily = SansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp,
                                lineHeight = 38.sp,
                                color = TextPrimary,
                            ),
                        )
                        Spacer(Modifier.height(14.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            draft.pills.take(4).forEach { Pill(text = it, large = true) }
                        }
                        Spacer(Modifier.height(14.dp))
                        EditorTextField(
                            value = description,
                            onValueChange = { description = it.take(260) },
                            placeholder = "Notes or look description",
                            singleLine = false,
                            minHeight = 72.dp,
                            textStyle = TextStyle(
                                fontFamily = SansFamily,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = TextPrimary,
                            ),
                        )
                        EditorReferenceImages(
                            referenceImageUris = referenceImageUris,
                            maxReferenceImages = maxReferenceImages,
                            onAddReferenceImage = onAddReferenceImage,
                            onRemoveReferenceImage = onRemoveReferenceImage,
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    EditorSection("Film Simulation") {
                        FilmSimulationPicker(
                            selected = sim,
                            selectedFamily = simFamily,
                            onFamilySelect = { simFamily = it },
                            onSelect = {
                                sim = it
                                simFamily = filmSimFamilyFor(it).label
                            },
                        )
                    }

                    EditorSection("Effects") {
                        ChipControl("D Range Priority", IconDR, dRangePriorityOptions, dRangePriority) { dRangePriority = it }
                        ChipControl(
                            label = "Dynamic Range",
                            icon = IconDR,
                            options = listOf("DR Auto", "DR100%", "DR200%", "DR400%"),
                            selected = dynamicRange,
                            enabled = dRangePriority == "Off",
                        ) { dynamicRange = it }
                        GrainEffectControl(IconGrain, grain) { grain = it }
                        if (!isMono) {
                            ChipControl("Color Chrome", IconCC, listOf("Off", "Weak", "Strong"), colorChrome) { colorChrome = it }
                            ChipControl("Color Chrome FX Blue", IconCCFXBlue, listOf("Off", "Weak", "Strong"), colorChromeBlue) { colorChromeBlue = it }
                        }
                        if (supportsSmoothSkin) {
                            ChipControl("Smooth Skin", IconSmoothSkin, listOf("Off", "Weak", "Strong"), smoothSkin) { smoothSkin = it }
                        }
                    }

                    EditorSection("Tone Curve") {
                        HalfStepControl("Highlight Tone", IconHighlight, highlight, -2f, 4f) { highlight = it }
                        HalfStepControl("Shadow Tone", IconShadow, shadow, -2f, 4f) { shadow = it }
                    }

                    EditorSection(if (isMono) "Monochrome Color" else "Color Response") {
                        if (isMono) {
                            StepperControl("Mono WC", IconColor, monoWc, -9, 9) { monoWc = it }
                            StepperControl("Mono MG", IconColor, monoMg, -9, 9) { monoMg = it }
                        } else {
                            StepperControl("Color", IconColor, color, -4, 4) { color = it }
                        }
                    }

                    EditorSection("Detail") {
                        StepperControl("Sharpness", IconSharpness, sharpness, -4, 4) { sharpness = it }
                        if (supportsClarity) StepperControl("Clarity", IconClarity, clarity, -5, 5) { clarity = it }
                        StepperControl("High ISO NR", IconNR, highIsoNr, -4, 4) { highIsoNr = it }
                    }

                    EditorSection("White Balance") {
                        ChipControl(
                            "White Balance",
                            IconWB,
                            whiteBalanceOptions,
                            whiteBalance,
                        ) { whiteBalance = it }
                        if (whiteBalance == "Color Temperature") {
                            StepperControl("Kelvin", IconWB, colorTemp, 2500, 10000, step = 100, valueText = "${colorTemp}K") { colorTemp = it }
                        }
                        if (!isMono) {
                            StepperControl("WB Shift R", IconWBShift, wbRed, -9, 9) { wbRed = it }
                            StepperControl("WB Shift B", IconWBShift, wbBlue, -9, 9) { wbBlue = it }
                        }
                    }

                    EditorSection("Shooting Settings", optional = true) {
                        OptionalFieldHeader(label = "Recommended ISO", icon = IconISO, isSet = isoMin != null || isoMax != null) { isoMin = null; isoMax = null }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StepperControl("", null, isoMin ?: 0, 0, 12800, step = 100, valueText = isoMin?.toString() ?: "—", modifier = Modifier.weight(1f)) { isoMin = it.takeIf { v -> v > 0 } }
                            Text(
                                text = when {
                                    isoMin != null && isoMax == null -> ">"
                                    isoMin == null && isoMax != null -> "<"
                                    else -> "–"
                                },
                                fontFamily = MonoFamily,
                                fontSize = 16.sp,
                                color = TextDim,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                            StepperControl("", null, isoMax ?: 0, 0, 12800, step = 100, valueText = isoMax?.toString() ?: "—", modifier = Modifier.weight(1f)) { isoMax = it.takeIf { v -> v > 0 } }
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
                        OptionalFieldHeader(label = "Exposure Compensation", icon = IconExposureComp, isSet = exposureCompMin != null || exposureCompMax != null) { exposureCompMin = null; exposureCompMax = null }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            HalfStepControl("", null, exposureCompMin ?: 0f, -5f, 5f, modifier = Modifier.weight(1f)) { exposureCompMin = it }
                            Text(
                                text = when {
                                    exposureCompMin != null && exposureCompMax == null -> ">"
                                    exposureCompMin == null && exposureCompMax != null -> "<"
                                    else -> "–"
                                },
                                fontFamily = MonoFamily,
                                fontSize = 16.sp,
                                color = TextDim,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                            HalfStepControl("", null, exposureCompMax ?: 0f, -5f, 5f, modifier = Modifier.weight(1f)) { exposureCompMax = it }
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
                        OptionalFieldHeader(label = "Supported Sensors", icon = IconCamera, isSet = sensorGens.isNotEmpty()) { sensorGens = emptyList() }
                        SensorGenCheckboxes(selected = sensorGens) { sensorGens = it }
                    }

                    Spacer(Modifier.height(120.dp))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Bg))),
                )
            }

        }
    }
}

@Composable
private fun EditorHeader(
    title: String,
    onClose: () -> Unit,
    canSave: Boolean,
    onSave: () -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onClose)
                .padding(horizontal = 4.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(IconClose, contentDescription = "Close", tint = TextPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, fontFamily = SansFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 1.6.sp, color = TextPrimary)
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = canSave) {
                    FujiHaptics.perform(context, effect = FujiHapticEffect.Confirm)
                    onSave()
                }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                IconCheck,
                contentDescription = null,
                tint = if (canSave) Gold else TextDim,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "SAVE",
                fontFamily = MonoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 1.3.sp,
                color = if (canSave) Gold else TextDim,
            )
        }
    }
}

@Composable
private fun EditorReferenceImages(
    referenceImageUris: List<String>,
    maxReferenceImages: Int,
    onAddReferenceImage: () -> Unit,
    onRemoveReferenceImage: (String) -> Unit,
) {
    val context = LocalContext.current
    val bitmaps by produceState(
        initialValue = referenceImageUris.map { it to (null as ImageBitmap?) },
        referenceImageUris,
    ) {
        value = referenceImageUris.map { it to (null as ImageBitmap?) }
        value = withContext(Dispatchers.IO) {
            referenceImageUris.map { uriString ->
                uriString to decodeSampledBitmap(context, Uri.parse(uriString))
            }
        }
    }
    val hasImages = bitmaps.any { it.second != null }

    Spacer(Modifier.height(14.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PanelHigh)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "REFERENCE IMAGES",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 1.6.sp,
                    color = TextDim,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (referenceImageUris.isEmpty()) "Add sample photos for this recipe" else "${referenceImageUris.size}/$maxReferenceImages added",
                    fontFamily = SansFamily,
                    fontSize = 12.sp,
                    color = TextMuted,
                )
            }
            Text(
                text = "ADD",
                fontFamily = MonoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.5.sp,
                letterSpacing = 1.3.sp,
                color = if (referenceImageUris.size < maxReferenceImages) Gold else TextDim,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = referenceImageUris.size < maxReferenceImages, onClick = onAddReferenceImage)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            )
        }

        if (hasImages) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                bitmaps.forEach { (uriString, bitmap) ->
                    bitmap ?: return@forEach
                    Box(modifier = Modifier.size(width = 104.dp, height = 78.dp)) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Bg),
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(5.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.68f))
                                .clickable { onRemoveReferenceImage(uriString) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                IconClose,
                                contentDescription = "Remove image",
                                tint = Color.White,
                                modifier = Modifier.size(10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
