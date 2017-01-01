package com.daxh.explore.madtest01.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;

import com.annimon.stream.Optional;
import com.annimon.stream.function.Function;
import com.daxh.explore.madtest01.R;
import com.daxh.explore.madtest01.tests.BasicRxUsages;
import com.daxh.explore.madtest01.tests.models.Person;
import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.jakewharton.rxbinding.widget.TextViewAfterTextChangeEvent;
import com.jakewharton.rxbinding.widget.TextViewBeforeTextChangeEvent;
import com.jakewharton.rxbinding.widget.TextViewTextChangeEvent;
import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.util.ArrayList;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.exceptions.Exceptions;
import rx.schedulers.Schedulers;

public class RxUiActivity extends AppCompatActivity{

    public static final int TOTAL_MASKED_PHONE_LENGTH = 16;

    Optional<Button> btStart = Optional.empty();
    Optional<EditText> etPhoneNumber = Optional.empty();

    Function<CharSequence, Boolean> checkSymbolRedundancy = cs -> cs.length() > TOTAL_MASKED_PHONE_LENGTH;
    Function<CharSequence, Boolean> checkPhoneСompleteness = cs -> cs.length() == TOTAL_MASKED_PHONE_LENGTH;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rx_ui);

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
    }

    private void runLrtWithRx() {
        Observable
                .fromCallable(() -> BasicRxUsages.getNewPersonOrError(1))
                .retry((integer, throwable) -> {
                    int max = 3;
                    if (integer < max) {
                        Logger.d("%d / %d getNewPersonOrError failed. Retrying ...", integer+1, max);
                        return true;
                    } else {
                        Logger.d("%d / %d getNewPersonOrError failed. Falling back.", integer, max);
                        return false;
                    }
                })
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(() -> Logger.d("Point #1"))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(person -> {
                    double random = Math.random();

                    if (random < .25){
                        try {
                            throw new IOException("Point #2 " + person);
                        } catch (IOException e) {
                            throw Exceptions.propagate(e);
                        }
                    }

                    if (random < .5){
                        Logger.d("EXCEPTION2: " + "Point #2 " + person);
                        throw new RuntimeException("Point #2 " + person);
                    }

                    Logger.d("Point #2 " + person);
                })
                .retryWhen(observable -> observable.flatMap(throwable -> {
                    if ((throwable instanceof RuntimeException)) {
                        Logger.d("RETRY: Point #2");
                        return Observable.just(null);
                    }

                    Logger.d("FALLBACK: Point #2");
                    return Observable.error(throwable);
                }))
                .observeOn(Schedulers.io())
                .flatMap(user -> Observable.zip(
                        Observable
                                .fromCallable(() -> BasicRxUsages.fetchPersonSettingsOrError(user))
                                .subscribeOn(Schedulers.io())
                                .onErrorReturn(throwable -> {
                                    Logger.d("DEFAULT: fetchPersonSettingsOrError");
                                    return new Person.Settings(user);
                                }),
                        Observable
                                .fromCallable(() -> BasicRxUsages.fetchPersonMessagesOrError(user))
                                .subscribeOn(Schedulers.io())
                                .onErrorReturn(throwable -> {
                                    Logger.d("DEFAULT: fetchPersonSettingsOrError");
                                    return new ArrayList<>();
                                }),
                        Pair::create))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(settingsMsgsListPair -> Logger.d("Point #4 " + settingsMsgsListPair))
                .flatMap(settingsMsgsListPair -> {
                    Logger.d("Point #5 " + settingsMsgsListPair);
                    return Observable.from(settingsMsgsListPair.second);
                })
                .subscribe(
                        // onNext
                        message -> Logger.d("Point #6"),
                        // onError
                        throwable -> {
                            // All exceptions could be processed
                            // in one place - here, if this is a
                            // desirable behavior
                            Logger.d("onError " + throwable.getMessage());
                        },
                        // onCompleted
                        () -> Logger.d("DONE")
                );
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
}
