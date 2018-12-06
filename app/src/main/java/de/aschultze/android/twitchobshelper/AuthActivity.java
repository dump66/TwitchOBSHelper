package de.aschultze.android.twitchobshelper;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class AuthActivity extends AppCompatActivity {

    private static final String EXTRA_URL_RESPONSE = "de.aschultze.android.twitchobshelper.url_resonse";

    private WebView mWebView;
    private String mUser;
    private String mTwitchState;

    public static Intent newIntent(Context packageContext, String user) {
        Intent intent = new Intent(packageContext, AuthActivity.class);
        intent.putExtra(EXTRA_URL_RESPONSE, user);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        mUser = getIntent().getStringExtra(EXTRA_URL_RESPONSE);

        mWebView = findViewById(R.id.auth_webview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new MyWebViewClient());
        mTwitchState = "15098B9074AB4157F92E09B6BF89A0E9";
        String url = "https://id.twitch.tv/oauth2/authorize?client_id=rveutt431uc0qwzp6ncbp4cm7edqoj&redirect_uri=http://localhost&response_type=token&scope=channel_editor&state="+mTwitchState;
        mWebView.loadUrl(url);
        //TODO: Request working on first try
    }
}

class MyWebViewClient extends WebViewClient {

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        System.out.println(request.getUrl().toString());
        return false;
    }

}
