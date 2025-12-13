package dev.js.qrcrawlingpractice

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun WebViewScreen(url: String, onImageUrlDetected: (String) -> Unit, onBackPressed: () -> Unit) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }

    // 뒤로가기 버튼 처리
    BackHandler {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            onBackPressed()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webView = this
                    webViewClient = WebViewClient()

                    // WebChromeClient 설정 (JavaScript 콘솔 로그 출력)
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                            Log.d("WebViewConsole", "${consoleMessage.message()} -- From line ${consoleMessage.lineNumber()} of ${consoleMessage.sourceId()}")
                            return true
                        }
                    }

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                    }

                    // 다운로드 리스너 설정
                    setDownloadListener { downloadUrl, _, _, _, _ ->
                        Log.d("WebViewDownload", "=== Download Detected ===")
                        Log.d("WebViewDownload", "URL: $downloadUrl")

                        // blob URL 처리 - 현재 페이지 URL에서 실제 이미지 URL 추출
                        if (downloadUrl.startsWith("blob:")) {
                            Log.d("WebViewDownload", "Detected blob URL, extracting real image URL...")
                            extractRealImageUrl(this, onImageUrlDetected)
                        }
                    }

                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { webView ->
                webView.loadUrl(url)
            }
        )
    }
}

// 현재 페이지 URL에서 실제 이미지 URL 추출
private fun extractRealImageUrl(webView: WebView, onImageUrlDetected: (String) -> Unit) {
    val currentUrl = webView.url ?: return
    Log.d("WebViewDownload", "Current page URL: $currentUrl")

    // https://photogray-download.aprd.io?id={base64} 형태에서 id 파라미터 추출
    val uri = Uri.parse(currentUrl)
    val encodedId = uri.getQueryParameter("id")

    if (encodedId != null) {
        try {
            // Base64 디코드
            val decodedBytes = android.util.Base64.decode(encodedId, android.util.Base64.DEFAULT)
            val decodedStr = String(decodedBytes)
            Log.d("WebViewDownload", "Decoded query string: $decodedStr")

            // sessionId 추출
            val sessionIdUri = Uri.parse("?$decodedStr")
            val sessionId = sessionIdUri.getQueryParameter("sessionId")

            if (sessionId != null) {
                // 실제 이미지 URL 생성
                val imageUrl = "https://pg-qr-resource.aprd.io/$sessionId/image.jpg"
                Log.d("WebViewDownload", "=== Real Image URL Found ===")
                Log.d("WebViewDownload", "Image URL: $imageUrl")
                Log.d("WebViewDownload", "========================")

                // 이미지 URL을 콜백으로 전달
                Handler(Looper.getMainLooper()).post {
                    onImageUrlDetected(imageUrl)
                }
            } else {
                Log.e("WebViewDownload", "SessionId not found in decoded string")
            }
        } catch (e: Exception) {
            Log.e("WebViewDownload", "Failed to extract image URL", e)
        }
    } else {
        Log.e("WebViewDownload", "ID parameter not found in URL")
    }
}
