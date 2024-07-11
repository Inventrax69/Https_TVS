package com.inventrax.jungheinrich.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.text.InputFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
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
import com.inventrax.jungheinrich.R;
import com.inventrax.jungheinrich.activities.MainActivity;
import com.inventrax.jungheinrich.common.Common;
import com.inventrax.jungheinrich.common.constants.EndpointConstants;
import com.inventrax.jungheinrich.common.constants.ErrorMessages;
import com.inventrax.jungheinrich.interfaces.ApiInterface;
import com.inventrax.jungheinrich.pojos.InboundDTO;
import com.inventrax.jungheinrich.pojos.OutbountDTO;
import com.inventrax.jungheinrich.pojos.ScanDTO;
import com.inventrax.jungheinrich.pojos.WMSCoreMessage;
import com.inventrax.jungheinrich.pojos.WMSExceptionMessage;
import com.inventrax.jungheinrich.searchableSpinner.SearchableSpinner;
import com.inventrax.jungheinrich.services.RetrofitBuilderHttpsEx;
import com.inventrax.jungheinrich.util.DecimalDigitsInputFilter;
import com.inventrax.jungheinrich.util.DialogUtils;
import com.inventrax.jungheinrich.util.ExceptionLoggerUtils;
import com.inventrax.jungheinrich.util.FragmentUtils;
import com.inventrax.jungheinrich.util.ProgressDialogUtils;
import com.inventrax.jungheinrich.util.ScanValidator;
import com.inventrax.jungheinrich.util.SoundUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.annotation.Nullable;
/**
 * Created by Prasanna ch on 06/26/2018.
 */

public class GskPickingDetailsFragment extends Fragment implements View.OnClickListener, BarcodeReader.TriggerListener, BarcodeReader.BarcodeListener, AdapterView.OnItemSelectedListener {

    private static final String classCode = "API_FRAG_OBD_PICKING";
    private View rootView;
    ImageView ivScanLocation, ivScanPallet, ivScanPalletTo, tvScanRSN, ivScanRSN, ivScanRSNnew;
    Button btnMaterialSkip, btnPick, btn_Skip, btnOk, btnCloseSkip, btnClosefinal,btnClose;
    TextView lblPickListNo, lblScannedSku;
    TextView lblSKuNo, lblLocationSuggested, lblMRP, lblrsnNoNew, lblMfgDate, lblExpDate, lblProjectRefNo, lblassignedQty, lblserialNo, lblBatchNo;
    CardView cvScanPallet, cvScanPalletTo, cvScanRSN, cvScanNewRSN, cvScanLocation;
    EditText lblReceivedQty;
    boolean IsStrictlycomplaince = false;
    String Mcode = null, NewMcode = null;
    String scanner = null;
    String getScanner = null;
    private IntentFilter filter;
    private Gson gson;
    String userId = null, scanType = null;
    private Common common;
    private WMSCoreMessage core;
    private String pickOBDno = "", pickobdId = "";
    int count = 0;
    private ScanValidator scanValidator;
    private ExceptionLoggerUtils exceptionLoggerUtils;
    private ErrorMessages errorMessages;
    EditText etPallet, etPalletTo;
    EditText et_oldrsn, et_printQty, et_newrsn, et_printerIP;
    boolean isValidLocation = false;
    boolean isPalletScanned = false;
    boolean isToPalletScanned = false;
    boolean pickValidateComplete = false;
    boolean isRSNScanned = false;
    String assignedId = "", KitId = "", soDetailsId = "", Lineno = "", POSOHeaderId = "", sLoc = "",accountId = "";
    double recQty, totalQty;
    //For Honey well barcode
    private static BarcodeReader barcodeReader;
    private AidcManager manager;
    RelativeLayout rlPickList, rlSkip;
    String printer = "Select Printer", skipReason = "", pickedQty = "", location = "";
    List<String> deviceIPList;
    SearchableSpinner spinnerSelectReason;
    SoundUtils soundUtils;

    // Cipher Barcode Scanner
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
    public GskPickingDetailsFragment() {

    }

    public void myScannedData(Context context, String barcode){
        try {
            ProcessScannedinfo(barcode.trim());
        }catch (Exception e){
            //  Toast.makeText(context, ""+e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_gsk_picking_details, container, false);
        barcodeReader = MainActivity.getBarcodeObject();
        loadFormControls();
        return rootView;
    }

    private void loadFormControls() {

        rlPickList = (RelativeLayout) rootView.findViewById(R.id.rlPickList);
        rlSkip = (RelativeLayout) rootView.findViewById(R.id.rlSkip);
        cvScanLocation = (CardView) rootView.findViewById(R.id.cvScanLocation);
        cvScanPallet = (CardView) rootView.findViewById(R.id.cvScanPallet);
        cvScanRSN = (CardView) rootView.findViewById(R.id.cvScanRSN);
        cvScanNewRSN = (CardView) rootView.findViewById(R.id.cvScanNewRSN);
        cvScanPalletTo = (CardView) rootView.findViewById(R.id.cvScanPalletTo);
        ivScanLocation = (ImageView) rootView.findViewById(R.id.ivScanLocation);
        ivScanPallet = (ImageView) rootView.findViewById(R.id.ivScanPallet);
        ivScanRSN = (ImageView) rootView.findViewById(R.id.ivScanRSN);
        ivScanPalletTo = (ImageView) rootView.findViewById(R.id.ivScanPalletTo);

        btnPick = (Button) rootView.findViewById(R.id.btnPick);
        btn_Skip = (Button) rootView.findViewById(R.id.btn_Skip);
        btnOk = (Button) rootView.findViewById(R.id.btnOk);
        btnCloseSkip = (Button) rootView.findViewById(R.id.btnCloseSkip);
        btnClose = (Button) rootView.findViewById(R.id.btnClose);

        lblPickListNo = (TextView) rootView.findViewById(R.id.lblPickListNo);
        lblSKuNo = (TextView) rootView.findViewById(R.id.lblSKUSuggested);
        lblLocationSuggested = (TextView) rootView.findViewById(R.id.lblLocationSuggested);
        lblMRP = (TextView) rootView.findViewById(R.id.lblMRP);

        etPallet = (EditText) rootView.findViewById(R.id.etPallet);

        etPalletTo = (EditText) rootView.findViewById(R.id.etPalletTo);

        lblReceivedQty = (EditText) rootView.findViewById(R.id.lblReceivedQty);
        lblMfgDate = (TextView) rootView.findViewById(R.id.lblMfgDate);
        lblExpDate = (TextView) rootView.findViewById(R.id.lblExpDate);
        lblProjectRefNo = (TextView) rootView.findViewById(R.id.lblProjectRefNo);
        lblserialNo = (TextView) rootView.findViewById(R.id.lblserialNo);
        lblBatchNo = (TextView) rootView.findViewById(R.id.lblBatchNo);
        lblassignedQty = (TextView) rootView.findViewById(R.id.lblRequiredQty);
        spinnerSelectReason = (SearchableSpinner) rootView.findViewById(R.id.spinnerSelectReason);
        spinnerSelectReason.setOnItemSelectedListener(this);
        lblReceivedQty.clearFocus();
        lblReceivedQty.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    MainActivity mainActivity=(MainActivity)getActivity();
                    mainActivity.barcode="";
                    return  true;
                }
                return false;
            }
        });
        lblReceivedQty.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(10, 2)});
        SharedPreferences sp = getActivity().getSharedPreferences("LoginActivity", Context.MODE_PRIVATE);
        userId = sp.getString("RefUserId", "");
        scanType = sp.getString("scanType", "");
        accountId = sp.getString("AccountId", "");

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

        gson = new GsonBuilder().create();
        btnPick.setOnClickListener(this);
        btn_Skip.setOnClickListener(this);
        btnOk.setOnClickListener(this);
        btnCloseSkip.setOnClickListener(this);
        cvScanPallet.setOnClickListener(this);
        cvScanPalletTo.setOnClickListener(this);
        btnClose.setOnClickListener(this);

        common = new Common();
        exceptionLoggerUtils = new ExceptionLoggerUtils();
        errorMessages = new ErrorMessages();
        soundUtils = new SoundUtils();
        ProgressDialogUtils.closeProgressDialog();
        common.setIsPopupActive(false);

        pickOBDno = getArguments().getString("pickOBDno");
        pickobdId = getArguments().getString("pickobdId");
        lblPickListNo.setText(pickOBDno);

        if (scanType.equals("Auto")) {
            btnPick.setEnabled(false);
            btnPick.setTextColor(getResources().getColor(R.color.black));
            btnPick.setBackgroundResource(R.drawable.button_hide);
        } else {
      //      btnPick.setEnabled(false);
            btnPick.setTextColor(getResources().getColor(R.color.white));
            btnPick.setBackgroundResource(R.drawable.button_shape);
        }

        //For Honeywell
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


        GetPickItem();


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {


            case R.id.btnPick:
             /*   if (!lblLocationSuggested.getText().toString().isEmpty()) {

                    if (!lblReceivedQty.getText().toString().isEmpty()) {

                        double reqQty = totalQty - recQty;
                        double qty = Double.parseDouble(lblReceivedQty.getText().toString());
                        if (reqQty < qty) {
                            common.showUserDefinedAlertType(errorMessages.EMC_0068, getActivity(), getContext(), "Error");

                        } else {

                            cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
                            ivScanRSN.setImageResource(R.drawable.fullscreen_img);
                            if(isRSNScanned){
                                UpsertPickItem();
                            } else {
                                common.showUserDefinedAlertType(errorMessages.EMC_0028, getActivity(), getContext(), "Error");
                                return;
                            }
                            return;
                        }
                    }
                 else {
                    common.showUserDefinedAlertType(errorMessages.EMC_0068, getActivity(), getContext(), "Error");
                    return;
                }
                } else {
                    common.showUserDefinedAlertType(errorMessages.EMC_0013, getActivity(), getContext(), "Error");
                    return;
                }
                break;*/
            if (isValidLocation) {

                if (isPalletScanned){
                    if (isToPalletScanned) {

                        if (isRSNScanned){
                            if (!lblReceivedQty.getText().toString().isEmpty()) {
                                if (lblReceivedQty.getText().toString().equals(".")) {
                                    common.showUserDefinedAlertType(getString(R.string.please_enter_valid_qty), getActivity(), getContext(), "Error");
                                    return;
                                }
                                double reqQty = totalQty - recQty;
                                double qty = Double.parseDouble(lblReceivedQty.getText().toString());
                                if (reqQty < qty) {
                                    common.showUserDefinedAlertType(errorMessages.EMC_0068, getActivity(), getContext(), "Error");

                                } else {

                                    cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
                                    ivScanRSN.setImageResource(R.drawable.fullscreen_img);
                                    if (isRSNScanned) {
                                        UpsertPickItem();
                                    } else {
                                        common.showUserDefinedAlertType(errorMessages.EMC_0028, getActivity(), getContext(), "Error");

                                    }

                                }
                            }else {
                                common.showUserDefinedAlertType(errorMessages.EMC_0067, getActivity(), getContext(), "Error");

                            }
                        }else {
                            common.showUserDefinedAlertType(errorMessages.EMC_0028, getActivity(), getContext(), "Error");

                        }
                    }else {
                        common.showUserDefinedAlertType(errorMessages.EMC_087, getActivity(), getContext(), "Error");

                    }
                } else {
                    common.showUserDefinedAlertType(errorMessages.EMC_098, getActivity(), getContext(), "Error");

                }
            } else {
                common.showUserDefinedAlertType(errorMessages.EMC_0007, getActivity(), getContext(), "Error");

            }
            break;
            case R.id.btn_Skip:
                if (isValidLocation) {
                    SkipItem();
                } else {
                    common.showUserDefinedAlertType(errorMessages.EMC_0007, getActivity(), getContext(), "Error");
                    return;
                }

                break;
            case R.id.btnOk:

                DialogUtils.showConfirmDialog(getActivity(), "Confirm", getString(R.string.are_you_sure_to_skip_this_location), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                common.setIsPopupActive(false);

                                if (!skipReason.equals("")) {

                                    // To skip the item and regenerating suggestions
                                    OBDSkipItem();
                                } else {
                                    common.showUserDefinedAlertType(errorMessages.EMC_0056, getActivity(), getContext(), "Error");
                                    return;
                                }

                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                common.setIsPopupActive(false);
                                break;
                        }

                    }
                });

                break;
            case R.id.btnCloseSkip:
                rlPickList.setVisibility(View.VISIBLE);
                rlSkip.setVisibility(View.GONE);
                break;
            case R.id.cvScanPallet:
                isPalletScanned=true;
                cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.white));
                ivScanPallet.setImageResource(R.drawable.check);
                break;
            case R.id.cvScanPalletTo:
                isToPalletScanned=true;
                cvScanPalletTo.setCardBackgroundColor(getResources().getColor(R.color.white));
                ivScanPalletTo.setImageResource(R.drawable.check);
                break;
            case R.id.btnClose:
                GskPickingHeaderFragment gskPickingHeaderFragment=new GskPickingHeaderFragment();
                FragmentUtils.replaceFragmentWithBackStack(getActivity(),R.id.container_body,gskPickingHeaderFragment);
                break;

        }
    }


    @Override
    public void onBarcodeEvent(final BarcodeReadEvent barcodeReadEvent) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // update UI to reflect the data
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

    //Honeywell Barcode reader Properties
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
            properties.put(BarcodeReader.PROPERTY_EAN_13_ENABLED, false);
            properties.put(BarcodeReader.PROPERTY_AZTEC_ENABLED, false);
            properties.put(BarcodeReader.PROPERTY_CODABAR_ENABLED, false);
            properties.put(BarcodeReader.PROPERTY_INTERLEAVED_25_ENABLED, false);
            properties.put(BarcodeReader.PROPERTY_PDF_417_ENABLED, false);
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


    public void ClearFields() {

        lblSKuNo.setText("");
        etPallet.setText("");
        etPalletTo.setText("");

        lblassignedQty.setText("");
        lblBatchNo.setText("");
        lblReceivedQty.setText("");
        lblMfgDate.setText("");
        lblExpDate.setText("");
        lblProjectRefNo.setText("");
        lblserialNo.setText("");
        lblMRP.setText("");

        isPalletScanned = false;
        isValidLocation = false;
        isToPalletScanned = false;
        isRSNScanned = false;
        pickValidateComplete = false;

    }

    public void clearData() {
        lblPickListNo.setText("");
        etPalletTo.setText("");
        etPallet.setText("");
        lblLocationSuggested.setText("");

        isValidLocation = false;
        isPalletScanned = false;
        isToPalletScanned = false;
        isRSNScanned=false;

        lblassignedQty.setText("");

    //    btnPick.setEnabled(false);

        cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
        ivScanRSN.setImageResource(R.drawable.fullscreen_img);

        cvScanLocation.setCardBackgroundColor(getResources().getColor(R.color.locationColor));
        ivScanLocation.setImageResource(R.drawable.fullscreen_img);

        cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.palletColor));
        ivScanPallet.setImageResource(R.drawable.fullscreen_img);

        cvScanPalletTo.setCardBackgroundColor(getResources().getColor(R.color.palletColor));
        ivScanPalletTo.setImageResource(R.drawable.fullscreen_img);

    }


    //Assigning scanned value to the respective fields
    public void ProcessScannedinfo(String scannedData) {

        if(((DrawerLayout) getActivity().findViewById(R.id.drawer_layout)).isDrawerOpen(GravityCompat.START)){
            return;
        }




        if (ProgressDialogUtils.isProgressActive() || Common.isPopupActive()) {
            common.showUserDefinedAlertType(errorMessages.EMC_082, getActivity(), getContext(), "Warning");
            return;
        }

        if (common.isPopupActive() && rlPickList.getVisibility() != View.VISIBLE) {

        } else if (scannedData != null && !common.isPopupActive()) {

            if (!ProgressDialogUtils.isProgressActive()) {
                if (!lblLocationSuggested.getText().toString().isEmpty()) {

                    if(!isValidLocation){

                        if (!lblLocationSuggested.getText().toString().isEmpty() && lblLocationSuggested.getText().toString().equalsIgnoreCase(scannedData)) {
                            cvScanLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
                            ivScanLocation.setImageResource(R.drawable.check);
                            location = scannedData;
                            isValidLocation = true;
                        } else {
                            cvScanLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
                            ivScanLocation.setImageResource(R.drawable.warning_img);
                            common.showUserDefinedAlertType(errorMessages.EMC_0033, getActivity(), getContext(), "Warning");
                            // common.showUserDefinedAlertType(errorMessages.EMC_0033+" # "+scannedData+ " # "+lblLocationNo.getText().toString(), getActivity(), getContext(), "Warning");
                        }
                    }else{
                        if(!isPalletScanned){
                            if (scannedData.equals(etPallet.getText().toString())) {
                                isPalletScanned = true;
                                //ValidatePalletCode(etPallet.getText().toString());
                                cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.white));
                                ivScanPallet.setImageResource(R.drawable.check);
                            } else {
                                common.showUserDefinedAlertType(errorMessages.EMC_0009, getActivity(), getContext(), "Error");
                            }
                        }else{
                            if(!isToPalletScanned){
                                ValidatePallet(scannedData);
                            }else{
                                ValiDateMaterial(scannedData);
                            }

                        }
                        //   ValidateLocation(scannedData);
                    }
                }
                else {
                    common.showUserDefinedAlertType(errorMessages.EMC_0012, getActivity(), getContext(), "Error");
                }
            }
            else {
                if(!Common.isPopupActive())
                {
                    common.showUserDefinedAlertType(errorMessages.EMC_080, getActivity(), getContext(), "Error");

                }
                soundUtils.alertWarning(getActivity(),getContext());
            }

        } else {
            common.showUserDefinedAlertType(errorMessages.EMC_0030, getActivity(), getContext(), "Error");
        }
    }

    public void ValiDateMaterial(final String scannedData) {

        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.ScanDTO, getContext());
            ScanDTO scanDTO = new ScanDTO();
            scanDTO.setUserID(userId);
            scanDTO.setAccountID(accountId);
            // scanDTO.setTenantID(String.valueOf(tenantID));
            //scanDTO.setWarehouseID(String.valueOf(warehouseID));
            scanDTO.setScanInput(scannedData);
            scanDTO.setObdNumber(lblPickListNo.getText().toString());
            //inboundDTO.setIsOutbound("0");
            message.setEntityObject(scanDTO);

            Log.v("ABCDE",new Gson().toJson(message));

            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                call = apiService.ValiDateMaterial(message);
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
                            isRSNScanned=false;
                            cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
                            ivScanRSN.setImageResource(R.drawable.fullscreen_img);
                            ProgressDialogUtils.closeProgressDialog();
                            common.showAlertType(owmsExceptionMessage, getActivity(), getContext());

                        } else {
                            LinkedTreeMap<?, ?>_lResult = new LinkedTreeMap<>();
                            _lResult = (LinkedTreeMap<?, ?>) core.getEntityObject();

                            ScanDTO scanDTO1=new ScanDTO(_lResult.entrySet());
                            ProgressDialogUtils.closeProgressDialog();
                            if(scanDTO1!=null){
                                if(scanDTO1.getScanResult()){

                                /* ----For RSN reference----
                                    0 Sku|1 BatchNo|2 SerialNO|3 MFGDate|4 EXpDate|5 ProjectRefNO|6 Kit Id|7 line No|8 MRP ---- For SKU with 9 MSP's

                                    0 Sku|1 BatchNo|2 SerialNO|3 KitId|4 lineNo  ---- For SKU with 5 MSP's   *//*
                                    // Eg. : ToyCar|1|bat1|ser123|12/2/2018|12/2/2019|0|001*/

                                  /*  if(scanDTO1.getSkuCode().equalsIgnoreCase(lblSKuNo.getText().toString().trim())){

                                        if((lblBatchNo.getText().toString().equalsIgnoreCase(scanDTO1.getBatch()) || scanDTO1.getBatch()==null
                                                || scanDTO1.getBatch().equalsIgnoreCase("") || scanDTO1.getBatch().isEmpty())&&
                                                lblserialNo.getText().toString().equalsIgnoreCase(scanDTO1.getSerialNumber()) &&
                                                lblMfgDate.getText().toString().equalsIgnoreCase(scanDTO1.getMfgDate()) &&
                                                lblExpDate.getText().toString().equalsIgnoreCase(scanDTO1.getExpDate()) &&
                                                lblProjectRefNo.getText().toString().equalsIgnoreCase(scanDTO1.getPrjRef())
                                        ) {*/

/*                                           &&
                                             lblMfgDate.getText().toString().equalsIgnoreCase(scanDTO1.getMfgDate()) &&
                                             lblExpDate.getText().toString().equalsIgnoreCase(scanDTO1.getExpDate()
                                             lblProjectRefNo.getText().toString().equalsIgnoreCase(scanDTO1.getPrjRef())*/


                                            cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.white));
                                            ivScanRSN.setImageResource(R.drawable.check);

                                            isRSNScanned=true;

                                            if (scanType.equalsIgnoreCase("Auto")) {
                                                lblReceivedQty.setText("1.00");

                                                UpsertPickItem();
                                            } else {
                                                lblReceivedQty.setEnabled(true);
                                           //     btnPick.setEnabled(true);
                                                soundUtils.alertWarning(getActivity(), getContext());
                                                //DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0073);
                                            }
                                     /*   }else{
                                            common.showUserDefinedAlertType(errorMessages.EMC_0079,getActivity(),getContext(),"Error");
                                        }

                                    }else {
                                        isRSNScanned=false;
                                        cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.white));
                                        ivScanRSN.setImageResource(R.drawable.warning_img);
                                        common.showUserDefinedAlertType(errorMessages.EMC_0029, getActivity(), getContext(), "Error");
                                    }*/

                                } else{
                                    // lblScannedSku.setText("");
                                    isRSNScanned=false;
                                    cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanRSN.setImageResource(R.drawable.warning_img);
                                    common.showUserDefinedAlertType(errorMessages.EMC_0009, getActivity(), getContext(), "Warning");
                                }
                            }else{
                                common.showUserDefinedAlertType(getString(R.string.error_while_getting_data), getActivity(), getContext(), "Error");
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

    public void ValidatePallet(final String scannedData) {
        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.ScanDTO, getContext());
            ScanDTO scanDTO = new ScanDTO();
            scanDTO.setUserID(userId);
            scanDTO.setAccountID(accountId);
            // scanDTO.setTenantID(String.valueOf(tenantID));
            //scanDTO.setWarehouseID(String.valueOf(warehouseID));
            scanDTO.setScanInput(scannedData);
            scanDTO.setObdNumber(lblPickListNo.getText().toString());
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

                            etPalletTo.setText("");
                            cvScanPalletTo.setCardBackgroundColor(getResources().getColor(R.color.white));
                            ivScanPalletTo.setImageResource(R.drawable.invalid_cross);
                            ProgressDialogUtils.closeProgressDialog();
                            common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                        } else {
                            LinkedTreeMap<?, ?>_lResult = new LinkedTreeMap<>();
                            _lResult = (LinkedTreeMap<?, ?>) core.getEntityObject();

                            ScanDTO scanDTO1=new ScanDTO(_lResult.entrySet());
                            ProgressDialogUtils.closeProgressDialog();
                            if(scanDTO1!=null){
                                if(scanDTO1.getScanResult()){
                                    etPalletTo.setText(scannedData);
                                    //ValidatePalletCode();
                                    isToPalletScanned = true;
                                    cvScanPalletTo.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanPalletTo.setImageResource(R.drawable.check);
                                } else{
                                    isToPalletScanned=false;
                                    etPalletTo.setText("");
                                    cvScanPalletTo.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanPalletTo.setImageResource(R.drawable.warning_img);
                                    common.showUserDefinedAlertType(errorMessages.EMC_0009, getActivity(), getContext(), "Warning");
                                }
                            }else{
                                isToPalletScanned=false;
                                common.showUserDefinedAlertType(getString(R.string.error_while_getting_data), getActivity(), getContext(), "Error");
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

    public void ValidateLocation(final String scannedData) {
        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.ScanDTO, getContext());
            ScanDTO scanDTO = new ScanDTO();
            scanDTO.setUserID(userId);
            scanDTO.setAccountID(accountId);
            // scanDTO.setTenantID(String.valueOf(tenantID));
            scanDTO.setObdNumber(lblPickListNo.getText().toString());
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
                            cvScanLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
                            ivScanLocation.setImageResource(R.drawable.invalid_cross);
                            ProgressDialogUtils.closeProgressDialog();
                            common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                        } else {
                            LinkedTreeMap<?, ?>_lResult = new LinkedTreeMap<>();
                            _lResult = (LinkedTreeMap<?, ?>) core.getEntityObject();

                            ScanDTO scanDTO1=new ScanDTO(_lResult.entrySet());

                            if(scanDTO1!=null){
                                if(scanDTO1.getScanResult()){
                                    if (!lblLocationSuggested.getText().toString().isEmpty() && lblLocationSuggested.getText().toString().equalsIgnoreCase(scannedData)) {
                                        cvScanLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
                                        ivScanLocation.setImageResource(R.drawable.check);
                                        location = scannedData;
                                        isValidLocation = true;
                                    } else {
                                        cvScanLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
                                        ivScanLocation.setImageResource(R.drawable.warning_img);
                                        common.showUserDefinedAlertType(errorMessages.EMC_0033, getActivity(), getContext(), "Warning");
                                    }
                                } else{
                                    cvScanLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanLocation.setImageResource(R.drawable.warning_img);
                                    common.showUserDefinedAlertType(errorMessages.EMC_0016, getActivity(), getContext(), "Warning");
/*                                    etLocationTo.setText("");
                                    cvScanToLoc.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanToLoc.setImageResource(R.drawable.warning_img);
                                    common.showUserDefinedAlertType(errorMessages.EMC_0010, getActivity(), getContext(), "Warning");*/
                                }
                            }else{
                                common.showUserDefinedAlertType(getString(R.string.error_while_getting_data), getActivity(), getContext(), "Error");
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


    public void GetPickItem() {
        //To get Picked item Details
        try {
            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.Outbound, getContext());
            final OutbountDTO outbountDTO = new OutbountDTO();
            outbountDTO.setUserId(userId);
            outbountDTO.setAccountID(accountId);
            outbountDTO.setOutboundID(pickobdId);
            message.setEntityObject(outbountDTO);
            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method2
                ProgressDialogUtils.showProgressDialog("Please Wait");
                call = apiService.GetOBDItemToPick(message);
            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "003_01", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0002, getActivity(), getContext(), "Error");
            }

            try {
                //Getting response from the method
                call.enqueue(new Callback<String>() {

                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        try {
                            if (response.body() != null) {
                                core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);
                                if ((core.getType().toString().equals("Exception"))) {
                                    List<LinkedTreeMap<?, ?>> _lExceptions = new ArrayList<LinkedTreeMap<?, ?>>();
                                    _lExceptions = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                    WMSExceptionMessage owmsExceptionMessage = null;
                                    for (int i = 0; i < _lExceptions.size(); i++) {
                                        owmsExceptionMessage = new WMSExceptionMessage(_lExceptions.get(i).entrySet());
                                    }
                                    ClearFields();
                                    ProgressDialogUtils.closeProgressDialog();
                                    common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                                    if (owmsExceptionMessage.getWMSExceptionCode().equals("EMC_OB_DAL_PICKSugg_EC02") || owmsExceptionMessage.getWMSExceptionCode().equals("EMC_OB_DAL_PICKSugg_EC03") || owmsExceptionMessage.getWMSExceptionCode().equals("EMC_OB_DAL_PICKSugg_EC01") || owmsExceptionMessage.getWMSExceptionCode().equals("EMC_OB_DAL_PICKSugg_EC04")) {
                                        ProgressDialogUtils.closeProgressDialog();
                                        // Clearfields();
                                    }

                                } else {
                                    //Response object Success
                                    List<LinkedTreeMap<?, ?>> _lstPickitem = new ArrayList<LinkedTreeMap<?, ?>>();
                                    _lstPickitem = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();
                                    List<OutbountDTO> _lstOutboundDTO = new ArrayList<OutbountDTO>();
                                    OutbountDTO oOutboundDTO = null;
                                    for (int i = 0; i < _lstPickitem.size(); i++) {
                                        oOutboundDTO = new OutbountDTO(_lstPickitem.get(i).entrySet());
                                    }

                                    // Picking suggestions after successful picking
                                    ProgressDialogUtils.closeProgressDialog();
                                    sLoc = "" + oOutboundDTO.getsLoc();
                                    POSOHeaderId = "" + oOutboundDTO.getpOSOHeaderId();
                                    Lineno = "" + oOutboundDTO.getLineno();
                                    lblSKuNo.setText(oOutboundDTO.getSKU());
                                    assignedId = "" + oOutboundDTO.getAssignedID();
                                    soDetailsId = "" + oOutboundDTO.getSODetailsID();
                                    KitId = "" + oOutboundDTO.getAssignedID();
                                    lblBatchNo.setText(oOutboundDTO.getBatchNo());
                                    lblLocationSuggested.setText(oOutboundDTO.getLocation());
                                    etPallet.setText(oOutboundDTO.getPalletNo());
                                    pickedQty = oOutboundDTO.getPickedQty();

                                    if(lblLocationSuggested.getText().toString().equals("")){
                                        common.showUserDefinedAlertType(errorMessages.EMC_0063 + lblPickListNo.getText().toString(), getActivity(), getContext(), "Success");
                                        ClearFields();
                                        clearData();
                                        return;
                                    }

                                 //   lblReceivedQty.setText(oOutboundDTO.getAssignedQuantity());
                                  //  lblReceivedQty.setEnabled(false);

                                    lblassignedQty.setText(oOutboundDTO.getPickedQty() + "/" + oOutboundDTO.getAssignedQuantity());

                                    recQty = Double.parseDouble(oOutboundDTO.getPickedQty());
                                    totalQty = Double.parseDouble(oOutboundDTO.getAssignedQuantity());

                                    lblMfgDate.setText(oOutboundDTO.getMfgDate());
                                    lblExpDate.setText(oOutboundDTO.getExpDate());
                                    lblProjectRefNo.setText(oOutboundDTO.getProjectNo());
                                    lblserialNo.setText(oOutboundDTO.getSerialNo());
                                    lblMRP.setText(oOutboundDTO.getMRP());

                                    cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
                                    ivScanRSN.setImageResource(R.drawable.fullscreen_img);

                                    if (!lblLocationSuggested.getText().toString().equals(location)) {    // if location is not same as previously picked location

                                        isValidLocation = false;
                                        isPalletScanned = false;
                                        isToPalletScanned=false;
                                        location = "";

                                   //     btnPick.setEnabled(false);

                                        cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
                                        ivScanRSN.setImageResource(R.drawable.fullscreen_img);

                                        cvScanLocation.setCardBackgroundColor(getResources().getColor(R.color.locationColor));
                                        ivScanLocation.setImageResource(R.drawable.fullscreen_img);

                                        cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.palletColor));
                                        ivScanPallet.setImageResource(R.drawable.fullscreen_img);

                                        cvScanPalletTo.setCardBackgroundColor(getResources().getColor(R.color.palletColor));
                                        ivScanPalletTo.setImageResource(R.drawable.fullscreen_img);

                                    }


                                    if (oOutboundDTO.getPickedQty().equals(oOutboundDTO.getAssignedQuantity())) {        // Outbound completes when Pending and picking quantities are equal

                                        cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
                                        ivScanRSN.setImageResource(R.drawable.fullscreen_img);

                                        lblassignedQty.setText(oOutboundDTO.getPickedQty() + "/" + oOutboundDTO.getAssignedQuantity());

                                        ClearFields();
                                        clearData();

                                        common.showUserDefinedAlertType(errorMessages.EMC_0071, getActivity(), getContext(), "Success");

                                        ProgressDialogUtils.closeProgressDialog();
                                        return;

                                    }

                                }
                            }
                            ProgressDialogUtils.closeProgressDialog();
                        } catch (Exception ex) {
                            try {
                                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "003_02", getActivity());
                                logException();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            ProgressDialogUtils.closeProgressDialog();
                        }
                    }

                    // response object fails
                    @Override
                    public void onFailure(Call<String> call, Throwable throwable) {
                        ProgressDialogUtils.closeProgressDialog();
                        common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
                    }
                });
            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "003_03", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
            }
        } catch (Exception ex) {
            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "003_04", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            common.showUserDefinedAlertType(errorMessages.EMC_0003, getActivity(), getContext(), "Error");
        }
    }

    public void ValidatePalletCode(String pallet) {

        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.Outbound, getContext());
            OutbountDTO outbountDTO = new OutbountDTO();
            outbountDTO.setPalletNo(pallet);
            outbountDTO.setOutboundID(pickobdId);
            outbountDTO.setAccountID(accountId);
            message.setEntityObject(outbountDTO);
            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                ProgressDialogUtils.showProgressDialog("Please Wait");
                call = apiService.CheckContainerOBD(message);
                // } else {
                // DialogUtils.showAlertDialog(getActivity(), "Please enable internet");
                // return;
                // }
            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "004_01", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0002, getActivity(), getContext(), "Error");
            }
            try {
                //Getting response from the method
                call.enqueue(new Callback<String>() {

                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        if (response.body() != null) {
                            core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);
                            if ((core.getType().toString().equals("Exception"))) {
                                List<LinkedTreeMap<?, ?>> _lExceptions = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lExceptions = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();
                                WMSExceptionMessage owmsExceptionMessage = null;
                                for (int i = 0; i < _lExceptions.size(); i++) {
                                    owmsExceptionMessage = new WMSExceptionMessage(_lExceptions.get(i).entrySet());

                                    if (isPalletScanned) {
                                        cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.white));
                                        ivScanPallet.setImageResource(R.drawable.invalid_cross);
                                        etPallet.setText("");
                                        isPalletScanned = false;

                                    } else {
                                        cvScanPalletTo.setCardBackgroundColor(getResources().getColor(R.color.white));
                                        ivScanPalletTo.setImageResource(R.drawable.invalid_cross);
                                        etPalletTo.setText("");
                                    }
                                    ProgressDialogUtils.closeProgressDialog();
                                    common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                                }

                            } else {
                                List<LinkedTreeMap<?, ?>> _lPalletInventory = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lPalletInventory = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();
                                if (_lPalletInventory != null) {
                                    if (_lPalletInventory.size() > 0) {

                                        if (isPalletScanned && etPalletTo.getText().toString().isEmpty()) {
                                            cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.white));
                                            ivScanPallet.setImageResource(R.drawable.check);
                                        } else {

                                            cvScanPalletTo.setCardBackgroundColor(getResources().getColor(R.color.white));
                                            ivScanPalletTo.setImageResource(R.drawable.check);
                                        }
                                        ProgressDialogUtils.closeProgressDialog();


                                    } else {
                                        ProgressDialogUtils.closeProgressDialog();
                                        common.showUserDefinedAlertType(errorMessages.EMC_0028, getActivity(), getContext(), "Warning");
                                        return;
                                    }
                                }
                            }
                        } else {
                            ProgressDialogUtils.closeProgressDialog();
                            common.showUserDefinedAlertType(errorMessages.EMC_0021, getActivity(), getContext(), "Error");
                            return;
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable throwable) {
                        ProgressDialogUtils.closeProgressDialog();
                        common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
                        return;
                    }
                });
            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "004_02", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
            }
        } catch (Exception ex) {
            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "004_03", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            common.showUserDefinedAlertType(errorMessages.EMC_0003, getActivity(), getContext(), "Error");
            return;
        }
    }

    public void SkipItem() {
        try {
            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.Inbound, getContext());
            InboundDTO outbountDTO = new InboundDTO();
            outbountDTO.setSkipType("2");
            outbountDTO.setUserId(userId);
            outbountDTO.setAccountID(accountId);
            message.setEntityObject(outbountDTO);

            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                ProgressDialogUtils.showProgressDialog("Please Wait");
                call = apiService.GetSkipReasonList(message);

            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "007_01", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0002, getActivity(), getContext(), "Error");
            }
            try {
                //Getting response from the method
                call.enqueue(new Callback<String>() {

                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {

                        try {
                            core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);
                            if ((core.getType().toString().equals("Exception"))) {
                                List<LinkedTreeMap<?, ?>> _lExceptions = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lExceptions = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                WMSExceptionMessage owmsExceptionMessage = null;
                                for (int i = 0; i < _lExceptions.size(); i++) {
                                    owmsExceptionMessage = new WMSExceptionMessage(_lExceptions.get(i).entrySet());
                                    ProgressDialogUtils.closeProgressDialog();
                                }
                                common.setIsPopupActive(true);
                                common.showAlertType(owmsExceptionMessage, getActivity(), getContext());

                                if (owmsExceptionMessage.getWMSExceptionCode().equals("EMC_OB_DAL_PICKSugg_EC02") ||
                                        owmsExceptionMessage.getWMSExceptionCode().equals("EMC_OB_DAL_PICKSugg_EC03") ||
                                        owmsExceptionMessage.getWMSExceptionCode().equals("EMC_OB_DAL_PICKSugg_EC01") ||
                                        owmsExceptionMessage.getWMSExceptionCode().equals("EMC_OB_DAL_PICKSugg_EC04") ||
                                        owmsExceptionMessage.getWMSExceptionCode().equals("EMC_IN_DAL_001")) {
                                    //Clearfields();
                                    GetPickItem();
                                }
                                //  btnPick.setEnabled(true);
                            } else {
                                List<LinkedTreeMap<?, ?>> _lPickRefNo = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lPickRefNo = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();
                                List<String> lstPickRefNo = new ArrayList<>();

                                List<OutbountDTO> lstDto = new ArrayList<OutbountDTO>();
                                for (int i = 0; i < _lPickRefNo.size(); i++) {
                                    OutbountDTO dto = new OutbountDTO(_lPickRefNo.get(i).entrySet());
                                    lstDto.add(dto);
                                }
                                for (int i = 0; i < lstDto.size(); i++) {
                                    lstPickRefNo.add(String.valueOf(lstDto.get(i).getSkipReason()));
                                }

                                if (lstPickRefNo == null) {
                                    ProgressDialogUtils.closeProgressDialog();
                                    DialogUtils.showAlertDialog(getActivity(), "Picklist is null");
                                } else {
                                    ProgressDialogUtils.closeProgressDialog();
                                    ArrayAdapter arrayAdapterPickList = new ArrayAdapter(getActivity(), R.layout.support_simple_spinner_dropdown_item, lstPickRefNo);
                                    spinnerSelectReason.setAdapter(arrayAdapterPickList);
                                }

                                rlSkip.setVisibility(View.VISIBLE);
                                rlPickList.setVisibility(View.GONE);


                            }
                            //  btnPick.setEnabled(true);
                        } catch (Exception ex) {
                            try {
                                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "007_02", getActivity());
                                logException();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            ProgressDialogUtils.closeProgressDialog();
                        }
                    }

                    // response object fails
                    @Override
                    public void onFailure(Call<String> call, Throwable throwable) {
                        ProgressDialogUtils.closeProgressDialog();
                        common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
                    }
                });
            } catch (Exception ex) {
                ProgressDialogUtils.closeProgressDialog();
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "007_03", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
            }
        } catch (Exception ex) {
            ProgressDialogUtils.closeProgressDialog();
            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "007_04", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            common.showUserDefinedAlertType(errorMessages.EMC_0003, getActivity(), getContext(), "Error");
        }
    }

    public void UpsertPickItem() {

        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.Outbound, getContext());
            OutbountDTO oOutboundDTO = new OutbountDTO();
            oOutboundDTO.setUserId(userId);
            oOutboundDTO.setAccountID(accountId);
            oOutboundDTO.setSKU(lblSKuNo.getText().toString());
            oOutboundDTO.setKitId(KitId);
            oOutboundDTO.setCartonNo(etPallet.getText().toString());
            oOutboundDTO.setSerialNo(lblserialNo.getText().toString());
            oOutboundDTO.setMfgDate(lblMfgDate.getText().toString());
            oOutboundDTO.setExpDate(lblExpDate.getText().toString());
            oOutboundDTO.setBatchNo(lblBatchNo.getText().toString());
            oOutboundDTO.setProjectNo(lblProjectRefNo.getText().toString());
            oOutboundDTO.setAssignedID(assignedId);
            oOutboundDTO.setoBDNo(pickOBDno);
            oOutboundDTO.setOutboundID(pickobdId);
            oOutboundDTO.setLocation(lblLocationSuggested.getText().toString());
            oOutboundDTO.setPalletNo(etPallet.getText().toString());
            oOutboundDTO.setPickedQty(lblReceivedQty.getText().toString());
            oOutboundDTO.setAssignedID(assignedId);
            oOutboundDTO.setToCartonNo(etPalletTo.getText().toString());
            oOutboundDTO.setSODetailsID(soDetailsId);
            oOutboundDTO.setLineno(Lineno);
            oOutboundDTO.setMRP(lblMRP.getText().toString());
            oOutboundDTO.setpOSOHeaderId(POSOHeaderId);
            oOutboundDTO.setHasDis("0");
            oOutboundDTO.setIsDam("0");
            message.setEntityObject(oOutboundDTO);
            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                ProgressDialogUtils.showProgressDialog("Please Wait");
                call = apiService.UpdatePickItem(message);

            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "007_01", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0002, getActivity(), getContext(), "Error");
            }
            try {
                call.enqueue(new Callback<String>() {

                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {

                        try {
                            core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);
                            if ((core.getType().toString().equals("Exception"))) {
                                List<LinkedTreeMap<?, ?>> _lExceptions = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lExceptions = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                WMSExceptionMessage owmsExceptionMessage = null;
                                for (int i = 0; i < _lExceptions.size(); i++) {
                                    owmsExceptionMessage = new WMSExceptionMessage(_lExceptions.get(i).entrySet());
                                    ProgressDialogUtils.closeProgressDialog();
                                }

                                cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.white));
                                ivScanRSN.setImageResource(R.drawable.warning_img);

                                lblReceivedQty.setText("");
                             //   lblReceivedQty.setEnabled(false);
                          //      btnPick.setEnabled(false);

                                common.setIsPopupActive(true);
                                common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                                if (owmsExceptionMessage.getWMSExceptionCode().equals("EMC_OB_DAL_PICKSugg_EC02") ||
                                        owmsExceptionMessage.getWMSExceptionCode().equals("EMC_OB_DAL_PICKSugg_EC03") ||
                                        owmsExceptionMessage.getWMSExceptionCode().equals("EMC_OB_DAL_PICKSugg_EC01") ||
                                        owmsExceptionMessage.getWMSExceptionCode().equals("EMC_OB_DAL_PICKSugg_EC04") ||
                                        owmsExceptionMessage.getWMSExceptionCode().equals("EMC_IN_DAL_001")) {
                                }
                                if (owmsExceptionMessage.getWMSExceptionCode().equals("EMC_OB_DAL_010")){

                                    cvScanPalletTo.setCardBackgroundColor(getResources().getColor(R.color.palletColor));
                                    ivScanPalletTo.setImageResource(R.drawable.fullscreen_img);

                                    etPalletTo.setText("");

                                }

                                isRSNScanned=false;

                            } else {

                                List<LinkedTreeMap<?, ?>> _lstPickitem = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lstPickitem = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                OutbountDTO oOutboundDTO = null;
                                for (int i = 0; i < _lstPickitem.size(); i++) {
                                    oOutboundDTO = new OutbountDTO(_lstPickitem.get(i).entrySet());
                                }

                                ProgressDialogUtils.closeProgressDialog();

                                isRSNScanned=false;

                                if (oOutboundDTO.getPendingQty().equals("0")) {

                                    lblassignedQty.setText(oOutboundDTO.getPendingQty());
                                    lblReceivedQty.setText("");
                                    lblSKuNo.setText("");

                                    // Added to clear data after completion of the outbound
                                    ClearFields();
                                    clearData();

                                    common.showUserDefinedAlertType(errorMessages.EMC_0071, getActivity(), getContext(), "Success");


                                } else {

                                    lblReceivedQty.setText("");
                                    lblReceivedQty.setEnabled(false);
                                    lblReceivedQty.clearFocus();

                                    cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanRSN.setImageResource(R.drawable.check);

                                    lblassignedQty.setText(oOutboundDTO.getPickedQty() + "/" + oOutboundDTO.getAssignedQuantity());


                                    soundUtils.alertSuccess(getActivity(),getContext());

                                    GetPickItem();
                                }


                            }
                        } catch (Exception ex) {
                            try {
                                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "007_02", getActivity());
                                logException();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            ProgressDialogUtils.closeProgressDialog();
                        }
                    }

                    // response object fails
                    @Override
                    public void onFailure(Call<String> call, Throwable throwable) {
                        ProgressDialogUtils.closeProgressDialog();
                        common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
                    }
                });
            } catch (Exception ex) {
                ProgressDialogUtils.closeProgressDialog();
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "007_03", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
            }
        } catch (Exception ex) {
            ProgressDialogUtils.closeProgressDialog();
            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "007_04", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            common.showUserDefinedAlertType(errorMessages.EMC_0003, getActivity(), getContext(), "Error");
        }
    }

    public void OBDSkipItem() {

        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.Outbound, getContext());
            //final OutbountDTO outbountDTO = new OutbountDTO();
            double reqQty = totalQty - recQty;
            OutbountDTO oOutboundDTO = new OutbountDTO();
            oOutboundDTO.setUserId(userId);
            oOutboundDTO.setAccountID(accountId);
            oOutboundDTO.setSkipReason(skipReason);
            oOutboundDTO.setSKU(lblSKuNo.getText().toString());
            oOutboundDTO.setSerialNo(lblserialNo.getText().toString());
            oOutboundDTO.setMfgDate(lblMfgDate.getText().toString());
            oOutboundDTO.setExpDate(lblExpDate.getText().toString());
            oOutboundDTO.setBatchNo(lblBatchNo.getText().toString());
            oOutboundDTO.setProjectNo(lblProjectRefNo.getText().toString());
            oOutboundDTO.setOutboundID(pickobdId);
            oOutboundDTO.setLocation(lblLocationSuggested.getText().toString());
            oOutboundDTO.setPalletNo(etPallet.getText().toString());
            oOutboundDTO.setSkipQty(String.valueOf(reqQty));
            oOutboundDTO.setPickedQty(lblReceivedQty.getText().toString());
            oOutboundDTO.setAssignedID(assignedId);
            oOutboundDTO.setsLoc(sLoc);
            oOutboundDTO.setMRP(lblMRP.getText().toString());
            message.setEntityObject(oOutboundDTO);
            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);
            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method2
                ProgressDialogUtils.showProgressDialog("Please Wait");
                call = apiService.OBDSkipItem(message);
            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "003_01", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0002, getActivity(), getContext(), "Error");
            }
            try {
                //Getting response from the method
                call.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        try {
                            if (response.body() != null) {
                                core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);
                                if ((core.getType().toString().equals("Exception"))) {
                                    List<LinkedTreeMap<?, ?>> _lExceptions = new ArrayList<LinkedTreeMap<?, ?>>();
                                    _lExceptions = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                    WMSExceptionMessage owmsExceptionMessage = null;
                                    for (int i = 0; i < _lExceptions.size(); i++) {
                                        owmsExceptionMessage = new WMSExceptionMessage(_lExceptions.get(i).entrySet());
                                    }
                                    // ClearFields();
                                    ProgressDialogUtils.closeProgressDialog();
                                    common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                                    if (owmsExceptionMessage.getWMSExceptionCode().equals("EMC_OB_DAL_PICKSugg_EC02")
                                            || owmsExceptionMessage.getWMSExceptionCode().equals("EMC_OB_DAL_PICKSugg_EC03")
                                            || owmsExceptionMessage.getWMSExceptionCode().equals("EMC_OB_DAL_PICKSugg_EC01")
                                            || owmsExceptionMessage.getWMSExceptionCode().equals("EMC_OB_DAL_PICKSugg_EC04")) {
                                        ProgressDialogUtils.closeProgressDialog();
                                    }

                                    rlPickList.setVisibility(View.VISIBLE);
                                    rlSkip.setVisibility(View.GONE);

                                    GetPickItem();

                                } else {
                                    //Response object Success
                                    List<LinkedTreeMap<?, ?>> _lstPickitem = new ArrayList<LinkedTreeMap<?, ?>>();
                                    _lstPickitem = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();
                                    List<OutbountDTO> _lstOutboundDTO = new ArrayList<OutbountDTO>();
                                    OutbountDTO oOutboundDTO = null;
                                    for (int i = 0; i < _lstPickitem.size(); i++) {
                                        oOutboundDTO = new OutbountDTO(_lstPickitem.get(i).entrySet());
                                    }

                                    location = "";
                                    isValidLocation = false;
                                    isPalletScanned = false;
                                    isToPalletScanned=false;

                                    etPalletTo.setText("");

                                    cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
                                    ivScanRSN.setImageResource(R.drawable.fullscreen_img);

                                    cvScanLocation.setCardBackgroundColor(getResources().getColor(R.color.locationColor));
                                    ivScanLocation.setImageResource(R.drawable.fullscreen_img);

                                    cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.palletColor));
                                    ivScanPallet.setImageResource(R.drawable.fullscreen_img);

                                    cvScanPalletTo.setCardBackgroundColor(getResources().getColor(R.color.palletColor));
                                    ivScanPalletTo.setImageResource(R.drawable.fullscreen_img);

                                    if (oOutboundDTO.getPendingQty().equals("0")) {

                                        rlPickList.setVisibility(View.VISIBLE);
                                        rlSkip.setVisibility(View.GONE);

                                        lblassignedQty.setText(oOutboundDTO.getPendingQty());
                                        lblReceivedQty.setText("");
                                        lblSKuNo.setText("");

                                        // Added to clear data after completion of the outbound

                                        common.showUserDefinedAlertType(errorMessages.EMC_0063 + lblPickListNo.getText().toString(), getActivity(), getContext(), "Success");
                                        ClearFields();
                                        clearData();

                                    }else{

                                        sLoc = "" + oOutboundDTO.getsLoc();
                                        POSOHeaderId = "" + oOutboundDTO.getpOSOHeaderId();
                                        Lineno = "" + oOutboundDTO.getLineno();
                                        lblSKuNo.setText(oOutboundDTO.getSKU());
                                        assignedId = "" + oOutboundDTO.getAssignedID();
                                        soDetailsId = "" + oOutboundDTO.getSODetailsID();
                                        KitId = "" + oOutboundDTO.getAssignedID();
                                        lblBatchNo.setText(oOutboundDTO.getBatchNo());
                                        lblLocationSuggested.setText(oOutboundDTO.getLocation());
                                        etPallet.setText(oOutboundDTO.getPalletNo());
                                        lblassignedQty.setText(oOutboundDTO.getPickedQty() + "/" + oOutboundDTO.getAssignedQuantity());
                                        lblMfgDate.setText(oOutboundDTO.getMfgDate());
                                        lblExpDate.setText(oOutboundDTO.getExpDate());
                                        lblProjectRefNo.setText(oOutboundDTO.getProjectNo());
                                        lblserialNo.setText(oOutboundDTO.getSerialNo());
                                        lblMRP.setText(oOutboundDTO.getMRP());
                                        lblReceivedQty.setText(oOutboundDTO.getAssignedQuantity());
                                        rlPickList.setVisibility(View.VISIBLE);
                                        rlSkip.setVisibility(View.GONE);


                                        common.showUserDefinedAlertType(errorMessages.EMC_0077,getActivity(),getContext(),"Success");

                                    }

                                    ProgressDialogUtils.closeProgressDialog();
                                }
                            }else {
                                common.showUserDefinedAlertType(errorMessages.EMC_0021, getActivity(), getContext(), "Error");
                                ProgressDialogUtils.closeProgressDialog();
                            }

                            ProgressDialogUtils.closeProgressDialog();

                        } catch (Exception ex) {
                            try {
                                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "003_02", getActivity());
                                logException();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            ProgressDialogUtils.closeProgressDialog();
                        }
                    }

                    // response object fails
                    @Override
                    public void onFailure(Call<String> call, Throwable throwable) {
                        ProgressDialogUtils.closeProgressDialog();
                        common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
                    }
                });
            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "003_03", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
            }
        } catch (Exception ex) {
            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "003_04", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            common.showUserDefinedAlertType(errorMessages.EMC_0003, getActivity(), getContext(), "Error");
        }
    }

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
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "002_01", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0002, getActivity(), getContext(), "Error");
            }
            try {
                //Getting response from the method
                call.enqueue(new Callback<String>() {

                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {

                        try {
                            core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);
                            // if any Exception throws
                            if ((core.getType().toString().equals("Exception"))) {
                                List<LinkedTreeMap<?, ?>> _lExceptions = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lExceptions = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                WMSExceptionMessage owmsExceptionMessage = null;
                                for (int i = 0; i < _lExceptions.size(); i++) {
                                    owmsExceptionMessage = new WMSExceptionMessage(_lExceptions.get(i).entrySet());
                                    ProgressDialogUtils.closeProgressDialog();
                                    common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                                    return;
                                }
                            } else {
                                LinkedTreeMap<String, String> _lResultvalue = new LinkedTreeMap<String, String>();
                                _lResultvalue = (LinkedTreeMap<String, String>) core.getEntityObject();
                                for (Map.Entry<String, String> entry : _lResultvalue.entrySet()) {
                                    if (entry.getKey().equals("Result")) {
                                        String Result = entry.getValue();
                                        if (Result.equals("0")) {
                                            ProgressDialogUtils.closeProgressDialog();
                                            return;
                                        } else {
                                            ProgressDialogUtils.closeProgressDialog();
                                            exceptionLoggerUtils.deleteFile(getActivity());
                                            return;
                                        }
                                    }
                                }
                            }
                        } catch (Exception ex) {

                            try {
                                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "002_02", getActivity());
                                logException();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            ProgressDialogUtils.closeProgressDialog();
                            //Log.d("Message", core.getEntityObject().toString());
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable throwable) {
                        //Toast.makeText(LoginActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
                        ProgressDialogUtils.closeProgressDialog();
                        common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
                    }
                });
            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "002_03", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0002, getActivity(), getContext(), "Error");
            }
        } catch (Exception ex) {
            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "002_04", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            common.showUserDefinedAlertType(errorMessages.EMC_0003, getActivity(), getContext(), "Error");
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
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.menu_obdPicking));
    }


    @Override
    public void onDestroyView() {

        // Honeywell onDestroyView
        if (barcodeReader != null) {
            // unregister barcode event listener honeywell
            barcodeReader.removeBarcodeListener((BarcodeReader.BarcodeListener) this);

            // unregister trigger state change listener
            barcodeReader.removeTriggerListener((BarcodeReader.TriggerListener) this);
        }

        // Cipher onDestroyView
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
        super.onDestroyView();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        skipReason = spinnerSelectReason.getSelectedItem().toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}