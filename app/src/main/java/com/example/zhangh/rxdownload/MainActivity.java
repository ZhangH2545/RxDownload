package com.example.zhangh.rxdownload;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.zhangh.rxdownload.listener.DownloadListener;
import com.example.zhangh.rxdownload.util.DownloadManagerFactory;

public class MainActivity extends AppCompatActivity implements DownloadListener {
private TextView btn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DownloadManagerFactory.getInstance(this).addListener(this);
        btn= (TextView) findViewById(R.id.click);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //地址 标志  下载存放路径 文件名称
                DownloadManagerFactory.getInstance(MainActivity.this).download("","标志","","");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DownloadManagerFactory.getInstance(this).removeDownloadListener(this);
    }

    @Override
    public void fail(String url, String msg) {

    }

    @Override
    public void progress(String url, int progress) {

    }

    @Override
    public void success(String url, String path, String fileName) {

    }

    @Override
    public void pause(String url) {

    }
}
