package de.appplant.cordova.plugin.background;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

import android.Manifest;

public class Inverso112Service extends JobService {

  private static final String TAG = "Inverso112Service";

  public static void schedulerJob(Context context) {
      ComponentName serviceComponent = new ComponentName(context, Inverso112Service.class);
      JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
      builder.setMinimumLatency(1 * 1000 * 60);    // wait at least
      builder.setOverrideDeadline(3 * 1000 * 60);  //delay time
      builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);  // require unmetered network
      builder.setRequiresCharging(false);  // we don't care if the device is charging or not
      builder.setRequiresDeviceIdle(true); // device should be idle
      Log.d(TAG, "schedulerJob");

      JobScheduler jobScheduler = null;
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.schedule(builder.build());
      } else{
        Log.d(TAG, "NO schedulerJob");
      }
  }



    @Override
    public boolean onStartJob(JobParameters params) {
      Inverso112Service.schedulerJob(getApplicationContext()); // re-schedule the job
      Log.d(TAG, "onStartJob");

      if(hasPermission()) {
        Log.d(TAG, "hasPermission() = true");
        getLocation();
      } else {
        Log.d(TAG, "hasPermission() = false");
      }

      return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
      Log.d(TAG, "onStopJob");
      return true;
    }

    private void getLocation() {
      Log.d(TAG, "getLocation");
      try {
        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        Log.d(TAG, "isGPSEnabled = " + isGPSEnabled);
        Log.d(TAG, "isNetworkEnabled = " + isNetworkEnabled);

        /*if (!isNetworkEnabled && !isGPSEnabled) {
          // cannot get location
        } else {
          Location location = null;
          if (isNetworkEnabled) {
            // locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_DISTANCE_CHANGE_FOR_UPDATES, MIN_TIME_BW_UPDATES, locationListener);
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
          } else if (isGPSEnabled)  {
            // locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_DISTANCE_CHANGE_FOR_UPDATES, MIN_TIME_BW_UPDATES, locationListener);
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
          }
          sendLocation(location);
        }*/
        sendLocation(getLastKnownLocation(locationManager));
      } catch ( SecurityException e ) {
        Log.d(TAG, "getLocation error", e.getCause());
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

                conn.setRequestProperty("X-Auth-Token", "HOLLAAAAAAA");

                org.json.JSONObject jsonParam = new org.json.JSONObject();
                org.json.JSONObject jsonPosition = new org.json.JSONObject();

                jsonParam.put("sessionID", "1575375698103");
                jsonParam.put("firebaseToken", "firebaseToken01");
                jsonParam.put("position", jsonPosition);

                jsonPosition.put("timestamp", location.getTime());
                jsonPosition.put("latitude", location.getLatitude());
                jsonPosition.put("longitude", location.getLongitude());
                jsonPosition.put("altitude", location.getAltitude());
                jsonPosition.put("accuracy", location.getAccuracy());

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
          });

          thread.start();
      } else {
        Log.d(TAG, "location = null");
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
