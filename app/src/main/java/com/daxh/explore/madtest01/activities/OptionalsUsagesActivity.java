package com.daxh.explore.madtest01.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.daxh.explore.madtest01.R;
import com.daxh.explore.madtest01.tests.OptionalUsages;
import com.daxh.explore.madtest01.utils.LoggerUtils;

import static com.daxh.explore.madtest01.utils.BindingUtils.bindButton;

public class OptionalsUsagesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_optionals_usages);
        LoggerUtils.brief();

        bindButton(this, R.id.btCreateOptionals, OptionalUsages::createOptional);
        bindButton(this, R.id.btUseDefaultValue, OptionalUsages::useDefaultValue);
        bindButton(this, R.id.btNonNullChaining, OptionalUsages::nonNullChaining);
        bindButton(this, R.id.btPerformIfExists, OptionalUsages::performIfExist);
    }
}
