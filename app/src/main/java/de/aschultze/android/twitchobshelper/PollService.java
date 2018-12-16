package de.aschultze.android.twitchobshelper;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;

public class PollService extends IntentService {
    private static final String TAG = "PollService";

    private static long pollInterval = TimeUnit.SECONDS.toSeconds(15);


    public PollService(){
        super(TAG);
    }
    public static Intent newIntent(Context context){
        return new Intent(context, PollService.class);
    }

    public static void setServiceAlarm(Context context, boolean isOn){
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (isOn){
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), pollInterval, pi);
        } else {
            alarmManager.cancel(pi);
            pi.cancel();
        }

    }

    private void setIntervall(Context context, long seconds){
        this.pollInterval = seconds;
        setServiceAlarm(context, false);
        setServiceAlarm(context, true);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }
}
