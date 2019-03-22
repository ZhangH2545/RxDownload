package com.example.zhangh.rxdownload.util;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.Log;

import com.example.zhangh.rxdownload.listener.DownloadListener;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import com.zhang.rxdownload.RxDownload;
import com.zhang.rxdownload.db.DataBaseHelper;
import com.zhang.rxdownload.entity.DownloadStatus;
public class DownloadManager {


    private volatile Map<String, Integer> mUrls = new HashMap<>();
    private volatile Set<String> mSetUrls = new HashSet<>();
    private volatile Map<String, Disposable> mDisposableUrls = new HashMap<>();
    private RxDownload rxDownload;  //单例

    private Handler mhandler = new Handler(Looper.getMainLooper());


    private volatile List<DownloadListener> mDownloadListenerList = new ArrayList<>();

    private Context mContext;

    protected DownloadManager(Context context) {
        mContext = context.getApplicationContext();
        if (rxDownload == null) {
            rxDownload = RxDownload.getInstance(mContext)
                    .maxThread(3)
                    .maxRetryCount(5)
                    .maxDownloadNumber(5);
        }

    }

    public void addListener(DownloadListener downloadListener) {
        if (downloadListener != null) {
            mDownloadListenerList.add(downloadListener);
        }
    }


    public void removeDownloadListener(DownloadListener downloadListener) {
        synchronized (mDownloadListenerList) {
            if (mDownloadListenerList.contains(downloadListener)) {
                mDownloadListenerList.remove(downloadListener);
            }
        }
    }

    public void pauseAll() {
        try {
            rxDownload.pauseAll();
            mUrls.clear();
            mDisposableUrls.clear();
            for (String url : mSetUrls) {
                DataBaseHelper.getSingleton(mContext).deleteRecord(url);
            }
            mSetUrls.clear();
        } catch (Exception e1) {
            e1.printStackTrace();
        }

    }


    public void pause(String uri, String tag) {
        String urlTag = TextUtils.isEmpty(tag) ? uri : tag;
        synchronized (mDisposableUrls) {
            if (mDisposableUrls.containsKey(urlTag)) {
                Disposable disposable = mDisposableUrls.get(urlTag);
                if (disposable != null && !disposable.isDisposed()) {
                    disposable.dispose();
                    synchronized (mUrls) {
                        if (mUrls.containsKey(urlTag)) {
                            mUrls.remove(urlTag);
                        }
                    }
                    synchronized (mDownloadListenerList) {
                        for (DownloadListener downloadListener : mDownloadListenerList) {
                            downloadListener.pause(urlTag);
                        }
                    }

                }
            }
        }
    }

    public void pause(String url) {
        pause(url, null);

    }

    /**
     * 注意，这里的url是下载地址 ，如果 tag是空，那么url就作为下载地址和唯一标识一起使用，如果tag不是空，那么后续所有操作只依赖于tag
     *
     * @param url
     * @param tag
     * @param path
     * @param fileName
     */
    public void download(String url, String tag, String path, String fileName) {

        if (checkSDCard()) {
            downloadUrl(url, tag, path, fileName);
        } else {
            ToastUtils.getInstance(mContext).show(MSG);
        }


    }

    public void download(String url, String path, String fileName) {

        download(url, null, path, fileName);

    }

    public static final String MSG = "外部存储空间不足，无法下载";

    public static boolean checkSDCard() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File sdcardDir = Environment.getExternalStorageDirectory();
            StatFs sf = new StatFs(sdcardDir.getPath());
            long blockSize = sf.getBlockSize();
            long blockCount = sf.getBlockCount();
            long availCount = sf.getAvailableBlocks();
            Log.d("", "可用的block数目：:" + availCount + ",剩余空间:" + availCount * blockSize + "M");

            long availableSize = availCount * blockSize; //m

            if (availableSize < 100) { //bug 8309
                return false;
            }
            return true;
        }
        return false;
    }



    private void downloadUrl(final String url, final String urlTag, final String path, final String fileName) {
        //如果之前下载过，就先删掉，重新下载，暂时不直接使用下载好的内容

        String downloadUrl = url;
        final String callBackTag = TextUtils.isEmpty(urlTag) ? url : urlTag;

        File[] files = rxDownload.getRealFiles(url);
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.exists()) {
                    file.delete();
                }
            }
        }
//        FileHelper.createNoMediaFile(path);

        final String hostUrl = getHostUrl(downloadUrl);
        mSetUrls.add(hostUrl);
        Disposable disposable = rxDownload
                .download(hostUrl, fileName, path)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<DownloadStatus>() {
                    @Override
                    public void accept(DownloadStatus status) throws Exception {
                        addProgress(callBackTag, status);
                    }

                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        //下载失败
                        addError(callBackTag, throwable.toString());
                        mSetUrls.remove(hostUrl);

                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        //下载成功
                        addComplete(callBackTag, path, fileName);
                        mSetUrls.remove(hostUrl);

                    }
                });
        mDisposableUrls.put(callBackTag, disposable);
    }


    private String getHostUrl(String url) {

        if (url.startsWith("http")) {
            return url;
        }
        return K12OkHttpUtils.getFileUrl(mContext) + url;

    }

    private void addComplete(final String tag, final String path, final String fileName) {


        synchronized (mDisposableUrls) {
            if (mDisposableUrls.containsKey(tag)) {
                mDisposableUrls.remove(tag);
            }
        }

        synchronized (mUrls) {
            if (mUrls.containsKey(tag)) {
                mUrls.remove(tag);
            }
        }

        mhandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                synchronized (mDownloadListenerList) {
                    for (DownloadListener downloadListener : mDownloadListenerList) {
                        downloadListener.success(tag, path, fileName);
                    }
                }

            }
        }, 500);
    }


    private void addError(final String url, final String error) {
        synchronized (mUrls) {
            if (mUrls.containsKey(url)) {
                mUrls.remove(url);
            }
        }
        synchronized (mDisposableUrls) {
            if (mDisposableUrls.containsKey(url)) {
                mDisposableUrls.remove(url);
            }
        }
        mhandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                synchronized (mDownloadListenerList) {
                    for (DownloadListener downloadListener : mDownloadListenerList) {
                        downloadListener.fail(url, error);
                    }
                }
            }
        }, 500);

    }



    private void addProgress( String url, DownloadStatus downloadStatus) {

        long progress = downloadStatus.getPercentNumber();

        if (progress >= 99) {
            mUrls.remove(url);
            return;
        }

        synchronized (mUrls) {
            mUrls.put(url, (int) progress);
        }
        synchronized (mDownloadListenerList) {
            for (DownloadListener downloadListener : mDownloadListenerList) {
                downloadListener.progress(url, (int) progress);
            }
        }


    }

    public int getProgress(String url) {
        if (mUrls.containsKey(url)) {
            return mUrls.get(url);
        }
        return -1;

    }
}
