package com.inventrax.jungheinrich.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.inventrax.jungheinrich.R;
import com.inventrax.jungheinrich.activities.LoginActivity;

public class LogoutService extends Service {

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "LogoutServiceChannel";
    private static final CharSequence CHANNEL_NAME = "Logout Service Channel";
    private static final long LOGOUT_INTERVAL = 15 * 60 * 1000;
    private Handler handler;
    private Runnable logoutRunnable;
    long startTime;



    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
         startTime = System.currentTimeMillis();
        Log.d("entered to the task", "started  Time:");
        logoutRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;

                // Log the elapsed time


                // Perform logout operations here
                // e.g., Clear user session, reset application state, etc.
                try {
                    Log.d("LogoutService", "Elapsed Time:");
                    Context applicationContext = getApplicationContext();

                    Toast.makeText(applicationContext, "Service called ", Toast.LENGTH_LONG).show();
                    // Example: Restart the app or navigate to the login screen
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    applicationContext.startActivity(intent);
                    handler.postDelayed(logoutRunnable, LOGOUT_INTERVAL);
                    // Schedule the next logout
                }catch (Exception e){
                    e.printStackTrace();
                     Log.d("Myexception", "exception"+""+e);
                }

            }
        };
        IntentFilter filter = new IntentFilter("USER_INTERACTION");
        registerReceiver(userInteractionReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        startLogoutTimer();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Not used in this example
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLogoutTimer();
        unregisterReceiver(userInteractionReceiver);
    }

    private void startLogoutTimer() {
        handler.postDelayed(logoutRunnable, LOGOUT_INTERVAL);


    }

    private void stopLogoutTimer() {
        handler.removeCallbacks(logoutRunnable);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Logout Service")
                .setContentText("Running in the background")
                .setSmallIcon(R.mipmap.in_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true);

        return builder.build();
    }



//whenever user intraction the receiver will call
    private BroadcastReceiver userInteractionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            restartLogoutTimer();
        }
    };

//  resetting the timer to 0 if user intracted with screen .
    private void restartLogoutTimer() {
        handler.removeCallbacks(logoutRunnable);
        handler.postDelayed(logoutRunnable, LOGOUT_INTERVAL);
    }
}
