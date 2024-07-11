package com.inventrax.jungheinrich.services;// LogoutWorker.java

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.inventrax.jungheinrich.activities.LoginActivity;


public class LogoutWorker extends Worker {


    public LogoutWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Perform logout here
        logoutUser();
        return Result.success();
    }

    private void logoutUser() {
        // Perform the logout logic, such as clearing session data, resetting user state, etc.
        Context applicationContext = getApplicationContext();
        // Example: Restart the app or navigate to the login screen
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        applicationContext.startActivity(intent);
    }
}
