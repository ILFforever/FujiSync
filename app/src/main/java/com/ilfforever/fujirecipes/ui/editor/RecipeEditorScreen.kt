package com.ilfforever.fujirecipes.ui.editor

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujirecipes.data.ptp.CameraPresetName
import com.ilfforever.fujirecipes.domain.model.FujiFilmSimulation
import com.ilfforever.fujirecipes.domain.model.canonicalFilmSimLabel
import com.ilfforever.fujirecipes.ui.components.decodeSampledBitmap
import com.ilfforever.fujirecipes.ui.components.FilmSimLabel
import com.ilfforever.fujirecipes.ui.components.IconCamera
import com.ilfforever.fujirecipes.ui.components.IconCC
import com.ilfforever.fujirecipes.ui.components.IconCCFXBlue
import com.ilfforever.fujirecipes.ui.components.IconCheck
import com.ilfforever.fujirecipes.ui.components.IconClarity
import com.ilfforever.fujirecipes.ui.components.IconClose
import com.ilfforever.fujirecipes.ui.components.IconColor
import com.ilfforever.fujirecipes.ui.components.IconDR
import com.ilfforever.fujirecipes.ui.components.IconExposureComp
import com.ilfforever.fujirecipes.ui.components.IconGrain
import com.ilfforever.fujirecipes.ui.components.IconHighlight
import com.ilfforever.fujirecipes.ui.components.IconISO
import com.ilfforever.fujirecipes.ui.components.IconNR
import com.ilfforever.fujirecipes.ui.components.IconShadow
import com.ilfforever.fujirecipes.ui.components.IconSharpness
import com.ilfforever.fujirecipes.ui.components.IconSmoothSkin
import com.ilfforever.fujirecipes.ui.components.IconWB
import com.ilfforever.fujirecipes.ui.components.IconWBShift
import com.ilfforever.fujirecipes.ui.components.Pill
import com.ilfforever.fujirecipes.ui.components.SectionLabel
import com.ilfforever.fujirecipes.ui.library.normalizedDRangePriorityLabel
import com.ilfforever.fujirecipes.ui.library.normalizedDynamicRangeLabel
import com.ilfforever.fujirecipes.ui.model.RecipeUiModel
import com.ilfforever.fujirecipes.ui.theme.Bg
import com.ilfforever.fujirecipes.ui.theme.Border
import com.ilfforever.fujirecipes.ui.theme.Gold
import com.ilfforever.fujirecipes.ui.theme.GoldDim
import com.ilfforever.fujirecipes.ui.theme.MonoFamily
import com.ilfforever.fujirecipes.ui.theme.PanelHigh
import com.ilfforever.fujirecipes.ui.theme.PanelLow
import com.ilfforever.fujirecipes.ui.theme.SansFamily
import com.ilfforever.fujirecipes.ui.haptics.FujiHapticEffect
import com.ilfforever.fujirecipes.ui.haptics.FujiHaptics
import com.ilfforever.fujirecipes.ui.theme.TextDim
import com.ilfforever.fujirecipes.ui.theme.TextMuted
import com.ilfforever.fujirecipes.ui.theme.TextPrimary
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

private val whiteBalanceOptions = listOf(
    "Auto",
    "Auto White Priority",
    "Ambience Priority",
    "Daylight",
    "Incandescent",
    "Underwater",
    "Fluorescent 1",
    "Fluorescent 2",
    "Fluorescent 3",
    "Shade",
    "Color Temperature",
)

private val dRangePriorityOptions = listOf("Off", "Strong", "Weak", "Auto")

private data class FilmSimFamily(
    val label: String,
    val sims: List<String>,
)

private val filmSimFamilies = listOf(
    FilmSimFamily(
        label = "Standard",
        sims = listOf("Provia / Standard", "Velvia / Vivid", "Astia / Soft"),
    ),
    FilmSimFamily(
        label = "Portrait",
        sims = listOf("Pro Neg Hi", "Pro Neg Std", "Nostalgic Neg", "Reala Ace"),
    ),
    FilmSimFamily(
        label = "Classic",
        sims = listOf("Classic Chrome", "Classic Neg", "Sepia"),
    ),
    FilmSimFamily(
        label = "Mono",
        sims = listOf("Monochrome", "Monochrome + Y", "Monochrome + R", "Monochrome + G", "Acros", "Acros + Y", "Acros + R", "Acros + G"),
    ),
    FilmSimFamily(
        label = "Cinema",
        sims = listOf("Eterna", "Eterna Bleach Bypass"),
    ),
)

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
                        ChipControl("Grain Effect", IconGrain, listOf("Off", "Weak Small", "Strong Small", "Weak Large", "Strong Large"), grain) { grain = it }
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

@Composable
private fun EditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minHeight: androidx.compose.ui.unit.Dp = 0.dp,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = textStyle,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(Gold),
        modifier = modifier
            .fillMaxWidth()
            .then(if (minHeight > 0.dp) Modifier.heightIn(min = minHeight) else Modifier),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.TopStart) {
                if (value.isBlank()) {
                    Text(
                        text = placeholder,
                        fontFamily = textStyle.fontFamily,
                        fontWeight = textStyle.fontWeight,
                        fontSize = textStyle.fontSize,
                        lineHeight = textStyle.lineHeight,
                        color = TextDim,
                    )
                }
                inner()
            }
        },
    )
}

@Composable
private fun EditorSection(
    title: String,
    optional: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SectionLabel(text = title)
        if (optional) {
            Text(
                text = "OPTIONAL",
                fontFamily = MonoFamily,
                fontSize = 9.sp,
                letterSpacing = 1.4.sp,
                color = TextDim,
            )
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
    Spacer(Modifier.height(16.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilmSimulationPicker(
    selected: String,
    selectedFamily: String,
    onFamilySelect: (String) -> Unit,
    onSelect: (String) -> Unit,
) {
    val family = filmSimFamilies.firstOrNull { it.label == selectedFamily } ?: filmSimFamilyFor(selected)

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        filmSimFamilies.forEach { item ->
            val active = item.label == family.label
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (active) GoldDim else Color.Transparent)
                    .border(1.dp, if (active) Gold else Border, RoundedCornerShape(999.dp))
                    .clickable { onFamilySelect(item.label) }
                    .padding(horizontal = 11.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = item.label.uppercase(),
                    fontFamily = MonoFamily,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 10.sp,
                    letterSpacing = 1.1.sp,
                    color = if (active) Gold else TextMuted,
                )
                Text(
                    text = item.sims.size.toString(),
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    color = if (active) Gold else TextDim,
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Border),
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        family.sims.forEach { option ->
            val active = option == selected
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) Gold else PanelHigh)
                    .border(1.dp, if (active) Gold else Border, RoundedCornerShape(10.dp))
                    .clickable { onSelect(option) }
                    .padding(start = 10.dp, end = 13.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (active) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Bg),
                    )
                }
                Text(
                    text = option,
                    fontFamily = SansFamily,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 13.5.sp,
                    color = if (active) Bg else TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipControl(
    label: String,
    icon: ImageVector,
    options: List<String>,
    selected: String,
    enabled: Boolean = true,
    onSelect: (String) -> Unit,
) {
    ControlLabel(label = label, icon = icon, enabled = enabled)
    ChipGrid(options = options, selected = selected, enabled = enabled, onSelect = onSelect)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGrid(
    options: List<String>,
    selected: String,
    enabled: Boolean = true,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val active = option == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            !enabled && active -> GoldDim.copy(alpha = 0.28f)
                            !enabled -> PanelLow
                            active -> Gold
                            else -> PanelHigh
                        },
                    )
                    .border(
                        1.dp,
                        when {
                            !enabled -> Color.Transparent
                            active -> Gold
                            else -> Border
                        },
                        RoundedCornerShape(8.dp),
                    )
                    .clickable(enabled = enabled) { onSelect(option) }
                    .padding(horizontal = 13.dp, vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    fontFamily = SansFamily,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 13.5.sp,
                    color = when {
                        !enabled && active -> Gold.copy(alpha = 0.82f)
                        !enabled -> TextDim
                        active -> Bg
                        else -> TextMuted
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StepperControl(
    label: String,
    icon: ImageVector?,
    value: Int,
    min: Int,
    max: Int,
    step: Int = 1,
    valueText: String = value.signedDisplay(),
    modifier: Modifier = Modifier,
    onValueChange: (Int) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlLabel(label = label, icon = icon, modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .border(1.dp, Border, RoundedCornerShape(9.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepButton("−", enabled = value > min) { onValueChange((value - step).coerceAtLeast(min)) }
            Box(
                modifier = Modifier
                    .width(68.dp)
                    .height(42.dp)
                    .background(Color(0xFF0A0908)),
                contentAlignment = Alignment.Center,
            ) {
                Text(valueText, fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            }
            StepButton("+", enabled = value < max) { onValueChange((value + step).coerceAtMost(max)) }
        }
    }
}

@Composable
private fun HalfStepControl(
    label: String,
    icon: ImageVector?,
    value: Float,
    min: Float,
    max: Float,
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlLabel(label = label, icon = icon, modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .border(1.dp, Border, RoundedCornerShape(9.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepButton("−", enabled = value > min) { onValueChange((value - 0.5f).coerceAtLeast(min)) }
            Box(
                modifier = Modifier
                    .width(68.dp)
                    .height(42.dp)
                    .background(Color(0xFF0A0908)),
                contentAlignment = Alignment.Center,
            ) {
                Text(value.signedDisplay(), fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            }
            StepButton("+", enabled = value < max) { onValueChange((value + 0.5f).coerceAtMost(max)) }
        }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 42.dp)
            .background(if (enabled) PanelHigh else PanelLow)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontFamily = SansFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = if (enabled) TextPrimary else TextDim)
    }
}

@Composable
private fun ControlLabel(
    label: String,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (enabled) Gold else TextDim, modifier = Modifier.size(18.dp))
        }
        Text(label, fontFamily = SansFamily, fontSize = 14.5.sp, color = if (enabled) TextPrimary else TextDim)
    }
}

@Composable
private fun OptionalFieldHeader(
    label: String,
    icon: ImageVector,
    isSet: Boolean,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlLabel(label = label, icon = icon)
        if (isSet) {
            Text(
                text = "CLEAR",
                fontFamily = MonoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = TextMuted,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onClear)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SensorGenCheckboxes(
    selected: List<Int>,
    onChanged: (List<Int>) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (1..5).forEach { gen ->
            val active = gen in selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) Gold else PanelHigh)
                    .border(1.dp, if (active) Gold else Border, RoundedCornerShape(8.dp))
                    .clickable {
                        onChanged(
                            if (active) selected - gen else (selected + gen).sorted()
                        )
                    }
                    .padding(horizontal = 13.dp, vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "X-Trans $gen",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    color = if (active) Bg else TextMuted,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun defaultRecipe() = RecipeUiModel(
    slot = "",
    name = "New Recipe",
    sim = FujiFilmSimulation.Provia.label,
    pills = listOf("DR AUTO"),
    effects = mapOf(
        "Dynamic Range" to "DR Auto",
        "D Range Priority" to "Off",
        "Grain Effect" to "Off",
        "Color Chrome" to "Off",
        "Color Chrome FX Blue" to "Off",
        "Smooth Skin" to "Off",
    ),
    tone = mapOf(
        "Highlight Tone" to "0",
        "Shadow Tone" to "0",
        "Color" to "0",
        "Sharpness" to "0",
        "High ISO NR" to "0",
        "Clarity" to "0",
    ),
    wb = mapOf("White Balance" to "Auto", "WB Shift R" to "0", "WB Shift B" to "0"),
)

private fun buildRecipe(
    base: RecipeUiModel,
    name: String,
    description: String,
    sim: String,
    dRangePriority: String,
    dynamicRange: String,
    grain: String,
    colorChrome: String,
    colorChromeBlue: String,
    smoothSkin: String,
    whiteBalance: String,
    wbRed: Int,
    wbBlue: Int,
    highlight: Float,
    shadow: Float,
    color: Int,
    sharpness: Int,
    highIsoNr: Int,
    clarity: Int,
    monoWc: Int,
    monoMg: Int,
    referenceImageUris: List<String>,
    isMono: Boolean,
    isoMin: Int?,
    isoMax: Int?,
    exposureCompMin: Float?,
    exposureCompMax: Float?,
    sensorGens: List<Int>,
): RecipeUiModel {
    val effects = linkedMapOf(
        "D Range Priority" to dRangePriority.normalizedDRangePriorityLabel(),
        "Dynamic Range" to dynamicRange.normalizedDynamicRangeLabel(),
        "Grain Effect" to grain,
        "Color Chrome" to if (isMono) "—" else colorChrome,
        "Color Chrome FX Blue" to if (isMono) "—" else colorChromeBlue,
        "Smooth Skin" to smoothSkin,
    )
    val tone = linkedMapOf(
        "Highlight Tone" to highlight.signedDisplay(),
        "Shadow Tone" to shadow.signedDisplay(),
    )
    if (isMono) {
        tone["Mono WC"] = monoWc.signedDisplay()
        tone["Mono MG"] = monoMg.signedDisplay()
    } else {
        tone["Color"] = color.signedDisplay()
    }
    tone["Sharpness"] = sharpness.signedDisplay()
    tone["High ISO NR"] = highIsoNr.signedDisplay()
    tone["Clarity"] = clarity.signedDisplay()

    val wb = linkedMapOf("White Balance" to whiteBalance)
    if (!isMono) {
        wb["WB Shift R"] = wbRed.signedDisplay()
        wb["WB Shift B"] = wbBlue.signedDisplay()
    }

    return base.copy(
        name = name,
        sim = sim,
        description = description,
        effects = effects,
        tone = tone,
        wb = wb,
        referenceImageUris = referenceImageUris,
        pills = buildPills(dRangePriority.normalizedDRangePriorityLabel(), dynamicRange.normalizedDynamicRangeLabel(), grain, colorChrome, colorChromeBlue, highlight, shadow, color, clarity, isMono),
        isoMin = isoMin,
        isoMax = isoMax,
        exposureCompMin = exposureCompMin,
        exposureCompMax = exposureCompMax,
        sensorGens = sensorGens,
    )
}

private fun buildPills(
    dRangePriority: String,
    dynamicRange: String,
    grain: String,
    colorChrome: String,
    colorChromeBlue: String,
    highlight: Float,
    shadow: Float,
    color: Int,
    clarity: Int,
    isMono: Boolean,
): List<String> = buildList {
    if (dRangePriority == "Off") {
        add(dynamicRange.uppercase())
    } else {
        add("DRP ${dRangePriority.uppercase()}")
    }
    if (grain != "Off") {
        add("GRAIN " + grain
            .replace("Weak Small", "WK/S")
            .replace("Weak Large", "WK/L")
            .replace("Strong Small", "ST/S")
            .replace("Strong Large", "ST/L")
            .uppercase())
    }
    if (!isMono) {
        if (colorChrome != "Off") add("CC ${if (colorChrome == "Strong") "STRONG" else "WEAK"}")
        if (colorChromeBlue != "Off") add("FX BLUE ${if (colorChromeBlue == "Strong") "STR" else "WK"}")
        if (shadow != 0f) add("SH ${shadow.signedDisplay()}")
        if (color != 0) add("COLOR ${color.signedDisplay()}")
    }
    if (highlight != 0f) add("HL ${highlight.signedDisplay()}")
    if (clarity != 0) add("CLARITY ${clarity.signedDisplay()}")
}

private fun String?.signedIntOrZero(): Int =
    this?.replace("−", "-")?.replace("+", "")?.trim()?.toIntOrNull() ?: 0

private fun String?.signedFloatOrZero(): Float =
    this?.replace("−", "-")?.replace("+", "")?.trim()?.toFloatOrNull() ?: 0f

private fun String?.editorWhiteBalanceMode(): String {
    val value = this?.trim().orEmpty()
    return when {
        value.endsWith("K", ignoreCase = true) -> "Color Temperature"
        value.equals("Auto (White Priority)", ignoreCase = true) -> "Auto White Priority"
        value.equals("Auto (Ambience)", ignoreCase = true) -> "Ambience Priority"
        value.equals("Tungsten", ignoreCase = true) -> "Incandescent"
        value in whiteBalanceOptions -> value
        else -> "Auto"
    }
}

private fun filmSimFamilyFor(sim: String): FilmSimFamily =
    filmSimFamilies.firstOrNull { family -> sim in family.sims }
        ?: filmSimFamilies.first()


private fun Int.signedDisplay(): String = when {
    this > 0 -> "+$this"
    this < 0 -> "−${-this}"
    else -> "0"
}

private fun Float.signedDisplay(): String {
    if (this == 0f) return "0"
    val prefix = if (this > 0f) "+" else "−"
    val abs = kotlin.math.abs(this)
    return if (abs == kotlin.math.floor(abs)) "$prefix${abs.toInt()}" else "$prefix$abs"
}
