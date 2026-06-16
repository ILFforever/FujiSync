package com.ilfforever.fujisync.ui.detail

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.data.qr.RecipeQr
import com.ilfforever.fujisync.ui.components.IconClose
import com.ilfforever.fujisync.ui.components.PrimaryCTA
import com.ilfforever.fujisync.ui.model.RecipeUiModel
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal fun RecipeQrSheet(
    recipe: RecipeUiModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = recipe) {
        value = withContext(Dispatchers.Default) {
            RecipeQr.createBitmap(RecipeQr.encode(recipe))
        }
    }
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    var entered by remember { mutableStateOf(!motionEnabled) }
    var dismissing by remember { mutableStateOf(false) }

    fun requestDismiss() {
        if (dismissing) return
        if (!motionEnabled) {
            onDismiss()
        } else {
            dismissing = true
            entered = false
        }
    }

    LaunchedEffect(motionEnabled) {
        entered = true
    }

    LaunchedEffect(dismissing) {
        if (dismissing) {
            delay(if (motionEnabled) 220 else 0)
            onDismiss()
        }
    }

    BackHandler(onBack = ::requestDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(onClick = ::requestDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = entered,
            enter = slideInVertically(
                animationSpec = tween(if (motionEnabled) 300 else 0, easing = FastOutSlowInEasing),
                initialOffsetY = { it },
            ),
            exit = slideOutVertically(
                animationSpec = tween(if (motionEnabled) 210 else 0, easing = FastOutSlowInEasing),
                targetOffsetY = { it },
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(PanelLow)
                    .border(1.dp, Border, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(onClick = {})
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp)
                    .padding(top = 14.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(TextDim.copy(alpha = 0.42f)),
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "RECIPE QR",
                            fontFamily = MonoFamily,
                            fontSize = 10.sp,
                            letterSpacing = 1.8.sp,
                            color = TextDim,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = recipe.name,
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            lineHeight = 24.sp,
                            color = TextPrimary,
                        )
                    }
                    IconButton(
                        onClick = ::requestDismiss,
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(IconClose, contentDescription = "Close", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val readyBitmap = bitmap
                    if (readyBitmap != null) {
                        Image(
                            bitmap = readyBitmap.asImageBitmap(),
                            contentDescription = "Recipe QR code",
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = Gold,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Building QR",
                                fontFamily = MonoFamily,
                                fontSize = 10.sp,
                                letterSpacing = 1.5.sp,
                                color = Bg.copy(alpha = 0.68f),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Scan with FujiSync to import this recipe.",
                    fontFamily = SansFamily,
                    fontSize = 12.5.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                PrimaryCTA(
                    label = "Share Recipe Card",
                    busy = false,
                    enabled = bitmap != null,
                    onClick = {
                        bitmap?.let { qr ->
                            scope.launch(Dispatchers.IO) {
                                shareRecipeQr(context, recipe, qr)
                            }
                        }
                    },
                )
            }
        }
    }
}

internal fun shareRecipeQr(context: Context, recipe: RecipeUiModel, qrBitmap: Bitmap) {
    val referenceBitmap = decodeShareReferenceBitmap(context, recipe.referenceImageUris.firstOrNull())
    // Generate QR at card size (280px) so it draws 1:1 on the card with no scaling distortion.
    val cardQr = RecipeQr.createBitmap(RecipeQr.encode(recipe), 280)
    val card = createRecipeShareCard(recipe, cardQr, referenceBitmap)
    cardQr.recycle()
    referenceBitmap?.recycle()
    val dir = File(context.cacheDir, "qr_codes").also { it.mkdirs() }
    val safeName = recipe.name
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "recipe" }
    val file = File(dir, "$safeName.png")
    file.outputStream().use { output ->
        card.compress(Bitmap.CompressFormat.PNG, 100, output)
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, recipe.name)
        putExtra(Intent.EXTRA_TEXT, "FujiSync recipe: ${recipe.name}")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
