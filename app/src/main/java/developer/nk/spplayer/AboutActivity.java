package developer.nk.spplayer;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import android.view.KeyEvent;

import android.view.MenuItem;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;


public class AboutActivity extends AppCompatActivity {
    private WebView webView;
    private static final String TAG = "AboutActivity";
    private String url = "https://docs.google.com/document/d/1mFXhB21TgUtbhZpv_ZTj4bVnBrnR-ACwwKZO-9RvDFU/edit?usp=sharing";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setupActionBar();
        webView = findViewById(R.id.webview);
        webView.setWebViewClient(new MyWebViewClient());
        webView.loadUrl(url);
    }
        private class MyWebViewClient extends WebViewClient {
            @TargetApi(Build.VERSION_CODES.O_MR1)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }
            // for old devices
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        }

        private void setupActionBar() {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                // Show the Up button in the action bar.
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    }


