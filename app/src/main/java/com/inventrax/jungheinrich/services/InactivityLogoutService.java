package com.inventrax.jungheinrich.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import com.inventrax.jungheinrich.activities.LoginActivity;

public class InactivityLogoutService extends Service {


    public interface LogOutListener {
        void doLogout();
    }


    private LogOutListener logOutListener;

    public void setLogOutListener(LogOutListener listener) {
        this.logOutListener = listener;
    }
    private static final long INACTIVITY_DELAY_MS = 5 * 60 * 1000; // 15 minutes

    private Handler handler;
    private Runnable logoutRunnable;
    private long lastInteractionTime;

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler();
        logoutRunnable = this::logout;
        lastInteractionTime = System.currentTimeMillis();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startTimer();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new InactivityLogoutServiceBinder();
    }



    private void startTimer() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long timeSinceLastInteraction = System.currentTimeMillis() - lastInteractionTime;
                if (timeSinceLastInteraction >= INACTIVITY_DELAY_MS) {
                    logout();
                } else {
                    // reset the timer if the time since last interaction is less than INACTIVITY_DELAY_MS
                    handler.postDelayed(this, INACTIVITY_DELAY_MS - timeSinceLastInteraction);
                }
            }
        }, INACTIVITY_DELAY_MS);
    }


    private void logout() {
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
       /* if (logOutListener != null) {
            logOutListener.doLogout();
        }*/
        // Clear the user's session and navigate to the login screen
        // Or display a toast message indicating that the user has been logged out due to inactivity
    }

    public class InactivityLogoutServiceBinder extends Binder {
        public InactivityLogoutService getService() {
            return InactivityLogoutService.this;
        }
    }
    public void restartTimer() {
        lastInteractionTime = System.currentTimeMillis();
        handler.removeCallbacks(logoutRunnable);
        startTimer();
    }
}



