package de.appplant.cordova.plugin.background;

import android.content.Intent;
import android.content.BroadcastReceiver;
import android.os.Build;
import android.util.Log;
import android.content.Context;

public class Inverso112Receiver extends BroadcastReceiver {
  public static final String TAG = "Inverso112Receiver";

  @Override
  public void onReceive(Context context, Intent intent) {
    try {
      Log.d(TAG, "Starting on boot");
      /*Intent serviceIntent = new Intent(context, Inverso112Service.class);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(serviceIntent);
      } else {
        context.startService(serviceIntent);
      }*/
      Inverso112Service.schedulerJob(context);
    } catch(Exception ex) {
      Log.d(TAG, "Cannot start app on boot. Exception" + ex.toString());
    }
  }

}
