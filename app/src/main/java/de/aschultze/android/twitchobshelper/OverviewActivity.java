package de.aschultze.android.twitchobshelper;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Filter;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

public class OverviewActivity extends AppCompatActivity {

    private static final String EXTRA_TOKEN = "de.aschultze.android.twitchobshelper.token";
    private static final String TAG = "OverviewActivity";
    private static final String TWITCH_CLIENT_ID = "rveutt431uc0qwzp6ncbp4cm7edqoj";
    private static final int TWITCH_UPDATE = 0;
    private static final int TWITCH_SEARCH = 1;

    private TextView mGame;
    private TextView mViewer;
    private Button mGameBtn;
    private Button mOkButton;
    private String mToken;
    private boolean mIsOnline = false;
    private PopupWindow mPopupWindow;
    private List<String> mGamesList;
    private AutoCompleteTextView mGameChooser;
    private ArrayAdapter<String> mAdapter;
    private RequestQueue mQueue;

    public static Intent newIntent(Context packageContext, String token) {
        Intent intent = new Intent(packageContext, OverviewActivity.class);
        intent.putExtra(EXTRA_TOKEN, token);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);

        // PopUpWindow init
        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.activity_choose_game, null);
        mPopupWindow = new PopupWindow(popupView, ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT, true);

        // Member variable init
        mGame = findViewById(R.id.overview_game);
        mViewer = findViewById(R.id.overview_viewer);
        mGameBtn = findViewById(R.id.overview_btn_game);
        mGameChooser = mPopupWindow.getContentView().findViewById(R.id.game_chooser);
        mGameChooser.setThreshold(1);
        mGamesList = new ArrayList<>();
        mAdapter = new NoFilterArrayAdapter(OverviewActivity.this, android.R.layout.simple_dropdown_item_1line, mGamesList);
        mGameChooser.setAdapter(mAdapter);
        mOkButton = mPopupWindow.getContentView().findViewById(R.id.game_ok);
        mToken = getIntent().getStringExtra(EXTRA_TOKEN);
        mQueue = Volley.newRequestQueue(this);

        updateTwitchGUI();

        initListener();
    }

    private void initListener() {
        mGameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPopupWindow.showAtLocation(findViewById(R.id.overview_cl), Gravity.CENTER, 0, 0);
            }
        });

        mGameChooser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                getGamesList(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
//                getGamesList(editable.toString());
            }
        });

        mOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPopupWindow.dismiss();
            }
        });
    }

    private void updateTwitchGUI() {
        mQueue = Volley.newRequestQueue(OverviewActivity.this);
        RequestFuture<JSONObject> futureRequestStream = RequestFuture.newFuture();
        JsonObjectRequest streamDataRequest = getStreamDataRequest(futureRequestStream);
        mQueue.add(streamDataRequest);
        new TwitchFetchr(futureRequestStream, TWITCH_UPDATE).execute();
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

    private JsonObjectRequest getGamesListRequest(RequestFuture<JSONObject> future, String id) {
        String url = "https://api.twitch.tv/helix/games?id=" + id;
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

    private void getGamesList(final String text) {


        RequestFuture<JSONObject> streamFuture = RequestFuture.newFuture();
        String url = "https://api.twitch.tv/kraken/search/games?query=" + text;
        JsonObjectRequest streamDataRequest = new JsonObjectRequest(Request.Method.GET, url, null, streamFuture, streamFuture) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap headers = new HashMap();
                headers.put("Accept", "application/vnd.twitchtv.v5+json");
                headers.put("Client-ID", TWITCH_CLIENT_ID);
                return headers;
            }
        };

        mQueue.add(streamDataRequest);
        ;
        new TwitchFetchr(streamFuture, TWITCH_SEARCH).execute();


    }

    private class TwitchFetchr extends AsyncTask<Void, Void, List<JSONObject>> {

        private RequestFuture<JSONObject> future;
        private int state;

        private TwitchFetchr(RequestFuture<JSONObject> future, int state) {
            this.future = future;
            this.state = state;
        }

        @Override
        protected List<JSONObject> doInBackground(Void... voids) {
            List<JSONObject> jsonObjects = new ArrayList<>();
            try {
                switch (state) {
                    case TWITCH_UPDATE:
                        JSONObject streamData = future.get(10, TimeUnit.SECONDS);
                        JSONArray dataArray = streamData.getJSONArray("data");
                        if (dataArray.length() > 0) {
                            JSONObject data = dataArray.getJSONObject(0);
                            String gameID = data.getString("game_id");
                            RequestFuture<JSONObject> gamesListFuture = RequestFuture.newFuture();
                            JsonObjectRequest gamesListDataRequest = getGamesListRequest(gamesListFuture, gameID);
                            mQueue.add(gamesListDataRequest);
                            JSONObject game = gamesListFuture.get(10, TimeUnit.SECONDS);
                            jsonObjects.add(streamData);
                            jsonObjects.add(game);
                        }
                        break;
                    case TWITCH_SEARCH:
                        JSONObject gameSearch = future.get(10, TimeUnit.SECONDS);
                        jsonObjects.add(gameSearch);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
            return jsonObjects;
        }

        @Override
        protected void onPostExecute(List<JSONObject> jsonObjects) {
            super.onPostExecute(jsonObjects);
            switch (state) {
                case TWITCH_UPDATE:
                    if (!jsonObjects.isEmpty()) {
                        try {
                            JSONObject stream = jsonObjects.get(0);
                            JSONObject game = jsonObjects.get(1);
                            JSONObject data = stream.getJSONArray("data").getJSONObject(0);
                            mIsOnline = true;
                            mViewer.setText("" + data.getInt("viewer_count"));
                            if (game.getJSONArray("data").length() > 0) {
                                String gameName = game.getJSONArray("data").getJSONObject(0).getString("name");
                                mGame.setText(gameName);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        mIsOnline = false;
                        mViewer.setText("0");
                        mGame.setText("");
                    }
                    break;
                case TWITCH_SEARCH:
                    if (!jsonObjects.isEmpty()) {
                        try {
                            JSONObject gameSearch = jsonObjects.get(0);
                            if (gameSearch.getJSONArray("games").length() == 0) {
                                mGamesList.clear();
                                mAdapter.notifyDataSetChanged();
                            } else {
                                mGamesList.clear();
                                for (int i = 0; i < 5; i++) {
                                    JSONObject game = gameSearch.getJSONArray("games").getJSONObject(i);
                                    mGamesList.add(game.getString("name"));
                                }
                                mAdapter.notifyDataSetChanged();
                                System.out.println();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
            }

        }

    }

    private class NoFilterArrayAdapter extends ArrayAdapter {

        public NoFilterArrayAdapter(@NonNull Context context, int resource, @NonNull List objects) {
            super(context, resource, objects);
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence charSequence) {
                    return new FilterResults();
                }

                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {

                }
            };
        }
    }
}
