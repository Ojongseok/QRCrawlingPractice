package dev.js.qrcrawlingpractice

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dev.js.qrcrawlingpractice.ui.theme.QRCrawlingPracticeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
    QRScannerWithPermission(modifier = modifier)
}

@ExperimentalGetImage
@ExperimentalPermissionsApi
@Composable
fun QRScannerWithPermission(modifier: Modifier = Modifier) {
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    if (cameraPermissionState.status.isGranted) {
        QRScannerScreen()
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