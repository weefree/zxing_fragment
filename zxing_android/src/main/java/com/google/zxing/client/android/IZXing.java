package com.google.zxing.client.android;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;

import com.google.zxing.Result;
import com.google.zxing.client.android.camera.CameraManager;

/**
 * Created by weefree on 2016/11/22.
 */

public interface IZXing {
    ViewfinderView getViewfinderView();
    void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor);
    Activity getActivity();
    void drawViewfinder();
    CameraManager getCameraManager();
    Handler getHandler();
    Rect getFramingRect();


}
