package de.aschultze.android.twitchobshelper;

import org.json.JSONObject;

public interface CallbackUI {
    void onChannelRequested(JSONObject json);
    void onStreamRequested(JSONObject json);
    void onSearchGame(JSONObject json);
    void onGameUpdated(JSONObject json);
    void onProgressFinished();
}
