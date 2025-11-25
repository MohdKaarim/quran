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
                
                // Try multiple server URLs for better success rate
                val serverUrls = listOf(
                    "https://www.everyayah.com/data/Abdul_Basit_Murattal_192kbps/$fileName",
                    "https://download.quranicaudio.com/quran/abdul_basit_murattal/$fileName",
                    "https://server.mp3quran.net/obasfr/$fileName"
                )
                
                // Ensure directory exists
                val storageDir = getQuranStorageDirectory()
                if (!storageDir.exists()) {
                    val created = storageDir.mkdirs()
                    if (!created) {
                        Toast.makeText(activity, "Failed to create storage directory", Toast.LENGTH_LONG).show()
                        activity.webView.evaluateJavascript("handleDownloadComplete($surahNumber, false);", null)
                        return@runOnUiThread
                    }
                }
                
                val destinationFile = File(storageDir, fileName)
                
                // Check if file already exists and is valid (at least 50KB for audio)
                if (destinationFile.exists() && destinationFile.length() > 50000) {
                    Toast.makeText(activity, "Surah $surahNumber already downloaded (${destinationFile.length()} bytes)", Toast.LENGTH_SHORT).show()
                    activity.webView.evaluateJavascript("handleDownloadComplete($surahNumber, true);", null)
                    return@runOnUiThread
                }
                
                // Delete incomplete file if exists
                if (destinationFile.exists()) {
                    destinationFile.delete()
                }
                
                // Try downloading from the primary server
                val url = serverUrls[0] // everyayah.com is more reliable
                
                val request = DownloadManager.Request(Uri.parse(url))
                request.setTitle("Surah $surahNumber - Holy Quran")
                request.setDescription("Downloading from ${Uri.parse(url).host}...")
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationUri(Uri.fromFile(destinationFile))
                request.allowScanningByMediaScanner()
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                request.setAllowedOverRoaming(false)
                
                val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val downloadId = downloadManager.enqueue(request)
                
                Toast.makeText(activity, "Starting download for Surah $surahNumber\nServer: ${Uri.parse(url).host}", Toast.LENGTH_LONG).show()
                
                // Progressive download checking
                var checkCount = 0
                val maxChecks = 30 // Check for up to 1.5 minutes
                
                fun checkDownloadProgress() {
                    checkCount++
                    val currentSize = if (destinationFile.exists()) destinationFile.length() else 0
                    
                    if (currentSize > 50000) {
                        // Download successful - audio files should be at least 50KB
                        activity.webView.evaluateJavascript("handleDownloadComplete($surahNumber, true);", null)
                        Toast.makeText(activity, "Surah $surahNumber downloaded successfully (${currentSize} bytes)", Toast.LENGTH_SHORT).show()
                    } else if (checkCount < maxChecks) {
                        // Continue checking
                        if (checkCount % 5 == 0 && currentSize > 0) {
                            Toast.makeText(activity, "Downloading... ${currentSize} bytes", Toast.LENGTH_SHORT).show()
                        }
                        activity.webView.postDelayed({ checkDownloadProgress() }, 3000)
                    } else {
                        // Download failed or taking too long
                        activity.webView.evaluateJavascript("handleDownloadComplete($surahNumber, false);", null)
                        Toast.makeText(activity, "Download timeout. File size: ${currentSize} bytes. Please try again.", Toast.LENGTH_LONG).show()
                        
                        // Clean up failed download
                        if (destinationFile.exists()) {
                            destinationFile.delete()
                        }
                    }
                }
                
                // Start checking after 3 seconds
                activity.webView.postDelayed({ checkDownloadProgress() }, 3000)
                
            } catch (e: Exception) {
                e.printStackTrace()
                activity.webView.evaluateJavascript("handleDownloadComplete($surahNumber, false);", null)
                Toast.makeText(activity, "Download error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
                
            } catch (e: Exception) {
                e.printStackTrace()
                activity.webView.evaluateJavascript("handleDownloadComplete($surahNumber, false);", null)
                Toast.makeText(activity, "Download failed for Surah $surahNumber: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    @JavascriptInterface
    fun playLocalSurah(surahNumber: Int) {
        activity.runOnUiThread {
            val fileName = String.format("%03d.mp3", surahNumber)
            val storageDir = getQuranStorageDirectory()
            val file = File(storageDir, fileName)
            
            Toast.makeText(activity, "Checking local file: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
            
            if (file.exists() && file.length() > 50000) { // Audio file should be at least 50KB
                try {
                    // Play from local file
                    val localUrl = "file://${file.absolutePath}"
                    activity.webView.loadUrl("file:///android_asset/surah.html?surah=$surahNumber&local=true&url=${Uri.encode(localUrl)}")
                    Toast.makeText(activity, "Playing Surah $surahNumber from local storage", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(activity, "Error playing local file: ${e.message}", Toast.LENGTH_LONG).show()
                    // Fallback to server
                    openSurahPage(surahNumber)
                }
            } else {
                // Fallback to server if local file doesn't exist or is empty
                if (!file.exists()) {
                    Toast.makeText(activity, "Local file not found: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(activity, "Local file is empty or corrupted (${file.length()} bytes)", Toast.LENGTH_LONG).show()
                }
                openSurahPage(surahNumber)
            }
        }
    }
    
    @JavascriptInterface
    fun isFileDownloaded(surahNumber: Int): Boolean {
        val fileName = String.format("%03d.mp3", surahNumber)
        val storageDir = getQuranStorageDirectory()
        val file = File(storageDir, fileName)
        val exists = file.exists() && file.length() > 50000 // Audio files should be at least 50KB
        
        if (exists) {
            activity.runOnUiThread {
                // Only show this in debug mode
                // Toast.makeText(activity, "File found: ${file.absolutePath} (${file.length()} bytes)", Toast.LENGTH_SHORT).show()
            }
        }
        
        return exists
    }
    
    @JavascriptInterface
    fun getStoragePath(): String {
        return getQuranStorageDirectory().absolutePath
    }
}
