package de.aschultze.android.twitchobshelper;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;

public class OverviewActivity extends AppCompatActivity implements CallbackUI {

    // General constants
    private static final String TAG = "OverviewActivity";
    private static final String EXTRA_TOKEN = "de.aschultze.android.twitchobshelper.token";
    private static final String TWITCH_CLIENT_ID = "rveutt431uc0qwzp6ncbp4cm7edqoj";

    // Others
    private ObsWebSocket webSocket;

    // Scheduler
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private int refreshSeconds = 10;

    // Member variables
    private String mToken;
    private RequestQueue mHttpRequestQueue;
    private String mChannelID;

    // GUI member variables
    private ProgressBar mProgressBar;
    private Button mStreamStatusButton;
    private Button mRecordStatusButton;
    private Button mGameButton;
    private TextView mGameTitleTV;
    private TextView mViewerCountTV;
    private SwitchCompat mRefreshSwitch;
    private PopupWindow mGameChooserPopup;
    private Button mGameChooserOkButton;
    private Button mGameChooserCancelButton;
    private AutoCompleteTextView mGameChooserACTV;
    private ArrayAdapter<String> mGameChooserAdapter;
    private List<String> mGameChooserAdapterList;


    public static Intent newIntent(Context packageContext, String token) {
        Intent intent = new Intent(packageContext, OverviewActivity.class);
        intent.putExtra(EXTRA_TOKEN, token);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);
        mToken = getIntent().getStringExtra(EXTRA_TOKEN);
        mHttpRequestQueue = Volley.newRequestQueue(this);

        // Scheduler init
        refreshHandler = new Handler();
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshGUI();
                long millis = refreshSeconds * 1000;
                refreshHandler.postDelayed(refreshRunnable, millis);
            }
        };

        // Websocket init
        URI obsURI = null;
        try {
            obsURI = new URI("ws://192.168.178.46:4444");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        webSocket = new ObsWebSocket(obsURI);
        // webSocket.connect();

        // PopUpWindow init
        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.activity_choose_game, null);
        mGameChooserPopup = new PopupWindow(popupView, ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT, true);

        // GUI member variable init
        // Overview
        mProgressBar = findViewById(R.id.overview_progress);
        mStreamStatusButton = findViewById(R.id.overview_stream_status);
        mRecordStatusButton = findViewById(R.id.overview_record_status);
        mGameButton = findViewById(R.id.overview_btn_game);
        mGameTitleTV = findViewById(R.id.overview_game);
        mViewerCountTV = findViewById(R.id.overview_viewer);
        mRefreshSwitch = findViewById(R.id.overview_refresh);

        // PopupWindow
        mGameChooserACTV = mGameChooserPopup.getContentView().findViewById(R.id.chooser_actv);
        mGameChooserACTV.setThreshold(1);
        mGameChooserAdapterList = new ArrayList<>();
        mGameChooserAdapter = new NoFilterArrayAdapter(OverviewActivity.this, android.R.layout.simple_dropdown_item_1line, mGameChooserAdapterList);
        mGameChooserACTV.setAdapter(mGameChooserAdapter);
        mGameChooserOkButton = mGameChooserPopup.getContentView().findViewById(R.id.chooser_ok);
        mGameChooserCancelButton = mGameChooserPopup.getContentView().findViewById(R.id.chooser_cancel);

        initListener();
        mRefreshSwitch.setChecked(true);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mRefreshSwitch.setChecked(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRefreshSwitch.setChecked(true);
    }

    private void initListener() {
        mGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGameChooserACTV.setText("");
                mGameChooserPopup.showAtLocation(findViewById(R.id.overview_cl), Gravity.CENTER, 0, 0);
            }
        });

        mGameChooserACTV.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!editable.toString().isEmpty()) {
                    searchGameTitles(editable.toString());
                }
            }
        });

        mRefreshSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    Log.d(TAG, "Starting Refresh Handler");
                    refreshHandler.postDelayed(refreshRunnable, 0);
                } else {
                    refreshHandler.removeCallbacks(refreshRunnable);
                    Log.d(TAG, "Stopped Refresh Handler");
                }
            }
        });

        mGameChooserOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateChannelGame();
            }
        });
        mGameChooserCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGameChooserPopup.dismiss();
            }
        });
    }

    private void refreshGUI() {
        Log.d(TAG, "Refreshing GUI...");
        // Get Game
        RequestFuture<JSONObject> futureRequestChannel = RequestFuture.newFuture();
        String channelURL = "https://api.twitch.tv/kraken/channel";
        JsonObjectRequest channelRequest = new JsonObjectRequest(Request.Method.GET, channelURL, null, futureRequestChannel, futureRequestChannel) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap headers = new HashMap();
                headers.put("Accept", "application/vnd.twitchtv.v5+json");
                headers.put("Client-ID", TWITCH_CLIENT_ID);
                headers.put("Authorization", "OAuth " + mToken);
                return headers;
            }
        };
        mHttpRequestQueue.add(channelRequest);
        startAsyncTask(futureRequestChannel, MyConstants.TWITCH_REQUEST_CHANNEL);

        // Get Viewers
        if (mChannelID != null && !mChannelID.isEmpty()) {
            RequestFuture<JSONObject> futureRequestStream = RequestFuture.newFuture();
            String streamURL = "https://api.twitch.tv/kraken/streams/" + mChannelID;
            JsonObjectRequest streamRequest = new JsonObjectRequest(Request.Method.GET, streamURL, null, futureRequestStream, futureRequestStream) {
                @Override
                public Map<String, String> getHeaders() {
                    HashMap headers = new HashMap();
                    headers.put("Accept", "application/vnd.twitchtv.v5+json");
                    headers.put("Client-ID", TWITCH_CLIENT_ID);
                    headers.put("Authorization", "OAuth " + mToken);
                    return headers;
                }
            };
            mHttpRequestQueue.add(streamRequest);
            startAsyncTask(futureRequestStream, MyConstants.TWITCH_REQUEST_STREAM);
        }
    }

    private void updateChannelGame() {
        try {
            RequestFuture<JSONObject> future = RequestFuture.newFuture();
            String url = "https://api.twitch.tv/kraken/channels/" + mChannelID;
            JSONObject channel = new JSONObject();
            channel.put("game", mGameChooserACTV.getText().toString());
            JSONObject request = new JSONObject();
            request.put("channel", channel);
            JsonObjectRequest gameUpdateRequest = new JsonObjectRequest(Request.Method.PUT, url, request, future, future) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap headers = new HashMap();
                    headers.put("Accept", "application/vnd.twitchtv.v5+json");
                    headers.put("Client-ID", TWITCH_CLIENT_ID);
                    headers.put("Authorization", "OAuth " + mToken);
                    return headers;
                }
            };
            mHttpRequestQueue.add(gameUpdateRequest);
            startAsyncTask(future, MyConstants.TWITCH_UPDATE_GAME);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void searchGameTitles(final String text) {
        RequestFuture<JSONObject> streamFuture = RequestFuture.newFuture();
        String url = "https://api.twitch.tv/kraken/search/games?query=" + text;
        JsonObjectRequest streamDataRequest = new JsonObjectRequest(Request.Method.GET, url, null, streamFuture, streamFuture) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap headers = new HashMap();
                headers.put("Accept", "application/vnd.twitchtv.v5+json");
                headers.put("Client-ID", TWITCH_CLIENT_ID);
                headers.put("Authorization", "OAuth " + mToken);
                return headers;
            }
        };

        mHttpRequestQueue.add(streamDataRequest);
        startAsyncTask(streamFuture, MyConstants.TWITCH_SEARCH_GAME);
    }

    private void setStreamStatus(boolean isOnline) {
        if (isOnline) {
            mStreamStatusButton.setText(R.string.overview_stream_online);
            mStreamStatusButton.setBackgroundResource(R.color.streamOnline);
        } else {
            mStreamStatusButton.setText(R.string.overview_stream_offline);
            mStreamStatusButton.setBackgroundResource(R.color.streamOffline);
        }
    }

    private void setViewerCount(int viewers) {
        mViewerCountTV.setText(Integer.toString(viewers));
        if (viewers > 0) {
            mViewerCountTV.setTextColor(getResources().getColor(R.color.colorPrimary));
        } else {
            mViewerCountTV.setTextColor(Color.BLACK);
        }
    }

    private void startAsyncTask(RequestFuture<JSONObject> future, int state) {
        if (state == MyConstants.TWITCH_REQUEST_CHANNEL || state == MyConstants.TWITCH_REQUEST_STREAM) {
            mProgressBar.setVisibility(View.VISIBLE);
        }
        new TwitchRequester(future, state, this).execute();
    }

    @Override
    public void onChannelRequested(JSONObject json) {
        if (json.has("game")) {
            try {
                String gameTitle = json.getString("game");
                mGameTitleTV.setText(gameTitle);

                // Set Channel ID once for future requests
                if (json.has("_id")) {
                    if (mChannelID == null || mChannelID.isEmpty()) {
                        mChannelID = (String) json.getString("_id");
                        // start again for stream status and viewer update
                        refreshGUI();
                    }
                } else {
                    if (mChannelID == null || mChannelID.isEmpty()) {
                        Log.d(TAG, "Channel ID was not set!");
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "Channel Request went wrong!");
        }
    }

    @Override
    public void onStreamRequested(JSONObject json) {
        try {
            // Stream is offline
            if (json.isNull("stream")) {
                setStreamStatus(false);
                setViewerCount(0);
            } else {
                JSONObject stream = json.getJSONObject("stream");
                setStreamStatus(true);
                // Set game title only by channel update
                // mGameTitleTV.setText(stream.getString("game"));
                setViewerCount(stream.getInt("viewers"));
            }
            Log.d(TAG, "GUI Refresh completed");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSearchGame(JSONObject json) {
        try {
            if (json.getJSONArray("games").length() == 0) {
                mGameChooserAdapterList.clear();
                mGameChooserAdapter.notifyDataSetChanged();
            } else {
                mGameChooserAdapterList.clear();
                int gamesCount = json.getJSONArray("games").length();
                // Max 5 Games
                gamesCount = (gamesCount < 5) ? gamesCount : 5;
                for (int i = 0; i < gamesCount; i++) {
                    JSONObject game = json.getJSONArray("games").getJSONObject(i);
                    mGameChooserAdapterList.add(game.getString("name"));
                }
                mGameChooserAdapter.notifyDataSetChanged();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGameUpdated(JSONObject json) {
        try {
            mGameChooserPopup.dismiss();
            refreshGUI();
            Log.d(TAG, "Game is successfully set to " + json.getString("game"));
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG, "Game Title could not be updated");
        }
    }

    @Override
    public void onProgressFinished() {
        mProgressBar.setVisibility(View.INVISIBLE);
    }


}
