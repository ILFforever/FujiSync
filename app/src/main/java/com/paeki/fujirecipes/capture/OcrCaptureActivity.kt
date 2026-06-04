package com.paeki.fujirecipes.capture

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Transparent no-UI activity whose sole job is to present the system MediaProjection
 * permission dialog and hand the result token off to OcrCaptureService.
 *
 * Launched by OcrTileService via startActivityAndCollapse, which collapses the QS panel
 * before the activity becomes visible. The 650 ms delay lets that animation finish so
 * the capture doesn't include the QS overlay.
 */
class OcrCaptureActivity : ComponentActivity() {

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        CaptureDiag.log(this, "projection result: resultCode=${result.resultCode} dataNull=${result.data == null}")
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            CaptureDiag.log(this, "starting OcrCaptureService")
            startForegroundService(
                Intent(this, OcrCaptureService::class.java).apply {
                    putExtra(OcrCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                    putExtra(OcrCaptureService.EXTRA_RESULT_DATA, result.data)
                }
            )
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CaptureDiag.log(this, "capture activity created, launching projection dialog in 650ms")
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing) {
                val mgr = getSystemService(MediaProjectionManager::class.java)
                projectionLauncher.launch(mgr.createScreenCaptureIntent())
            }
        }, 650L)
    }
}
