package com.inventrax.tvs.fragments;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.cipherlab.barcode.GeneralString;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.honeywell.aidc.AidcManager;
import com.honeywell.aidc.BarcodeFailureEvent;
import com.honeywell.aidc.BarcodeReadEvent;
import com.honeywell.aidc.BarcodeReader;
import com.honeywell.aidc.ScannerUnavailableException;
import com.honeywell.aidc.TriggerStateChangeEvent;
import com.honeywell.aidc.UnsupportedPropertyException;
import com.inventrax.tvs.R;
import com.inventrax.tvs.activities.MainActivity;
import com.inventrax.tvs.common.Common;
import com.inventrax.tvs.common.constants.EndpointConstants;
import com.inventrax.tvs.common.constants.ErrorMessages;
import com.inventrax.tvs.interfaces.ApiInterface;
import com.inventrax.tvs.pojos.ScanDTO;
import com.inventrax.tvs.pojos.StockTake;
import com.inventrax.tvs.pojos.StockTakeDetails;
import com.inventrax.tvs.pojos.WMSCoreMessage;
import com.inventrax.tvs.pojos.WMSExceptionMessage;
import com.inventrax.tvs.room.AppDatabase;
import com.inventrax.tvs.room.RoomAppDatabase;
import com.inventrax.tvs.room.StockTakeDAO;
import com.inventrax.tvs.room.StockTakeTable;
import com.inventrax.tvs.services.RetrofitBuilderHttpsEx;
import com.inventrax.tvs.util.DecimalDigitsInputFilter;
import com.inventrax.tvs.util.DialogUtils;
import com.inventrax.tvs.util.ExceptionLoggerUtils;
import com.inventrax.tvs.util.FragmentUtils;
import com.inventrax.tvs.util.ProgressDialogUtils;
import com.inventrax.tvs.util.ScanValidator;
import com.inventrax.tvs.util.SoundUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Padmaja Rani.B on 19/12/2018
 */

public class StockTakeFragment extends Fragment implements View.OnClickListener, BarcodeReader.TriggerListener, BarcodeReader.BarcodeListener {

    private static final String classCode = "API_FRAG_StockTake";
    private View rootView;
    private CardView cvScanPallet, cvScanSku, cvScanDock;
    private ImageView ivScanPallet, ivScanSku, ivScanDock;
    private EditText etBin, etCarton;
    private TextView lblScannedSku;

    private Button btnClear, btnSubmit;
    DialogUtils dialogUtils;
    FragmentUtils fragmentUtils;
    private Common common = null;
    String scanner = null;
    String getScanner = null;
    private IntentFilter filter;
    private ScanValidator scanValidator;
    private Gson gson;
    private WMSCoreMessage core;
    private String Materialcode = null;
    private String userId = null, scanType = null, accountId = null;
    int warehouseID = 0, tenantID = 0;

    SoundUtils sound = null;
    private ExceptionLoggerUtils exceptionLoggerUtils;
    private ErrorMessages errorMessages;
    private boolean isLocation = false, isContanierScanned = false, isRsnScanned = false;
    SoundUtils soundUtils;

    StockTakeDAO db;
    AppDatabase dataBase;

    //For Honey well barcode
    private static BarcodeReader barcodeReader;
    private AidcManager manager;

    public AppCompatButton btn_ok;
    public EditText et_qty;


    private final BroadcastReceiver myDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scanner = intent.getStringExtra(GeneralString.BcReaderData);  // Scanned Barcode info
            ProcessScannedinfo(scanner.trim().toString());
        }
    };
    // Zebra scanner integration
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals("com.inventrax.vesuvius.SCAN")){
                try {
                    final String decodedData = intent.getStringExtra("com.symbol.datawedge.data_string");
                    ProcessScannedinfo(decodedData.toString().trim());
                    Log.d("Decoded data = ", decodedData);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

    };

    public void myScannedData(Context context, String barcode) {
        try {
            ProcessScannedinfo(barcode.trim());
        } catch (Exception e) {
            //  Toast.makeText(context, ""+e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public StockTakeFragment() {
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_stocktake, container, false);
        barcodeReader = MainActivity.getBarcodeObject();
        loadFormControls();
        return rootView;

    }

    // Form controls
    private void loadFormControls() {

        cvScanPallet = (CardView) rootView.findViewById(R.id.cvScanPallet);
        cvScanSku = (CardView) rootView.findViewById(R.id.cvScanSku);
        cvScanDock = (CardView) rootView.findViewById(R.id.cvScanDock);

        ivScanPallet = (ImageView) rootView.findViewById(R.id.ivScanPallet);
        ivScanSku = (ImageView) rootView.findViewById(R.id.ivScanSku);
        ivScanDock = (ImageView) rootView.findViewById(R.id.ivScanBin);

        btnSubmit = (Button) rootView.findViewById(R.id.btnSubmit);

        etBin = (EditText) rootView.findViewById(R.id.etBin);
        etCarton = (EditText) rootView.findViewById(R.id.etCarton);

        lblScannedSku = (TextView) rootView.findViewById(R.id.lblScannedSku);


        /*etQty.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    MainActivity mainActivity=(MainActivity)getActivity();
                    mainActivity.barcode="";
                    return  true;
                }
                return false;
            }
        });*/

        dataBase = new RoomAppDatabase(getActivity()).getAppDatabase();

        db = dataBase.getStockTakeDAO();

        //db.deleteAll();

        SharedPreferences sp = getActivity().getSharedPreferences("LoginActivity", Context.MODE_PRIVATE);
        userId = sp.getString("RefUserId", "");
        scanType = sp.getString("scanType", "");
        accountId = sp.getString("AccountId", "");
        warehouseID = sp.getInt("WarehouseID", 0);
        tenantID = sp.getInt("TenantID", 0);

        //btnClear.setOnClickListener(this);
        btnSubmit.setOnClickListener(this);
        cvScanPallet.setOnClickListener(this);

        exceptionLoggerUtils = new ExceptionLoggerUtils();
        sound = new SoundUtils();
        common = new Common();
        errorMessages = new ErrorMessages();
        gson = new GsonBuilder().create();
        core = new WMSCoreMessage();
        soundUtils = new SoundUtils();
        ProgressDialogUtils.closeProgressDialog();
        common.setIsPopupActive(false);

        // For Cipher Barcode reader
        Intent RTintent = new Intent("sw.reader.decode.require");
        RTintent.putExtra("Enable", true);
        getActivity().sendBroadcast(RTintent);
        this.filter = new IntentFilter();
        this.filter.addAction("sw.reader.decode.complete");
        getActivity().registerReceiver(this.myDataReceiver, this.filter);

        // Zebra Scanner in
        IntentFilter filter = new IntentFilter();
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        filter.addAction("com.inventrax.vesuvius.SCAN");
        getActivity().registerReceiver(receiver, filter);

        //For Honey well
        AidcManager.create(getActivity(), new AidcManager.CreatedCallback() {

            @Override
            public void onCreated(AidcManager aidcManager) {

                manager = aidcManager;
                barcodeReader = manager.createBarcodeReader();
                try {
                    barcodeReader.claim();
                    HoneyWellBarcodeListeners();

                } catch (ScannerUnavailableException e) {
                    e.printStackTrace();
                }
            }
        });


    }

    //button Clicks
    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.cvScanPallet:
                isContanierScanned = true;
                cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.white));
                ivScanPallet.setImageResource(R.drawable.check);
                etCarton.setText("");
                break;

            case R.id.btnSubmit:
                List<StockTakeTable> insertedRecords = db.getAll();
                if (insertedRecords != null && insertedRecords.size() > 0) {
                    StockTakeDetails stockTakeDetails = null;
                    List<StockTakeDetails> stockTakeDetailsList = new ArrayList<>();
                    for (StockTakeTable record : insertedRecords) {
                        stockTakeDetails = new StockTakeDetails();
                        stockTakeDetails.setCartonCode(record.carton);
                        stockTakeDetails.setLocationCode(record.bin);
                        stockTakeDetails.setQuantity(record.qty);
                        stockTakeDetails.setMaterialCode(record.sku);
                        stockTakeDetailsList.add(stockTakeDetails);
                    }
                    SubmitData(stockTakeDetailsList);

                }else {
                    ProgressDialogUtils.closeProgressDialog();
                    DialogUtils.showAlertDialog(getActivity(), "No records found");
                    soundUtils.alertError(getActivity(),getContext());
                }

                break;

            default:
                break;
        }
    }

    public void SubmitData(List<StockTakeDetails> list) {

        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.StockTakeDTO, getContext());

            StockTake stockTake = new StockTake();
            stockTake.setStockTakeDetails(list);
            message.setEntityObject(stockTake);


            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                call = apiService.UpsertStockTake(message);
                ProgressDialogUtils.showProgressDialog("Please Wait");
                // } else {
                // DialogUtils.showAlertDialog(getActivity(), "Please enable internet");
                // return;
                // }

            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "002_01", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0002);
            }
            try {
                //Getting response from the method
                call.enqueue(new Callback<String>() {

                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        ProgressDialogUtils.closeProgressDialog();
                        if (response.body() != null) {
                            if (response.body().equals("1")) {
                                clearFields();
                                common.showUserDefinedAlertType(getString(R.string.successfully_posted), getActivity(), getContext(), "Success");
                            } else {
                                common.showUserDefinedAlertType(getString(R.string.something_went_wrong_please_submit_again), getActivity(), getContext(), "Error");
                            }
                        } else {
                            common.showUserDefinedAlertType(getString(R.string.something_went_wrong_please_submit_again), getActivity(), getContext(), "Error");
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable throwable) {
                        //Toast.makeText(LoginActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
                        ProgressDialogUtils.closeProgressDialog();
                        DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0001);
                    }
                });
            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "002_02", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0001);
            }
        } catch (Exception ex) {
            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "002_03", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0002);
        }
    }


    public void clearFields() {

        db.deleteAll();

        cvScanSku.setCardBackgroundColor(getResources().getColor(R.color.black));
        ivScanSku.setImageResource(R.drawable.fullscreen_img);

        cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.secondarycolor));
        ivScanPallet.setImageResource(R.drawable.fullscreen_img);

        cvScanDock.setCardBackgroundColor(getResources().getColor(R.color.primarycolor));
        ivScanDock.setImageResource(R.drawable.fullscreen_img);

        etBin.setText("");
        etCarton.setText("");
        lblScannedSku.setText("");


    }


    @Override
    public void onBarcodeEvent(final BarcodeReadEvent barcodeReadEvent) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                //update UI to reflect the data
                //List<String> list = new ArrayList<String>();
                //list.add("Barcode data: " + barcodeReadEvent.getBarcodeData());

                getScanner = barcodeReadEvent.getBarcodeData();
               ProcessScannedinfo(getScanner.trim().toString());


            }

        });
    }

    @Override
    public void onFailureEvent(BarcodeFailureEvent barcodeFailureEvent) {

    }

    @Override
    public void onTriggerEvent(TriggerStateChangeEvent triggerStateChangeEvent) {

    }

    public void HoneyWellBarcodeListeners() {

        barcodeReader.addTriggerListener(this);

        if (barcodeReader != null) {
            // set the trigger mode to client control
            barcodeReader.addBarcodeListener(this);
            try {
                barcodeReader.setProperty(BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE, BarcodeReader.TRIGGER_CONTROL_MODE_AUTO_CONTROL);
            } catch (UnsupportedPropertyException e) {
                // Toast.makeText(this, "Failed to apply properties", Toast.LENGTH_SHORT).show();
            }

            Map<String, Object> properties = new HashMap<String, Object>();
            // Set Symbologies On/Off
            properties.put(BarcodeReader.PROPERTY_CODE_128_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_GS1_128_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_QR_CODE_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_CODE_39_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_DATAMATRIX_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_UPC_A_ENABLE, true);
            properties.put(BarcodeReader.PROPERTY_EAN_13_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_AZTEC_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_CODABAR_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_INTERLEAVED_25_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_PDF_417_ENABLED, true);
            // Set Max Code 39 barcode length
            properties.put(BarcodeReader.PROPERTY_CODE_39_MAXIMUM_LENGTH, 10);
            // Turn on center decoding
            properties.put(BarcodeReader.PROPERTY_CENTER_DECODE, true);
            // Enable bad read response
            properties.put(BarcodeReader.PROPERTY_NOTIFICATION_BAD_READ_ENABLED, true);
            // Apply the settings
            barcodeReader.setProperties(properties);
        }

    }


    //Assigning scanned value to the respective fields
    public void ProcessScannedinfo(String scannedData) {

        if (ProgressDialogUtils.isProgressActive() || Common.isPopupActive()) {
            common.showUserDefinedAlertType(errorMessages.EMC_082, getActivity(), getContext(), "Warning");
            return;
        }

        if (scannedData != null && !Common.isPopupActive()) {

            if (!ProgressDialogUtils.isProgressActive()) {

                if (etBin.getText().toString().isEmpty()) {
                    ValidateLocation(scannedData);


                }
                else if (!isContanierScanned) {
                    if (etBin.getText().toString().isEmpty()) {
                        common.showUserDefinedAlertType("Please scan Bin", getActivity(), getContext(), "Error");
                        return;
                    }
                     isContanierScanned=true;
                    etCarton.setText(scannedData);
                    cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.white));
                    ivScanPallet.setImageResource(R.drawable.check);
                //    ValidatePallet(scannedData);
                   /* etCarton.setText(scannedData);
                    cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.white));
                    ivScanPallet.setImageResource(R.drawable.check);*/
                } else {
                    if (etBin.getText().toString().isEmpty()) {
                        common.showUserDefinedAlertType(errorMessages.EMC_084, getActivity(), getContext(), "Error");
                        return;
                    }
                   /* if (etCarton.getText().toString().isEmpty()) {
                        common.showUserDefinedAlertType(errorMessages.EMC_085, getActivity(), getContext(), "Error");
                        return;
                    }*/
                 String SplitSKU = scannedData.split("[|]")[0];
                    lblScannedSku.setText(SplitSKU);
                    cvScanSku.setCardBackgroundColor(getResources().getColor(R.color.white));
                    ivScanSku.setImageResource(R.drawable.check);

                  //  db.insert(new StockTakeTable(etBin.getText().toString(), etCarton.getText().toString(), lblScannedSku.getText().toString(),"1"));

                    StockTakeTable previousRecord = db.getPreviousRecord(etBin.getText().toString(), etCarton.getText().toString(), lblScannedSku.getText().toString());
                    if (previousRecord != null) {
                        showDialog(getActivity(), previousRecord.qty, true);
                    } else {
                        showDialog(getActivity(), "", false);
                    }


                }

            } else {
                if (!Common.isPopupActive()) {
                    common.showUserDefinedAlertType(errorMessages.EMC_080, getActivity(), getContext(), "Error");
                }
                sound.alertWarning(getActivity(), getContext());
            }

        }
    }
    public void showDialog(Context mContext, String qty, final boolean isPreviousRecord) {
        AlertDialog alertDialog;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialog = inflater.inflate(R.layout.alert_dialog, null);
        et_qty = (EditText) dialog.findViewById(R.id.et_qty);
        et_qty.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(20, 2)});
        et_qty.setText(qty);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(dialog);
        builder.setCancelable(false);
        builder.setTitle(getResources().getString(R.string.enter_qty));

        // Set the "OK" button with a dummy listener, which will be overridden later
        builder.setPositiveButton(getResources().getString(R.string.ok), null);

        // Set the "Cancel" button to close the dialog
        builder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        alertDialog = builder.create();
        alertDialog.show();

        // Get the positive button and set the actual click listener
        Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!et_qty.getText().toString().isEmpty()) {
                    if (!et_qty.getText().toString().equals("0")) {
                        if (isPreviousRecord) {
                            db.update(etBin.getText().toString(), etCarton.getText().toString(), lblScannedSku.getText().toString(), et_qty.getText().toString());
                        } else {
                            db.insert(new StockTakeTable(etBin.getText().toString(), etCarton.getText().toString(), lblScannedSku.getText().toString(), et_qty.getText().toString()));
                        }
                        Toast.makeText(getActivity(), getString(R.string.record_added_successfully), Toast.LENGTH_SHORT).show();
                        alertDialog.dismiss();
                    } else {
                        common.showUserDefinedAlertType(getString(R.string.quantity_should_not_0), getActivity(), getContext(), "Error");
                    }
                } else {
                    common.showUserDefinedAlertType(getString(R.string.please_enter_quantity_before_proceeding), getActivity(), getContext(), "Error");
                }
            }
        });

        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

/*
    public void showDialog(Context mContext, String qty, final boolean isPreviousRecord) {
        AlertDialog alertDialog;
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialog = inflater.inflate(R.layout.alert_dialog, null);
      //  btn_ok = (AppCompatButton) dialog.findViewById(R.id.btn_ok);
        et_qty = (EditText) dialog.findViewById(R.id.et_qty);
        et_qty.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(20, 2)});
        et_qty.setText(qty);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(dialog);
        builder.setCancelable(false);
        builder.setTitle(getResources().getString(R.string.enter_qty));

        builder.setNegativeButton(getResources().getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        if (!et_qty.getText().toString().isEmpty()) {
                            if (!et_qty.getText().toString().equals("0")) {

                                if (isPreviousRecord) {
                                    db.update(etBin.getText().toString(), etCarton.getText().toString(), lblScannedSku.getText().toString(), et_qty.getText().toString());
                                } else {
                                    db.insert(new StockTakeTable(etBin.getText().toString(), etCarton.getText().toString(), lblScannedSku.getText().toString(), et_qty.getText().toString()));
                                }

                                Toast.makeText(getActivity(), getString(R.string.record_added_successfully), Toast.LENGTH_SHORT).show();
                        */
/*if (db.getAll() != null) {
                            List<StockTakeTable> tables = db.getAll();
                            if (tables.size() > 0) {
                                //Toast.makeText(getActivity(), gson.toJson(tables).toString(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getActivity(), "No records found", Toast.LENGTH_SHORT).show();
                        }*//*

                            } else {
                                common.showUserDefinedAlertType(getString(R.string.quantity_should_not_0), getActivity(), getContext(), "Error");
                            }
                        } else {
                            common.showUserDefinedAlertType(getString(R.string.please_enter_quantity_before_proceeding), getActivity(), getContext(), "Error");
                        }


                      //  builder.setCancelable(true);

                    }
                });
 */
/*       btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!et_qty.getText().toString().isEmpty()) {
                    if (!et_qty.getText().toString().equals("0")) {

                        if (isPreviousRecord) {
                            db.update(etBin.getText().toString(), etCarton.getText().toString(), lblScannedSku.getText().toString(), et_qty.getText().toString());
                        } else {
                            db.insert(new StockTakeTable(etBin.getText().toString(), etCarton.getText().toString(), lblScannedSku.getText().toString(), et_qty.getText().toString()));
                        }


                        Toast.makeText(getActivity(), "Record added successfully", Toast.LENGTH_SHORT).show();
                        *//*
     */
/*if (db.getAll() != null) {
                            List<StockTakeTable> tables = db.getAll();
                            if (tables.size() > 0) {
                                //Toast.makeText(getActivity(), gson.toJson(tables).toString(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getActivity(), "No records found", Toast.LENGTH_SHORT).show();
                        }*//*
     */
/*
                    } else {
                        common.showUserDefinedAlertType("Quantity should not 0 ", getActivity(), getContext(), "Error");
                    }
                } else {
                    common.showUserDefinedAlertType("Please enter quantity before proceeding", getActivity(), getContext(), "Error");
                }


            }
        });*//*



         alertDialog = builder.create();

        alertDialog.show();

        Window window = alertDialog.getWindow();
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

    }
*/


    // sending exception to the database
    public void logException() {
        try {

            String textFromFile = exceptionLoggerUtils.readFromFile(getActivity());

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.Exception, getActivity());
            WMSExceptionMessage wmsExceptionMessage = new WMSExceptionMessage();
            wmsExceptionMessage.setWMSMessage(textFromFile);
            message.setEntityObject(wmsExceptionMessage);

            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                call = apiService.LogException(message);
                // } else {
                // DialogUtils.showAlertDialog(getActivity(), "Please enable internet");
                // return;
                // }

            } catch (Exception ex) {
                ProgressDialogUtils.closeProgressDialog();
            }
            try {
                //Getting response from the method
                call.enqueue(new Callback<String>() {

                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {

                        try {

                            core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);


                        } catch (Exception ex) {

                            /*try {
                                exceptionLoggerUtils.createExceptionLog(ex.toString(),classCode,"002",getContext());

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            logException();*/


                            ProgressDialogUtils.closeProgressDialog();
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable throwable) {
                        ProgressDialogUtils.closeProgressDialog();
                        //Toast.makeText(LoginActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
                        DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0001);
                    }
                });
            } catch (Exception ex) {
                ProgressDialogUtils.closeProgressDialog();
                // Toast.makeText(LoginActivity.this, ex.toString(), Toast.LENGTH_LONG).show();
            }
        } catch (Exception ex) {
            ProgressDialogUtils.closeProgressDialog();
            DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0003);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (barcodeReader != null) {
            // release the scanner claim so we don't get any scanner
            try {
                barcodeReader.claim();
            } catch (ScannerUnavailableException e) {
                e.printStackTrace();
            }
            // notifications while paused.
            barcodeReader.release();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (barcodeReader != null) {
            try {
                barcodeReader.claim();
            } catch (ScannerUnavailableException e) {
                e.printStackTrace();
                // Toast.makeText(this, "Scanner unavailable", Toast.LENGTH_SHORT).show();
            }
        }
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.stock_take_Title));
    }

    //Barcode scanner API
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (barcodeReader != null) {
            // unregister barcode event listener honeywell
            barcodeReader.removeBarcodeListener((BarcodeReader.BarcodeListener) this);

            // unregister trigger state change listener
            barcodeReader.removeTriggerListener((BarcodeReader.TriggerListener) this);
        }

        Intent RTintent = new Intent("sw.reader.decode.require");
        RTintent.putExtra("Enable", false);
        getActivity().sendBroadcast(RTintent);
        getActivity().unregisterReceiver(this.myDataReceiver);


        // Zebra code
        Intent intent = new Intent();
        intent.setAction("com.zebra.intent.action.GET_ACTIVE_MODIFIER");
        intent.setPackage("com.zebra.keyeventservice");
        getActivity().sendBroadcast(intent);
        getActivity().unregisterReceiver(this.receiver);
        super.onDestroyView();    }

    public void ValidateLocation(final String scannedData) {
        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.ScanDTO, getContext());
            ScanDTO scanDTO = new ScanDTO();
            scanDTO.setUserID(userId);
            scanDTO.setAccountID(accountId);
            // scanDTO.setTenantID(String.valueOf(tenantID));
            scanDTO.setWarehouseID(String.valueOf(warehouseID));
            scanDTO.setScanInput(scannedData);
            // scanDTO.setInboundID(inboundId);
            //inboundDTO.setIsOutbound("0");
            message.setEntityObject(scanDTO);


            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);
            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                call = apiService.ValidateLocation(message);
                ProgressDialogUtils.showProgressDialog("Please Wait");
                // } else {
                // DialogUtils.showAlertDialog(getActivity(), "Please enable internet");
                // return;
                // }

            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "002_01", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0002);
            }
            try {
                //Getting response from the method
                call.enqueue(new Callback<String>() {

                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);

                        if ((core.getType().toString().equals("Exception"))) {
                            List<LinkedTreeMap<?, ?>> _lExceptions = new ArrayList<LinkedTreeMap<?, ?>>();
                            _lExceptions = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                            WMSExceptionMessage owmsExceptionMessage = null;
                            for (int i = 0; i < _lExceptions.size(); i++) {

                                owmsExceptionMessage = new WMSExceptionMessage(_lExceptions.get(i).entrySet());
                            }
                            etBin.setText("");
                            cvScanDock.setCardBackgroundColor(getResources().getColor(R.color.white));
                            ivScanDock.setImageResource(R.drawable.warning_img);


                            ProgressDialogUtils.closeProgressDialog();
                            common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                        } else {
                            LinkedTreeMap<?, ?> _lResult = new LinkedTreeMap<>();
                            _lResult = (LinkedTreeMap<?, ?>) core.getEntityObject();

                            ScanDTO scanDTO1 = new ScanDTO(_lResult.entrySet());

                            if (scanDTO1 != null) {
                                if (scanDTO1.getScanResult()) {
                                    etBin.setText(scannedData);
                                    cvScanDock.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanDock.setImageResource(R.drawable.check);


                                } else {

                                    etBin.setText("");
                                    cvScanDock.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanDock.setImageResource(R.drawable.warning_img);

                                    common.showUserDefinedAlertType(errorMessages.EMC_0016, getActivity(), getContext(), "Warning");
/*                                    etLocationTo.setText("");
                                    cvScanToLoc.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanToLoc.setImageResource(R.drawable.warning_img);
                                    common.showUserDefinedAlertType(errorMessages.EMC_0010, getActivity(), getContext(), "Warning");*/
                                }
                            } else {
                                common.showUserDefinedAlertType("Error while getting data", getActivity(), getContext(), "Error");
                            }
                            ProgressDialogUtils.closeProgressDialog();
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable throwable) {
                        //Toast.makeText(LoginActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
                        ProgressDialogUtils.closeProgressDialog();
                        DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0001);
                    }
                });
            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "002_02", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0001);
            }
        } catch (Exception ex) {
            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "002_03", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0002);
        }
    }


    public void ValidatePallet(final String scannedData) {
        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.ScanDTO, getContext());
            ScanDTO scanDTO = new ScanDTO();
            scanDTO.setUserID(userId);
            scanDTO.setAccountID(accountId);
            // scanDTO.setTenantID(String.valueOf(tenantID));
            scanDTO.setWarehouseID(String.valueOf(warehouseID));
            scanDTO.setScanInput(scannedData);
            // scanDTO.setInboundID(inboundId);
            //inboundDTO.setIsOutbound("0");
            message.setEntityObject(scanDTO);


            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                call = apiService.ValidatePallet(message);
                ProgressDialogUtils.showProgressDialog("Please Wait");
                // } else {
                // DialogUtils.showAlertDialog(getActivity(), "Please enable internet");
                // return;
                // }

            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "002_01", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0002);
            }
            try {
                //Getting response from the method
                call.enqueue(new Callback<String>() {

                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);

                        if ((core.getType().toString().equals("Exception"))) {
                            List<LinkedTreeMap<?, ?>> _lExceptions = new ArrayList<LinkedTreeMap<?, ?>>();
                            _lExceptions = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                            WMSExceptionMessage owmsExceptionMessage = null;
                            for (int i = 0; i < _lExceptions.size(); i++) {
                                owmsExceptionMessage = new WMSExceptionMessage(_lExceptions.get(i).entrySet());
                            }
                            /*etBin.setText("");
                            cvScanDock.setCardBackgroundColor(getResources().getColor(R.color.white));
                            ivScanDock.setImageResource(R.drawable.check);*/
                            etCarton.setText("");
                            cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.white));
                            ivScanPallet.setImageResource(R.drawable.warning_img);
                            ProgressDialogUtils.closeProgressDialog();
                            common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                        } else {
                            LinkedTreeMap<?, ?> _lResult = new LinkedTreeMap<>();
                            _lResult = (LinkedTreeMap<?, ?>) core.getEntityObject();

                            ScanDTO scanDTO1 = new ScanDTO(_lResult.entrySet());
                            ProgressDialogUtils.closeProgressDialog();
                            if (scanDTO1 != null) {

                                if (scanDTO1.getScanResult()) {



                                    etCarton.setText(scannedData);
                                    cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanPallet.setImageResource(R.drawable.check);


                                } else {

                                    etCarton.setText("");
                                    cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanPallet.setImageResource(R.drawable.warning_img);

                                    common.showUserDefinedAlertType(errorMessages.EMC_0016, getActivity(), getContext(), "Warning");
/*                                    etLocationTo.setText("");
                                    cvScanToLoc.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanToLoc.setImageResource(R.drawable.warning_img);
                                    common.showUserDefinedAlertType(errorMessages.EMC_0010, getActivity(), getContext(), "Warning");*/
                                }
                                }
                            }

                        }


                    @Override
                    public void onFailure(Call<String> call, Throwable throwable) {
                        //Toast.makeText(LoginActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
                        ProgressDialogUtils.closeProgressDialog();
                        DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0001);
                    }
                });
            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "002_02", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0001);
            }
        } catch (Exception ex) {
            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "002_03", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0002);
        }
    }

}