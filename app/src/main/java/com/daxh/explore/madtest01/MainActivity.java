package com.daxh.explore.madtest01;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.daxh.explore.madtest01.tests.BasicRxUsages;
import com.daxh.explore.madtest01.tests.LambdasUsages;
import com.daxh.explore.madtest01.tests.OptionalUsages;
import com.daxh.explore.madtest01.tests.StreamsUsages;
import com.daxh.explore.madtest01.utils.LoggerUtils;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LoggerUtils.brief();

        OptionalUsages.start(false);
        StreamsUsages.start(false);
        LambdasUsages.start(false);
        BasicRxUsages.start(true);
    }
}
