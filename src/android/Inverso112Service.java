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

import java.io.UnsupportedEncodingException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
// import javax.xml.bind.DatatypeConverter;


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

            java.net.URL url = new java.net.URL("https://servicios.pre.ertzaintza.eus/euskarri/112SosDeiak/App/B76/api/v1/reverse112");

            javax.net.ssl.HttpsURLConnection conn = (javax.net.ssl.HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(android.net.SSLCertificateSocketFactory.getInsecure(0, null));
            conn.setHostnameVerifier(getHostnameVerifier());

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("Accept","application/json");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            String firebaseToken = FirebaseInstanceId.getInstance().getToken();

            String authToken = getToken(uuid);

            Log.d(TAG, "X-Auth-Token1: " + authToken);

            conn.setRequestProperty("X-Auth-Token", authToken);
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
            jsonPosition.put("precision", Math.round(location.getAccuracy()));

            Log.d(TAG, "FIREBASE TOKEN = " +  firebaseToken);
            Log.d(TAG, "LOCATION = " +  jsonPosition.toString());


            Log.d(TAG, "BODY = " +  jsonParam.toString());

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
  private javax.net.ssl.HostnameVerifier getHostnameVerifier() {
    javax.net.ssl.HostnameVerifier hostnameVerifier = new javax.net.ssl.HostnameVerifier() {
      @Override
      public boolean verify(String hostname, javax.net.ssl.SSLSession session) {
        javax.net.ssl.HostnameVerifier hv =
          javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier();
        return hv.verify("servicios.pre.ertzaintza.eus", session);
      }
    };
    return hostnameVerifier;
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

  private String getToken(String uuid) {
    int keySize = 256;
    String secretKeyInstance = "pvBHOsitvs0mt9OLabzxkAu806FHjVcxeE2QWbahtYrKX6lY5KjdaBbYxEwFow2s";
    long timestamp = new Date().getTime();
    return generateSessionToken(uuid,timestamp,secretKeyInstance,keySize) + ":" + timestamp;
  }
  public String generateSessionToken(String uuid, long timestamp, String passphrase, int keySize) {
    String token = new StringBuilder(64).append(uuid).append('+').append(timestamp).toString();
    return new AesCipher(keySize).encrypt(passphrase, token);
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

class AesCipher {

  private static final String ENCODING = "UTF-8";
  private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
  private static final String DEFAULT_IV = "00000000000000000000000000000000";
  private static final String DEFAULT_SALT = "00000000000000000000000000000000";

  private final int keySize;
  private final int iterationCount;
  private final String salt;
  private final String iv;
  private final Cipher cipher;

  public AesCipher(int keySize) {
    this(keySize, 10, DEFAULT_SALT, DEFAULT_IV);
  }

  public AesCipher(int keySize, int iterationCount, String salt, String iv) {
    this.keySize = keySize;
    this.iterationCount = iterationCount;
    this.salt = salt;
    this.iv = iv;
    try {
      cipher = Cipher.getInstance(ALGORITHM);
    } catch (Exception e) {
      throw fail(e);
    }
  }

  public String encrypt(String passphrase, String plaintext) {
    try {
      SecretKey key = generateKey(salt, passphrase);
      byte[] encrypted = doFinal(Cipher.ENCRYPT_MODE, key, iv, plaintext.getBytes(ENCODING));
      return encodeBase64(encrypted);
    } catch (UnsupportedEncodingException e) {
      throw fail(e);
    }
  }

  public String decrypt(String passphrase, String ciphertext) {
    try {
      SecretKey key = generateKey(salt, passphrase);
      byte[] decrypted = doFinal(Cipher.DECRYPT_MODE, key, iv, decodeBase64(ciphertext));
      return new String(decrypted, ENCODING);
    } catch (UnsupportedEncodingException e) {
      throw fail(e);
    }
  }

  private byte[] doFinal(int encryptMode, SecretKey key, String iv, byte[] bytes) {
    try {
      cipher.init(encryptMode, key, new IvParameterSpec(decodeHex(iv)));
      return cipher.doFinal(bytes);
    } catch (Exception e) {
      throw fail(e);
    }
  }

  private SecretKey generateKey(String salt, String passphrase) {
    try {
      SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), decodeHex(salt), iterationCount, keySize);
      return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    } catch (Exception e) {
      throw fail(e);
    }
  }

  private static String encodeBase64(byte[] bytes) {
    return new DatatypeConverterImpl().printBase64Binary(bytes);
  }

  private static byte[] decodeBase64(String str) {
    return new DatatypeConverterImpl().parseBase64Binary(str);
  }

  private static byte[] decodeHex(String str) {
      return new DatatypeConverterImpl().parseHexBinary(str);
  }

  private IllegalStateException fail(Exception e) {
    return new IllegalStateException(e);
  }

}


class DatatypeConverterImpl  {

  protected DatatypeConverterImpl() {
  }


  public byte[] parseBase64Binary(String lexicalXSDBase64Binary) {
    return _parseBase64Binary(lexicalXSDBase64Binary);
  }

  public byte[] parseHexBinary(String s) {
    final int len = s.length();

    if( len%2 != 0 )
      throw new IllegalArgumentException("hexBinary needs to be even-length: "+s);

    byte[] out = new byte[len/2];

    for( int i=0; i<len; i+=2 ) {
      int h = hexToBin(s.charAt(i  ));
      int l = hexToBin(s.charAt(i+1));
      if( h==-1 || l==-1 )
        throw new IllegalArgumentException("contains illegal character for hexBinary: "+s);

      out[i/2] = (byte)(h*16+l);
    }

    return out;
  }

  private static int hexToBin( char ch ) {
    if( '0'<=ch && ch<='9' )    return ch-'0';
    if( 'A'<=ch && ch<='F' )    return ch-'A'+10;
    if( 'a'<=ch && ch<='f' )    return ch-'a'+10;
    return -1;
  }

  public String printBase64Binary(byte[] val) {
    return _printBase64Binary(val);
  }

  private static final byte[] decodeMap = initDecodeMap();
  private static final byte PADDING = 127;

  private static byte[] initDecodeMap() {
    byte[] map = new byte[128];
    int i;
    for( i=0; i<128; i++ )        map[i] = -1;

    for( i='A'; i<='Z'; i++ )    map[i] = (byte)(i-'A');
    for( i='a'; i<='z'; i++ )    map[i] = (byte)(i-'a'+26);
    for( i='0'; i<='9'; i++ )    map[i] = (byte)(i-'0'+52);
    map['+'] = 62;
    map['/'] = 63;
    map['='] = PADDING;

    return map;
  }

  private static int guessLength( String text ) {
    final int len = text.length();

    int j=len-1;
    for(; j>=0; j-- ) {
      byte code = decodeMap[text.charAt(j)];
      if(code==PADDING)
        continue;
      if(code==-1)
        return text.length()/4*3;
      break;
    }

    j++;
    int padSize = len-j;
    if(padSize >2)
      return text.length()/4*3;

    return text.length()/4*3-padSize;
  }

  public static byte[] _parseBase64Binary(String text) {
    final int buflen = guessLength(text);
    final byte[] out = new byte[buflen];
    int o=0;

    final int len = text.length();
    int i;

    final byte[] quadruplet = new byte[4];
    int q=0;

    for( i=0; i<len; i++ ) {
      char ch = text.charAt(i);
      byte v = decodeMap[ch];

      if( v!=-1 )
        quadruplet[q++] = v;

      if(q==4) {
        out[o++] = (byte)((quadruplet[0]<<2)|(quadruplet[1]>>4));
        if( quadruplet[2]!=PADDING )
          out[o++] = (byte)((quadruplet[1]<<4)|(quadruplet[2]>>2));
        if( quadruplet[3]!=PADDING )
          out[o++] = (byte)((quadruplet[2]<<6)|(quadruplet[3]));
        q=0;
      }
    }

    if(buflen==o)
      return out;

    byte[] nb = new byte[o];
    System.arraycopy(out,0,nb,0,o);
    return nb;
  }

  private static final char[] encodeMap = initEncodeMap();

  private static char[] initEncodeMap() {
    char[] map = new char[64];
    int i;
    for( i= 0; i<26; i++ )        map[i] = (char)('A'+i);
    for( i=26; i<52; i++ )        map[i] = (char)('a'+(i-26));
    for( i=52; i<62; i++ )        map[i] = (char)('0'+(i-52));
    map[62] = '+';
    map[63] = '/';

    return map;
  }

  public static char encode( int i ) {
    return encodeMap[i&0x3F];
  }

  public static String _printBase64Binary(byte[] input) {
    return _printBase64Binary(input, 0, input.length);
  }
  public static String _printBase64Binary(byte[] input, int offset, int len) {
    char[] buf = new char[((len+2)/3)*4];
    int ptr = _printBase64Binary(input,offset,len,buf,0);
    assert ptr==buf.length;
    return new String(buf);
  }

  public static int _printBase64Binary(byte[] input, int offset, int len, char[] buf, int ptr) {
    for( int i=offset; i<len; i+=3 ) {
      switch( len-i ) {
        case 1:
          buf[ptr++] = encode(input[i]>>2);
          buf[ptr++] = encode(((input[i])&0x3)<<4);
          buf[ptr++] = '=';
          buf[ptr++] = '=';
          break;
        case 2:
          buf[ptr++] = encode(input[i]>>2);
          buf[ptr++] = encode(
            ((input[i]&0x3)<<4) |
              ((input[i+1]>>4)&0xF));
          buf[ptr++] = encode((input[i+1]&0xF)<<2);
          buf[ptr++] = '=';
          break;
        default:
          buf[ptr++] = encode(input[i]>>2);
          buf[ptr++] = encode(
            ((input[i]&0x3)<<4) |
              ((input[i+1]>>4)&0xF));
          buf[ptr++] = encode(
            ((input[i+1]&0xF)<<2)|
              ((input[i+2]>>6)&0x3));
          buf[ptr++] = encode(input[i+2]&0x3F);
          break;
      }
    }
    return ptr;
  }

}
