package de.appplant.cordova.plugin.background;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class Inverso112Service extends Service {

    private static final String TAG = "Inverso112Service";

    public static final int interval = 300000;  //interval between two services(Here Service run every 5 Minute)

    private Handler handler = new Handler();   //run on another Thread to avoid crash
    private Timer timer = null;    //timer handling


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        if (timer != null) { // Cancel if already existed
            timer.cancel();
        } else {
            timer = new Timer();   //recreate new
        }
        timer.scheduleAtFixedRate(new TimeDisplay(), 0, interval);   //Schedule task
        Log.d(TAG, "onCreate");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        timer.cancel();
        Log.d(TAG, "onDestroy");
    }

    class TimeDisplay extends TimerTask {

        @Override
        public void run() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "TimeDisplay");
                }
            });
        }

    }

}
