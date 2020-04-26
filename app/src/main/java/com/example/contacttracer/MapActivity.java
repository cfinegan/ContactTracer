package com.example.contacttracer;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class MapActivity extends AppCompatActivity {

    public static final String ARG_LATS = "lats";
    public final static String ARG_LNGS = "lngs";

    double[] lats;
    double[] lngs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        lats = getIntent().getDoubleArrayExtra(ARG_LATS);
        lngs = getIntent().getDoubleArrayExtra(ARG_LNGS);
    }
}
