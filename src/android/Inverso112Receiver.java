package de.appplant.cordova.plugin.background;

import android.content.Intent;
import android.content.BroadcastReceiver;
import android.os.Build;
import android.util.Log;
import android.content.Context;

public class Inverso112Receiver extends BroadcastReceiver {

  public static final String TAG = "Inverso112Receiver";

  public static final String ACTION_ALARM = "de.appplant.cordova.plugin.background.alarms.ACTION_ALARM";

  public static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
  public static final String PACKAGE_REPLACED = "android.intent.action.PACKAGE_REPLACED";
  public static final String PACKAGE_ADDED = "android.intent.action.PACKAGE_ADDED";

  public static final String DATE_CHANGED = "android.intent.action.DATE_CHANGED";
  public static final String TIME_SET = "android.intent.action.TIME_SET";
  public static final String TIMEZONE_CHANGED = "android.intent.action.TIMEZONE_CHANGED";

  private boolean isValidAction(String action) {
    if(action != null && (action.equalsIgnoreCase(ACTION_ALARM) || action.equalsIgnoreCase(BOOT_COMPLETED) || action.equalsIgnoreCase(PACKAGE_REPLACED) || action.equalsIgnoreCase(PACKAGE_ADDED))){
      return true;
    }
    return false;
  }
  private boolean restartAlarm(String action) {
    if(action != null && (action.equalsIgnoreCase(DATE_CHANGED) || action.equalsIgnoreCase(TIME_SET) || action.equalsIgnoreCase(TIMEZONE_CHANGED))){
      return true;
    }
    return false;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    try {
      Log.d(TAG, "ACTION = " + intent.getAction());
      if(isValidAction(intent.getAction())) {
        Intent serviceIntent = new Intent(context, Inverso112Service.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(serviceIntent);
        } else {
          context.startService(serviceIntent);
        }
      } else if(restartAlarm(intent.getAction())) {
        Intent alarmIntent = new Intent(context, Inverso112Receiver.class);
        alarmIntent.setAction(ACTION_ALARM);

        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) context.getSystemService(android.app.Activity.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

        alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (1000 * 60 * 2), pendingIntent);
      }
      
      //Inverso112Service.schedulerJob(context);

      /*AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
      Intent alarmIntent = new Intent(this, Inverso112Receiver.class);
      pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, 0, pendingIntent);
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, 0, pendingIntent);
      } else {
        alarmManager.set(AlarmManager.RTC_WAKEUP, 0, pendingIntent);
      }*/

    } catch(Exception ex) {
      Log.d(TAG, "Cannot start app on boot. Exception" + ex.toString());
    }
  }

}
