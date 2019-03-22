package com.example.zhangh.rxdownload.util;

import android.content.Context;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DownloadManagerFactory {


    private volatile static DownloadManager sDownloadManager;
    private static ThreadPoolExecutor mExecutor = new ThreadPoolExecutor(3, 5,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingDeque<Runnable>());

    public static DownloadManager getInstance(final Context context) {
        synchronized (DownloadManagerFactory.class) {
            if (sDownloadManager == null) {
                synchronized (DownloadManagerFactory.class) {
                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            sDownloadManager = new DownloadManager(context);

                        }
                    });
                }
            }

        }
        return sDownloadManager;

    }
}
