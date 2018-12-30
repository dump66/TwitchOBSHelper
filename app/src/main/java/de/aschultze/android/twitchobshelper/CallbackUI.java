package de.aschultze.android.twitchobshelper;

import org.json.JSONException;
import org.json.JSONObject;

public interface CallbackUI {
    void onTwitchChannelRequested(JSONObject json);
    void onTwitchStreamRequested(JSONObject json);
    void onTwitchSearchGame(JSONObject json);
    void onTwitchGameUpdated(JSONObject json);
    void onTwitchProgressFinished();
    void onObsIsConnected();
    void onObsErrorOrClosed(String message);
    void onObsStatusRequested(JSONObject json);
    void onObsTriggerStream(JSONObject json, boolean isOnline) throws JSONException;
    void onObsTriggerRecording(JSONObject json, boolean isOnline);
}
