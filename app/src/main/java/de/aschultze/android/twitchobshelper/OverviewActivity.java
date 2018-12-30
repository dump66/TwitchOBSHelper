package de.aschultze.android.twitchobshelper;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

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
    private static final String EXTRA_IP = "de.aschultze.android.twitchobshelper.ip";
    private static final String TWITCH_CLIENT_ID = "rveutt431uc0qwzp6ncbp4cm7edqoj";

    // OBS
    private ObsWebSocket mWebSocket;
    private boolean mIsStreaming = false;
    private boolean mIsRecording = false;

    // Scheduler
    private Handler mRefreshHandler;
    private Runnable mRefreshRunnable;
    private int mRefreshSeconds = 10;

    // Member variables
    private String mIP;
    private String mToken;
    private URI mObsURI;
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
    private TextView mObsStatus;


    public static Intent newIntent(Context packageContext, String token, String ip) {
        Intent intent = new Intent(packageContext, OverviewActivity.class);
        intent.putExtra(EXTRA_TOKEN, token);
        intent.putExtra(EXTRA_IP, ip);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mToken = getIntent().getStringExtra(EXTRA_TOKEN);
        mIP = getIntent().getStringExtra(EXTRA_IP);
        mHttpRequestQueue = Volley.newRequestQueue(this);

        // Scheduler init
        mRefreshHandler = new Handler();
        mRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshGUI();
                long millis = mRefreshSeconds * 1000;
                mRefreshHandler.postDelayed(mRefreshRunnable, millis);
            }
        };

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
        mObsStatus = findViewById(R.id.overview_tv_obs);

        // PopupWindow
        mGameChooserACTV = mGameChooserPopup.getContentView().findViewById(R.id.chooser_actv);
        mGameChooserACTV.setThreshold(1);
        mGameChooserAdapterList = new ArrayList<>();
        mGameChooserAdapter = new NoFilterArrayAdapter(OverviewActivity.this, android.R.layout.simple_dropdown_item_1line, mGameChooserAdapterList);
        mGameChooserACTV.setAdapter(mGameChooserAdapter);
        mGameChooserOkButton = mGameChooserPopup.getContentView().findViewById(R.id.chooser_ok);
        mGameChooserCancelButton = mGameChooserPopup.getContentView().findViewById(R.id.chooser_cancel);

        // Websocket init
        try {
            mObsURI = new URI(String.format("ws://%s:4444", mIP));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        mWebSocket = new ObsWebSocket(mObsURI, this);
        mWebSocket.connect();

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

        mStreamStatusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mProgressBar.setVisibility(View.VISIBLE);
                JSONObject json = new JSONObject();
                try {
                    json.put("request-type", "StartStopStreaming");
                    json.put("message-id", MyConstants.OBS_TRIGGER_STREAM);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                sendWebSocketRequest(json);
            }
        });

        mRecordStatusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mProgressBar.setVisibility(View.VISIBLE);
                JSONObject json = new JSONObject();
                try {
                    json.put("request-type", "StartStopRecording");
                    json.put("message-id", MyConstants.OBS_TRIGGER_RECORDING);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                sendWebSocketRequest(json);
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
                    mRefreshHandler.postDelayed(mRefreshRunnable, 0);
                } else {
                    mRefreshHandler.removeCallbacks(mRefreshRunnable);
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

        // Get OBS status
        JSONObject json = new JSONObject();
        try {
            json.put("request-type", "GetStreamingStatus");
            json.put("message-id", MyConstants.OBS_STREAM_STATUS);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendWebSocketRequest(json);
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
                public Map<String, String> getHeaders() {
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

    private void sendWebSocketRequest(JSONObject json) {
        if (mWebSocket.isOpen()) {
            mWebSocket.send(json.toString());
        } else {
            Log.d(TAG, "Websocket is closed. Creating new and retry to connect...");
            mWebSocket.close();
            mWebSocket = new ObsWebSocket(mObsURI, this);
            mWebSocket.connect();
        }
    }

    private void setStreamStatus(boolean isStreaming) {
        if (isStreaming) {
            mIsStreaming = true;
            mStreamStatusButton.setText(R.string.overview_stream_online);
            mStreamStatusButton.setBackgroundResource(R.color.streamOnline);
        } else {
            mIsStreaming = false;
            mStreamStatusButton.setText(R.string.overview_stream_offline);
            mStreamStatusButton.setBackgroundResource(R.color.streamOffline);
        }
    }

    private void setRecordStatus(boolean isRecording) {
        if (isRecording) {
            mIsRecording = true;
            mRecordStatusButton.setText(R.string.overview_record_online);
            mRecordStatusButton.setBackgroundResource(R.color.streamOnline);
        } else {
            mIsRecording = false;
            mRecordStatusButton.setText(R.string.overview_record_offline);
            mRecordStatusButton.setBackgroundResource(R.color.streamOffline);
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
    public void onTwitchChannelRequested(JSONObject json) {
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
    public void onTwitchStreamRequested(JSONObject json) {
        try {
            // Stream is offline
            if (json.isNull("stream")) {
//                Only Obs sets stream status
//                setStreamStatus(false);

                setViewerCount(0);
            } else {
                JSONObject stream = json.getJSONObject("stream");

//                Only Obs sets stream status
//                setStreamStatus(true);

//                Set game title only by channel update
//                mGameTitleTV.setText(stream.getString("game"));

                setViewerCount(stream.getInt("viewers"));
            }
            Log.d(TAG, "GUI Refresh completed");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTwitchSearchGame(JSONObject json) {
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
    public void onTwitchGameUpdated(JSONObject json) {
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
    public void onTwitchProgressFinished() {
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onObsIsConnected() {
        mObsStatus.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onObsErrorOrClosed(String message) {
        mObsStatus.setVisibility(View.VISIBLE);
    }

    @Override
    public void onObsStatusRequested(final JSONObject json) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    String status = json.getString("status");
                    boolean isStreaming = json.getBoolean("streaming");
                    boolean isRecording = json.getBoolean("recording");
                    if (status.equals("ok")) {
                        setStreamStatus(isStreaming);
                        setRecordStatus(isRecording);
                    } else {
                        Log.d(TAG, "Stream Trigger failed: " + json.getString("error"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        });

    }

    @Override
    public void onObsTriggerStream(final JSONObject json, final boolean isOnline) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setStreamStatus(isOnline);
                mProgressBar.setVisibility(View.INVISIBLE);
            }
        });

    }

    @Override
    public void onObsTriggerRecording(final JSONObject json, final boolean isOnline) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setRecordStatus(isOnline);
                mProgressBar.setVisibility(View.INVISIBLE);
            }
        });
    }


}
