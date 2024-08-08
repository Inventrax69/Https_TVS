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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
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
import com.inventrax.tvs.common.Common;
import com.inventrax.tvs.common.constants.EndpointConstants;
import com.inventrax.tvs.common.constants.ErrorMessages;
import com.inventrax.tvs.interfaces.ApiInterface;
import com.inventrax.tvs.pojos.HouseKeepingDTO;
import com.inventrax.tvs.pojos.InboundDTO;
import com.inventrax.tvs.pojos.InventoryDTO;
import com.inventrax.tvs.pojos.WMSCoreMessage;
import com.inventrax.tvs.pojos.WMSExceptionMessage;
import com.inventrax.tvs.searchableSpinner.SearchableSpinner;
import com.inventrax.tvs.services.RetrofitBuilderHttpsEx;
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
 * Created by Prasann on 05/08/2018.
 */

public class OutdropToLoadingTransfersFragment extends Fragment implements View.OnClickListener, BarcodeReader.TriggerListener, BarcodeReader.BarcodeListener {

    private static final String classCode = "API_FRAG_0011";
    private View rootView;
    String scanner = null;
    String getScanner = null;
    private IntentFilter filter;
    private Gson gson;
    private ScanValidator scanValidator;
    private static BarcodeReader barcodeReader;
    private AidcManager manager;
    Common common;
    private WMSCoreMessage core;
    private RelativeLayout rlIPalletTransfer, rlSelect;
    private CardView cvScanFromCont, cvScanLocation;
    private ImageView ivScanFromCont, ivScanLocation;
    private SearchableSpinner spinnerSelectTenant, spinnerSelectWarehouse;
    private Button btnBinComplete, btn_clear, btnGo,btnClose;

    private String Materialcode = null, Userid = null, scanType = "", accountId = "", storageloc = "";
    private int IsToLoc = 0;
    private ExceptionLoggerUtils exceptionLoggerUtils;
    private ErrorMessages errorMessages;
    Boolean isPalletScaned = false, isLocationScaned = false, isSKUScanned = false, IsProceedForBinTransfer = false;
    private SoundUtils soundUtils;
    private String selectedTenant = "", selectedWH = "";
    private int tenantId , whId ;
    List<HouseKeepingDTO> lstTenants = null;
    List<HouseKeepingDTO> lstWarehouse = null;


    TextView txtWarehousetName,txtTendentName,textpallet,txtsuggestedlocation,txtfromlocation;
    RecyclerView case_list;
    SDKAdapter adapter;
    List<String> caseList;

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
    public void myScannedData(Context context, String barcode){
        try {
            ProcessScannedinfo(barcode.trim());
        }catch (Exception e){
            //  Toast.makeText(context, ""+e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public OutdropToLoadingTransfersFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_outdrop_to_loading_transfers, container, false);
        loadFormControls();
        return rootView;

    }

    private void loadFormControls() {

        SharedPreferences sp = getActivity().getSharedPreferences("LoginActivity", Context.MODE_PRIVATE);
        Userid = sp.getString("RefUserId", "");
        scanType = sp.getString("scanType", "");
        accountId = sp.getString("AccountId", "");
        whId = sp.getInt("WarehouseID", 0);
        tenantId = sp.getInt("TenantID", 0);

        rlIPalletTransfer = (RelativeLayout) rootView.findViewById(R.id.rlIPalletTransfer);
        rlSelect = (RelativeLayout) rootView.findViewById(R.id.rlSelect);

        cvScanFromCont = (CardView) rootView.findViewById(R.id.cvScanFromCont);
        ivScanFromCont = (ImageView) rootView.findViewById(R.id.ivScanFromCont);


        textpallet = (TextView) rootView.findViewById(R.id.textpallet);

        txtsuggestedlocation = (TextView) rootView.findViewById(R.id.txtsuggestedlocation);
        txtfromlocation= (TextView) rootView.findViewById(R.id.txtfromlocation);

        btnBinComplete = (Button) rootView.findViewById(R.id.btnBinComplete);
        btn_clear = (Button) rootView.findViewById(R.id.btn_clear);
        btnClose=(Button) rootView.findViewById(R.id.btnClose);

        btnBinComplete.setOnClickListener(this);
        btn_clear.setOnClickListener(this);
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
        // Zebra Scanner in
        IntentFilter filter = new IntentFilter();
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        filter.addAction("com.inventrax.vesuvius.SCAN");
        getActivity().registerReceiver(receiver, filter);
        common = new Common();
        gson = new GsonBuilder().create();
        core = new WMSCoreMessage();
        ProgressDialogUtils.closeProgressDialog();
        common.setIsPopupActive(false);

        case_list=(RecyclerView)rootView.findViewById(R.id.case_list);
        case_list.setHasFixedSize(true);
        case_list.setLayoutManager(new LinearLayoutManager(getContext()));


        //For Honeywell Broadcast receiver intiation
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

            case R.id.btn_clear:
                Clearfields();                       // clear the scanned fields
                break;


            case R.id.btnGo:
                if (whId != 0 && tenantId !=0) {
                    rlSelect.setVisibility(View.GONE);
                    rlIPalletTransfer.setVisibility(View.VISIBLE);
                    // method to get the storage locations
                } else {
                    common.showUserDefinedAlertType(errorMessages.EMC_0011, getActivity(), getContext(), "Error");
                }
                break;

            case R.id.btnBinComplete:
                if(!txtsuggestedlocation.getText().toString().isEmpty()){
                    TransferOutdropToLoading();
                }
                break;

//            //<mahe>
            case R.id.btnClose:
                HomeFragment homeFragment=new HomeFragment();
                FragmentUtils.replaceFragmentWithBackStack(getActivity(),R.id.container_body,homeFragment);
                break;
        }
    }

    private void Clearfields() {

        cvScanFromCont.setCardBackgroundColor(getResources().getColor(R.color.primarycolor));
        ivScanFromCont.setImageResource(R.drawable.fullscreen_img);

                   isPalletScaned = false;
        textpallet.setText("");
        txtsuggestedlocation.setText("");
        txtfromlocation.setText("");
        case_list.setAdapter(null);
    }

    public void  loadList(List<InventoryDTO> caseList){
        adapter=new SDKAdapter(getActivity(),caseList);
        case_list.setAdapter(adapter);

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

        if (common.isPopupActive()) {

        } else if (scannedData != null && !scannedData.equalsIgnoreCase("")) {

            if (!ProgressDialogUtils.isProgressActive()) {

                if(!isPalletScaned){
                    splitWorkOrderData(scannedData);
                    //ValidatePallet(scannedData);
                }else{
                    common.showUserDefinedAlertType(errorMessages.EMC_088, getActivity(), getContext(), "Error");
                    ProgressDialogUtils.closeProgressDialog();
                }
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


   public  void  splitWorkOrderData(String ScannedData){
        try {
            String[] parts = ScannedData.split(",");

            String palletNumber = parts[0];
            textpallet.setText(palletNumber);
            cvScanFromCont.setCardBackgroundColor(getResources().getColor(R.color.white));
            ivScanFromCont.setImageResource(R.drawable.check);
            GenerateLoadingSuggestion();
        }catch (Exception e){
            common.showUserDefinedAlertType(e.toString(), getActivity(), getContext(), "Error");

        }
    }

    // honeywell Barcode reader
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
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.title_outdroptoloading));
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
        super.onDestroyView();    }





    public class SDKAdapter extends RecyclerView.Adapter<SDKAdapter.ViewHolder> {
        private Context context;
        private List<InventoryDTO> caseList;

        public SDKAdapter(Context context, List<InventoryDTO> caseList) {
            this.context = context;
            this.caseList = caseList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.sdk_list2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
           // String item = caseList.get(position).getMaterialCode();
            holder.caseno.setText(caseList.get(position).getMaterialCode());
            holder.serialnumber.setText(String.valueOf(position+1)+".");

        }

        @Override
        public int getItemCount() {
            return caseList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView caseno,serialnumber;


            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                caseno = itemView.findViewById(R.id.caseno);
                serialnumber= itemView.findViewById(R.id.serialnumber);
            }
        }
    }


    public void TransferOutdropToLoading() {
        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.Inventory, getContext());
            InventoryDTO inventoryDTO = new InventoryDTO();
            inventoryDTO.setAccountID(accountId);
            inventoryDTO.setTenantID(String.valueOf(tenantId));
            inventoryDTO.setWarehouseId(String.valueOf(whId));
           inventoryDTO.setSugLocation(txtsuggestedlocation.getText().toString());
            inventoryDTO.setPalletNumber(textpallet.getText().toString());
            inventoryDTO.setLocationCode(txtfromlocation.getText().toString());
            message.setEntityObject(inventoryDTO);

            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);
            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                ProgressDialogUtils.showProgressDialog("Please Wait");
                call = apiService.TransferOutdropToLoading(message);
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
                        if (response.body() != null) {
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

                                Log.v("ABCDE",new Gson().toJson(core.getEntityObject()));

                                List<LinkedTreeMap<?, ?>> _lINB = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lINB = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                InboundDTO dto = null;
                                ProgressDialogUtils.closeProgressDialog();

                                for (int i = 0; i < _lINB.size(); i++) {

                                    dto = new InboundDTO(_lINB.get(i).entrySet());

                                    if (dto.getResult().equals("Success")) {
                                        common.showUserDefinedAlertType(errorMessages.EMC_099, getActivity(), getContext(), "Success");
                                        Clearfields();
                                    }else {
                                        common.showUserDefinedAlertType(errorMessages.EMC_086, getActivity(), getContext(), "Success");
                                    }
                                }
                               /* if(lstInventory!=null){
                                    common.showUserDefinedAlertType(errorMessages.EMC_099, getActivity(), getContext(), "Success");
                                    Clearfields();
                                }else{
                                    common.showUserDefinedAlertType(errorMessages.EMC_086, getActivity(), getContext(), "Error");
                                }*/


                                ProgressDialogUtils.closeProgressDialog();
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
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_02", getActivity());
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
            common.showUserDefinedAlertType(errorMessages.EMC_0003, getActivity(), getContext(), "Error");
            return;
        }
    }

    public void GenerateLoadingSuggestion() {

        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.Inventory, getContext());
            InventoryDTO inventoryDTO = new InventoryDTO();
            inventoryDTO.setContainerCode(textpallet.getText().toString());
            inventoryDTO.setTenantCode(selectedTenant);
            inventoryDTO.setAccountID(accountId);
            inventoryDTO.setTenantID(String.valueOf(tenantId));
            inventoryDTO.setWarehouseId(String.valueOf(whId));
            inventoryDTO.setWarehouse(selectedWH);
            message.setEntityObject(inventoryDTO);


            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {

                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                ProgressDialogUtils.showProgressDialog("Please Wait");
                call = apiService.GenerateLoadingSuggestion(message);
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
                        if (response.body() != null) {
                            core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);
                            if ((core.getType().toString().equals("Exception"))) {
                                List<LinkedTreeMap<?, ?>> _lExceptions = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lExceptions = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                WMSExceptionMessage owmsExceptionMessage = null;

                                for (int i = 0; i < _lExceptions.size(); i++) {

                                    owmsExceptionMessage = new WMSExceptionMessage(_lExceptions.get(i).entrySet());


                                }
                                textpallet.setText("");
                                case_list.setAdapter(null);

                                ProgressDialogUtils.closeProgressDialog();
                                common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                            } else {


                                List<LinkedTreeMap<?, ?>> _lInventory = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lInventory = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();
                                List<InventoryDTO> lstInventory = new ArrayList<InventoryDTO>();

                                ProgressDialogUtils.closeProgressDialog();

                                if(_lInventory!=null){
                                    if (_lInventory.size() > 0) {
                                        InventoryDTO inventorydto = null;
                                        for (int i = 0; i < _lInventory.size(); i++) {
                                            inventorydto = new InventoryDTO(_lInventory.get(i).entrySet());
                                            lstInventory.add(inventorydto);
                                        }
                                        txtsuggestedlocation.setText(lstInventory.get(0).getSugLocation());
                                        txtfromlocation.setText(lstInventory.get(0).getLocationCode());
                                        loadList(lstInventory);
                                    }
                                    else {
                                        case_list.setAdapter(null);
                                        common.showUserDefinedAlertType(errorMessages.EMC_0060, getActivity(), getContext(), "Warning");
                                    }
                                }else{
                                    case_list.setAdapter(null);
                                    common.showUserDefinedAlertType(errorMessages.EMC_0060, getActivity(), getContext(), "Warning");
                                }
                            }
                        } else {
                            case_list.setAdapter(null);
                            ProgressDialogUtils.closeProgressDialog();
                            common.showUserDefinedAlertType(errorMessages.EMC_0021, getActivity(), getContext(), "Error");
                            return;
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable throwable) {
                        case_list.setAdapter(null);
                        ProgressDialogUtils.closeProgressDialog();
                        common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
                        return;
                    }
                });
            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_02", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                case_list.setAdapter(null);
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
            common.showUserDefinedAlertType(errorMessages.EMC_0003, getActivity(), getContext(), "Error");
            return;
        }
    }

}