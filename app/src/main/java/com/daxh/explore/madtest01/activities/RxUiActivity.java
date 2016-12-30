package com.daxh.explore.madtest01.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.annimon.stream.Optional;
import com.daxh.explore.madtest01.R;
import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.jakewharton.rxbinding.widget.TextViewAfterTextChangeEvent;
import com.jakewharton.rxbinding.widget.TextViewBeforeTextChangeEvent;
import com.jakewharton.rxbinding.widget.TextViewTextChangeEvent;
import com.orhanobut.logger.Logger;

import rx.Observable;

public class RxUiActivity extends AppCompatActivity{

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rx_ui);

        Optional.ofNullable(findViewById(R.id.btStart))
                .ifPresent(v -> RxView.clicks(v)
                .subscribe(aVoid -> runLrtWithRx()));

        Optional.ofNullable((EditText) findViewById(R.id.etPhoneNumber))
                .ifPresent(editText -> {
                    setupPhoneNumberInputMask(editText);

                    RxTextView.editorActionEvents(editText).subscribe(event -> {
                        if (event.actionId() == EditorInfo.IME_ACTION_DONE) {
                            runLrtWithRx();
                        }
                    });
                });
    }

    private void runLrtWithRx() {
        Logger.d("runLrtWithRx");
    }

    private void setupPhoneNumberInputMask(EditText editText) {
        // 'Share' = useful operator that allows to
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

                    if (curText.length() >= 17) {
                        // All numbers already entered
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
