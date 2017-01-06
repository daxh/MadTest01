package com.daxh.explore.madtest01.activities;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.Button;

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
        SignDialog signDialog = new SignDialog();
        signDialog.show(getSupportFragmentManager(), "dlg");
    }

    public static class SignDialog extends RxAppCompatDialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            // Get the layout inflater
            LayoutInflater inflater = getActivity().getLayoutInflater();

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(inflater.inflate(R.layout.dialog_signin, null))
                    // Add action buttons
                    .setPositiveButton(R.string.signin, (dialog, id) -> {
                        // sign in the user ...
                    })
                    .setNegativeButton(R.string.cancel, (dialog, id) -> {
//                            SignDialogFragment.this.getDialog().cancel();
                    });
            return builder.create();
        }
    }
}
