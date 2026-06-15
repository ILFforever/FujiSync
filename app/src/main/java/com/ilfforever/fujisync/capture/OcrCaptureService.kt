package com.ilfforever.fujisync.capture

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.ilfforever.fujisync.MainActivity
import com.ilfforever.fujisync.R
import java.io.File
import java.io.FileOutputStream

class OcrCaptureService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val ACTION_OCR_TILE_RESULT = "com.ilfforever.fujisync.OCR_TILE_RESULT"
        const val EXTRA_CAPTURE_URI = "capture_uri"
        const val EXTRA_CAPTURE_TOKEN = "capture_token"
        private const val NOTIF_CHANNEL_ID = "ocr_capture"
        private const val NOTIF_ID = 9001
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val thread = HandlerThread("ocr-capture").also { it.start() }
    private val bgHandler = Handler(thread.looper)
    private var captureHandled = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(NOTIF_CHANNEL_ID, "OCR Capture", NotificationManager.IMPORTANCE_LOW)
        )
        val notif = NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile_ocr)
            .setContentTitle("Scanning recipe…")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, -1) ?: -1
        val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        CaptureDiag.log(this, "service onStartCommand: resultCode=$resultCode dataNull=${data == null}")
        if (resultCode != android.app.Activity.RESULT_OK || data == null) { stopSelf(); return START_NOT_STICKY }

        val mgr = getSystemService(MediaProjectionManager::class.java)
        CaptureDiag.log(this, "calling getMediaProjection")
        try {
            projection = mgr.getMediaProjection(resultCode, data)
        } catch (e: Exception) {
            CaptureDiag.log(this, "getMediaProjection threw: ${e.javaClass.simpleName}: ${e.message}")
            stopSelf(); return START_NOT_STICKY
        }
        CaptureDiag.log(this, "projection=${projection != null}")
        if (projection == null) { CaptureDiag.log(this, "projection null, aborting"); stopSelf(); return START_NOT_STICKY }

        val width: Int
        val height: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = getSystemService(android.view.WindowManager::class.java)
                .currentWindowMetrics.bounds
            width = bounds.width()
            height = bounds.height()
        } else {
            @Suppress("DEPRECATION")
            val metrics = DisplayMetrics().also {
                getSystemService(android.view.WindowManager::class.java)
                    .defaultDisplay.getRealMetrics(it)
            }
            width = metrics.widthPixels
            height = metrics.heightPixels
        }
        val density = resources.displayMetrics.densityDpi
        CaptureDiag.log(this, "display: ${width}x${height} dpi=$density")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            projection!!.registerCallback(object : android.media.projection.MediaProjection.Callback() {
                override fun onStop() { cleanup() }
            }, bgHandler)
        }

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        try {
            virtualDisplay = projection!!.createVirtualDisplay(
                "ocr-cap", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader!!.surface, null, bgHandler,
            )
        } catch (e: Exception) {
            CaptureDiag.log(this, "createVirtualDisplay threw: ${e.javaClass.simpleName}: ${e.message}")
            stopSelf(); return START_NOT_STICKY
        }

        CaptureDiag.log(this, "VirtualDisplay created ${width}x${height}, waiting for frame")
        imageReader!!.setOnImageAvailableListener({ reader ->
            if (captureHandled) return@setOnImageAvailableListener
            captureHandled = true
            CaptureDiag.log(this, "image frame available")
            val image = reader.acquireLatestImage() ?: run {
                CaptureDiag.log(this, "acquireLatestImage returned null")
                return@setOnImageAvailableListener
            }
            try {
                val plane = image.planes[0]
                val rowPadding = plane.rowStride - plane.pixelStride * width
                val bmp = Bitmap.createBitmap(
                    width + rowPadding / plane.pixelStride, height, Bitmap.Config.ARGB_8888
                )
                bmp.copyPixelsFromBuffer(plane.buffer)
                val cropped = Bitmap.createBitmap(bmp, 0, 0, width, height)
                bmp.recycle()
                saveAndDispatch(cropped)
            } finally {
                image.close()
                cleanup()
            }
        }, bgHandler)

        return START_NOT_STICKY
    }

    private fun saveAndDispatch(bitmap: Bitmap) {
        val dir = File(cacheDir, "ocr_captures").also { it.mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "capture.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()

        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val token = System.nanoTime().toString()
        val result = Intent(ACTION_OCR_TILE_RESULT).apply {
            setPackage(packageName)
            putExtra(EXTRA_CAPTURE_URI, uri.toString())
            putExtra(EXTRA_CAPTURE_TOKEN, token)
        }
        val launch = Intent(result).apply {
            setClass(this@OcrCaptureService, MainActivity::class.java)
            action = ACTION_OCR_TILE_RESULT
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        CaptureDiag.log(this, "saved capture, uri=$uri, token=$token, dispatching result")
        dispatchResultBroadcast(result)
        try {
            startActivity(launch)
            CaptureDiag.log(this, "startActivity returned OK")
        } catch (e: Exception) {
            CaptureDiag.log(this, "startActivity threw: ${e.javaClass.simpleName}: ${e.message}")
        }
        Handler(mainLooper).postDelayed({ dispatchResultBroadcast(result) }, 250L)
    }

    private fun dispatchResultBroadcast(intent: Intent) {
        try {
            sendBroadcast(intent)
            CaptureDiag.log(this, "sendBroadcast returned OK")
        } catch (e: Exception) {
            CaptureDiag.log(this, "sendBroadcast threw: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun cleanup() {
        virtualDisplay?.release()
        projection?.stop()
        virtualDisplay = null
        projection = null
        imageReader?.close()
        imageReader = null
        stopSelf()
    }

    override fun onDestroy() {
        cleanup()
        thread.quitSafely()
        super.onDestroy()
    }
}
