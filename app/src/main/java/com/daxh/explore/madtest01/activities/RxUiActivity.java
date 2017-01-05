package com.daxh.explore.madtest01.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.annimon.stream.Optional;
import com.annimon.stream.function.Function;
import com.daxh.explore.madtest01.R;
import com.daxh.explore.madtest01.tests.BasicRxUsages;
import com.daxh.explore.madtest01.tests.models.Person;
import com.daxh.explore.madtest01.utils.LoggerUtils;
import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.jakewharton.rxbinding.widget.TextViewAfterTextChangeEvent;
import com.jakewharton.rxbinding.widget.TextViewBeforeTextChangeEvent;
import com.jakewharton.rxbinding.widget.TextViewTextChangeEvent;
import com.orhanobut.logger.Logger;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.exceptions.Exceptions;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

// We use RxAppCompatActivity to automatically mange subscriptions
// and prevent memory / context leaks due to configuration changes
// events and other related android 'goodies'. RxLifecycle provides
// all necessary base classes to handle these stuff (RxActivity,
// RxAppCompatActivity, RxFragment, RxAppCompatFragment, dialogs and
// even ability to implements all that stuff manually if out of the
// box solution does not fit your needs). Of course it is possible
// to manage all this stuff manually (using CompositeSubscription)
// like in previous commit, like, for example here:
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
public class RxUiActivity extends RxAppCompatActivity {

    public static final int TOTAL_MASKED_PHONE_LENGTH = 16;
    public static final int GET_NEW_PRSN_MAX_RETRIES = 10;

    private Optional<Button> btStart = Optional.empty();
    private Optional<EditText> etPhoneNumber = Optional.empty();
    private Optional<TextView> tvLog = Optional.empty();
    private Optional<ScrollView> svLog = Optional.empty();
    private Optional<ProgressBar> pbLoading = Optional.empty();

    // We using subjects to simplify calls to UI
    // from other threads, when this is need to be
    // mostly from doOnSubscribe, as we can't easily
    // partially switch thread for this operator.
    // More could be found here
    // https://groups.google.com/forum/#!topic/rxjava/TCGBiT0gbyI
    private PublishSubject<String> sbjLog = PublishSubject.create();

    Function<CharSequence, Boolean> checkSymbolRedundancy = cs -> cs.length() > TOTAL_MASKED_PHONE_LENGTH;
    Function<CharSequence, Boolean> checkPhoneСompleteness = cs -> cs.length() == TOTAL_MASKED_PHONE_LENGTH;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rx_ui);
        LoggerUtils.explicit(7);


        sbjLog
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .compose(bindToLifecycle())  // bind subscription to RxLifecycle
                .subscribe(this::appendToLog);

        btStart = Optional.ofNullable((Button)findViewById(R.id.btStart))
                .executeIfPresent(view -> RxView.clicks(view).compose(bindToLifecycle()).subscribe(aVoid -> runLrtWithRx()));

        etPhoneNumber = Optional.ofNullable((EditText) findViewById(R.id.etPhoneNumber))
                .executeIfPresent(editText -> {
                    RxTextView.textChangeEvents(editText).compose(bindToLifecycle()).subscribe(event -> {
                        btStart.executeIfPresent(bt -> bt.setEnabled(checkPhoneСompleteness.apply(event.text())));
                    });

                    RxTextView.editorActionEvents(editText).compose(bindToLifecycle()).subscribe(event -> {
                        if (event.actionId() == EditorInfo.IME_ACTION_DONE) {
                            runLrtWithRx();
                        }
                    });

                    setupPhoneNumberInputMask(editText);
                });

        svLog = Optional.ofNullable((ScrollView) findViewById(R.id.svLog));
        tvLog = Optional.ofNullable((TextView) findViewById(R.id.tvLog));
            tvLog.ifPresent(tv -> RxView.globalLayouts(tv).compose(bindToLifecycle()).subscribe(aVoid -> svLog.get().fullScroll(View.FOCUS_DOWN)));
        pbLoading = Optional.ofNullable((ProgressBar) findViewById(R.id.pbLoading));
    }

    private void runLrtWithRx() {
        Observable
                // This operator accepts Callable.call function
                // that rethrows checked exceptions that's why
                // we don't need any additional try/catch blocks
                // or Exceptions.propagate

                // Right now we are on the Thread: Io, below calls
                // to 'doOnSubscribe' for new observable and other
                // will be made on Thread: Io too respectively.
                // There is no clean way to do something on UI
                // thread just using operators, so in this specific
                // case it is possible to use subjects
                .fromCallable(() -> {
                    // Thanks to '.subscribeOn(Schedulers.io())' line
                    // here we always have some of Thread: RxIoScheduler

                    Person person = null;

                    // Technically we don't need this try/catch/thrown
                    // statement, but I will leave it here to make logging
                    // more detailed
                    try {
                        sbjLog.onNext("STARTED: getNewPersonOrError");
                        person = BasicRxUsages.getNewPersonOrError(1);
                        sbjLog.onNext("COMPLETED: getNewPersonOrError");
                    } catch (Exception e) {
                        sbjLog.onNext("EXCEPTION1: getNewPersonOrError");
                        throw e;
                    }
                    return person;
                })
                .subscribeOn(Schedulers.io())
                // This line switches everything starting from 'retry'
                // ('retry' included) to Thread: Main, but keeps lrt
                // itself on Thread: RxIoScheduler
                .observeOn(AndroidSchedulers.mainThread())
                .retry((attempts, throwable) -> {
                    // By default retry happens on the same thread as original
                    // lrt (but queued thread, on Scheduler.trampoline). Also
                    // looks like we could switch to main, and we switch it, by
                    // placing '.observeOn(AndroidSchedulers.mainThread())' above.
                    appendToLog(String.format(Locale.US, "RETRY1: %d, %s",attempts, throwable.getLocalizedMessage()));
                    return attempts <= GET_NEW_PRSN_MAX_RETRIES;
                })
                .doOnSubscribe(() -> {
                    // This happening on Thread: Main as everything is
                    // started from Thread: Main
                    appendToLog("Entering chain");
                    showProgress(true);
                })
                // This whole trick (with doOnNext and one
                // more Observable.call inside it) is made to
                // separate a part of actions and related error
                // handling with retry from the main rx chain.
                // This let us to handle errors and restart(
                // resubscribe) only in this sub-chain without
                // even touching the main chain.
                .doOnNext(person -> Observable.fromCallable(() -> {
                            // Still Thread: Main
                            // Demonstration how different exceptions could
                            // be handled in Observable.fromCallable calls
                            // This operator accepts Callable.call function
                            // that rethrows checked exceptions (unchecked
                            // rethrown by rx out of the box) that's why we
                            // don't need any additional try/catch blocks or
                            // Exceptions.propagate
                            appendToLog("STARTED: Person obtained");

                            double random = Math.random();

                            // Unchecked exception
                            if (random < .25){
                                appendToLog("EXCEPTION2: Person obtained");
                                throw new RuntimeException("EXCEPTION 2: Person obtained");
                            }

                            // Checked exception
                            if (random < .9){
                                appendToLog("EXCEPTION3: Person obtained");
                                throw new IOException("EXCEPTION 1: Person obtained");
                            }

                            appendToLog("COMPLETED: Person obtained\nInfo: " + person);
                            return person;
                        })
                        // Demonstration how different exceptions could
                        // be handled in doOnNext and analogous places
                        .doOnNext(person1 -> {
                            appendToLog("STARTED: After person obtained");
                            if (Math.random() < .5){
                                try {
                                    appendToLog("EXCEPTION4: After person obtained");
                                    throw new IOException("EXCEPTION4: After person obtained");
                                } catch (IOException e) {
                                    // Here we using MyException to distinguish this
                                    // particular situation from analogous runtime
                                    // exceptions and Exceptions.propagate to forward
                                    // exception to error handling methods (like 'retry',
                                    // 'onErrorResumeNext', and so on) below.
                                    throw Exceptions.propagate(new MyException(e));
                                }
                            }
                            appendToLog("COMPLETED: After person obtained");
                        })
                        .retry((attempts, throwable) -> {
                            appendToLog(String.format(Locale.US, "RETRY2: %d, %s",attempts, throwable.getLocalizedMessage()));

                            if (attempts<= GET_NEW_PRSN_MAX_RETRIES) {
                                if (throwable instanceof IOException) {
                                    appendToLog("RETRY2: retry " + throwable.getLocalizedMessage());
                                    return true; // try again
                                }

                                if (throwable instanceof MyException) {
                                    appendToLog("RETRY2: skip " + throwable.getLocalizedMessage());
                                    return false; // go downstream on chain
                                }

                                if (throwable instanceof RuntimeException) {
                                    appendToLog("RETRY2: skip " + throwable.getLocalizedMessage());
                                    return false; // go downstream on chain
                                }
                            }

                            appendToLog("RETRY2: fallback" + throwable.getLocalizedMessage());
                            return false; // go downstream on chain
                        })
                        .onErrorResumeNext(throwable -> {
                            if (throwable instanceof MyException) {
                                appendToLog("DEFAULT1: skipping " + throwable.getLocalizedMessage());
                                return Observable.just(person); // error, but we could provide default value and complete
                            }

                            if (throwable instanceof RuntimeException) {
                                appendToLog("DEFAULT1: skipping " + throwable.getLocalizedMessage());
                                return Observable.just(person); // error, but we could provide default value and complete
                            }

                            appendToLog("DEFAULT1: falling back " + throwable.getLocalizedMessage());
                            return Observable.error(throwable); // error, no way to succeed
                        })
                        .compose(bindToLifecycle())
                        .subscribe(
                                result -> appendToLog("onNext1: " + result),
                                throwable -> appendToLog("onError1: " + throwable.getLocalizedMessage()),
                                () -> appendToLog("onCompleted1"))
                )
                // Switching to Thread: Io
                .observeOn(Schedulers.io())
                .flatMap(person -> Observable.zip(
                        // Right now we are on the Thread: Io, below calls
                        // to 'doOnSubscribe' for new observable and other
                        // will be made on Thread: Io too respectively.
                        // There is no clean way to do something on UI
                        // thread just using operators, so in this specific
                        // case it is possible to use subjects

                        Observable
                                .fromCallable(() -> BasicRxUsages.fetchPersonSettingsOrError(person))
                                .doOnSubscribe(() -> sbjLog.onNext("fetchPersonSettingsOrError"))
                                .subscribeOn(Schedulers.io())
                                .doOnNext(settings -> sbjLog.onNext("Settings: " + settings))
                                .onErrorReturn(throwable -> {
                                    sbjLog.onNext("DEFAULT: fetchPersonSettingsOrError");
                                    return new Person.Settings(person); // error, but we could provide default value and complete
                                }),
                        Observable
                                .fromCallable(() -> BasicRxUsages.fetchPersonMessagesOrError(person))
                                .doOnSubscribe(() -> sbjLog.onNext("fetchPersonMessagesOrError"))
                                .subscribeOn(Schedulers.io())
                                .doOnNext(messages -> sbjLog.onNext("Messages: " + messages))
                                .onErrorReturn(throwable -> {
                                    sbjLog.onNext("DEFAULT: fetchPersonMessagesOrError");
                                    return new ArrayList<>(); // error, but we could provide default value and complete
                                }),
                        Pair::create))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(settingsMsgsListPair -> appendToLog("User info obtained..."))
                .flatMap(settingsMsgsListPair -> {
                    appendToLog("Checking messages...");
                    return Observable.from(settingsMsgsListPair.second);
                })
                .compose(bindToLifecycle())
                .subscribe(
                        // onNext
                        message -> appendToLog("onNext2: " + message.toString()),
                        // onError
                        throwable -> {
                            // All exceptions could be processed
                            // in one place - here, if this is a
                            // desirable behavior
                            appendToLog("onError2 " + throwable.getMessage());
                            showProgress(false);
                        },
                        // onCompleted
                        () -> {
                            appendToLog("onCompleted2");
                            showProgress(false);
                        }
                );
    }

    private void appendToLog(String s) {
        tvLog.ifPresent(tv -> tv.setText(tv.getText().toString() + "\n" + s + "\n"));
        Logger.d(s);
    }

    private void showProgress(boolean show) {
        if (show) {
            tvLog.ifPresent(tv -> tv.setText(""));
            pbLoading.ifPresent(progressBar -> progressBar.setVisibility(View.VISIBLE));

            btStart.ifPresent(bt -> bt.setEnabled(false));
            etPhoneNumber.ifPresent(et -> et.setEnabled(false));
        } else {
            pbLoading.ifPresent(progressBar -> progressBar.setVisibility(View.GONE));

            btStart.ifPresent(bt -> bt.setEnabled(true));
            etPhoneNumber.ifPresent(et -> et.setEnabled(true));
        }
    }

    private void setupPhoneNumberInputMask(EditText editText) {
        // 'Share' - useful operator that allows to
        // use few subsribers with "one" (technically
        // multiple) observable.
        Observable<TextViewBeforeTextChangeEvent> before = RxTextView.beforeTextChangeEvents(editText).share();
        Observable<TextViewTextChangeEvent> on = RxTextView.textChangeEvents(editText).share();
        Observable<TextViewAfterTextChangeEvent> after = RxTextView.afterTextChangeEvents(editText).share();

        // Just for logging and this is really
        // convenient, because we could keep it
        // separately avoiding noise in code
        before.compose(bindToLifecycle()).subscribe(e -> {
            Logger.d("before text = %s\tstart = %d\tcount = %d\tafter = %d", e.text(), e.start(), e.count(), e.after());
        });
        on.compose(bindToLifecycle()).subscribe(e -> {
            Logger.d("on text = %s\tstart = %d\tbefore = %d\tcount = %d", e.text(), e.start(), e.before(), e.count());
        });
        after.compose(bindToLifecycle()).subscribe(e -> {
            Logger.d("after text = %s", e.editable().toString());
        });

        // Magic starts here
        Observable.zip(
                before.flatMap(e -> Observable.just(
                        // It is vitally important to use String.valueOf
                        // here because this allows us to keep text in a
                        // instance separated from any changes to source
                        // But this note in general mostly relates to
                        // EditText features, not to Rx in general
                        String.valueOf(e.text())
                )),
                on.flatMap(e -> Observable.just(new Pair<>(
                        e.start(),
                        // The same situation here too
                        String.valueOf(e.text())
                ))),
                Pair::create)
                .flatMap(stringPairPair -> {
                    String oldText = stringPairPair.first;
                    int    curIdx  = stringPairPair.second.first;
                    String curText = stringPairPair.second.second;

                    if (curText.length() < oldText.length()) {
                        // Backspace pressed
                        return Observable.just(new Pair<>(-1, ""));
                    }

                    if (checkSymbolRedundancy.apply(curText)) {
                        // All numbers already entered, delete extra
                        return Observable.just(new Pair<>(-2, ""));
                    }

                    if (!TextUtils.isEmpty(oldText)) {
                        // Break loop
                        String oldChar = curIdx <= oldText.length()-1 ?
                                String.valueOf(oldText.charAt(curIdx)) : "";
                        String curChar = curIdx <= curText.length()-1 ?
                                String.valueOf(curText.charAt(curIdx)) : "";
                        if (curChar.contentEquals(oldChar)){
                            return Observable.just(new Pair<>(-1, ""));
                        }
                    }

                    if (curIdx == 0 && (TextUtils.isEmpty(curText) || !curText.subSequence(curIdx, curIdx+1).equals("+"))){
                        return Observable.just(new Pair<>(0, "+"));
                    }

                    if (curIdx == 2 && !curText.subSequence(curIdx, curIdx+1).equals("(")){
                        return Observable.just(new Pair<>(2, "("));
                    }

                    if (curIdx == 6 && !curText.subSequence(curIdx, curIdx+1).equals(")")){
                        return Observable.just(new Pair<>(6, ")"));
                    }

                    if (curIdx == 10 && !curText.subSequence(curIdx, curIdx+1).equals("-")){
                        return Observable.just(new Pair<>(10, "-"));
                    }

                    if (curIdx == 13 && !curText.subSequence(curIdx, curIdx+1).equals("-")){
                        return Observable.just(new Pair<>(13, "-"));
                    }

                    return Observable.just(new Pair<>(-1, ""));
                })
                .zipWith(after, Pair::create)
                .compose(bindToLifecycle())
                .subscribe(pair -> {
                    Logger.d("Point #6");
                    int idx = pair.first.first;
                    if (idx > -1) {
                        CharSequence c = pair.first.second;
                        Logger.d("Point #7 " + idx + " " + c);
                        Editable text = pair.second.editable();
                        text.insert(idx,c);
                    } else if(idx == -2) {
                        Editable text = pair.second.editable();
                        text.delete(text.length()-1,text.length());
                    }
                }, Throwable::printStackTrace);
    }

    class MyException extends RuntimeException {
        public MyException(Throwable throwable) {
            super(throwable);
        }

        @Override
        public String toString() {
            return "MyException." + super.toString();
        }
    }
}
