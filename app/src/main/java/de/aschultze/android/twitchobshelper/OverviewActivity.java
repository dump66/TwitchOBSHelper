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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;

public class OverviewActivity extends AppCompatActivity {

    // General constants
    private static final String TAG = "OverviewActivity";
    private static final String EXTRA_TOKEN = "de.aschultze.android.twitchobshelper.token";
    private static final String TWITCH_CLIENT_ID = "rveutt431uc0qwzp6ncbp4cm7edqoj";

    // Scheduler
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private int refreshSeconds = 10;

    // TwitchRequester states
    private static final int TWITCH_REQUEST_CHANNEL = 0;
    private static final int TWITCH_REQUEST_STREAM = 1;
    private static final int TWITCH_SEARCH_GAME = 2;
    private static final int TWITCH_UPDATE_GAME = 3;

    // Member variables
    private String mToken;
    private RequestQueue mHttpRequestQueue;
    private String mChannelID;

    // GUI member variables
    private ProgressBar mProgressBar;
    private Button mStreamStatusButton;
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

        // PopUpWindow init
        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.activity_choose_game, null);
        mGameChooserPopup = new PopupWindow(popupView, ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT, true);

        // GUI member variable init
        // Overview
        mProgressBar = findViewById(R.id.overview_progress);
        mStreamStatusButton = findViewById(R.id.overview_stream_status);
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
        new TwitchRequester(futureRequestChannel, TWITCH_REQUEST_CHANNEL).execute();

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
            new TwitchRequester(futureRequestStream, TWITCH_REQUEST_STREAM).execute();
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
            new TwitchRequester(future, TWITCH_UPDATE_GAME).execute();
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
        new TwitchRequester(streamFuture, TWITCH_SEARCH_GAME).execute();
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

    private class TwitchRequester extends AsyncTask<Void, Void, JSONObject> {

        private static final String TAG = "TwitchRequester";

        private RequestFuture<JSONObject> future;
        private int state;

        private TwitchRequester(RequestFuture<JSONObject> future, int state) {
            this.future = future;
            this.state = state;
            if (state == TWITCH_REQUEST_CHANNEL || state == TWITCH_REQUEST_STREAM) {
                mProgressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected JSONObject doInBackground(Void... voids) {
            JSONObject jsonObject = new JSONObject();
            try {
                switch (state) {
                    case TWITCH_REQUEST_CHANNEL:
                        JSONObject channel = future.get(10, TimeUnit.SECONDS);
                        if (channel != null) {
                            jsonObject = channel;
                        } else {
                            Log.d(TAG, "Requesting channel data went wrong!");
                        }
                        break;
                    case TWITCH_REQUEST_STREAM:
                        JSONObject stream = future.get(10, TimeUnit.SECONDS);
                        if (stream != null) {
                            jsonObject = stream;
                        } else {
                            Log.d(TAG, "Requesting stream data went wrong!");
                        }
                        break;
                    case TWITCH_SEARCH_GAME:
                        JSONObject gameSearch = future.get(10, TimeUnit.SECONDS);
                        if (gameSearch != null) {
                            jsonObject = gameSearch;
                        } else {
                            Log.d(TAG, "Searching games went wrong!");
                        }
                        break;
                    case TWITCH_UPDATE_GAME:
                        JSONObject gameUpdate = future.get(10, TimeUnit.SECONDS);
                        if (gameUpdate != null) {
                            jsonObject = gameUpdate;
                        } else {
                            Log.d(TAG, "Updating game title went wrong!");
                        }
                        break;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
            return jsonObject;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);
            switch (state) {
                case TWITCH_REQUEST_CHANNEL:
                    if (jsonObject.has("game")) {
                        try {
                            String gameTitle = (String) jsonObject.getString("game");
                            mGameTitleTV.setText(gameTitle);

                            // Set Channel ID once for future requests
                            if (jsonObject.has("_id")) {
                                if (mChannelID == null || mChannelID.isEmpty()) {
                                    mChannelID = (String) jsonObject.getString("_id");
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
                    break;
                case TWITCH_REQUEST_STREAM:
                    try {
                        // Stream is offline
                        if (jsonObject.isNull("stream")) {
                            setStreamStatus(false);
                            setViewerCount(0);
                        } else {
                            JSONObject stream = jsonObject.getJSONObject("stream");
                            setStreamStatus(true);
                            // Set game title only by channel update
//                            mGameTitleTV.setText(stream.getString("game"));
                            setViewerCount(stream.getInt("viewers"));
                        }
                        Log.d(TAG, "GUI Refresh completed");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case TWITCH_SEARCH_GAME:
                    try {
                        if (jsonObject.getJSONArray("games").length() == 0) {
                            mGameChooserAdapterList.clear();
                            mGameChooserAdapter.notifyDataSetChanged();
                        } else {
                            mGameChooserAdapterList.clear();
                            int gamesCount = jsonObject.getJSONArray("games").length();
                            // Max 5 Games
                            gamesCount = (gamesCount < 5) ? gamesCount : 5;
                            for (int i = 0; i < gamesCount; i++) {
                                JSONObject game = jsonObject.getJSONArray("games").getJSONObject(i);
                                mGameChooserAdapterList.add(game.getString("name"));
                            }
                            mGameChooserAdapter.notifyDataSetChanged();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case TWITCH_UPDATE_GAME:
                    try {
                        mGameChooserPopup.dismiss();
                        refreshGUI();
                        Log.d(TAG, "Game is successfully set to " + jsonObject.getString("game"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.d(TAG, "Game Title could not be updated");
                    }
                    break;
            }
            mProgressBar.setVisibility(View.INVISIBLE);
        }

    }
}
