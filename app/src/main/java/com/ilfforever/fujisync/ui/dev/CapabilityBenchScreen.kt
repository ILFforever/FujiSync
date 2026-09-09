package com.ilfforever.fujisync.ui.dev

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.data.capability.CameraCapability
import com.ilfforever.fujisync.data.capability.RecipeWritePlanner
import com.ilfforever.fujisync.domain.model.CameraSlot
import com.ilfforever.fujisync.domain.model.FujiFilmSimulation
import com.ilfforever.fujisync.ui.components.CameraCapabilityCard
import com.ilfforever.fujisync.ui.components.CompatibilityNotice
import com.ilfforever.fujisync.ui.components.PrimaryCTA
import com.ilfforever.fujisync.ui.components.SectionLabel
import com.ilfforever.fujisync.ui.detail.RecipeDetailScreen
import com.ilfforever.fujisync.ui.detail.SyncToCameraSheet
import com.ilfforever.fujisync.ui.model.RecipeUiModel
import com.ilfforever.fujisync.ui.model.blockingFilmSimulation
import com.ilfforever.fujisync.ui.model.capabilityProfile
import com.ilfforever.fujisync.ui.model.compatibilitySummary
import com.ilfforever.fujisync.ui.model.toPreset
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.GoldDim
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelHigh
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary

/**
 * Every compatibility panel, rendered against a body of your choosing without needing that body.
 *
 * The gating states are the hardest part of this feature to see: the interesting ones only appear
 * on cameras the developer does not own, and the X-H2 sitting on the desk triggers none of them
 * because it supports everything. This drives the real composables — the same
 * [CompatibilityNotice], [CameraCapabilityCard] and CTA logic the app ships — from a capability
 * resolved out of the real shipped table, so what you see here is what a photographer with that
 * body would see.
 *
 * Camera-free: it never opens a USB connection.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CapabilityBenchScreen(
    viewModel: CapabilityBenchViewModel,
    onClose: () -> Unit,
) {
    var deviceKey by remember { mutableStateOf(SIMULATED_BODIES.first().key) }
    var recipeIndex by remember { mutableStateOf(0) }
    var showDetail by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }

    val capability = remember(deviceKey) { viewModel.capabilityFor(deviceKey) }
    val recipe = BENCH_RECIPES[recipeIndex]
    val bodyLabel = SIMULATED_BODIES.first { it.key == deviceKey }.label
    val summary = remember(recipe, capability) { compatibilitySummary(recipe, capability, bodyLabel) }
    val blockedSim = remember(recipe, capability) { blockingFilmSimulation(recipe, capability) }
    val plan = remember(recipe, capability) {
        RecipeWritePlanner.plan(recipe.toPreset(CameraSlot.C1), capability)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 40.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "CAPABILITY BENCH",
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 1.6.sp,
                    color = TextMuted,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "No camera needed",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = TextPrimary,
                )
            }
            Text(
                text = "CLOSE",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 1.6.sp,
                color = Gold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }

        Text(
            text = if (viewModel.tableLoaded) {
                "Capability table loaded · ${viewModel.tableVersion ?: "unknown version"}"
            } else {
                "CAPABILITY TABLE FAILED TO LOAD — every panel below will be empty. " +
                    "That is itself the finding: check the asset shipped."
            },
            fontFamily = SansFamily,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            color = if (viewModel.tableLoaded) TextDim else Gold,
        )

        Spacer(Modifier.height(18.dp))
        SectionLabel(text = "Pretend this body is attached")
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SIMULATED_BODIES.forEach { body ->
                BenchChip(
                    label = body.label,
                    active = body.key == deviceKey,
                ) { deviceKey = body.key }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel(text = "Recipe being pushed")
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BENCH_RECIPES.forEachIndexed { index, item ->
                BenchChip(label = item.name, active = index == recipeIndex) { recipeIndex = index }
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel(text = "See it in the real screens")
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Opens the shipping recipe detail page and sync sheet with this body " +
                "pretended to be attached. Nothing is written — there is no camera.",
            fontFamily = SansFamily,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            color = TextDim,
        )
        Spacer(Modifier.height(10.dp))
        PrimaryCTA(label = "Open recipe detail page", onClick = { showDetail = true }, secondary = true)
        Spacer(Modifier.height(8.dp))
        PrimaryCTA(label = "Open sync sheet", onClick = { showSheet = true }, secondary = true)

        Spacer(Modifier.height(24.dp))
        SectionLabel(text = "Camera detail card")
        Spacer(Modifier.height(10.dp))
        CameraCapabilityCard(
            profile = capabilityProfile(capability),
            background = PanelLow,
            borderColor = Border,
        )

        Spacer(Modifier.height(24.dp))
        SectionLabel(text = "Pre-write notice · tap to expand")
        Spacer(Modifier.height(10.dp))
        if (summary.isEmpty) {
            EmptyState("Nothing to warn about — this recipe fits this body exactly.")
        } else {
            CompatibilityNotice(summary = summary)
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel(text = "Write button")
        Spacer(Modifier.height(10.dp))
        PrimaryCTA(
            label = if (blockedSim != null) "Film Sim Not Supported" else "Write to C1",
            onClick = {},
            enabled = blockedSim == null,
        )

        Spacer(Modifier.height(24.dp))
        SectionLabel(text = "What the write path would do")
        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PanelLow)
                .border(1.dp, Border, RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            if (plan.isBlocked) {
                PlanLine("BLOCKED", "nothing is sent — the slot is left untouched", Gold)
            }
            plan.writes.forEach { write ->
                PlanLine(
                    write.property.displayName,
                    buildString {
                        append(write.value)
                        write.adjustment?.let { append("  (was ${it.from})") }
                        write.warning?.let { append("  · sent despite doubt") }
                    },
                    if (write.adjustment != null || write.warning != null) Gold else TextPrimary,
                )
            }
            plan.skipped.forEach { skip ->
                PlanLine(skip.property.displayName, "skipped — ${skip.reason}", TextDim)
            }
        }
    }

    // The real screens, driven by the simulated body. These are the shipping composables, not
    // stand-ins — the point is to judge the panels where they actually sit.
    if (showDetail) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            RecipeDetailScreen(
                recipe = recipe,
                connected = true,
                onClose = { showDetail = false },
                onWrite = {},
                onAddReferenceImage = {},
                onRemoveReferenceImage = { _, _ -> },
                onToggleFavorite = {},
                onEdit = {},
                onClone = {},
                onDelete = {},
                writeBusy = false,
                cameraModel = SIMULATED_BODIES.first { it.key == deviceKey }.label,
                cameraFirmware = "5.20",
                cameraBattery = "28%",
                cameraSlots = emptyList(),
                capability = capability,
            )
        }
    }

    if (showSheet) {
        SyncToCameraSheet(
            connected = true,
            cameraModel = SIMULATED_BODIES.first { it.key == deviceKey }.label,
            cameraSlots = BENCH_SLOTS,
            recipeName = recipe.name,
            writeBusy = false,
            onDismiss = { showSheet = false },
            onWriteToSlot = { showSheet = false },
            recipe = recipe,
            capability = capability,
            cameraFirmware = "5.20",
            cameraBattery = "28%",
        )
    }
}

/** Stand-in slot contents so the sync sheet's list is not empty in the bench. */
private val BENCH_SLOTS = listOf(
    "C1" to "Leica-Like", "C2" to "Portra 160", "C3" to "Kodak Vericolor",
    "C4" to "Fujicolor Super HG", "C5" to "tokyo 200", "C6" to "London",
    "C7" to "black&white infared",
).map { (slot, name) ->
    RecipeUiModel(slot = slot, name = name, sim = FujiFilmSimulation.ClassicNeg.label, pills = emptyList())
}

@Composable
private fun BenchChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) GoldDim else PanelHigh)
            .border(1.dp, if (active) Gold else Border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text(
            text = label,
            fontFamily = MonoFamily,
            fontSize = 11.sp,
            letterSpacing = 0.6.sp,
            color = if (active) Gold else TextMuted,
        )
    }
}

@Composable
private fun PlanLine(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontFamily = SansFamily, fontSize = 12.5.sp, color = TextMuted)
        Text(
            value,
            fontFamily = MonoFamily,
            fontSize = 11.5.sp,
            color = color,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun EmptyState(text: String) {
    Text(
        text = text,
        fontFamily = SansFamily,
        fontSize = 13.sp,
        color = TextDim,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(10.dp))
            .padding(14.dp),
    )
}

internal data class SimulatedBody(val label: String, val key: String)

/**
 * Bodies chosen because each one lands on a different side of a boundary in Fuji's data: X-T2 is
 * X-Trans III with no Clarity and whole-step tone dials, X-T3 is the last generation without
 * per-preset WB shift, X-Pro3 is the first with it, X-T4 gains half steps but stops at Eterna
 * Bleach Bypass, and the X-H2 records take everything.
 */
internal val SIMULATED_BODIES = listOf(
    SimulatedBody("X-T2", "X-T2_0100"),
    SimulatedBody("X-T3", "X-T3_0100"),
    SimulatedBody("X-Pro3", "X-Pro3_0100"),
    SimulatedBody("X-T4", "X-T4_0100"),
    SimulatedBody("X-S10", "X-S10_0100"),
    SimulatedBody("X-H2 fw1", "X-H2_0100"),
    SimulatedBody("X-H2 fw2", "X-H2_0200"),
    SimulatedBody("GFX100RF", "GFX100RF_0100"),
    SimulatedBody("Unknown body", "X-NOPE_0100"),
)

private fun benchRecipe(
    name: String,
    sim: String,
    effects: Map<String, String>,
    tone: Map<String, String>,
    wb: Map<String, String>,
) = RecipeUiModel(slot = "", name = name, sim = sim, pills = emptyList(), effects = effects, tone = tone, wb = wb)

/** Recipes picked to hit one gating path each. */
internal val BENCH_RECIPES = listOf(
    benchRecipe(
        name = "Reala Ace",
        sim = FujiFilmSimulation.RealaAce.label,
        effects = mapOf("D Range Priority" to "Off", "Dynamic Range" to "DR400%", "Grain Effect" to "Weak Large"),
        tone = mapOf("Highlight Tone" to "+1.5", "Shadow Tone" to "0", "Clarity" to "+2", "Sharpness" to "0", "High ISO NR" to "0"),
        wb = mapOf("White Balance" to "Auto", "WB Shift R" to "+2", "WB Shift B" to "−4"),
    ),
    benchRecipe(
        name = "Classic Neg",
        sim = FujiFilmSimulation.ClassicNeg.label,
        effects = mapOf("D Range Priority" to "Off", "Dynamic Range" to "DR200%", "Smooth Skin" to "Weak"),
        tone = mapOf("Highlight Tone" to "+0.5", "Shadow Tone" to "+1", "Clarity" to "0", "Sharpness" to "0", "High ISO NR" to "0"),
        wb = mapOf("White Balance" to "Auto White Priority", "WB Shift R" to "0", "WB Shift B" to "0"),
    ),
    benchRecipe(
        name = "Kelvin 5650",
        sim = FujiFilmSimulation.Provia.label,
        effects = mapOf("D Range Priority" to "Off", "Dynamic Range" to "DR Auto"),
        tone = mapOf("Highlight Tone" to "0", "Shadow Tone" to "0", "Clarity" to "0", "Sharpness" to "0", "High ISO NR" to "0"),
        wb = mapOf("White Balance" to "5650K", "WB Shift R" to "0", "WB Shift B" to "0"),
    ),
    benchRecipe(
        name = "Acros mono",
        sim = FujiFilmSimulation.Acros.label,
        effects = mapOf("D Range Priority" to "Strong", "Dynamic Range" to "DR400%", "Grain Effect" to "Strong Large"),
        tone = mapOf("Highlight Tone" to "+2", "Shadow Tone" to "+1", "Mono WC" to "+3", "Sharpness" to "0", "High ISO NR" to "0"),
        wb = mapOf("White Balance" to "Daylight"),
    ),
    benchRecipe(
        name = "Provia plain",
        sim = FujiFilmSimulation.Provia.label,
        effects = mapOf("D Range Priority" to "Off", "Dynamic Range" to "DR100%"),
        tone = mapOf("Highlight Tone" to "0", "Shadow Tone" to "0", "Clarity" to "0", "Sharpness" to "0", "High ISO NR" to "0"),
        wb = mapOf("White Balance" to "Daylight", "WB Shift R" to "0", "WB Shift B" to "0"),
    ),
)
