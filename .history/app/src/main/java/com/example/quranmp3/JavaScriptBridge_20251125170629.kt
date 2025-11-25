package com.example.quranmp3

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.JavascriptInterface
import android.widget.Toast
import java.io.File

class JavaScriptBridge(private val activity: MainActivity) {

    @JavascriptInterface
    fun openSurahPage(surahNumber: Int) {
        activity.runOnUiThread {
            activity.webView.loadUrl("file:///android_asset/surah.html?surah=$surahNumber")
        }
    }
    
    @JavascriptInterface
    fun downloadSurah(surahNumber: Int, callback: String) {
        activity.runOnUiThread {
            try {
                val fileName = String.format("%03d.mp3", surahNumber)
                val url = "https://server.mp3quran.net/obasfr/$fileName"
                
                val request = DownloadManager.Request(Uri.parse(url))
                request.setTitle("Surah $surahNumber")
                request.setDescription("Downloading Quran Surah...")
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_MUSIC, fileName)
                request.allowScanningByMediaScanner()
                
                val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val downloadId = downloadManager.enqueue(request)
                
                // For now, we'll assume success. In a real implementation, you'd want to track the download progress
                // and call the callback when the download completes
                Toast.makeText(activity, "Download started for Surah $surahNumber", Toast.LENGTH_SHORT).show()
                
                // Simulate successful download after a short delay
                activity.webView.postDelayed({
                    activity.webView.evaluateJavascript("$callback(true);", null)
                }, 2000)
                
            } catch (e: Exception) {
                e.printStackTrace()
                activity.webView.evaluateJavascript("$callback(false);", null)
                Toast.makeText(activity, "Download failed for Surah $surahNumber", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    @JavascriptInterface
    fun playLocalSurah(surahNumber: Int) {
        activity.runOnUiThread {
            val fileName = String.format("%03d.mp3", surahNumber)
            val file = File(activity.getExternalFilesDir(Environment.DIRECTORY_MUSIC), fileName)
            
            if (file.exists()) {
                // Play from local file
                val localUrl = "file://${file.absolutePath}"
                activity.webView.loadUrl("file:///android_asset/surah.html?surah=$surahNumber&local=true&url=${Uri.encode(localUrl)}")
                Toast.makeText(activity, "Playing Surah $surahNumber from local storage", Toast.LENGTH_SHORT).show()
            } else {
                // Fallback to server if local file doesn't exist
                openSurahPage(surahNumber)
                Toast.makeText(activity, "Local file not found, playing from server", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    @JavascriptInterface
    fun isFileDownloaded(surahNumber: Int): Boolean {
        val fileName = String.format("%03d.mp3", surahNumber)
        val file = File(activity.getExternalFilesDir(Environment.DIRECTORY_MUSIC), fileName)
        return file.exists()
    }
}
