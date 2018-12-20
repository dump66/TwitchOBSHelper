package de.aschultze.android.twitchobshelper;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class TestActivity extends AppCompatActivity {

    private TextView updateTV;
    private Button start;
    private long startTime;
    private Handler timerHandler;
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            long seconds = millis / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;

            updateTV.setText(String.format("%d:%02d", minutes, seconds));
            timerHandler.postDelayed(timerRunnable, 500);
        }
    };


    public static Intent newIntent(Context packageContext) {
        Intent intent = new Intent(packageContext, TestActivity.class);
        return intent;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        setContentView(R.layout.activity_test);
        updateTV = findViewById(R.id.textview_test);
        start = findViewById(R.id.button_start);
        startTime = 0L;
        timerHandler = new Handler();

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (start.getText().toString().equalsIgnoreCase("stop")){
                    timerHandler.removeCallbacks(timerRunnable);
                    start.setText("start");
                } else {
                    startTime = System.currentTimeMillis();
                    timerHandler.postDelayed(timerRunnable, 0);
                    start.setText("stop");
                }
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
        start.setText("start");
    }
}
