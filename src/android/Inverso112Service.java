package de.appplant.cordova.plugin.background;

import android.annotation.TargetApi;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import android.content.Context;

import android.Manifest;

import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONObject;

public class Inverso112Service extends Service {

  private static final String TAG = "Inverso112Service";


  // Fixed ID for the 'foreground' notification
  public static final int NOTIFICATION_ID = -574595411;

  // Default title of the background notification
  private static final String NOTIFICATION_TITLE =
    "112 Inverso is running in background";

  // Default text of the background notification
  private static final String NOTIFICATION_TEXT =
    "Doing heavy tasks.";

  // Default icon of the background notification
  private static final String NOTIFICATION_ICON = "icon";

  // Binder given to clients
  private final IBinder binder = new Inverso112Service.Inverso112Binder();

  /**
   * Allow clients to call on to the service.
   */
  @Override
  public IBinder onBind (Intent intent) {
    return binder;
  }

  /**
   * Class used for the client Binder.  Because we know this service always
   * runs in the same process as its clients, we don't need to deal with IPC.
   */
  class Inverso112Binder extends Binder
  {
    Inverso112Service getService()
    {
      return Inverso112Service.this;
    }
  }

  /**
   * Put the service in a foreground state to prevent app from being killed
   * by the OS.
   */
  @Override
  public void onCreate()
  {
    super.onCreate();
    startForeground(NOTIFICATION_ID, makeNotification());

    setAlarm();
    if(hasPermission()) {
      Log.d(TAG, "hasPermission() = true");
      getLocation();
    } else {
      Log.d(TAG, "hasPermission() = false");
      stopSelf();
    }

  }


  /**
   * No need to run headless on destroy.
   */
  @Override
  public void onDestroy()
  {
    super.onDestroy();
    Log.d(TAG, "onDestroy");
    sleepWell();
  }


  /**
   * Stop background mode.
   */
  private void sleepWell()
  {
    stopForeground(true);
    getNotificationManager().cancel(NOTIFICATION_ID);
  }

  private void setAlarm(){
    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    Intent alarmIntent = new Intent(this, Inverso112Receiver.class);
    alarmIntent.setAction(Inverso112Receiver.ACTION_ALARM);
    PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0);
    alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (1000 * 60 * 30), pendingIntent);
  }

     /* Create a notification as the visible part to be able to put the service
     * in a foreground state by using the default settings.
     */
  private Notification makeNotification()
  {
    return makeNotification(BackgroundMode.getSettings());
  }

  /**
   * Create a notification as the visible part to be able to put the service
   * in a foreground state.
   *
   * @param settings The config settings
   */
  private Notification makeNotification (JSONObject settings)
  {
    // use channelid for Oreo and higher
    String CHANNEL_ID = "cordova-plugin-background-mode-112-inverso";
    if(Build.VERSION.SDK_INT >= 26){
      // The user-visible name of the channel.
      CharSequence name = "cordova-plugin-background-mode-112-inverso";
      // The user-visible description of the channel.
      String description = "cordova-plugin-background-moden-112-inverso notification";

      int importance = NotificationManager.IMPORTANCE_LOW;

      NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);

      // Configure the notification channel.
      mChannel.setDescription(description);

      getNotificationManager().createNotificationChannel(mChannel);
    }
    String title    = settings.optString("title", NOTIFICATION_TITLE);
    String text     = settings.optString("text", NOTIFICATION_TEXT);
    boolean bigText = settings.optBoolean("bigText", false);

    Context context = getApplicationContext();
    String pkgName  = context.getPackageName();
    Intent intent   = context.getPackageManager()
      .getLaunchIntentForPackage(pkgName);

    Notification.Builder notification = new Notification.Builder(context)
      .setContentTitle(title)
      .setContentText(text)
      .setOngoing(true)
      .setSmallIcon(getIconResId(settings));

    if(Build.VERSION.SDK_INT >= 26){
      notification.setChannelId(CHANNEL_ID);
    }

    if (settings.optBoolean("hidden", true)) {
      notification.setPriority(Notification.PRIORITY_MIN);
    }

    if (bigText || text.contains("\n")) {
      notification.setStyle(
        new Notification.BigTextStyle().bigText(text));
    }

    setColor(notification, settings);

    if (intent != null && settings.optBoolean("resume")) {
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
      PendingIntent contentIntent = PendingIntent.getActivity(
        context, NOTIFICATION_ID, intent,
        PendingIntent.FLAG_UPDATE_CURRENT);


      notification.setContentIntent(contentIntent);
    }

    return notification.build();
  }

  /**
   * Retrieves the resource ID of the app icon.
   *
   * @param settings A JSON dict containing the icon name.
   */
  private int getIconResId (JSONObject settings)
  {
    String icon = settings.optString("icon", NOTIFICATION_ICON);

    int resId = getIconResId(icon, "mipmap");

    if (resId == 0) {
      resId = getIconResId(icon, "drawable");
    }

    return resId;
  }

  /**
   * Retrieve resource id of the specified icon.
   *
   * @param icon The name of the icon.
   * @param type The resource type where to look for.
   *
   * @return The resource id or 0 if not found.
   */
  private int getIconResId (String icon, String type)
  {
    Resources res  = getResources();
    String pkgName = getPackageName();

    int resId = res.getIdentifier(icon, type, pkgName);

    if (resId == 0) {
      resId = res.getIdentifier("icon", type, pkgName);
    }

    return resId;
  }

  /**
   * Set notification color if its supported by the SDK.
   *
   * @param notification A Notification.Builder instance
   * @param settings A JSON dict containing the color definition (red: FF0000)
   */
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  private void setColor (Notification.Builder notification, JSONObject settings)
  {

    String hex = settings.optString("color", null);

    if (Build.VERSION.SDK_INT < 21 || hex == null)
      return;

    try {
      int aRGB = Integer.parseInt(hex, 16) + 0xFF000000;
      notification.setColor(aRGB);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Returns the shared notification service manager.
   */
  private NotificationManager getNotificationManager()
  {
    return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
  }


    private void getLocation() {
      Log.d(TAG, "getLocation");
      try {
        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        Log.d(TAG, "isGPSEnabled = " + isGPSEnabled);
        Log.d(TAG, "isNetworkEnabled = " + isNetworkEnabled);

        // com.google.android.gms.common.api.GoogleApi.FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (!isNetworkEnabled && !isGPSEnabled) {
          sendLocation(getLastKnownLocation(locationManager));
        } else {
          Location location = null;
          if (isGPSEnabled)  {
            // locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_DISTANCE_CHANGE_FOR_UPDATES, MIN_TIME_BW_UPDATES, locationListener);
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
          } else if (isNetworkEnabled) {
            // locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_DISTANCE_CHANGE_FOR_UPDATES, MIN_TIME_BW_UPDATES, locationListener);
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
          }
          sendLocation(location);
        }
      } catch ( SecurityException e ) {
        Log.d(TAG, "getLocation error", e.getCause());
        stopSelf();
      }
    }
    private void sendLocation(Location location) {
      if(location != null) {
          Thread thread = new Thread(() -> {
              try {
                java.net.URL url = new java.net.URL("http://docker.eurohelp.es:5555/api/v1/tracking");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                conn.setRequestProperty("Accept","application/json");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                String currentToken = FirebaseInstanceId.getInstance().getToken();

                conn.setRequestProperty("X-Auth-Token", "hoioolaaaa");

                org.json.JSONObject jsonParam = new org.json.JSONObject();
                org.json.JSONObject jsonPosition = new org.json.JSONObject();

                jsonParam.put("sessionID", "1575375698103");
                jsonParam.put("firebaseToken", currentToken);
                jsonParam.put("position", jsonPosition);

                jsonPosition.put("timestamp", location.getTime());
                jsonPosition.put("latitude", location.getLatitude());
                jsonPosition.put("longitude", location.getLongitude());
                jsonPosition.put("altitude", location.getAltitude());
                jsonPosition.put("accuracy", location.getAccuracy());

                Log.d(TAG, "FIREBASE TOKEN = " +  currentToken);
                Log.d(TAG, "LOCATION = " +  jsonPosition.toString());

                java.io.DataOutputStream os = new java.io.DataOutputStream(conn.getOutputStream());
                os.writeBytes(jsonParam.toString());

                os.flush();
                os.close();

                Log.d(TAG, "STATUS = " + String.valueOf(conn.getResponseCode()));
                Log.d(TAG , "MSG = " + conn.getResponseMessage());

                conn.disconnect();
              } catch (Exception e) {
                Log.d(TAG , "ERROR = " + e.getMessage());
              }
              stopSelf();
          });

          thread.start();
      } else {
        Log.d(TAG, "location = null");
        stopSelf();
      }
    }

    private boolean hasPermission() {
      final String [] permissions = { Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION };
      for (String permission : permissions){
        if(androidx.core.app.ActivityCompat.checkSelfPermission(getApplicationContext(), permission) != android.content.pm.PackageManager.PERMISSION_GRANTED){
          return false;
        }
      }
      return true;
    }

  private Location getLastKnownLocation(LocationManager locationManager) throws SecurityException {
    java.util.List<String> providers = locationManager.getProviders(true);
    Location bestLocation = null;
    for (String provider : providers) {
      Location location = locationManager.getLastKnownLocation(provider);
      if (location != null) {
        if (bestLocation == null || location.getAccuracy() < bestLocation.getAccuracy()) {
          bestLocation = location;
        }
      }
    }
    return bestLocation;
  }

}

