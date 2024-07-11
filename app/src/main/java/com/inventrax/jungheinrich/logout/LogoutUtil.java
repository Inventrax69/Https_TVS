package com.inventrax.jungheinrich.logout;

import android.app.Activity;
import android.content.Intent;


import androidx.fragment.app.FragmentActivity;

import com.inventrax.jungheinrich.activities.LoginActivity;
import com.inventrax.jungheinrich.interfaces.MainActivityView;
import com.inventrax.jungheinrich.util.ProgressDialogUtils;
import com.inventrax.jungheinrich.util.SharedPreferencesUtils;


public class LogoutUtil {

    private androidx.fragment.app.FragmentActivity fragmentActivity;
    private SharedPreferencesUtils sharedPreferencesUtils;
    private Activity activity;
    private MainActivityView mainActivityView;



    public void setFragmentActivity(FragmentActivity fragmentActivity) {
        this.fragmentActivity = fragmentActivity;
    }

    public void setSharedPreferencesUtils(SharedPreferencesUtils sharedPreferencesUtils) {
        this.sharedPreferencesUtils = sharedPreferencesUtils;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
        new ProgressDialogUtils(activity);
    }



    public void setMainActivityView(MainActivityView mainActivityView) {
        this.mainActivityView = mainActivityView;
    }











    public void doLogout(){

        try
        {
            Intent loginIntent = new Intent(activity, LoginActivity.class);
            activity.startActivity(loginIntent);
            sharedPreferencesUtils.removePreferences("url");

            //Toast.makeText(activity, "You have successfully logged out", Toast.LENGTH_LONG).show();
            activity.finish();

        }catch (Exception ex){

        }

    }


}
