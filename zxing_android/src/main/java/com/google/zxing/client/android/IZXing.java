package com.google.zxing.client.android;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;

import com.google.zxing.Result;
import com.google.zxing.client.android.camera.CameraManager;

/**
 * Created by weefree on 2016/11/22.
 */

public interface IZXing {
    public ViewfinderView getViewfinderView();
    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor);
    public Activity getActivity();
    public void drawViewfinder();
    public CameraManager getCameraManager();
    public Handler getHandler();

}
