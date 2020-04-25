package com.example.contacttracer;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class MapActivity extends AppCompatActivity {

    double[] lats;
    double[] lngs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        lats = getIntent().getDoubleArrayExtra("lats");
        lngs = getIntent().getDoubleArrayExtra("lngs");
    }
}
