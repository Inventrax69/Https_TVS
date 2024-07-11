package com.inventrax.jungheinrich.fragments;


import android.content.Context;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.honeywell.aidc.AidcManager;
import com.honeywell.aidc.BarcodeReader;
import com.inventrax.jungheinrich.R;
import com.inventrax.jungheinrich.activities.MainActivity;
import com.inventrax.jungheinrich.adapters.CycleCountLocationListAdapter;
import com.inventrax.jungheinrich.common.Common;
import com.inventrax.jungheinrich.common.constants.EndpointConstants;
import com.inventrax.jungheinrich.common.constants.ErrorMessages;
import com.inventrax.jungheinrich.interfaces.ApiInterface;
import com.inventrax.jungheinrich.pojos.CycleCountDTO;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * Created by karthik.m on 06/27/2018.
 */

public class CcHeaderFragment extends Fragment implements View.OnClickListener {

    private static final String classCode = "API_FRAG_004";
    private View rootView;
    SearchableSpinner spinnerSelectCycleCount;
    Button btnGo;
    RelativeLayout rlCCHeaderOne;
    TextView lblCycleCount, lblSuggestedLoc, tvLocationsScanned, tvTotalSKUQty, txt_cyclecount, txt_cc_name, txt_blockedlocations;
    CardView cvScanLocation;
    ImageView ivScanLocation;
    TextInputLayout txtInputLayoutLocation;
    EditText etBoxQty, etLocation;

    DialogUtils dialogUtils;
    List<CycleCountDTO> lstCycleCount = null;

    private WMSCoreMessage core;
    String scanner = null;
    String getScanner = null;
    private IntentFilter filter;
    private Gson gson;
    private String userId = null, scanType = null, accountId = null, warehouseId = "", tenantId = "";
    private ScanValidator scanValidator;

    //For Honey well barcode
    private static BarcodeReader barcodeReader;
    private AidcManager manager;

    private String CCname = "";
    private Common common = null;

    private ExceptionLoggerUtils exceptionLoggerUtils;
    private ErrorMessages errorMessages;
    String Rack = "", Column = "", Level = "", CycleCountSeqCode = "", Bin = "";
    RecyclerView cucleCountRecyclerview;
    CycleCountLocationListAdapter cycleCountLocationListAdapter;

    public CcHeaderFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_cc_header, container, false);
        barcodeReader = MainActivity.getBarcodeObject();
        loadFormControls();

        return rootView;
    }


    private void loadFormControls() {

        SharedPreferences sp = getActivity().getSharedPreferences("LoginActivity", Context.MODE_PRIVATE);
        userId = sp.getString("RefUserId", "");
        scanType = sp.getString("scanType", "");
        accountId = sp.getString("AccountId", "");

        exceptionLoggerUtils = new ExceptionLoggerUtils();

        rlCCHeaderOne = (RelativeLayout) rootView.findViewById(R.id.rlCCHeaderOne);


        spinnerSelectCycleCount = (SearchableSpinner) rootView.findViewById(R.id.spinnerSelectCycleCount);
        spinnerSelectCycleCount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CCname = spinnerSelectCycleCount.getSelectedItem().toString();
                getWarehouseId();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        btnGo = (Button) rootView.findViewById(R.id.btnGo);
        btnGo.setOnClickListener(this);

        lblCycleCount = (TextView) rootView.findViewById(R.id.lblCycleCount);
        lblSuggestedLoc = (TextView) rootView.findViewById(R.id.lblSuggestedLoc);
        txt_cyclecount = (TextView) rootView.findViewById(R.id.txt_cyclecount);
        txt_cc_name = (TextView) rootView.findViewById(R.id.txt_cc_name);
        txt_blockedlocations = (TextView) rootView.findViewById(R.id.txt_blockedlocations);


        cvScanLocation = (CardView) rootView.findViewById(R.id.cvScanLocation);
        ivScanLocation = (ImageView) rootView.findViewById(R.id.ivScanLocation);


        txtInputLayoutLocation = (TextInputLayout) rootView.findViewById(R.id.txtInputLayoutLocation);


        etLocation = (EditText) rootView.findViewById(R.id.etLocation);
        cucleCountRecyclerview = (RecyclerView) rootView.findViewById(R.id.cycleCountRecyclerView);
        cucleCountRecyclerview.setVisibility(View.GONE);
        errorMessages = new ErrorMessages();
        lstCycleCount = new ArrayList<CycleCountDTO>();

        common = new Common();
        gson = new GsonBuilder().create();
        ProgressDialogUtils.closeProgressDialog();
        common.setIsPopupActive(false);

        // To get Cycle count names
        getCCNames();

        if (getArguments()!=null){
            warehouseId = getArguments().getString("warehouseId");
            tenantId = getArguments().getString("tenantId");
            CycleCountSeqCode = getArguments().getString("CycleCountSeqCode");
            CCname = getArguments().getString("CCname");
            cucleCountRecyclerview.setVisibility(View.VISIBLE);
            rlCCHeaderOne.setVisibility(View.GONE);
            txt_cyclecount.setVisibility(View.VISIBLE);
            txt_cc_name.setVisibility(View.VISIBLE);
            txt_cc_name.setText(CCname);
            txt_blockedlocations.setVisibility(View.VISIBLE);
            GetCCBlockedLocations();

        }


    }


    public void getWarehouseId() {
        for (CycleCountDTO oCycleCount : lstCycleCount) {
            if (oCycleCount.getCCName().equals(CCname)) {
                warehouseId = oCycleCount.getWarehouseID();
                tenantId = oCycleCount.getTenantId();
                Rack = oCycleCount.getRack();
                Column = oCycleCount.getColumn();
                Level = oCycleCount.getLevel();
//                Bin = oCycleCount.getBin();
                CycleCountSeqCode = oCycleCount.getCycleCountSeqCode();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btnGo:

                if (CCname.equals("") || CCname.equals("null")) {
                    common.showUserDefinedAlertType(errorMessages.EMC_0062, getActivity(), getContext(), "Warning");
                    return;

                } else {
                    GetCCdetails();
                }

                break;


        }
    }

    // bundle the Details for Deatils Fragemnt
    public void GetCCdetails() {

        cucleCountRecyclerview.setVisibility(View.VISIBLE);
        rlCCHeaderOne.setVisibility(View.GONE);
        txt_cyclecount.setVisibility(View.VISIBLE);
        txt_cc_name.setVisibility(View.VISIBLE);
        txt_cc_name.setText(CCname);
        txt_blockedlocations.setVisibility(View.VISIBLE);
        GetCCBlockedLocations();


    }


    // Get Cycle count names list
    public void getCCNames() {
        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.CycleCount, getActivity());
            final CycleCountDTO cycleCountDTO = new CycleCountDTO();
            cycleCountDTO.setUserId(userId);
            cycleCountDTO.setAccountID(accountId);
            message.setEntityObject(cycleCountDTO);


            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {

                ProgressDialogUtils.showProgressDialog("Please Wait");
                call = apiService.GetCCNames(message);



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
                                    lstCycleCount.add(dto);
                                }

                                for (int i = 0; i < lstDto.size(); i++) {

                                    _lstCCNames.add(lstDto.get(i).getCCName());

                                }

                                ArrayAdapter arrayAdapter = new ArrayAdapter(getActivity(), R.layout.support_simple_spinner_dropdown_item, _lstCCNames);
                                spinnerSelectCycleCount.setAdapter(arrayAdapter);
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

    public void GetCCBlockedLocations() {


    WMSCoreMessage message = new WMSCoreMessage();
    message =common.SetAuthentication(EndpointConstants.CycleCount, getContext());

    CycleCountDTO cycleCountDTO = new CycleCountDTO();
        cycleCountDTO.setUserId(userId);
        cycleCountDTO.setCycleCountSeqCode(CycleCountSeqCode);
        message.setEntityObject(cycleCountDTO);


    Call<String> call = null;
    ApiInterface apiService = RestService.getClient().create(ApiInterface.class);

        try

    {

        ProgressDialogUtils.showProgressDialog("Please Wait");
        call = apiService.GetCCBlockedLocations(message);

    } catch(
    Exception ex)

    {
        try {
            ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "GeneratePutawaySuggestions_01", getActivity());
            logException();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ProgressDialogUtils.closeProgressDialog();
        common.showUserDefinedAlertType(errorMessages.EMC_0002, getActivity(), getContext(), "Error");
    }
        try

    {
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
                            final List<CycleCountDTO> listccLocations = new ArrayList<CycleCountDTO>();

                            //Header for Adapter
                            List<String> lstnumberHeader = new ArrayList<String>();
//
                            Log.v("ABCDE", new Gson().toJson(core));

                            CycleCountDTO cycleCountDTO1 = null;
                            //Iterating through the Inventory list
                            for (int i = 0; i < _lOutbound.size(); i++) {

                                // Each individual value of the map into DTO
                                cycleCountDTO1 = new CycleCountDTO(_lOutbound.get(i).entrySet());
                                //Adding Inventory Dto into List
                                listccLocations.add(cycleCountDTO1);
                            }
                            if (listccLocations.size() > 0) {
                                getMyList(listccLocations);
                            } else {
                                DialogUtils.showAlertDialog(getActivity(), "No Data Found");
                                ProgressDialogUtils.closeProgressDialog();
                            }


                        }

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
    } catch(
    Exception ex)

    {
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




    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(int pos);
    }

    public  void getMyList(final List<CycleCountDTO> ccLocationList){
        cycleCountLocationListAdapter=new CycleCountLocationListAdapter(getActivity(),getContext(),ccLocationList, new CcHeaderFragment.OnListFragmentInteractionListener() {
            @Override
            public void onListFragmentInteraction(int pos) {


//                                        ArrayList<String> arraylist = new ArrayList<String>();
//                                        Bundle bundle = new Bundle();
//                                        bundle.putSerializable("mylist", arraylist);


                String location = ccLocationList.get(pos).getLocation();

                Bundle bundle = new Bundle();
                bundle.putString("CCname",CCname);
                bundle.putString("Location",location);
                bundle.putString("warehouseId", warehouseId);
                bundle.putString("tenantId", tenantId);
                bundle.putString("CycleCountSeqCode", CycleCountSeqCode);

                CcDetailsFragment ccDetailsFragment = new CcDetailsFragment();
                ccDetailsFragment.setArguments(bundle);
                FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, ccDetailsFragment);

            }
        }
        );
        LinearLayoutManager linearLayoutManager=new LinearLayoutManager(getActivity());
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getActivity(),DividerItemDecoration.VERTICAL);
        cucleCountRecyclerview.setLayoutManager(linearLayoutManager);
        cucleCountRecyclerview.setAdapter(cycleCountLocationListAdapter);
        ProgressDialogUtils.closeProgressDialog();
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

    }

    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.title_activity_cycle_count));
    }


    @Override
    public void onDestroyView() {

        super.onDestroyView();

    }
}