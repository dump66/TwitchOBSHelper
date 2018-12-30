package de.aschultze.android.twitchobshelper;

import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.toolbox.RequestFuture;

import org.json.JSONObject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TwitchRequester extends AsyncTask<Void, Void, JSONObject> {

    private static final String TAG = "TwitchRequester";

    private RequestFuture<JSONObject> future;
    private int state;
    private CallbackUI listener;

    public TwitchRequester(RequestFuture<JSONObject> future, int state, CallbackUI listener) {
        this.future = future;
        this.state = state;
        this.listener = listener;

    }

    @Override
    protected JSONObject doInBackground(Void... voids) {
        JSONObject jsonObject = new JSONObject();
        try {
            switch (state) {
                case MyConstants.TWITCH_REQUEST_CHANNEL:
                    JSONObject channel = future.get(10, TimeUnit.SECONDS);
                    if (channel != null) {
                        jsonObject = channel;
                    } else {
                        Log.d(TAG, "Requesting channel data went wrong!");
                    }
                    break;
                case MyConstants.TWITCH_REQUEST_STREAM:
                    JSONObject stream = future.get(10, TimeUnit.SECONDS);
                    if (stream != null) {
                        jsonObject = stream;
                    } else {
                        Log.d(TAG, "Requesting stream data went wrong!");
                    }
                    break;
                case MyConstants.TWITCH_SEARCH_GAME:
                    JSONObject gameSearch = future.get(10, TimeUnit.SECONDS);
                    if (gameSearch != null) {
                        jsonObject = gameSearch;
                    } else {
                        Log.d(TAG, "Searching games went wrong!");
                    }
                    break;
                case MyConstants.TWITCH_UPDATE_GAME:
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
            case MyConstants.TWITCH_REQUEST_CHANNEL:
                listener.onTwitchChannelRequested(jsonObject);
                break;
            case MyConstants.TWITCH_REQUEST_STREAM:
                listener.onTwitchStreamRequested(jsonObject);
                break;
            case MyConstants.TWITCH_SEARCH_GAME:
                listener.onTwitchSearchGame(jsonObject);
                break;
            case MyConstants.TWITCH_UPDATE_GAME:
                listener.onTwitchGameUpdated(jsonObject);
                break;
        }
        listener.onTwitchProgressFinished();
    }
}
