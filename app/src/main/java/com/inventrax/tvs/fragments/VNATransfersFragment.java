package com.inventrax.tvs.fragments;


import android.app.Dialog;
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
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
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
import com.inventrax.tvs.pojos.InboundDTO;
import com.inventrax.tvs.pojos.WMSCoreMessage;
import com.inventrax.tvs.pojos.WMSExceptionMessage;
import com.inventrax.tvs.searchableSpinner.SearchableSpinner;
import com.inventrax.tvs.services.RetrofitBuilderHttpsEx;
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

public class VNATransfersFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemSelectedListener, BarcodeReader.TriggerListener, BarcodeReader.BarcodeListener {

    private static final String classCode = "API_FRAG_TASK_INTER_LEAVING";
    private View rootView;
    private CardView cvScanFromLocation,cvScanPallet,cvScanToLocation;
    private ImageView ivScanFromLocation,ivScanPallet, ivScanToLocation;
    private EditText etPallet,etToLocation;
    Button btnClear, btnSkip,btnCloseLoadPallet,btnStart,btnStop;
    private Common common = null;
    String scanner = null;
    String getScanner = null;
    IntentFilter filter;
    private ScanValidator scanValidator;
    private Gson gson;
    private WMSCoreMessage core;
    private String userId = null, stRefNo = null, palletType = null, materialType = null;
    private SearchableSpinner spinnerSelectReason;
    // For Honey well barcode
    private static BarcodeReader barcodeReader;
    private AidcManager manager;

    SoundUtils sound = null;
    private ExceptionLoggerUtils exceptionLoggerUtils;
    private ErrorMessages errorMessages;
    public Bundle bundle;
    boolean isPalletScanned,isFromLocationScanned,isToLocationScanned;
    boolean isPicking,isPutaway;
    TextView tvStRef;
    public String SuggestionType;
    public String inOutId="1";
    String skipReason;
    TextView lblVLPDNumber,txtVLPDNumber,txtPenPallet,etFromLocation;

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

    public VNATransfersFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_vna_transfers, container, false);
        barcodeReader = MainActivity.getBarcodeObject();
        loadFormControls();
        return rootView;
    }

    // Form controls
    private void loadFormControls() {

        isPalletScanned=false;
        isFromLocationScanned=true;
        isToLocationScanned=false;

        tvStRef=(TextView)rootView.findViewById(R.id.tvStRef);
        lblVLPDNumber=(TextView)rootView.findViewById(R.id.lblVLPDNumber);
        txtVLPDNumber=(TextView)rootView.findViewById(R.id.txtVLPDNumber);
        txtPenPallet=(TextView)rootView.findViewById(R.id.txtPenPallet);

        cvScanFromLocation=(CardView)rootView.findViewById(R.id.cvScanFromActualLocation);
        cvScanPallet=(CardView)rootView.findViewById(R.id.cvScanPallet);
        cvScanToLocation=(CardView)rootView.findViewById(R.id.cvScanToLocation);

        ivScanFromLocation=(ImageView)rootView.findViewById(R.id.ivScanFromActualLocation);
        ivScanPallet=(ImageView)rootView.findViewById(R.id.ivScanPallet);
        ivScanToLocation=(ImageView)rootView.findViewById(R.id.ivScanToLocation);

        etFromLocation=(TextView) rootView.findViewById(R.id.etFromLocation);
        etPallet=(EditText) rootView.findViewById(R.id.etPallet);
        etToLocation=(EditText) rootView.findViewById(R.id.etToLocation);

        btnClear=(Button)rootView.findViewById(R.id.btnClear);
        btnSkip=(Button)rootView.findViewById(R.id.btnSkip);
        btnStart=(Button)rootView.findViewById(R.id.btnStart);
        btnStop=(Button)rootView.findViewById(R.id.btnStop);
        btnCloseLoadPallet=(Button)rootView.findViewById(R.id.btnCloseLoadPallet);

        btnClear.setOnClickListener(this);
        btnSkip.setOnClickListener(this);
        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnCloseLoadPallet.setOnClickListener(this);

        SharedPreferences sp = getActivity().getSharedPreferences("LoginActivity", Context.MODE_PRIVATE);
        userId = sp.getString("RefUserId", "");
        materialType = sp.getString("division", "");
        SharedPreferences spPrinterIP = getActivity().getSharedPreferences("SettingsActivity", Context.MODE_PRIVATE);

        common = new Common();
        errorMessages = new ErrorMessages();
        exceptionLoggerUtils = new ExceptionLoggerUtils();
        sound = new SoundUtils();
        gson = new GsonBuilder().create();
        core = new WMSCoreMessage();

        bundle = new Bundle();
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

        // For Honey well
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

        radioButtonClicks();
        Common.setIsPopupActive(false);

    }

    private void radioButtonClicks() {
        ((RadioButton) rootView.findViewById(R.id.radioAuto)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearAllFileds1();
                  SuggestionType="1";
                  inOutId="1";
                txtPenPallet.setVisibility(View.GONE);
                  setSuggestionTypeApi(SuggestionType);
                btnSkip.setVisibility(View.VISIBLE);
               // common.showUserDefinedAlertType("No Yet Enabled", getActivity(), getContext(), "Warning");
            }
        });
        // ((RadioButton)rootView.findViewById(R.id.radioAuto)).performClick();
        ((RadioButton) rootView.findViewById(R.id.radioPicking)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SuggestionType = "3";
                inOutId = "2";
                clearAllFileds1();
                setSuggestionTypeApi(SuggestionType);
                tvStRef.setText("Picking");
                lblVLPDNumber.setVisibility(View.VISIBLE);
                txtVLPDNumber.setVisibility(View.VISIBLE);
                txtPenPallet.setVisibility(View.VISIBLE);
                btnSkip.setVisibility(View.VISIBLE);
            }
        });
        ((RadioButton) rootView.findViewById(R.id.radioPutaway)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SuggestionType = "2";
                inOutId = "1";
                clearAllFileds1();
                isPutaway = true;
                isPicking = false;
                setSuggestionTypeApi(SuggestionType);
                tvStRef.setText("Putaway");
                lblVLPDNumber.setVisibility(View.GONE);
                txtVLPDNumber.setVisibility(View.GONE);
                txtPenPallet.setVisibility(View.GONE);
                btnSkip.setVisibility(View.VISIBLE);
            }
        });
    }



    public void clearAllFileds(){
        ProgressDialogUtils.closeProgressDialog();
        Common.setIsPopupActive(false);
        isPalletScanned=false;
        isFromLocationScanned=true;
        isToLocationScanned=false;
        etFromLocation.setText("");
        etPallet.setText("");
        etToLocation.setText("");
        txtPenPallet.setText("");
        cvScanFromLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
        ivScanFromLocation.setImageResource(R.drawable.check);
        cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.palletColor));
        ivScanPallet.setImageResource(R.drawable.fullscreen_img);
        cvScanToLocation.setCardBackgroundColor(getResources().getColor(R.color.locationColor));
        ivScanToLocation.setImageResource(R.drawable.fullscreen_img);
    }

    public void clearAllFileds1(){

        ProgressDialogUtils.closeProgressDialog();
        Common.setIsPopupActive(false);
        isPalletScanned=false;
        isFromLocationScanned=true;
        isToLocationScanned=false;
        lblVLPDNumber.setVisibility(View.INVISIBLE);
        txtVLPDNumber.setVisibility(View.INVISIBLE);
        cvScanFromLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
        ivScanFromLocation.setImageResource(R.drawable.check);
        cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.palletColor));
        ivScanPallet.setImageResource(R.drawable.fullscreen_img);
        cvScanToLocation.setCardBackgroundColor(getResources().getColor(R.color.locationColor));
        ivScanToLocation.setImageResource(R.drawable.fullscreen_img);
        etToLocation.setText("");
        etFromLocation.setText("");
        etPallet.setText("");
        tvStRef.setText("");
    }




    //button Clicks
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnClear:
                clearAllFileds1();
                break;
            case R.id.btnStart:
                GeneratePutawayandPickingSuggestion();
                break;
            case R.id.btnStop:

                StopSuggestion();
                break;

            case R.id.btnSkip:

                if(isPicking){
                    /* if(isFromLocationScanned && !isPalletScanned && !isToLocationScanned){*/

                    final Dialog pickingSkipdialog = new Dialog(getActivity());
                    pickingSkipdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    pickingSkipdialog.setCancelable(false);
                    pickingSkipdialog.setContentView(R.layout.skip_dialog);
                    TextView btnOk = (TextView) pickingSkipdialog.findViewById(R.id.btnOk);
                    btnOk.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (inOutId.equalsIgnoreCase("2")){
                                VNApickingSkip();
                                pickingSkipdialog.dismiss();
                            }else {
                                VNAPutawaySkip();
                                pickingSkipdialog.dismiss();
                            }

                        }
                    });

                    TextView btnCancel = (TextView) pickingSkipdialog.findViewById(R.id.btnCancel);
                    btnCancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            pickingSkipdialog.dismiss();
                        }
                    });

                    final List<String> lstSkipReason = new ArrayList<>();
                    lstSkipReason.add("Damage");
                    lstSkipReason.add("Not Found");
                    lstSkipReason.add("Location Not Empty");
                    spinnerSelectReason=(SearchableSpinner) pickingSkipdialog.findViewById(R.id.spinnerSelectReason);
                    ArrayAdapter arrayAdapterSelectReason = new ArrayAdapter(getActivity(), R.layout.support_simple_spinner_dropdown_item, lstSkipReason);
                    spinnerSelectReason.setAdapter(arrayAdapterSelectReason);
                    spinnerSelectReason.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            skipReason=spinnerSelectReason.getSelectedItem().toString();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {
                            skipReason=lstSkipReason.get(0);
                        }
                    });

                    pickingSkipdialog.show();

/*                        // TODO after Skipping()
                    }else{
                        if(isToLocationScanned && isPalletScanned){
                            Toast.makeText(getActivity(), "Already Transfered", Toast.LENGTH_SHORT).show();
                        }else{
                            common.showUserDefinedAlertType(errorMessages.EMC_090, getActivity(), getContext(), "Error");
                          //  Toast.makeText(getActivity(), "Please scan 'from location' and pallet to skip", Toast.LENGTH_SHORT).show();
                        }
                        //TODO setError messages
                    }*/
                }else if(!isPicking) {

                    final Dialog pickingSkipdialog = new Dialog(getActivity());
                    pickingSkipdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    pickingSkipdialog.setTitle("Select the reason");
                    pickingSkipdialog.setCancelable(false);
                    pickingSkipdialog.setContentView(R.layout.skip_dialog);
                    TextView btnOk = (TextView) pickingSkipdialog.findViewById(R.id.btnOk);
                    btnOk.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (inOutId.equalsIgnoreCase("2")){
                                VNApickingSkip();
                                pickingSkipdialog.dismiss();
                            }else {
                                VNAPutawaySkip();
                                pickingSkipdialog.dismiss();
                            }

                        }
                    });

                    TextView btnCancel = (TextView) pickingSkipdialog.findViewById(R.id.btnCancel);
                    btnCancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            pickingSkipdialog.dismiss();
                        }
                    });

                    final List<String> lstSkipReason = new ArrayList<>();
                    lstSkipReason.add("Damage");
                    lstSkipReason.add("Not Found");
                    lstSkipReason.add("Location Not Empty");
                    spinnerSelectReason=(SearchableSpinner) pickingSkipdialog.findViewById(R.id.spinnerSelectReason);
                    ArrayAdapter arrayAdapterSelectReason = new ArrayAdapter(getActivity(), R.layout.support_simple_spinner_dropdown_item, lstSkipReason);
                    spinnerSelectReason.setAdapter(arrayAdapterSelectReason);
                    spinnerSelectReason.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            skipReason=spinnerSelectReason.getSelectedItem().toString();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {
                            skipReason=lstSkipReason.get(0);
                        }
                    });

                    pickingSkipdialog.show();

            }else {

                    if(isFromLocationScanned && isPalletScanned && !isToLocationScanned){
                        Toast.makeText(getActivity(), "Skiped", Toast.LENGTH_SHORT).show();
                        // TODO after Skipping()
                    }else{
                        if(isToLocationScanned){
                            Toast.makeText(getActivity(), "Already Transferred", Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(getActivity(), getString(R.string.please_scan_from_location_and_pallet_to_skip), Toast.LENGTH_SHORT).show();
                        }
                        //TODO setError messages
                    }
                }
                break;
            case R.id.btnCloseLoadPallet:
                FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, new HomeFragment());
                break;
            default:
                break;
        }
    }





    @Override
    public void onBarcodeEvent(final BarcodeReadEvent barcodeReadEvent) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                getScanner = barcodeReadEvent.getBarcodeData();
                ProcessScannedinfo(getScanner);

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
            properties.put(BarcodeReader.PROPERTY_EAN_13_CHECK_DIGIT_TRANSMIT_ENABLED, true);
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

        if (scannedData != null && !Common.isPopupActive()) {

            if (!ProgressDialogUtils.isProgressActive()) {

                if(tvStRef.getText().toString().isEmpty()){
                    common.showUserDefinedAlertType(getString(R.string.no_suggestion_type_defined), getActivity(), getContext(), "Error");
                    return;
                }




                   if (!isPalletScanned) {
                       if (etPallet.getText().toString().equals(scannedData)) {
                           VNAPalletScan(scannedData);
                       } else {
                           cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.white));
                           ivScanPallet.setImageResource(R.drawable.warning_img);
                           isPalletScanned = false;
                           common.showUserDefinedAlertType(errorMessages.EMC_093, getActivity(), getContext(), "Warning");
                       }
                   }else {
                       if(isPicking){
                           if(!isFromLocationScanned){
                               if(etFromLocation.getText().toString().equals(scannedData)){
                                   cvScanFromLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
                                   ivScanFromLocation.setImageResource(R.drawable.check);
                                   isFromLocationScanned=true;

                               }
                               else{
                                   cvScanFromLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
                                   ivScanFromLocation.setImageResource(R.drawable.warning_img);
                                   isFromLocationScanned=true;
                                   common.showUserDefinedAlertType(errorMessages.EMC_094, getActivity(), getContext(), "Error");
                               }
                           }
                           else{
                               if(isPalletScanned){
                                   if(etToLocation.getText().toString().equals(scannedData)){
                                       VNA_UpsertBintoBinTransfer(scannedData);
                                   }else{
                                       cvScanToLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
                                       ivScanToLocation.setImageResource(R.drawable.warning_img);
                                       isToLocationScanned=false;
                                       common.showUserDefinedAlertType(errorMessages.EMC_0013, getActivity(), getContext(), "Error");
                                   }
                               }
                               else{
                                   common.showUserDefinedAlertType(errorMessages.EMC_088, getActivity(), getContext(), "Error");
                               }
                           }
                       }else{
                           if(!isFromLocationScanned){
                               cvScanFromLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
                               ivScanFromLocation.setImageResource(R.drawable.check);
                               isFromLocationScanned=true;
                               etFromLocation.setText(scannedData);
                           }else{
                               if(isPalletScanned){
                                   if(etToLocation.getText().toString().equals(scannedData)){
                                       VNA_UpsertBintoBinTransfer(scannedData);
                                   }else{
                                       common.showUserDefinedAlertType(errorMessages.EMC_0013, getActivity(), getContext(), "Error");
                                   }
                               }else{
                                   common.showUserDefinedAlertType(errorMessages.EMC_088, getActivity(), getContext(), "Error");
                               }
                           }
                       }
                   }




            }else {
                if(!Common.isPopupActive())
                {
                    common.showUserDefinedAlertType(errorMessages.EMC_080, getActivity(), getContext(), "Error");

                }
                sound.alertWarning(getActivity(),getContext());

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
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.title_activity_vna_transfer));
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
        super.onDestroyView();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

        //storageloc=spinnerSelectSloc.getSelectedItem().toString();

    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }


    public void setSuggestionTypeApi(String suggestionType){
        try {
            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.VNA, getContext());
            InboundDTO inboundDTO = new InboundDTO();
            inboundDTO.setUserId(userId);
            inboundDTO.setSuggestionType(suggestionType);
            inboundDTO.setInoutId(inOutId);
            message.setEntityObject(inboundDTO);

            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method

                call = apiService.VNASuggestion(message);
                ProgressDialogUtils.showProgressDialog("Please Wait");
                // } else {
                // DialogUtils.showAlertDialog(getActivity(), "Please enable internet");
                // return;
                // }

            } catch (Exception ex) {
                try {
                    ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_01", getActivity());
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
                            if(response.body()!=null){
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
                                core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);
                                List<LinkedTreeMap<?, ?>> _lInbound = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lInbound = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                InboundDTO dto=null;
                                for (int i = 0; i < _lInbound.size(); i++) {
                                    dto = new InboundDTO(_lInbound.get(i).entrySet());
                                }

                                if(dto.getInout()!=null){

                                    if(SuggestionType.equals("1")){
                                        if(dto.getInoutId().equals("1")){
                                            isPicking=false;
                                            isPutaway=true;
                                            inOutId="1";
                                            etFromLocation.setText(dto.getPickedLocation());
                                            etPallet.setText(dto.getPalletNo());
                                            etToLocation.setText(dto.getSuggestedLocation());
                                            txtPenPallet.setText("Pending Pallet Count:"+dto.getPendingPalletcount());
                                            txtVLPDNumber.setText(dto.getVLPDNumber());
                                            lblVLPDNumber.setVisibility(View.GONE);
                                            txtVLPDNumber.setVisibility(View.GONE);
                                            txtPenPallet.setVisibility(View.GONE);
                                            btnSkip.setVisibility(View.VISIBLE);
                                        }else{
                                            isPicking=true;
                                            isPutaway=false;
                                            inOutId="2";
                                            etFromLocation.setText(dto.getPickedLocation());
                                            etPallet.setText(dto.getPalletNo());
                                            etToLocation.setText(dto.getSuggestedLocation());
                                            txtVLPDNumber.setText(dto.getVLPDNumber());
                                            txtPenPallet.setText("Pending Pallet Count: "+dto.getPendingPalletcount());
                                            lblVLPDNumber.setVisibility(View.VISIBLE);
                                            txtVLPDNumber.setVisibility(View.VISIBLE);
                                            txtPenPallet.setVisibility(View.VISIBLE);
                                            btnSkip.setVisibility(View.VISIBLE);
                                        }
                                    }else if(SuggestionType.equals("2")){
                                        isPicking=false;
                                        isPutaway=true;
                                        etFromLocation.setText(dto.getPickedLocation());
                                        etPallet.setText(dto.getPalletNo());
                                        etToLocation.setText(dto.getSuggestedLocation());
                                        etToLocation.setText(dto.getSuggestedLocation());
                                        txtVLPDNumber.setText(dto.getVLPDNumber());
                                        txtPenPallet.setText("Pending Pallet Count: "+dto.getPendingPalletcount());
                                        lblVLPDNumber.setVisibility(View.GONE);
                                        txtVLPDNumber.setVisibility(View.GONE);
                                        txtPenPallet.setVisibility(View.GONE);
                                        btnSkip.setVisibility(View.VISIBLE);
                                    }else{
                                        isPicking=true;
                                        isPutaway=false;
                                        etFromLocation.setText(dto.getPickedLocation());
                                        etPallet.setText(dto.getPalletNo());
                                        etToLocation.setText(dto.getSuggestedLocation());
                                        txtVLPDNumber.setText(dto.getVLPDNumber());
                                        txtPenPallet.setText("Pending Pallet Count: "+dto.getPendingPalletcount());
                                        lblVLPDNumber.setVisibility(View.VISIBLE);
                                        txtVLPDNumber.setVisibility(View.VISIBLE);
                                        txtPenPallet.setVisibility(View.VISIBLE);
                                        btnSkip.setVisibility(View.VISIBLE);
                                    }

/*                                    if(dto.getInoutId().equals("1")){
                                        isPicking=false;
                                        isPutaway=true;
                                        if(inOutId.equals("2")){
                                            etFromLocation.setText(dto.getPickedLocation());
                                            etPallet.setText(dto.getPalletNo());
                                            etToLocation.setText(dto.getSuggestedLocation());
                                        }
                                    }else{
                                        if(inOutId.equals("2")){
                                            etFromLocation.setText(dto.getPickedLocation());
                                            etPallet.setText(dto.getPalletNo());
                                            etToLocation.setText(dto.getSuggestedLocation());
                                        }
                                        if(dto.getInout().equals("PUTWAY"))
                                        {
                                            isPicking=false;
                                            isPutaway=true;
                                        }else{
                                            isPicking=true;
                                            isPutaway=false;
                                        }

                                    }*/

                                    if(isPicking){
                                        tvStRef.setText("Picking");
                                    }
                                    else{
                                        tvStRef.setText("Putaway");
                                    }

                                }else{

                                    clearAllFileds();
                                    common.showUserDefinedAlertType(errorMessages.EMC_095, getActivity(), getContext(), "Warning");
                                }
                                ProgressDialogUtils.closeProgressDialog();

                            }
                            }else {
                                ProgressDialogUtils.closeProgressDialog();
                                common.showUserDefinedAlertType(errorMessages.EMC_0021, getActivity(), getContext(), "Error");
                            }
                        } catch (Exception ex) {
                            try {
                                ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_02", getActivity());
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
                        //Toast.makeText(LoginActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
                        ProgressDialogUtils.closeProgressDialog();
                        common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
                    }
                });
            } catch (Exception ex) {
                try {
                    ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_03", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
            }
        } catch (Exception ex) {
            try {
                ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_04", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            common.showUserDefinedAlertType(errorMessages.EMC_0003, getActivity(), getContext(), "Error");
        }
    }


    private void VNAPalletScan(String scannedData) {

        try {
            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.VNA, getContext());
            InboundDTO inboundDTO = new InboundDTO();
            inboundDTO.setUserId(userId);
            inboundDTO.setPalletNo(scannedData);
            inboundDTO.setInoutId(inOutId);
            message.setEntityObject(inboundDTO);

            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method

                call = apiService.VNAPalletScan(message);
                ProgressDialogUtils.showProgressDialog("Please Wait");
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
                            if(response.body()!=null){
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
                                core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);
                                List<LinkedTreeMap<?, ?>> _lInbound = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lInbound = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                Log.v("ABCDE",new Gson().toJson(_lInbound));

                                InboundDTO inboundDTO1=null;
                                for(int i=0;i<_lInbound.size();i++){
                                    inboundDTO1=new InboundDTO(_lInbound.get(i).entrySet());
                                }

                                if(inboundDTO1.getResult().equals("1")){
                                    cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanPallet.setImageResource(R.drawable.check);
                                    isPalletScanned=true;
                                }
                                else{
                                    cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanPallet.setImageResource(R.drawable.warning_img);
                                    isPalletScanned=false;
                                    common.showUserDefinedAlertType(getString(R.string.please_scan_valid_pallet), getActivity(), getContext(), "Warning");
                                }
                                ProgressDialogUtils.closeProgressDialog();
                            }
                            }else {
                                ProgressDialogUtils.closeProgressDialog();
                                common.showUserDefinedAlertType(errorMessages.EMC_0021, getActivity(), getContext(), "Error");
                            }
                        } catch (Exception ex) {
                            try {
                                ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_02", getActivity());
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
                        //Toast.makeText(LoginActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
                        ProgressDialogUtils.closeProgressDialog();
                        common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
                    }
                });
            } catch (Exception ex) {
                try {
                    ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_03", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
            }
        } catch (Exception ex) {
            try {
                ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_04", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            common.showUserDefinedAlertType(errorMessages.EMC_0003, getActivity(), getContext(), "Error");
        }
    }

    private void VNA_UpsertBintoBinTransfer(String scannedData) {

        try {
            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.VNA, getContext());
            InboundDTO inboundDTO = new InboundDTO();
            inboundDTO.setUserId(userId);
            inboundDTO.setLocation(etFromLocation.getText().toString());
            inboundDTO.setPalletNo(etPallet.getText().toString());
            inboundDTO.setToLocation(etToLocation.getText().toString());
            inboundDTO.setPutwayType("0");
            inboundDTO.setInoutId(inOutId);
            inboundDTO.setSuggestionType(SuggestionType);
            if(isPicking)
                inboundDTO.setInout("2");
            else
                inboundDTO.setInout("1");
            message.setEntityObject(inboundDTO);


            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method

                call = apiService.VNA_UpsertBintoBinTransfer(message);
                ProgressDialogUtils.showProgressDialog("Please Wait");
                // } else {
                // DialogUtils.showAlertDialog(getActivity(), "Please enable internet");
                // return;
                // }

            } catch (Exception ex) {
                try {
                    ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_01", getActivity());
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
                            if(response.body()!=null){
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
                                core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);

                                List<LinkedTreeMap<?, ?>> _lInbound = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lInbound = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                InboundDTO dto=null;
                                for (int i = 0; i < _lInbound.size(); i++) {
                                    dto = new InboundDTO(_lInbound.get(i).entrySet());
                                }
                                ProgressDialogUtils.closeProgressDialog();
                                if(dto.getResult().equals("Successfully Transfer")){
                                    cvScanToLocation.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanToLocation.setImageResource(R.drawable.check);
                                    isToLocationScanned=true;
                                    inOutId=dto.getInoutId();
/*                                    if(SuggestionType.equals("2")){
                                        clearAllFileds1();
                                    }else{*/
                                    clearAllFileds();
                                    setSuggestionTypeApi(SuggestionType);

                                    /*}*/
                                }else{
                                    common.showUserDefinedAlertType(dto.getResult(), getActivity(), getContext(), "Error");
                                }
                            }
                            }else {
                                ProgressDialogUtils.closeProgressDialog();
                                common.showUserDefinedAlertType(errorMessages.EMC_0021, getActivity(), getContext(), "Error");
                            }
                        } catch (Exception ex) {
                            try {
                                ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_02", getActivity());
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
                        //Toast.makeText(LoginActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
                        ProgressDialogUtils.closeProgressDialog();
                        common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
                    }
                });
            } catch (Exception ex) {
                try {
                    ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_03", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
            }
        } catch (Exception ex) {
            try {
                ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_04", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            common.showUserDefinedAlertType(errorMessages.EMC_0003, getActivity(), getContext(), "Error");
        }
    }

    private void VNApickingSkip() {

        try {
            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.VNA, getContext());
            InboundDTO inboundDTO = new InboundDTO();
            inboundDTO.setUserId(userId);
            inboundDTO.setLocation(etFromLocation.getText().toString());
            inboundDTO.setPalletNo(etPallet.getText().toString());
            inboundDTO.setVLPDNumber(txtVLPDNumber.getText().toString());
            inboundDTO.setSkipReason(skipReason);
            message.setEntityObject(inboundDTO);

            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method

                call = apiService.VNApickingSkip(message);
                ProgressDialogUtils.showProgressDialog("Please Wait");

                // } else {
                // DialogUtils.showAlertDialog(getActivity(), "Please enable internet");
                // return;
                // }

            } catch (Exception ex) {
                try {
                    ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_01", getActivity());
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
                            if(response.body()!=null) {
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
                                    core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);
                                    ProgressDialogUtils.closeProgressDialog();
                                    List<LinkedTreeMap<?, ?>> _lInbound = new ArrayList<LinkedTreeMap<?, ?>>();
                                    _lInbound = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                    InboundDTO dto = null;
                                    for (int i = 0; i < _lInbound.size(); i++) {
                                        dto = new InboundDTO(_lInbound.get(i).entrySet());
                                    }

                                    Log.v("ABCDE", new Gson().toJson(dto));
                                    ProgressDialogUtils.closeProgressDialog();
                                    if (dto.getResult().equals("1") && dto.getResult() != null) {
                                        inOutId = "2";
                                        setSuggestionTypeApi(SuggestionType);
                                    } else {
                                        common.showUserDefinedAlertType("Cannot Skip", getActivity(), getContext(), "Error");
                                    }

                                }
                            }else {
                                ProgressDialogUtils.closeProgressDialog();
                                common.showUserDefinedAlertType(errorMessages.EMC_0021, getActivity(), getContext(), "Error");
                            }
                        } catch (Exception ex) {
                            try {
                                ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_02", getActivity());
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
                        //Toast.makeText(LoginActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
                        ProgressDialogUtils.closeProgressDialog();
                        common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
                    }
                });
            } catch (Exception ex) {
                try {
                    ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_03", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
            }
        } catch (Exception ex) {
            try {
                ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_04", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            common.showUserDefinedAlertType(errorMessages.EMC_0003, getActivity(), getContext(), "Error");
        }
    }



    private void StopSuggestion() {

        try {
            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.VNA, getContext());
            InboundDTO inboundDTO = new InboundDTO();
            inboundDTO.setUserId(userId);
            message.setEntityObject(inboundDTO);

            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                call = apiService.StopSuggestion(message);
                ProgressDialogUtils.showProgressDialog("Please Wait");
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
                            if(response.body()!=null){
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
                                core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);
                                List<LinkedTreeMap<?, ?>> _lInbound = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lInbound = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                InboundDTO inboundDTO1=null;
                                for(int i=0;i<_lInbound.size();i++){
                                    inboundDTO1=new InboundDTO(_lInbound.get(i).entrySet());
                                }

                                if(inboundDTO1.getResult().equals("1")){
                                    common.showUserDefinedAlertType("Success", getActivity(), getContext(), "Success");
                                    ((RadioButton)rootView.findViewById(R.id.radioAuto)).setEnabled(true);
                                    ((RadioButton)rootView.findViewById(R.id.radioPicking)).setEnabled(true);
                                    ((RadioButton)rootView.findViewById(R.id.radioPutaway)).setEnabled(true);
                                }else{
                                    common.showUserDefinedAlertType(inboundDTO1.getResult(), getActivity(), getContext(), "Error");
                                }

                                ProgressDialogUtils.closeProgressDialog();
                            }
                            }else {
                                ProgressDialogUtils.closeProgressDialog();
                                common.showUserDefinedAlertType(errorMessages.EMC_0021, getActivity(), getContext(), "Error");
                            }
                        } catch (Exception ex) {
                            try {
                                ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_02", getActivity());
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
                        //Toast.makeText(LoginActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
                        ProgressDialogUtils.closeProgressDialog();
                        common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
                    }
                });
            } catch (Exception ex) {
                try {
                    ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_03", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
            }
        } catch (Exception ex) {
            try {
                ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_04", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            common.showUserDefinedAlertType(errorMessages.EMC_0003, getActivity(), getContext(), "Error");
        }
    }

    private void GeneratePutawayandPickingSuggestion() {

        try {
            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.VNA, getContext());
            InboundDTO inboundDTO = new InboundDTO();
            inboundDTO.setUserId(userId);
            message.setEntityObject(inboundDTO);

            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                call = apiService.GeneratePutawayandPickingSuggestion(message);
                ProgressDialogUtils.showProgressDialog("Please Wait");
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
                            if(response.body()!=null){
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
                                core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);
                                List<LinkedTreeMap<?, ?>> _lInbound = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lInbound = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                InboundDTO inboundDTO1=null;
                                for(int i=0;i<_lInbound.size();i++) {
                                    inboundDTO1 = new InboundDTO(_lInbound.get(i).entrySet());
                                }

                                if(inboundDTO1.getResult().equals("Success")){
                                    common.showUserDefinedAlertType(inboundDTO1.getResult(), getActivity(), getContext(), "Success");
                                    ((RadioButton)rootView.findViewById(R.id.radioAuto)).setEnabled(false);
                                    ((RadioButton)rootView.findViewById(R.id.radioPicking)).setEnabled(false);
                                    ((RadioButton)rootView.findViewById(R.id.radioPutaway)).setEnabled(false);
                                    ((RadioButton)rootView.findViewById(R.id.radioAuto)).setChecked(false);
                                    ((RadioButton)rootView.findViewById(R.id.radioPicking)).setChecked(false);
                                    ((RadioButton)rootView.findViewById(R.id.radioPutaway)).setChecked(false);
                                }else{
                                    common.showUserDefinedAlertType(inboundDTO1.getResult(), getActivity(), getContext(), "Error");
                                }

                                ProgressDialogUtils.closeProgressDialog();
                            }
                            }else {
                                ProgressDialogUtils.closeProgressDialog();
                                common.showUserDefinedAlertType(errorMessages.EMC_0021, getActivity(), getContext(), "Error");
                            }
                        } catch (Exception ex) {
                            try {
                                ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_02", getActivity());
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
                        //Toast.makeText(LoginActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
                        ProgressDialogUtils.closeProgressDialog();
                        common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
                    }
                });
            } catch (Exception ex) {
                try {
                    ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_03", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
            }
        } catch (Exception ex) {
            try {
                ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_04", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            common.showUserDefinedAlertType(errorMessages.EMC_0003, getActivity(), getContext(), "Error");
        }
    }





    private void VNAPutawaySkip() {

        try {
            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.VNA, getContext());
            InboundDTO inboundDTO = new InboundDTO();
            inboundDTO.setUserId(userId);
            inboundDTO.setLocation(etFromLocation.getText().toString());
            inboundDTO.setSuggestedLocation(etToLocation.getText().toString());
            inboundDTO.setPalletNo(etPallet.getText().toString());
            inboundDTO.setVLPDNumber(txtVLPDNumber.getText().toString());
            inboundDTO.setSkipReason(skipReason);
            message.setEntityObject(inboundDTO);

            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method

                call = apiService.VNAPutawaySkip(message);
                ProgressDialogUtils.showProgressDialog("Please Wait");

                // } else {
                // DialogUtils.showAlertDialog(getActivity(), "Please enable internet");
                // return;
                // }

            } catch (Exception ex) {
                try {
                    ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_01", getActivity());
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
                            if (response.body()!=null) {
                                core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);

                                if ((core.getType().toString().equals("Exception"))) {
                                    List<LinkedTreeMap<?, ?>> _lExceptions = new ArrayList<LinkedTreeMap<?, ?>>();
                                    _lExceptions = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                    WMSExceptionMessage owmsExceptionMessage = null;
                                    for (int i = 0; i < _lExceptions.size(); i++) {
                                        owmsExceptionMessage = new WMSExceptionMessage(_lExceptions.get(i).entrySet());
                                    }
                                    ProgressDialogUtils.closeProgressDialog();
                                    if (owmsExceptionMessage.getWMSMessage().equalsIgnoreCase(getString(R.string.no_locations_available_in_the_rack))) {
                                        clearAllFileds1();
                                    }
                                    common.showAlertType(owmsExceptionMessage, getActivity(), getContext());

                                } else {
                                    core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);
                                    ProgressDialogUtils.closeProgressDialog();
                                    List<LinkedTreeMap<?, ?>> _lInbound = new ArrayList<LinkedTreeMap<?, ?>>();
                                    _lInbound = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                    InboundDTO dto = null;
                                    for (int i = 0; i < _lInbound.size(); i++) {
                                        dto = new InboundDTO(_lInbound.get(i).entrySet());
                                    }

                                    Log.v("ABCDE", new Gson().toJson(dto));
                                    ProgressDialogUtils.closeProgressDialog();
                                    if (dto.getResult().equalsIgnoreCase("Success")) {
                                        inOutId = "1";
                                        etToLocation.setText(dto.getSuggestedLocation());
                                        etFromLocation.setText(dto.getPickedLocation());
                                        etPallet.setText(dto.getPalletNo());
                                    } else {
                                        common.showUserDefinedAlertType("Cannot Skip", getActivity(), getContext(), "Error");
                                    }

                                }
                            }
                            else {
                                ProgressDialogUtils.closeProgressDialog();
                                common.showUserDefinedAlertType(errorMessages.EMC_0021, getActivity(), getContext(), "Error");
                            }
                        } catch (Exception ex) {
                            try {
                                ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_02", getActivity());
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
                        //Toast.makeText(LoginActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
                        ProgressDialogUtils.closeProgressDialog();
                        common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
                    }
                });
            } catch (Exception ex) {
                try {
                    ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_03", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
            }
        } catch (Exception ex) {
            try {
                ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_04", getActivity());
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
            String textFromFile = ExceptionLoggerUtils.readFromFile(getActivity());
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
                common.showUserDefinedAlertType(errorMessages.EMC_0002, getActivity(), getContext(), "Error");
            }
            try {
                //Getting response from the method
                call.enqueue(new Callback<String>() {

                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {

                        try {

                            core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);


                        } catch (Exception ex) {

                            try {
                                ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "002", getContext());

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            logException();


                            ProgressDialogUtils.closeProgressDialog();
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable throwable) {
                        ProgressDialogUtils.closeProgressDialog();
                        //Toast.makeText(LoginActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
                        common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
                    }
                });
            } catch (Exception ex) {
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0003, getActivity(), getContext(), "Error");
            }
        } catch (Exception ex) {
            ProgressDialogUtils.closeProgressDialog();
            common.showUserDefinedAlertType(errorMessages.EMC_0003, getActivity(), getContext(), "Error");
        }
    }

}