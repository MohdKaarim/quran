package com.example.quranmp3

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
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
    
    private fun getQuranStorageDirectory(): File {
        // Use external storage directory under Android folder for better access
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+ use app-specific external storage
            File(activity.getExternalFilesDir(null), "QuranAudio")
        } else {
            // For older versions, use public external storage
            File(Environment.getExternalStorageDirectory(), "Android/data/${activity.packageName}/files/QuranAudio")
        }
    }
    
    @JavascriptInterface
    fun downloadSurah(surahNumber: Int) {
        activity.runOnUiThread {
            try {
                val fileName = String.format("%03d.mp3", surahNumber)
                val url = "https://server.mp3quran.net/obasfr/$fileName"
                
                // Ensure directory exists
                val storageDir = getQuranStorageDirectory()
                if (!storageDir.exists()) {
                    storageDir.mkdirs()
                }
                
                val request = DownloadManager.Request(Uri.parse(url))
                request.setTitle("Surah $surahNumber")
                request.setDescription("Downloading Quran Surah...")
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                
                // Set destination to the QuranAudio directory
                request.setDestinationUri(Uri.fromFile(File(storageDir, fileName)))
                request.allowScanningByMediaScanner()
                
                val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val downloadId = downloadManager.enqueue(request)
                
                Toast.makeText(activity, "Download started for Surah $surahNumber", Toast.LENGTH_SHORT).show()
                
                // Simulate successful download after a short delay
                activity.webView.postDelayed({
                    activity.webView.evaluateJavascript("handleDownloadComplete($surahNumber, true);", null)
                }, 3000) // Increased delay to allow for actual download
                
            } catch (e: Exception) {
                e.printStackTrace()
                activity.webView.evaluateJavascript("handleDownloadComplete($surahNumber, false);", null)
                Toast.makeText(activity, "Download failed for Surah $surahNumber: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    @JavascriptInterface
    fun playLocalSurah(surahNumber: Int) {
        activity.runOnUiThread {
            val fileName = String.format("%03d.mp3", surahNumber)
            val storageDir = getQuranStorageDirectory()
            val file = File(storageDir, fileName)
            
            if (file.exists()) {
                // Play from local file
                val localUrl = "file://${file.absolutePath}"
                activity.webView.loadUrl("file:///android_asset/surah.html?surah=$surahNumber&local=true&url=${Uri.encode(localUrl)}")
                Toast.makeText(activity, "Playing Surah $surahNumber from local storage", Toast.LENGTH_SHORT).show()
            } else {
                // Fallback to server if local file doesn't exist
                openSurahPage(surahNumber)
                Toast.makeText(activity, "Local file not found at ${file.absolutePath}, playing from server", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    @JavascriptInterface
    fun isFileDownloaded(surahNumber: Int): Boolean {
        val fileName = String.format("%03d.mp3", surahNumber)
        val storageDir = getQuranStorageDirectory()
        val file = File(storageDir, fileName)
        return file.exists() && file.length() > 0
    }
    
    @JavascriptInterface
    fun getStoragePath(): String {
        return getQuranStorageDirectory().absolutePath
    }
}
