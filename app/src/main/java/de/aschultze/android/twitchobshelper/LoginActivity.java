package de.aschultze.android.twitchobshelper;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private ProgressBar mProgress;
    private EditText mEditUser;
    private EditText mEditToken;
    private Button mLogin;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate(Bundle) called");
        setContentView(R.layout.activity_login);

        mProgress = findViewById(R.id.login_pg);
        mEditUser = findViewById(R.id.login_pt_user);
        mEditToken = findViewById(R.id.login_pw_token);
        mLogin = findViewById(R.id.login_btn_login);

        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mEditUser.getText().toString().equals("") || mEditToken.getText().toString().equals("")) {
                    Toast.makeText(LoginActivity.this, R.string.login_error, Toast.LENGTH_LONG).show();
                } else {
                    // TODO: Twitch Abfrage!
                }
            }
        });
    }
}