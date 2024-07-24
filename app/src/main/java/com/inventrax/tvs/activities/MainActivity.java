package com.inventrax.tvs.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.work.WorkManager;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.honeywell.aidc.BarcodeReader;
import com.inventrax.tvs.R;
import com.inventrax.tvs.application.AbstractApplication;
import com.inventrax.tvs.application.AppController;
import com.inventrax.tvs.fragments.AboutFragment;
import com.inventrax.tvs.fragments.CcDetailsFragment;
import com.inventrax.tvs.fragments.CcHeaderFragment;
import com.inventrax.tvs.fragments.CycleCountDetailsFragment;
import com.inventrax.tvs.fragments.DeleteOBDPickedItemsFragment;
import com.inventrax.tvs.fragments.DeleteVLPDPickedItemsFragment;
import com.inventrax.tvs.fragments.DetailsFlaggedBinsFragment;
import com.inventrax.tvs.fragments.DrawerFragment;
import com.inventrax.tvs.fragments.GoodsInFragment;
import com.inventrax.tvs.fragments.GskPickingDetailsFragment;
import com.inventrax.tvs.fragments.GskPickingHeaderFragment;
import com.inventrax.tvs.fragments.HeaderFlaggedBinsFragment;
import com.inventrax.tvs.fragments.HomeFragment;
import com.inventrax.tvs.fragments.InnerTransferFragment;
import com.inventrax.tvs.fragments.InternalBinTransferFragment;
import com.inventrax.tvs.fragments.InternalPickingDetailsFragment;
import com.inventrax.tvs.fragments.InternalPickingHeaderFragment;
import com.inventrax.tvs.fragments.InternalTransferFragment;
import com.inventrax.tvs.fragments.ItemPutawayFragment;
import com.inventrax.tvs.fragments.LiveStockFragment;
import com.inventrax.tvs.fragments.LoadGenerationFragment;
import com.inventrax.tvs.fragments.LoadSheetFragment;
import com.inventrax.tvs.fragments.LoadingFragment;
import com.inventrax.tvs.fragments.MaterialTransferFragment;
import com.inventrax.tvs.fragments.NewLoadSheetFragment;
import com.inventrax.tvs.fragments.OBDPickingDetailsFragment;
import com.inventrax.tvs.fragments.OBDPickingHeaderFragment;
import com.inventrax.tvs.fragments.OutboundRevertDetailsFragment;
import com.inventrax.tvs.fragments.OutboundRevertHeaderFragment;
import com.inventrax.tvs.fragments.PackingFragment;
import com.inventrax.tvs.fragments.PackingInfoFragment;
import com.inventrax.tvs.fragments.PalletPickingFragment;
import com.inventrax.tvs.fragments.PalletReceivingFragment;
import com.inventrax.tvs.fragments.PalletTransfersFragment;
import com.inventrax.tvs.fragments.PalletizationFragment;
import com.inventrax.tvs.fragments.PickingDetailsFragment;
import com.inventrax.tvs.fragments.PickingHeaderFragment;
import com.inventrax.tvs.fragments.PutawayDetailsFragment;
import com.inventrax.tvs.fragments.PutawayFragment;
import com.inventrax.tvs.fragments.PutawayHeaderFragment;
import com.inventrax.tvs.fragments.PutawayPalletTransfersFragment;
import com.inventrax.tvs.fragments.SortingFragment;
import com.inventrax.tvs.fragments.StockTakeFragment;
import com.inventrax.tvs.fragments.StockTransferPutAway;
import com.inventrax.tvs.fragments.ToInDropLocation;
import com.inventrax.tvs.fragments.UnloadingFragment;
import com.inventrax.tvs.fragments.VLPDPickingDetailsFragment;
import com.inventrax.tvs.fragments.VLPDPickingHeaderFragment;
import com.inventrax.tvs.fragments.VNATransfersFragment;
import com.inventrax.tvs.fragments.WorkOrderRevertDetailsFragment;
import com.inventrax.tvs.interfaces.ScanKeyListener;
import com.inventrax.tvs.logout.LogoutUtil;
import com.inventrax.tvs.model.NavDrawerItem;
import com.inventrax.tvs.services.InactivityLogoutService;
import com.inventrax.tvs.util.AndroidUtils;
import com.inventrax.tvs.util.DialogUtils;
import com.inventrax.tvs.util.FragmentUtils;
import com.inventrax.tvs.util.ProgressDialogUtils;
import com.inventrax.tvs.util.SharedPreferencesUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements DrawerFragment.FragmentDrawerListener, InactivityLogoutService.LogOutListener {

    private androidx.appcompat.widget.Toolbar mToolbar;
    private DrawerFragment drawerFragment;
    private FragmentUtils fragmentUtils;
    private CharSequence[] userRouteCharSequences;
    private List<String> userRouteStringList;
    private String selectedRouteCode;
    private FragmentActivity fragmentActivity;
    private SharedPreferencesUtils sharedPreferencesUtils;
    private LogoutUtil logoutUtil;
    private static BarcodeReader barcodeReader;
    public String barcode = "";
    GoodsInFragment goodsInFragment;
    IntentIntegrator qrScan;
    private AppController appController;
    Intent serviceIntent;
    private InactivityLogoutService inactivityService;
    private boolean isServiceBound = false;

    private static final String WORK_REQUEST_TAG = "your_work_request_tag";
    private WorkManager workManager;


    ScanKeyListener scanKeyListener = new ScanKeyListener() {
        @Override
        public void getScannedData(String message) {

        }
    };

    public void setScanKeyListener(ScanKeyListener scanKeyListener) {
        this.scanKeyListener = scanKeyListener;
    }

    public static BarcodeReader getBarcodeObject() {
        return barcodeReader;
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @SuppressLint("RestrictedApi")
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {



            if (e.getAction() == KeyEvent.ACTION_DOWN && e.getKeyCode() != KeyEvent.KEYCODE_ENTER) {
                // Log.i(TAG,"dispatchKeyEvent: "+e.toString());
                char pressedKey = (char) e.getUnicodeChar();

                if (Character.toString(pressedKey).matches("^[a-zA-Z0-9!@#$&( )|_\\-`.+,/\"]*$")) {
                    barcode = new StringBuilder(barcode).append(pressedKey).toString();
                }
            }

            if (e.getAction() == KeyEvent.ACTION_DOWN && e.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                // Toast.makeText(getApplicationContext(), barcode, Toast.LENGTH_LONG).show();
                ProcessScan(barcode);
                return true;
            }

        return super.dispatchKeyEvent(e);
    }

    @Override
    public void onBackPressed() {
        barcode="";
        super.onBackPressed();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {

            setContentView(R.layout.activity_main);

            goodsInFragment = new GoodsInFragment();
            appController = AppController.getInstance();
            loadFormControls();


        } catch (Exception ex) {

        }
    }
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @SuppressLint("RestrictedApi")
    public void ProcessScan(String ScannedData){


        if (ScannedData != null) {



            FragmentManager fragmentManager = getSupportFragmentManager();


            for (final androidx.fragment.app.Fragment fragment : fragmentManager.getFragments()) {

                if (fragment != null && fragment.isVisible() && fragment instanceof CycleCountDetailsFragment) {
                    ((CycleCountDetailsFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof DeleteOBDPickedItemsFragment) {
                    ((DeleteOBDPickedItemsFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof DeleteVLPDPickedItemsFragment) {
                    ((DeleteVLPDPickedItemsFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof GoodsInFragment) {
                    ((GoodsInFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof InternalTransferFragment) {
                    ((InternalTransferFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof InnerTransferFragment) {
                    ((InnerTransferFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof LiveStockFragment) {
                    ((LiveStockFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof LoadingFragment) {
                    ((LoadingFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof OBDPickingDetailsFragment) {
                    ((OBDPickingDetailsFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof OBDPickingHeaderFragment) {
                    ((OBDPickingHeaderFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof PalletTransfersFragment) {
                    ((PalletTransfersFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof PutawayFragment) {
                    ((PutawayFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof SortingFragment) {
                    ((SortingFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof StockTransferPutAway) {
                    ((StockTransferPutAway) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof VLPDPickingDetailsFragment) {
                    ((VLPDPickingDetailsFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof LoadSheetFragment) {
                    ((LoadSheetFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof PackingFragment) {
                    ((PackingFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof PackingInfoFragment) {
                    ((PackingInfoFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof LoadGenerationFragment) {
                    ((LoadGenerationFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof NewLoadSheetFragment) {
                    ((NewLoadSheetFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof OutboundRevertHeaderFragment) {
                    ((OutboundRevertHeaderFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof OutboundRevertDetailsFragment) {
                    ((OutboundRevertDetailsFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof MaterialTransferFragment) {
                    ((MaterialTransferFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof PalletizationFragment) {
                    ((PalletizationFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof PutawayHeaderFragment) {
                    ((PutawayHeaderFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof PutawayDetailsFragment) {
                    ((PutawayDetailsFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof PickingDetailsFragment) {
                    ((PickingDetailsFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof PickingHeaderFragment) {
                    ((PickingHeaderFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof PutawayPalletTransfersFragment) {
                    ((PutawayPalletTransfersFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof CcDetailsFragment) {
                    ((CcDetailsFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof GskPickingDetailsFragment) {
                    ((GskPickingDetailsFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof DetailsFlaggedBinsFragment) {
                    ((DetailsFlaggedBinsFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }

                if (fragment != null && fragment.isVisible() && fragment instanceof InternalPickingHeaderFragment) {
                    ((InternalPickingHeaderFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }

                if (fragment != null && fragment.isVisible() && fragment instanceof InternalPickingDetailsFragment) {
                    ((InternalPickingDetailsFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }

                if (fragment != null && fragment.isVisible() && fragment instanceof InternalBinTransferFragment) {
                    ((InternalBinTransferFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof StockTakeFragment) {
                    ((StockTakeFragment) fragment).myScannedData(MainActivity.this, ScannedData);
                }

                if (fragment != null && fragment.isVisible() && fragment instanceof WorkOrderRevertDetailsFragment) {
                    ((WorkOrderRevertDetailsFragment) fragment).myScannedData(MainActivity.this, ScannedData);
                }

                if (fragment != null && fragment.isVisible() && fragment instanceof ItemPutawayFragment) {
                    ((ItemPutawayFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }

                if (fragment != null && fragment.isVisible() && fragment instanceof VNATransfersFragment) {
                    ((VNATransfersFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }

                if (fragment != null && fragment.isVisible() && fragment instanceof ToInDropLocation) {
                    ((ToInDropLocation) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }

                if (fragment != null && fragment.isVisible() && fragment instanceof PalletReceivingFragment) {
                    ((PalletReceivingFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
                if (fragment != null && fragment.isVisible() && fragment instanceof PalletPickingFragment) {
                    ((PalletPickingFragment) fragment).myScannedData(com.inventrax.tvs.activities.MainActivity.this, ScannedData);
                }
            }
        }

        barcode = "";
    }

    public void loadFormControls() {

        try {


            setScanKeyListener(scanKeyListener);

            logoutUtil = new LogoutUtil();

            mToolbar = (androidx.appcompat.widget.Toolbar) findViewById(R.id.toolbar);

            fragmentUtils = new FragmentUtils();

            fragmentActivity = this;

            new ProgressDialogUtils(this);

            AbstractApplication.FRAGMENT_ACTIVITY = this;

            setSupportActionBar(mToolbar);


            View logoView = AndroidUtils.getToolbarLogoIcon(mToolbar);

            if (logoView != null) logoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FragmentUtils.replaceFragmentWithBackStack(fragmentActivity, R.id.container_body, new HomeFragment());
                }
            });

            drawerFragment = (DrawerFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
            drawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), mToolbar);
            drawerFragment.setDrawerListener(this);




            sharedPreferencesUtils = new SharedPreferencesUtils("LoginActivity", getApplicationContext());

            userRouteStringList = new ArrayList<>();


            userRouteCharSequences = userRouteStringList.toArray(new CharSequence[userRouteStringList.size()]);
            logoutUtil.setActivity(this);
            logoutUtil.setFragmentActivity(fragmentActivity);
            logoutUtil.setSharedPreferencesUtils(sharedPreferencesUtils);
            // display the first navigation drawer view on app launch
            displayView(0, new NavDrawerItem(false, "Home"));


            //initializing scan object
            qrScan = new IntentIntegrator(this);
            qrScan.setOrientationLocked(false);
            qrScan.setCaptureActivity(CaptureActivityPortrait.class);



        } catch (Exception ex) {
            DialogUtils.showAlertDialog(this, "Error while loading form controls");
            return;
        }




    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @SuppressLint("RestrictedApi")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();


        switch (id) {
            case R.id.action_logout: {
                logoutUtil.doLogout();
            }
            break;


            case R.id.action_about: {
                FragmentUtils.replaceFragmentWithBackStack(this, R.id.container_body, new AboutFragment());
            }
            break;

            case R.id.home: {
                FragmentUtils.replaceFragmentWithBackStack(this, R.id.container_body, new HomeFragment());
            }
            break;

            case R.id.rescan: {
                qrScan.initiateScan();
            }
            break;

        }


        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {

            if (result.getContents() == null) {
                Toast.makeText(this, "Result Not Found", Toast.LENGTH_LONG).show();
            } else {
                ProcessScan(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    @Override
    public void onDrawerItemSelected(View view, int position, NavDrawerItem menuItem) {
        displayView(position, menuItem);
    }


    private void displayView(int position, NavDrawerItem menuItem) {

        Fragment fragment = null;
        String title = getString(R.string.app_name);

        switch (menuItem.getTitle()) {
            case "Home":
                fragment = new HomeFragment();
                title = "Home";
                break;

            case "Receiving":
                fragment = new UnloadingFragment();
                title = "Receiving";
                break;

            case "Putaway": {
                fragment = new PutawayPalletTransfersFragment();
                title = "Putaway";
            }
            break;

            case "Picking": {
                fragment = new GskPickingHeaderFragment();
                title = "Picking";
            }
            break;

            case "Sampling Pick": {
                fragment = new PickingHeaderFragment();
                title = "Sampling Pick";
            }
            break;

            case "VLPD Picking": {
                fragment = new VLPDPickingHeaderFragment();
                title = "OBD Picking";
            }
            break;



            case "Cycle Count": {
                fragment = new CcHeaderFragment();
                title = "Cycle Count";
            }
            break;

            case "Bin to Bin": {
                fragment = new InternalTransferFragment();
                title = "Transfers";
            }
            break;

            case "Pallet Transfers": {
                fragment = new PalletTransfersFragment();
                title = "Pallet Transfers";
            }
            break;

            case "Live Stock": {
                fragment = new LiveStockFragment();
                title = "Live Stock";
            }
            break;

            case "Packing": {
                fragment = new PackingFragment();
                title = "Packing";
            }
            break;

            case "Sorting": {
                fragment = new SortingFragment();
                title = "Packing";
            }
            break;

            case "Loading": {
                fragment = new NewLoadSheetFragment();
                title = "Loading";
            }
            break;

           case "Load Generation": {
                fragment = new LoadGenerationFragment();
                title = "Load Generation";

           }
           break;

            case "Delete OBD Picked Items": {
                fragment = new DeleteOBDPickedItemsFragment();
                title = "Delete OBD";
            }
            break;

            case "Delete VLPD Picked Items": {
                fragment = new DeleteVLPDPickedItemsFragment();
                title = "Delete OBD";
            }
            break;

            case "Flagged Bins": {
                fragment = new HeaderFlaggedBinsFragment();
                title = "Flagged Bins";
            }
            break;

            default:
                break;

        }

        if (fragment != null) {
            fragmentUtils.replaceFragmentWithBackStack(this, R.id.container_body, fragment);
            // set the toolbar title
            getSupportActionBar().setTitle(title);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }











    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {
        super.onStop();

    }


    @Override
    public void doLogout() {
        logoutUtil.doLogout();
    }







}
