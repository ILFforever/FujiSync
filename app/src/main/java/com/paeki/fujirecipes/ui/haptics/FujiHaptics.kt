package com.paeki.fujirecipes.ui.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import androidx.annotation.RequiresApi

enum class FujiHapticEffect {
    Selection,
    SoftSelection,
    SheetOpen,
    SheetDismiss,
    DrawerSwooshOpen,
    DrawerSwooshDismiss,
    Confirm,
    SoftConfirm,
    Reject,
    DragStart,
    DragEnd,
    SoftSuccess,
    CameraWriteSuccess,
    CameraWriteFailure,
    EnvelopeRise,
    EnvelopeSnap,
    RumbleRise,
    RumbleFall,
    RumblePop,
    TripleKnock,
    ScanRamp,
    DelayedRamp,
    ElasticSnap,
    WarningBuzz,
    CalmDouble,
    DeliberateDouble,
    WarningPause,
    SuccessPause,
}

data class FujiHapticCapabilities(
    val sdk: Int,
    val hasVibrator: Boolean,
    val hasAmplitudeControl: Boolean,
    val supportedPrimitives: Map<Int, Boolean>,
    val primitiveDurationsMs: Map<Int, Int>,
    val envelopeEffectsSupported: Boolean,
)

data class FujiHapticPrimitiveStep(
    val primitive: Int,
    val scale: Float = 1f,
    val delayMs: Int = 0,
)

data class FujiHapticResult(
    val played: Boolean,
    val path: String,
)

object FujiHaptics {
    var enabled: Boolean = true

    val primitiveIds: List<Int> = listOf(
        VibrationEffect.Composition.PRIMITIVE_CLICK,
        VibrationEffect.Composition.PRIMITIVE_TICK,
        VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
        VibrationEffect.Composition.PRIMITIVE_THUD,
        VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
        VibrationEffect.Composition.PRIMITIVE_QUICK_FALL,
        VibrationEffect.Composition.PRIMITIVE_SLOW_RISE,
        VibrationEffect.Composition.PRIMITIVE_SPIN,
    )

    fun perform(
        context: Context,
        view: View? = null,
        effect: FujiHapticEffect,
    ): FujiHapticResult {
        if (!enabled) return FujiHapticResult(false, "haptics disabled")
        when (effect) {
            FujiHapticEffect.SheetOpen -> {
                return primitiveSequence(
                    context = context,
                    steps = listOf(
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.38f),
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.42f, 34),
                    ),
                    fallbackLabel = "sheet open",
                    fallbackEffectId = VibrationEffect.EFFECT_TICK,
                )
            }

            FujiHapticEffect.SheetDismiss -> {
                return primitiveSequence(
                    context = context,
                    steps = listOf(FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.18f)),
                    fallbackLabel = "sheet dismiss",
                    fallbackEffectId = VibrationEffect.EFFECT_TICK,
                )
            }

            FujiHapticEffect.DrawerSwooshOpen -> {
                if (Build.VERSION.SDK_INT >= 36) {
                    drawerSwooshOpen(context).takeIf { it.played }?.let { return it }
                }
                return waveformPattern(
                    context = context,
                    timings = longArrayOf(0, 8, 7, 9, 6, 11, 5, 13),
                    amplitudes = intArrayOf(0, 42, 0, 92, 0, 168, 0, 232),
                    label = "drawer swoosh open",
                    fallbackEffectId = VibrationEffect.EFFECT_TICK,
                )
            }

            FujiHapticEffect.DrawerSwooshDismiss -> {
                if (Build.VERSION.SDK_INT >= 36) {
                    drawerSwooshDismiss(context).takeIf { it.played }?.let { return it }
                }
                return waveformPattern(
                    context = context,
                    timings = longArrayOf(0, 10, 10, 8, 14, 6),
                    amplitudes = intArrayOf(0, 96, 0, 48, 0, 18),
                    label = "drawer swoosh dismiss",
                    fallbackEffectId = VibrationEffect.EFFECT_TICK,
                )
            }

            FujiHapticEffect.Selection -> {
                return primitiveSequence(
                    context = context,
                    steps = listOf(FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_TICK, 0.9f)),
                    fallbackLabel = "selection",
                    fallbackEffectId = VibrationEffect.EFFECT_TICK,
                )
            }

            FujiHapticEffect.SoftSelection -> {
                return primitiveSequence(
                    context = context,
                    steps = listOf(FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.20f)),
                    fallbackLabel = "soft selection",
                    fallbackEffectId = VibrationEffect.EFFECT_TICK,
                )
            }

            FujiHapticEffect.Confirm -> {
                // Strong quick commit feel.
                return primitiveSequence(
                    context = context,
                    steps = listOf(
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f),
                    ),
                    fallbackLabel = "confirm",
                    fallbackEffectId = VibrationEffect.EFFECT_HEAVY_CLICK,
                )
            }

            FujiHapticEffect.SoftConfirm -> {
                // Gentle nav confirmation — single short low-amplitude pulse.
                return waveformPattern(
                    context = context,
                    timings = longArrayOf(0, 10),
                    amplitudes = intArrayOf(0, 72),
                    label = "soft confirm",
                    fallbackEffectId = VibrationEffect.EFFECT_TICK,
                )
            }

            FujiHapticEffect.Reject -> {
                // iOS notificationError feel: heavy-light-light triple
                return primitiveSequence(
                    context = context,
                    steps = listOf(
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f),
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.86f, 46),
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_TICK, 0.68f, 34),
                    ),
                    fallbackLabel = "reject",
                    fallbackEffectId = VibrationEffect.EFFECT_DOUBLE_CLICK,
                )
            }

            FujiHapticEffect.DragStart -> {
                return primitiveSequence(
                    context = context,
                    steps = listOf(FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_THUD, 0.85f)),
                    fallbackLabel = "drag start",
                    fallbackEffectId = VibrationEffect.EFFECT_HEAVY_CLICK,
                )
            }

            FujiHapticEffect.DragEnd -> {
                return primitiveSequence(
                    context = context,
                    steps = listOf(FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f)),
                    fallbackLabel = "drag end",
                    fallbackEffectId = VibrationEffect.EFFECT_CLICK,
                )
            }

            FujiHapticEffect.SoftSuccess -> {
                // iOS impactLight double: gentle tick + clean click
                return primitiveSequence(
                    context = context,
                    steps = listOf(
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_TICK, 0.82f),
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 30),
                    ),
                    fallbackLabel = "soft success",
                    fallbackEffectId = VibrationEffect.EFFECT_CLICK,
                )
            }

            FujiHapticEffect.CameraWriteSuccess -> {
                // iOS notificationSuccess premium: rise → sharp punch → crisp tick
                return primitiveSequence(
                    context = context,
                    steps = listOf(
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.65f),
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 38),
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_TICK, 0.6f, 30),
                    ),
                    fallbackLabel = "camera write success",
                    fallbackEffectId = VibrationEffect.EFFECT_HEAVY_CLICK,
                )
            }

            FujiHapticEffect.CameraWriteFailure -> {
                // Two heavy thumps — unmistakably wrong
                return primitiveSequence(
                    context = context,
                    steps = listOf(
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f),
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_THUD, 0.85f, 80),
                    ),
                    fallbackLabel = "camera write failure",
                    fallbackEffectId = VibrationEffect.EFFECT_DOUBLE_CLICK,
                )
            }

            FujiHapticEffect.EnvelopeRise -> {
                if (Build.VERSION.SDK_INT >= 36) {
                    envelopeRise(context).takeIf { it.played }?.let { return it }
                }
                return primitiveSequence(
                    context = context,
                    steps = listOf(
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.94f),
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 45),
                    ),
                    fallbackLabel = "envelope rise",
                    fallbackEffectId = VibrationEffect.EFFECT_TICK,
                )
            }

            FujiHapticEffect.EnvelopeSnap -> {
                return primitiveSequence(
                    context = context,
                    steps = listOf(
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f),
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.7f, 35),
                    ),
                    fallbackLabel = "envelope snap",
                    fallbackEffectId = VibrationEffect.EFFECT_HEAVY_CLICK,
                )
            }

            FujiHapticEffect.RumbleRise -> {
                if (Build.VERSION.SDK_INT >= 36) {
                    rumbleRise(context).takeIf { it.played }?.let { return it }
                }
                return waveformPattern(
                    context = context,
                    timings = longArrayOf(0, 18, 8, 20, 8, 22, 8, 24),
                    amplitudes = intArrayOf(0, 56, 0, 112, 0, 178, 0, 244),
                    label = "rumble rise",
                    fallbackEffectId = VibrationEffect.EFFECT_HEAVY_CLICK,
                )
            }

            FujiHapticEffect.RumbleFall -> {
                if (Build.VERSION.SDK_INT >= 36) {
                    rumbleFall(context).takeIf { it.played }?.let { return it }
                }
                return waveformPattern(
                    context = context,
                    timings = longArrayOf(0, 24, 8, 20, 8, 18, 8, 14),
                    amplitudes = intArrayOf(0, 240, 0, 172, 0, 96, 0, 42),
                    label = "rumble fall",
                    fallbackEffectId = VibrationEffect.EFFECT_TICK,
                )
            }

            FujiHapticEffect.RumblePop -> {
                return waveformPattern(
                    context = context,
                    timings = longArrayOf(0, 12, 6, 14, 18, 22),
                    amplitudes = intArrayOf(0, 92, 0, 184, 0, 255),
                    label = "rumble pop",
                    fallbackEffectId = VibrationEffect.EFFECT_HEAVY_CLICK,
                )
            }

            FujiHapticEffect.TripleKnock -> {
                return primitiveSequence(
                    context = context,
                    steps = listOf(
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f),
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.86f, 58),
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_TICK, 0.72f, 48),
                    ),
                    fallbackLabel = "triple knock",
                    fallbackEffectId = VibrationEffect.EFFECT_DOUBLE_CLICK,
                )
            }

            FujiHapticEffect.ScanRamp -> {
                return waveformPattern(
                    context = context,
                    timings = longArrayOf(0, 8, 18, 8, 18, 8, 18, 8, 16, 18),
                    amplitudes = intArrayOf(0, 60, 0, 104, 0, 154, 0, 212, 0, 255),
                    label = "scan ramp",
                    fallbackEffectId = VibrationEffect.EFFECT_TICK,
                )
            }

            FujiHapticEffect.DelayedRamp -> {
                return waveformPattern(
                    context = context,
                    timings = longArrayOf(0, 16, 100, 16, 100, 17, 100, 17, 100, 18, 100, 18, 100, 20, 100, 22),
                    amplitudes = intArrayOf(0, 56, 0, 84, 0, 112, 0, 144, 0, 176, 0, 208, 0, 232, 0, 255),
                    label = "delayed ramp",
                    fallbackEffectId = VibrationEffect.EFFECT_HEAVY_CLICK,
                )
            }

            FujiHapticEffect.ElasticSnap -> {
                return primitiveSequence(
                    context = context,
                    steps = listOf(
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.78f),
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 32),
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.72f, 26),
                    ),
                    fallbackLabel = "elastic snap",
                    fallbackEffectId = VibrationEffect.EFFECT_HEAVY_CLICK,
                )
            }

            FujiHapticEffect.WarningBuzz -> {
                return waveformPattern(
                    context = context,
                    timings = longArrayOf(0, 28, 20, 28, 20, 34),
                    amplitudes = intArrayOf(0, 255, 0, 210, 0, 245),
                    label = "warning buzz",
                    fallbackEffectId = VibrationEffect.EFFECT_DOUBLE_CLICK,
                )
            }

            FujiHapticEffect.CalmDouble -> {
                return primitiveSequence(
                    context = context,
                    steps = listOf(
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.86f),
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 74),
                    ),
                    fallbackLabel = "calm double",
                    fallbackEffectId = VibrationEffect.EFFECT_DOUBLE_CLICK,
                )
            }

            FujiHapticEffect.DeliberateDouble -> {
                return primitiveSequence(
                    context = context,
                    steps = listOf(
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_THUD, 0.92f),
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 108),
                    ),
                    fallbackLabel = "deliberate double",
                    fallbackEffectId = VibrationEffect.EFFECT_DOUBLE_CLICK,
                )
            }

            FujiHapticEffect.WarningPause -> {
                return primitiveSequence(
                    context = context,
                    steps = listOf(
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f),
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_THUD, 0.78f, 128),
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_TICK, 0.64f, 52),
                    ),
                    fallbackLabel = "warning pause",
                    fallbackEffectId = VibrationEffect.EFFECT_DOUBLE_CLICK,
                )
            }

            FujiHapticEffect.SuccessPause -> {
                return primitiveSequence(
                    context = context,
                    steps = listOf(
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_TICK, 0.72f),
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 82),
                        FujiHapticPrimitiveStep(VibrationEffect.Composition.PRIMITIVE_TICK, 0.54f, 44),
                    ),
                    fallbackLabel = "success pause",
                    fallbackEffectId = VibrationEffect.EFFECT_HEAVY_CLICK,
                )
            }
        }
    }

    /** Sharp click that ramps in intensity: step 0 is lightest, step (total-1) is strongest. */
    fun performStepClick(context: Context, view: View? = null, step: Int, total: Int): FujiHapticResult {
        if (!enabled) return FujiHapticResult(false, "haptics disabled")
        val fraction = if (total <= 1) 1f else (step.toFloat() / (total - 1))
        val amplitude = (80 + (175 * fraction)).toInt().coerceIn(80, 255)
        return waveformPattern(
            context = context,
            timings = longArrayOf(0, 14),
            amplitudes = intArrayOf(0, amplitude),
            label = "step click ${step + 1}/$total",
            fallbackEffectId = VibrationEffect.EFFECT_CLICK,
        )
    }

    fun performSystem(view: View, constant: Int): FujiHapticResult =
        view.performSystemHaptic(constant)

    fun predefined(context: Context, effectId: Int, label: String = "predefined"): FujiHapticResult {
        val vibrator = context.defaultVibrator()
        if (!vibrator.hasVibrator()) return FujiHapticResult(false, "no vibrator")

        return if (Build.VERSION.SDK_INT >= 29) {
            vibrator.vibrate(VibrationEffect.createPredefined(effectId))
            FujiHapticResult(true, "$label: predefined")
        } else {
            vibrator.vibrate(VibrationEffect.createOneShot(22, VibrationEffect.DEFAULT_AMPLITUDE))
            FujiHapticResult(true, "$label: one-shot fallback")
        }
    }

    fun primitiveSequence(
        context: Context,
        steps: List<FujiHapticPrimitiveStep>,
        fallbackLabel: String = "primitive sequence",
        fallbackEffectId: Int = VibrationEffect.EFFECT_CLICK,
    ): FujiHapticResult {
        val vibrator = context.defaultVibrator()
        if (!vibrator.hasVibrator()) return FujiHapticResult(false, "no vibrator")
        if (steps.isEmpty()) return FujiHapticResult(false, "empty sequence")

        if (Build.VERSION.SDK_INT >= 30 && supportsAllPrimitives(vibrator, steps.map { it.primitive })) {
            val composition = VibrationEffect.startComposition()
            steps.forEach { step ->
                composition.addPrimitive(
                    step.primitive,
                    step.scale.coerceIn(0f, 1f),
                    step.delayMs.coerceAtLeast(0),
                )
            }
            vibrator.vibrate(composition.compose())
            return FujiHapticResult(true, "$fallbackLabel: primitives")
        }

        return waveformFallback(
            context = context,
            steps = steps,
            label = fallbackLabel,
            fallbackEffectId = fallbackEffectId,
        )
    }

    fun cancel(context: Context) {
        context.defaultVibrator().cancel()
    }

    fun capabilities(context: Context): FujiHapticCapabilities {
        val vibrator = context.defaultVibrator()
        val supportedPrimitives = if (Build.VERSION.SDK_INT >= 31) {
            val supported = vibrator.arePrimitivesSupported(*primitiveIds.toIntArray())
            primitiveIds.zip(supported.toList()).toMap()
        } else {
            primitiveIds.associateWith { false }
        }
        val primitiveDurations = if (Build.VERSION.SDK_INT >= 31) {
            val durations = vibrator.getPrimitiveDurations(*primitiveIds.toIntArray())
            primitiveIds.zip(durations.toList()).toMap()
        } else {
            emptyMap()
        }

        return FujiHapticCapabilities(
            sdk = Build.VERSION.SDK_INT,
            hasVibrator = vibrator.hasVibrator(),
            hasAmplitudeControl = vibrator.hasAmplitudeControl(),
            supportedPrimitives = supportedPrimitives,
            primitiveDurationsMs = primitiveDurations,
            envelopeEffectsSupported = Build.VERSION.SDK_INT >= 36 && vibrator.areEnvelopeEffectsSupported(),
        )
    }

    fun primitiveName(primitive: Int): String = when (primitive) {
        VibrationEffect.Composition.PRIMITIVE_CLICK -> "Click"
        VibrationEffect.Composition.PRIMITIVE_TICK -> "Tick"
        VibrationEffect.Composition.PRIMITIVE_LOW_TICK -> "Low tick"
        VibrationEffect.Composition.PRIMITIVE_THUD -> "Thud"
        VibrationEffect.Composition.PRIMITIVE_QUICK_RISE -> "Quick rise"
        VibrationEffect.Composition.PRIMITIVE_QUICK_FALL -> "Quick fall"
        VibrationEffect.Composition.PRIMITIVE_SLOW_RISE -> "Slow rise"
        VibrationEffect.Composition.PRIMITIVE_SPIN -> "Spin"
        else -> "Primitive $primitive"
    }

    private fun supportsAllPrimitives(vibrator: Vibrator, primitives: List<Int>): Boolean {
        if (Build.VERSION.SDK_INT < 31) return false
        return vibrator.arePrimitivesSupported(*primitives.toIntArray()).all { it }
    }

    private fun waveformFallback(
        context: Context,
        steps: List<FujiHapticPrimitiveStep>,
        label: String,
        fallbackEffectId: Int,
    ): FujiHapticResult {
        val vibrator = context.defaultVibrator()
        if (!vibrator.hasVibrator()) return FujiHapticResult(false, "no vibrator")
        if (steps.isEmpty()) return FujiHapticResult(false, "empty sequence")

        val timings = mutableListOf<Long>()
        val amplitudes = mutableListOf<Int>()

        steps.forEachIndexed { index, step ->
            val delay = step.delayMs.coerceAtLeast(0).toLong()
            if (index == 0 || delay > 0) {
                timings += delay
                amplitudes += 0
            }
            timings += primitiveFallbackDurationMs(step.primitive)
            amplitudes += (255 * step.scale.coerceIn(0f, 1f)).toInt().coerceIn(1, 255)
        }

        if (timings.sum() <= 0L) return predefined(context, fallbackEffectId, "$label fallback")

        val effect = if (vibrator.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(
                timings.toLongArray(),
                amplitudes.toIntArray(),
                -1,
            )
        } else {
            VibrationEffect.createWaveform(timings.toLongArray(), -1)
        }
        vibrator.vibrate(effect)
        return FujiHapticResult(true, "$label: waveform fallback")
    }

    private fun waveformPattern(
        context: Context,
        timings: LongArray,
        amplitudes: IntArray,
        label: String,
        fallbackEffectId: Int,
    ): FujiHapticResult {
        val vibrator = context.defaultVibrator()
        if (!vibrator.hasVibrator()) return FujiHapticResult(false, "no vibrator")
        if (timings.isEmpty() || timings.size != amplitudes.size || timings.sum() <= 0L) {
            return predefined(context, fallbackEffectId, "$label fallback")
        }

        val effect = if (vibrator.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(timings, amplitudes, -1)
        } else {
            VibrationEffect.createWaveform(timings, -1)
        }
        vibrator.vibrate(effect)
        return FujiHapticResult(true, "$label: waveform")
    }

    private fun primitiveFallbackDurationMs(primitive: Int): Long = when (primitive) {
        VibrationEffect.Composition.PRIMITIVE_CLICK -> 20L
        VibrationEffect.Composition.PRIMITIVE_TICK -> 12L
        VibrationEffect.Composition.PRIMITIVE_LOW_TICK -> 10L
        VibrationEffect.Composition.PRIMITIVE_THUD -> 30L
        VibrationEffect.Composition.PRIMITIVE_QUICK_RISE -> 24L
        VibrationEffect.Composition.PRIMITIVE_QUICK_FALL -> 18L
        VibrationEffect.Composition.PRIMITIVE_SLOW_RISE -> 42L
        VibrationEffect.Composition.PRIMITIVE_SPIN -> 48L
        else -> 18L
    }

    @RequiresApi(36)
    private fun envelopeRise(context: Context): FujiHapticResult {
        val vibrator = context.defaultVibrator()
        if (!vibrator.hasVibrator() || !vibrator.areEnvelopeEffectsSupported()) {
            return FujiHapticResult(false, "envelope unsupported")
        }
        val effect = VibrationEffect.BasicEnvelopeBuilder()
            .setInitialSharpness(0.18f)
            .addControlPoint(0.18f, 0.35f, 28)
            .addControlPoint(1f, 0.95f, 64)
            .addControlPoint(0f, 0.5f, 42)
            .build()
        vibrator.vibrate(effect)
        return FujiHapticResult(true, "basic envelope")
    }

    @RequiresApi(36)
    private fun drawerSwooshOpen(context: Context): FujiHapticResult {
        val vibrator = context.defaultVibrator()
        if (!vibrator.hasVibrator() || !vibrator.areEnvelopeEffectsSupported()) {
            return FujiHapticResult(false, "envelope unsupported")
        }
        val effect = VibrationEffect.BasicEnvelopeBuilder()
            .setInitialSharpness(0.08f)
            .addControlPoint(0.10f, 0.16f, 10)
            .addControlPoint(0.24f, 0.28f, 10)
            .addControlPoint(0.58f, 0.62f, 12)
            .addControlPoint(0.92f, 0.90f, 14)
            .addControlPoint(0f, 0.55f, 22)
            .build()
        vibrator.vibrate(effect)
        return FujiHapticResult(true, "drawer open envelope")
    }

    @RequiresApi(36)
    private fun drawerSwooshDismiss(context: Context): FujiHapticResult {
        val vibrator = context.defaultVibrator()
        if (!vibrator.hasVibrator() || !vibrator.areEnvelopeEffectsSupported()) {
            return FujiHapticResult(false, "envelope unsupported")
        }
        val effect = VibrationEffect.BasicEnvelopeBuilder()
            .setInitialSharpness(0.22f)
            .addControlPoint(0.34f, 0.38f, 10)
            .addControlPoint(0.16f, 0.26f, 16)
            .addControlPoint(0f, 0.14f, 26)
            .build()
        vibrator.vibrate(effect)
        return FujiHapticResult(true, "drawer dismiss envelope")
    }

    @RequiresApi(36)
    private fun rumbleRise(context: Context): FujiHapticResult {
        val vibrator = context.defaultVibrator()
        if (!vibrator.hasVibrator() || !vibrator.areEnvelopeEffectsSupported()) {
            return FujiHapticResult(false, "envelope unsupported")
        }
        val effect = VibrationEffect.BasicEnvelopeBuilder()
            .setInitialSharpness(0.18f)
            .addControlPoint(0.12f, 0.18f, 32)
            .addControlPoint(0.36f, 0.34f, 42)
            .addControlPoint(0.72f, 0.58f, 52)
            .addControlPoint(1f, 0.95f, 60)
            .addControlPoint(0f, 0.45f, 34)
            .build()
        vibrator.vibrate(effect)
        return FujiHapticResult(true, "rumble rise envelope")
    }

    @RequiresApi(36)
    private fun rumbleFall(context: Context): FujiHapticResult {
        val vibrator = context.defaultVibrator()
        if (!vibrator.hasVibrator() || !vibrator.areEnvelopeEffectsSupported()) {
            return FujiHapticResult(false, "envelope unsupported")
        }
        val effect = VibrationEffect.BasicEnvelopeBuilder()
            .setInitialSharpness(0.42f)
            .addControlPoint(1f, 0.86f, 36)
            .addControlPoint(0.62f, 0.52f, 42)
            .addControlPoint(0.28f, 0.32f, 46)
            .addControlPoint(0f, 0.16f, 34)
            .build()
        vibrator.vibrate(effect)
        return FujiHapticResult(true, "rumble fall envelope")
    }

    @RequiresApi(36)
    private fun envelopeSnap(context: Context): FujiHapticResult {
        val vibrator = context.defaultVibrator()
        if (!vibrator.hasVibrator() || !vibrator.areEnvelopeEffectsSupported()) {
            return FujiHapticResult(false, "envelope unsupported")
        }
        val effect = VibrationEffect.WaveformEnvelopeBuilder()
            .setInitialFrequencyHz(120f)
            .addControlPoint(1f, 220f, 36)
            .addControlPoint(0.42f, 95f, 34)
            .addControlPoint(0f, 95f, 30)
            .build()
        vibrator.vibrate(effect)
        return FujiHapticResult(true, "waveform envelope")
    }

    private fun Context.defaultVibrator(): Vibrator =
        if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }

    private fun View.performSystemHaptic(constant: Int): FujiHapticResult {
        val played = performHapticFeedback(constant)
        return FujiHapticResult(
            played = played,
            path = if (played) "system feedback" else "system feedback unavailable",
        )
    }
}
