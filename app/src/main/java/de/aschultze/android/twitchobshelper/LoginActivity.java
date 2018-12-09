package de.aschultze.android.twitchobshelper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.webkit.WebViewClientCompat;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private Context mContext;
    private Activity mActivity;

    private ConstraintLayout mLayout;
    private Button mLogin;

    private PopupWindow mPopupWindow;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mContext = getApplicationContext();
        mActivity = LoginActivity.this;
        mLayout = findViewById(R.id.login_cl);
        mLogin = findViewById(R.id.login_btn_login);

        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
                View popupView = inflater.inflate(R.layout.activity_auth, null);
                mPopupWindow = new PopupWindow(popupView, ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
                mPopupWindow.showAtLocation(mLayout, Gravity.CENTER, 0, 0);
                WebView webView = popupView.findViewById(R.id.auth_webview);
                webView.getSettings().setJavaScriptEnabled(true);
                webView.setWebViewClient(new WebViewClientCompat() {
                    @Override
                    public boolean shouldOverrideUrlLoading(@NonNull WebView view, @NonNull WebResourceRequest request) {
                        if (request.getUrl().toString().startsWith("http://localhost/#access_token=")) {
                            String url = request.getUrl().toString();
                            String token = url.split("&")[0].split("=")[1];
                            mPopupWindow.dismiss();
                            Intent intent = OverviewActivity.newIntent(mContext, token);
                            startActivity(intent);

                        }
                        return false;
                    }
                });
                String state = "15098B9074AB4157F92E09B6BF89A0E9";
                String url = "https://id.twitch.tv/oauth2/authorize?client_id=rveutt431uc0qwzp6ncbp4cm7edqoj&redirect_uri=http://localhost&response_type=token&scope=channel_editor&state=" + state;
                webView.loadUrl(url);
            }
        });
    }
}