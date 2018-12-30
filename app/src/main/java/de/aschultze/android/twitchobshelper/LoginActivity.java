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
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.webkit.WebViewClientCompat;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private Activity mActivity;

    private ConstraintLayout mLayout;
    private EditText mIP;
    private Button mLogin;
    private ProgressBar mProgress;

    private PopupWindow mPopupWindow;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mActivity = LoginActivity.this;
        mLayout = findViewById(R.id.login_cl);
        mIP = findViewById(R.id.login_et_ip);
        mLogin = findViewById(R.id.login_btn_login);
        mProgress = findViewById(R.id.login_progress);


        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mProgress.setVisibility(View.VISIBLE);
                LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
                View popupView = inflater.inflate(R.layout.activity_auth, null);
                mPopupWindow = new PopupWindow(popupView, ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT, true);
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
                            Intent intent = OverviewActivity.newIntent(mActivity, token, mIP.getText().toString());
                            startActivity(intent);
                            mProgress.setVisibility(View.INVISIBLE);
                        }
                        return false;
                    }
                });
                String state = "15098B9074AB4157F92E09B6BF89A0E9";
                String url = "https://id.twitch.tv/oauth2/authorize?client_id=rveutt431uc0qwzp6ncbp4cm7edqoj&redirect_uri=http://localhost&response_type=token&scope=channel_editor channel_read&state=" + state;
                webView.loadUrl(url);
            }
        });
    }
}