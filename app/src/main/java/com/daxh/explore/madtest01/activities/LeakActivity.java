package com.daxh.explore.madtest01.activities;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.annimon.stream.Optional;
import com.daxh.explore.madtest01.MadTest01App;
import com.daxh.explore.madtest01.R;
import com.daxh.explore.madtest01.tests.models.Person;
import com.daxh.explore.madtest01.utils.LoggerUtils;
import com.jakewharton.rxbinding.view.RxView;
import com.orhanobut.logger.Logger;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

// We use RxAppCompatActivity to automatically mange subscriptions
// and prevent memory / context leaks due to configuration changes
// events and other related android 'goodies'. RxLifecycle provides
// all necessary base classes to handle these stuff (RxActivity,
// RxAppCompatActivity, RxFragment, RxAppCompatFragment, dialogs and
// even ability to implements all that stuff manually if out of the
// box solution does not fit your needs). Of course it is possible
// to manage all this stuff manually (using CompositeSubscription)
// like for example here:
// http://blog.danlew.net/2014/10/08/grokking-rxjava-part-4/
// ... starting from  'The second problem can be solved by properly'.
// But according to this note
// https://groups.google.com/forum/#!topic/rxjava/77pCKg2iHEk
// in a complex case at some moment you will start to re-invent things
// already implemented in RxLifecycle. Moreover, there is no reason
// why you can't combine both approaches.
// IMPORTANT: RxLifecycle does not actually unsubscribe the sequence.
// Instead it terminates the sequence. So, if you really need behavior
// with un-subscribe, then implement it manually (using
// Composite Subscription). More info could be found here:
// https://github.com/trello/RxLifecycle
public class LeakActivity extends RxAppCompatActivity {

    private Optional<ProgressBar> pbLoading = Optional.empty();

    private Subscription sbspnLeakableClicks;
    private Subscription sbspnLeakableLrt;
    private Subscription sbspnUnleakableClicks;
    private Subscription sbspnUnleakableLrt;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_leak);
        LoggerUtils.brief();

        Optional.ofNullable((Button)findViewById(R.id.btStartAsyncTask))
                .ifPresent(button -> button.setOnClickListener(view -> asyncRunLrt()));

        Optional.ofNullable((Button) findViewById(R.id.btStartLeakableRxTask))
                .ifPresent(bt -> sbspnLeakableClicks = RxView.clicks(bt).subscribe(aVoid -> {
                    sbspnLeakableLrt = Observable.fromCallable(this::someLrtExample)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnSubscribe(() -> {
                                Logger.d("LEAKABLE STARTED");
                                showProgress(true);
                            })
                            .doOnCompleted(() -> showProgress(false))
                            .subscribe(
                                    p -> {
                                    },
                                    throwable -> {
                                        Logger.d("LEAKABLE EXCEPTION");
                                        Logger.d(throwable.getLocalizedMessage());
                                    },
                                    () -> Logger.d("LEAKABLE COMPLETED")
                            );
                }));

        Optional.ofNullable((Button) findViewById(R.id.btStartUnleakableRxTask))
                .ifPresent(bt -> sbspnUnleakableClicks = RxView.clicks(bt)
                        // This operator binds this subscription
                        // to RxLifecycle, that will cause a call
                        // to onComplete for this subscription in
                        // appropriate moment of activity's lifecycle
                        // (mostly on onDestroy). And this will be
                        // enough to solve this context/memory leaking
                        // problem related to Rx.
                        .compose(bindToLifecycle())
                        .subscribe(aVoid -> {
                    sbspnUnleakableLrt = Observable.fromCallable(this::someLrtExample)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnSubscribe(() -> {
                                Logger.d("UNLEAKABLE STARTED");
                                showProgress(true);
                            })
                            .doOnCompleted(() -> showProgress(false))
                            // This operator binds this subscription
                            // to RxLifecycle, that will cause a call
                            // to onComplete for this subscription in
                            // appropriate moment of activity's lifecycle
                            // (mostly on onDestroy). And this will be
                            // enough to solve this context/memory leaking
                            // problem related to Rx.
                            .compose(bindToLifecycle())
                            .subscribe(
                                    p -> {
                                    },
                                    throwable -> {
                                        Logger.d("UNLEAKABLE EXCEPTION");
                                        Logger.d(throwable.getLocalizedMessage());
                                    },
                                    () -> Logger.d("UNLEAKABLE COMPLETED")
                            );
                }));

        pbLoading = Optional.ofNullable((ProgressBar) findViewById(R.id.pbLoading));
    }

    // IMPORTANT: If you suspect that some variable may
    // leak or cause a leak, then you could use RefWatcher
    // like we do it below. __BUT__ you should call it right
    // before you think this field must be GC'ed. Below we
    // start 'refwatching' for subscriptions in onDestroy,
    // as we expect that right after onDestroy this fields
    // will be GC'ed.
    @Override
    protected void onDestroy() {
        super.onDestroy();

        Optional.ofNullable(sbspnLeakableClicks).ifPresent(s -> MadTest01App.instance().watch(s));
        Optional.ofNullable(sbspnLeakableLrt).ifPresent(s -> MadTest01App.instance().watch(s));
        Optional.ofNullable(sbspnUnleakableClicks).ifPresent(s -> MadTest01App.instance().watch(s));
        Optional.ofNullable(sbspnUnleakableLrt).ifPresent(s -> MadTest01App.instance().watch(s));
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
        Logger.d("IN");
        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Logger.d("OUT");
        return new Person();
    }

    private void showProgress(boolean show){
        pbLoading.ifPresent(pb -> pb.setVisibility(show ? View.VISIBLE : View.GONE));
    }
}
