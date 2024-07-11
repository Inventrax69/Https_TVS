package com.inventrax.tvs.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;

import com.inventrax.tvs.R;
import com.inventrax.tvs.application.AbstractApplication;
import com.inventrax.tvs.appupdate.GetUpdateJsonResponse;
import com.inventrax.tvs.appupdate.PermissionUtils;
import com.inventrax.tvs.appupdate.UpdateApp;
import com.inventrax.tvs.common.Common;
import com.inventrax.tvs.common.constants.EndpointConstants;
import com.inventrax.tvs.common.constants.ErrorMessages;
import com.inventrax.tvs.common.constants.ServiceURL;
import com.inventrax.tvs.interfaces.ApiInterface;
import com.inventrax.tvs.login.LoginPresenter;
import com.inventrax.tvs.login.LoginPresenterImpl;
import com.inventrax.tvs.login.LoginView;
import com.inventrax.tvs.pojos.LoginUserDTO;
import com.inventrax.tvs.pojos.ProfileDTO;
import com.inventrax.tvs.pojos.WMSCoreAuthentication;
import com.inventrax.tvs.pojos.WMSCoreMessage;
import com.inventrax.tvs.pojos.WMSExceptionMessage;
import com.inventrax.tvs.searchableSpinner.SearchableSpinner;
import com.inventrax.tvs.services.RestService;
import com.inventrax.tvs.services.RetrofitBuilderHttpsEx;
import com.inventrax.tvs.util.AndroidUtils;
import com.inventrax.tvs.util.DateUtils;
import com.inventrax.tvs.util.DialogUtils;
import com.inventrax.tvs.util.ExceptionLoggerUtils;
import com.inventrax.tvs.util.NetworkUtils;
import com.inventrax.tvs.util.ProgressDialogUtils;
import com.inventrax.tvs.util.SharedPreferencesUtils;
import com.inventrax.tvs.util.SoundUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Created by Padmaja.B on 20/12/2018.
 */

public class LoginActivity extends AppCompatActivity implements LoginView, AdapterView.OnItemSelectedListener {

    private static final String classCode = "API_ACT_LOGIN";
    private EditText inputUserId, inputPassword;
    private TextInputLayout inputLayoutUserId, inputLayoutPassword;
    private Button btnLogin, btnClear;
    private CheckBox chkRememberPassword;
    private TextView txtVersion, txtReleaseDate;
    private SearchableSpinner spinnerSelectDivision;
    private ProgressDialogUtils progressDialogUtils;
    private LoginPresenter loginPresenter;
    private SharedPreferencesUtils sharedPreferencesUtils;
    private Gson gson;
    private WMSCoreMessage core;
    private Common common;
    private SoundUtils soundUtils;
    private ExceptionLoggerUtils exceptionLoggerUtils;
    private ErrorMessages errorMessages;
    RestService restService;
    private String scanType = null;
    private ArrayList<String> listDivision;
    public static final int MULTIPLE_PERMISSIONS = 10;

    // if the android mobile version is greater than 6.0 we are giving the following permissions
    String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.GET_ACCOUNTS, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CALL_PHONE, Manifest.permission.WRITE_CONTACTS, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET, Manifest.permission.WAKE_LOCK, Manifest.permission.VIBRATE, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_NETWORK_STATE, Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS, Manifest.permission.READ_PHONE_STATE};

    ImageView settings;
    String serviceUrlString = null;
    ServiceURL serviceURL;

    String versionName = "";
    int versionCode=0;
    private String APK_URL = "";
    private static final String JSON_VERSION_CODE = "versionCode";
    private static final String JSON_UPDATE_URL = "updateURL";
    private static final int PERMISSION_REQUEST_CODE = 769;
    AlertDialog.Builder builder;
    TextView txtVersionName;

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onResume() {
        super.onResume();
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = pInfo.versionName;
            versionCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        SharedPreferences sp = this.getSharedPreferences("SettingsActivity", Context.MODE_PRIVATE);
        serviceUrlString = sp.getString("url", "");

        if(!serviceUrlString.isEmpty()){
            new AsyncTask<String, String, String>() {
                @Override
                protected String doInBackground(String... strings) {

                    String json_string = new GetUpdateJsonResponse().getContents(serviceUrlString+"/update.json");

                    try{

                        final JSONObject json = new JSONObject(json_string);
                        int result = json.getInt(JSON_VERSION_CODE);
                        if(versionCode < result){
                            com.inventrax.tvs.activities.LoginActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    builder = new AlertDialog.Builder(com.inventrax.tvs.activities.LoginActivity.this);
                                    //Uncomment the below code to Set the message and title from the strings.xml file
                                    builder.setMessage("") .setTitle("App Update");

                                    //Setting message manually and performing action on button click
                                    builder.setMessage("Update Available! Please update to newer version")
                                            .setCancelable(false)
                                            .setPositiveButton("Install", new DialogInterface.OnClickListener() {
                                                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
                                                public void onClick(DialogInterface dialog, int id) {
                                                    try{
                                                        APK_URL = json.getString(JSON_UPDATE_URL);
                                                        checkForUpdate();
                                                    }catch (JSONException e){

                                                    }
                                                }
                                            });
                                    //Creating dialog box
                                    AlertDialog alert = builder.create();
                                    //Setting the title manually
                                    alert.setTitle("App Update");
                                    alert.show();
                                }
                            });

                        }else{
                            return "App is update to date";
                        }
                    }catch (Exception e){
                        return  "Error : " + e.toString();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(String s) {
                    super.onPostExecute(s);
                }
            }.execute();
        }


    }


    private void displayNeverAskAgainDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("We need to send Storage for performing necessary task. Please permit the permission through "
                + "Settings screen.\n\nSelect Permissions -> Enable permission");
        builder.setCancelable(false);
        builder.setPositiveButton("Permit Manually", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Intent intent = new Intent();
                intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        });
        // builder.setNegativeButton("Cancel", null);
        builder.show();

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                boolean writeSAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean readSAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                if (writeSAccepted && readSAccepted) {
                    if(!APK_URL.isEmpty()){
                        UpdateApp updateApp = new UpdateApp();
                        updateApp.setContext(com.inventrax.tvs.activities.LoginActivity.this);
                        updateApp.execute(APK_URL);
                    }
                }else{
                    finish();
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void checkForUpdate(){
        if (checkPermission()) {
            if(!APK_URL.isEmpty()){
                UpdateApp updateApp = new UpdateApp();
                updateApp.setContext(com.inventrax.tvs.activities.LoginActivity.this);
                updateApp.execute(APK_URL);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (PermissionUtils.neverAskAgainSelected(com.inventrax.tvs.activities.LoginActivity.this, WRITE_EXTERNAL_STORAGE) && PermissionUtils.neverAskAgainSelected(com.inventrax.tvs.activities.LoginActivity.this, READ_EXTERNAL_STORAGE)) {
                    displayNeverAskAgainDialog();
                } else {
                    requestPermission();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        requestforpermissions(permissions);
        loadFormControls();
        loginPresenter = new LoginPresenterImpl(this);
    }

    //Loading all the form controls
    private void loadFormControls() {

        try {

            try {
                PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
                versionName = pInfo.versionName;
                versionCode = pInfo.versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            sharedPreferencesUtils = new SharedPreferencesUtils("LoginActivity", getApplicationContext());
            inputUserId = (EditText) findViewById(R.id.etUsername);
            inputPassword = (EditText) findViewById(R.id.etPass);
            txtVersionName = (TextView) findViewById(R.id.txtVersionName);
            chkRememberPassword = (CheckBox) findViewById(R.id.cbRememberMe);
            btnLogin = (Button) findViewById(R.id.btnLogin);

            spinnerSelectDivision = (SearchableSpinner) findViewById(R.id.spinnerSelectDivision);
            spinnerSelectDivision.setOnItemSelectedListener(this);

            listDivision = new ArrayList<>();
            listDivision.add("Manual");
            listDivision.add("Auto");

            ArrayAdapter listDivisionAdapter = new ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, listDivision);
            spinnerSelectDivision.setAdapter(listDivisionAdapter);

            SharedPreferences sp = this.getSharedPreferences("SettingsActivity", Context.MODE_PRIVATE);
            serviceUrlString = sp.getString("url", "");

            common = new Common();
            errorMessages = new ErrorMessages();
            serviceURL = new ServiceURL();

            exceptionLoggerUtils = new ExceptionLoggerUtils();
            restService = new RestService();
            soundUtils = new SoundUtils();
            inputUserId.addTextChangedListener(new LoginViewTextWatcher(inputUserId));
            inputPassword.addTextChangedListener(new LoginViewTextWatcher(inputPassword));
            gson = new GsonBuilder().create();
            core = new WMSCoreMessage();

            ServiceURL.setServiceUrl(serviceUrlString);

            txtVersionName.setText("Version : " + versionName);
            inputPassword.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    final int DRAWABLE_LEFT = 0;
                    final int DRAWABLE_TOP = 1;
                    final int DRAWABLE_RIGHT = 2;
                    final int DRAWABLE_BOTTOM = 3;


                    if(event.getRawX() >= (inputPassword.getRight() - inputPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        switch ( event.getAction() ) {
                            case MotionEvent.ACTION_DOWN:

                                inputPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                                break;
                            case MotionEvent.ACTION_UP:
                                inputPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());

                                break;
                        }

                        return true;
                    }

                    return false;
                }
            });

            try {
                btnLogin.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        if (!inputUserId.getText().toString().isEmpty() && !inputPassword.getText().toString().isEmpty()) {
                            //materialDialogUtil.showErrorDialog(LoginActivity.this,"Failed Failed Failed Failed Failed Failed Failed Failed Failed Failed Failed Failed");
                            if (submitForm()) {
                                // Checking Internet Connection
                                if (serviceUrlString != null && !serviceUrlString.equals("")) {
                                    try{
                                        validateUserSession();
                                    }catch (Exception e){
                                        DialogUtils.showAlertDialog(com.inventrax.tvs.activities.LoginActivity.this, getString(R.string.configure_url_correctly));
                                    }

                                } else {
                                    DialogUtils.showAlertDialog(com.inventrax.tvs.activities.LoginActivity.this, getString(R.string.configure_url));
                                }

                                //If User Clicks on remember me username,Password is stored in Shared preferences
                                if (chkRememberPassword.isChecked()) {
                                    sharedPreferencesUtils.savePreference("userId", inputUserId.getText().toString().trim());
                                    sharedPreferencesUtils.savePreference("password", inputPassword.getText().toString().trim());
                                    sharedPreferencesUtils.savePreference("isRememberPasswordChecked", true);
                                }
                            } else {
                                //Toast.makeText(getApplicationContext(),"Enter credentials",Toast.LENGTH_LONG).show();
                            }
                        } else {
                            DialogUtils.showAlertDialog(com.inventrax.tvs.activities.LoginActivity.this, getString(R.string.enter_user_id_and_password));
                        }
                    }
                });
            } catch (Exception ex) {
                Log.d("", "");
            }
            settings = (ImageView) findViewById(R.id.ivSettings);
            try {
                settings.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Intent intent = new Intent(com.inventrax.tvs.activities.LoginActivity.this, SettingsActivity.class);
                        startActivity(intent);

                    }
                });
            } catch (Exception ex) {

            }
            progressDialogUtils = new ProgressDialogUtils(this);


            if (sharedPreferencesUtils.loadPreferenceAsBoolean("isRememberPasswordChecked", false)) {
                inputUserId.setText(sharedPreferencesUtils.loadPreference("userId", ""));
                inputPassword.setText(sharedPreferencesUtils.loadPreference("password", ""));
                chkRememberPassword.setChecked(true);
            } else {
                inputUserId.setText(sharedPreferencesUtils.loadPreference("userId", ""));
                inputPassword.setText(sharedPreferencesUtils.loadPreference("password", ""));
                sharedPreferencesUtils.loadPreferenceAsBoolean("isRememberPasswordChecked", true);
            }
            AbstractApplication.CONTEXT = getApplicationContext();
        } catch (Exception ex) {

            DialogUtils.showAlertDialog(this, getString(R.string.error_while_initializing_controls));
            return;

        }
    }

    @Override
    protected void onDestroy() {
        loginPresenter.onDestroy();
        super.onDestroy();
    }

    @Override
    public void showProgress() {
        progressDialogUtils.showProgressDialog("Please Wait ...");
    }

    @Override
    public void hideProgress() {
        progressDialogUtils.closeProgressDialog();
    }

    @Override
    public void setUsernameError() {

    }

    @Override
    public void setPasswordError() {

    }

    @Override
    public void showLoginError(String message) {
        DialogUtils.showAlertDialog(this, message);
        return;
    }

    @Override
    public void navigateToHome() {

        // sharedPreferencesUtils.savePreference("login_status", true);

        showProgress();
        hideProgress();

        this.startActivity(new Intent(this, MainActivity.class));
        //  this.finish();
    }

    /**
     * Validating form
     */
    private boolean submitForm() {

        String userId = inputUserId.getText().toString().trim();

        if (userId.isEmpty() || !isValidUserId(userId)) {
            inputLayoutUserId.setError(getString(R.string.userHint));
            inputLayoutUserId.setErrorEnabled(true);
            return false;
        }
        if (inputPassword.getText().toString().trim().isEmpty()) {
            inputLayoutPassword.setError(getString(R.string.passHint));
            inputLayoutPassword.setErrorEnabled(true);
            return false;
        }



        return true;
    }


    //Validating the User credentials and Calling the API method
    public void validateUserSession() {

        if (NetworkUtils.isInternetAvailable(this)) {
            
        } else {
            DialogUtils.showAlertDialog(this, getString(R.string.please_enable_internet));
            // soundUtils.alertSuccess(LoginActivity.this,getBaseContext());
            return;
        }
        WMSCoreMessage message = new WMSCoreMessage();
        WMSCoreAuthentication token = new WMSCoreAuthentication();
        token.setAuthKey(AndroidUtils.getDeviceSerialNumber(getContentResolver()).toString());
        token.setUserId("1");
        token.setAuthValue("");
        token.setLoginTimeStamp(DateUtils.getTimeStamp().toString());
        token.setAuthToken("");
        token.setRequestNumber(1);
        message.setType(EndpointConstants.LoginUserDTO);
        message.setAuthToken(token);
        LoginUserDTO loginUserDTO = new LoginUserDTO();
        loginUserDTO.setMailID(inputUserId.getText().toString());
        loginUserDTO.setPasswordEncrypted(inputPassword.getText().toString());
        loginUserDTO.setClientMAC(AndroidUtils.getMacAddress(this).toString());
        loginUserDTO.setSessionIdentifier(AndroidUtils.getIPAddress(true));

        message.setEntityObject(loginUserDTO);

        Call<String> call = null;
        ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(this).create(ApiInterface.class);

        try {

            call = apiService.UserLogin(message);
            ProgressDialogUtils.showProgressDialog("Please Wait");

        } catch (Exception ex) {

            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_01", com.inventrax.tvs.activities.LoginActivity.this);
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
        }
        try {
            //Getting response from the method
            call.enqueue(new Callback<String>() {

                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    //checking the Entity obeject is not null
                    if (response.body() != null) {

                        core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);

                        if (core.getEntityObject() != null) {

                            if ((core.getType().toString().equals("Exception"))) {
                                List<LinkedTreeMap<?, ?>> _lExceptions = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lExceptions = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                WMSExceptionMessage owmsExceptionMessage = null;
                                for (int i = 0; i < _lExceptions.size(); i++) {

                                    owmsExceptionMessage = new WMSExceptionMessage(_lExceptions.get(i).entrySet());
                                    ProgressDialogUtils.closeProgressDialog();
                                    common.showAlertType(owmsExceptionMessage, com.inventrax.tvs.activities.LoginActivity.this, getApplicationContext());
                                    return;

                                }
                            } else {

                                try {
                                    List<LinkedTreeMap<?, ?>> _lProfileDto = new ArrayList<LinkedTreeMap<?, ?>>();
                                    _lProfileDto = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                    ProfileDTO oProfileDto = null;
                                    for (int i = 0; i < _lProfileDto.size(); i++) {
                                        oProfileDto = new ProfileDTO(_lProfileDto.get(i).entrySet());
                                    }


                                    if (oProfileDto.getUserID() != null) {
                                        sharedPreferencesUtils.savePreference("RefUserId", oProfileDto.getUserID());
                                    }

                                    if (oProfileDto.getFirstName() != null) {
                                        sharedPreferencesUtils.savePreference("UserName", oProfileDto.getFirstName());
                                    }

                                    if (oProfileDto.getAccountId() != null) {
                                        sharedPreferencesUtils.savePreference("AccountId", oProfileDto.getAccountId().split("[.]")[0]);
                                    }

                                    if (oProfileDto.getWarehouseID() != null) {
                                        sharedPreferencesUtils.savePreference("WarehouseID", oProfileDto.getWarehouseID());
                                    }

                                    if (oProfileDto.getTenantID() != null) {
                                        sharedPreferencesUtils.savePreference("TenantID", oProfileDto.getTenantID());
                                    }

                                    if (oProfileDto.getUserRole() != null) {
                                        sharedPreferencesUtils.savePreference("UserRole", oProfileDto.getUserRole());
                                    }

                                    if (oProfileDto.getDepartmentIDs() != null) {
                                        sharedPreferencesUtils.savePreference("DepartmentIDs", oProfileDto.getDepartmentIDs());
                                    }


                                } catch (Exception ex) {
                                    try {
                                        exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001", com.inventrax.tvs.activities.LoginActivity.this);
                                        logException();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                }
                            }
                            Intent i = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(i);
                            ProgressDialogUtils.closeProgressDialog();
                        } else {
                            ProgressDialogUtils.closeProgressDialog();
                            DialogUtils.showAlertDialog(com.inventrax.tvs.activities.LoginActivity.this, errorMessages.EMC_0002);
                            //DialogUtils.showAlertDialog(LoginActivity.this,"Network Error");
                        }

                    } else {
                        ProgressDialogUtils.closeProgressDialog();
                        DialogUtils.showAlertDialog(com.inventrax.tvs.activities.LoginActivity.this, errorMessages.EMC_0021);
                    }
                }

                // response object fails
                @Override
                public void onFailure(Call<String> call, Throwable throwable) {
                    //Toast.makeText(LoginActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
                    ProgressDialogUtils.closeProgressDialog();
                    DialogUtils.showAlertDialog(com.inventrax.tvs.activities.LoginActivity.this, errorMessages.EMC_0001);
                    // soundUtils.alertConfirm(LoginActivity.this,getBaseContext());
                }
            });
        } catch (Exception ex) {

            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001", com.inventrax.tvs.activities.LoginActivity.this);
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            DialogUtils.showAlertDialog(com.inventrax.tvs.activities.LoginActivity.this, errorMessages.EMC_0003);
        }
    }

    // sending exception to the database
    public void logException() {
        try {

            String textFromFile = exceptionLoggerUtils.readFromFile(com.inventrax.tvs.activities.LoginActivity.this);

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.Exception, this);
            WMSExceptionMessage wmsExceptionMessage = new WMSExceptionMessage();
            wmsExceptionMessage.setWMSMessage(textFromFile);
            message.setEntityObject(wmsExceptionMessage);

            Call<String> call = null;
            ApiInterface apiService = RetrofitBuilderHttpsEx.getInstance(this).create(ApiInterface.class);

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
                Log.d("Message", ex.toString());
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
                                    common.showAlertType(owmsExceptionMessage, com.inventrax.tvs.activities.LoginActivity.this, getApplicationContext());
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
                                            exceptionLoggerUtils.deleteFile(com.inventrax.tvs.activities.LoginActivity.this);
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
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable throwable) {
                        //Toast.makeText(LoginActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
                        DialogUtils.showAlertDialog(com.inventrax.tvs.activities.LoginActivity.this, getString(R.string.not_valid_credentials));
                        return;
                    }
                });
            } catch (Exception ex) {
                // Toast.makeText(LoginActivity.this, ex.toString(), Toast.LENGTH_LONG).show();
            }
        } catch (Exception ex) {
            DialogUtils.showAlertDialog(com.inventrax.tvs.activities.LoginActivity.this, getString(R.string.please_check_the_values));
            return;
        }
    }


    private static boolean isValidUserId(String userId) {
        return !TextUtils.isEmpty(userId);
    }







    private class LoginViewTextWatcher implements TextWatcher {

        private View view;

        private LoginViewTextWatcher(View view) {
            this.view = view;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            switch (view.getId()) {
                case R.id.etUsername:

                    break;
                case R.id.etPass:

                break;
            }
        }
    }



    public void requestforpermissions(String[] permissions) {
        if (checkPermissions()) {

        }
        //  permissions  granted.
    }

    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(com.inventrax.tvs.activities.LoginActivity.this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }




    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        scanType = spinnerSelectDivision.getSelectedItem().toString();
        sharedPreferencesUtils.savePreference("scanType", scanType);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

}