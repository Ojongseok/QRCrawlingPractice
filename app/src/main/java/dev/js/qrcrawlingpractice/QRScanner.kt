package dev.js.qrcrawlingpractice

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@androidx.camera.core.ExperimentalGetImage
@Composable
fun QRScannerScreen(onUrlDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var detectedQRCode by remember { mutableStateOf<String?>(null) }
    var hasProcessedUrl by remember { mutableStateOf(false) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // URL 감지 시 콜백 호출
    LaunchedEffect(detectedQRCode) {
        detectedQRCode?.let { qrCode ->
            if (!hasProcessedUrl) {
                Log.d("QRScanner", "QR Code detected: $qrCode")

                // 카메라 정지
                cameraProvider?.unbindAll()
                Log.d("QRScanner", "Camera stopped")

                // WebView로 전환
                onUrlDetected(qrCode)
                hasProcessedUrl = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = ContextCompat.getMainExecutor(ctx)

                cameraProviderFuture.addListener({
                    val provider = cameraProviderFuture.get()
                    cameraProvider = provider

                    // Preview
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    // Image Analysis for QR Code detection
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val barcodeScanner = BarcodeScanning.getClient()
                    val analysisExecutor = Executors.newSingleThreadExecutor()

                    imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        processImageProxy(barcodeScanner, imageProxy) { barcodes ->
                            barcodes.firstOrNull()?.rawValue?.let { qrCodeValue ->
                                if (detectedQRCode != qrCodeValue) {
                                    detectedQRCode = qrCodeValue
                                    hasProcessedUrl = false
                                }
                            }
                        }
                    }

                    // Camera selector
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("QRScanner", "Camera binding failed", e)
                    }
                }, executor)

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // 어두운 오버레이 + 스캔 영역
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // 스캔 영역 크기 (화면의 60%)
            val scanSize = canvasWidth * 0.6f
            val left = (canvasWidth - scanSize) / 2
            val top = (canvasHeight - scanSize) / 2

            // 전체 화면을 어둡게
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                size = Size(canvasWidth, canvasHeight)
            )

            // 스캔 영역을 투명하게 (구멍 뚫기)
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(scanSize, scanSize),
                cornerRadius = CornerRadius(16f, 16f),
                blendMode = BlendMode.Clear
            )

            // 스캔 영역 테두리
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(left, top),
                size = Size(scanSize, scanSize),
                cornerRadius = CornerRadius(16f, 16f),
                style = Stroke(width = 4f)
            )

            // 모서리 강조선
            val cornerLength = 40f
            val cornerWidth = 6f

            // 왼쪽 위
            drawLine(
                color = Color(0xFF4CAF50),
                start = Offset(left, top),
                end = Offset(left + cornerLength, top),
                strokeWidth = cornerWidth
            )
            drawLine(
                color = Color(0xFF4CAF50),
                start = Offset(left, top),
                end = Offset(left, top + cornerLength),
                strokeWidth = cornerWidth
            )

            // 오른쪽 위
            drawLine(
                color = Color(0xFF4CAF50),
                start = Offset(left + scanSize, top),
                end = Offset(left + scanSize - cornerLength, top),
                strokeWidth = cornerWidth
            )
            drawLine(
                color = Color(0xFF4CAF50),
                start = Offset(left + scanSize, top),
                end = Offset(left + scanSize, top + cornerLength),
                strokeWidth = cornerWidth
            )

            // 왼쪽 아래
            drawLine(
                color = Color(0xFF4CAF50),
                start = Offset(left, top + scanSize),
                end = Offset(left + cornerLength, top + scanSize),
                strokeWidth = cornerWidth
            )
            drawLine(
                color = Color(0xFF4CAF50),
                start = Offset(left, top + scanSize),
                end = Offset(left, top + scanSize - cornerLength),
                strokeWidth = cornerWidth
            )

            // 오른쪽 아래
            drawLine(
                color = Color(0xFF4CAF50),
                start = Offset(left + scanSize, top + scanSize),
                end = Offset(left + scanSize - cornerLength, top + scanSize),
                strokeWidth = cornerWidth
            )
            drawLine(
                color = Color(0xFF4CAF50),
                start = Offset(left + scanSize, top + scanSize),
                end = Offset(left + scanSize, top + scanSize - cornerLength),
                strokeWidth = cornerWidth
            )
        }

        // 상단 안내 문구
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "QR 코드를 스캔 영역에 맞춰주세요",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }

        // Display detected QR code
        detectedQRCode?.let { qrCode ->
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "QR Code: $qrCode",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .background(
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@androidx.camera.core.ExperimentalGetImage
private fun processImageProxy(
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onBarcodeDetected: (List<Barcode>) -> Unit
) {
    imageProxy.image?.let { image ->
        val inputImage = InputImage.fromMediaImage(
            image,
            imageProxy.imageInfo.rotationDegrees
        )

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    onBarcodeDetected(barcodes)
                }
            }
            .addOnFailureListener { e ->
                Log.e("QRScanner", "Barcode scanning failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } ?: imageProxy.close()
}