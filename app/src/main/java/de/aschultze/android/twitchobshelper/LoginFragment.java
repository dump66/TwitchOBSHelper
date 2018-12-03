package de.aschultze.android.twitchobshelper;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";

    private ProgressBar mProgress;
    private EditText mEditUser;
    private EditText mEditToken;
    private Button mLogin;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_login, container, false);

        mProgress = v.findViewById(R.id.login_pg);
        mEditUser = v.findViewById(R.id.login_pt_user);
        mEditToken = v.findViewById(R.id.login_pw_token);
        mLogin = v.findViewById(R.id.login_btn_login);

        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mEditUser.getText().toString().equals("") || mEditToken.getText().toString().equals("")){
                    Toast.makeText(getActivity(), R.string.login_error, Toast.LENGTH_LONG).show();
                } else {
                    // TODO: Twitch Abfrage!
                }
            }
        });
        return v;
    }
}
