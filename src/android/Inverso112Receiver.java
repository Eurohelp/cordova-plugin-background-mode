package de.appplant.cordova.plugin.background;

import android.content.Intent;
import android.content.BroadcastReceiver;
import android.util.Log;
import android.content.Context;

public class Inverso112Receiver extends BroadcastReceiver {
  public static final String TAG = "Inverso112Receiver";

  @Override
  public void onReceive(Context context, Intent intent) {
    try {
      Log.d(TAG, "Starting on boot");
      Intent serviceIntent = new Intent(context, Inverso112Service.class);
      context.startService(serviceIntent);
    } catch(Exception ex) {
      Log.d(TAG, "Cannot start app on boot. Exception" + ex.toString());
    }
  }

}
