package com.daxh.explore.madtest01.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.daxh.explore.madtest01.R;
import com.daxh.explore.madtest01.utils.LoggerUtils;

import static com.daxh.explore.madtest01.utils.BindingUtils.bindButton;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LoggerUtils.brief();

        bindButton(this, R.id.btOptionals, () -> new Intent(this, OptionalsUsagesActivity.class));
        bindButton(this, R.id.btLambdas, () -> new Intent(this, LambdasUsagesActivity.class));
        bindButton(this, R.id.btStreams, () -> new Intent(this, StreamsUsagesActivity.class));
        bindButton(this, R.id.btBasicRx, () -> new Intent(this, BasicRxUsagesActivity.class));
    }
}
