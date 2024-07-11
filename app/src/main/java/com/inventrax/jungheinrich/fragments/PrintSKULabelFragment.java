package com.inventrax.jungheinrich.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.cipherlab.barcode.GeneralString;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import com.inventrax.jungheinrich.common.Common;
import com.inventrax.jungheinrich.common.constants.EndpointConstants;
import com.inventrax.jungheinrich.common.constants.ErrorMessages;
import com.inventrax.jungheinrich.interfaces.ApiInterface;
import com.inventrax.jungheinrich.pojos.HouseKeepingDTO;
import com.inventrax.jungheinrich.pojos.PrintDTO;
import com.inventrax.jungheinrich.pojos.ScanDTO;
import com.inventrax.jungheinrich.pojos.WMSCoreMessage;
import com.inventrax.jungheinrich.pojos.WMSExceptionMessage;
import com.inventrax.jungheinrich.searchableSpinner.SearchableSpinner;
import com.inventrax.jungheinrich.services.RetrofitBuilderHttpsEx;
import com.inventrax.jungheinrich.util.DialogUtils;
import com.inventrax.jungheinrich.util.ExceptionLoggerUtils;
import com.inventrax.jungheinrich.util.ProgressDialogUtils;
import com.inventrax.jungheinrich.util.ScanValidator;
import com.inventrax.jungheinrich.util.SoundUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class PrintSKULabelFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemSelectedListener, BarcodeReader.TriggerListener, BarcodeReader.BarcodeListener {
    View rootView;
    private SearchableSpinner  spinnerSelectPrinter,etLabelType;
    private EditText etSKUCode, etSKUDesc, etBatch, etSerial, etExpDate, etMfgDate, etMRP, etProjectRef, etPrintQty;
    private FloatingActionButton fab_print;
    byte FONT_TYPE;
    private static BluetoothSocket btsocket;
    private static OutputStream btoutputstream;
    BluetoothAdapter bAdapter;
    String selectedDevice = "",selectedlabletype;
    String selectLabelType, scannedSKU = null;
    private static final String classCode = "API_FRAG_0011";

    String[] lstlabelTypes = new String[]{ "3x4", "4x4"};


    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;

    // needed for communication to bluetooth device / network
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;

    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;
    Set<BluetoothDevice> pairedDevices;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;

    private String Materialcode = null, Userid = null, scanType = "", accountId = "", storageloc = "", ZPlString = "", PrintDesc = "";
    Common common;
    private Gson gson;
    private WMSCoreMessage core;
    private ExceptionLoggerUtils exceptionLoggerUtils;
    private ErrorMessages errorMessages;
    Boolean isPalletScaned = false, isLocationScaned = false, isSKUScanned = false, IsProceedForBinTransfer = false;
    private SoundUtils soundUtils;
    private String selectedTenant = "", selectedWH = "", tenantId = "", whId = "";
    List<HouseKeepingDTO> lstTenants = null;
    List<HouseKeepingDTO> lstWarehouse = null;
    private SearchableSpinner spinnerSelectTenant, spinnerSelectWarehouse;

    String labelType;

    private CardView cvScanRSN;
    private ImageView ivScanRSN;
    String scanner = null;
    private IntentFilter filter;
    String getScanner = null;
    private static BarcodeReader barcodeReader;
    private AidcManager manager;
    public PrintSKULabelFragment() {

    }
    public void myScannedData(Context context, String barcode){
        try {
            ProcessScannedinfo(barcode.trim().toString());
        }catch (Exception e){
            //  Toast.makeText(context, ""+e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private final BroadcastReceiver myDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scanner = intent.getStringExtra(GeneralString.BcReaderData);  // Scanned Barcode info
            ProcessScannedinfo(scanner.trim().toString());
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_print_sku_label, container, false);

        loadFromControllers();


        return rootView;

    }
    public void ProcessScannedinfo(String scannedData) {


        if(((DrawerLayout) getActivity().findViewById(R.id.drawer_layout)).isDrawerOpen(GravityCompat.START)){
            return;
        }

        if (ProgressDialogUtils.isProgressActive() || Common.isPopupActive()) {
            common.showUserDefinedAlertType(errorMessages.EMC_082, getActivity(), getContext(), "Warning");
            return;
        }


        if (scannedData != null) {

                // Validate Material

                if (ScanValidator.isRSNScanned(scannedData)) {
                    scannedData = scannedData.split("[-]", 2)[0];
                }
                ValiDateMaterial(scannedData);



        }
    }
    void loadFromControllers() {

        SharedPreferences sp = getActivity().getSharedPreferences("LoginActivity", Context.MODE_PRIVATE);
        Userid = sp.getString("RefUserId", "");
        scanType = sp.getString("scanType", "");
        accountId = sp.getString("AccountId", "");

        spinnerSelectPrinter = (SearchableSpinner) rootView.findViewById(R.id.spinnerSelectPrinter);
        etLabelType = (SearchableSpinner) rootView.findViewById(R.id.etLabelType);
        cvScanRSN = (CardView) rootView.findViewById(R.id.cvScanRSN);
        ivScanRSN = (ImageView) rootView.findViewById(R.id.ivScanRSN);
        etSKUCode = (EditText) rootView.findViewById(R.id.etSKUCode);
        etSKUDesc = (EditText) rootView.findViewById(R.id.etSKUDesc);
        etBatch = (EditText) rootView.findViewById(R.id.etBatch);
        etSerial = (EditText) rootView.findViewById(R.id.etSerial);
        etExpDate = (EditText) rootView.findViewById(R.id.etExpDate);
        etMfgDate = (EditText) rootView.findViewById(R.id.etMfgDate);
        etMRP = (EditText) rootView.findViewById(R.id.etMRP);
        etProjectRef = (EditText) rootView.findViewById(R.id.etProjectRef);
        etPrintQty = (EditText) rootView.findViewById(R.id.etPrintQty);
        fab_print = (FloatingActionButton) rootView.findViewById(R.id.fab_print);
        fab_print.setOnClickListener(this);
        spinnerSelectPrinter.setOnItemSelectedListener(this);
        etLabelType.setOnItemSelectedListener(this);
        // spinnerSelectLabelType.setOnItemSelectedListener(this);
        common = new Common();
        gson = new GsonBuilder().create();
        core = new WMSCoreMessage();
        ProgressDialogUtils.closeProgressDialog();
        common.setIsPopupActive(false);

        lstTenants = new ArrayList<HouseKeepingDTO>();
        lstWarehouse = new ArrayList<HouseKeepingDTO>();
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



        ArrayAdapter arrayAdapterSLoc = new ArrayAdapter(getActivity(), R.layout.support_simple_spinner_dropdown_item, lstlabelTypes);
        etLabelType.setAdapter(arrayAdapterSLoc);

   /*     bAdapter = BluetoothAdapter.getDefaultAdapter();
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

        }
        pairedDevices = bAdapter.getBondedDevices();*/


        // findBT();

        getPairedPrinters();


        selectedTenant = getArguments().getString("tenantId");
        selectedWH = getArguments().getString("warehouseId");


       /* try {
            openBT();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab_print:
                //connect();

                try {
                    sendData();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //getPairedPrinters();

                break;
            default:
                break;
        }
    }


    void getPairedPrinters() {
        // put the

            // List all the bonded devices(paired)
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
                if (Build.VERSION.SDK_INT >= 31) {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 100);
                    return;
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                bluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
            }
            if (Build.VERSION.SDK_INT >= 31) {
                bluetoothAdapter = bluetoothManager.getAdapter();
            } else {
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }

            pairedDevices = bluetoothAdapter.getBondedDevices();
            List<String> lstDeviceNames = new ArrayList<>();
            lstDeviceNames.add("select printer");
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    lstDeviceNames.add(device.getName());

                }

                ArrayAdapter arrayAdapterSLoc = new ArrayAdapter(getActivity(), R.layout.support_simple_spinner_dropdown_item, lstDeviceNames);
                spinnerSelectPrinter.setAdapter(arrayAdapterSLoc);


            }

    }



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
    public void onDestroy() {
        super.onDestroy();
        try {
            if (btsocket != null) {
                btoutputstream.close();
                btsocket.close();
                btsocket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.spinnerSelectPrinter:
                selectedDevice = spinnerSelectPrinter.getSelectedItem().toString();
                if (selectedDevice.isEmpty() && selectedDevice.equalsIgnoreCase("select printer")) {

                } else {
                    if (pairedDevices.size() > 0) {
                        for (BluetoothDevice device : pairedDevices) {
                            if (device.getName().equals(selectedDevice)) {
                                device.getAddress();
                                Log.i("mac", device.getAddress());
                                mmDevice = device;
                                try {
                                    openBT();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                        }
                    }
                }

            case R.id.etLabelType:
                selectedlabletype = etLabelType.getSelectedItem().toString();
                getItemMasterPrint();
                break;

        }

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }


    void openBT() throws IOException {
        try {
            // Standard SerialPortService ID
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.

            }
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            mmSocket.connect();
            mmOutputStream = mmSocket.getOutputStream();
            mmInputStream = mmSocket.getInputStream();
            beginListenForData();
            //   myLabel.setText("Bluetooth Opened");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * after opening a connection to bluetooth printer device,
     * we have to listen and check if a data were sent to be printed.
     */

    void beginListenForData() {
        try {
            final Handler handler = new Handler();

            // this is the ASCII code for a newline character
            final byte delimiter = 10;

            stopWorker = false;
            readBufferPosition = 0;
            readBuffer = new byte[1024];

            workerThread = new Thread(new Runnable() {
                public void run() {

                    while (!Thread.currentThread().isInterrupted() && !stopWorker) {

                        try {

                            int bytesAvailable = mmInputStream.available();

                            if (bytesAvailable > 0) {

                                byte[] packetBytes = new byte[bytesAvailable];
                                mmInputStream.read(packetBytes);

                                for (int i = 0; i < bytesAvailable; i++) {

                                    byte b = packetBytes[i];
                                    if (b == delimiter) {

                                        byte[] encodedBytes = new byte[readBufferPosition];
                                        System.arraycopy(
                                                readBuffer, 0,
                                                encodedBytes, 0,
                                                encodedBytes.length
                                        );

                                        // specify US-ASCII encoding
                                        final String data = new String(encodedBytes, "US-ASCII");
                                        readBufferPosition = 0;

                                        // tell the user data were sent to bluetooth printer device
                                        handler.post(new Runnable() {
                                            public void run() {
                                                // myLabel.setText(data);
                                            }
                                        });

                                    } else {
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }
                            }

                        } catch (IOException ex) {
                            stopWorker = true;
                        }

                    }
                }
            });

            workerThread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // this will send text data to be printed by the bluetooth printer

    void sendData() throws IOException {
        try {


            String barcodeString = etSKUCode.getText().toString()
                    + "|" + etBatch.getText().toString()
                    + "|" + etMfgDate.getText().toString()
                    + "|" + etExpDate.getText().toString()
                    + "|" + etProjectRef.getText().toString()
                    + "||||"+ 1+"|"+1;
            String replacedZPlString = null;
            // the text typed by the user
            replacedZPlString = ZPlString.replace("@SKU", "Part# :"+ " " + etSKUCode.getText().toString())
                    .replace("@Desc.","Desc :"+ " " + etSKUDesc.getText().toString())
                    .replace("@Mfg.Date","Mfg.Date :" + " "+ etMfgDate.getText().toString())
                    .replace("@Exp.Date", "Exp.Date :" + " "+ etExpDate.getText().toString())
                    .replace("@Batch","Batch # /Lot :" + " " + etBatch.getText().toString())
                    .replace("@Supplier Lot", "Supplier Lot :" + " " + etSerial.getText().toString())
                    .replace("@Project Ref No.","Project Ref # :" + " "+ etProjectRef.getText().toString())
                    .replace("@NoofLabels", etPrintQty.getText().toString())
                    .replace("barcodegeneratorcodewithmfgandexp", barcodeString);

            // the text typed by the user
            String msg = replacedZPlString;


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (mmOutputStream!=null) {
                    mmOutputStream.write(msg.getBytes(StandardCharsets.UTF_8));
                }else {
                    openBT();
                    sendData();
                }
            }
            // tell the user data were sent
            //  myLabel.setText("Data sent.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void getItemMasterPrint() {

        try {


            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.HouseKeepingDTO, getContext());
            PrintDTO printDTO = new PrintDTO();
            printDTO.setTenantID(selectedTenant);
            printDTO.setLabletype("SKU");
            printDTO.setWareHouseID(selectedWH);
            if (selectedlabletype.equalsIgnoreCase("3x4")) {
                printDTO.setLabletype("QRCodeNew1");
            }
            else {
                printDTO.setLabletype("QRCodeNew");
            }
            message.setEntityObject(printDTO);


            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                call = apiService.ItemMasterPrint(message);
                ProgressDialogUtils.showProgressDialog(getString(R.string.please_wait));
                // } else {
                // DialogUtils.showAlertDialog(getActivity(), "Please enable internet");
                // return;

                // }

            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_01", getActivity());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                DialogUtils.showAlertDialog(getActivity(), getActivity().getResources().getString(R.string.EMC_0002));

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

                                List<LinkedTreeMap<?, ?>> _lstActiveStock = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lstActiveStock = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();


                                ProgressDialogUtils.closeProgressDialog();

                                for (int i = 0; i < _lstActiveStock.size(); i++) {
                                    PrintDTO printDTO1 = new PrintDTO(_lstActiveStock.get(0).entrySet());


                                    ZPlString = printDTO1.getZPLScript();
                                    PrintDesc = printDTO1.getPrintDesc();

                                }

                            }
                        } catch (Exception ex) {
                            try {
                                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_02", getActivity());

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
                        DialogUtils.showAlertDialog(getActivity(), getActivity().getResources().getString(R.string.EMC_0001));
                    }
                });
            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_03", getActivity());

                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                DialogUtils.showAlertDialog(getActivity(), getActivity().getResources().getString(R.string.EMC_0001));
            }
        } catch (Exception ex) {
            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_04", getActivity());

            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            DialogUtils.showAlertDialog(getActivity(), getActivity().getResources().getString(R.string.EMC_0003));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.title_activity_printLabel));
        if (selectedDevice.isEmpty() && selectedDevice.equalsIgnoreCase("select printer")) {

        } else {
          /*  if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {

                    // RPP300 is the name of the bluetooth printer device
                    // we got this name from the list of paired devices

                    if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    if (device.getName().equals(selectedDevice)) {
                        device.getAddress();


                        Log.i("mac", device.getAddress());
                        mmDevice = device;
                        try {
                            openBT();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            }*/
        }
    }

    public void ValiDateMaterial(final String scannedData) {
        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.ScanDTO, getContext());
            ScanDTO scanDTO = new ScanDTO();
            scanDTO.setUserID(Userid);
            scanDTO.setAccountID(accountId);
            scanDTO.setTenantID(String.valueOf(selectedTenant));
            scanDTO.setWarehouseID(String.valueOf(selectedWH));
            scanDTO.setScanInput(scannedData);
            //  scanDTO.setInboundID(inboundId);
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
                            scannedSKU = "";
                            cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
                            ivScanRSN.setImageResource(R.drawable.fullscreen_img);
                            ProgressDialogUtils.closeProgressDialog();
                            common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                        } else {
                            LinkedTreeMap<?, ?>_lResult = new LinkedTreeMap<>();
                            _lResult = (LinkedTreeMap<?, ?>) core.getEntityObject();

                            Log.v("ABCDE",new Gson().toJson(_lResult));

                            ScanDTO scanDTO1=new ScanDTO(_lResult.entrySet());
                            ProgressDialogUtils.closeProgressDialog();
                            if(scanDTO1!=null){
                                if(scanDTO1.getScanResult()){

                                /* ----For RSN reference----
                                    0 Sku|1 BatchNo|2 SerialNO|3 MFGDate|4 EXpDate|5 ProjectRefNO|6 Kit Id|7 line No|8 MRP ---- For SKU with 9 MSP's
                                    0 Sku|1 BatchNo|2 SerialNO|3 KitId|4 lineNo  ---- For SKU with 5 MSP's   *//*
                                    // Eg. : ToyCar|1|bat1|ser123|12/2/2018|12/2/2019|0|001*/

                                    cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.skuColor));
                                    ivScanRSN.setImageResource(R.drawable.fullscreen_img);

                                    /*    if (scannedData.split("[|]").length != 5) {*/

                                    /*Materialcode = scanDTO1.getSkuCode();
                                    etBatch.setText(scanDTO1.getBatch());
                                    etSerial.setText(scanDTO1.getSerialNumber());
                                    etMfgDate.setText(scanDTO1.getMfgDate());
                                    etExpDate.setText(scanDTO1.getExpDate());
                                    etPrjRef.setText(scanDTO1.getPrjRef());
                                    etKidID.setText(scanDTO1.getKitID());
                                    etMRP.setText(scanDTO1.getMrp());
                                    lineNo = scanDTO1.getLineNumber();*/
                                    //supplierInvoiceDetailsId = scanDTO1.getSupplierInvoiceDetailsID();

                                    etSKUCode.setText(scanDTO1.getSkuCode());
                                    scannedSKU = scanDTO1.getSkuCode();
                                    etBatch.setText(scanDTO1.getBatch());
                                    etSerial.setText(scanDTO1.getSerialNumber());
                                    etMfgDate.setText(scanDTO1.getMfgDate());
                                    etExpDate.setText(scanDTO1.getExpDate());
                                  etProjectRef.setText(scanDTO1.getPrjRef());
                                  etMRP.setText(scanDTO1.getMrp());

                                    isSKUScanned = true;
                                    cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanRSN.setImageResource(R.drawable.check);


                                    //   etMRP.setText(scannedData.split("[|]")[7]);


/*                                    } else {
                                        Materialcode = scannedData.split("[|]")[0];
                                        etBatch.setText(scannedData.split("[|]")[1]);
                                        etSerial.setText(scannedData.split("[|]")[2]);
                                        etKidID.setText(scannedData.split("[|]")[3]);
                                        lineNo = scannedData.split("[|]")[4];
                                    }*/




/*                                    if (scanType.equals("Auto")) {
                                        etQty.setText("1");
                                        getReceivedQty();          // To get the pending and received quantities
                                        return;
                                    } else {
                                        // for Manual mode
                                        etQty.setEnabled(true);
                                        btnReceive.setEnabled(true);
                                        btnReceive.setTextColor(getResources().getColor(R.color.white));
                                        btnReceive.setBackgroundResource(R.drawable.button_shape);
                                        lblInboundQty.setText("");
                                        getreceivedQty();           // To get the pending and received quantities
                                    }*/
                                } else{
                                    scannedSKU = "";
                                    cvScanRSN.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanRSN.setImageResource(R.drawable.warning_img);
                                    common.showUserDefinedAlertType(errorMessages.EMC_0009, getActivity(), getContext(), "Warning");
                                }
                            }else{
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
    public void onTriggerEvent(TriggerStateChangeEvent triggerStateChangeEvent) {

    }

    @Override
    public void onBarcodeEvent(BarcodeReadEvent barcodeReadEvent) {
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
}