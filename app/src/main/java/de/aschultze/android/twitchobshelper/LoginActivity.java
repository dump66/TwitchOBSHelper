package de.aschultze.android.twitchobshelper;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int REQUEST_TOKEN = 0;

    private ProgressBar mProgress;
    private EditText mEditUser;
    private EditText mEditToken;
    private Button mLogin;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate(Bundle) called");
        setContentView(R.layout.activity_login);

        mEditUser = findViewById(R.id.login_pt_user);
        mLogin = findViewById(R.id.login_btn_login);

        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mEditUser.getText().toString().equals("")) {
                    Toast.makeText(LoginActivity.this, R.string.login_error, Toast.LENGTH_LONG).show();
                } else {
                    String url = "https://id.twitch.tv/oauth2/authorize?client_id=rveutt431uc0qwzp6ncbp4cm7edqoj&redirect_uri=http://localhost&response_type=token&scope=channel_editor";
                    RequestQueue queue = Volley.newRequestQueue(LoginActivity.this);
                    StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Intent intent = AuthActivity.newIntent(LoginActivity.this, mEditUser.getText().toString());
                            startActivityForResult(intent, REQUEST_TOKEN);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d(TAG, "Whoops!, Wrong Response");
                        }
                    });
                    queue.add(stringRequest);
                }
            }
        });
    }
}