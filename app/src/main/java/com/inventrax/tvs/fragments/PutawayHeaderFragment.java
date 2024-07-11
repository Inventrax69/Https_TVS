 package com.inventrax.tvs.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.inventrax.tvs.R;
import com.inventrax.tvs.adapters.ExpandableListAdapter;
import com.inventrax.tvs.common.Common;
import com.inventrax.tvs.common.constants.EndpointConstants;
import com.inventrax.tvs.common.constants.ErrorMessages;
import com.inventrax.tvs.interfaces.ApiInterface;
import com.inventrax.tvs.model.MaterialInfo;
import com.inventrax.tvs.model.PalletInfo;
import com.inventrax.tvs.pojos.InventoryDTO;
import com.inventrax.tvs.pojos.WMSCoreMessage;
import com.inventrax.tvs.pojos.WMSExceptionMessage;
import com.inventrax.tvs.services.RestService;
import com.inventrax.tvs.util.ExceptionLoggerUtils;
import com.inventrax.tvs.util.FragmentUtils;
import com.inventrax.tvs.util.ProgressDialogUtils;
import com.inventrax.tvs.util.ScanValidator;
import com.inventrax.tvs.util.SoundUtils;

import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


 public class PutawayHeaderFragment extends Fragment implements View.OnClickListener, BarcodeReader.TriggerListener, BarcodeReader.BarcodeListener {
 
     private static final String classCode = "API_FRAG_PutAwayHeaderFragment_";
     private View rootView;
     private ScanValidator scanValidator;
     public RelativeLayout rlPutawaySuggestions;
     private TextView lblSuggestedText, lbldesc, lblScanAllPallets;
     private Button btnCloseOne;
     private ExpandableListView expListView;
     //For Honey well barcode
     private static BarcodeReader barcodeReader;
     private AidcManager manager;
     private WMSCoreMessage core;
     private Common common = null;
     List<String> lstPalletCodes;
     ExpandableListAdapter listAdapter = null;
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
     public PutawayHeaderFragment() {
 
     }
 
     public boolean IsPutaway = false;
 
     @Nullable
     @Override
     public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
 
         rootView = inflater.inflate(R.layout.fragment_putaway_header, container, false);
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
         btnCloseOne = (Button) rootView.findViewById(R.id.btnCloseOne);
 
         lblScanAllPallets = (TextView) rootView.findViewById(R.id.lblScanAllPallets);
 
         btnCloseOne.setOnClickListener(this);
 
         expListView = (ExpandableListView) rootView.findViewById(R.id.lvExp);
         LocaMaterialInfoList = new HashMap<String, List<String>>();
         PalletnumberHeaderList = new ArrayList<String>();
         lstPalletCodes = new ArrayList<String>();
         common = new Common();
         gson = new GsonBuilder().create();
         errorMessages = new ErrorMessages();
         soundUtils = new SoundUtils();
         // For Cipher Barcode reader
         Intent RTintent = new Intent("sw.reader.decode.require");
         RTintent.putExtra("Enable", true);
         getActivity().sendBroadcast(RTintent);
         this.filter = new IntentFilter();
         this.filter.addAction("sw.reader.decode.complete");
         getActivity().registerReceiver(this.myDataReceiver, this.filter);
         if (getArguments() != null) {
             if (getArguments().getString("PalletNo") != null) {
                 try {
 
                     PalletnumberHeaderList.add(getArguments().getString("PalletNo"));
                     lblScanAllPallets.setText(getArguments().getString("PalletNo"));
 
                 } catch (Exception ex) {
 
                 }
 
             }
             if (getArguments().getString("LocaMaterialInfojson") != null) {
                 try {
                     LocaMaterialInfoList = new HashMap<String, List<String>>();
                     JSONArray jsonArray = new JSONArray(getArguments().getString("LocaMaterialInfojson").toString());
 
                     for (int i = 0; i < jsonArray.length(); i++) {
                         MaterialQty.add(jsonArray.getString(i));
                         LocaMaterialInfoList.put(getArguments().getString("PalletNo"), MaterialQty);
                     }
                 } catch (Exception ex) {
 
                 }
 
 
             }
             if (getArguments().getBoolean("IsFetchSuggestions")) {
 
 
                 try {
                     IsFetchSuggestions = getArguments().getBoolean("IsFetchSuggestions");
                     RegeneratePalletcode = getArguments().getString("RegeneratePalletcode");
                     if (IsFetchSuggestions) {
                         lblScanAllPallets.setText(RegeneratePalletcode);
                         new Handler().postDelayed(new Runnable() {
                             @Override
                             public void run() {
 
                                 GetPutawaysuggesttions();
                             }
                         }, 5000);
 
 
                     }
                 } catch (Exception ex) {
 
                 }
             }
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
 
 
 
     }
 
     //Button clicks
     @Override
     public void onClick(View v) {
 
         switch (v.getId()) {
 
             case R.id.btnCloseOne:
                 FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, new HomeFragment());
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
 
 
     //Assigning scanned value to the respective fields
     public void ProcessScannedinfo(String scannedData) {
         if (scannedData != null && !Common.isPopupActive) {
             //mahi
 //            ispalletisscanned();
          if (ScanValidator.isContainerScanned(scannedData)) {
                 lblScanAllPallets.setText(scannedData);
              GetPutawaysuggesttions();
 
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
         ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.title_activity_putaway));
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
 
 
     /*
           Generating the Suggestions based on Scanned pallet
      */
     public void GetPutawaysuggesttions() {
         if (lblScanAllPallets.getText().toString().equals("Scan Pallet")) {
             common.showUserDefinedAlertType(errorMessages.EMC_0019, getActivity(), getContext(), "Error");
             return;
         }
         WMSCoreMessage message = new WMSCoreMessage();
         message = common.SetAuthentication(EndpointConstants.Inventory, getContext());
         InventoryDTO iventoryDTO = new InventoryDTO();
         iventoryDTO.setContainerCode(lblScanAllPallets.getText().toString());
         iventoryDTO.setUserId(userId);
         message.setEntityObject(iventoryDTO);
 
 
         Call<String> call = null;
         ApiInterface apiService = RestService.getClient().create(ApiInterface.class);
 
         try {
             //Checking for Internet Connectivity
             // if (NetworkUtils.isInternetAvailable()) {
             // Calling the Interface method
             ProgressDialogUtils.showProgressDialog("Please Wait");
             call = apiService.GeneratePutawaySuggestions(message);
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
                                 List<LinkedTreeMap<?, ?>> _lInventory = new ArrayList<LinkedTreeMap<?, ?>>();
                                 _lInventory = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();
                                 //Converting List of Inventory
                                 List<InventoryDTO> lstInventory = new ArrayList<InventoryDTO>();
                                 //Header for Adapter
                                 List<String> lstPalletnumberHeader = new ArrayList<String>();
                                 List<String> lstTenantIDHeader = new ArrayList<String>();
                                 //child for Hashmap
                                 HashMap<String, List<String>> lstLocaMaterialInfo = new HashMap<String, List<String>>();
 
                                 Log.v("ABCDE",new Gson().toJson(core));
 
                                 InventoryDTO inventorydto = null;
                                 //Iterating through the Inventory list
                                 for (int i = 0; i < _lInventory.size(); i++) {
 
                                     // Each individual value of the map into DTO
                                     inventorydto = new InventoryDTO(_lInventory.get(i).entrySet());
                                     //Adding Inventory Dto into List
                                     lstInventory.add(inventorydto);
                                 }
 
 
                                 PalletInfo palletInfo = new PalletInfo();
                                 List<PalletInfo> lstPalletinfo = new ArrayList<PalletInfo>();
 
                                 List<MaterialInfo> lstMaterilinfo = new ArrayList<MaterialInfo>();
                                 List<String> lstLocMaterialQty = new ArrayList<String>();
 
 
                                 //Iterating to List of Inventory and Mapping it into  Local Model class
                                 for (InventoryDTO inventoryDTO : lstInventory) {
                                     MaterialInfo materialInfo = new MaterialInfo();
                                     materialInfo.setMcode(inventoryDTO.getMaterialCode());
                                     materialInfo.setLocation(inventoryDTO.getLocationCode());
                                     materialInfo.setQty((int)Double.parseDouble(inventoryDTO.getQuantity()));
                                     materialInfo.setSuggestionId(inventoryDTO.getSuggestionID());
                                     materialInfo.setWareHouseID(inventoryDTO.getWarehouseId());
                                     materialInfo.setTenantID(inventoryDTO.getTenantID());
                                     palletInfo.setPalletCode(inventoryDTO.getContainerCode());
                                     lstMaterilinfo.add(materialInfo);
                                 }
 
                                 // added Material Information into list
                                 // Setting Material information to Palletinfo
                                 palletInfo.setMaterialinfo(lstMaterilinfo);
                                 // Add pallet info to List
                                 lstPalletinfo.add(palletInfo);
                                 palletInfo.setPalletCode(lblScanAllPallets.getText().toString());
                                 // Adding Pallet no to the Header list
                                 lstPalletnumberHeader.add(palletInfo.getPalletCode());
 
                                 for (int Palletlist = 0; Palletlist < palletInfo.getMaterialinfo().size(); Palletlist++) {
                                     // Adding the Loc,Matrial,Qty to the list
                                     /*lstLocMaterialQty.add(palletInfo.getMaterialinfo().get(Palletlist).getLocation() + System.getProperty("line.separator") +
                                             palletInfo.getMaterialinfo().get(Palletlist).getMcode() + "/"
                                             + palletInfo.getMaterialinfo().get(Palletlist).getQty() +
                                             "/" + palletInfo.getMaterialinfo().get(Palletlist).getSuggestionId());
                                            *//* +"/" + palletInfo.getMaterialinfo().get(Palletlist).getTenantID()+"/"
                                             + palletInfo.getMaterialinfo().get(Palletlist).getWareHouseID());
                                 */
 
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
 
                                 listAdapter = new ExpandableListAdapter(getActivity(), getContext(), lstPalletnumberHeader, lstLocaMaterialInfo);
                                 expListView.setAdapter(listAdapter);
                                 ProgressDialogUtils.closeProgressDialog();
 
                             }
 
                             // Listview Group collasped listener
                             expListView.setOnGroupCollapseListener(new OnGroupCollapseListener() {
                                 @Override
                                 public void onGroupCollapse(int groupPosition) {
 
                                 }
                             });
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
 
 
 }
