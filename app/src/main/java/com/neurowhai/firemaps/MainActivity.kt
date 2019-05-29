package com.neurowhai.firemaps

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.KeyEvent
import android.webkit.*

class MainActivity : AppCompatActivity() {

    private lateinit var mWebView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        val activity = this

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mWebView = findViewById(R.id.activity_main_webview)

        val webSettings = mWebView.settings
        webSettings.javaScriptEnabled = true
        webSettings.setGeolocationEnabled(true)

        if (Build.VERSION.SDK_INT > 16) {
            webSettings.mediaPlaybackRequiresUserGesture = false
        }

        mWebView.webChromeClient = object: WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                super.onGeolocationPermissionsShowPrompt(origin, callback)

                callback?.invoke(origin, true, true)

                if (Build.VERSION.SDK_INT >= 23
                    && ContextCompat.checkSelfPermission(mWebView.context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_DENIED
                ) {
                    // Request a permission for fine location.
                    ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
                }
            }

            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                AlertDialog.Builder(view!!.context)
                    .setTitle("알림")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                    .setCancelable(false)
                    .create()
                    .show()

                return true
            }

            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                AlertDialog.Builder(view!!.context)
                    .setTitle("확인")
                    .setMessage(message)
                    .setPositiveButton("확인") { _, _ -> result?.confirm() }
                    .setNegativeButton("취소") { _, _ -> result?.cancel() }
                    .setCancelable(false)
                    .create()
                    .show()

                return true
            }
        }

        mWebView.loadUrl("https://firemaps.neurowhai.cf")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                if (mWebView.canGoBack()) {
                    mWebView.goBack()
                    return false
                }
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 0
            && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mWebView.reload()
        }
    }
}
