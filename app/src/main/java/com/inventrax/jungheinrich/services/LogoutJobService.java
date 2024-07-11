package com.inventrax.jungheinrich.services;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.inventrax.jungheinrich.activities.LoginActivity;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class LogoutJobService extends JobService {
    private static final String TAG = "LogoutJobService";
    private static final int JOB_ID = 123;

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "Job started");

        // Perform logout action here
        performLogout();

        // Reschedule the job for the next execution
       // scheduleJob();

        // Return false as the job is completed synchronously
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "Job stopped");
        return false;
    }

    public static void scheduleJob(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        ComponentName componentName = new ComponentName(context, LogoutJobService.class);

        JobInfo jobInfo = new JobInfo.Builder(JOB_ID, componentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(15 * 60 * 1000) // 15 minutes
                .build();

        // Schedule the job
        int resultCode = jobScheduler.schedule(jobInfo);
        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Job scheduled successfully");
        } else {
            Log.d(TAG, "Job scheduling failed");
        }
    }

    private void performLogout() {
        // Implement your logout logic here
        // ...
        Context applicationContext = getApplicationContext();
        // Example: Restart the app or navigate to the login screen
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        applicationContext.startActivity(intent);
    }
}
