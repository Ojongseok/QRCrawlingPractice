package dev.js.qrcrawlingpractice

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dev.js.qrcrawlingpractice.ui.theme.QRCrawlingPracticeTheme
import androidx.compose.foundation.layout.Box

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContent {
            QRCrawlingPracticeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    QRScannerApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class, ExperimentalPermissionsApi::class)
@Composable
fun QRScannerApp(modifier: Modifier = Modifier) {
    var showScanner by remember { mutableStateOf(false) }
    var detectedUrl by remember { mutableStateOf<String?>(null) }
    var imageUrl by remember { mutableStateOf<String?>(null) }

    when {
        imageUrl != null -> {
            // 이미지 URL로 이미지 표시
            ImageDisplayScreen(
                imageUrl = imageUrl!!,
                onScanAgain = {
                    imageUrl = null
                    detectedUrl = null
                    showScanner = false
                }
            )
        }
        detectedUrl != null -> {
            // WebView 화면 표시
            WebViewScreen(
                url = detectedUrl!!,
                onImageUrlDetected = { url ->
                    imageUrl = url
                },
                onBackPressed = {
                    // 뒤로가기로 WebView 닫을 때
                    if (imageUrl == null) {
                        // 이미지 URL이 없으면 QR 스캐너로
                        detectedUrl = null
                    }
                    // 이미지 URL이 있으면 imageUrl != null이므로 이미지 화면으로 자동 전환
                }
            )
        }
        showScanner -> {
            // QR 스캐너 화면 표시
            QRScannerWithPermission(
                modifier = modifier,
                onUrlDetected = { url ->
                    detectedUrl = url
                }
            )
        }
        else -> {
            // 홈 화면
            HomeScreen(
                onStartScan = {
                    showScanner = true
                }
            )
        }
    }
}

@Composable
fun HomeScreen(onStartScan: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "QR 코드 스캐너",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 40.dp)
            )

            Button(
                onClick = onStartScan,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "스캔하기",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ImageDisplayScreen(imageUrl: String, onScanAgain: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 상단 타이틀
        Text(
            text = "추출된 이미지",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 100.dp, bottom = 16.dp)
        )

        // 이미지
        Image(
            painter = rememberAsyncImagePainter(imageUrl),
            contentDescription = "Downloaded Image",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 40.dp, vertical = 16.dp),
            contentScale = ContentScale.Fit
        )

        // 하단 버튼
        Button(
            onClick = onScanAgain,
            modifier = Modifier.padding(horizontal = 40.dp, vertical = 40.dp)
        ) {
            Text("다시 스캔하기")
        }
    }
}

@ExperimentalGetImage
@ExperimentalPermissionsApi
@Composable
fun QRScannerWithPermission(
    modifier: Modifier = Modifier,
    onUrlDetected: (String) -> Unit
) {
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    if (cameraPermissionState.status.isGranted) {
        QRScannerScreen(onUrlDetected = onUrlDetected)
    } else {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "카메라 권한이 필요합니다. 설정에서 권한을 허용해주세요.")
        }

        // Request permission
        androidx.compose.runtime.LaunchedEffect(Unit) {
            cameraPermissionState.launchPermissionRequest()
        }
    }
}
