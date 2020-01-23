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
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import android.content.Context;
import android.Manifest;
import com.google.firebase.iid.FirebaseInstanceId;
import org.json.JSONObject;

import java.util.Date;

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
      getLocation();
    } else {
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
      com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this);
      fusedLocationClient.getLastLocation().addOnCompleteListener(task -> {
        if (task.isSuccessful()) {
          sendLocation(task.getResult());
        } else {
          Log.d(TAG, "getLocation error", task.getException().getCause());
          stopSelf();
        }
      });
    }

  private void sendLocation(Location location) {
    if(location != null) {
      Thread thread = new Thread(() -> {
        try {
          String[] fromDatabase = getFromDB();

          if(fromDatabase != null && fromDatabase.length != 0) {

            String uuid = fromDatabase[0];
            String telefono = fromDatabase[1];

            Log.d(TAG, "UUID: " + uuid);
            Log.d(TAG, "TELEFONO: " + telefono);

            java.net.URL url = new java.net.URL("http://docker.eurohelp.es:5555/api/v2/inverse112");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("Accept","application/json");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            String firebaseToken = FirebaseInstanceId.getInstance().getToken();

            String authToken = getToken(uuid);
            Log.d(TAG, "X-Auth-Token: " + android.util.Base64.encodeToString(authToken.getBytes(), android.util.Base64.NO_WRAP));

            conn.setRequestProperty("X-Auth-Token", android.util.Base64.encodeToString(authToken.getBytes(), android.util.Base64.NO_WRAP));
            conn.setRequestProperty("FCM-Token", firebaseToken);

            org.json.JSONObject jsonParam = new org.json.JSONObject();
            org.json.JSONObject jsonPosition = new org.json.JSONObject();

            android.telephony.TelephonyManager manager = (android.telephony.TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            String versionApp = "";
            try {
              android.content.pm.PackageInfo pInfo = getApplicationContext().getPackageManager().getPackageInfo(getPackageName(), 0);
              versionApp = pInfo.versionName;
            } catch (android.content.pm.PackageManager.NameNotFoundException e) {
              e.printStackTrace();
            }

            Log.d(TAG, "operadora: " + manager.getSimOperatorName());

            jsonParam.put("SO", "Android");
            jsonParam.put("fechaEnvio", location.getTime());
            jsonParam.put("operadora", manager.getSimOperatorName());
            jsonParam.put("versionApp", versionApp);
            jsonParam.put("versionSO", android.os.Build.VERSION.RELEASE);
            jsonParam.put("ubicacion", jsonPosition);
            jsonParam.put("telefono", telefono);

            jsonPosition.put("lat", location.getLatitude());
            jsonPosition.put("lon", location.getLongitude());
            jsonPosition.put("precision", location.getAccuracy());

            Log.d(TAG, "FIREBASE TOKEN = " +  firebaseToken);
            Log.d(TAG, "LOCATION = " +  jsonPosition.toString());

            java.io.DataOutputStream os = new java.io.DataOutputStream(conn.getOutputStream());
            os.writeBytes(jsonParam.toString());

            os.flush();
            os.close();

            Log.d(TAG, "STATUS = " + String.valueOf(conn.getResponseCode()));
            Log.d(TAG , "MSG = " + conn.getResponseMessage());

            conn.disconnect();
          }
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



    private static javax.crypto.spec.SecretKeySpec getSecretKey(String secretKeyInstance, String salt, int pswdIterations, int keySize) {
      try {
        javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        java.security.spec.KeySpec spec = new javax.crypto.spec.PBEKeySpec(secretKeyInstance.toCharArray(), /*salt.getBytes()*/ Hex.decodeHex(salt.toCharArray()), pswdIterations, keySize);

        return new javax.crypto.spec.SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
      } catch (java.security.spec.InvalidKeySpecException e) {
        e.printStackTrace();
      } catch (java.security.NoSuchAlgorithmException e) {
        e.printStackTrace();
      } catch (Exception e) {
        e.printStackTrace();
      }
      return null;
    }

    private static String getToken(String uuid) throws Exception {

      int pswdIterations = 10;
      int keySize = 256;
      String cypherInstance = "AES/CBC/PKCS5Padding";
      String secretKeyInstance = "pvBHOsitvs0mt9OLabzxkAu806FHjVcxeE2QWbahtYrKX6lY5KjdaBbYxEwFow2s";
      String AESSalt = "00000000000000000000000000000000";
      String initializationVector = "0000000000000000";

      long timestamp = new Date().getTime();
      String plaintext = uuid + "+" + timestamp;

      javax.crypto.spec.SecretKeySpec skeySpec = getSecretKey(secretKeyInstance, AESSalt, pswdIterations, keySize);

      javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(cypherInstance);

      cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, skeySpec, new javax.crypto.spec.IvParameterSpec(initializationVector.getBytes()));

      byte[] encrypted = cipher.doFinal(plaintext.getBytes());

      return (android.util.Base64.encodeToString(encrypted, android.util.Base64.DEFAULT) + ":" + timestamp);
    }

  private String[] getFromDB() {
    String DB_PATH;
    if (android.os.Build.VERSION.SDK_INT >= 17)
      DB_PATH = getApplicationContext().getApplicationInfo().dataDir + "/databases/";
    else
      DB_PATH = "/data/data/" + getApplicationContext().getPackageName() + "/databases/";

    java.io.File dbfile = new java.io.File(DB_PATH + "APP112");
    if (!dbfile.exists()) {
      return null;
    }
    android.database.sqlite.SQLiteDatabase mydb = android.database.sqlite.SQLiteDatabase.openDatabase(dbfile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);

    android.database.Cursor resultSet = mydb.rawQuery("select uuid, telefono from personalData", null);
    resultSet.moveToFirst();

    return new String [] { resultSet.getString(0), resultSet.getString(1) };
  }

}

class Hex {

  public static byte[] decodeHex(char[] data) throws Exception {
    int len = data.length;
    if ((len & 0x01) != 0) {
      throw new Exception("Odd number of characters.");
    }
    byte[] out = new byte[len >> 1];
    // two characters form the hex value.
    for (int i = 0, j = 0; j < len; i++) {
      int f = toDigit(data[j], j) << 4;
      j++;
      f = f | toDigit(data[j], j);
      j++;
      out[i] = (byte) (f & 0xFF);
    }
    return out;
  }
  protected static int toDigit(char ch, int index) throws Exception {
    int digit = Character.digit(ch, 16);
    if (digit == -1) {
      throw new Exception("Illegal hexadecimal charcter " + ch + " at index " + index);
    }
    return digit;
  }

}
