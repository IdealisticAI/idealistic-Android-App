package ai.idealistic;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private static final String BASE_URL = "https://www.idealistic.ai";
    private static final String ALLOWED_DOMAIN = "idealistic.ai";

    private ValueCallback<Uri[]> fileChooserCallback;
    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (fileChooserCallback != null) {
                    if (uris != null && !uris.isEmpty()) {
                        fileChooserCallback.onReceiveValue(uris.toArray(new Uri[0]));
                    } else {
                        fileChooserCallback.onReceiveValue(null);
                    }
                    fileChooserCallback = null;
                }
            }
    );

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        webView.setBackgroundColor(Color.BLACK);
        progressBar = findViewById(R.id.progressBar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // Configure CookieManager
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSaveFormData(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                CookieManager.getInstance().flush();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(MainActivity.this, "Network Error: " + error.getDescription(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                String host = request.getUrl().getHost();
                if (host != null && host.endsWith(ALLOWED_DOMAIN)) {
                    return false; // Load in app
                }

                // Open external URLs in Chrome Custom Tabs with X close button
                try {
                    CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                    builder.setShowTitle(true);
                    
                    // Set close button icon (white 'X')
                    Bitmap closeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_close);
                    if (closeIcon != null) {
                        builder.setCloseButtonIcon(closeIcon);
                    }
                    
                    CustomTabsIntent intent = builder.build();
                    intent.launchUrl(MainActivity.this, Uri.parse(url));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "External link error", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (fileChooserCallback != null) {
                    fileChooserCallback.onReceiveValue(null);
                }
                fileChooserCallback = filePathCallback;
                try {
                    filePickerLauncher.launch("*/*");
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Picker failed", Toast.LENGTH_SHORT).show();
                    fileChooserCallback.onReceiveValue(null);
                    fileChooserCallback = null;
                    return false;
                }
                return true;
            }
        });

        swipeRefreshLayout.setOnRefreshListener(() -> webView.reload());

        // Fix scroll conflict
        swipeRefreshLayout.setOnChildScrollUpCallback((parent, child) -> webView.getScrollY() > 0);

        // Handle Back button
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        if (savedInstanceState == null) {
            webView.loadUrl(BASE_URL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush();
    }
}
