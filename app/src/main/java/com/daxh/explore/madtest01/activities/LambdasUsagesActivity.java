package com.daxh.explore.madtest01.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.daxh.explore.madtest01.R;
import com.daxh.explore.madtest01.tests.LambdasUsages;
import com.daxh.explore.madtest01.utils.LoggerUtils;

import static com.daxh.explore.madtest01.utils.BindingUtils.bindButton;

public class LambdasUsagesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lambdas_usages);
        LoggerUtils.brief();

        bindButton(this, R.id.btAsVars, LambdasUsages::lambdasAsVariables);
        bindButton(this, R.id.btAndClasses, LambdasUsages::lambdasAndClasses);
        bindButton(this, R.id.btAsArgs, LambdasUsages::lambdasAsArgs);
    }
}
