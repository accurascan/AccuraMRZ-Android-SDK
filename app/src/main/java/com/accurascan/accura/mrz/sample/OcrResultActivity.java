package com.accurascan.accura.mrz.sample;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.accurascan.ocr.mrz.model.RecogResult;
import com.bumptech.glide.Glide;

public class OcrResultActivity extends AppCompatActivity {

    Bitmap face1;
    TableLayout mrz_table_layout;

    ImageView ivUserProfile, iv_frontside, iv_backside;
    LinearLayout ly_back, ly_front;
    View ly_mrz_container;
    View loutImg, loutFaceImageContainer;

    protected void onCreate(Bundle savedInstanceState) {
        if (getIntent().getIntExtra("app_orientation", 1) != 0) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr_result);

        initUI();
        RecogResult g_recogResult = RecogResult.getRecogResult();
        if (g_recogResult != null) {
            setMRZData(g_recogResult);

            if (g_recogResult.docFrontBitmap != null) {
                iv_frontside.setImageBitmap(g_recogResult.docFrontBitmap);
            } else {
                ly_front.setVisibility(View.GONE);
            }

            if (g_recogResult.docBackBitmap != null) {
                iv_backside.setImageBitmap(g_recogResult.docBackBitmap);
            } else {
                ly_back.setVisibility(View.GONE);
            }


            if (g_recogResult.faceBitmap != null) {
                face1 = g_recogResult.faceBitmap;
            }
        }
        setData();

    }

    private void initUI() {
        //initialize the UI
        ivUserProfile = findViewById(R.id.ivUserProfile);
        loutFaceImageContainer = findViewById(R.id.lyt_face_image_container);
        loutImg = findViewById(R.id.lyt_img_cover);

        ly_back = findViewById(R.id.ly_back);
        ly_front = findViewById(R.id.ly_front);
        iv_frontside = findViewById(R.id.iv_frontside);
        iv_backside = findViewById(R.id.iv_backside);

        mrz_table_layout = findViewById(R.id.mrz_table_layout);

        ly_mrz_container = findViewById(R.id.ly_mrz_container);
        ly_mrz_container.setVisibility(View.GONE);
    }

    private void setMRZData(RecogResult recogResult) {

        ly_mrz_container.setVisibility(View.VISIBLE);
        try {
            addLayout("MRZ", recogResult.lines);
            addLayout("Document Type", recogResult.docType);
            addLayout("First Name", recogResult.givenname);
            addLayout("Last Name", recogResult.surname);
            addLayout("Document No.", recogResult.docnumber);
            addLayout("Document check No.", recogResult.docchecksum);
            addLayout("Correct Document check No.", recogResult.correctdocchecksum);
            addLayout("Country", recogResult.country);
            addLayout("Nationality", recogResult.nationality);
            String s = (recogResult.sex.equals("M")) ? "Male" : ((recogResult.sex.equals("F")) ? "Female" : recogResult.sex);
            addLayout("Sex", s);
            addLayout("Date of Birth", recogResult.birth);
            addLayout("Birth Check No.", recogResult.birthchecksum);
            addLayout("Correct Birth Check No.", recogResult.correctbirthchecksum);
            addLayout("Date of Expiry", recogResult.expirationdate);
            addLayout("Expiration Check No.", recogResult.expirationchecksum);
            addLayout("Correct Expiration Check No.", recogResult.correctexpirationchecksum);
            addLayout("Date Of Issue", recogResult.issuedate);
            addLayout("Department No.", recogResult.departmentnumber);
            addLayout("Other ID", recogResult.otherid);
            addLayout("Other ID Check", recogResult.otheridchecksum);
            addLayout("Second Row Check No.", recogResult.secondrowchecksum);
            addLayout("Correct Second Row Check No.", recogResult.correctsecondrowchecksum);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addLayout(String key, String s) {
        if (TextUtils.isEmpty(s)) return;
        View layout1 = LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row, null);
        TextView tv_key1 = layout1.findViewById(R.id.tv_key);
        TextView tv_value1 = layout1.findViewById(R.id.tv_value);
        tv_key1.setText(key);
        tv_value1.setText(s);
        mrz_table_layout.addView(layout1);
    }

    private void setData() {
        if (face1 != null) {
            Glide.with(this).load(face1).centerCrop().into(ivUserProfile);
            ivUserProfile.setVisibility(View.VISIBLE);
        } else {
            loutFaceImageContainer.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Runtime.getRuntime().gc();
    }

    @Override
    public void onBackPressed() {
        try {
            RecogResult.getRecogResult().docFrontBitmap.recycle();
            RecogResult.getRecogResult().faceBitmap.recycle();
            RecogResult.getRecogResult().docBackBitmap.recycle();
        } catch (Exception e) {
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        setResult(RESULT_OK);
        finish();
    }
}
