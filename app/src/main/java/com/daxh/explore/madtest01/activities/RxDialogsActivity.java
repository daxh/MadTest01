package com.daxh.explore.madtest01.activities;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.annimon.stream.Optional;
import com.daxh.explore.madtest01.R;
import com.jakewharton.rxbinding.view.RxView;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;
import com.trello.rxlifecycle.components.support.RxAppCompatDialogFragment;

public class RxDialogsActivity extends RxAppCompatActivity {

    private Optional<Button> btStart = Optional.empty();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rx_dialogs);

        btStart = Optional.ofNullable((Button)findViewById(R.id.btStart))
                .executeIfPresent(view -> RxView.clicks(view)
                        .compose(bindToLifecycle())
                        .subscribe(aVoid -> showSignInDialog()));
    }

    private void showSignInDialog() {
        SignDialog signDialog = SignDialog.create(null, new SignDialog.Listener() {
            @Override
            public void onEntered(String firstName, String lastName) {
                Toast.makeText(getApplicationContext(), "Entered: " + firstName + " " + lastName, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCanceled() {
                Toast.makeText(getApplicationContext(), "Cancelled", Toast.LENGTH_SHORT).show();
            }
        });
        signDialog.show(getSupportFragmentManager(), "dlg");
    }

    public static class SignDialog extends RxAppCompatDialogFragment {

        Optional<String> title = Optional.empty();

        Optional<EditText> etFirstName = Optional.empty();
        Optional<EditText> etLastName = Optional.empty();

        Optional<Listener> listener;

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
                etFirstName = Optional.ofNullable((EditText) v.findViewById(R.id.etFirstName));
                etLastName = Optional.ofNullable((EditText) v.findViewById(R.id.etLastName));

                title.ifPresent(builder::setTitle);

                builder.setView(v)
                        // Add action buttons
                        .setPositiveButton(R.string.signin, (dialog, id) -> {
                            // sign in the user ...
                            listener.ifPresent(l -> l.onEntered(
                                            etFirstName.flatMap(et -> Optional.ofNullable(et.getText().toString())).orElse(""),
                                            etLastName.flatMap(et -> Optional.ofNullable(et.getText().toString())).orElse("")));
                        })
                        .setNegativeButton(R.string.cancel, (dialog, id) -> {
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
}
