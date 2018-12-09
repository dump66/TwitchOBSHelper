package de.aschultze.android.twitchobshelper;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class OverviewActivity extends AppCompatActivity {

    private static final String EXTRA_TOKEN = "de.aschultze.android.twitchobshelper.token";
    private static final String TAG = "OverviewActivity";

    private TextView mGame;
    private TextView mViewer;
    private String mToken;

    public static Intent newIntent(Context packageContext, String token) {
        Intent intent = new Intent(packageContext, OverviewActivity.class);
        intent.putExtra(EXTRA_TOKEN, token);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);

        mGame = findViewById(R.id.overview_game);
        mViewer = findViewById(R.id.overview_viewer);
        mToken = getIntent().getStringExtra(EXTRA_TOKEN);

        RequestQueue queue = Volley.newRequestQueue(this);
        String header = "Client-ID: rveutt431uc0qwzp6ncbp4cm7edqoj";
        String url = "https://api.twitch.tv/helix/streams?user_login=dump66";

        JsonObjectRequest jsonObjectRequestRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Ups, da ging was schief");
            }
        })
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap headers = new HashMap();
                headers.put("Client-ID", "rveutt431uc0qwzp6ncbp4cm7edqoj");
                return headers;
            }
        };

        queue.add(jsonObjectRequestRequest);
    }
}
