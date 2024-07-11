package com.inventrax.tvs.application;

import android.app.Application;
import android.content.Context;


import androidx.multidex.MultiDex;

import com.inventrax.tvs.services.InactivityLogoutService;

import java.util.Map;


public class AppController extends Application {

    public static final String TAG = com.inventrax.tvs.application.AppController.class.getSimpleName();
    public static String DEVICE_GCM_REGISTER_ID;
    private static com.inventrax.tvs.application.AppController mInstance;
    private  Context appContext;
    public static Map<String,String> mapUserRoutes;


    private InactivityLogoutService inactivityLogoutService;

    public static synchronized com.inventrax.tvs.application.AppController getInstance() {
        return mInstance;
    }


    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Initializing Acra
        mInstance = this;
        MultiDex.install(this);
       // ACRA.init(this);
        AbstractApplication.CONTEXT = getApplicationContext();
        appContext= getApplicationContext();
        inactivityLogoutService = new InactivityLogoutService();
        //LocaleHelper.onCreate(this, "en");


        //LocaleHelper.onCreate(this, "en");

    }



    @Override
    public void onLowMemory() {
        super.onLowMemory();

        System.runFinalization();
        Runtime.getRuntime().gc();
        System.gc();

    }
    public InactivityLogoutService getInactivityLogoutService() {
        return inactivityLogoutService;
    }

}