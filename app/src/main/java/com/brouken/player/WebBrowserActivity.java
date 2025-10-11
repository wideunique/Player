package com.brouken.player;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;


public class WebBrowserActivity extends AppCompatActivity {

    public static final String EXTRA_URL = "extra_url";

    private WebView webView;
    private EditText urlInput;
    private int initialZoomPercent = 100;

    private ProgressBar progressBar;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_browser);

        webView = findViewById(R.id.webView);
        urlInput = findViewById(R.id.urlInput);
        progressBar = findViewById(R.id.progressBar);
        Button goButton = findViewById(R.id.goButton);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setVisibility(newProgress == 100 ? View.GONE : View.VISIBLE);
                progressBar.setProgress(newProgress);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (Utils.isVideoUrl(url) || Utils.isAudioUrl(url)) {
                    playInPlayer(url);
                    return true;
                }
                return false; // load in WebView
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (Utils.isVideoUrl(url) || Utils.isAudioUrl(url)) {
                    playInPlayer(url);
                    return true;
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                urlInput.setText(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                getSharedPreferences("web", MODE_PRIVATE).edit().putString("last_url", url).apply();
                // Apply saved initial zoom again after load to ensure it sticks
                final int toApply = initialZoomPercent;
                if (toApply > 0) {
                    webView.post(() -> webView.setInitialScale(toApply));
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                android.widget.Toast.makeText(WebBrowserActivity.this, description, android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        final android.content.SharedPreferences prefs = getSharedPreferences("web", MODE_PRIVATE);
        final String lastUrl = prefs.getString("last_url", null);

        goButton.setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();
            if (!url.isEmpty()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "http://" + url;
                }
                prefs.edit().putString("last_url", url).apply();
                int z = prefs.getInt("zoom:" + zoomKeyForUrl(url), prefs.getInt("last_zoom_scale", 100));
                if (z < 1) z = 1;
                initialZoomPercent = z;
                webView.setInitialScale(initialZoomPercent);
                if (Utils.isVideoUrl(url) || Utils.isAudioUrl(url)) {
                    playInPlayer(url);
                } else {
                    webView.loadUrl(url);
                }
            }
        });

        String extraUrl = getIntent().getStringExtra(EXTRA_URL);
        String initialUrl = null;
        if (extraUrl != null && !extraUrl.isEmpty()) {
            initialUrl = extraUrl;
        } else if (lastUrl != null && !lastUrl.isEmpty()) {
            initialUrl = lastUrl;
        }
        if (initialUrl != null) {
            urlInput.setText(initialUrl);
            int z = prefs.getInt("zoom:" + zoomKeyForUrl(initialUrl), prefs.getInt("last_zoom_scale", 100));
            if (z < 1) z = 1;
            initialZoomPercent = z;
            webView.setInitialScale(initialZoomPercent);
            webView.loadUrl(initialUrl);
        }
    }

    private void playInPlayer(String url) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
        finish();
    }

    private static String zoomKeyForUrl(String url) {
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            String path = uri.getPath();
            if (host == null) return url;
            host = host.toLowerCase(Locale.US);
            if (path == null || path.isEmpty()) path = "/";
            else if (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
            return host + path;
        } catch (Exception e) {
            return url;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            float scale = webView.getScale();
            int percent = Math.round(scale * 100f);
            if (percent < 1) percent = 1; // avoid zero
            final android.content.SharedPreferences prefs = getSharedPreferences("web", MODE_PRIVATE);
            prefs.edit().putInt("last_zoom_scale", percent).apply();
            String current = webView.getUrl();
            if (current != null && !current.isEmpty()) {
                prefs.edit().putInt("zoom:" + zoomKeyForUrl(current), percent).apply();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

