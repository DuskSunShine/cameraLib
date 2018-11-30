package com.scy.cameralib.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * Created by SCY on 2018/11/29 at 11:33.
 */
public class CameraManager implements CameraLifecycle, SurfaceHolder.Callback {

    private static final String TAG = "CameraManager";

    @SuppressLint("StaticFieldLeak")
    private static CameraManager cameraManager = null;

    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    private CameraControllerImpl cameraControl;
    /**
     * 是否开始预览
     */
    private boolean hasSurface = false;

    private SurfaceHolder surfaceHolder;

    private CameraFacing cameraId;

    private CameraManager() {

    }

    public static CameraManager init(Context context) {
        mContext = context;
        if (cameraManager == null) {
            synchronized (CameraManager.class) {
                cameraManager = new CameraManager();
            }
        }
        return cameraManager;
    }

    @Override
    public void onCreate(SurfaceHolder surfaceHolder, CameraFacing cameraId) {
        this.surfaceHolder = surfaceHolder;
        this.cameraId = cameraId;
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.setKeepScreenOn(true);
        cameraControl = new CameraControllerImpl(mContext);
    }


    @Override
    public void onResume() {
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            if (cameraControl.isCameraOpen()) {
                Log.w(TAG, "已经打开摄像头");
                return;
            }
            initCameraPreview();
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this);
        }
    }

    /**
     * 初始化相机，开始预览
     */
    private void initCameraPreview() {
        cameraControl.openDriver(surfaceHolder, cameraId.ordinal());
        cameraControl.startPreview();
    }

    @Override
    public void onPause() {
        cameraControl.stopPreview();
        cameraControl.closeDriver();
        if (!hasSurface) {
            surfaceHolder.removeCallback(this);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            if (cameraControl.isCameraOpen()) {
                Log.w(TAG, "已经打开摄像头");
                return;
            }
            initCameraPreview();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    public CameraControllerImpl getCameraController() {
        return cameraControl;
    }
}
