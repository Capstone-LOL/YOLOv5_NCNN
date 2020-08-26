package com.wzt.yolov5;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.wzt.yolov5.ocr.OcrActivity;

public class WelcomeActivity extends AppCompatActivity {

    private Button yolov5s;
    private Button yolov4tiny;
    private Button mobilenetyolov3nano;
    private Button simplepose;
    private Button yolact;
    private Button chineseocrlite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        yolov5s = findViewById(R.id.btn_start_detect1);
        yolov5s.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.USE_MODEL = MainActivity.YOLOV5S;
                Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                WelcomeActivity.this.startActivity(intent);
            }
        });

        yolov4tiny = findViewById(R.id.btn_start_detect2);
        yolov4tiny.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.USE_MODEL = MainActivity.YOLOV4_TINY;
                Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                WelcomeActivity.this.startActivity(intent);
            }
        });

        mobilenetyolov3nano = findViewById(R.id.btn_start_detect3);
        mobilenetyolov3nano.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.USE_MODEL = MainActivity.MOBILENETV2_YOLOV3_NANO;
                Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                WelcomeActivity.this.startActivity(intent);
            }
        });

        simplepose = findViewById(R.id.btn_start_detect4);
        simplepose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.USE_MODEL = MainActivity.SIMPLE_POSE;
                Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                WelcomeActivity.this.startActivity(intent);
            }
        });

        yolact = findViewById(R.id.btn_start_detect5);
        yolact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.USE_MODEL = MainActivity.YOLACT;
                Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                WelcomeActivity.this.startActivity(intent);
            }
        });

        chineseocrlite = findViewById(R.id.btn_start_detect6);
        chineseocrlite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WelcomeActivity.this, OcrActivity.class);
                WelcomeActivity.this.startActivity(intent);
            }
        });
    }


}
