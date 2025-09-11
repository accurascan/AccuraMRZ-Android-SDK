package com.accurascan.accura.mrz.sample.download;

import com.androidnetworking.error.ANError;

public abstract class AccuraDownloadListener {

    public abstract void onDownloadComplete(String filePath);
    public void onProgress(int progress){}
    public abstract void onError(String s, ANError error);
}
