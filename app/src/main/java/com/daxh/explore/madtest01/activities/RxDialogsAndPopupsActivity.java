package com.daxh.explore.madtest01.activities;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.annimon.stream.Optional;
import com.daxh.explore.madtest01.R;
import com.jakewharton.rxbinding.view.RxView;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;
import com.trello.rxlifecycle.components.support.RxAppCompatDialogFragment;

import rx.Observable;
import rx.subscriptions.Subscriptions;

// This example is based on the following article:
// http://adelnizamutdinov.github.io/blog/2014/11/23/advanced-rxjava-on-android-popupmenus-and-dialogs/

public class RxDialogsAndPopupsActivity extends RxAppCompatActivity {

    private Optional<Button> btStart = Optional.empty();
    private Optional<Button> btSwitch = Optional.empty();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rx_dialogs_and_popups);

        // Rx Style
        btStart = Optional.ofNullable((Button)findViewById(R.id.btStart))
                .executeIfPresent(view -> RxView.clicks(view)
                        .flatMap(aVoid -> RxSigninDialog.create((String) view.getText(), getSupportFragmentManager(), "dlg"))
                        .flatMap(dialogRes -> Observable.just(
                                Optional.ofNullable(dialogRes)
                                    .flatMap(pairName -> Optional.of("Entered: " + pairName.first + " " + pairName.second))
                                    .orElse("Cancelled")
                            )
                        )
                        .compose(bindToLifecycle())
                        .subscribe(s -> Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show()));

//        // Old style
//        btSwitch = Optional.ofNullable((Button)findViewById(R.id.btSwitch))
//                .executeIfPresent(bt -> bt.setOnClickListener(view -> showPopup(view, R.menu.menu_switch, this::switchPopupMenuItemClicked)));

        // Rx Style
        btSwitch = Optional.ofNullable((Button)findViewById(R.id.btSwitch))
                .executeIfPresent(bt -> RxView.clicks(bt)
                        .flatMap(aVoid -> showPopupAsObservable(bt, R.menu.menu_switch))
                        .compose(bindToLifecycle())
                        .subscribe(this::switchPopupMenuItemClicked));
    }

//    private void showPopup(View view, int menuId, Action1<MenuItem> popupMenuItemClicked) {
//        final PopupMenu menu = new PopupMenu(this, view);
//        menu.inflate(menuId);
//        menu.setOnMenuItemClickListener(item -> {
//            popupMenuItemClicked.call(item);
//            return true;
//        });
//        menu.show();
//    }

    private Observable<MenuItem> showPopupAsObservable(View view, int menuId) {
        return Observable.create(subscriber -> {
            final PopupMenu menu = new PopupMenu(this, view);
            menu.inflate(menuId);

            // cleaning up in case of unsubscribe() call
            subscriber.add(Subscriptions.create(() -> {
                menu.setOnMenuItemClickListener(null);
                menu.dismiss();
            }));

            menu.setOnMenuItemClickListener(item -> {
                subscriber.onNext(item);
                // PopupMenu always emits exactly one item
                subscriber.onCompleted();
                return true;
            });

            menu.show();
        });
    }

    private void switchPopupMenuItemClicked(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.miSignin:
                btStart.ifPresent(bt -> bt.setText(R.string.signin));
                break;
            case R.id.miLogin:
                btStart.ifPresent(bt -> bt.setText(R.string.login));
                break;
            case R.id.miReset:
                btStart.ifPresent(bt -> bt.setText(R.string.reset));
                break;
        }
    }

    public static class SignDialog extends RxAppCompatDialogFragment {

        Optional<String> title = Optional.empty();

        Optional<TextView> tvTitle = Optional.empty();
        Optional<EditText> etFirstName = Optional.empty();
        Optional<EditText> etLastName = Optional.empty();

        Optional<Listener> listener = Optional.empty();

        public static SignDialog create(String title, Listener listener){
            SignDialog signDialog = new SignDialog();
            signDialog.setTitle(title);
            signDialog.setListener(listener);
            return signDialog;
        }

        public void setTitle(String title) {
            this.title = Optional.ofNullable(title);
        }

        public void setListener(Listener listener) {
            this.listener = Optional.ofNullable(listener);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            // Get the layout inflater
            LayoutInflater inflater = getActivity().getLayoutInflater();

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            Optional<View> root = Optional.ofNullable(inflater.inflate(R.layout.dialog_signin, null));
            root.ifPresent(v -> {
                tvTitle = Optional.ofNullable((TextView) v.findViewById(R.id.tvTitle));
                etFirstName = Optional.ofNullable((EditText) v.findViewById(R.id.etFirstName));
                etLastName = Optional.ofNullable((EditText) v.findViewById(R.id.etLastName));

                title.ifPresent(s -> tvTitle.ifPresent(tv -> tv.setText(s)));

                builder.setView(v)
                        // Add action buttons
                        .setPositiveButton(R.string.bt_ok, (dialog, id) -> {
                            // sign in the user ...
                            listener.ifPresent(l -> l.onEntered(
                                            etFirstName.flatMap(et -> Optional.ofNullable(et.getText().toString())).orElse(""),
                                            etLastName.flatMap(et -> Optional.ofNullable(et.getText().toString())).orElse("")));
                        })
                        .setNegativeButton(R.string.bt_cancel, (dialog, id) -> {
                            listener.ifPresent(Listener::onCanceled);
                        });
            });

            return builder.create();
        }

        public interface Listener{
            void onEntered(String firstName, String lastName);
            void onCanceled();
        }
    }

    // Special factory method that allows us to use our
    // dialog in rx chains implemented as a separate class
    // not accidentally. This is some kind of demonstration
    // of how easily any of old existing dialogs that we
    // have could be transformed into rx style.
    public static class RxSigninDialog {
        public static Observable<Pair<String, String>> create(String title, FragmentManager fragmentManager, String tag){
            return Observable.create(subscriber -> {
                SignDialog signDialog = SignDialog.create(title, new SignDialog.Listener() {

                    @Override
                    public void onEntered(String firstName, String lastName) {
                        subscriber.onNext(new Pair<>(firstName, lastName));
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onCanceled() {
                        subscriber.onNext(null);
                        subscriber.onCompleted();
                    }
                });

                subscriber.add(Subscriptions.create(signDialog::dismiss));
                signDialog.show(fragmentManager, tag);
            });
        }
    }
}
