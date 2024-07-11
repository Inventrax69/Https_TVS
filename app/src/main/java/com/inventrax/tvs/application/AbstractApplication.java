package com.inventrax.tvs.application;

import android.content.Context;

import androidx.fragment.app.FragmentActivity;


public class AbstractApplication {

    public static Context CONTEXT;
    public static androidx.fragment.app.FragmentActivity FRAGMENT_ACTIVITY;

    public static Context  get(){
       return CONTEXT;
    }

    public static FragmentActivity getFragmentActivity(){
        return  FRAGMENT_ACTIVITY;
    }


}
