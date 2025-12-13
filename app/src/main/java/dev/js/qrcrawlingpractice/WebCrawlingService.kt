package dev.js.qrcrawlingpractice

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jsoup.Jsoup

class WebCrawlingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("WebCrawlingService", "Service started")

        // 고정된 URL로 크롤링 시작
        val targetUrl = "https://www.hankyung.com/article/2025121210367"

        serviceScope.launch {
            try {
                crawlImages(targetUrl)
            } catch (e: Exception) {
                Log.e("WebCrawlingService", "Crawling failed", e)
            } finally {
                // 크롤링 완료 후 서비스 종료
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun crawlImages(url: String) {
        try {
            Log.d("WebCrawlingService", "Starting crawling: $url")

            val document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get()

            // figure-img 클래스를 가진 div 찾기
            val figureImgDivs = document.select("div.figure-img")

            Log.d("WebCrawlingService", "Found ${figureImgDivs.size} div.figure-img elements")

            // 각 div에서 img 태그 찾기
            figureImgDivs.forEach { div ->
                val imgElements = div.select("img[src]")

                imgElements.forEach { img ->
                    val imgSrc = img.attr("abs:src") // 절대 URL 가져오기

                    // .jpg 확장자인지 확인
                    if (imgSrc.endsWith(".jpg", ignoreCase = true)) {
                        Log.d("WebCrawlingService", "Found JPG image: $imgSrc")
                    }
                }
            }

            Log.d("WebCrawlingService", "Crawling completed")

        } catch (e: Exception) {
            Log.e("WebCrawlingService", "Error during crawling", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("WebCrawlingService", "Service destroyed")
    }
}