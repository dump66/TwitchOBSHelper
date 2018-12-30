package de.aschultze.android.twitchobshelper;

public class MyConstants {

    // TwitchRequester states
    public static final int TWITCH_REQUEST_CHANNEL = 0;
    public static final int TWITCH_REQUEST_STREAM = 1;
    public static final int TWITCH_SEARCH_GAME = 2;
    public static final int TWITCH_UPDATE_GAME = 3;

    // OBS Message IDs
    public static final String OBS_STREAM_STATUS = "StreamStatus";
    public static final String OBS_TRIGGER_STREAM = "TriggerStream";
    public static final String OBS_TRIGGER_RECORDING = "TriggerRecording";
}
