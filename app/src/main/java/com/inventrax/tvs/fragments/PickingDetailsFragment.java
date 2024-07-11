package com.inventrax.tvs.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
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

import com.inventrax.tvs.R;
import com.inventrax.tvs.activities.MainActivity;
import com.inventrax.tvs.common.Common;
import com.inventrax.tvs.common.constants.EndpointConstants;
import com.inventrax.tvs.common.constants.ErrorMessages;
import com.inventrax.tvs.interfaces.ApiInterface;
import com.inventrax.tvs.pojos.InventoryDTO;
import com.inventrax.tvs.pojos.OutbountDTO;
import com.inventrax.tvs.pojos.ScanDTO;
import com.inventrax.tvs.pojos.WMSCoreMessage;
import com.inventrax.tvs.pojos.WMSExceptionMessage;
import com.inventrax.tvs.services.RetrofitBuilderHttpsEx;
import com.inventrax.tvs.util.DecimalDigitsInputFilter;
import com.inventrax.tvs.util.DialogUtils;
import com.inventrax.tvs.util.ExceptionLoggerUtils;
import com.inventrax.tvs.util.FragmentUtils;
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
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.annotation.Nullable;
/**
 * Created by Prasanna ch on 06/26/2018.
 */

public class PickingDetailsFragment extends Fragment implements View.OnClickListener, BarcodeReader.TriggerListener, BarcodeReader.BarcodeListener, AdapterView.OnItemSelectedListener {

    private static final String classCode = "API_FRAG_OBD_PICKING";
    private View rootView;
    ImageView ivScanLocation, ivScanPallet, ivScanPalletTo, ivScanRSN, ivScanRSNnew;
    Button btnMaterialSkip, btnPick, btn_Next, btnOk, btnCloseSkip, btnClosefinal,btn_clear;
    TextView lblPickListNo, lblScannedSku, lblHu,tvScanRSN;
    TextView lblSKuNo, lblLocationNo, lblMRP, lblrsnNoNew, lblMfgDate, lblExpDate, lblProjectRefNo, lblRequiredQty, lblserialNo, lblBatchNo,lblCustomerName,lblDockLoc,lblMaterialDescription,txt_BatchNo;
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
    private String pickOBDno = "", pickobdId = "",sku="" ,sugLoc="",customerName="",skuDes="",dock="",pickqty="",requiredqty="",pendingqty="",huNo="",CartonNo,serialNo,driverNo,mfgDate,expDate;
//    psn="",lineNo="",batchNo="",cartonNo="",serialNo="",driverNo="";
    int count = 0;
    private ScanValidator scanValidator;
    private ExceptionLoggerUtils exceptionLoggerUtils;
    private ErrorMessages errorMessages;
    EditText etPallet,etlocation;
    EditText et_oldrsn, et_printQty, et_newrsn, et_printerIP;
    boolean isValidLocation = false;
    boolean isPalletScanned = false;
    boolean isToPalletScanned = false;
    boolean pickValidateComplete = false;
    boolean isRSNScanned = false;
    String assignedId = "", KitId = "", soDetailsId = "", Lineno = "", POSOHeaderId = "", mrp = "", isPSN ="", PSN  ="", sLoc = "", accountId = "", huSize = "",batch;
    int recQty, totalQty, rid;
    //For Honey well barcode
    private static BarcodeReader barcodeReader;
    private AidcManager manager;
    RelativeLayout rlPickList, rlSkip;
    String printer = "Select Printer", skipReason = "", pickedQty = "", location = "";
    List<String> deviceIPList;
    SearchableSpinner spinnerSelectReason;
    SoundUtils soundUtils;
    OutbountDTO oOutboundDTO;
    Double pickedqty;
    Double assignedqty;

   // List<OutbountDTO> = listbundle.getParcelableArrayList("list");

    // Cipher Barcode Scanner
    private final BroadcastReceiver myDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scanner = intent.getStringExtra(GeneralString.BcReaderData);  // Scanned Barcode info
            ProcessScannedinfo(scanner.trim().toString());
        }
    };

    public PickingDetailsFragment() {

    }

    public void myScannedData(Context context, String barcode) {
        try {
            ProcessScannedinfo(barcode.trim());
        } catch (Exception e) {
            //  Toast.makeText(context, ""+e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_picking_details, container, false);
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
        btn_clear= (Button) rootView.findViewById(R.id.btn_clear);
        btn_Next = (Button) rootView.findViewById(R.id.btn_Next);
        btnOk = (Button) rootView.findViewById(R.id.btnOk);
        btnCloseSkip = (Button) rootView.findViewById(R.id.btnCloseSkip);

        lblPickListNo = (TextView) rootView.findViewById(R.id.lblPickListNo);
        lblSKuNo = (TextView) rootView.findViewById(R.id.lblSKUSuggested);
        lblLocationNo = (TextView) rootView.findViewById(R.id.lblLocationSuggested);
        lblMRP = (TextView) rootView.findViewById(R.id.lblMRP);
        lblMaterialDescription = (TextView) rootView.findViewById(R.id.lblMDESCRIPTION);
        txt_BatchNo = (TextView) rootView.findViewById(R.id.txt_BatchNo);
        lblHu = (TextView) rootView.findViewById(R.id.lblHu);
        tvScanRSN = (TextView) rootView.findViewById(R.id.tvScanRSN);
        lblCustomerName = (TextView) rootView.findViewById(R.id.lblCustmerName);
        lblDockLoc = (TextView) rootView.findViewById(R.id.lblDock);

        lblHu.setText("");

        etPallet = (EditText) rootView.findViewById(R.id.etPallet);

        etlocation = (EditText) rootView.findViewById(R.id.etlocation);

        lblReceivedQty = (EditText) rootView.findViewById(R.id.lblReceivedQty);
        lblMfgDate = (TextView) rootView.findViewById(R.id.lblMfgDate);
        lblExpDate = (TextView) rootView.findViewById(R.id.lblExpDate);
        lblProjectRefNo = (TextView) rootView.findViewById(R.id.lblProjectRefNo);
        lblserialNo = (TextView) rootView.findViewById(R.id.lblserialNo);
        lblBatchNo = (TextView) rootView.findViewById(R.id.lblBatchNo);
        lblRequiredQty = (TextView) rootView.findViewById(R.id.lblRequiredQty);
        spinnerSelectReason = (SearchableSpinner) rootView.findViewById(R.id.spinnerSelectReason);
        spinnerSelectReason.setOnItemSelectedListener(this);
        lblReceivedQty.clearFocus();
        lblReceivedQty.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
        lblReceivedQty.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(20, 3)});
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

        gson = new GsonBuilder().create();
        btnPick.setOnClickListener(this);
        btn_Next.setOnClickListener(this);
        btn_clear.setOnClickListener(this);
        btnOk.setOnClickListener(this);
        btnCloseSkip.setOnClickListener(this);
        cvScanPallet.setOnClickListener(this);
        cvScanPalletTo.setOnClickListener(this);

        common = new Common();
        exceptionLoggerUtils = new ExceptionLoggerUtils();
        errorMessages = new ErrorMessages();
        soundUtils = new SoundUtils();
        ProgressDialogUtils.closeProgressDialog();
        common.setIsPopupActive(false);



//        GetPickItem(0);


if (getArguments()!=null) {
    pickOBDno = getArguments().getString("ObdNum");
    sku =getArguments().getString("Sku");
    customerName =getArguments().getString("CustomerName");
    sugLoc =getArguments().getString("SugLoc");
    skuDes =getArguments().getString("SkuDes");
    dock =getArguments().getString("Dock");
    pickedqty= Double.parseDouble(getArguments().getString("pickedQty"));
    assignedqty= Double.parseDouble(getArguments().getString("AssignedQuantity"));
    mrp=getArguments().getString("Mrp");
     pendingqty=getArguments().getString("pendingqty");

    huNo=getArguments().getString("HuNo");
    isPSN=getArguments().getString("IsPSN");
    huSize=getArguments().getString("HUsize");
    PSN=getArguments().getString("PSN");
    Lineno=getArguments().getString("LineNo");
    batch =getArguments().getString("BatchNo");
    CartonNo =getArguments().getString("CartonNo");
    serialNo=getArguments().getString("SerialNo");
    driverNo=getArguments().getString("DriverNo");
    soDetailsId=getArguments().getString("soDetailsId");
    assignedId=getArguments().getString("AssignedID");
    POSOHeaderId=getArguments().getString("pOSOHeaderId");
    pickobdId =getArguments().getString("pickobdId");
    mfgDate =getArguments().getString("MfgDate");
    expDate =getArguments().getString("ExpDate");

//    Pallet=getArguments().getString("PalletNo");
//

    lblSKuNo.setText(sku);
//    .split("[-]", 6)[3]+mrp
    lblPickListNo.setText(pickOBDno);
    lblCustomerName.setText(customerName);
    lblDockLoc.setText(dock);
    lblLocationNo.setText(sugLoc);
    lblMaterialDescription.setText(skuDes);
    lblMaterialDescription.setText(skuDes);
    lblRequiredQty.setText( pickedqty + "/" + assignedqty);
    lblBatchNo.setText(batch);
    txt_BatchNo.setText(batch);
    lblserialNo.setText(serialNo);
    lblMfgDate.setText(mfgDate);
    lblExpDate.setText(expDate);



    /*if ( pendingqty.equals("0.00")) {
//        common.showUserDefinedAlertType("Qty. is already picked", getActivity(), getContext(), "Error");
        common.showUserDefinedAlertType(errorMessages.EMC_090, getActivity(), getContext(), "Success");
        btnPick.setVisibility(View.GONE);
        Bundle bundle = new Bundle();
        bundle.putString("ObdNum", pickOBDno);
        bundle.putString("pickobdId", pickobdId);

        PickingHeaderFragment pickingHeaderFragment = new PickingHeaderFragment();
        pickingHeaderFragment.setArguments(bundle);
        FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, pickingHeaderFragment);


//
   }*/
//

//    UpsertPickItem();

  if(isPSN.equalsIgnoreCase("1")) {
//        oOutboundDTO.setPickedQty("1");
//                isPSN = oOutboundDTO.getIsPSN();
        tvScanRSN.setText("Scan PSN");
        lblReceivedQty.setEnabled(false);
        lblReceivedQty.setText("1");

    }

}

  /*   final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
     builder.setTitle("Quantity is already picked");
     builder.setMessage(R.string.dialog_message);
     builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
         @Override
         public void onClick(DialogInterface dialog, int which) {



         }
     });*/
     /*builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
         @Override public void onClick(DialogInterface dialog, int which) {

             PickingHeaderFragment pickingHeaderFragment=new PickingHeaderFragment();
             FragmentUtils.replaceFragmentWithBackStack(getActivity(),R.id.container_body,pickingHeaderFragment);
         }
     });*/
//     builder.create().show(); // Create the Dialog and display it to the user



        if (scanType.equals("Auto")) {
            btnPick.setEnabled(false);
            btnPick.setTextColor(getResources().getColor(R.color.black));
            btnPick.setBackgroundResource(R.drawable.button_hide);
        } else {
            btnPick.setEnabled(false);
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


//


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {



            case R.id.btnPick:
                if (!etlocation.getText().toString().isEmpty()) {

                    cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
                    ivScanRSN.setImageResource(R.drawable.fullscreen_img);
                    if (isRSNScanned) {
                        WorkOrderPicking();
                        //  UpsertPickItem();
                    } else {
                        common.showUserDefinedAlertType(errorMessages.EMC_0028, getActivity(), getContext(), "Error");
                        return;
                    }

                    /*if (!lblReceivedQty.getText().toString().isEmpty() && !lblReceivedQty.getText().toString().equals("0")) {


                        int asnqty = assignedqty - pickedqty;
                        int qty = Integer.parseInt(lblReceivedQty.getText().toString().split("[.]")[0]);
                        if (  asnqty < qty ) {

                            common.showUserDefinedAlertType("Qty. Should not be more than Pick qty.", getActivity(), getContext(), "Error");

//
                        }
                        else {

                            cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
                            ivScanRSN.setImageResource(R.drawable.fullscreen_img);
                            if (isRSNScanned) {
                                WorkOrderPicking();
                              //  UpsertPickItem();
                            } else {
                                common.showUserDefinedAlertType(errorMessages.EMC_0028, getActivity(), getContext(), "Error");
                                return;
                            }
                            return;
                        }
                    }*/
                } else {
                    common.showUserDefinedAlertType(errorMessages.EMC_0068, getActivity(), getContext(), "Error");
                    return;
                }
                break;
            case R.id.btn_Next:

//                GetPickItem(1);


                break;
            case R.id.btn_clear:
               clearData();
                ClearFields();
//                GetPickItem(1);


                break;

            case R.id.btnOk:

                DialogUtils.showConfirmDialog(getActivity(), "Confirm", "Are you sure to skip this Location? ", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                common.setIsPopupActive(false);

                                if (!skipReason.equals("")) {
                                    // To skip the item and regenerating suggestions
                                    //OBDSkipItem();
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
//                rlPickList.setVisibility(View.VISIBLE);
                rlSkip.setVisibility(View.GONE);
                Bundle bundle = new Bundle();
                bundle.putString("ObdNum", pickOBDno);
                bundle.putString("pickobdId", pickobdId);
                PickingHeaderFragment pickingHeaderFragment = new PickingHeaderFragment();
                pickingHeaderFragment.setArguments(bundle);
                btnOk.setVisibility(View.GONE);

                FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, pickingHeaderFragment);
                ProgressDialogUtils.showProgressDialog("Please Wait");
                break;

            case R.id.cvScanPallet:
                isPalletScanned = true;
                cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.white));
                ivScanPallet.setImageResource(R.drawable.check);
                break;
            case R.id.cvScanPalletTo:
                isToPalletScanned = true;
                cvScanPalletTo.setCardBackgroundColor(getResources().getColor(R.color.white));
                ivScanPalletTo.setImageResource(R.drawable.check);
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
        txt_BatchNo.setText("");
        lblSKuNo.setText("");
        etPallet.setText("");
        etlocation.setText("");
        lblCustomerName.setText("");
        lblMaterialDescription.setText("");
//        lblCustomer.setText("");
//        lblDockLoc.setText("");

      //  lblRequiredQty.setText("");
        lblBatchNo.setText("");
        lblReceivedQty.setText("");
        lblMfgDate.setText("");
        lblExpDate.setText("");
        lblProjectRefNo.setText("");
        lblserialNo.setText("");
        lblMRP.setText("");
        mrp = "";
        lblHu.setText("");
        huNo = "";
        huSize = "";
        rid = 0;

        isPalletScanned = false;
        isValidLocation = false;
        isToPalletScanned = false;
        isRSNScanned = false;
        pickValidateComplete = false;

    }

    public void clearData() {

        etlocation.setText("");
        etPallet.setText("");
        lblLocationNo.setText("");

        isValidLocation = false;
        isPalletScanned = false;
        isToPalletScanned = false;
        isRSNScanned = false;

     //   lblRequiredQty.setText("");

        btnPick.setEnabled(false);

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

        if (((DrawerLayout) getActivity().findViewById(R.id.drawer_layout)).isDrawerOpen(GravityCompat.START)) {
            return;
        }


        if (ProgressDialogUtils.isProgressActive() || Common.isPopupActive()) {
            common.showUserDefinedAlertType(errorMessages.EMC_082, getActivity(), getContext(), "Warning");
            return;
        }

        if (common.isPopupActive() && rlPickList.getVisibility() != View.VISIBLE) {

        } else if (scannedData != null && !common.isPopupActive()) {

            if (!ProgressDialogUtils.isProgressActive()) {
                //if (!lblLocationNo.getText().toString().isEmpty()) {

                    if (!isValidLocation) {
                        /* && lblLocationNo.getText().toString().equalsIgnoreCase(scannedData)*/
                       // if (!lblLocationNo.getText().toString().isEmpty()) {
                          ValidateLocation(scannedData);
                          /*  cvScanLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
                            ivScanLocation.setImageResource(R.drawable.check);*/
                            location = scannedData;
                        //    isValidLocation = true;
                        /*else { }
                            cvScanLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
                            ivScanLocation.setImageResource(R.drawable.warning_img);
                            common.showUserDefinedAlertType(errorMessages.EMC_0033, getActivity(), getContext(), "Warning");
                            // common.showUserDefinedAlertType(errorMessages.EMC_0033+" # "+scannedData+ " # "+lblLocationNo.getText().toString(), getActivity(), getContext(), "Warning");
                        }*/
                    } else {
                        if(!isPalletScanned){
                         //   if (scannedData.equals(etPallet.getText().toString())) {
                               // isPalletScanned = true;
                                ValidatePalletCode(scannedData);
                                cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.white));
                                ivScanPallet.setImageResource(R.drawable.check);
                          /*  } else {
                                common.showUserDefinedAlertType(errorMessages.EMC_0009, getActivity(), getContext(), "Error");
                            }*/
                        }else {
                            ValiDateMaterial(scannedData);
                        }
                       // scannedData = scannedData.split("[-]", 2)[0];

                      /*  if(isPSN.equalsIgnoreCase("1")) {

                            if (ScanValidator.isRSNScanned(scannedData)) {
                                String isCSNAtCharSeven = String.valueOf(scannedData.split("[-]", 2)[1].charAt(7));

                                if(isCSNAtCharSeven.equalsIgnoreCase("C")){
                                    common.showUserDefinedAlertType("CSN scan is not allowed", getActivity(), getContext(), "Warning");
                                    return;
                                }
                                String isCSNAtCharSix = String.valueOf(scannedData.split("[-]", 2)[1].charAt(6));
                                if(isCSNAtCharSix.equalsIgnoreCase("C")){
                                    common.showUserDefinedAlertType("CSN scan is not allowed", getActivity(), getContext(), "Warning");
                                    return;
                                }
                                if(scannedData.split("[-]").length != 2){
                                    common.showUserDefinedAlertType("Please scan USN only", getActivity(), getContext(), "Warning");
                                    return;
                                }else {
                                    lblReceivedQty.setText("1");
                                    PSN = scannedData;
                                    scannedData = scannedData.split("[-]", 2)[0];
                                }

                            }else {
                                common.showUserDefinedAlertType("Please scan USN only", getActivity(), getContext(), "Warning");
                                return;
                            }
                        }*/
                       /*if(scannedData.split("[-]").length != 2){
                            common.showUserDefinedAlertType("Please scan USN only", getActivity(), getContext(), "Warning");
                        }else {*/

                        /* }*/


                    }


              /*  } else {
                    common.showUserDefinedAlertType(errorMessages.EMC_0012, getActivity(), getContext(), "Error");
                }*/
            } else {
                if (!Common.isPopupActive()) {
                    common.showUserDefinedAlertType(errorMessages.EMC_080, getActivity(), getContext(), "Error");

                }
                soundUtils.alertWarning(getActivity(), getContext());
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
                            isRSNScanned = false;
                            PSN= "";
                            cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
                            ivScanRSN.setImageResource(R.drawable.fullscreen_img);
                            ProgressDialogUtils.closeProgressDialog();
                            common.showAlertType(owmsExceptionMessage, getActivity(), getContext());

                        } else {
                            LinkedTreeMap<?, ?> _lResult = new LinkedTreeMap<>();
                            _lResult = (LinkedTreeMap<?, ?>) core.getEntityObject();

                            ScanDTO scanDTO1 = new ScanDTO(_lResult.entrySet());
                            ProgressDialogUtils.closeProgressDialog();
                            if (scanDTO1 != null) {
                                if (scanDTO1.getScanResult()) {

                                /* ----For RSN reference----
                                    0 Sku|1 BatchNo|2 SerialNO|3 MFGDate|4 EXpDate|5 ProjectRefNO|6 Kit Id|7 line No|8 MRP ---- For SKU with 9 MSP's

                                    0 Sku|1 BatchNo|2 SerialNO|3 KitId|4 lineNo  ---- For SKU with 5 MSP's   *//*
                                    // Eg. : ToyCar|1|bat1|ser123|12/2/2018|12/2/2019|0|001*/
                                    lblSKuNo.setText(scanDTO1.getSkuCode());
                                    lblBatchNo.setText(scanDTO1.getBatch());
                                    lblserialNo.setText(scanDTO1.getSerialNumber());
                                    lblMfgDate.setText(scanDTO1.getMfgDate());
                                    lblExpDate.setText(scanDTO1.getExpDate());
                                    lblProjectRefNo.setText(scanDTO1.getPrjRef());

                                    cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
                                    ivScanRSN.setImageResource(R.drawable.fullscreen_img);
                                    lblReceivedQty.setEnabled(true);
                                    //lblReceivedQty.setText(scanDTO1.get);
                                    btnPick.setEnabled(true);
                                    isRSNScanned = true;
                                    GetAvailbleQtyList();
                                    soundUtils.alertWarning(getActivity(), getContext());

                                    DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0073);
                                   /* if (scanDTO1.getSkuCode().equalsIgnoreCase(lblSKuNo.getText().toString().trim().split("[/]")[0] )) {

                                        if ((lblBatchNo.getText().toString().equalsIgnoreCase(scanDTO1.getBatch()) || scanDTO1.getBatch() == null
                                                || scanDTO1.getBatch().equalsIgnoreCase("") || scanDTO1.getBatch().isEmpty()) &&
                                                lblserialNo.getText().toString().equalsIgnoreCase(scanDTO1.getSerialNumber()) &&
                                                lblMfgDate.getText().toString().equalsIgnoreCase(scanDTO1.getMfgDate()) &&
                                                lblExpDate.getText().toString().equalsIgnoreCase(scanDTO1.getExpDate()) &&
                                                lblProjectRefNo.getText().toString().equalsIgnoreCase(scanDTO1.getPrjRef())
                                        ) {

*//*                                           &&
                                             lblMfgDate.getText().toString().equalsIgnoreCase(scanDTO1.getMfgDate()) &&
                                             lblExpDate.getText().toString().equalsIgnoreCase(scanDTO1.getExpDate()
                                             lblProjectRefNo.getText().toString().equalsIgnoreCase(scanDTO1.getPrjRef())*//*


                                            cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
                                            ivScanRSN.setImageResource(R.drawable.fullscreen_img);

                                            isRSNScanned = true;

                                            if (scanType.equalsIgnoreCase("Auto")) {
                                                lblReceivedQty.setText("1");
                                                WorkOrderPicking();
                                            } else {
                                                if(isPSN.equalsIgnoreCase("1")){
                                                    //oOutboundDTO.setPickedQty("1");
//                                                    isPSNoOutboundDTO = oOutboundDTO.getIsPSN();
//                                                    tvScanRSN.setText("Scan PSN");
//                                                    lblReceivedQty.setEnabled(false);
//                                                    lblReceivedQty.setText("1");

                                                    WorkOrderPicking();
                                                }else {
                                                    lblReceivedQty.setEnabled(true);
                                                    btnPick.setEnabled(true);
                                                    soundUtils.alertWarning(getActivity(), getContext());
                                                    DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0073);
                                                }

                                            }
                                        } else {
                                            common.showUserDefinedAlertType(errorMessages.EMC_0079, getActivity(), getContext(), "Error");
                                        }

                                    } else {
                                        isRSNScanned = false;
                                        cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.white));
                                        ivScanRSN.setImageResource(R.drawable.warning_img);
                                        common.showUserDefinedAlertType(errorMessages.EMC_0029, getActivity(), getContext(), "Error");
                                    }*/

                                } else {
                                    // lblScannedSku.setText("");
                                    isRSNScanned = false;
                                    cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanRSN.setImageResource(R.drawable.warning_img);
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

                        //    etPalletTo.setText("");
                            cvScanPalletTo.setCardBackgroundColor(getResources().getColor(R.color.white));
                            ivScanPalletTo.setImageResource(R.drawable.invalid_cross);
                            ProgressDialogUtils.closeProgressDialog();
                            common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                        } else {
                            LinkedTreeMap<?, ?> _lResult = new LinkedTreeMap<>();
                            _lResult = (LinkedTreeMap<?, ?>) core.getEntityObject();

                            ScanDTO scanDTO1 = new ScanDTO(_lResult.entrySet());
                            ProgressDialogUtils.closeProgressDialog();
                            if (scanDTO1 != null) {
                                if (scanDTO1.getScanResult()) {
                                   // etPalletTo.setText(scannedData);
                                    //ValidatePalletCode();
                                    isToPalletScanned = true;
                                    cvScanPalletTo.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanPalletTo.setImageResource(R.drawable.check);
                                } else {
                                    isToPalletScanned = false;
                                   // etPalletTo.setText("");
                                    cvScanPalletTo.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanPalletTo.setImageResource(R.drawable.warning_img);
                                    common.showUserDefinedAlertType(errorMessages.EMC_0009, getActivity(), getContext(), "Warning");
                                }
                            } else {
                                isToPalletScanned = false;
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
                            LinkedTreeMap<?, ?> _lResult = new LinkedTreeMap<>();
                            _lResult = (LinkedTreeMap<?, ?>) core.getEntityObject();

                            ScanDTO scanDTO1 = new ScanDTO(_lResult.entrySet());

                            if (scanDTO1 != null) {
                                if (scanDTO1.getScanResult()) {
                                    cvScanLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanLocation.setImageResource(R.drawable.check);
                                    location = scannedData;
                                    isValidLocation = true;
                                    etlocation.setText(scannedData);
                                   /* if (!lblLocationNo.getText().toString().isEmpty() && lblLocationNo.getText().toString().equalsIgnoreCase(scannedData)) {
                                        cvScanLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
                                        ivScanLocation.setImageResource(R.drawable.check);
                                        location = scannedData;
                                        isValidLocation = true;
                                    } else {
                                        cvScanLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
                                        ivScanLocation.setImageResource(R.drawable.warning_img);
                                        common.showUserDefinedAlertType(errorMessages.EMC_0033, getActivity(), getContext(), "Warning");
                                    }*/
                                } else {
                                    cvScanLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanLocation.setImageResource(R.drawable.warning_img);
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


    public void GetPickItem(int fetchNextItem) {
        //To get Picked item Details
        try {
            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.Outbound, getContext());
            final OutbountDTO outbountDTO = new OutbountDTO();
            outbountDTO.setUserId(userId);
            outbountDTO.setAccountID(accountId);
            outbountDTO.setOutboundID(pickobdId);
            outbountDTO.setRID(rid);

            outbountDTO.setFetchNextItem(fetchNextItem);
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
//                                     lblSKuNo.setText(oOutboundDTO.getSKU()) + "-" + lblMRP.setText(oOutboundDTO.getMRP());
                                    //lblSKuNo.setText(oOutboundDTO.getSKU()+ " - " +oOutboundDTO.getMRP());
                                    lblSKuNo.setText(oOutboundDTO.getSKU());
                                    assignedId = "" + oOutboundDTO.getAssignedID();
                                    soDetailsId = "" + oOutboundDTO.getSODetailsID();
                                    KitId = "" + oOutboundDTO.getAssignedID();
                                    lblBatchNo.setText(oOutboundDTO.getBatchNo());
                                    lblLocationNo.setText(oOutboundDTO.getLocation());
                                    etPallet.setText(oOutboundDTO.getPalletNo());
                                    pickedQty = oOutboundDTO.getPickedQty();
                                    lblCustomerName.setText(oOutboundDTO.getCustomerName());
                                    lblMaterialDescription.setText(oOutboundDTO.getMaterialDescription());
                                    rid = oOutboundDTO.getRID();
                                    mrp = oOutboundDTO.getMRP();
                                    lblMRP.setText(mrp +"/-");

                                    lblDockLoc.setText(oOutboundDTO.getDockLocation());

                                    huNo = oOutboundDTO.getHUNo();
                                    huSize = oOutboundDTO.getHUSize();
                                    if (!huSize.equals("1")) {
                                        lblHu.setText("Hu: " + "" + huNo + "/" + huSize);
                                    }else {
                                        lblHu.setText("");
                                    }

                                    if(oOutboundDTO.getIsPSN().equalsIgnoreCase("1")){
                                        isPSN=oOutboundDTO.getIsPSN();
                                        tvScanRSN.setText("Scan PSN");
                                        lblReceivedQty.setEnabled(false);
                                        lblReceivedQty.setText("1");



                                    }
                                  /*  if ( pendingqty.equals("0.00")) {
                                        common.showUserDefinedAlertType("Qty. is already picked", getActivity(), getContext(), "Error");
                                        btnPick.setVisibility(View.GONE);
                                        Bundle bundle = new Bundle();
                                        bundle.putString("ObdNum", pickOBDno);
                                        bundle.putString("pickobdId", pickobdId);

                                        PickingHeaderFragment pickingHeaderFragment = new PickingHeaderFragment();
                                        pickingHeaderFragment.setArguments(bundle);
                                        FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, pickingHeaderFragment);
                                    }*/
                                    else {
                                        isPSN="";
                                        tvScanRSN.setText(getString(R.string.sku));
                                        lblReceivedQty.setEnabled(false);
                                        lblReceivedQty.setText("");
                                        lblReceivedQty.setEnabled(false);
                                        lblReceivedQty.clearFocus();

                                    }

                                    if (lblLocationNo.getText().toString().equals("")) {
                                        common.showUserDefinedAlertType(errorMessages.EMC_0063 + lblPickListNo.getText().toString(), getActivity(), getContext(), "Success");
                                        return;
                                    }

                                    lblRequiredQty.setText(oOutboundDTO.getPickedQty() + "/" + oOutboundDTO.getAssignedQuantity());

                                    recQty = Integer.parseInt(oOutboundDTO.getPickedQty().split("[.]")[0]);
                                    totalQty = Integer.parseInt(oOutboundDTO.getAssignedQuantity().split("[.]")[0]);

                                    lblMfgDate.setText(oOutboundDTO.getMfgDate());
                                    lblExpDate.setText(oOutboundDTO.getExpDate());
                                    lblProjectRefNo.setText(oOutboundDTO.getProjectNo());
                                    lblserialNo.setText(oOutboundDTO.getSerialNo());
                                    // lblMRP.setText(oOutboundDTO.getMRP());
                                    mrp = oOutboundDTO.getMRP();

                                    cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
                                    ivScanRSN.setImageResource(R.drawable.fullscreen_img);

                                    if (!lblLocationNo.getText().toString().equals(location)) {    // if location is not same as previously picked location

                                        isValidLocation = false;
                                        isPalletScanned = false;
                                        location = "";

                                        btnPick.setEnabled(false);

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

                                        lblRequiredQty.setText(oOutboundDTO.getPickedQty() + "/" + oOutboundDTO.getAssignedQuantity());

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

    public void ValidatePalletCode(final String pallet) {

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

                                   // if (isPalletScanned) {
                                        cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.white));
                                        ivScanPallet.setImageResource(R.drawable.invalid_cross);
                                        etPallet.setText("");
                                        isPalletScanned = false;

                                 //   } /*else {
                                        cvScanPalletTo.setCardBackgroundColor(getResources().getColor(R.color.white));
                                        ivScanPalletTo.setImageResource(R.drawable.invalid_cross);
                                    etPallet.setText("");
                                   // }*/
                                    ProgressDialogUtils.closeProgressDialog();
                                    common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                                }

                            } else {
                                List<LinkedTreeMap<?, ?>> _lPalletInventory = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lPalletInventory = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();
                                if (_lPalletInventory != null) {
                                    if (_lPalletInventory.size() > 0) {
                                        cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.white));
                                        ivScanPallet.setImageResource(R.drawable.check);
                                        isPalletScanned=true;
                                        etPallet.setText(pallet);
                                       /* if (isPalletScanned && etPalletTo.getText().toString().isEmpty()) {
                                            cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.white));
                                            ivScanPallet.setImageResource(R.drawable.check);
                                        } else {

                                            cvScanPalletTo.setCardBackgroundColor(getResources().getColor(R.color.white));
                                            ivScanPalletTo.setImageResource(R.drawable.check);
                                        }*/
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

    /*public void SkipItem() {
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
    }*/

    public void WorkOrderPicking() {

        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.Outbound, getContext());
            OutbountDTO oOutboundDTO = new OutbountDTO();
            oOutboundDTO.setUserId(userId);
            oOutboundDTO.setAccountID(accountId);
            oOutboundDTO.setSKU(lblSKuNo.getText().toString().split("[/]")[0] );
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
            oOutboundDTO.setLocation(etlocation.getText().toString());
            oOutboundDTO.setPalletNo(etPallet.getText().toString());

            if(isPSN.equalsIgnoreCase("1")) {
                oOutboundDTO.setPickedQty("1");
//                isPSN = oOutboundDTO.getIsPSN();
//                tvScanRSN.setText("Scan PSN");
//                lblReceivedQty.setEnabled(false);
//                lblReceivedQty.setText("1");
            }
            else
                oOutboundDTO.setPickedQty(lblReceivedQty.getText().toString());
            oOutboundDTO.setAssignedID(assignedId);
            oOutboundDTO.setToCartonNo("");
            oOutboundDTO.setSODetailsID(soDetailsId);
            oOutboundDTO.setLineno(Lineno);
            oOutboundDTO.setMRP("");
            oOutboundDTO.setpOSOHeaderId(POSOHeaderId);
            oOutboundDTO.setHUNo(huNo);
            oOutboundDTO.setPSN(PSN);
            oOutboundDTO.setHUSize(huSize);
            oOutboundDTO.setHasDis("0");
            oOutboundDTO.setIsDam("0");
            oOutboundDTO.setIsPSN(isPSN);
            message.setEntityObject(oOutboundDTO);
            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                ProgressDialogUtils.showProgressDialog("Please Wait");
                call = apiService.WorkOrderPicking(message);

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

//                                cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.white));
//                                ivScanRSN.setImageResource(R.drawable.warning_img);
//                                tvScanRSN.setText("Scan PSN");
                                lblReceivedQty.setText("");
                                lblReceivedQty.setEnabled(false);
                                btnPick.setEnabled(false);

                                common.setIsPopupActive(true);
                                common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                                if (owmsExceptionMessage.getWMSExceptionCode().equals("EMC_OB_DAL_PICKSugg_EC02") ||
                                        owmsExceptionMessage.getWMSExceptionCode().equals("EMC_OB_DAL_PICKSugg_EC03") ||
                                        owmsExceptionMessage.getWMSExceptionCode().equals("EMC_OB_DAL_PICKSugg_EC01") ||
                                        owmsExceptionMessage.getWMSExceptionCode().equals("EMC_OB_DAL_PICKSugg_EC04") ||
                                        owmsExceptionMessage.getWMSExceptionCode().equals("EMC_IN_DAL_001")) {
                                }
                                if (owmsExceptionMessage.getWMSExceptionCode().equals("EMC_OB_DAL_010")) {

                                    cvScanPalletTo.setCardBackgroundColor(getResources().getColor(R.color.palletColor));
                                    ivScanPalletTo.setImageResource(R.drawable.fullscreen_img);

                                 //   etPalletTo.setText("");

                                }

                                isRSNScanned = false;

                            } else {

                                List<LinkedTreeMap<?, ?>> _lstPickitem = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lstPickitem = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                OutbountDTO oOutboundDTO = null;

                                for (int i = 0; i < _lstPickitem.size(); i++) {
                                    oOutboundDTO = new OutbountDTO(_lstPickitem.get(i).entrySet());

                                }


                             lblReceivedQty.setText("");
                             lblReceivedQty.setEnabled(false);
                             lblReceivedQty.clearFocus();



                             lblRequiredQty.setText(oOutboundDTO.getPickedQty()+ "/" + oOutboundDTO.getAssignedQuantity());


                             soundUtils.alertSuccess(getActivity(), getContext());


                             pickedqty = Double.parseDouble(oOutboundDTO.getPickedQty());
                             assignedqty = Double.parseDouble(oOutboundDTO.getAssignedQuantity());

                             ProgressDialogUtils.closeProgressDialog();

                             isRSNScanned = false;


                              /*  if (oOutboundDTO.getPendingQty().equals("0")) {

                                    lblRequiredQty.setText(oOutboundDTO.getPendingQty());
                                    lblReceivedQty.setText("");
                                    lblSKuNo.setText("");


                                    // Added to clear data after completion of the outbound
                                    ClearFields();
                                    clearData();

                                    common.showUserDefinedAlertType(errorMessages.EMC_0071, getActivity(), getContext(), "Success");


                                }*/
//                                if(oOutboundDTO.getIsPSN().equalsIgnoreCase("1")) {
//                                    isPSN = oOutboundDTO.getIsPSN();
//                                    tvScanRSN.setText("Scan PSN");
//                                    lblReceivedQty.setEnabled(false);
//                                    lblReceivedQty.setText("1");
//                                }
                               /* else {

                                    lblReceivedQty.setText("");
                                    lblReceivedQty.setEnabled(false);
                                    lblReceivedQty.clearFocus();

//                                    cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.white));
//                                    ivScanRSN.setImageResource(R.drawable.check);

                                    lblRequiredQty.setText(oOutboundDTO.getPickedQty().split("[.]")[0] + "/" + oOutboundDTO.getAssignedQuantity().split("[.]")[0]);


                                    soundUtils.alertSuccess(getActivity(), getContext());


                                   *//* if (oOutboundDTO.getPendingQty().equals("0.00")) {
//                                        common.showUserDefinedAlertType("Qty. is already picked", getActivity(), getContext(), "Error");
                                        common.showUserDefinedAlertType(errorMessages.EMC_090, getActivity(), getContext(), "Success");
                                        btnPick.setVisibility(View.GONE);
                                        Bundle bundle = new Bundle();
                                        bundle.putString("ObdNum", pickOBDno);
                                        bundle.putString("pickobdId", pickobdId);

                                        PickingHeaderFragment pickingHeaderFragment = new PickingHeaderFragment();
                                        pickingHeaderFragment.setArguments(bundle);
                                        FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, pickingHeaderFragment);


                                    }*//*
//                                    GetPickItem(0);
                                }
*/

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

   /* public void OBDSkipItem() {

        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.Outbound, getContext());
            //final OutbountDTO outbountDTO = new OutbountDTO();
            int reqQty = totalQty - recQty;
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
            oOutboundDTO.setLocation(lblLocationNo.getText().toString());
            oOutboundDTO.setPalletNo(etPallet.getText().toString());
            oOutboundDTO.setSkipQty(String.valueOf(reqQty));
            oOutboundDTO.setPickedQty(lblReceivedQty.getText().toString());
            oOutboundDTO.setAssignedID(assignedId);
            oOutboundDTO.setsLoc(sLoc);
            oOutboundDTO.setHUSize(huSize);
            oOutboundDTO.setHUNo(huNo);
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
                                    isToPalletScanned = false;

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

                                        lblRequiredQty.setText(oOutboundDTO.getPendingQty());
                                        lblReceivedQty.setText("");
                                        lblSKuNo.setText("");

                                        // Added to clear data after completion of the outbound
                                        ClearFields();
                                        clearData();

                                        common.showUserDefinedAlertType(errorMessages.EMC_0063 + lblPickListNo.getText().toString(), getActivity(), getContext(), "Success");

                                    } else {

                                        sLoc = "" + oOutboundDTO.getsLoc();
                                        POSOHeaderId = "" + oOutboundDTO.getpOSOHeaderId();
                                        Lineno = "" + oOutboundDTO.getLineno();
                                        lblSKuNo.setText(oOutboundDTO.getSKU());
                                        assignedId = "" + oOutboundDTO.getAssignedID();
                                        soDetailsId = "" + oOutboundDTO.getSODetailsID();
                                        KitId = "" + oOutboundDTO.getAssignedID();
                                        lblBatchNo.setText(oOutboundDTO.getBatchNo());
                                        lblLocationNo.setText(oOutboundDTO.getLocation());
                                        etPallet.setText(oOutboundDTO.getPalletNo());
                                        lblRequiredQty.setText(oOutboundDTO.getPickedQty().split("[.]")[0] + "/" + oOutboundDTO.getAssignedQuantity().split("[.]")[0]);
                                        lblMfgDate.setText(oOutboundDTO.getMfgDate());
                                        lblExpDate.setText(oOutboundDTO.getExpDate());
                                        lblProjectRefNo.setText(oOutboundDTO.getProjectNo());
                                        lblserialNo.setText(oOutboundDTO.getSerialNo());
                                        lblMRP.setText(oOutboundDTO.getMRP());

                                        huNo = oOutboundDTO.getHUNo();
                                        huSize = oOutboundDTO.getHUSize();
                                        if (!huSize.equals("1")) {
                                            lblHu.setText("Hu: " + "" + huNo + "/" + huSize);
                                        }else {
                                            lblHu.setText("");
                                        }

                                        rlPickList.setVisibility(View.VISIBLE);
                                        rlSkip.setVisibility(View.GONE);


                                        common.showUserDefinedAlertType(errorMessages.EMC_0077, getActivity(), getContext(), "Success");

                                    }

                                    ProgressDialogUtils.closeProgressDialog();
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
    }*/

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
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.picking));
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
        super.onDestroyView();

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        skipReason = spinnerSelectReason.getSelectedItem().toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }


    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(int pos);
    }

    public void GetAvailbleQtyList() {
        try {
            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.Inventory, getContext());
            InventoryDTO inventoryDTO = new InventoryDTO();

            inventoryDTO.setUserId(userId);
            inventoryDTO.setAccountId(accountId);
            inventoryDTO.setMaterialCode(lblSKuNo.getText().toString());
            inventoryDTO.setSLOC("");
//            if (storageloc.equalsIgnoreCase("SLOC")) {
//
//            } else {
//                inventoryDTO.setSLOC(storageloc);
//            }
            inventoryDTO.setLocationCode(etlocation.getText().toString());
            inventoryDTO.setContainerCode(etPallet.getText().toString());
            inventoryDTO.setMfgDate(lblMfgDate.getText().toString());
            inventoryDTO.setExpDate(lblExpDate.getText().toString());
            inventoryDTO.setSerialNo(lblserialNo.getText().toString());
            inventoryDTO.setBatchNo(lblBatchNo.getText().toString());
            inventoryDTO.setProjectNo(lblProjectRefNo.getText().toString());
            inventoryDTO.setMRP(lblMRP.getText().toString());
            inventoryDTO.setTenantID("3");
            inventoryDTO.setWarehouseId("5");
            inventoryDTO.setIsWorkOrder("1");

            message.setEntityObject(inventoryDTO);
            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);
            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                ProgressDialogUtils.showProgressDialog("Please Wait");
                call = apiService.GetAvailbleQtyList(message);
//                GetAvailbleQtyList
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

                                        lblReceivedQty.setEnabled(false);
                                        lblReceivedQty.setText("");

                                        //isSKUScanned = false;
                                        //etSku.setText("");

                                        cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.white));
                                        ivScanRSN.setImageResource(R.drawable.invalid_cross);

                                        //GetBinToBinStorageLocations();
                                        common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                                        return;
                                    }
                                } else {
                                    List<LinkedTreeMap<?, ?>> _lstPickitem = new ArrayList<LinkedTreeMap<?, ?>>();
                                    _lstPickitem = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();
                                    List<OutbountDTO> _lstOutboundDTO = new ArrayList<OutbountDTO>();
                                    InventoryDTO oOutboundDTO = null;
                                    for (int i = 0; i < _lstPickitem.size(); i++) {
                                        oOutboundDTO = new InventoryDTO(_lstPickitem.get(i).entrySet());
                                    }
                                    lblReceivedQty.setText(oOutboundDTO.getQuantity());


                                   /* if (scanType.equalsIgnoreCase("Auto")) {
                                        etQty.setText("1");
                                        isSKUScanned = true;


                                    } else {
                                        etQty.setEnabled(true);
                                        isSKUScanned = true;

                                    }*/

                                   /* cvScanSku.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanSku.setImageResource(R.drawable.check);

                                    spinnerSelectSloc.setEnabled(true);*/

                                    ProgressDialogUtils.closeProgressDialog();


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

}