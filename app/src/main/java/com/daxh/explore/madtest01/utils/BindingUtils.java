package com.daxh.explore.madtest01.utils;

import android.app.Activity;
import android.content.Intent;

import com.annimon.stream.Optional;

import rx.functions.Action0;
import rx.functions.Func0;

public class BindingUtils {
    public static void bindButton(Activity activity, int btId, Action0 action0) {
        Optional.ofNullable(activity.findViewById(btId))
                .ifPresent(
                bt -> bt.setOnClickListener(
                v -> Optional.ofNullable(action0).ifPresent(Action0::call)));
    }

    public static void bindButton(Activity activity, int btId, Func0<Intent> func0) {
        Optional.ofNullable(activity.findViewById(btId))
                .ifPresent(
                bt -> bt.setOnClickListener(
                v -> Optional.ofNullable(func0).ifPresent(intentFunc0 -> activity.startActivity(intentFunc0.call()))));
    }
}
