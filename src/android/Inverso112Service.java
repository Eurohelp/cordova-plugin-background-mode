package de.appplant.cordova.plugin.background;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;


public class Inverso112Service extends JobService {

  private static final String TAG = "Inverso112Service";

  public static void schedulerJob(Context context) {
      ComponentName serviceComponent = new ComponentName(context, Inverso112Service.class);
      JobInfo.Builder builder = new JobInfo.Builder(0,serviceComponent);
      builder.setMinimumLatency(30*1000);    // wait at least
      builder.setOverrideDeadline(60*1000);  //delay time
      builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);  // require unmetered network
      builder.setRequiresCharging(false);  // we don't care if the device is charging or not
      builder.setRequiresDeviceIdle(true); // device should be idle
        Log.d(TAG, "scheduler Job");

      JobScheduler jobScheduler = null;
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        jobScheduler = context.getSystemService(JobScheduler.class);
      }
      jobScheduler.schedule(builder.build());
  }



    @Override
    public boolean onStartJob(JobParameters params) {
      Inverso112Service.schedulerJob(getApplicationContext()); // reschedule the job
      Log.d(TAG, "onStartJob");
      return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
      Log.d(TAG, "onCreate");
      return true;
    }

}
