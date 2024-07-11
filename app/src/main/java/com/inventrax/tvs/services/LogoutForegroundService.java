package com.inventrax.tvs.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.inventrax.tvs.R;
import com.inventrax.tvs.activities.LoginActivity;

public class LogoutForegroundService extends Service {

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "logout_channel";
    private static final String CHANNEL_NAME = "Logout Channel";

    @Override
    public void onCreate() {
        super.onCreate();

        // Create the notification channel for the foreground service
        createNotificationChannel();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Perform the logout logic, such as clearing session data, resetting user state, etc.
        // Example: Restart the app or navigate to the login screen
        if (intent != null && intent.getBooleanExtra("timeout", false)) {
            // Perform the logout logic here
            Intent logoutIntent = new Intent(getApplicationContext(), LoginActivity.class);
            logoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(logoutIntent);
        }
        // Make the service a foreground service to keep it running in the foreground
        startForeground(NOTIFICATION_ID, buildNotification());

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("App Logout")
                .setContentText("You have been logged out due to inactivity.")
                .setSmallIcon(R.mipmap.in_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true);

        return builder.build();
    }


}
