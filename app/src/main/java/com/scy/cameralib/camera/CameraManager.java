package com.scy.cameralib.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.SurfaceHolder;


/**
 * Created by SCY on 2018/11/29 at 11:33.
 */
public class CameraManager implements CameraLifecycle, SurfaceHolder.Callback {

    private static final String TAG = "CameraManager";

    @SuppressLint("StaticFieldLeak")
    private static CameraManager cameraManager = null;

    private Context mContext;

    private CameraControllerImpl cameraController;
    /**
     * 是否开始预览
     */
    private boolean hasSurface = false;

    private SurfaceHolder surfaceHolder;

    private CameraFacing cameraId;

    private OnPreviewFrameListener previewFrameListener;

    private OnTakePhotoListener onTakePhotoListener;
    /**
     * 拍照存储的文件
     */
    private String fileName;

    /**
     * 拍照存储的文件夹名称
     */
    private String fileDir;

    /**
     * 矩形取景框大小
     */
    private Rect rectViewFinderSize;

    /*public static CameraManager init(Context context) {
        mContext = context;
        if (cameraManager == null) {
            synchronized (CameraManager.class) {
                cameraManager = new CameraManager();
            }
        }
        return cameraManager;
    }*/
    private CameraManager() {
    }

    public CameraManager(Builder builder) {
        this.fileName = builder.fileName;
        this.fileDir = builder.fileDir;
        this.mContext = builder.context;
    }

    public static class Builder {

        private String fileName = "CameraLib";
        private String fileDir = "CameraLib";
        private Context context;
        /**
         * 矩形取景框大小
         */
        private Rect rectViewFinderSize;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder setFileDir(String fileDir) {
            this.fileDir = fileDir;
            return this;
        }

        public CameraManager build() {
            return new CameraManager(this);
        }
    }

    /**
     * Take pictures
     */
    public void takePhoto() {
        if (cameraManager != null) {
            //framingRect = cameraManager.getFramingRect();
            if (cameraController.getCamera() != null) {
                cameraController.getCamera()
                        .takePicture(null,
                                null,
                                new PhotoCallback(mContext,
                                        fileName,
                                        fileDir, rectViewFinderSize
                                        , cameraController, onTakePhotoListener));
            }
        }
    }


    @Override
    public void cameraOpen(SurfaceHolder surfaceHolder, CameraFacing cameraId) {
        this.surfaceHolder = surfaceHolder;
        this.cameraId = cameraId;
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.setKeepScreenOn(true);
        cameraController = new CameraControllerImpl(mContext, previewFrameListener);

    }


    @Override
    public void onResume() {
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            if (cameraController.isCameraOpen()) {
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
        cameraController.openDriver(surfaceHolder, cameraId.ordinal());
        cameraController.startPreview();
    }

    @Override
    public void onPause() {
        cameraController.stopPreview();
        cameraController.closeDriver();
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
            if (cameraController.isCameraOpen()) {
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
        return cameraController;
    }


    public void setOnPreviewFrameListener(OnPreviewFrameListener listener) {
        previewFrameListener = listener;
    }

    public void setOnTakePhotoListener(OnTakePhotoListener onTakePhotoListener) {
        this.onTakePhotoListener = onTakePhotoListener;
    }
}
