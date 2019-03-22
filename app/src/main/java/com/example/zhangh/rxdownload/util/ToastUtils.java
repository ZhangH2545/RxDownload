package com.example.zhangh.rxdownload.util;

import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

public class ToastUtils {

    public static Toast mToast;

    private volatile static ToastUtils mToastUtils = null;

    public static ToastUtils getInstance(Context context) {
        if(mToastUtils == null){
            synchronized (ToastUtils.class) {
                if (mToastUtils == null) {
                    mToastUtils = new ToastUtils(context);
                }
            }
        }

        return mToastUtils;
    }

    private ToastUtils(Context context) {
        mToast = new Toast(context);
        mToast.setDuration(Toast.LENGTH_SHORT);
        mToast.setView(Toast.makeText(context, "", Toast.LENGTH_SHORT).getView());
    }


    public static void show(String msg) {
        if (mToast != null && !TextUtils.isEmpty(msg)) {
            mToast.setText(msg);
            mToast.show();
        }
    }

    public static void cancel() {
        if (mToast != null)
            mToast.cancel();

    }
}
