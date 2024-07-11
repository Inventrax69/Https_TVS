
package com.inventrax.tvs.fragments;
import android.content.BroadcastReceiver;
import android.content.Context;
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
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.inventrax.tvs.adapters.WorkOrderAdapter;
import com.inventrax.tvs.adapters.PickingExpandableListAdapter;
import com.inventrax.tvs.common.Common;
import com.inventrax.tvs.common.constants.EndpointConstants;
import com.inventrax.tvs.common.constants.ErrorMessages;
import com.inventrax.tvs.interfaces.ApiInterface;
import com.inventrax.tvs.pojos.OutbountDTO;
import com.inventrax.tvs.pojos.ScanDTO;
import com.inventrax.tvs.pojos.WMSCoreMessage;
import com.inventrax.tvs.pojos.WMSExceptionMessage;
import com.inventrax.tvs.services.RestService;
import com.inventrax.tvs.services.RetrofitBuilderHttpsEx;
import com.inventrax.tvs.util.DialogUtils;
import com.inventrax.tvs.util.ExceptionLoggerUtils;
import com.inventrax.tvs.util.FragmentUtils;
import com.inventrax.tvs.util.ProgressDialogUtils;
import com.inventrax.tvs.searchableSpinner.SearchableSpinner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WorkorderRevertFragment extends Fragment implements View.OnClickListener, BarcodeReader.TriggerListener,
        BarcodeReader.BarcodeListener {

    private static final String classCode = "API_FRAG_020";
    private View rootView;
    Button btnGo,btnClear;
    SearchableSpinner spinnerSelectreftype;
    private IntentFilter filter;
    private Gson gson;
    private Common common;
    private WMSCoreMessage core;
    private ExceptionLoggerUtils exceptionLoggerUtils;
    private ErrorMessages errorMessages;
    private String userId = null;
    String accountId = null;
    private String pickRefNo = "", pickobdId, refType = "" ,reftypevalue ="0";
    List<String> lstObdIds;
    String scanner = null;
    String getScanner = null;
    //For Honey well barcode
    private static BarcodeReader barcodeReader;
    private AidcManager manager;
    ArrayAdapter arrayAdapterPickList;
    ArrayAdapter arrayAdapterRefType;
    CardView cvScanSONumber;
    ImageView ivScanSONumber;
    boolean isValidSO=false;
    PickingExpandableListAdapter listAdapter = null;
    private ExpandableListView expListView;
    RecyclerView pickingRecyclerview;
    WorkOrderAdapter WorkOrderAdapter;
    RelativeLayout rlPickListOne;
    TextView pickingoOdNo,txt_obd;
    private LinearLayout llPickingSugg,llsku;
    List<OutbountDTO> myoutboundlist;

    public WorkorderRevertFragment() {}

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_workorder_revert, container, false);
        loadFormControls();
        return rootView;
    }

    private void loadFormControls() {

        SharedPreferences sp = getActivity().getSharedPreferences("LoginActivity", Context.MODE_PRIVATE);
        userId = sp.getString("RefUserId", "");
        accountId = sp.getString("AccountId", "");

        myoutboundlist    = new ArrayList<OutbountDTO>();
        cvScanSONumber = (CardView) rootView.findViewById(R.id.cvScanSONumber);
        ivScanSONumber = (ImageView) rootView.findViewById(R.id.ivScanSONumber);

        spinnerSelectreftype = (SearchableSpinner) rootView.findViewById(R.id.spinnerSelectreftype);
//        spinnerSelectreftype.setOnItemSelectedListener(this);

        btnGo = (Button) rootView.findViewById(R.id.btnGo);
        btnClear = (Button) rootView.findViewById(R.id.btnClear);

        llPickingSugg = (LinearLayout) rootView.findViewById(R.id.llPickingSugg);
        llsku=(LinearLayout) rootView.findViewById(R.id.llsku);

        gson = new GsonBuilder().create();
        btnGo.setOnClickListener(this);
        btnClear.setOnClickListener(this);

        common = new Common();
        exceptionLoggerUtils = new ExceptionLoggerUtils();
        errorMessages = new ErrorMessages();

        pickingRecyclerview= (RecyclerView) rootView.findViewById(R.id.pickingRecyclerView);
        pickingoOdNo= (TextView) rootView.findViewById(R.id.pickingoOdNo);
        txt_obd= (TextView) rootView.findViewById(R.id.txt_obd);

        rlPickListOne=(RelativeLayout)rootView.findViewById(R.id.rlPickListOne);
        ProgressDialogUtils.closeProgressDialog();
        common.setIsPopupActive(false);


        List<String> refTypes =new ArrayList<>();
        refTypes.add("Work Order");
        refTypes.add("Sampling");

     /*   arrayAdapterRefType = new ArrayAdapter(getActivity(), R.layout.support_simple_spinner_dropdown_item, refTypes);
        spinnerSelectreftype.setAdapter(arrayAdapterRefType);*/


        spinnerSelectreftype.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                pickRefNo = spinnerSelectreftype.getSelectedItem().toString();
                pickobdId =lstObdIds.get(position).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        LoadPickRefnos();
        // For Cipher Barcode reader
        Intent RTintent = new Intent("sw.reader.decode.require");
        RTintent.putExtra("Enable", true);
        getActivity().sendBroadcast(RTintent);
        this.filter = new IntentFilter();
        this.filter.addAction("sw.reader.decode.complete");
        getActivity().registerReceiver(this.myDataReceiver, this.filter);


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


        if (getArguments()!=null) {
          /*  pickRefNo = getArguments().getString("ObdNum");
            pickobdId = getArguments().getString("pickobdId");
*/



            if (getArguments().getString("OBDId")!=null) {
                pickobdId = getArguments().getString("OBDId");
                pickRefNo = getArguments().getString("OBDNo");
                pickingRecyclerview.setVisibility(View.VISIBLE);
                rlPickListOne.setVisibility(View.GONE);
                pickingoOdNo.setVisibility(View.VISIBLE);
                txt_obd.setVisibility(View.VISIBLE);
                llsku.setVisibility(View.VISIBLE);
                pickingoOdNo.setText(pickRefNo);
                GetSkuList();
            }
         /*   if(!pickRefNo.isEmpty()){

                pickingRecyclerview.setVisibility(View.VISIBLE);
                rlPickListOne.setVisibility(View.GONE);
                pickingoOdNo.setVisibility(View.VISIBLE);
                txt_obd.setVisibility(View.VISIBLE);
                llPickingSugg.setVisibility(View.VISIBLE);
                llsku.setVisibility(View.VISIBLE);
                pickingoOdNo.setText(pickRefNo);
                btnGo.setVisibility(View.GONE);
                spinnerSelectPickList.setVisibility(View.GONE);
                rlPickListOne.setVisibility(View.GONE);
              //  GetOBDItemsForPicking();
            }else {
                rlPickListOne.setVisibility(View.VISIBLE);
                llPickingSugg.setVisibility(View.GONE);
            }*/






           /* if (getArguments().getSerializable("SKUList") !=null)
            {
                myoutboundlist= (List<OutbountDTO>) getArguments().getSerializable("SKUList");
                pickingRecyclerview.setVisibility(View.VISIBLE);
                rlPickListOne.setVisibility(View.GONE);
                pickingoOdNo.setVisibility(View.VISIBLE);
                txt_obd.setVisibility(View.VISIBLE);
                llsku.setVisibility(View.VISIBLE);
                pickingoOdNo.setText(pickRefNo);
                getMyList(myoutboundlist);

            }*/

        }


    }


    public void myScannedData(Context context, String barcode){
        try {
            ProcessScannedinfo(barcode.trim());
        }
        catch (Exception e){
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

    //Assigning scanned value to the respective fields
    public void ProcessScannedinfo(String scannedData) {

        if(((DrawerLayout) getActivity().findViewById(R.id.drawer_layout)).isDrawerOpen(GravityCompat.START)){
            return;
        }

        if (ProgressDialogUtils.isProgressActive() || Common.isPopupActive()) {
            common.showUserDefinedAlertType(errorMessages.EMC_082, getActivity(), getContext(), "Warning");
            return;
        }

        if (scannedData != null && !common.isPopupActive) {
            pickRefNo = ""; pickobdId="";
            //  GetOBDNosUnderSO(scannedData);
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


    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.btnGo:

                if (pickRefNo.equalsIgnoreCase("")) {
                    common.showUserDefinedAlertType(errorMessages.EMC_0035, getActivity(), getContext(), "Warning");

                }

                else {
//                    getPickListdetails();
//                    GetPutawaysuggesttions();
                    pickingRecyclerview.setVisibility(View.VISIBLE);
                    rlPickListOne.setVisibility(View.GONE);
                    pickingoOdNo.setVisibility(View.VISIBLE);
                    txt_obd.setVisibility(View.VISIBLE);
                    llsku.setVisibility(View.VISIBLE);
                    pickingoOdNo.setText(pickRefNo);
                    GetSkuList();
                    // GetOBDItemsForPicking();
                }
                break;

            case R.id.btnClear:
                isValidSO=false;
               pickRefNo = ""; pickobdId="";
//                cvScanSONumber.setCardBackgroundColor(getResources().getColor(R.color.palletColor));
           //     ivScanSONumber.setImageResource(R.drawable.fullscreen_img);
               LoadPickRefnos();
                break;

        }

    }


    public void getPickListdetails() {
        Bundle bundle = new Bundle();
        bundle.putString("pickOBDno", pickRefNo);
        bundle.putString("pickobdId", pickobdId);
        PickingDetailsFragment PickingDetailsFragment = new PickingDetailsFragment();
        PickingDetailsFragment.setArguments(bundle);
        FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, PickingDetailsFragment);
    }

    public void GetSkuList() {
//        if (lblScanAllPallets.getText().toString().equals("Scan Pallet")) {
//            common.showUserDefinedAlertType(errorMessages.EMC_0019, getActivity(), getContext(), "Error");
//            return;
//        }
        WMSCoreMessage message = new WMSCoreMessage();
        message = common.SetAuthentication(EndpointConstants.Outbound, getContext());
//        InventoryDTO iventoryDTO = new InventoryDTO();
        OutbountDTO outbountDTO=new OutbountDTO();
        outbountDTO.setUserId(userId);
//        iventoryDTO.setContainerCode(lblScanAllPallets.getText().toString());
        outbountDTO.setOutboundID(pickobdId);
        message.setEntityObject(outbountDTO);


        Call<String> call = null;
        ApiInterface apiService = RestService.getClient().create(ApiInterface.class);

        try {
            //Checking for Internet Connectivity
            // if (NetworkUtils.isInternetAvailable()) {
            // Calling the Interface method
            ProgressDialogUtils.showProgressDialog("Please Wait");
            call = apiService.GetWORevertItemsForPicking(message);
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
                                ProgressDialogUtils.closeProgressDialog();
                                common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
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
                                for (int i = 0; i < _lOutbound.size(); i++) {

                                    // Each individual value of the map into DTO
                                    outbountDTO2 = new OutbountDTO(_lOutbound.get(i).entrySet());
                                    //Adding Inventory Dto into List
                                    lstOutbound.add(outbountDTO2);
                                }
                                if (lstOutbound.size()>0) {
                                    getMyList(lstOutbound);
                                }else{
                                    DialogUtils.showAlertDialog(getActivity(), "No Data Found");
                                    ProgressDialogUtils.closeProgressDialog();
                                }

                             /*   WorkOrderAdapter=new WorkOrderAdapter(getActivity(),getContext(),lstOutbound, new OnListFragmentInteractionListener() {
                                    @Override
                                    public void onListFragmentInteraction(int pos) {


//                                        ArrayList<String> arraylist = new ArrayList<String>();
//                                        Bundle bundle = new Bundle();
//                                        bundle.putSerializable("mylist", arraylist);


                                        String batchNo = lstOutbound.get(pos).getBatchNo();
                                        String materialCode = lstOutbound.get(pos).getMaterialCode();
                                        String quantity = lstOutbound.get(pos).getQuantity();
                                        String vlpdId =lstOutbound.get(pos).getvLPDId();



                                        Bundle bundle = new Bundle();
                                        bundle.putString("BatchNo", batchNo);
                                        bundle.putString("MaterialCode", materialCode);
                                        bundle.putString("Quantity", quantity);
                                        bundle.putString("VLPDId",vlpdId);
                                        bundle.putString("OBDId",pickobdId);


*//*

                                        PickingDetailsFragment pickingDetailsFragment = new PickingDetailsFragment();
                                        pickingDetailsFragment.setArguments(bundle);
                                        FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, pickingDetailsFragment);
*//*
                                        InternalPickingDetailsFragment internalPickingDetailsFragment = new InternalPickingDetailsFragment();
                                        internalPickingDetailsFragment.setArguments(bundle);
                                        FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, internalPickingDetailsFragment);

                                    }
                                }
                                );
                                LinearLayoutManager linearLayoutManager=new LinearLayoutManager(getActivity());
                                DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getActivity(),DividerItemDecoration.VERTICAL);
                                pickingRecyclerview.setLayoutManager(linearLayoutManager);
                                pickingRecyclerview.setAdapter(WorkOrderAdapter);
                                ProgressDialogUtils.closeProgressDialog();*/
                            }

                          /*  // Listview Group collasped listener
                            expListView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
                                @Override
                                public void onGroupCollapse(int groupPosition) {

                                }
                            });*/
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

    public void ValidateSO(final String scannedData) {

        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.ScanDTO, getContext());
            ScanDTO scanDTO = new ScanDTO();
            scanDTO.setUserID(userId);
            scanDTO.setAccountID(accountId);
            // scanDTO.setTenantID(String.valueOf(tenantID));
            //scanDTO.setWarehouseID(String.valueOf(warehouseID));
            scanDTO.setScanInput(scannedData);
            //inboundDTO.setIsOutbound("0");
            message.setEntityObject(scanDTO);

            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                call = apiService.ValidateSO(message);
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

                            isValidSO=false;
                            cvScanSONumber.setCardBackgroundColor(getResources().getColor(R.color.white));
                            ivScanSONumber.setImageResource(R.drawable.warning_img);
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
                                    isValidSO=true;
                                    cvScanSONumber.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanSONumber.setImageResource(R.drawable.check);
                                    //       GetOBDNosUnderSO(scannedData);
                                } else {
                                    // lblScannedSku.setText("");
                                    isValidSO=false;
                                    cvScanSONumber.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanSONumber.setImageResource(R.drawable.warning_img);
                                    common.showUserDefinedAlertType("Invalid SO Number", getActivity(), getContext(), "Warning");
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


    public void GetOBDNosUnderSO(final String scannedData) {

        try {
            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.Outbound, getContext());
            OutbountDTO outbountDTO = new OutbountDTO();
            outbountDTO.setTenatID(userId);
            outbountDTO.setAccountID(accountId);
            outbountDTO.setSONumber(scannedData);
            message.setEntityObject(outbountDTO);

            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                ProgressDialogUtils.showProgressDialog("Please Wait");
                call = apiService.GetOBDNosUnderSO(message);
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
                                    common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                                    isValidSO=false;
                                    cvScanSONumber.setCardBackgroundColor(getResources().getColor(R.color.palletColor));
                                    ivScanSONumber.setImageResource(R.drawable.fullscreen_img);
                                    return;
                                }
                            } else {
                                List<LinkedTreeMap<?, ?>> _lPickRefNo = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lPickRefNo = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();
                                List<String> lstPickRefNo = new ArrayList<>();
                                lstObdIds = new ArrayList<>();
                                List<OutbountDTO> lstDto = new ArrayList<OutbountDTO>();
                                for (int i = 0; i < _lPickRefNo.size(); i++) {
                                    OutbountDTO dto = new OutbountDTO(_lPickRefNo.get(i).entrySet());
                                    lstDto.add(dto);
                                }
                                for (int i = 0; i < lstDto.size(); i++) {
                                    lstPickRefNo.add(String.valueOf(lstDto.get(i).getoBDNo()));
                                    lstObdIds.add(String.valueOf(lstDto.get(i).getOutboundID()));
                                }

                                isValidSO=true;
                                cvScanSONumber.setCardBackgroundColor(getResources().getColor(R.color.white));
                                ivScanSONumber.setImageResource(R.drawable.check);

                                if (lstPickRefNo == null) {
                                    ProgressDialogUtils.closeProgressDialog();
                                    DialogUtils.showAlertDialog(getActivity(), "Picklist is null");
                                } else {
                                    ProgressDialogUtils.closeProgressDialog();
                                    if(lstPickRefNo.size()==0){
                                        pickobdId="";pickRefNo="";
                                        /*arrayAdapterPickList = new ArrayAdapter(getActivity(), R.layout.support_simple_spinner_dropdown_item, lstPickRefNo);
                                        spinnerSelectPickList.setAdapter(arrayAdapterPickList);*/
                                        common.showUserDefinedAlertType("There is no corresponding OBD to pick for this SO", getActivity(), getContext(), "Warning");
                                        return;
                                    }
                                    if(lstPickRefNo.size()==1){
                                        pickRefNo=lstPickRefNo.get(0).toString().split("[-]", 2)[0];
                                        pickobdId = lstObdIds.get(0).toString();
                                        pickingRecyclerview.setVisibility(View.VISIBLE);
                                        rlPickListOne.setVisibility(View.GONE);
                                        pickingoOdNo.setVisibility(View.VISIBLE);
                                        pickingoOdNo.setText(pickRefNo);
                                        //  GetOBDItemsForPicking();
                                       /* Bundle bundle = new Bundle();
                                        bundle.putString("pickOBDno", pickRefNo);
                                        bundle.putString("pickobdId", pickobdId);
                                        OBDPickingDetailsFragment OBDPickingDetailsFragment = new OBDPickingDetailsFragment();
                                        OBDPickingDetailsFragment.setArguments(bundle);
                                        FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, OBDPickingDetailsFragment);*/
                                    }

                                }

                            }

                            ProgressDialogUtils.closeProgressDialog();

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


    public void LoadPickRefnos() {

        try {
            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.Outbound, getContext());
            OutbountDTO outbountDTO = new OutbountDTO();
            outbountDTO.setUserId(userId);
            outbountDTO.setAccountId(accountId);
            outbountDTO.setRID(1);
            outbountDTO.setIsWorkOrder(reftypevalue);
            message.setEntityObject(outbountDTO);

            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                // Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                ProgressDialogUtils.showProgressDialog("Please Wait");
                call = apiService.GetWORefNos(message);
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
                                List<OutbountDTO> lstDto = new ArrayList<OutbountDTO>();
                                for (int i = 0; i < _lPickRefNo.size(); i++) {
                                    OutbountDTO dto = new OutbountDTO(_lPickRefNo.get(i).entrySet());
                                    lstDto.add(dto);
                                }
                                for (int i = 0; i < lstDto.size(); i++) {
                                    lstPickRefNo.add(String.valueOf(lstDto.get(i).getoBDNo()));
                                    lstObdIds.add(String.valueOf(lstDto.get(i).getOutboundID()));
                                }

                                if (lstPickRefNo == null) {
                                    ProgressDialogUtils.closeProgressDialog();
                                    DialogUtils.showAlertDialog(getActivity(), "Picklist is null");
                                }


                                else {
                                    ProgressDialogUtils.closeProgressDialog();
                                    arrayAdapterPickList = new ArrayAdapter(getActivity(), R.layout.support_simple_spinner_dropdown_item, lstPickRefNo);
                                    spinnerSelectreftype.setAdapter(arrayAdapterPickList);
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
                DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0001);}
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
                DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0002);
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
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("WorkOrder  Revert");
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






    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(int pos);
    }

    public  void getMyList(final List<OutbountDTO> skulist){
        WorkOrderAdapter=new WorkOrderAdapter(getActivity(),getContext(),skulist, new OnListFragmentInteractionListener() {
            @Override
            public void onListFragmentInteraction(int pos) {

                double revertedQty = Double.parseDouble(skulist.get(pos).getRevertQty());
                double qty = Double.parseDouble(skulist.get(pos).getQty());

                if (revertedQty<=qty) {
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("Outbound", skulist.get(pos));
                    //bundle.putString("VLPDId",vlpdId);
                    bundle.putString("OBDId", pickobdId);
                    bundle.putString("OBDNo", pickRefNo);

                    WorkOrderRevertDetailsFragment workorederdetailsFragment = new WorkOrderRevertDetailsFragment();
                    workorederdetailsFragment.setArguments(bundle);
                    FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, workorederdetailsFragment);
                }

            }
        });
        LinearLayoutManager linearLayoutManager=new LinearLayoutManager(getActivity());
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getActivity(),DividerItemDecoration.VERTICAL);
        pickingRecyclerview.setLayoutManager(linearLayoutManager);
        pickingRecyclerview.setAdapter(WorkOrderAdapter);
        ProgressDialogUtils.closeProgressDialog();
    }



}
