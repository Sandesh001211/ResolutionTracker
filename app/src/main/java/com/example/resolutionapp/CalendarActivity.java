package com.example.resolutionapp;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class CalendarActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // We might change this to a Fragment later, but keeping for manifest
        // compatibility
        setContentView(R.layout.activity_main);
    }
}
