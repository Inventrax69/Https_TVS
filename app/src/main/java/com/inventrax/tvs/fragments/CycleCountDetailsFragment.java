package com.inventrax.tvs.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cipherlab.barcode.GeneralString;
import com.google.android.material.textfield.TextInputLayout;
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
import com.inventrax.tvs.adapters.CCExportAdapter;
import com.inventrax.tvs.common.Common;
import com.inventrax.tvs.common.constants.EndpointConstants;
import com.inventrax.tvs.common.constants.ErrorMessages;
import com.inventrax.tvs.interfaces.ApiInterface;
import com.inventrax.tvs.pojos.CycleCountDTO;
import com.inventrax.tvs.pojos.InventoryDTO;
import com.inventrax.tvs.pojos.ScanDTO;
import com.inventrax.tvs.pojos.WMSCoreMessage;
import com.inventrax.tvs.pojos.WMSExceptionMessage;
import com.inventrax.tvs.services.RetrofitBuilderHttpsEx;
import com.inventrax.tvs.util.DialogUtils;
import com.inventrax.tvs.util.ExceptionLoggerUtils;
import com.inventrax.tvs.util.ProgressDialogUtils;
import com.inventrax.tvs.util.ScanValidator;
import com.inventrax.tvs.util.SoundUtils;
import com.inventrax.tvs.searchableSpinner.SearchableSpinner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Padmaja on 06/27/2018.
 */

public class CycleCountDetailsFragment extends Fragment implements View.OnClickListener, BarcodeReader.TriggerListener, BarcodeReader.BarcodeListener, AdapterView.OnItemSelectedListener {

    private static final String classCode = "API_FRAG_CYCLE COUNT";
    private View rootView;
    DialogUtils dialogUtils;

    private Button btnConfirm, btnBinComplete, btnClear, btnExportCC, btnCloseExport;
    private TextView lblCycleCount, lblScannedSku;
    private CardView cvScanLocation,  cvScanSKU;
    private TextInputLayout txtInputLayoutLocation, txtInputLayoutContainer, txtInputLayoutSerial, txtInputLayoutBatch, txtInputLayoutMfgDate,
            txtInputLayoutExpDate, txtInputLayoutProjectRef, txtInputLayoutCCQty, txtInputLayoutMRP;

    private EditText etLocation, etSerial, etBatch, etMfgDate, etExpDate, etProjectRef, etCCQty, etCCMRP;
    private ImageView ivScanLocation, ivScanSKU;
    private RelativeLayout rlCC, rlCCExport;
    private RecyclerView rvPendingCC;

    String scanner = null;
    String getScanner = null;

    private IntentFilter filter;
    private Gson gson;
    private WMSCoreMessage core;


    //For Honey well barcode
    private static BarcodeReader barcodeReader;
    private AidcManager manager;

    String materialCode = "", warehouseId = "", tenantId = "";
    private Common common = null;

    String userId = null, scanType = null, accountId = null;

    private ExceptionLoggerUtils exceptionLoggerUtils;
    private ErrorMessages errorMessages;
    private SoundUtils soundUtils;
    private LinearLayoutManager linearLayoutManager;
    boolean isValidLocation = false;
    boolean isPalletScanned = false;
    boolean isRSNScanned = false;
    String Rack = "", Column = "", Level = "",CycleCountSeqCode="";
    TextView tvRack, tvColumn, tvLevel;
    SearchableSpinner spinnerSelectSloc;
    String storageLoc;

    List<InventoryDTO> _lstOutboundDTO;

    public void myScannedData(Context context, String barcode){
        try {
            ProcessScannedinfo(barcode.trim());
        }catch (Exception e){
            //  Toast.makeText(context, ""+e.toString(), Toast.LENGTH_SHORT).show();
        }
    }


    // Cipher Barcode Scanner
    private final BroadcastReceiver myDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scanner = intent.getStringExtra(GeneralString.BcReaderData);  // Scanned Barcode info
            ProcessScannedinfo(scanner.trim().toString());
        }
    };

    public CycleCountDetailsFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_cyclecount_details, container, false);
        barcodeReader = MainActivity.getBarcodeObject();
        loadFormControls();

        return rootView;
    }

    private void loadFormControls() {

        SharedPreferences sp = getActivity().getSharedPreferences("LoginActivity", Context.MODE_PRIVATE);
        userId = sp.getString("RefUserId", "");
        scanType = sp.getString("scanType", "");
        accountId = sp.getString("AccountId", "");

        rlCC = (RelativeLayout) rootView.findViewById(R.id.rlCC);
        rlCCExport = (RelativeLayout) rootView.findViewById(R.id.rlCCExport);

        rvPendingCC = (RecyclerView) rootView.findViewById(R.id.rvPendingCC);
        rvPendingCC.setHasFixedSize(true);

        linearLayoutManager = new LinearLayoutManager(getContext());

        // use a linear layout manager
        rvPendingCC.setLayoutManager(linearLayoutManager);

        cvScanLocation = (CardView) rootView.findViewById(R.id.cvScanLocation);
        cvScanSKU = (CardView) rootView.findViewById(R.id.cvScanSKU);

        ivScanLocation = (ImageView) rootView.findViewById(R.id.ivScanLocation);
        ivScanSKU = (ImageView) rootView.findViewById(R.id.ivScanSKU);

        btnBinComplete = (Button) rootView.findViewById(R.id.btnBinComplete);
        btnCloseExport = (Button) rootView.findViewById(R.id.btnCloseExport);
        btnExportCC = (Button) rootView.findViewById(R.id.btnExportCC);
        btnConfirm = (Button) rootView.findViewById(R.id.btnConfirm);
        btnClear = (Button) rootView.findViewById(R.id.btnClear);

        lblCycleCount = (TextView) rootView.findViewById(R.id.lblCycleCount);
        lblScannedSku = (TextView) rootView.findViewById(R.id.lblScannedSku);

        tvRack = (TextView) rootView.findViewById(R.id.tvRack);
        tvColumn = (TextView) rootView.findViewById(R.id.tvColumn);
        tvLevel = (TextView) rootView.findViewById(R.id.tvLevel);

        etLocation = (EditText) rootView.findViewById(R.id.etLocation);
        etSerial = (EditText) rootView.findViewById(R.id.etSerial);
        etMfgDate = (EditText) rootView.findViewById(R.id.etMfgDate);
        etBatch = (EditText) rootView.findViewById(R.id.etBatch);
        etExpDate = (EditText) rootView.findViewById(R.id.etExpDate);
        etProjectRef = (EditText) rootView.findViewById(R.id.etProjectRef);
        etCCQty = (EditText) rootView.findViewById(R.id.etCCQty);
        etCCMRP = (EditText) rootView.findViewById(R.id.etCCMRP);

        etCCQty.clearFocus();
        etCCQty.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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

        txtInputLayoutLocation = (TextInputLayout) rootView.findViewById(R.id.txtInputLayoutLocation);
        txtInputLayoutBatch = (TextInputLayout) rootView.findViewById(R.id.txtInputLayoutBatch);
        txtInputLayoutSerial = (TextInputLayout) rootView.findViewById(R.id.txtInputLayoutSerial);
        txtInputLayoutMfgDate = (TextInputLayout) rootView.findViewById(R.id.txtInputLayoutMfgDate);
        txtInputLayoutExpDate = (TextInputLayout) rootView.findViewById(R.id.txtInputLayoutExpDate);
        txtInputLayoutProjectRef = (TextInputLayout) rootView.findViewById(R.id.txtInputLayoutProjectRef);
        txtInputLayoutCCQty = (TextInputLayout) rootView.findViewById(R.id.txtInputLayoutCCQty);
        txtInputLayoutMRP = (TextInputLayout) rootView.findViewById(R.id.txtInputLayoutMRP);

        spinnerSelectSloc = (SearchableSpinner) rootView.findViewById(R.id.spinnerSelectSloc);
        spinnerSelectSloc.setOnItemSelectedListener(this);

        lblCycleCount.setText(getArguments().getString("CCname"));
        warehouseId = getArguments().getString("warehouseId");
        tenantId = getArguments().getString("tenantId");
        Rack = getArguments().getString("Rack");
        Column = getArguments().getString("Column");
        Level = getArguments().getString("Level");
        CycleCountSeqCode = getArguments().getString("CycleCountSeqCode");

        tvRack.setText("Rack : " + Rack);
        tvColumn.setText("Col : " + Column);
        tvLevel.setText("Level : " + Level);

        exceptionLoggerUtils = new ExceptionLoggerUtils();
        errorMessages = new ErrorMessages();
        soundUtils = new SoundUtils();

        // For Cipher Barcode reader
        Intent RTintent = new Intent("sw.reader.decode.require");
        RTintent.putExtra("Enable", true);
        getActivity().sendBroadcast(RTintent);
        this.filter = new IntentFilter();
        this.filter.addAction("sw.reader.decode.complete");
        getActivity().registerReceiver(this.myDataReceiver, this.filter);

        gson = new GsonBuilder().create();
        common = new Common();
        ProgressDialogUtils.closeProgressDialog();
        common.setIsPopupActive(false);

        btnBinComplete.setOnClickListener(this);
        btnClear.setOnClickListener(this);
        btnCloseExport.setOnClickListener(this);
        btnConfirm.setOnClickListener(this);
        btnExportCC.setOnClickListener(this);


        if (scanType.equals("Auto")) {
            btnConfirm.setEnabled(false);
            btnConfirm.setTextColor(getResources().getColor(R.color.black));
            btnConfirm.setBackgroundResource(R.drawable.button_hide);
        } else {
            btnConfirm.setEnabled(true);
            btnConfirm.setTextColor(getResources().getColor(R.color.white));
            btnConfirm.setBackgroundResource(R.drawable.button_shape);
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





    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnBinComplete:
                if (!etLocation.getText().toString().isEmpty()) {
                    DialogUtils.showConfirmDialog(getActivity(), "Confirm", "Are you sure to complete this bin? ", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    common.setIsPopupActive(false);
                                    releaseCycleCountLocation();
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    common.setIsPopupActive(false);
                                    break;
                            }

                        }
                    });
                } else {
                    common.showUserDefinedAlertType(errorMessages.EMC_0007, getActivity(), getContext(), "Error");
                    return;
                }
                break;

            case R.id.btnConfirm:
                if (!materialCode.equals("")) {
                    if (etCCQty.getText().toString().isEmpty()) {
                        common.showUserDefinedAlertType("Please enter quantity", getActivity(), getContext(), "Error");
                        return;
                    } else {
                        DialogUtils.showConfirmDialog(getActivity(), "Confirm", "Are you sure to submit this sku? ", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                switch (which) {
                                    case DialogInterface.BUTTON_POSITIVE:
                                        common.setIsPopupActive(false);
                                        upsertCycleCount();
                                        break;

                                    case DialogInterface.BUTTON_NEGATIVE:
                                        common.setIsPopupActive(false);
                                        break;
                                }

                            }
                        });

                    }
                } else {
                    common.showUserDefinedAlertType(errorMessages.EMC_0065, getActivity(), getContext(), "Error");
                    return;
                }
                break;

            case R.id.btnCloseExport:
                rlCC.setVisibility(View.VISIBLE);
                rlCCExport.setVisibility(View.GONE);
                break;

            case R.id.btnClear:
                clearFields();
                break;
            case R.id.cvScanContainer:
                if (isPalletScanned) {
                    clearFields1();
                } else {
                    clearFields1();
                    isPalletScanned = true;
                }

                break;

            case R.id.btnExportCC:

                if (!etLocation.getText().toString().isEmpty()) {

                    rlCC.setVisibility(View.GONE);
                    rlCCExport.setVisibility(View.VISIBLE);

                    rvPendingCC.setAdapter(null);

                    getCycleCountInformation();

                } else {
                    common.showUserDefinedAlertType(errorMessages.EMC_0007, getActivity(), getContext(), "Error");
                    return;
                }
                break;
        }
    }



    // honeywell
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
                barcodeReader.setProperty(BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE,
                        BarcodeReader.TRIGGER_CONTROL_MODE_AUTO_CONTROL);
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


    //Assigning scanned value to the respective fields
    public void ProcessScannedinfo(String scannedData) {

        if(((DrawerLayout) getActivity().findViewById(R.id.drawer_layout)).isDrawerOpen(GravityCompat.START)){
            return;
        }

        if (ProgressDialogUtils.isProgressActive() || Common.isPopupActive()) {
            common.showUserDefinedAlertType(errorMessages.EMC_082, getActivity(), getContext(), "Warning");
            return;
        }

        if (rlCC.getVisibility() == View.VISIBLE) {

            if (scannedData != null && !common.isPopupActive) {

                if (!isValidLocation) {
                    ValidateLocation(scannedData);
                } else {


                        if (ScanValidator.isRSNScanned(scannedData)) {
                            scannedData = scannedData.split("[-]", 2)[0];
                            lblScannedSku.setText(scannedData);
                        }

                        ValiDateMaterial(scannedData);

                }


            }
        }
    }


    public void isBlockedLocation() {

        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.CycleCount, getActivity());
            final CycleCountDTO cycleCountDTO = new CycleCountDTO();
            cycleCountDTO.setUserId(userId);
            cycleCountDTO.setAccountID(accountId);
            cycleCountDTO.setCCName(lblCycleCount.getText().toString());
            cycleCountDTO.setLocation(etLocation.getText().toString());
            cycleCountDTO.setWarehouseID(warehouseId);
            cycleCountDTO.setTenantId(tenantId);
            message.setEntityObject(cycleCountDTO);

            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                ProgressDialogUtils.showProgressDialog("Please Wait");
                call = apiService.IsBlockedLocation(message);

                // } else {
                // DialogUtils.showAlertDialog(getActivity(), "Please enable internet");
                // return;
                // }

            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_01", getActivity());
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


                                }

                                etLocation.setText("");
                                isValidLocation = false;
                                cvScanLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
                                ivScanLocation.setImageResource(R.drawable.invalid_cross);

                                ProgressDialogUtils.closeProgressDialog();
                                common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                            } else {

                                List<LinkedTreeMap<?, ?>> _lstCC = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lstCC = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                List<CycleCountDTO> lstDto = new ArrayList<CycleCountDTO>();
                                List<String> _lstCCNames = new ArrayList<>();

                                for (int i = 0; i < _lstCC.size(); i++) {
                                    CycleCountDTO dto = new CycleCountDTO(_lstCC.get(i).entrySet());
                                    lstDto.add(dto);
                                }

                                ProgressDialogUtils.closeProgressDialog();

                                for (int i = 0; i < lstDto.size(); i++) {

                                    if (lstDto.get(i).getResult().equals("-1")) {
                                        isValidLocation = true;
                                        cvScanLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
                                        ivScanLocation.setImageResource(R.drawable.check);
                                        return;
                                    } else {
                                        isValidLocation = false;
                                        etLocation.setText("");
                                        cvScanLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
                                        ivScanLocation.setImageResource(R.drawable.warning_img);
                                        common.showUserDefinedAlertType(lstDto.get(i).getResult(), getActivity(), getContext(), "Error");
                                        return;
                                    }

                                }


                            }

                        } catch (Exception ex) {
                            try {
                                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_02", getActivity());
                                logException();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            ProgressDialogUtils.closeProgressDialog();
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
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_03", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
            }
        } catch (Exception ex) {
            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_03", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
        }
    }

    public void checkMaterialAvailablilty() {

        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.CycleCount, getActivity());
            final CycleCountDTO cycleCountDTO = new CycleCountDTO();
            cycleCountDTO.setUserId(userId);
            cycleCountDTO.setAccountID(accountId);
            cycleCountDTO.setCCName(lblCycleCount.getText().toString());
            cycleCountDTO.setLocation(etLocation.getText().toString());
            cycleCountDTO.setMaterialCode(lblScannedSku.getText().toString());
            cycleCountDTO.setWarehouseID(warehouseId);
            cycleCountDTO.setTenantId(tenantId);
            message.setEntityObject(cycleCountDTO);


            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                ProgressDialogUtils.showProgressDialog("Please Wait");
                call = apiService.CheckMaterialAvailablilty(message);

                // } else {
                // DialogUtils.showAlertDialog(getActivity(), "Please enable internet");
                // return;
                // }

            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_01", getActivity());
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
                                }

                                cvScanSKU.setCardBackgroundColor(getResources().getColor(R.color.white));
                                ivScanSKU.setImageResource(R.drawable.warning_img);

                                materialCode = "";
                                lblScannedSku.setText("");
                                isRSNScanned = false;

                                ProgressDialogUtils.closeProgressDialog();
                                common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                            } else {

                                List<LinkedTreeMap<?, ?>> _lstCC = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lstCC = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                List<CycleCountDTO> lstDto = new ArrayList<CycleCountDTO>();
                                List<String> _lstCCNames = new ArrayList<>();

                                for (int i = 0; i < _lstCC.size(); i++) {
                                    CycleCountDTO dto = new CycleCountDTO(_lstCC.get(i).entrySet());
                                    lstDto.add(dto);
                                }

                                ProgressDialogUtils.closeProgressDialog();

                                for (int i = 0; i < lstDto.size(); i++) {

                                    if (lstDto.get(i).getResult().equals("1")) {
                                        isRSNScanned = true;
                                        cvScanSKU.setCardBackgroundColor(getResources().getColor(R.color.white));
                                        ivScanSKU.setImageResource(R.drawable.check);

//                                        <MAHE>
                                        etCCQty.setText("1");
                                            upsertCycleCount();

                                    } else {
                                        common.showUserDefinedAlertType(lstDto.get(i).getResult(), getActivity(), getContext(), "Error");
                                        return;
                                    }

                                }


                            }

                        } catch (Exception ex) {
                            try {
                                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_02", getActivity());
                                logException();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            ProgressDialogUtils.closeProgressDialog();
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
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_03", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
            }
        } catch (Exception ex) {
            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_03", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
        }
    }

    public void upsertCycleCount() {
        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.CycleCount, getActivity());

            final CycleCountDTO cycleCountDTO = new CycleCountDTO();
            cycleCountDTO.setUserId(userId);
            cycleCountDTO.setAccountID(accountId);
            cycleCountDTO.setWarehouseID(warehouseId);
            cycleCountDTO.setTenantId(tenantId);
            cycleCountDTO.setCCName(lblCycleCount.getText().toString());
            cycleCountDTO.setLocation(etLocation.getText().toString());
            cycleCountDTO.setPalletNo("");
            cycleCountDTO.setMaterialCode(lblScannedSku.getText().toString());
            cycleCountDTO.setCCQty(etCCQty.getText().toString());
            cycleCountDTO.setBatchNo(etBatch.getText().toString());
            cycleCountDTO.setSerialNo(etSerial.getText().toString());
            cycleCountDTO.setProjectRefNo(etProjectRef.getText().toString());
            cycleCountDTO.setMfgDate(etMfgDate.getText().toString());
            cycleCountDTO.setExpDate(etExpDate.getText().toString());
            //cycleCountDTO.setCount(tvCount.getText().toString());
            cycleCountDTO.setMRP(etCCMRP.getText().toString());
            cycleCountDTO.setStorageLocation("");
            message.setEntityObject(cycleCountDTO);


            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                ProgressDialogUtils.showProgressDialog("Please Wait");
                call = apiService.UpsertCycleCount(message);
                // } else {
                // DialogUtils.showAlertDialog(getActivity(), "Please enable internet");
                // return;
                // }

            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_01", getActivity());
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
                                }

                                cvScanSKU.setCardBackgroundColor(getResources().getColor(R.color.white));
                                ivScanSKU.setImageResource(R.drawable.warning_img);

                                lblScannedSku.setText("");

                                materialCode = "";

                                ProgressDialogUtils.closeProgressDialog();
                                common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                            } else {

                                List<LinkedTreeMap<?, ?>> _lstCC = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lstCC = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                List<CycleCountDTO> lstDto = new ArrayList<CycleCountDTO>();
                                List<String> _lstCCNames = new ArrayList<>();

                                for (int i = 0; i < _lstCC.size(); i++) {
                                    CycleCountDTO dto = new CycleCountDTO(_lstCC.get(i).entrySet());
                                    lstDto.add(dto);
                                }

                                ProgressDialogUtils.closeProgressDialog();



                                for (int i = 0; i < lstDto.size(); i++) {

                                    if (lstDto.get(i).getResult().equals("Confirmed successfully")) {

                                        cvScanSKU.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
                                        ivScanSKU.setImageResource(R.drawable.fullscreen_img);

                                        materialCode = "";
                                        lblScannedSku.setText("");
                                        etSerial.setText("");
                                        etBatch.setText("");
                                        etMfgDate.setText("");
                                        etExpDate.setText("");
                                        etProjectRef.setText("");
                                        etCCQty.setText("");

                                        soundUtils.alertSuccess(getActivity(), getContext());

                                        etCCQty.setEnabled(false);
                                        etCCQty.clearFocus();


                                    } else {
                                        common.showUserDefinedAlertType(lstDto.get(i).getResult(), getActivity(), getContext(), "Error");
                                        return;
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            try {
                                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_02", getActivity());
                                logException();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            ProgressDialogUtils.closeProgressDialog();
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
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_03", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
            }
        } catch (Exception ex) {
            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_03", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
        }
    }

    public void getCycleCountInformation() {
        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.CycleCount, getActivity());
            final CycleCountDTO cycleCountDTO = new CycleCountDTO();
            cycleCountDTO.setUserId(userId);
            cycleCountDTO.setAccountID(accountId);
            cycleCountDTO.setCCName(lblCycleCount.getText().toString());
            cycleCountDTO.setLocation(etLocation.getText().toString());
            cycleCountDTO.setWarehouseID(warehouseId);
            cycleCountDTO.setTenantId(tenantId);
            message.setEntityObject(cycleCountDTO);


            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                ProgressDialogUtils.showProgressDialog("Please Wait");
                call = apiService.GetCycleCountInformation(message);



            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_01", getActivity());
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
                                }

                                ProgressDialogUtils.closeProgressDialog();
                                common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                            } else {

                                List<LinkedTreeMap<?, ?>> _lCCExport = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lCCExport = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();
                                List<CycleCountDTO> lstCCExport = new ArrayList<CycleCountDTO>();

                                ProgressDialogUtils.closeProgressDialog();

                                if (_lCCExport.size() > 0) {
                                    CycleCountDTO ccdto = null;
                                    for (int i = 0; i < _lCCExport.size(); i++) {

                                        ccdto = new CycleCountDTO(_lCCExport.get(i).entrySet());
                                        lstCCExport.add(ccdto);
                                    }

                                    CCExportAdapter ccExportAdapter = new CCExportAdapter(getActivity(), lstCCExport);
                                    rvPendingCC.setAdapter(ccExportAdapter);
                                } else {
                                    common.showUserDefinedAlertType(errorMessages.EMC_0060, getActivity(), getContext(), "Warning");
                                    return;
                                }

                            }

                        } catch (Exception ex) {
                            try {
                                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_02", getActivity());
                                logException();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            ProgressDialogUtils.closeProgressDialog();
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
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_03", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
            }
        } catch (Exception ex) {
            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_03", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
        }
    }

    public void releaseCycleCountLocation() {

        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.CycleCount, getActivity());
            final CycleCountDTO cycleCountDTO = new CycleCountDTO();
            cycleCountDTO.setUserId(userId);
            cycleCountDTO.setAccountID(accountId);
            cycleCountDTO.setCCName(lblCycleCount.getText().toString());
            cycleCountDTO.setLocation(etLocation.getText().toString());
            cycleCountDTO.setWarehouseID(warehouseId);
            cycleCountDTO.setTenantId(tenantId);
            cycleCountDTO.setCycleCountSeqCode(CycleCountSeqCode);
            message.setEntityObject(cycleCountDTO);

            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                ProgressDialogUtils.showProgressDialog("Please Wait");
                call = apiService.ReleaseCycleCountLocation(message);



            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_01", getActivity());
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
                                }

                                ProgressDialogUtils.closeProgressDialog();
                                common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                            } else {

                                List<LinkedTreeMap<?, ?>> _lstCC = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lstCC = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                List<CycleCountDTO> lstDto = new ArrayList<CycleCountDTO>();
                                List<String> _lstCCNames = new ArrayList<>();

                                for (int i = 0; i < _lstCC.size(); i++) {
                                    CycleCountDTO dto = new CycleCountDTO(_lstCC.get(i).entrySet());
                                    lstDto.add(dto);
                                }

                                ProgressDialogUtils.closeProgressDialog();

                                for (int i = 0; i < lstDto.size(); i++) {

                                    if (lstDto.get(i).getResult().equals("Closed successfully")) {

                                        common.showUserDefinedAlertType(lstDto.get(i).getResult(), getActivity(), getContext(), "Success");
                                        clearFields();

                                    } else {
                                        common.showUserDefinedAlertType(lstDto.get(i).getResult(), getActivity(), getContext(), "Error");
                                        return;
                                    }

                                }


                            }

                        } catch (Exception ex) {
                            try {
                                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_02", getActivity());
                                logException();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            ProgressDialogUtils.closeProgressDialog();
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
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_03", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
            }
        } catch (Exception ex) {
            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_03", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
        }
    }

    public void clearFields1() {

        ProgressDialogUtils.closeProgressDialog();
        common.setIsPopupActive(false);

        cvScanSKU.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
        ivScanSKU.setImageResource(R.drawable.fullscreen_img);

        lblScannedSku.setText("");
        materialCode = "";
        etExpDate.setText("");
        etMfgDate.setText("");
        etSerial.setText("");
        etBatch.setText("");
        etProjectRef.setText("");
        etCCQty.setText("");
        // To get Storage Locations


        etCCQty.setEnabled(false);
        etCCQty.clearFocus();

        rvPendingCC.setAdapter(null);

        isPalletScanned = false;
        isRSNScanned = false;

    }

    public void clearFields() {

        ProgressDialogUtils.closeProgressDialog();
        common.setIsPopupActive(false);

        cvScanLocation.setCardBackgroundColor(getResources().getColor(R.color.locationColor));
        ivScanLocation.setImageResource(R.drawable.fullscreen_img);

        cvScanSKU.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
        ivScanSKU.setImageResource(R.drawable.fullscreen_img);

        lblScannedSku.setText("");
        materialCode = "";

        etLocation.setText("");
        etExpDate.setText("");
        etMfgDate.setText("");
        etSerial.setText("");
        etBatch.setText("");
        etProjectRef.setText("");
        etCCQty.setText("");

        rvPendingCC.setAdapter(null);

        etCCQty.setEnabled(false);
        etCCQty.clearFocus();

        isValidLocation = false;
        isPalletScanned = false;
        isRSNScanned = false;


    }

    public void GetActivestockStorageLocations() {
        try {
            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.Inventory, getContext());
            InventoryDTO inventoryDTO = new InventoryDTO();

            inventoryDTO.setUserId(userId);
            inventoryDTO.setAccountId(accountId);
            inventoryDTO.setMaterialCode(materialCode);

            message.setEntityObject(inventoryDTO);
            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);
            try {

                ProgressDialogUtils.showProgressDialog("Please Wait");
                call = apiService.GetActivestockStorageLocations(message);

            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_01", getActivity());
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
                        try {

                            if (response.body() != null) {

                                core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);
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
                                    List<LinkedTreeMap<?, ?>> _lstPickitem = new ArrayList<LinkedTreeMap<?, ?>>();
                                    _lstPickitem = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();
                                    _lstOutboundDTO = new ArrayList<InventoryDTO>();
                                    List<String> _lstindound = new ArrayList<>();
                                    InventoryDTO oOutboundDTO = null;
                                    for (int i = 0; i < _lstPickitem.size(); i++) {
                                        oOutboundDTO = new InventoryDTO(_lstPickitem.get(i).entrySet());
                                        _lstOutboundDTO.add(oOutboundDTO);

                                    }
                                    for (int j = 0; j < _lstOutboundDTO.size(); j++) {

                                        // List of store ref no.
                                        _lstindound.add(_lstOutboundDTO.get(j).getStorageLocation());
                                        //_lstindound.add(oOutboundDTO.getStorageLocation());
                                    }
                                    ArrayAdapter arrayAdapter = new ArrayAdapter(getActivity(), R.layout.support_simple_spinner_dropdown_item, _lstindound);
                                    spinnerSelectSloc.setAdapter(arrayAdapter);



                                    ProgressDialogUtils.closeProgressDialog();
                                    return;

                                }
                            } else {
                                ProgressDialogUtils.closeProgressDialog();

                            }
                        } catch (Exception ex) {
                            try {
                                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_02", getActivity());
                                logException();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            ProgressDialogUtils.closeProgressDialog();
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable throwable) {
                        ProgressDialogUtils.closeProgressDialog();
                        DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0001);
                    }
                });
            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_03", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0001);
            }
        } catch (Exception ex) {
            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_04", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0013);
        }
    }



    public void blockLocationForCycleCount() {
        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.CycleCount, getActivity());
            final CycleCountDTO cycleCountDTO = new CycleCountDTO();
            cycleCountDTO.setUserId(userId);
            cycleCountDTO.setAccountID(accountId);
            cycleCountDTO.setCCName(lblCycleCount.getText().toString());
            cycleCountDTO.setLocation(etLocation.getText().toString());
            message.setEntityObject(cycleCountDTO);


            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                ProgressDialogUtils.showProgressDialog("Please Wait");
                call = apiService.BlockLocationForCycleCount(message);

                // } else {
                // DialogUtils.showAlertDialog(getActivity(), "Please enable internet");
                // return;
                // }

            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_01", getActivity());
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


                                }

                                cvScanLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
                                ivScanLocation.setImageResource(R.drawable.warning_img);

                                etLocation.setText("");

                                ProgressDialogUtils.closeProgressDialog();
                                common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                            } else {

                                List<LinkedTreeMap<?, ?>> _lstCC = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lstCC = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                List<CycleCountDTO> lstDto = new ArrayList<CycleCountDTO>();
                                List<String> _lstCCNames = new ArrayList<>();

                                for (int i = 0; i < _lstCC.size(); i++) {
                                    CycleCountDTO dto = new CycleCountDTO(_lstCC.get(i).entrySet());
                                    lstDto.add(dto);
                                }

                                ProgressDialogUtils.closeProgressDialog();

                                for (int i = 0; i < lstDto.size(); i++) {

                                    if (lstDto.get(i).getResult().equals("")) {

                                        //tvCount.setText(lstDto.get(i).getCount());

                                        //common.showUserDefinedAlertType(errorMessages.EMC_0063, getActivity(), getContext(), "Warning");

                                        cvScanLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
                                        ivScanLocation.setImageResource(R.drawable.check);

                                    } else {
                                        common.showUserDefinedAlertType(lstDto.get(i).getResult(), getActivity(), getContext(), "Error");
                                        return;
                                    }

                                }


                            }

                        } catch (Exception ex) {
                            try {
                                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_02", getActivity());
                                logException();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            ProgressDialogUtils.closeProgressDialog();
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
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_03", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
            }
        } catch (Exception ex) {
            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_03", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
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

                call = apiService.LogException(message);

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
                        DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0001);
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
                DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0001);
            }
        } catch (Exception ex) {
            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "002_04", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.title_activity_cycle_count));
    }

    @Override
    public void onDestroyView() {

        // Honeywell onDestroyView
        if (barcodeReader != null) {
            // unregister barcode event listener honeywell
            barcodeReader.removeBarcodeListener((BarcodeReader.BarcodeListener) this);


            barcodeReader.removeTriggerListener((BarcodeReader.TriggerListener) this);
        }

        // Cipher onDestroyView
        Intent RTintent = new Intent("sw.reader.decode.require");
        RTintent.putExtra("Enable", false);
        getActivity().sendBroadcast(RTintent);
        getActivity().unregisterReceiver(this.myDataReceiver);
        super.onDestroyView();

    }

    public void ValiDateMaterial(final String scannedData) {

        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.ScanDTO, getContext());
            ScanDTO scanDTO = new ScanDTO();
            scanDTO.setUserID(userId);
            scanDTO.setAccountID(accountId);
            scanDTO.setTenantID(String.valueOf(tenantId));
            scanDTO.setWarehouseID(String.valueOf(warehouseId));
            scanDTO.setScanInput(scannedData);
            //  scanDTO.setInboundID(inboundId);
            //inboundDTO.setIsOutbound("0");
            message.setEntityObject(scanDTO);


            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                call = apiService.ValiDateMaterial(message);
                ProgressDialogUtils.showProgressDialog("Please Wait");


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
                            isRSNScanned = false;
                            cvScanSKU.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
                            ivScanSKU.setImageResource(R.drawable.fullscreen_img);
                            ProgressDialogUtils.closeProgressDialog();
                            common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                        } else {
                            LinkedTreeMap<?, ?> _lResult = new LinkedTreeMap<>();
                            _lResult = (LinkedTreeMap<?, ?>) core.getEntityObject();

                            Log.v("ABCDE", new Gson().toJson(_lResult));

                            ScanDTO scanDTO1 = new ScanDTO(_lResult.entrySet());
                            ProgressDialogUtils.closeProgressDialog();
                            if (scanDTO1 != null) {
                                if (scanDTO1.getScanResult()) {



                                    cvScanSKU.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
                                    ivScanSKU.setImageResource(R.drawable.fullscreen_img);


                                    materialCode = scanDTO1.getSkuCode();



                                    lblScannedSku.setText(materialCode);
                                    etBatch.setText(scanDTO1.getBatch());
                                    etSerial.setText(scanDTO1.getSerialNumber());
                                    etMfgDate.setText(scanDTO1.getMfgDate());
                                    etExpDate.setText(scanDTO1.getExpDate());
                                    etProjectRef.setText(scanDTO1.getPrjRef());
                                    etCCMRP.setText(scanDTO1.getMrp());

                                    isRSNScanned = true;
                                    cvScanSKU.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanSKU.setImageResource(R.drawable.check);

                                    checkMaterialAvailablilty();

                                } else {
                                    isRSNScanned = false;
                                    cvScanSKU.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanSKU.setImageResource(R.drawable.warning_img);
                                    common.showUserDefinedAlertType(errorMessages.EMC_0009, getActivity(), getContext(), "Warning");
                                }
                            } else {
                                common.showUserDefinedAlertType("Error while getting data", getActivity(), getContext(), "Error");
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
            scanDTO.setTenantID(String.valueOf(tenantId));
            scanDTO.setWarehouseID(String.valueOf(warehouseId));
            scanDTO.setScanInput(scannedData);
            scanDTO.setCycleCount(true);

            message.setEntityObject(scanDTO);


            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {

                call = apiService.ValidateLocation(message);
                ProgressDialogUtils.showProgressDialog("Please Wait");


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

                            isValidLocation = false;
                            cvScanLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
                            ivScanLocation.setImageResource(R.drawable.invalid_cross);
                            ProgressDialogUtils.closeProgressDialog();
                            common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                        } else {
                            LinkedTreeMap<?, ?> _lResult = new LinkedTreeMap<>();
                            _lResult = (LinkedTreeMap<?, ?>) core.getEntityObject();

                            ScanDTO scanDTO1 = new ScanDTO(_lResult.entrySet());

                            if (scanDTO1 != null) {
                                if (scanDTO1.getScanResult()) {
                                    isValidLocation = true;
                                    cvScanLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanLocation.setImageResource(R.drawable.check);
                                    etLocation.setText(scannedData);
                                    isBlockedLocation();
                                } else {
                                    isValidLocation = false;
                                    cvScanLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanLocation.setImageResource(R.drawable.warning_img);
                                    common.showUserDefinedAlertType(errorMessages.EMC_0010, getActivity(), getContext(), "Warning");
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



    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        storageLoc = spinnerSelectSloc.getSelectedItem().toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }


}