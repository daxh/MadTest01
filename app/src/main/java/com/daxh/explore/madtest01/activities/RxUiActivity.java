package com.daxh.explore.madtest01.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.exceptions.Exceptions;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class RxUiActivity extends AppCompatActivity{

    public static final int TOTAL_MASKED_PHONE_LENGTH = 16;
    public static final int GET_NEW_PRSN_MAX_RETRIES = 10;

    Optional<Button> btStart = Optional.empty();
    Optional<EditText> etPhoneNumber = Optional.empty();
    Optional<TextView> tvLog = Optional.empty();
    Optional<ProgressBar> pbLoading = Optional.empty();

    // We using subjects to sinplify calls to UI
    // from other threads, when this is need to be
    // mostly from doOnSubscribe, as we can't easily
    // switch thread for this operator. More could be
    // found here
    // https://groups.google.com/forum/#!topic/rxjava/TCGBiT0gbyI
    PublishSubject<Boolean> sbjLoading = PublishSubject.create();
    PublishSubject<String> sbjLog = PublishSubject.create();

    Function<CharSequence, Boolean> checkSymbolRedundancy = cs -> cs.length() > TOTAL_MASKED_PHONE_LENGTH;
    Function<CharSequence, Boolean> checkPhoneСompleteness = cs -> cs.length() == TOTAL_MASKED_PHONE_LENGTH;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rx_ui);
        LoggerUtils.explicit();

        sbjLoading
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showProgress);
        sbjLog
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(this::appendToLog);

        btStart = Optional.ofNullable((Button)findViewById(R.id.btStart))
                .executeIfPresent(view -> RxView.clicks(view)
                .subscribe(aVoid -> runLrtWithRx()));

        etPhoneNumber = Optional.ofNullable((EditText) findViewById(R.id.etPhoneNumber))
                .executeIfPresent(editText -> {
                    RxTextView.textChangeEvents(editText).subscribe(event -> {
                        btStart.executeIfPresent(bt -> bt.setEnabled(checkPhoneСompleteness.apply(event.text())));
                    });

                    RxTextView.editorActionEvents(editText).subscribe(event -> {
                        if (event.actionId() == EditorInfo.IME_ACTION_DONE) {
                            runLrtWithRx();
                        }
                    });

                    setupPhoneNumberInputMask(editText);
                });

        tvLog = Optional.ofNullable((TextView) findViewById(R.id.tvLog));
        pbLoading = Optional.ofNullable((ProgressBar) findViewById(R.id.pbLoading));
    }

    private void runLrtWithRx() {
        Observable
                // This operator accepts Callable.call function
                // that rethrows checked exceptions that's why
                // we don't need any additional try/catch blocks
                // or Exceptions.propagate
                .fromCallable(() -> {
                    // Thanks to '.subscribeOn(Schedulers.io())' line
                    // here we always have some of Thread: RxIoScheduler

                    Person person = null;

                    // Technically we don't need this try/catch/thrown
                    // statement, but I will leave it here to make logging
                    // more detailed
                    try {
                        Logger.d("STARTED: getNewPersonOrError");
                        person = BasicRxUsages.getNewPersonOrError(1);
                        Logger.d("COMPLETED: getNewPersonOrError");
                    } catch (Exception e) {
                        Logger.d("EXCEPTION1: getNewPersonOrError");
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
                    Logger.d(String.format(Locale.US, "RETRY1: %d, %s",attempts, throwable.getLocalizedMessage()));
                    return attempts <= GET_NEW_PRSN_MAX_RETRIES;
                })
                .doOnSubscribe(() -> {
                    // This happening on Thread: Main as everything is
                    // started from Thread: Main
                    Logger.d("Entering chain");
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
                            Logger.d("STARTED: Person obtained");

                            double random = Math.random();

                            // Unchecked exception
                            if (random < .25){
                                Logger.d("EXCEPTION2: Person obtained");
                                throw new RuntimeException("EXCEPTION 2: Person obtained");
                            }

                            // Checked exception
                            if (random < .5){
                                Logger.d("EXCEPTION3: Person obtained");
                                throw new IOException("EXCEPTION 1: Person obtained");
                            }

                            Logger.d("COMPLETED: Person obtained\nInfo: " + person);
                            return person;
                        })
                        // Demonstration how different exceptions could
                        // be handled in doOnNext and analogous places
                        .doOnNext(person1 -> {
                            Logger.d("STARTED: After person obtained");
                            if (Math.random() < .9){
                                try {
                                    Logger.d("EXCEPTION4: After person obtained");
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
                            Logger.d("COMPLETED: After person obtained");
                        })
                        .retry((attempts, throwable) -> {
                            Logger.d(String.format(Locale.US, "RETRY2: %d, %s",attempts, throwable.getLocalizedMessage()));

                            if (attempts<= GET_NEW_PRSN_MAX_RETRIES) {
                                if (throwable instanceof IOException) {
                                    Logger.d("RETRY2: retry " + throwable.getLocalizedMessage());
                                    return true; // try again
                                }

                                if (throwable instanceof MyException) {
                                    Logger.d("RETRY2: skip " + throwable.getLocalizedMessage());
                                    return false; // go downstream on chain
                                }

                                if (throwable instanceof RuntimeException) {
                                    Logger.d("RETRY2: skip " + throwable.getLocalizedMessage());
                                    return false; // go downstream on chain
                                }
                            }

                            Logger.d("RETRY2: fallback" + throwable.getLocalizedMessage());
                            return false; // go downstream on chain
                        })
                        .onErrorResumeNext(throwable -> {
                            if (throwable instanceof MyException) {
                                Logger.d("DEFAULT1: skipping " + throwable.getLocalizedMessage());
                                return Observable.just(person); // error, but we could provide default value and complete
                            }

                            if (throwable instanceof RuntimeException) {
                                Logger.d("DEFAULT1: skipping " + throwable.getLocalizedMessage());
                                return Observable.just(person); // error, but we could provide default value and complete
                            }

                            Logger.d("DEFAULT1: falling back " + throwable.getLocalizedMessage());
                            return Observable.error(throwable); // error, no way to succeed
                        })
                        .subscribe(
                                result -> Logger.d("onNext1: " + result),
                                throwable -> Logger.d("onError1: " + throwable.getLocalizedMessage()),
                                () -> Logger.d("onCompleted1"))
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
                                .doOnSubscribe(() -> Logger.d("fetchPersonSettingsOrError"))
                                .subscribeOn(Schedulers.io())
                                .doOnNext(settings -> Logger.d("Settings: " + settings))
                                .onErrorReturn(throwable -> {
                                    Logger.d("DEFAULT: fetchPersonSettingsOrError");
                                    return new Person.Settings(person); // error, but we could provide default value and complete
                                }),
                        Observable
                                .fromCallable(() -> BasicRxUsages.fetchPersonMessagesOrError(person))
                                .doOnSubscribe(() -> Logger.d("fetchPersonMessagesOrError"))
                                .subscribeOn(Schedulers.io())
                                .doOnNext(messages -> Logger.d("Messages: " + messages))
                                .onErrorReturn(throwable -> {
                                    Logger.d("DEFAULT: fetchPersonMessagesOrError");
                                    return new ArrayList<>(); // error, but we could provide default value and complete
                                }),
                        Pair::create))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(settingsMsgsListPair -> Logger.d("User info obtained..."))
                .flatMap(settingsMsgsListPair -> {
                    Logger.d("Checking messages...");
                    return Observable.from(settingsMsgsListPair.second);
                })
                .subscribe(
                        // onNext
                        message -> Logger.d("onNext2: " + message.toString()),
                        // onError
                        throwable -> {
                            // All exceptions could be processed
                            // in one place - here, if this is a
                            // desirable behavior
                            Logger.d("onError2 " + throwable.getMessage());
                            showProgress(false);
                        },
                        // onCompleted
                        () -> {
                            Logger.d("onCompleted2");
                            showProgress(false);
                        }
                );
    }

    private void appendToLog(String s) {
        tvLog.ifPresent(tv -> tv.setText(tv.getText().toString() + "\n" + s));
        Logger.e(s);
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
        before.subscribe(e -> {
            Logger.d("before text = %s\tstart = %d\tcount = %d\tafter = %d", e.text(), e.start(), e.count(), e.after());
        });
        on.subscribe(e -> {
            Logger.d("on text = %s\tstart = %d\tbefore = %d\tcount = %d", e.text(), e.start(), e.before(), e.count());
        });
        after.subscribe(e -> {
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
    }
}
