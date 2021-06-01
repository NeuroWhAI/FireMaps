package com.neurowhai.firemaps

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.webkit.*


class MainActivity : AppCompatActivity() {

    private val LOCATION_PERMISSION_CODE = 0
    private val STORAGE_PERMISSION_CODE = 1

    private val FILE_SELECTED_CODE = 2001

    private lateinit var mWebView: WebView
    private var onFileSelected: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val activity = this

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        WebView.setWebContentsDebuggingEnabled(true)
        mWebView = findViewById(R.id.activity_main_webview)

        val webSettings = mWebView.settings
        webSettings.javaScriptEnabled = true
        webSettings.setGeolocationEnabled(true)

        if (Build.VERSION.SDK_INT > 16) {
            webSettings.mediaPlaybackRequiresUserGesture = false
        }

        mWebView.webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                if (error == null) {
                    return super.onReceivedSslError(view, handler, error)
                }

                val cert = error.certificate
                if (cert.issuedTo.cName == "cctvsec.ktict.co.kr") {
                    handler?.proceed()
                }
            }
        }

        mWebView.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                super.onGeolocationPermissionsShowPrompt(origin, callback)

                callback?.invoke(origin, true, true)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && ContextCompat.checkSelfPermission(
                        mWebView.context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    == PackageManager.PERMISSION_DENIED
                ) {
                    // Show permission information.
                    AlertDialog.Builder(mWebView.context)
                        .setTitle("권한 설명")
                        .setMessage("""
지도에 사용자를 표시하기 위해서 위치 권한이 필요합니다.
취소하시면 설정에서 수동 허용이 필요합니다.
""".trimMargin())
                        .setPositiveButton(android.R.string.yes) { _, _ ->
                            // Request a permission for fine location.
                            val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                            ActivityCompat.requestPermissions(
                                activity,
                                permissions,
                                LOCATION_PERMISSION_CODE
                            )
                        }
                        .setNegativeButton(android.R.string.no) { _, _ ->
                            // Do nothing.
                        }
                        .setCancelable(false)
                        .show()
                }
            }

            override fun onJsAlert(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                AlertDialog.Builder(view!!.context)
                    .setTitle("알림")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                    .setCancelable(false)
                    .create()
                    .show()

                return true
            }

            override fun onJsConfirm(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                AlertDialog.Builder(view!!.context)
                    .setTitle("확인")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.yes) { _, _ -> result?.confirm() }
                    .setNegativeButton(android.R.string.no) { _, _ -> result?.cancel() }
                    .setCancelable(false)
                    .create()
                    .show()

                return true
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                onFileSelected = filePathCallback

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && ContextCompat.checkSelfPermission(
                        mWebView.context,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                    == PackageManager.PERMISSION_DENIED
                ) {
                    // Request a permission for reading file.
                    val permissions = arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    ActivityCompat.requestPermissions(
                        activity,
                        permissions,
                        STORAGE_PERMISSION_CODE
                    )
                } else {
                    selectFile()
                }

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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                LOCATION_PERMISSION_CODE -> mWebView.reload()
                STORAGE_PERMISSION_CODE -> selectFile()
            }
        } else {
            when (requestCode) {
                STORAGE_PERMISSION_CODE -> {
                    onFileSelected?.onReceiveValue(null)
                    onFileSelected = null
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_SELECTED_CODE) {
            if (resultCode == RESULT_OK) {
                onFileSelected?.onReceiveValue(
                    WebChromeClient.FileChooserParams.parseResult(
                        resultCode,
                        data
                    )
                )
            } else {
                onFileSelected?.onReceiveValue(null)
                onFileSelected = null
            }
        }
    }

    fun selectFile() {
        if (onFileSelected == null) {
            return
        }

        val pickIntent = Intent(Intent.ACTION_PICK)
        pickIntent.setDataAndType(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Images.Media.CONTENT_TYPE
        )

        val pickTitle = "사진을 가져옵니다."
        val chooserIntent = Intent.createChooser(pickIntent, pickTitle)

        startActivityForResult(chooserIntent, FILE_SELECTED_CODE)
    }
}
