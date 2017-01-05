package com.daxh.explore.madtest01.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.daxh.explore.madtest01.R;
import com.daxh.explore.madtest01.tests.StreamsUsages;
import com.daxh.explore.madtest01.utils.LoggerUtils;

import static com.daxh.explore.madtest01.utils.BindingUtils.bindButton;

public class StreamsUsagesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streams_usages);
        LoggerUtils.brief();

        bindButton(this, R.id.btFind1, StreamsUsages::findPersonsWithConfiguredStreetNames);
        bindButton(this, R.id.btFind2, StreamsUsages::findPersonsWithStreetNamesContaining3);
        bindButton(this, R.id.btFind3, StreamsUsages::findConfiguredStreetsOfPersonsOlderThan4);
        bindButton(this, R.id.btMakeEveryoneOlder, StreamsUsages::makeEveryoneOlder);
        bindButton(this, R.id.btMakeEveryoneOlderShowAges, StreamsUsages::makeEveryoneOlderConvertToAges);
        bindButton(this, R.id.btMakeEveryoneOlderShowIntStreamAges, StreamsUsages::makeEveryoneOlderConvertToIntStreamAges);
        bindButton(this, R.id.btAverageAge, StreamsUsages::findAverageAge);
    }
}
