package edu.cmu.pocketsphinx.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;

import java.util.ArrayList;

public class alarms extends AppCompatActivity {
    ArrayList<Integer> arrayAlarm;
    ArrayAdapter<Integer> arrayAdaptor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarms);
        arrayAlarm = AlarmData.getTimes();


    }
}
