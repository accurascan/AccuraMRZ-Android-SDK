package com.accurascan.accura.mrz.sample;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.accurascan.accura.mrz.sample.download.AccuraDownloadListener;
import com.accurascan.accura.mrz.sample.download.DownloadUtils;
import com.accurascan.ocr.mrz.model.RecogResult;
import com.accurascan.ocr.mrz.util.AccuraLog;
import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.docrecog.scan.MRZDocumentType;
import com.docrecog.scan.RecogEngine;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private ProgressDialog progressBar;

    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                if (activity.progressBar != null && activity.progressBar.isShowing()) {
                    activity.progressBar.dismiss();
                }
                Log.e(TAG, "handleMessage: " + msg.what);
                if (msg.what == 1) {
                    if (activity.sdkModel.isMRZEnable) {
                        activity.btnIdMrz.setVisibility(View.VISIBLE);
                        activity.btnVisaMrz.setVisibility(View.VISIBLE);
                        activity.btnPassportMrz.setVisibility(View.VISIBLE);
                        activity.btnMrz.setVisibility(View.VISIBLE);
                        activity.btnCapturedMrz.setVisibility(View.VISIBLE);
                    }
                } else {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(activity);
                    builder1.setMessage(activity.responseMessage);
                    builder1.setCancelable(true);
                    builder1.setPositiveButton(
                            "OK",
                            (dialog, id) -> dialog.cancel());
                    AlertDialog alert11 = builder1.create();
                    alert11.show();
                }
            }
        }
    }
    RecogEngine engine;
    private static class NativeThread extends Thread {
        private final WeakReference<MainActivity> mActivity;
        RecogEngine recogEngine;

        public NativeThread(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
            recogEngine = activity.engine;
        }

        @Override
        public void run() {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                try {
                    RecogEngine recogEngine = new RecogEngine();
                    AccuraLog.enableLogs(false); // make sure to disable logs in release mode
                    AccuraLog.refreshLogfile(activity);
                    recogEngine.setDialog(false); // setDialog(false) To set your custom dialog for license validation
                    if (!TextUtils.isEmpty(activity.keyLicensePath)) {
                        activity.sdkModel = recogEngine.initEngine(activity, activity.keyLicensePath);
                    } else
                        activity.sdkModel = recogEngine.initEngine(activity);
                    AccuraLog.loge(TAG, "Initialized Engine : " + activity.sdkModel.i + " -> " + activity.sdkModel.message);
                    activity.responseMessage = activity.sdkModel.message;

                    if (activity.sdkModel.i >= 0) {

                        recogEngine.setBlurPercentage(activity, 62);
                        recogEngine.setFaceBlurPercentage(activity, 70);
                        recogEngine.setGlarePercentage(activity, 6, 98);
                        recogEngine.isCheckPhotoCopy(activity, false);
                        recogEngine.SetHologramDetection(activity, true);
                        recogEngine.setLowLightTolerance(activity, 39);
                        recogEngine.setMotionThreshold(activity, 18);

                        activity.handler.sendEmptyMessage(1);
                    } else
                        activity.handler.sendEmptyMessage(0);
                } catch (Exception e) {
                }
            }
            super.run();
        }
    }

    private Thread nativeThread = new NativeThread(this);
    private View btnMrz, btnPassportMrz, btnIdMrz, btnVisaMrz, btnCapturedMrz;
    private RecogEngine.SDKModel sdkModel;
    private String responseMessage;
    private Handler handler = new MyHandler(this);
    final private int PICK_IMAGE = 1; // request code of select image from gallery

    private SharedPreferences sharedPreferences;
    String keyLicensePath;
    // Progress Dialog
    private ProgressDialog pDialog;

    protected Dialog showProgressDialog() {
        pDialog = new ProgressDialog(this);
        pDialog.setMessage("Downloading file. Please wait...");
        pDialog.setIndeterminate(false);
        pDialog.setMax(100);
        pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pDialog.setCanceledOnTouchOutside(false);
        pDialog.setCancelable(false);
        pDialog.show();
        return pDialog;

    }
    public void downloadTextFile(Context context, final String link) {
        NetworkInfo activeNetworkInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (!(activeNetworkInfo != null && activeNetworkInfo.isConnected())) {
            onErrora("Please check your internet connection", null);
            return;
        }

        String oldVersion = sharedPreferences.getString(DownloadUtils.LICENSE_VERSION, "");
        String lastSavedFile = sharedPreferences.getString(DownloadUtils.LICENSE_NAME, "");

        File licenseDir = new File(context.getFilesDir().toString(), "accura");
        DownloadUtils.getInstance(new AccuraDownloadListener() {
            @Override
            public void onDownloadComplete(String licenseDetails) {
                String latestVersion = "";
                String keyfileName = "";
                try {
                    JSONObject jsonObject = new JSONObject(licenseDetails);
                    if (!jsonObject.has("Android")) {
                        onErrora("Please add license details for Android", null);
                        return;
                    }
                    JSONObject object = jsonObject.getJSONObject("Android");

                    if (object.has("ocr_license") && !TextUtils.isEmpty(object.getString("ocr_license"))) {
                        keyfileName = object.getString("ocr_license");
                    }
                    if (jsonObject.has("version") && !TextUtils.isEmpty(jsonObject.getString("version"))) {
                        latestVersion = jsonObject.getString("version");
                    }
                } catch (JSONException e) {
                    AccuraLog.loge(TAG, Log.getStackTraceString(e));
                }

                if (TextUtils.isEmpty(keyfileName)) {
                    onErrora("File details not valid", null);
                    return;
                }

                // check to download license by using version
                if (lastSavedFile.isEmpty() || oldVersion.isEmpty() || !latestVersion.equals(oldVersion)) {
                    // Download latest license
                    downloadKeyFile(keyfileName, latestVersion);
                } else {
                    // License file is up to date
                    File licenseFilePath = null;
                    if (new File(licenseDir, keyfileName).exists()) {
                        licenseFilePath = new File(licenseDir, keyfileName);
                    } else if (new File(licenseDir, lastSavedFile).exists()) {
                        licenseFilePath = new File(licenseDir, lastSavedFile);
                    }
                    if (licenseFilePath != null) {
                        keyLicensePath = licenseFilePath.getPath();
                        sharedPreferences.edit().putString(DownloadUtils.LICENSE_PATH, keyLicensePath).apply();
                    } else {
                        onErrora(DownloadUtils.ERROR_CODE_FILE_NOT_FOUND, null);
                    }
                }
            }

            @Override
            public void onError(String s, ANError error) {
                onErrora(s, error);
            }

            private void downloadKeyFile(String finalKeyfileName, String finalUpdatedVersion) {
                showProgressDialog();
                AccuraLog.loge(TAG, "Start Download key license : " );

                DownloadUtils.getInstance(new AccuraDownloadListener() {
                    @Override
                    public void onDownloadComplete(String fileName) {
                        sharedPreferences.edit().putString(DownloadUtils.LICENSE_VERSION, finalUpdatedVersion).apply();
                        sharedPreferences.edit().putString(DownloadUtils.LICENSE_NAME, fileName).apply();
                        keyLicensePath = licenseDir.getPath() + "/" + fileName;
                        sharedPreferences.edit().putString(DownloadUtils.LICENSE_PATH, keyLicensePath).apply();
                        if (pDialog != null && pDialog.isShowing()) {
                            pDialog.dismiss();
                        }
                        nativeThread.start();
                    }

                    @Override
                    public void onProgress(int progress) {
                        if (pDialog != null) {
                            pDialog.setProgress(progress);
                            AccuraLog.loge(TAG, "on Key Progress : " + progress);

                        }
                    }

                    @Override
                    public void onError(String s, ANError error) {
                        AccuraLog.loge("TAG", "Download key file: " +  Log.getStackTraceString(error));
                        error.printStackTrace();
                        sharedPreferences.edit().putString(DownloadUtils.LICENSE_VERSION, "").apply();
                        if (pDialog != null && pDialog.isShowing()) {
                            pDialog.dismiss();
                        }
                        onErrora("Key license error " + s, error);
                    }
                }).downloadLicenseFile(licenseDir.getPath(), link.substring(0, link.lastIndexOf("/")+1)+ finalKeyfileName, finalKeyfileName, lastSavedFile);
            }

        }).parseFile(licenseDir.getPath(), link, link.substring(link.lastIndexOf("/")));
    }

    public void onErrora(String s, ANError error) {
        Toast.makeText(MainActivity.this, s+"\n"+Log.getStackTraceString(error), Toast.LENGTH_SHORT).show();
        if (pDialog != null && pDialog.isShowing()) {
            pDialog.dismiss();
        }
        nativeThread.start();
    }

    private void download() {
        if (!isFinishing()) {
            progressBar.show();
        }
        downloadTextFile(this, "https://example.com/config.json");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        engine = new RecogEngine();

        btnCapturedMrz = findViewById(R.id.lout_captured_mrz);
        btnCapturedMrz.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, ""), PICK_IMAGE);
        });

        btnMrz = findViewById(R.id.lout_mrz);
        btnMrz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OcrActivity.class);
                MRZDocumentType.NONE.attachTo(intent);
                intent.putExtra("card_name", getResources().getString(R.string.other_mrz));
                intent.putExtra("app_orientation", getRequestedOrientation());
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

        btnPassportMrz = findViewById(R.id.lout_passport_mrz);
        btnIdMrz = findViewById(R.id.lout_id_mrz);
        btnVisaMrz = findViewById(R.id.lout_visa_mrz);
        btnPassportMrz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OcrActivity.class);
                MRZDocumentType.PASSPORT_MRZ.attachTo(intent);
                intent.putExtra("card_name", getResources().getString(R.string.passport_mrz));
                intent.putExtra("app_orientation", getRequestedOrientation());
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });
        btnIdMrz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OcrActivity.class);
                MRZDocumentType.ID_CARD_MRZ.attachTo(intent);
                intent.putExtra("card_name", getResources().getString(R.string.id_mrz));
                intent.putExtra("app_orientation", getRequestedOrientation());
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });
        btnVisaMrz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OcrActivity.class);
                MRZDocumentType.VISA_MRZ.attachTo(intent);
                intent.putExtra("card_name", getResources().getString(R.string.visa_mrz));
                intent.putExtra("app_orientation", getRequestedOrientation());
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isPermissionsGranted(this)) {
            requestCameraPermission();
        } else {
            doWork();
        }
    }

    public static boolean isPermissionsGranted(Context context) {
        String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        for (String permission : permissions) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
        }
        return true;
    }

    //requesting the camera permission
    public void requestCameraPermission() {
        int currentapiVersion = Build.VERSION.SDK_INT;
        if (currentapiVersion >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                } else {
                    requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
        }
        switch (requestCode) {
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    try {
                        doWork();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    Toast.makeText(this, "You declined to allow the app to access your camera", Toast.LENGTH_LONG).show();
                }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE) { //handle request code PICK_IMAGE used for selecting image from gallery

            if (data == null) // data contain result of selected image from gallery and other
                return;

            if (progressBar != null && !progressBar.isShowing()) {
                progressBar.setMessage("Processing...");
                progressBar.show();
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    final Uri[] selectedImageUri = {data.getData()};
                    // Get the path from the Uri
                    final String path = FileUtils.getPath(MainActivity.this, data.getData());//getPathFromURI(selectedImageUri[0]);
                    if (path != null) {
                        File f = new File(path);
                        selectedImageUri[0] = Uri.fromFile(f);
                        Bitmap selectedImageBitmap;
                        try {
                            selectedImageBitmap
                                    = MediaStore.Images.Media.getBitmap(
                                    MainActivity.this.getContentResolver(),
                                    selectedImageUri[0]);
                            RecogResult result = engine.detectFromCapturedImage(selectedImageBitmap, MRZDocumentType.NONE, "all", 1);

                            if (progressBar != null && progressBar.isShowing()) {
                                progressBar.dismiss();
                            }
                            RecogResult.setRecogResult(result);
                            Intent intent = new Intent(MainActivity.this, OcrResultActivity.class);
                            intent.putExtra("app_orientation", getRequestedOrientation());
                            startActivity(intent);
                        } catch (IOException e) {
                            if (progressBar != null && progressBar.isShowing()) {
                                progressBar.dismiss();
                            }
                            e.printStackTrace();
                        }
                    } else {
                        if (progressBar != null && progressBar.isShowing()) {
                            progressBar.dismiss();
                        }
                        Toast.makeText(MainActivity.this, "Image not found", Toast.LENGTH_SHORT).show();
                    }
                }
            }, 1000);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_land_port, menu);
        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        final int orientation = display.getOrientation();
        MenuItem item = menu.findItem(R.id.item_land_port);
        switch (orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                item.setTitle("Portrait");
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                item.setTitle("Portrait");
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.item_land_port) {

            if (item.getTitle().toString().toLowerCase().equals("landscape")) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                item.setTitle("Portrait");
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                item.setTitle("Landscape");
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void doWork() {
        progressBar = new ProgressDialog(this);
        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressBar.setMessage("Please wait...");
        progressBar.setCancelable(false);
        download();
//        if (!isFinishing()) {
//            progressBar.show();
//            nativeThread.start();
//        }
    }
}
