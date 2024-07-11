package com.inventrax.tvs.util;

/**
 * Created by nareshp on 05/01/2016.
 */

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class FragmentUtils {

    private static FragmentManager fragmentManager;
    private static FragmentTransaction fragmentTransaction;

    /**
     * To add fragment to the container
     *
     * @param activity          current FragmentActivity reference
     * @param fragmentContainer fragment container which holds the specified fragment
     * @param fragment          fragment added to the specified container
     */
    public static void addFragment(androidx.fragment.app.FragmentActivity activity, int fragmentContainer, androidx.fragment.app.Fragment fragment) {

        try {
            fragmentManager = activity.getSupportFragmentManager();
            fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(fragmentContainer, fragment);
            fragmentTransaction.commit();
        } catch (Exception ex) {

        }
    }

    /**
     * To add fragment to the container with back stack option
     *
     * @param activity          current FragmentActivity reference
     * @param fragmentContainer fragment container which holds the specified fragment
     * @param fragment          fragment added to the specified container
     */
    public static void addFragmentWithBackStack(FragmentActivity activity, int fragmentContainer, Fragment fragment) {

        try {
            fragmentManager = activity.getSupportFragmentManager();
            fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(fragmentContainer, fragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        } catch (Exception ex) {

        }
    }

    /**
     * To replace fragment for the specified container
     *
     * @param activity          current FragmentActivity reference
     * @param fragmentContainer fragment container which holds the specified fragment
     * @param fragment          fragment added to the specified container
     */
    public static void replaceFragment(FragmentActivity activity, int fragmentContainer, Fragment fragment) {

        try {
            fragmentManager = activity.getSupportFragmentManager();
            fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(fragmentContainer, fragment);
            fragmentTransaction.commit();
        } catch (Exception ex) {

        }
    }

    /**
     * To replace fragment for the specified container with back stack option
     *
     * @param activity          current FragmentActivity reference
     * @param fragmentContainer fragment container which holds the specified fragment
     * @param fragment          fragment added to the specified container
     */
    public static void replaceFragmentWithBackStack(androidx.fragment.app.FragmentActivity activity, int fragmentContainer, Fragment fragment) {

        try {
            fragmentManager = activity.getSupportFragmentManager();
            fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(fragmentContainer, fragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        } catch (Exception ex) {

        }
    }

    /**
     * To replace fragment for the specified container with back stack option
     *
     * @param activity          current FragmentActivity reference
     * @param fragmentContainer fragment container which holds the specified fragment
     * @param fragment          fragment added to the specified container
     */
    public static void replaceFragmentWithBackStackWithArguments(FragmentActivity activity, int fragmentContainer, Fragment fragment, Bundle args) {

        try {
            fragmentManager = activity.getSupportFragmentManager();
            fragmentTransaction = fragmentManager.beginTransaction();
            fragment.setArguments(args);
            fragmentTransaction.replace(fragmentContainer, fragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        } catch (Exception ex) {

        }
    }

    /**
     * To remove fragment from the container
     *
     * @param activity current FragmentActivity reference
     * @param fragment fragment added to the specified container
     */
    public static void removeFragment(FragmentActivity activity, Fragment fragment) {

        try {
            fragmentManager = activity.getSupportFragmentManager();
            fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.remove(fragment);
            fragmentTransaction.commit();
        } catch (Exception ex) {

        }
    }


}