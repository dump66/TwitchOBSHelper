package de.aschultze.android.twitchobshelper;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class OverviewActivity extends AppCompatActivity {

    private static final String EXTRA_TOKEN = "de.aschultze.android.twitchobshelper.token";
    private static final String TAG = "OverviewActivity";
    private static final String TWITCH_CLIENT_ID = "rveutt431uc0qwzp6ncbp4cm7edqoj";

    private TextView mGame;
    private TextView mViewer;
    private String mToken;
    private boolean mIsOnline = false;

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

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    RequestQueue queue = Volley.newRequestQueue(OverviewActivity.this);
                    RequestFuture<JSONObject> gamesListFuture = RequestFuture.newFuture();
                    JsonObjectRequest gamesListDataRequest = getGamesListRequest(gamesListFuture);
                    RequestFuture<JSONObject> streamFuture = RequestFuture.newFuture();
                    JsonObjectRequest streamDataRequest = getStreamDataRequest(streamFuture);

                    queue.add(gamesListDataRequest);
                    queue.add(streamDataRequest);

                    JSONObject gamesList = gamesListFuture.get(10, TimeUnit.SECONDS);
                    HashMap<String, String> gamesMap = new HashMap<>();
                    for (int i = 0; i < gamesList.getJSONArray("data").length(); i++){
                        String id = gamesList.getJSONArray("data").getJSONObject(i).getString("id");
                        String name = gamesList.getJSONArray("data").getJSONObject(i).getString("name");
                        gamesMap.put(id, name);
                    }

                    JSONObject streamData = streamFuture.get(10, TimeUnit.SECONDS);
                    if (streamData.getJSONArray("data").length() == 0){
                        mIsOnline = false;
                        mViewer.setText("0");
                        mGame.setText("");
                    } else {
                        JSONObject data = streamData.getJSONArray("data").getJSONObject(0);
                        mIsOnline = true;
                        mViewer.setText(""+data.getInt("viewer_count"));
                        mGame.setText(gamesMap.get(data.getString("game_id")));
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private JsonObjectRequest getStreamDataRequest(RequestFuture<JSONObject> future) {
        String url = "https://api.twitch.tv/helix/streams?user_login=dump66";
        JsonObjectRequest streamDataRequest = new JsonObjectRequest(Request.Method.GET, url, null, future, future) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap headers = new HashMap();
                headers.put("Client-ID", TWITCH_CLIENT_ID);
                return headers;
            }
        };
        return streamDataRequest;
    }

    private JsonObjectRequest getGamesListRequest(RequestFuture<JSONObject> future) {
        String url = "https://api.twitch.tv/helix/games/top";
        JsonObjectRequest gamesListRequest = new JsonObjectRequest(Request.Method.GET, url, null, future, future) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap headers = new HashMap();
                headers.put("Client-ID", TWITCH_CLIENT_ID);
                return headers;
            }
        };
        return gamesListRequest;
    }
}
