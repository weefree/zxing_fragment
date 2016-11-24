package com.google.zxing.client.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.client.android.camera.CameraManager;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Created by weefree on 2016/11/23.
 */
public class CaptureFragment extends BaseFragment implements SurfaceHolder.Callback,IZXing {
    private static final String TAG = BaseFragment.class.getSimpleName();

    private static final int FRAME_WIDTH_DP = 196;
    private static final int FRAME_HEIGHT_DP = 196;

    private CameraManager cameraManager;
    private ViewfinderView viewfinderView;
    private CaptureActivityHandler handler;
    private BeepManager beepManager;
    private AmbientLightManager ambientLightManager;
    private InactivityTimer inactivityTimer;
    private boolean hasSurface;
    private Collection<BarcodeFormat> decodeFormats;
    private Map<DecodeHintType,?> decodeHints;
    private String characterSet;
    private Result savedResultToShow;

    private OnFragmentInteractionListener mListener;

    @Override
    public int getResouceLayout() {
        return R.layout.fragment_capture;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public static CaptureFragment newInstance() {

        Bundle args = new Bundle();
        CaptureFragment fragment = new CaptureFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        hasSurface = false;
        inactivityTimer = new InactivityTimer(getActivity());
        beepManager = new BeepManager(getActivity());
        ambientLightManager = new AmbientLightManager(getActivity());

        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        startZXing();
    }

    @Override
    public void onPause() {
        stopZXing();
        super.onPause();
    }

    public void startZXing(){

        cameraManager = new CameraManager(this);

        viewfinderView = (ViewfinderView) getView().findViewById(R.id.viewfinder_view);
        viewfinderView.setCameraManager(cameraManager);


        handler = null;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        beepManager.updatePrefs();
        ambientLightManager.start(cameraManager);

        inactivityTimer.onResume();

        decodeFormats = null;
        characterSet = null;


        SurfaceView surfaceView = (SurfaceView) getView().findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this);
        }
    }

   public void stopZXing(){
       if (handler != null) {
           handler.quitSynchronously();
           handler = null;
       }
       inactivityTimer.onPause();
       ambientLightManager.stop();
       beepManager.close();
       cameraManager.closeDriver();
       //historyManager = null; // Keep for onActivityResult
       if (!hasSurface) {
           SurfaceView surfaceView = (SurfaceView) getView().findViewById(R.id.preview_view);
           SurfaceHolder surfaceHolder = surfaceView.getHolder();
           surfaceHolder.removeCallback(this);
       }
   }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(this, decodeFormats, decodeHints, characterSet, cameraManager);
            }
            decodeOrStoreSavedBitmap(null, null);
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }
    }

    private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
        // Bitmap isn't used yet -- will be used soon
        if (handler == null) {
            savedResultToShow = result;
        } else {
            if (result != null) {
                savedResultToShow = result;
            }
            if (savedResultToShow != null) {
                Message message = Message.obtain(handler, R.id.decode_succeeded, savedResultToShow);
                handler.sendMessage(message);
            }
            savedResultToShow = null;
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(getString(R.string.msg_camera_framework_bug));
        builder.setPositiveButton(R.string.button_ok, null);
        builder.setOnCancelListener(new FinishListener(getActivity()));
        builder.show();
    }

    public void openTorch(boolean isOpen){
        if(cameraManager!=null){
            cameraManager.setTorch(isOpen);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    @Override
    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {

        inactivityTimer.onActivity();
        if(mListener!=null)mListener.handleDecode(rawResult,barcode,scaleFactor);
        if(beepManager!=null)beepManager.playBeepSoundAndVibrate();
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
    }

    @Override
    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    @Override
    public CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public Handler getHandler() {
        return handler;
    }

    /**
     * 定义扫描区域
     * 此Fragment只定义简单扫描UI,复杂UI放在Activity层定义
     * @return
     */
    @Override
    public Rect getFramingRect() {
        Point screenSize = ViewUtils.getScreenSize(getActivity());
        int widthPX = ViewUtils.dip2px(getActivity(),FRAME_WIDTH_DP);
        int heightPX = ViewUtils.dip2px(getActivity(),FRAME_HEIGHT_DP);
        int topPX = (screenSize.y-heightPX)/2;
        int leftPX = (screenSize.x-widthPX)/2;
        return new Rect(leftPX,topPX,leftPX+widthPX,topPX+heightPX);
    }

    @Override
    public void onDestroy() {
        if(inactivityTimer!=null)inactivityTimer.shutdown();
        super.onDestroy();
    }

    public interface OnFragmentInteractionListener {
        public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor);
    }
}