package com.inventrax.tvs.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UserPresentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
                Intent serviceIntent = new Intent(context, InactivityLogoutService.class);
                context.startService(serviceIntent);
            }
        }
    }