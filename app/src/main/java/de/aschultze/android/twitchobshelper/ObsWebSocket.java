package de.aschultze.android.twitchobshelper;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;

public class ObsWebSocket extends WebSocketClient {

    private static final String TAG = "ObsWebSocket";

    public ObsWebSocket(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.d(TAG, "Connected with message: " + handshakedata.getHttpStatusMessage());
        JSONObject json = new JSONObject();
        try {
            json.put("request-type", "SetHeartbeat");
            json.put("message-id", "myID");
            json.put("enable", true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.send(json.toString());
    }

    @Override
    public void onMessage(String message) {
        Log.d(TAG, "Message received: " + message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d(TAG, "Closed with reason: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        Log.d(TAG, "Error occurred: " + ex.getMessage());
    }
}
