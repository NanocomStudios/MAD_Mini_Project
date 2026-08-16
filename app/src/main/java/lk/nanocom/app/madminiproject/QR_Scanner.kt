package lk.nanocom.app.madminiproject

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class QrCodeAnalyzer(
    private val onQrCodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    // Configure ML Kit to scan specifically for QR codes
    private val scanner = BarcodeScanning.getClient()
    private var lastAnalyzedTimestamp = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        // Throttling: only process one frame every 1000ms (1 second)
        if (currentTime - lastAnalyzedTimestamp >= 1000) {
            lastAnalyzedTimestamp = currentTime

            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            // Check if the detected barcode format is a QR Code
                            if (barcode.valueType != Barcode.TYPE_UNKNOWN) {
                                barcode.rawValue?.let { qrValue ->
                                    onQrCodeDetected(qrValue)
                                }
                            }
                        }
                    }
                    .addOnFailureListener {
                        // Handle failure gracefully
                    }
                    .addOnCompleteListener {
                        // Crucial: Close the proxy to release the frame buffer for the next image
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        } else {
            // Drop the frame to slow down the analysis rate
            imageProxy.close()
        }
    }
}

@Composable
fun QrScannerPreview(
    onQrDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context) }

    // Dedicated executor to handle background image analysis
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // 1. Setup Camera Preview Surface
                val preview = androidx.camera.core.Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                // 2. Setup Image Analyzer linked with our ML Kit logic
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, QrCodeAnalyzer { qrCodeValue ->
                            onQrDetected(qrCodeValue)
                        })
                    }

                // 3. Select standard back camera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    // Unbind all use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind preview and analysis workflows to lifecycle
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    Log.e("QrScanner", "Use case binding failed", exc)
                }

            }, ContextCompat.getMainExecutor(context))

            previewView
        },
        modifier = modifier.fillMaxSize()
    )
}

@Composable
fun QrScannerScreen(
    onQrIDDetected: (String) -> Unit
) {
    var isDetected by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize()) {
        // Render Camera Preview Layout
        if(!isDetected) {
            QrScannerPreview(
                onQrDetected = { code ->
                    isDetected = true
                    onQrIDDetected(code)
                }
            )
        }
    }
}