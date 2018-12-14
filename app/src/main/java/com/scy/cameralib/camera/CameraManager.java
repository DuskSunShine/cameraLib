package com.scy.cameralib.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
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
    private String fileName = "CameraLib";

    /**
     * 拍照存储的文件夹名称
     */
    private String fileDir = "CameraLib";

    /**
     * 矩形取景框大小
     */
    /**
     * 矩形取景框大小
     */
    private int width = 240;
    private int height = 240;

    private Rect frameRect;
    private Rect framingRectInPreview;

    /**
     * 是否开启连续对焦
     */
    private boolean openAutoFocus = false;

    /**
     * 是否使用取景框
     */
    private boolean useViewFinder;

    private PhotoCallback photoCallback;
    private PreviewFrameCallback previewFrameCallback;

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
        this.openAutoFocus = builder.openAutoFocus;
        this.useViewFinder = builder.useViewFinder;
        this.width = builder.width;
        this.height = builder.height;
    }

    public static class Builder {
        /**
         * 拍照保存的照片名称
         */
        private String fileName;
        /**
         * 拍照时保存照片的文件夹
         */
        private String fileDir;
        private Context context;
        /**
         * 是否开启连续自动对焦(拍照不适合开启)
         */
        private boolean openAutoFocus;
        /**
         * 矩形取景框大小
         */
        private int width;
        private int height;
        /**
         * 是否使用取景框
         */
        private boolean useViewFinder;

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

        public Builder setOpenAutoFocus(boolean openAutoFocus) {
            this.openAutoFocus = openAutoFocus;
            return this;
        }

        public Builder setUseViewFinder(boolean useViewFinder) {
            this.useViewFinder = useViewFinder;
            return this;
        }

        public Builder setRectViewFinderSize(int width, int height) {
            this.width = width;
            this.height = height;
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
//        if (cameraManager != null) {
//            //framingRect = cameraManager.getFramingRect();
//            if (cameraController.getCamera() != null) {
//                cameraController.getCamera()
//                        .takePicture(null,
//                                null,
//                                new PhotoCallback(mContext,
//                                        fileName,
//                                        fileDir, rectViewFinderSize
//                                        , cameraController, onTakePhotoListener));
//            }
//        }
        if (cameraController != null && photoCallback != null) {
            cameraController.takePhoto(photoCallback);
        }
    }


    @Override
    public void cameraOpen(SurfaceHolder surfaceHolder, CameraFacing cameraId) {
        if (surfaceHolder == null) {
            throw new NullPointerException("SurfaceHolder为空！");
        }
        this.surfaceHolder = surfaceHolder;
        this.cameraId = cameraId;
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.setKeepScreenOn(true);
        if (cameraController == null) {
            cameraController = new CameraControllerImpl(mContext, openAutoFocus, useViewFinder);
        }
    }

    /**
     * 将设置的取景框宽高，转化为Rect
     */
    private void rectViewFinderWH2Rect() {
        Point screenResolution = cameraController.getScreenResolution();
        if (width > screenResolution.x) {
            width = screenResolution.x;
        }
        if (height > screenResolution.y) {
            height = screenResolution.y;
        }
        int centerX = (screenResolution.x) / 2;
        int centerY = (screenResolution.y) / 2;
        frameRect = new Rect(centerX - width / 2, centerY - height / 2, centerX + width / 2, centerY + height / 2);
        Log.w(TAG, "设置矩形取景框大小: " + frameRect.toString());
    }

    /**
     * 获取矩形取景框大小
     *
     * @return
     */
    public synchronized Rect getFramingRect() {
        if (frameRect == null) {
            Point screenResolution = cameraController.getScreenResolution();
            if (screenResolution == null) {
                // Called early, before init even finished
                return null;
            }
            int centerX = (screenResolution.x) / 2;
            int centerY = (screenResolution.y) / 2;
            frameRect = new Rect(centerX - width / 2, centerY - height / 2, centerX + width / 2, centerY + height / 2);
            Log.w(TAG, "设置矩形取景框大小: " + frameRect.toString());
        }
        return frameRect;
    }

    /**
     * Like {@link #getFramingRect} but coordinates are in terms of the preview frame,
     * not UI / screen.
     *
     * @return {@link Rect} expressing barcode scan area in terms of the preview size
     * 这是计算出的只取取景框中的帧数据
     */
    public synchronized Rect getFramingRectInPreview() {
        if (framingRectInPreview==null){
            Rect framingRect = getFramingRect();
            Point cameraResolution = cameraController.getCameraResolution();
            Point screenResolution = cameraController.getScreenResolution();
            if (framingRect == null) {
                return null;
            }
            Rect rect = new Rect(framingRect);
            if (cameraResolution == null || screenResolution == null) {
                // Called early, before init even finished
                return null;
            }
            if (screenResolution.x < screenResolution.y) {
                // portrait
                rect.left = rect.left * cameraResolution.y / screenResolution.x;
                rect.right = rect.right * cameraResolution.y / screenResolution.x;
                rect.top = rect.top * cameraResolution.x / screenResolution.y;
                rect.bottom = rect.bottom * cameraResolution.x / screenResolution.y;
            } else {
                // landscape
                rect.left = rect.left * cameraResolution.x / screenResolution.x;
                rect.right = rect.right * cameraResolution.x / screenResolution.x;
                rect.top = rect.top * cameraResolution.y / screenResolution.y;
                rect.bottom = rect.bottom * cameraResolution.y / screenResolution.y;

            }
            framingRectInPreview=rect;
            Log.w(TAG, "预览中数据所在位置(只取取景框中的数据):" + framingRectInPreview.toString());
        }

        return framingRectInPreview;
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
        rectViewFinderWH2Rect();
        if (photoCallback == null) {
            photoCallback = new PhotoCallback(mContext,
                    fileName,
                    fileDir, getFramingRectInPreview()
                    , cameraController, useViewFinder
                    , onTakePhotoListener);
        }
        if (previewFrameCallback == null) {
            previewFrameCallback = new PreviewFrameCallback(cameraController, useViewFinder, getFramingRectInPreview(), previewFrameListener);
        }
        cameraController.setPreviewFrameCallback(previewFrameCallback);
    }

    @Override
    public void onPause() {
        cameraController.stopPreview();
        removeCallback();
        cameraController.closeDriver();
        if (!hasSurface) {
            surfaceHolder.removeCallback(this);
        }
    }

    /**
     * remove takePhoto and framePreview callback.
     */
    private void removeCallback() {
        if (photoCallback != null) {
            photoCallback = null;
        }
        if (previewFrameCallback != null) {
            previewFrameCallback = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e(TAG,"surface create");
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


    public void setOnPreviewFrameListener(OnPreviewFrameListener previewFrameListener) {
        this.previewFrameListener = previewFrameListener;
    }

    public void setOnTakePhotoListener(OnTakePhotoListener onTakePhotoListener) {
        this.onTakePhotoListener = onTakePhotoListener;
    }
}
