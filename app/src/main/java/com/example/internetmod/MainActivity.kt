package com.example.internetmod

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.example.internetmod.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var websiteToBlock: String = "https://www.tiktok.com/en/"
    private var appPackageToBlock: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val webSettings: WebSettings = binding.webView.settings
        webSettings.javaScriptEnabled = true  // Enable JavaScript (if needed)
        webSettings.domStorageEnabled = true

        val sharedPreferences = getPreferences(MODE_PRIVATE)
        val blockedWebsite = sharedPreferences.getString("blocked_website", "")
        val blockedAppPackage = sharedPreferences.getString("blocked_app", "")

        binding.websiteInput.setText(blockedWebsite)
        binding.appPackageInput.setText(blockedAppPackage)

        binding.saveButton.setOnClickListener {
            websiteToBlock = binding.websiteInput.text.toString()
            appPackageToBlock = binding.appPackageInput.text.toString()

            // Check if the website is accessible
            isWebsiteAccessible(websiteToBlock) { isAccessible ->
                if (isAccessible) {
                    // Save blocked website and app to SharedPreferences
                    val editor = sharedPreferences.edit()
                    editor.putString("blocked_website", websiteToBlock)
                    editor.putString("blocked_app", appPackageToBlock)
                    editor.apply()

                    // Block the specified app
                    blockApp(appPackageToBlock)

                    // Load the website in WebView
                    runOnUiThread {
                        binding.webView.loadUrl(websiteToBlock)
                    }
                } else {
                    // Handle case where the website is not accessible
                    // For example, display an error message
                }
            }
        }

        // WebView for website blocking
        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                if (url.contains(websiteToBlock)) {
                    // Block the website
                    return true
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }

        binding.webView.loadUrl("https://www.tiktok.com/en/")
    }

    private fun isWebsiteAccessible(url: String, callback: (Boolean) -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                val isAccessible = response.isSuccessful
                response.close()
                callback(isAccessible)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    private fun blockApp(packageName: String) {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val deviceAdmin = ComponentName(this, MyAdminReceiver::class.java)

        if (dpm.isAdminActive(deviceAdmin)) {
            val blacklistedPackages = arrayOf(packageName)
            dpm.setPackagesSuspended(deviceAdmin, blacklistedPackages, true)
        }
    }
}
