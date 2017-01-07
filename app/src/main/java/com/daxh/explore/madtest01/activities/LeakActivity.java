package com.daxh.explore.madtest01.activities;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.annimon.stream.Optional;
import com.daxh.explore.madtest01.R;
import com.daxh.explore.madtest01.tests.BasicRxUsages;
import com.daxh.explore.madtest01.tests.models.Person;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

public class LeakActivity extends RxAppCompatActivity {

    private Optional<Button> btStartAsyncTask = Optional.empty();
    private Optional<Button> btStartUnsafeRxTask = Optional.empty();
    private Optional<Button> btStartSafeRxTask = Optional.empty();

    private Optional<ProgressBar> pbLoading = Optional.empty();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_leak);

        btStartAsyncTask = Optional.ofNullable((Button)findViewById(R.id.btStartAsyncTask))
                .executeIfPresent(button -> button.setOnClickListener(view -> asyncRunLrt()));

        pbLoading = Optional.ofNullable((ProgressBar) findViewById(R.id.pbLoading));
    }

    private void asyncRunLrt(){
        AsyncTask<Void, Void, Person> task = new AsyncTask<Void, Void, Person>() {
            @Override
            protected void onPreExecute() {
                showProgress(true);
            }

            @Override
            protected Person doInBackground(Void... voids) {
                return someLrtExample();
            }

            @Override
            protected void onPostExecute(Person person) {
                showProgress(false);
            }
        };
        task.execute();
    }

    private Person someLrtExample() {
        BasicRxUsages.getNewPerson(1);
        BasicRxUsages.getNewPerson(1);
        return BasicRxUsages.getNewPerson(1);
    }

    private void showProgress(boolean show){
        pbLoading.ifPresent(pb -> pb.setVisibility(show ? View.VISIBLE : View.GONE));
    }
}
