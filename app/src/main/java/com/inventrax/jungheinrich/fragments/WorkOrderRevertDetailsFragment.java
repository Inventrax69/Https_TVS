package com.inventrax.jungheinrich.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

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

import com.inventrax.jungheinrich.R;
import com.inventrax.jungheinrich.activities.MainActivity;
import com.inventrax.jungheinrich.common.Common;
import com.inventrax.jungheinrich.common.constants.EndpointConstants;
import com.inventrax.jungheinrich.common.constants.ErrorMessages;
import com.inventrax.jungheinrich.interfaces.ApiInterface;
import com.inventrax.jungheinrich.pojos.InventoryDTO;
import com.inventrax.jungheinrich.pojos.OutbountDTO;
import com.inventrax.jungheinrich.pojos.ScanDTO;
import com.inventrax.jungheinrich.pojos.WMSCoreMessage;
import com.inventrax.jungheinrich.pojos.WMSExceptionMessage;
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


/**
 * Created by Padmaja Rani.B on 19/12/2018
 */

public class WorkOrderRevertDetailsFragment extends Fragment implements View.OnClickListener,
        BarcodeReader.TriggerListener, BarcodeReader.BarcodeListener, View.OnLongClickListener {

    private static final String classCode = "API_FRAG_GOODSIN";
    private View rootView;
    private TextView  lblSKU, lblScannedSku, lblOBD,etSerial, etMfgDate, etExpDate, etBatch,lblRequiredQty;
    private CardView cvScanPallet, cvScanSku, cvScanDock;
    private ImageView ivScanPallet, ivScanSku, ivScanDock;
    private TextInputLayout txtInputLayoutPallet, txtInputLayoutDock;
    private EditText etPallet, etQty,  etDock;

    private Button btnClear, btnRevert, btnClose;
    DialogUtils dialogUtils;
    FragmentUtils fragmentUtils;
    private Common common = null;
    String scanner = null;
    String getScanner = null;
    private IntentFilter filter;
    private ScanValidator scanValidator;
    private Gson gson;
    private WMSCoreMessage core;
    private String Materialcode = null, huNo = "", huSize = "";
    private String userId = null, scanType = null, accountId = null, lineNo = null,OBDId="",OBDNo="",projectRef="", VLPDId="",
            receivedQty = null, pendingQty = null, dock = "", vehicleNo = "";
    String storageLoc = null, inboundId = null, invoiceQty = null, recQty = "";
    int warehouseID = 0, tenantID = 0;
    SoundUtils sound = null;
    private ExceptionLoggerUtils exceptionLoggerUtils;
    private ErrorMessages errorMessages;
    private boolean isInboundCompleted = false, isDockScanned = false, isContanierScanned = false, isRsnScanned = false;
    SoundUtils soundUtils;
    String supplierInvoiceDetailsId = "";
    //For Honey well barcode
    private static BarcodeReader barcodeReader;
    private AidcManager manager;
    List<InventoryDTO> _lstOutboundDTO;
    OutbountDTO outbountDTO;
    Double poType;
    double PickedQty=0.0, RevertedQty=0.0;

    int fivePercentValueOfTotal = 0;

    private final BroadcastReceiver myDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scanner = intent.getStringExtra(GeneralString.BcReaderData);  // Scanned Barcode info
            ProcessScannedinfo(scanner.trim().toString());
        }
    };


    public void myScannedData(Context context, String barcode) {
        try {
            ProcessScannedinfo(barcode.trim());
        } catch (Exception e) {
            //  Toast.makeText(context, ""+e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public WorkOrderRevertDetailsFragment() {
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_work_order_revert_details, container, false);
        barcodeReader = MainActivity.getBarcodeObject();
        loadFormControls();
        return rootView;

    }

    // Form controls
    private void loadFormControls() {

        lblSKU = (TextView) rootView.findViewById(R.id.lblSKU);
        lblScannedSku = (TextView) rootView.findViewById(R.id.lblScannedSku);
        lblOBD = (TextView) rootView.findViewById(R.id.lblOBD);


        cvScanPallet = (CardView) rootView.findViewById(R.id.cvScanPallet);
        cvScanSku = (CardView) rootView.findViewById(R.id.cvScanSku);
        cvScanDock = (CardView) rootView.findViewById(R.id.cvScanDock);

        ivScanPallet = (ImageView) rootView.findViewById(R.id.ivScanPallet);
        ivScanSku = (ImageView) rootView.findViewById(R.id.ivScanSku);
        ivScanDock = (ImageView) rootView.findViewById(R.id.ivScanDock);

        txtInputLayoutPallet = (TextInputLayout) rootView.findViewById(R.id.txtInputLayoutPallet);
        txtInputLayoutDock = (TextInputLayout) rootView.findViewById(R.id.txtInputLayoutDock);
        etPallet = (EditText) rootView.findViewById(R.id.etPallet);
        etSerial = (TextView) rootView.findViewById(R.id.etSerial);
        etMfgDate = (TextView) rootView.findViewById(R.id.etMfgDate);
        etBatch = (TextView) rootView.findViewById(R.id.etBatch);
        lblRequiredQty= (TextView) rootView.findViewById(R.id.lblRequiredQty);
        etExpDate = (TextView) rootView.findViewById(R.id.etExpDate);
        etQty = (EditText) rootView.findViewById(R.id.etQty);
        etDock = (EditText) rootView.findViewById(R.id.etDock);


        btnClear = (Button) rootView.findViewById(R.id.btnClear);
        btnRevert = (Button) rootView.findViewById(R.id.btnRevert);
        btnClose = (Button) rootView.findViewById(R.id.btnClose);
        etQty.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    mainActivity.barcode = "";
                    return true;
                }
                return false;
            }
        });
        etQty.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(20, 2)});
        SharedPreferences sp = getActivity().getSharedPreferences("LoginActivity", Context.MODE_PRIVATE);
        userId = sp.getString("RefUserId", "");
        scanType = sp.getString("scanType", "");
        accountId = sp.getString("AccountId", "");
        warehouseID = sp.getInt("WarehouseID", 0);
        tenantID = sp.getInt("TenantID", 0);
        btnClear.setOnClickListener(this);
        btnRevert.setOnClickListener(this);
        cvScanPallet.setOnClickListener(this);
        btnClose.setOnClickListener(this);
        if (scanType.equals("Auto")) {
            btnRevert.setEnabled(false);
            btnRevert.setTextColor(getResources().getColor(R.color.black));
            btnRevert.setBackgroundResource(R.drawable.button_hide);
        } else {
            btnRevert.setEnabled(true);
            btnRevert.setTextColor(getResources().getColor(R.color.white));
            btnRevert.setBackgroundResource(R.drawable.button_shape);
        }

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
        if (getArguments()!=null) {
             outbountDTO = (OutbountDTO) getArguments().getSerializable("Outbound");
            OBDId =getArguments().getString("OBDId");
            OBDNo =getArguments().getString("OBDNo");

             lblOBD.setText(OBDNo);
             lblSKU.setText(outbountDTO.getSKU());
            etBatch.setText(outbountDTO.getBatchNo());
             etSerial.setText(outbountDTO.getSerialNo());
            etMfgDate.setText(outbountDTO.getMfgDate());
            etExpDate.setText(outbountDTO.getExpDate());
            lblRequiredQty.setText(outbountDTO.getRevertQty()+"/"+outbountDTO.getQty());
            projectRef= outbountDTO.getProjectNo();
            PickedQty= Double.parseDouble(outbountDTO.getQty());
            RevertedQty=Double.parseDouble(outbountDTO.getRevertQty());
            VLPDId= outbountDTO.getvLPDId();
        }


       /* int invoice_Qty = (int) Double.parseDouble(invoiceQty);
        int rec_qty = (int) Double.parseDouble(recQty);
        int percentValue = (int) (invoice_Qty * (5.0f / 100.0f));
        fivePercentValueOfTotal = invoice_Qty + percentValue;
        pendingQty = String.valueOf((double) (fivePercentValueOfTotal));
        if (poType==5.0){
            cbExcess.setChecked(true);
            cbExcess.setVisibility(View.VISIBLE);
        }else {
            if (rec_qty == fivePercentValueOfTotal) {
                inboundCompleted();
                cbExcess.setChecked(false);
                return;
            }
        }*/


       /* if (recQty.equals(invoiceQty) || (Double.parseDouble(recQty) > Double.parseDouble(invoiceQty))) {
            inboundCompleted();
            cbExcess.setVisibility(View.VISIBLE);
            cbExcess.setEnabled(true);
        }*/





    }



    //button Clicks
    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btnClear:
                clearFields();
                break;



            case R.id.btnClose:
               /* UnloadingFragment unloadingFragment = new UnloadingFragment();
                FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, unloadingFragment);*/
                break;

            case R.id.btnRevert:

                if (isDockScanned) {

                    if (isContanierScanned) {

                        if (!lblScannedSku.getText().toString().isEmpty() && !Materialcode.equals("")) {

                            UpsertHHTWORevert();

                        } else {
                            common.showUserDefinedAlertType(errorMessages.EMC_0028, getActivity(), getContext(), "Warning");
                            return;
                        }
                    } else {
                        common.showUserDefinedAlertType(errorMessages.EMC_088, getActivity(), getContext(), "Warning");
                        return;
                    }
                } else {
                    common.showUserDefinedAlertType(errorMessages.EMC_0018, getActivity(), getContext(), "Warning");
                    return;
                }

                break;

            default:
                break;
        }
    }

    public void clearFields() {

        cvScanSku.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
        ivScanSku.setImageResource(R.drawable.fullscreen_img);

        cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.palletColor));
        ivScanPallet.setImageResource(R.drawable.fullscreen_img);

        cvScanDock.setCardBackgroundColor(getResources().getColor(R.color.locationColor));
        ivScanDock.setImageResource(R.drawable.fullscreen_img);
        etPallet.setText("");
        etQty.setText("");
        lblScannedSku.setText("");

        isDockScanned = false;
        etDock.setText("");

        isDockScanned = false;
        isContanierScanned = false;

        /*
        btnRevert.setEnabled(false);
        btnRevert.setTextColor(getResources().getColor(R.color.black));
        btnRevert.setBackgroundResource(R.drawable.button_hide);
        */


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

        if (((DrawerLayout) getActivity().findViewById(R.id.drawer_layout)).isDrawerOpen(GravityCompat.START)) {
            return;
        }

        if (ProgressDialogUtils.isProgressActive() || Common.isPopupActive()) {
            common.showUserDefinedAlertType(errorMessages.EMC_082, getActivity(), getContext(), "Warning");
            return;
        }

        if (scannedData != null && !Common.isPopupActive()) {

            if (!ProgressDialogUtils.isProgressActive()) {

                if (!isDockScanned) {
                    ValidateLocation(scannedData);
                } else {
                    if (!isContanierScanned) {
                        ValidatePallet(scannedData);
                    } else {
                        if (ScanValidator.isRSNScanned(scannedData)) {
                            scannedData = scannedData.split("[-]", 2)[0];
                            lblScannedSku.setText(scannedData);
                        }
                        ValiDateMaterial(scannedData);
                       /* if(receivedQty !=null) {

                            if ((int) Double.parseDouble(receivedQty) < fivePercentValueOfTotal) {



                            } else {
                                common.showUserDefinedAlertType(errorMessages.EMC_0075, getActivity(), getContext(), "Warning");
                                return;
                            }
                        }else {
                            if ((int) Double.parseDouble(recQty) < fivePercentValueOfTotal) {

                                ValiDateMaterial(scannedData);

                            } else {
                                common.showUserDefinedAlertType(errorMessages.EMC_0075, getActivity(), getContext(), "Warning");
                                return;
                            }
                        }*/




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
            scanDTO.setObdNumber(OBDNo);
            //inboundDTO.setIsOutbound("0");
            message.setEntityObject(scanDTO);

            Log.v("ABCDE", new Gson().toJson(message));

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

                            cvScanSku.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
                            ivScanSku.setImageResource(R.drawable.fullscreen_img);
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

                                /* ----For RSN reference----
                                    0 Sku|1 BatchNo|2 SerialNO|3 MFGDate|4 EXpDate|5 ProjectRefNO|6 Kit Id|7 line No|8 MRP ---- For SKU with 9 MSP's

                                    0 Sku|1 BatchNo|2 SerialNO|3 KitId|4 lineNo  ---- For SKU with 5 MSP's   *//*
                                    // Eg. : ToyCar|1|bat1|ser123|12/2/2018|12/2/2019|0|001*/


                                    cvScanSku.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanSku.setImageResource(R.drawable.check);
//                                    cvScanDock.setCardBackgroundColor(getResources().getColor(R.color.white));
//                                    ivScanDock.setImageResource(R.drawable.check);
                                    /*    if (scannedData.split("[|]").length != 5) {*/

                                    Materialcode = scanDTO1.getSkuCode();




                                    lblScannedSku.setText(Materialcode);


                                    //   etMRP.setText(scannedData.split("[|]")[7]);


/*                                    } else {
                                        Materialcode = scannedData.split("[|]")[0];
                                        etBatch.setText(scannedData.split("[|]")[1]);
                                        etSerial.setText(scannedData.split("[|]")[2]);
                                        etKidID.setText(scannedData.split("[|]")[3]);
                                        lineNo = scannedData.split("[|]")[4];
                                    }*/
//

                                    if (scanType.equals("Auto")) {
                                        etQty.setText("1.00");
                                              // To get the pending and received quantities
                                        return;
                                    } else {
                                        // for Manual mode
                                        etQty.setEnabled(true);
                                        btnRevert.setEnabled(true);
                                        btnRevert.setTextColor(getResources().getColor(R.color.white));
                                        btnRevert.setBackgroundResource(R.drawable.button_shape);

                                           // To get the pending and received quantities
                                    }
                                } else {
                                    isRsnScanned = true;
                                    lblScannedSku.setText("");
                                    cvScanSku.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanSku.setImageResource(R.drawable.warning_img);
                                    common.showUserDefinedAlertType(errorMessages.EMC_0009, getActivity(), getContext(), "Warning");
                                }
                            } else {
                                isRsnScanned = true;
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
            // scanDTO.setTenantID(String.valueOf(tenantID));
            // scanDTO.setWarehouseID(String.valueOf(warehouseID));
            scanDTO.setScanInput(scannedData);
            scanDTO.setInboundID(inboundId);
            // inboundDTO.setIsOutbound("0");
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

                            isDockScanned = false;
                            etDock.setText("");
                            cvScanDock.setCardBackgroundColor(getResources().getColor(R.color.white));
                            ivScanDock.setImageResource(R.drawable.invalid_cross);
                            ProgressDialogUtils.closeProgressDialog();
                            common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                        } else {
                            LinkedTreeMap<?, ?> _lResult = new LinkedTreeMap<>();
                            _lResult = (LinkedTreeMap<?, ?>) core.getEntityObject();

                            ScanDTO scanDTO1 = new ScanDTO(_lResult.entrySet());

                            if (scanDTO1 != null) {
                                if (scanDTO1.getScanResult()) {
                                    etDock.setText(scannedData);
                                    isDockScanned = true;
                                    cvScanDock.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanDock.setImageResource(R.drawable.check);
                                } else {
                                    isDockScanned = false;
                                    etDock.setText("");
                                    cvScanDock.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanDock.setImageResource(R.drawable.warning_img);
                                    common.showUserDefinedAlertType(errorMessages.EMC_0010, getActivity(), getContext(), "Warning");
                                }
                            } else {
                                isDockScanned = false;
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
            //scanDTO.setWarehouseID(String.valueOf(warehouseID));
            scanDTO.setScanInput(scannedData);
            scanDTO.setInboundID(inboundId);
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

                            etPallet.setText("");
                            cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.white));
                            ivScanPallet.setImageResource(R.drawable.invalid_cross);
                            ProgressDialogUtils.closeProgressDialog();
                            common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                        } else {
                            LinkedTreeMap<?, ?> _lResult = new LinkedTreeMap<>();
                            _lResult = (LinkedTreeMap<?, ?>) core.getEntityObject();

                            ScanDTO scanDTO1 = new ScanDTO(_lResult.entrySet());
                            ProgressDialogUtils.closeProgressDialog();
                            if (scanDTO1 != null) {
                                if (scanDTO1.getScanResult()) {
                                    etPallet.setText(scannedData);
                                    //ValidatePalletCode();
                                    isContanierScanned = true;
                                    cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanPallet.setImageResource(R.drawable.check);
                                } else {
                                    isContanierScanned = false;
                                    etPallet.setText("");
                                    cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanPallet.setImageResource(R.drawable.warning_img);
                                    common.showUserDefinedAlertType(errorMessages.EMC_0009, getActivity(), getContext(), "Warning");
                                }
                            } else {
                                isContanierScanned = false;
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






    // for auto mode


    // Separated for getReceivedQty() method for manual mode



    public void UpsertHHTWORevert() {



        if (etQty.getText().toString().isEmpty()) {
            common.showUserDefinedAlertType(errorMessages.EMC_0067, getActivity(), getContext(), "Error");
            return;
        }

        double EnteredQty = Double.parseDouble(etQty.getText().toString());
        if (EnteredQty>PickedQty) {
            common.showUserDefinedAlertType(errorMessages.EMC_0068, getActivity(), getContext(), "Error");
            return;
        }

        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.Outbound, getContext());
            OutbountDTO outbountDTO1 = new OutbountDTO();
            outbountDTO1.setSKU(Materialcode);

            outbountDTO1.setCartonNo(etPallet.getText().toString());


            outbountDTO1.setUserId(userId);
            outbountDTO1.setAccountID(accountId);
            if (scanType.equals("Manual")) {
                outbountDTO1.setQty(etQty.getText().toString());
            } else {
                outbountDTO1.setQty("1");
            }
            outbountDTO1.setBatchNo(etBatch.getText().toString());
            outbountDTO1.setSerialNo(etSerial.getText().toString());
            outbountDTO1.setMfgDate(etMfgDate.getText().toString());
            outbountDTO1.setExpDate(etExpDate.getText().toString());
            outbountDTO1.setOutboundID(OBDId);
            outbountDTO1.setLocation(etDock.getText().toString());
            outbountDTO1.setProjectNo(projectRef);
            outbountDTO1.setvLPDId(VLPDId);

            message.setEntityObject(outbountDTO1);
            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                ProgressDialogUtils.showProgressDialog("Please Wait");
                call = apiService.UpsertHHTWORevert(message);
                // } else {
                // DialogUtils.showAlertDialog(getActivity(), "Please enable internet");
                // return;
                // }

            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "003_01", getActivity());
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
                            if (core != null) {

                                core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);

                                if ((core.getType().toString().equals("Exception"))) {
                                    List<LinkedTreeMap<?, ?>> _lExceptions = new ArrayList<LinkedTreeMap<?, ?>>();
                                    _lExceptions = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                    WMSExceptionMessage owmsExceptionMessage = null;
                                    for (int i = 0; i < _lExceptions.size(); i++) {

                                        owmsExceptionMessage = new WMSExceptionMessage(_lExceptions.get(i).entrySet());

                                        cvScanSku.setCardBackgroundColor(getResources().getColor(R.color.white));
                                        ivScanSku.setImageResource(R.drawable.warning_img);

                                        etQty.setText("");

                                        ProgressDialogUtils.closeProgressDialog();
                                        common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                                        return;
                                    }

                                } else {
                                    List<LinkedTreeMap<?, ?>> _lINB = new ArrayList<LinkedTreeMap<?, ?>>();
                                    _lINB = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                    OutbountDTO dto = null;
                                    ProgressDialogUtils.closeProgressDialog();

                                    for (int i = 0; i < _lINB.size(); i++) {

                                        dto = new OutbountDTO(_lINB.get(i).entrySet());

                                        if (dto.getResult().equals("1")) {

                                            double pendingQty= Double.parseDouble(dto.getPendingQty());
                                            if (pendingQty<=0){

                                                Bundle bundle = new Bundle();
                                                bundle.putString("OBDId",OBDId);
                                                bundle.putString("OBDNo",OBDNo);

                                                common.showUserDefinedAlertType("Item Completely Reverted", getActivity(), getContext(), "Success");
                                                WorkorderRevertFragment workorderRevertFragment = new WorkorderRevertFragment();
                                                workorderRevertFragment.setArguments(bundle);
                                                FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, workorderRevertFragment);
                                               // common.showUserDefinedAlertType("Item Completely Reverted", getActivity(), getContext(), "Success");

                                            }else {
                                                 PickedQty = Double.parseDouble(dto.getPendingQty());
                                                RevertedQty = Double.parseDouble(dto.getRevertQty());
                                                lblRequiredQty.setText(dto.getRevertQty()+"/"+dto.getQty());

                                            }





                                                soundUtils.alertSuccess(getActivity(), getContext());
                                                return;


                                        } else {
                                            cvScanSku.setCardBackgroundColor(getResources().getColor(R.color.white));
                                            ivScanSku.setImageResource(R.drawable.invalid_cross);
                                            common.showUserDefinedAlertType(dto.getResult(), getActivity(), getContext(), "Error");
                                        }
                                    }
                                }
                            }
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

                    @Override
                    public void onFailure(Call<String> call, Throwable throwable) {
                        //Toast.makeText(LoginActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
                        ProgressDialogUtils.closeProgressDialog();
                        DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0001);
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
                DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0001);
            }
        } catch (Exception ex) {
            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "003_04", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0002);
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
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Work Order Revert");
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
        super.onDestroyView();
    }




    @Override
    public boolean onLongClick(View view) {
        return false;
    }

}