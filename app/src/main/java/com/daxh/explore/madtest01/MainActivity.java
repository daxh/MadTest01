package com.daxh.explore.madtest01;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.daxh.explore.madtest01.Utils.LoggerUtils;
import com.orhanobut.logger.Logger;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LoggerUtils.brief();
        Logger.d("Pam Pam Point 1");

        LoggerUtils.explicit();
        Logger.d("Pam Pam Point 2");

        LoggerUtils.brief();
        Logger.d("Pam Pam Point 3");
    }
}
