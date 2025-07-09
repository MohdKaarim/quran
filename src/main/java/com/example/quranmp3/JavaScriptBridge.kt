package com.example.quranmp3

import android.webkit.JavascriptInterface

class JavaScriptBridge(private val activity: MainActivity) {

    @JavascriptInterface
    fun openSurahPage(surahNumber: Int) {
        activity.runOnUiThread {
            activity.webView.loadUrl("file:///android_asset/surah.html?surah=$surahNumber")
        }
    }
}
