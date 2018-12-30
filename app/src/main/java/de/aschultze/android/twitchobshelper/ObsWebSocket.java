package de.aschultze.android.twitchobshelper;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;

public class ObsWebSocket extends WebSocketClient {

    private static final String TAG = "ObsWebSocket";
    private CallbackUI listener;

    public ObsWebSocket(URI serverUri, CallbackUI listener) {
        super(serverUri);
        this.listener = listener;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.d(TAG, "Connected with message: " + handshakedata.getHttpStatusMessage());
        listener.onObsIsConnected();
    }

    @Override
    public void onMessage(String message) {
        Log.d(TAG, "Message received!");
        try {
            JSONObject json = new JSONObject(message);
            if (json.has("update-type")) {
                switch (json.getString("update-type")) {
                    case "StreamStarted":
                        listener.onObsTriggerStream(json, true);
                        break;
                    case "StreamStopped":
                        listener.onObsTriggerStream(json, false);
                        break;
                    case "RecordingStarted":
                        listener.onObsTriggerRecording(json, true);
                        break;
                    case "RecordingStopped":
                        listener.onObsTriggerRecording(json, false);
                        break;
                }
            }

            if (json.has("message-id")) {
                switch (json.getString("message-id")) {
                    case MyConstants.OBS_STREAM_STATUS:
                        listener.onObsStatusRequested(json);
                        break;
//                    case MyConstants.OBS_TRIGGER_STREAM:
//                        listener.onObsTriggerStream(json);
//                        break;
//                    case MyConstants.OBS_TRIGGER_RECORDING:
//                        listener.onObsTriggerRecording(json);
//                        break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d(TAG, "Closed with reason: " + reason);
        listener.onObsErrorOrClosed(reason);
    }

    @Override
    public void onError(Exception ex) {
        Log.d(TAG, "Error occurred: " + ex.getMessage());
        listener.onObsErrorOrClosed(ex.getMessage());
    }
}
