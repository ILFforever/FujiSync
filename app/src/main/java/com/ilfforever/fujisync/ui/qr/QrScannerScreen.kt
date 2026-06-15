package com.ilfforever.fujisync.ui.qr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.ilfforever.fujisync.data.qr.RecipeQr
import com.ilfforever.fujisync.ui.components.IconCamera
import com.ilfforever.fujisync.ui.components.IconClose
import com.ilfforever.fujisync.ui.components.IconImage
import com.ilfforever.fujisync.ui.components.PrimaryCTA
import com.ilfforever.fujisync.ui.model.RecipeUiModel
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelHigh
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun QrScannerScreen(
    onClose: () -> Unit,
    onDetected: (RecipeUiModel) -> Unit,
    onOpenImage: () -> Unit,
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }

    BackHandler(onBack = onClose)

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!hasCameraPermission) {
        QrScannerPermissionScreen(
            onClose = onClose,
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onOpenImage = onOpenImage,
        )
        return
    }

    QrScannerCameraScreen(
        onClose = onClose,
        onDetected = onDetected,
        onOpenImage = onOpenImage,
    )
}

@Composable
private fun QrScannerCameraScreen(
    onClose: () -> Unit,
    onDetected: (RecipeUiModel) -> Unit,
    onOpenImage: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val handled = remember { AtomicBoolean(false) }

    DisposableEffect(context, lifecycleOwner, previewView) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                val provider = providerFuture.get()
                val preview = CameraPreview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { imageAnalysis ->
                        imageAnalysis.setAnalyzer(executor) { image ->
                            val recipe = if (handled.get()) null else decodeImage(image)
                            image.close()
                            if (recipe != null && handled.compareAndSet(false, true)) {
                                mainHandler.post { onDetected(recipe) }
                            }
                        }
                    }

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            runCatching { providerFuture.get().unbindAll() }
            executor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )
        ScannerFrame(modifier = Modifier.align(Alignment.Center))
        ScannerTopBar(onClose = onClose)
        ScannerBottomBar(
            onOpenImage = onOpenImage,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun QrScannerPermissionScreen(
    onClose: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenImage: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        ScannerTopBar(onClose = onClose)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(PanelLow)
                    .border(1.dp, Border, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IconCamera, contentDescription = null, tint = Gold, modifier = Modifier.size(28.dp))
            }
            Text(
                text = "CAMERA ACCESS",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                color = TextDim,
            )
            Text(
                text = "Allow camera access to scan FujiSync recipe QR codes in real time.",
                fontFamily = SansFamily,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
            PrimaryCTA(label = "Allow Camera", onClick = onRequestPermission)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenImage)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(IconImage, contentDescription = null, tint = Gold, modifier = Modifier.size(16.dp))
                Text(
                    text = "OPEN IMAGE",
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp,
                    color = Gold,
                )
            }
        }
    }
}

@Composable
private fun ScannerTopBar(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "QR SCANNER",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 1.8.sp,
                color = TextDim,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Scan with camera",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                color = TextPrimary,
            )
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.42f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(IconClose, contentDescription = "Close", tint = TextPrimary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ScannerBottomBar(
    onOpenImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.42f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = "Hold the QR inside the frame. Import starts automatically.",
                fontFamily = SansFamily,
                fontSize = 12.5.sp,
                color = TextPrimary,
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(PanelHigh.copy(alpha = 0.94f))
                .border(1.dp, Border, RoundedCornerShape(999.dp))
                .clickable(onClick = onOpenImage)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(IconImage, contentDescription = null, tint = Gold, modifier = Modifier.size(15.dp))
            Text(
                text = "OPEN IMAGE",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                color = Gold,
            )
        }
    }
}

@Composable
private fun ScannerFrame(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(260.dp)
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, Gold.copy(alpha = 0.7f), RoundedCornerShape(28.dp)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val corner = 42.dp.toPx()
            val inset = 13.dp.toPx()
            val stroke = 3.dp.toPx()
            val gold = Gold
            drawLine(gold, Offset(inset, inset), Offset(inset + corner, inset), stroke)
            drawLine(gold, Offset(inset, inset), Offset(inset, inset + corner), stroke)
            drawLine(gold, Offset(size.width - inset, inset), Offset(size.width - inset - corner, inset), stroke)
            drawLine(gold, Offset(size.width - inset, inset), Offset(size.width - inset, inset + corner), stroke)
            drawLine(gold, Offset(inset, size.height - inset), Offset(inset + corner, size.height - inset), stroke)
            drawLine(gold, Offset(inset, size.height - inset), Offset(inset, size.height - inset - corner), stroke)
            drawLine(gold, Offset(size.width - inset, size.height - inset), Offset(size.width - inset - corner, size.height - inset), stroke)
            drawLine(gold, Offset(size.width - inset, size.height - inset), Offset(size.width - inset, size.height - inset - corner), stroke)
        }
    }
}

private fun decodeImage(image: ImageProxy): RecipeUiModel? {
    val yPlane = image.planes.firstOrNull() ?: return null
    val buffer = yPlane.buffer
    val data = ByteArray(buffer.remaining())
    buffer.get(data)
    return RecipeQr.decodeYPlane(
        data = data,
        dataWidth = yPlane.rowStride,
        dataHeight = image.height,
        frameWidth = image.width,
        frameHeight = image.height,
    )
}
