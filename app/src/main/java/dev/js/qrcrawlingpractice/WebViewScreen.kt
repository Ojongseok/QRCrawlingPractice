package dev.js.qrcrawlingpractice

import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
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
import androidx.compose.ui.text.Paragraph
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

                    // 2. 크기가 제대로 잡히는지 강제 설정
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                    // WebViewClient 설정
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            Log.d("WebView", "Page started loading: $url")
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            Log.d("WebView", "Page finished loading: $url")
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            Log.e("WebView", "Error loading page: ${error?.description}")
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            Log.d("WebView", "Loading URL: ${request?.url}")
                            return false
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): android.webkit.WebResourceResponse? {
                            val url = request?.url.toString()

                            // Photoism: S3 이미지 URL 감지
                            if (url.contains("photoism-cms-prd.s3.ap-northeast-2.amazonaws.com", ignoreCase = true) &&
                                (url.endsWith(".jpg", ignoreCase = true) || url.endsWith(".png", ignoreCase = true))) {

                                Log.d("Photoism", "=== S3 Image URL Detected ===")
                                Log.d("Photoism", "Image URL: $url")
                                Log.d("Photoism", "========================")

                                Handler(Looper.getMainLooper()).post {
                                    onImageUrlDetected(url)
                                }
                            }

                            // Photosignature: 경로 패턴 감지 (o.png, video.mp4 등)
                            if ((url.contains("photoqr.kr/R/", ignoreCase = true) ||
                                 url.contains("photoqr3.kr/R/", ignoreCase = true)) &&
                                (url.endsWith("o.png", ignoreCase = true) ||
                                 url.endsWith("video.mp4", ignoreCase = true))) {

                                // 경로에서 마지막 파일명을 a.jpg로 교체
                                val basePath = url.substringBeforeLast("/")
                                val imageUrl = "$basePath/a.jpg"

                                Log.d("Photosignature", "=== Path Pattern Detected ===")
                                Log.d("Photosignature", "Original URL: $url")
                                Log.d("Photosignature", "Image URL: $imageUrl")
                                Log.d("Photosignature", "========================")

                                Handler(Looper.getMainLooper()).post {
                                    onImageUrlDetected(imageUrl)
                                }
                            }

                            // 모든 요청 로깅
                            Log.d("WebViewRequest", "Request: $url")

                            return super.shouldInterceptRequest(view, request)
                        }
                    }

                    // WebChromeClient 설정 (JavaScript 콘솔 로그 출력)
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                            Log.d("WebViewConsole", "${consoleMessage.message()} -- From line ${consoleMessage.lineNumber()} of ${consoleMessage.sourceId()}")
                            return true
                        }

                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            Log.d("WebView", "Loading progress: $newProgress%")
                        }
                    }

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                        // 추가 설정
                        loadsImagesAutomatically = true
                        blockNetworkImage = false
                        blockNetworkLoads = false

                        // 기타 설정
                        databaseEnabled = true
                        javaScriptCanOpenWindowsAutomatically = true
                        mediaPlaybackRequiresUserGesture = false
                    }

                    // 다운로드 리스너 설정
                    setDownloadListener { downloadUrl, _, _, _, _ ->
                        Log.d("WebViewDownload", "=== Download Detected ===")
                        Log.d("WebViewDownload", "URL: $downloadUrl")

                        // 브랜드 감지 및 처리
                        val brand = detectBrand(downloadUrl, this.url)
                        Log.d("WebViewDownload", "Detected brand: ${brand?.name ?: "Unknown"}")

                        brand?.let {
                            handleBrandDownload(it, this, onImageUrlDetected)
                        }
                    }

                    Log.d("WebView", "Initial load URL: $url")
                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

// 브랜드 정의
sealed class PhotoBrand(
    val name: String,
    val keywords: List<String>
) {
    object PhotoGray : PhotoBrand("PhotoGray", listOf("pg-qr-resource.aprd.io", "photogray-download.aprd.io"))
    object Photoism : PhotoBrand("Photoism", listOf("qr.seobuk.kr"))
    object Photosignature : PhotoBrand("Photosignature", listOf("photoqr.kr", "photoqr3.kr"))
    // 추가 브랜드는 여기에 정의
}

// 브랜드 감지
private fun detectBrand(downloadUrl: String, currentPageUrl: String?): PhotoBrand? {
    val urlToCheck = "$downloadUrl $currentPageUrl"

    return when {
        PhotoBrand.PhotoGray.keywords.any { urlToCheck.contains(it, ignoreCase = true) } -> PhotoBrand.PhotoGray
        PhotoBrand.Photoism.keywords.any { urlToCheck.contains(it, ignoreCase = true) } -> PhotoBrand.Photoism
        PhotoBrand.Photosignature.keywords.any { urlToCheck.contains(it, ignoreCase = true) } -> PhotoBrand.Photosignature
        else -> null
    }
}


// 브랜드별 다운로드 처리
private fun handleBrandDownload(brand: PhotoBrand, webView: WebView, onImageUrlDetected: (String) -> Unit) {
    when (brand) {
        is PhotoBrand.PhotoGray -> extractPhotoGrayImageUrl(webView, onImageUrlDetected)
        is PhotoBrand.Photoism -> extractPhotoismImageUrl(webView, onImageUrlDetected)
        is PhotoBrand.Photosignature -> extractPhotosignatureImageUrl(webView, onImageUrlDetected)
    }
}

// PhotoGray 이미지 URL 추출
private fun extractPhotoGrayImageUrl(webView: WebView, onImageUrlDetected: (String) -> Unit) {
    Log.d("PhotoGray", "Extracting PhotoGray image URL")
    val currentUrl = webView.url ?: return
    Log.d("PhotoGray", "Current page URL: $currentUrl")

    // https://photogray-download.aprd.io?id={base64} 형태에서 id 파라미터 추출
    val uri = Uri.parse(currentUrl)
    val encodedId = uri.getQueryParameter("id")

    if (encodedId != null) {
        try {
            // Base64 디코드
            val decodedBytes = android.util.Base64.decode(encodedId, android.util.Base64.DEFAULT)
            val decodedStr = String(decodedBytes)
            Log.d("PhotoGray", "Decoded query string: $decodedStr")

            // sessionId 추출
            val sessionIdUri = Uri.parse("?$decodedStr")
            val sessionId = sessionIdUri.getQueryParameter("sessionId")

            if (sessionId != null) {
                // 실제 이미지 URL 생성
                val imageUrl = "https://pg-qr-resource.aprd.io/$sessionId/image.jpg"
                Log.d("PhotoGray", "=== Real Image URL Found ===")
                Log.d("PhotoGray", "Image URL: $imageUrl")
                Log.d("PhotoGray", "========================")

                // 이미지 URL을 콜백으로 전달
                Handler(Looper.getMainLooper()).post {
                    onImageUrlDetected(imageUrl)
                }
            } else {
                Log.e("PhotoGray", "SessionId not found in decoded string")
            }
        } catch (e: Exception) {
            Log.e("PhotoGray", "Failed to extract image URL", e)
        }
    } else {
        Log.e("PhotoGray", "ID parameter not found in URL")
    }
}

// Photoism 이미지 URL 추출 (Request Interceptor 방식 사용 - shouldInterceptRequest에서 처리)
private fun extractPhotoismImageUrl(webView: WebView, onImageUrlDetected: (String) -> Unit) {
    Log.d("Photoism", "Photoism uses request interceptor method")
    Log.d("Photoism", "Image will be detected via shouldInterceptRequest")
}

// Photosignature 이미지 URL 추출
private fun extractPhotosignatureImageUrl(webView: WebView, onImageUrlDetected: (String) -> Unit) {
    Log.d("Photosignature", "Extracting Photosignature image URL")
    val currentUrl = webView.url ?: return
    Log.d("Photosignature", "Current page URL: $currentUrl")

    // TODO: Photosignature 로직 구현
    // Photosignature의 URL 구조에 맞게 이미지 URL 추출 로직 추가
    try {
        // 여기에 Photosignature 로직 구현
        Log.d("Photosignature", "Waiting for implementation...")
    } catch (e: Exception) {
        Log.e("Photosignature", "Failed to extract image URL", e)
    }
}
