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

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
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
import com.inventrax.tvs.adapters.FlaggedAdapter;
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
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.annotation.Nullable;
public class HeaderFlaggedBinsFragment extends Fragment implements View.OnClickListener, BarcodeReader.TriggerListener, BarcodeReader.BarcodeListener, AdapterView.OnItemSelectedListener {

    private static final String classCode = "API_FRAG_020";
    private View rootView;
    Button btnGo,btnClear;
    SearchableSpinner spinnerSelectPickList;
    private IntentFilter filter;
    private Gson gson;
    private Common common;
    private WMSCoreMessage core;
    private ExceptionLoggerUtils exceptionLoggerUtils;
    private ErrorMessages errorMessages;
    private String userId = null;
    String accountId = null;
    private String pickRefNo = "", pickobdId;
    List<String> lstObdIds;
    String scanner = null;
    String getScanner = null;
    //For Honey well barcode
    private static BarcodeReader barcodeReader;
    private AidcManager manager;
    ArrayAdapter arrayAdapterPickList;
    CardView cvScanSONumber;
    ImageView ivScanSONumber;
    boolean isValidSO=false;
    PickingExpandableListAdapter listAdapter = null;
    private ExpandableListView expListView;
    RecyclerView pickingRecyclerview;
//    PickingAdapter pickingAdapter;
    FlaggedAdapter flaggedAdapter;
    RelativeLayout rlPickListOne;
    TextView pickingoOdNo;
    private LinearLayout llPickingSugg;

    public HeaderFlaggedBinsFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_header_flagged_bins, container, false);
        loadFormControls();
        return rootView;
    }

    private void loadFormControls() {

        SharedPreferences sp = getActivity().getSharedPreferences("LoginActivity", Context.MODE_PRIVATE);
        userId = sp.getString("RefUserId", "");
        accountId = sp.getString("AccountId", "");


        cvScanSONumber = (CardView) rootView.findViewById(R.id.cvScanSONumber);
        ivScanSONumber = (ImageView) rootView.findViewById(R.id.ivScanSONumber);

        spinnerSelectPickList = (SearchableSpinner) rootView.findViewById(R.id.spinnerSelectPickList);
        spinnerSelectPickList.setOnItemSelectedListener(this);
        btnGo = (Button) rootView.findViewById(R.id.btnGo);
        btnClear = (Button) rootView.findViewById(R.id.btnClear);

        llPickingSugg = (LinearLayout) rootView.findViewById(R.id.llPickingSugg);

        gson = new GsonBuilder().create();
        btnGo.setOnClickListener(this);
        btnClear.setOnClickListener(this);

        common = new Common();
        exceptionLoggerUtils = new ExceptionLoggerUtils();
        errorMessages = new ErrorMessages();

        pickingRecyclerview= (RecyclerView) rootView.findViewById(R.id.pickingRecyclerView);
        pickingoOdNo= (TextView) rootView.findViewById(R.id.pickingoOdNo);

        rlPickListOne=(RelativeLayout)rootView.findViewById(R.id.rlPickListOne);
        pickingRecyclerview.setVisibility(View.GONE);
        ProgressDialogUtils.closeProgressDialog();
        common.setIsPopupActive(false);

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
            pickRefNo = getArguments().getString("ObdNum");
            pickobdId = getArguments().getString("pickobdId");

            if(!pickRefNo.isEmpty()){

                pickingRecyclerview.setVisibility(View.VISIBLE);
                rlPickListOne.setVisibility(View.GONE);
                pickingoOdNo.setVisibility(View.VISIBLE);
                llPickingSugg.setVisibility(View.VISIBLE);
                pickingoOdNo.setText(pickRefNo);
                btnGo.setVisibility(View.GONE);
                spinnerSelectPickList.setVisibility(View.GONE);
                rlPickListOne.setVisibility(View.GONE);
                GetOBDItemsForPicking();
            }else {
                rlPickListOne.setVisibility(View.VISIBLE);
                llPickingSugg.setVisibility(View.GONE);
            }

        }


    }

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
            GetOBDNosUnderSO(scannedData);
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
                } else {
//                    getPickListdetails();
//                    GetPutawaysuggesttions();
                    pickingRecyclerview.setVisibility(View.VISIBLE);
                    rlPickListOne.setVisibility(View.GONE);
                    pickingoOdNo.setVisibility(View.VISIBLE);
                    pickingoOdNo.setText(pickRefNo);
                    GetOBDItemsForPicking();
                }
                break;

            case R.id.btnClear:
                isValidSO=false;
                pickRefNo = ""; pickobdId="";
                cvScanSONumber.setCardBackgroundColor(getResources().getColor(R.color.palletColor));
                ivScanSONumber.setImageResource(R.drawable.fullscreen_img);
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

    public void GetOBDItemsForPicking() {
//        if (lblScanAllPallets.getText().toString().equals("Scan Pallet")) {
//            common.showUserDefinedAlertType(errorMessages.EMC_0019, getActivity(), getContext(), "Error");
//            return;
//        }
        WMSCoreMessage message = new WMSCoreMessage();
        message = common.SetAuthentication(EndpointConstants.Outbound, getContext());
//        InventoryDTO iventoryDTO = new InventoryDTO();
        OutbountDTO outbountDTO=new OutbountDTO();

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
            call = apiService.GetOBDItemsForPicking(message);
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
//                                List<String> lstPalletnumberHeader = new ArrayList<String>();
//                                List<String> lstTenantIDHeader = new ArrayList<String>();
//                                //child for Hashmap
//                                HashMap<String, List<String>> lstLocaMaterialInfo = new HashMap<String, List<String>>();

                                Log.v("ABCDE",new Gson().toJson(core));

                                OutbountDTO outbountDTO2 = null;
                                //Iterating through the Inventory list
                                for (int i = 0; i < _lOutbound.size(); i++) {

                                    // Each individual value of the map into DTO
                                    outbountDTO2 = new OutbountDTO(_lOutbound.get(i).entrySet());
                                    //Adding Inventory Dto into List
                                    lstOutbound.add(outbountDTO2);
                                }


                        /*       PalletInfo palletInfo = new PalletInfo();
                                List<PalletInfo> lstPalletinfo = new ArrayList<PalletInfo>();

                                List<MaterialInfo> lstMaterilinfo = new ArrayList<MaterialInfo>();
                                List<String> lstLocMaterialQty = new ArrayList<String>();
*/
/*
                                Iterating to List of Inventory and Mapping it into  Local Model class
                                for (InventoryDTO inventoryDTO : lstInventory) {
                                    MaterialInfo materialInfo = new MaterialInfo();
                                    materialInfo.setMcode(inventoryDTO.getMaterialCode());
                                    materialInfo.setLocation(inventoryDTO.getLocationCode());
                                    materialInfo.setQty((int)Double.parseDouble(inventoryDTO.getQuantity()));
                                    materialInfo.setSuggestionId(inventoryDTO.getSuggestionID());
                                    materialInfo.setWareHouseID(inventoryDTO.getWarehouseId());
                                    materialInfo.setTenantID(inventoryDTO.getTenantID());
                                    palletInfo.setPalletCode(inventoryDTO.getContainerCode());
                                    lstMaterilinfo.add(materialInfo);*/
//                                }

                                // added Material Information into list
                    /*            // Setting Material information to Palletinfo
                                palletInfo.setMaterialinfo(lstMaterilinfo);
                                // Add pallet info to List
                                lstPalletinfo.add(palletInfo);
//                                palletInfo.setPalletCode(lblScanAllPallets.getText().toString());
                                // Adding Pallet no to the Header list
                                lstPalletnumberHeader.add(palletInfo.getPalletCode());

                                for (int Palletlist = 0; Palletlist < palletInfo.getMaterialinfo().size(); Palletlist++) {
                                    // Adding the Loc,Matrial,Qty to the list
                                    *//*lstLocMaterialQty.add(palletInfo.getMaterialinfo().get(Palletlist).getLocation() + System.getProperty("line.separator") +
                                            palletInfo.getMaterialinfo().get(Palletlist).getMcode() + "/"
                                            + palletInfo.getMaterialinfo().get(Palletlist).getQty() +
                                            "/" + palletInfo.getMaterialinfo().get(Palletlist).getSuggestionId());
                                           *//**//* +"/" + palletInfo.getMaterialinfo().get(Palletlist).getTenantID()+"/"
                                            + palletInfo.getMaterialinfo().get(Palletlist).getWareHouseID());
                                *//*

                                    lstLocMaterialQty.add(palletInfo.getMaterialinfo().get(Palletlist).getLocation() + System.getProperty("line.separator") +
                                            palletInfo.getMaterialinfo().get(Palletlist).getMcode() + "/"
                                            + (int)palletInfo.getMaterialinfo().get(Palletlist).getQty() +
                                            "/" + palletInfo.getMaterialinfo().get(Palletlist).getSuggestionId()
                                            +"/" + palletInfo.getMaterialinfo().get(Palletlist).getTenantID()+"/"
                                            + palletInfo.getMaterialinfo().get(Palletlist).getWareHouseID());
                                }
                                // Adding header and Child to Map
                                lstLocaMaterialInfo.put(palletInfo.getPalletCode(), lstLocMaterialQty);
                                // Passing header and Child to the Adapters
                                // Passing header and Child to the Adapters
*/
//                                listAdapter = new PickingExpandableListAdapter(getActivity(), getContext(),lstOutbound);
//                                expListView.setAdapter(listAdapter);
//                                ProgressDialogUtils.closeProgressDialog();

                                flaggedAdapter=new FlaggedAdapter(getActivity(),getContext(),lstOutbound, new OnListFragmentInteractionListener() {
                                    @Override
                                    public void onListFragmentInteraction(int pos) {


//                                        ArrayList<String> arraylist = new ArrayList<String>();
//                                        Bundle bundle = new Bundle();
//                                        bundle.putSerializable("mylist", arraylist);


                                        String customerName = lstOutbound.get(pos).getCustomerName();
                                        String sku = lstOutbound.get(pos).getSKU();
                                        String sugloc = lstOutbound.get(pos).getLocation();
                                        String skuDes = lstOutbound.get(pos).getMaterialDescription();
                                        String Dock = lstOutbound.get(pos).getDockLocation();
                                        String obdNum = pickRefNo;
                                        String picked = lstOutbound.get(pos).getPickedQty();
                                        String assignedQuantity = lstOutbound.get(pos).getAssignedQuantity();
                                        String mrp = lstOutbound.get(pos).getMRP();
                                        String  pendingqty = lstOutbound.get(pos).getPendingQty();
                                        String  mfgDate = lstOutbound.get(pos).getMfgDate();
                                        String  expDate = lstOutbound.get(pos).getExpDate();


                                        String  huno = lstOutbound.get(pos).getHUNo();
                                        String  psn = lstOutbound.get(pos).getPSN();
                                        String  husize = lstOutbound.get(pos).getHUSize();
                                        String  ispsn = lstOutbound.get(pos).getIsPSN();
                                        String  lineno = lstOutbound.get(pos).getLineno();
                                        String  BatchNo = lstOutbound.get(pos).getBatchNo();
                                        String  CartonNo = lstOutbound.get(pos).getCartonNo();
                                        String  SerialNo = lstOutbound.get(pos).getSerialNo();
                                        String  DriverNo = lstOutbound.get(pos).getDriverNo();
                                        String  soDetailsId = lstOutbound.get(pos).getSODetailsID();
                                        String  AssignedID = lstOutbound.get(pos).getAssignedID();
                                        String  pOSOHeaderId = lstOutbound.get(pos).getpOSOHeaderId();
                                        String  PalletNo = lstOutbound.get(pos).getPalletNo();



                                        Bundle bundle = new Bundle();
                                        bundle.putString("CustomerName", customerName);
                                        bundle.putString("Sku", sku);
                                        bundle.putString("SugLoc", sugloc);
                                        bundle.putString("SkuDes", skuDes);
                                        bundle.putString("ObdNum", obdNum);
                                        bundle.putString("Dock", Dock);
                                        bundle.putString("pickedQty", picked);
                                        bundle.putString("AssignedQuantity", assignedQuantity);
                                        bundle.putString("Mrp", mrp);

                                        bundle.putString("HuNo", huno);
                                        bundle.putString("PSN", psn);
                                        bundle.putString("pendingqty", pendingqty);
                                        bundle.putString("HUsize", husize);
                                        bundle.putString("IsPSN", ispsn);
                                        bundle.putString("LineNo", lineno);
                                        bundle.putString("BatchNo", BatchNo);
                                        bundle.putString("CartonNo", CartonNo);
                                        bundle.putString("SerialNo", SerialNo);
                                        bundle.putString("DriverNo", DriverNo);
                                        bundle.putString("soDetailsId", soDetailsId);
                                        bundle.putString("AssignedID", AssignedID);
                                        bundle.putString("pOSOHeaderId", pOSOHeaderId);
                                        bundle.putString("PalletNo", PalletNo);
                                        bundle.putString("pickobdId", pickobdId);
                                        bundle.putString("MfgDate", mfgDate);
                                        bundle.putString("ExpDate", expDate);


                                        DetailsFlaggedBinsFragment detailsFlaggedBinsFragment = new DetailsFlaggedBinsFragment();
                                        detailsFlaggedBinsFragment.setArguments(bundle);
                                        FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, detailsFlaggedBinsFragment);


                                    }
                                }
                                );
                                LinearLayoutManager linearLayoutManager=new LinearLayoutManager(getActivity());
                                DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getActivity(),DividerItemDecoration.VERTICAL);
                                dividerItemDecoration.setDrawable(getResources().getDrawable(R.drawable.divider));
                                pickingRecyclerview.addItemDecoration(dividerItemDecoration);
                                pickingRecyclerview.setLayoutManager(linearLayoutManager);
                                pickingRecyclerview.setAdapter(flaggedAdapter);
                                ProgressDialogUtils.closeProgressDialog();
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
                                    GetOBDNosUnderSO(scannedData);
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
                                        GetOBDItemsForPicking();
                                       /* Bundle bundle = new Bundle();
                                        bundle.putString("pickOBDno", pickRefNo);
                                        bundle.putString("pickobdId", pickobdId);
                                        OBDPickingDetailsFragment OBDPickingDetailsFragment = new OBDPickingDetailsFragment();
                                        OBDPickingDetailsFragment.setArguments(bundle);
                                        FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, OBDPickingDetailsFragment);*/
                                    }else{
                                        arrayAdapterPickList = new ArrayAdapter(getActivity(), R.layout.support_simple_spinner_dropdown_item, lstPickRefNo);
                                        spinnerSelectPickList.setAdapter(arrayAdapterPickList);
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
            outbountDTO.setRID(0);
            message.setEntityObject(outbountDTO);

            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                // Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                ProgressDialogUtils.showProgressDialog("Please Wait");
                call = apiService.GetobdRefNos(message);
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
                                } else {
                                    ProgressDialogUtils.closeProgressDialog();
                                    arrayAdapterPickList = new ArrayAdapter(getActivity(), R.layout.support_simple_spinner_dropdown_item, lstPickRefNo);
                                    spinnerSelectPickList.setAdapter(arrayAdapterPickList);
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
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.menu_flaggedBins));
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
        pickRefNo = spinnerSelectPickList.getSelectedItem().toString().split("[-]", 2)[0];
        pickobdId = lstObdIds.get(position).toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {



    }
    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(int pos);
    }

}
