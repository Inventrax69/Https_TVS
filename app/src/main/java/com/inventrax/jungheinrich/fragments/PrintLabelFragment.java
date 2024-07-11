package com.inventrax.jungheinrich.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.inventrax.jungheinrich.R;
import com.inventrax.jungheinrich.common.Common;
import com.inventrax.jungheinrich.common.constants.EndpointConstants;
import com.inventrax.jungheinrich.common.constants.ErrorMessages;
import com.inventrax.jungheinrich.interfaces.ApiInterface;
import com.inventrax.jungheinrich.pojos.HouseKeepingDTO;
import com.inventrax.jungheinrich.pojos.WMSCoreMessage;
import com.inventrax.jungheinrich.pojos.WMSExceptionMessage;
import com.inventrax.jungheinrich.searchableSpinner.SearchableSpinner;
import com.inventrax.jungheinrich.services.RetrofitBuilderHttpsEx;
import com.inventrax.jungheinrich.util.DialogUtils;
import com.inventrax.jungheinrich.util.ExceptionLoggerUtils;
import com.inventrax.jungheinrich.util.FragmentUtils;
import com.inventrax.jungheinrich.util.ProgressDialogUtils;
import com.inventrax.jungheinrich.util.SoundUtils;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class PrintLabelFragment extends Fragment implements View.OnClickListener {
    View rootView;

    private static final String classCode = "API_FRAG_0011";

    private String Materialcode = null, Userid = null, scanType = "", accountId = "", storageloc = "";
    Common common;
    private Gson gson;
    private WMSCoreMessage core;
    private ExceptionLoggerUtils exceptionLoggerUtils;
    private ErrorMessages errorMessages;

    private SoundUtils soundUtils;
    private String selectedTenant = "", selectedWH = "", tenantId = "", whId = "";
    List<HouseKeepingDTO> lstTenants = null;
    List<HouseKeepingDTO> lstWarehouse = null;
    private SearchableSpinner spinnerSelectTenant, spinnerSelectWarehouse;
    Button btnGo;

    public PrintLabelFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_print_label, container, false);

        loadFromControllers();


        return rootView;

    }

    void loadFromControllers() {

        SharedPreferences sp = getActivity().getSharedPreferences("LoginActivity", Context.MODE_PRIVATE);
        Userid = sp.getString("RefUserId", "");
        scanType = sp.getString("scanType", "");
        accountId = sp.getString("AccountId", "");

        btnGo= (Button) rootView.findViewById(R.id.btnGo);

        btnGo.setOnClickListener(this);
        common = new Common();
        gson = new GsonBuilder().create();
        core = new WMSCoreMessage();
        ProgressDialogUtils.closeProgressDialog();
        common.setIsPopupActive(false);

        spinnerSelectTenant = (SearchableSpinner) rootView.findViewById(R.id.spinnerSelectTenant);
        spinnerSelectWarehouse = (SearchableSpinner) rootView.findViewById(R.id.spinnerSelectWarehouse);
        lstTenants = new ArrayList<HouseKeepingDTO>();
        lstWarehouse = new ArrayList<HouseKeepingDTO>();
        exceptionLoggerUtils = new ExceptionLoggerUtils();
        errorMessages = new ErrorMessages();
        soundUtils = new SoundUtils();
        spinnerSelectTenant.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedTenant = spinnerSelectTenant.getSelectedItem().toString();
                //   txtTendentName.setText(selectedTenant);
                getTenantId();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        spinnerSelectWarehouse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedWH = spinnerSelectWarehouse.getSelectedItem().toString();
                //  txtWarehousetName.setText(selectedWH);
                getWarehouseId();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });




        getWarehouse();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnGo:
                Bundle bundle = new Bundle();
                bundle.putString("tenantId", tenantId);
                bundle.putString("warehouseId", whId);
                PrintSKULabelFragment printSKULabelFragment = new PrintSKULabelFragment();
                printSKULabelFragment.setArguments(bundle);
                FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, printSKULabelFragment);
                break;

            default:
                break;
        }
    }





    @Override
    public void onDestroy() {
        super.onDestroy();
    }



    public void getTenants() {

        try {


            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.HouseKeepingDTO, getContext());
            HouseKeepingDTO houseKeepingDTO = new HouseKeepingDTO();
            houseKeepingDTO.setUserId(Userid);
            houseKeepingDTO.setTenantID(tenantId);
            houseKeepingDTO.setAccountID(accountId);
            houseKeepingDTO.setWarehouseId(whId);
            message.setEntityObject(houseKeepingDTO);


            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                call = apiService.GetTenants(message);
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

                                List<HouseKeepingDTO> lstDto = new ArrayList<HouseKeepingDTO>();
                                List<String> _lstStock = new ArrayList<>();

                                ProgressDialogUtils.closeProgressDialog();

                                for (int i = 0; i < _lstActiveStock.size(); i++) {
                                    HouseKeepingDTO dto = new HouseKeepingDTO(_lstActiveStock.get(i).entrySet());
                                    lstDto.add(dto);
                                    lstTenants.add(dto);
                                }

                                for (int i = 0; i < lstDto.size(); i++) {

                                    _lstStock.add(lstDto.get(i).getTenantName());

                                }


                                ArrayAdapter liveStockAdapter = new ArrayAdapter(getActivity(), R.layout.support_simple_spinner_dropdown_item, _lstStock);
                                spinnerSelectTenant.setAdapter(liveStockAdapter);
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

    public void getWarehouse() {

        try {


            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.HouseKeepingDTO, getContext());
            HouseKeepingDTO houseKeepingDTO = new HouseKeepingDTO();
            houseKeepingDTO.setAccountID(accountId);
            houseKeepingDTO.setUserId(Userid);
            houseKeepingDTO.setTenantID(tenantId);
            message.setEntityObject(houseKeepingDTO);


            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(getActivity()).create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                call = apiService.GetWarehouse(message);
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

                                List<HouseKeepingDTO> lstDto = new ArrayList<HouseKeepingDTO>();
                                List<String> _lstStock = new ArrayList<>();

                                ProgressDialogUtils.closeProgressDialog();

                                for (int i = 0; i < _lstActiveStock.size(); i++) {
                                    HouseKeepingDTO dto = new HouseKeepingDTO(_lstActiveStock.get(i).entrySet());
                                    lstDto.add(dto);
                                    lstWarehouse.add(dto);
                                }

                                for (int i = 0; i < lstDto.size(); i++) {

                                    _lstStock.add(lstDto.get(i).getWarehouse());

                                }


                                ArrayAdapter liveStockAdapter = new ArrayAdapter(getActivity(), R.layout.support_simple_spinner_dropdown_item, _lstStock);
                                spinnerSelectWarehouse.setAdapter(liveStockAdapter);


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

    public void getTenantId() {

        for (HouseKeepingDTO oHouseKeeping : lstTenants) {
            // iterating housekeeping list to get tenant id of selected tenant
            if (oHouseKeeping.getTenantName().equals(selectedTenant)) {

                tenantId = oHouseKeeping.getTenantID();   // Te

                // get warehouses of selected tenant
                //  getWarehouse();
            }
        }


    }

    public void getWarehouseId() {
        for (HouseKeepingDTO oHouseKeeping : lstWarehouse) {
            if (oHouseKeeping.getWarehouse().equals(selectedWH)) {
                whId = oHouseKeeping.getWarehouseId();    // Warehouse Id of selected warehouse
                getTenants();
            }
        }
    }





    @Override
    public void onResume() {
        super.onResume();

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.title_activity_printLabel));

    }


}