package com.daxh.explore.madtest01.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.daxh.explore.madtest01.R;
import com.daxh.explore.madtest01.tests.BasicRxUsages;
import com.daxh.explore.madtest01.utils.LoggerUtils;

import static com.daxh.explore.madtest01.utils.BindingUtils.bindButton;

public class BasicRxUsagesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_rx_usages);
        LoggerUtils.explicit();

        bindButton(this, R.id.btLrtWithResult, BasicRxUsages::rxAndLongRunningTaskWithResult);
        bindButton(this, R.id.btLrtVoid, BasicRxUsages::rxAndLongRunningTaskVoid);
        bindButton(this, R.id.btFewSerialLrts, BasicRxUsages::combiningFewTasksWhichDependsOnEachOtherInSerial);
        bindButton(this, R.id.btFewParallelLrts, BasicRxUsages::combiningFewTasksWhichDependsOnEachOtherInParallel);
        bindButton(this, R.id.btDownstreamExceptions, BasicRxUsages::forwardingExceptionsToSubscriber);
        bindButton(this, R.id.btDetailedErrorHandling, BasicRxUsages::detailedErrorHandling);
    }
}
