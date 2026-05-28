package ai.idealistic;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final String BASE_URL = "https://www.idealistic.ai";
    private static final String ALLOWED_DOMAIN = "idealistic.ai";

    private static class NoSuggestionsWebView extends WebView {
        public NoSuggestionsWebView(Context context) {
            super(context);
        }

        @Override
        public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
            InputConnection ic = super.onCreateInputConnection(outAttrs);
            if (ic != null) {
                // Disable keyboard suggestions and autocorrect
                outAttrs.inputType = outAttrs.inputType | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
            }
            return ic;
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new NoSuggestionsWebView(this);

        // Enable cookie persistence
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSaveFormData(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String host = request.getUrl().getHost();
                if (host != null && host.endsWith(ALLOWED_DOMAIN)) {
                    return false; // Allow loading
                }
                return true; // Block other URLs
            }
        });

        setContentView(webView);

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
        // Flush cookies to persistent storage
        CookieManager.getInstance().flush();
    }
}
