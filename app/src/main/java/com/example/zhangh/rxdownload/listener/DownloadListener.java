package com.example.zhangh.rxdownload.listener;

public interface DownloadListener {
    void fail(String url, String msg);

    void progress(String url, int progress);

    void success(String url, String path, String fileName);

    void pause(String url);
}
