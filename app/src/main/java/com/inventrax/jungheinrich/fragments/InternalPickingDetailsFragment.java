package com.inventrax.jungheinrich.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
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
import com.inventrax.jungheinrich.adapters.InternalPickingExpandableListAdapter;
import com.inventrax.jungheinrich.common.Common;
import com.inventrax.jungheinrich.common.constants.EndpointConstants;
import com.inventrax.jungheinrich.common.constants.ErrorMessages;
import com.inventrax.jungheinrich.interfaces.ApiInterface;
import com.inventrax.jungheinrich.pojos.InventoryDTO;
import com.inventrax.jungheinrich.pojos.OutbountDTO;
import com.inventrax.jungheinrich.pojos.WMSCoreMessage;
import com.inventrax.jungheinrich.pojos.WMSExceptionMessage;
import com.inventrax.jungheinrich.searchableSpinner.SearchableSpinner;
import com.inventrax.jungheinrich.services.RestService;
import com.inventrax.jungheinrich.services.RetrofitBuilderHttpsEx;
import com.inventrax.jungheinrich.util.DialogUtils;
import com.inventrax.jungheinrich.util.ExceptionLoggerUtils;
import com.inventrax.jungheinrich.util.FragmentUtils;
import com.inventrax.jungheinrich.util.ProgressDialogUtils;
import com.inventrax.jungheinrich.util.ScanValidator;
import com.inventrax.jungheinrich.util.SoundUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.annotation.Nullable;

public class InternalPickingDetailsFragment extends Fragment implements View.OnClickListener, BarcodeReader.TriggerListener, BarcodeReader.BarcodeListener, AdapterView.OnItemSelectedListener {

    private static final String classCode = "API_FRAG_PutAwayHeaderFragment_";
    private View rootView;
    private ScanValidator scanValidator;
    public RelativeLayout rlPutawaySuggestions;
    private TextView lblSuggestedText, lbldesc, lblScanAllPallets,lbl_qty;
    private Button btnPickComplete,btnClose;
    private ExpandableListView expListView;
    //For Honey well barcode
    private static BarcodeReader barcodeReader;
    private AidcManager manager;
    private WMSCoreMessage core;
    private Common common = null;
    List<String> lstPalletCodes;
    InternalPickingExpandableListAdapter listAdapter = null;
    private ExceptionLoggerUtils exceptionLoggerUtils;
    private ErrorMessages errorMessages;
    String userId = null;
    String scanner = null;
    String getScanner = null;
    String RegeneratePalletcode = null;
    private Gson gson;
    private boolean IsFetchSuggestions = false;
    private IntentFilter filter;
    public List<String> PalletnumberHeaderList = null;
    public HashMap<String, List<String>> LocaMaterialInfoList = null;
    List<String> MaterialQty = null;
    private SoundUtils soundUtils;
    String MaterialCode, BatchNo, Quantity ="",VLPDId,pickOBDID,reftypevalue="";
    ArrayAdapter arrayAdapterPickList;
    SearchableSpinner spinnerSelectPickList;
    private String pickRefNo = "", pickobdId;
    List<String> lstObdIds;
    List<String> lstLocMaterialQty;
    HashMap<String, List<String>> lstLocaMaterialInfo;
    List<String> lstPalletnumberHeader;
    CheckBox cbmoreitems;
    String isMoreOptions="0";
    // Cipher Barcode Scanner
    private final BroadcastReceiver myDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scanner = intent.getStringExtra(GeneralString.BcReaderData);  // Scanned Barcode info
            ProcessScannedinfo(scanner.trim().toString());
        }
    };

    public void myScannedData(Context context, String barcode){
        try {
            ProcessScannedinfo(barcode.trim());
        }catch (Exception e){
            //  Toast.makeText(context, ""+e.toString(), Toast.LENGTH_SHORT).show();
        }
    }
    public InternalPickingDetailsFragment() {

    }

    public boolean IsPutaway = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_internal_picking_details, container, false);
        //        barcodeReader = MainActivity.getBarcodeObject();
        loadFormControls();

        return rootView;
    }

    /// Loading form Controls
    private void loadFormControls() {

        rlPutawaySuggestions = (RelativeLayout) rootView.findViewById(R.id.rlPutawaySuggestions);

        exceptionLoggerUtils = new ExceptionLoggerUtils();
        SharedPreferences sp = getActivity().getSharedPreferences("LoginActivity", Context.MODE_PRIVATE);
        userId = sp.getString("RefUserId", "");
        MaterialQty = new ArrayList<>();
        btnPickComplete = (Button) rootView.findViewById(R.id.btnPickComplete);
        btnClose = (Button) rootView.findViewById(R.id.btnClose);
        lblScanAllPallets = (TextView) rootView.findViewById(R.id.lblScanAllPallets);
        cbmoreitems= (CheckBox) rootView.findViewById(R.id.cbmoreitems);

        lbl_qty = (TextView) rootView.findViewById(R.id.lbl_qty);

        btnPickComplete.setOnClickListener(this);
        btnClose.setOnClickListener(this);
        spinnerSelectPickList = (SearchableSpinner) rootView.findViewById(R.id.spinnerSelectPickList);
        spinnerSelectPickList.setOnItemSelectedListener(this);
        expListView = (ExpandableListView) rootView.findViewById(R.id.lvExp);
        LocaMaterialInfoList = new HashMap<String, List<String>>();
        PalletnumberHeaderList = new ArrayList<String>();
        lstPalletCodes = new ArrayList<String>();
        common = new Common();
        gson = new GsonBuilder().create();
        errorMessages = new ErrorMessages();
        soundUtils = new SoundUtils();
        common.setIsPopupActive(false);
        // For Cipher Barcode reader
        Intent RTintent = new Intent("sw.reader.decode.require");
        RTintent.putExtra("Enable", true);
        getActivity().sendBroadcast(RTintent);
        this.filter = new IntentFilter();
        this.filter.addAction("sw.reader.decode.complete");
        getActivity().registerReceiver(this.myDataReceiver, this.filter);


        if (getArguments() != null) {

            if (getArguments().getString("MaterialCode") != null){

                MaterialCode = getArguments().getString("MaterialCode");
                BatchNo = getArguments().getString("BatchNo");
                Quantity = getArguments().getString("Quantity");
                VLPDId=getArguments().getString("VLPDId");
                pickOBDID=getArguments().getString("OBDId");
                reftypevalue =getArguments().getString("reftypevalue");
            }

            if (getArguments().getString("mcode") != null){
              Quantity =getArguments().getString("PickQuantity");
                MaterialCode = getArguments().getString("mcode");
                BatchNo = getArguments().getString("batchNo");
                reftypevalue =  getArguments().getString("reftypevalue");
                VLPDId=getArguments().getString("vldpID");
                pickOBDID =getArguments().getString("pickOBDID");


            }
              if (Quantity!=null) {
                  lbl_qty.setText("Qty" + " " + Quantity);
              }else {
                  lbl_qty.setText("Qty" + " " + "0.0");
              }

            if (reftypevalue.equalsIgnoreCase("0")){
                cbmoreitems.setVisibility(View.GONE);
            }

            GetLocationsBySKU(BatchNo,MaterialCode,userId);


             /*listAdapter = new ExpandableListAdapter(getActivity(), getContext(), PalletnumberHeaderList, LocaMaterialInfoList);
             expListView.setAdapter(listAdapter);*/
            // GetPutawaysuggesttions();
            ProgressDialogUtils.closeProgressDialog();
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


        cbmoreitems.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {


                if (cbmoreitems.isChecked()) {
                    isMoreOptions="1";

                     GetLocationsBySKU(BatchNo,MaterialCode,userId);
                } else {
                    isMoreOptions="0";
                    GetLocationsBySKU(BatchNo,MaterialCode,userId);

                }
            }
        });

    }

    //Button clicks
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnPickComplete:
                confirmPickComplete();
                break;
            case R.id.btnClose:
               Bundle bundle =new Bundle();
               bundle.putString("OBDid",pickOBDID);
               bundle.putString("reftypevalue",reftypevalue);
                InternalPickingHeaderFragment internalPickingHeaderFragment = new InternalPickingHeaderFragment();
                internalPickingHeaderFragment.setArguments(bundle);
                FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, internalPickingHeaderFragment);
                break;

        }


    }


    // Honeywell Barcode read event
    @Override
    public void onBarcodeEvent(final BarcodeReadEvent barcodeReadEvent) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // update UI to reflect the data
                getScanner = barcodeReadEvent.getBarcodeData();
               // ProcessScannedinfo(getScanner);

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


    //Assigning scanned value to the respective fields
    public void ProcessScannedinfo(String scannedData) {
        if (scannedData != null && !Common.isPopupActive) {
            //mahi
            //            ispalletisscanned();
            if (ScanValidator.isContainerScanned(scannedData)) {
                lblScanAllPallets.setText(scannedData);
               // GetPutawaysuggesttions();

            }
            else {
                common.showUserDefinedAlertType(errorMessages.EMC_0019, getActivity(), getContext(), "Error");

            }
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
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Internal Transfer");
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




      public void  confirmPickComplete(){
        DialogUtils.showConfirmDialog(getActivity(), "Pick Complete", "Are you sure to complete this Pick?", "Yes", "No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        common.setIsPopupActive(false);
                        WorkOrderLineItemComplete();
                        dialog.dismiss();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        common.setIsPopupActive(false);
                        dialog.dismiss();
                        break;
                }

            }
        });

      }
    /*
          Generating the Suggestions based on Scanned pallet
     */
    public void GetLocationsBySKU(String batchNo,String materialCode, String userId) {
        WMSCoreMessage message = new WMSCoreMessage();
        message = common.SetAuthentication(EndpointConstants.Outbound, getContext());
        OutbountDTO outbountDTO = new OutbountDTO();
        outbountDTO.setBatchNo(batchNo);//BatchNo
        outbountDTO.setmCode(materialCode);//MaterialCode
        outbountDTO.setUserId(userId);//userId
        outbountDTO.setIsWorkOrder(reftypevalue);
        outbountDTO.setIsMoreOptions(isMoreOptions);
        message.setEntityObject(outbountDTO);


        Call<String> call = null;
        ApiInterface apiService = RestService.getClient().create(ApiInterface.class);

        try {
            //Checking for Internet Connectivity
            // if (NetworkUtils.isInternetAvailable()) {
            // Calling the Interface method
            ProgressDialogUtils.showProgressDialog("Please Wait");
            call = apiService.GetLocationsBySKU(message);
            // } else {
            // DialogUtils.showAlertDialog(getActivity(), "Please enable internet");
            // return;
            // }

        } catch (Exception ex) {
            try {
                ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "GeneratePutawaySuggestions_01", getActivity());
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
                        if (core != null) {
                            if ((core.getType().toString().equals("Exception"))) {
                                List<LinkedTreeMap<?, ?>> _lExceptions = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lExceptions = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                WMSExceptionMessage owmsExceptionMessage = null;
                                for (int i = 0; i < _lExceptions.size(); i++) {
                                    owmsExceptionMessage = new WMSExceptionMessage(_lExceptions.get(i).entrySet());
                                }

                                common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                                ProgressDialogUtils.closeProgressDialog();
                            } else {
                                List<LinkedTreeMap<?, ?>> _lInventory = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lInventory = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();
                                //Converting List of Inventory
                                List<InventoryDTO> lstInventory = new ArrayList<InventoryDTO>();
                                //Header for Adapter
                               lstPalletnumberHeader = new ArrayList<String>();
                                List<String> lstTenantIDHeader = new ArrayList<String>();
                                //child for Hashmap
                                lstLocaMaterialInfo = new HashMap<String, List<String>>();

                                Log.v("ABCDE",new Gson().toJson(core));

                                InventoryDTO inventorydto = null;
                                //Iterating through the Inventory list
                                for (int i = 0; i < _lInventory.size(); i++) {

                                    // Each individual value of the map into DTO
                                    inventorydto = new InventoryDTO(_lInventory.get(i).entrySet());
                                    //Adding Inventory Dto into List
                                    lstInventory.add(inventorydto);
                                }




                                List<OutbountDTO> lstMaterilinfo = new ArrayList<OutbountDTO>();
                               lstLocMaterialQty = new ArrayList<String>();


                                //Iterating to List of Inventory and Mapping it into  Local Model class
                                for (InventoryDTO inventoryDTO : lstInventory) {
                                    OutbountDTO materialInfo = new OutbountDTO();
                                    materialInfo.setMaterialCode(inventoryDTO.getMaterialCode());
                                    materialInfo.setQuantity(inventoryDTO.getQuantity());
                                    materialInfo.setBatchNo(inventoryDTO.getBatchNo());
                                    materialInfo.setLocation(inventoryDTO.getLocationCode());
                                    lstMaterilinfo.add(materialInfo);
                                }

                                // added Material Information into list
                                // Setting Material information to Palletinfo

                                // Add pallet info to List

                                // Adding Pallet no to the Header list


                                for (int i = 0; i < lstMaterilinfo.size(); i++) {
                                    lstLocMaterialQty.add(lstMaterilinfo.get(i).getLocation() + "/" +
                                             lstMaterilinfo.get(i).getQuantity()+ "/" + lstMaterilinfo.get(i).getBatchNo() + "/" +lstMaterilinfo.get(i).getMaterialCode() + "/"  +pickRefNo );
                                }
                                GetDockLocations();
                                lstPalletnumberHeader.add(MaterialCode);
                                lstLocaMaterialInfo.put(MaterialCode, lstLocMaterialQty);
                                  loadExpandableData( lstPalletnumberHeader,lstLocaMaterialInfo,pickRefNo);

                                // Adding header and Child to Map

                                // Passing header and Child to the Adapters
                                // Passing header and Child to the Adapters



                            }

                            // Listview Group collasped listener

                        }
                    } catch (Exception ex) {
                        try {
                            ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "GeneratePutawaySuggestions_02", getActivity());
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
                    common.showUserDefinedAlertType(errorMessages.EMC_0002, getActivity(), getContext(), "Error");
                }
            });
        } catch (Exception ex) {
            try {
                ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "GeneratePutawaySuggestions_03", getActivity());
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
            ApiInterface apiService = RestService.getClient().create(ApiInterface.class);

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
                        //Toast.makeText(LoginActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
                        ProgressDialogUtils.closeProgressDialog();
                        common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
                    }
                });
            } catch (Exception ex) {
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
            }
        } catch (Exception ex) {
            ProgressDialogUtils.closeProgressDialog();
            common.showUserDefinedAlertType(errorMessages.EMC_0003, getActivity(), getContext(), "Error");
        }
    }



    public void GetDockLocations() {

        try {
            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.Outbound, getContext());
            OutbountDTO outbountDTO = new OutbountDTO();
            outbountDTO.setUserId(userId);

            message.setEntityObject(outbountDTO);

            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                // Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                ProgressDialogUtils.showProgressDialog("Please Wait");
                call = apiService.GetDockLocations(message);
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
                            core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);
                            if ((core.getType().toString().equals("Exception"))) {
                                List<LinkedTreeMap<?, ?>> _lExceptions = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lExceptions = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();
                                WMSExceptionMessage owmsExceptionMessage = null;
                                for (int i = 0; i < _lExceptions.size(); i++) {
                                    owmsExceptionMessage = new WMSExceptionMessage(_lExceptions.get(i).entrySet());
                                    ProgressDialogUtils.closeProgressDialog();
                                }
                                DialogUtils.showAlertDialog(getActivity(), owmsExceptionMessage.getWMSMessage());
                            } else {
                                List<LinkedTreeMap<?, ?>> _lPickRefNo = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lPickRefNo = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();
                                List<String> lstPickRefNo = new ArrayList<>();
                                lstObdIds = new ArrayList<>();
                                List<InventoryDTO> lstDto = new ArrayList<InventoryDTO>();
                                for (int i = 0; i < _lPickRefNo.size(); i++) {
                                    InventoryDTO dto = new InventoryDTO(_lPickRefNo.get(i).entrySet());
                                    lstDto.add(dto);
                                }
                                lstPickRefNo.add("Select Location");
                                for (int i = 0; i < lstDto.size(); i++) {
                                    lstPickRefNo.add(String.valueOf(lstDto.get(i).getLocationCode()));
                                    lstObdIds.add(String.valueOf(lstDto.get(i).getLocationID()));
                                }
                                ProgressDialogUtils.closeProgressDialog();
                                arrayAdapterPickList = new ArrayAdapter(getActivity(), R.layout.support_simple_spinner_dropdown_item, lstPickRefNo);
                                spinnerSelectPickList.setAdapter(arrayAdapterPickList);

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

                    // response object fails
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
            DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0003);
        }
    }
    public void WorkOrderLineItemComplete() {


        try {
            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.Inventory, getContext());
            OutbountDTO outbountDTO = new OutbountDTO();
            outbountDTO.setvLPDId(VLPDId);
            outbountDTO.setOutboundID(pickOBDID);
            message.setEntityObject(outbountDTO);

            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                // Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                ProgressDialogUtils.showProgressDialog("Please Wait");
                call = apiService.WorkOrderLineItemComplete(message);
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
                            core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);
                            if ((core.getType().toString().equals("Exception"))) {
                                List<LinkedTreeMap<?, ?>> _lExceptions = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lExceptions = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();
                                WMSExceptionMessage owmsExceptionMessage = null;
                                for (int i = 0; i < _lExceptions.size(); i++) {
                                    owmsExceptionMessage = new WMSExceptionMessage(_lExceptions.get(i).entrySet());
                                    ProgressDialogUtils.closeProgressDialog();
                                }
                                DialogUtils.showAlertDialog(getActivity(), owmsExceptionMessage.getWMSMessage());
                            } else {
                                List<LinkedTreeMap<?, ?>> _lOutbound = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lOutbound = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();
                                //Converting List of Inventory
                                final List<OutbountDTO> lstOutbound = new ArrayList<OutbountDTO>();

                                //Header for Adapter
                                List<String> lstnumberHeader = new ArrayList<String>();
//
                                Log.v("ABCDE",new Gson().toJson(core));

                                OutbountDTO outbountDTO2 = null;
                                //Iterating through the Inventory list
                                if (_lOutbound!=null) {
                                    for (int i = 0; i < _lOutbound.size(); i++) {

                                        // Each individual value of the map into DTO
                                        outbountDTO2 = new OutbountDTO(_lOutbound.get(i).entrySet());
                                        //Adding Inventory Dto into List
                                        lstOutbound.add(outbountDTO2);
                                    }
                                           if (lstOutbound.size()>0) {
                                               Bundle bundle = new Bundle();
                                               bundle.putSerializable("SKUList", (Serializable) lstOutbound);

                                               common.showUserDefinedAlertType("Pick Completed", getActivity(), getContext(), "Success");
                                               InternalPickingHeaderFragment internalPickingHeaderFragment = new InternalPickingHeaderFragment();
                                               internalPickingHeaderFragment.setArguments(bundle);

                                               FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, internalPickingHeaderFragment);
                                           }else {
                                               DialogUtils.showAlertDialog(getActivity(), "No Data Found");
                                               InternalPickingHeaderFragment internalPickingHeaderFragment = new InternalPickingHeaderFragment();
                                               FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, internalPickingHeaderFragment);
                                               common.showUserDefinedAlertType("Pick Completed", getActivity(), getContext(), "Success");

                                           }

                                }else {

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

                    // response object fails
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
            DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0003);
        }
    }
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (!spinnerSelectPickList.getSelectedItem().toString().equalsIgnoreCase("Select Location")) {
            pickRefNo = spinnerSelectPickList.getSelectedItem().toString();
            pickobdId = lstObdIds.get(position - 1).toString();
            loadExpandableData(lstPalletnumberHeader, lstLocaMaterialInfo, pickRefNo);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {



    }
    public  void loadExpandableData( List<String> lstPalletnumberHeader, HashMap<String, List<String>> lstLocaMaterialInfo,String pickRefNo){
        listAdapter = new InternalPickingExpandableListAdapter(getActivity(), getContext(), lstPalletnumberHeader, lstLocaMaterialInfo,pickRefNo,Quantity,reftypevalue,VLPDId,pickOBDID);
        expListView.setAdapter(listAdapter);
        expListView.expandGroup(0);
        ProgressDialogUtils.closeProgressDialog();
        expListView.setOnGroupCollapseListener(new OnGroupCollapseListener() {
            @Override
            public void onGroupCollapse(int groupPosition) {

            }
        });
    }

}
