package com.scy.cameralib.camera;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.scy.cameralib.AutoFocusManager;
import com.scy.cameralib.CameraManager;

import java.io.IOException;

/**
 * Created by SCY on 2018/11/29 at 11:52.
 * 相机控制具体实现
 */
public class CameraControlImpl implements CameraControl,Camera.PreviewCallback {

    private static final String TAG = "CameraControlImpl";

    /**
     * 相机方向
     */
    private int cameraOrientation;

    /**
     * 相机位置
     */
    private int cameraFacing;

    /**
     * 相机
     */
    private Camera mCamera;

    private boolean previewing;

    private AutoFocusManager autoFocusManager;

    private OnPreviewFrameListener previewFrameListener;

    /**
     * 开启连续自动对焦(不适合拍照)
     */
    private boolean openAutoFocus = false;
    /**
     * 开始初始化相机配置
     */
    private boolean initialized;

    /**
     *
     */
    private Point screenResolution;

    private Point cameraResolution;

    private Point bestPreviewSize;

    private Point bestPictureSize;

    private int cwRotationCamera;

    private Context mContext;

    public CameraControlImpl(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    public synchronized void openCamera(int cameraId) {
        int numberOfCameras = Camera.getNumberOfCameras();
        if (numberOfCameras == 0) {
            Log.w(TAG, "设备没有可用相机");
            return;
        }
        if (cameraId >= numberOfCameras) {
            Log.w(TAG, "没有存在的相机(0/后置，1/前置)# " + cameraId);
            return;
        }
        Log.i(TAG, "打开相机#" + cameraId + "(0/后置，1/前置)");
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);
        cameraOrientation = cameraInfo.orientation;
        cameraFacing = cameraInfo.facing;
        mCamera = Camera.open(cameraId);
    }

    @Override
    public void initCameraParameters() {
        if (!initialized) {
            initialized = true;
            Camera.Parameters parameters = mCamera.getParameters();
            WindowManager manager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            if (manager != null) {
                Display display = manager.getDefaultDisplay();
                int displayRotation = display.getRotation();//当前摄像头角度
                int displayDegree;//需要的显示角度
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                        displayDegree = 0;
                        break;
                    case Surface.ROTATION_90:
                        displayDegree = 90;
                        break;
                    case Surface.ROTATION_180:
                        displayDegree = 180;
                        break;
                    case Surface.ROTATION_270:
                        displayDegree = 270;
                        break;
                    default:
                        if (displayRotation % 90 == 0) {
                            displayDegree = (360 + displayRotation) % 360;
                        } else {
                            throw new IllegalArgumentException("Bad rotation: " + displayRotation);
                        }
                }
                Log.i(TAG, "当前显示角度#: " +displayDegree);

                cwRotationCamera=cameraOrientation;
                Log.i(TAG, "当前相机角度#: " + cameraOrientation);

                // Still not 100% sure about this. But acts like we need to flip this:
                if (CameraFacing.values()[cameraFacing] == CameraFacing.FRONT) {
                    cwRotationCamera= (360 - cwRotationCamera) % 360;
                    Log.i(TAG, "前置摄像头重置方向#: " + cwRotationCamera);
                }

                cwRotationCamera =
                        (360 + cwRotationCamera - displayDegree) % 360;
                Log.i(TAG, "最终显示方向#: " + cwRotationCamera);

                Point theScreenResolution = new Point();
                display.getSize(theScreenResolution);
                screenResolution = theScreenResolution;
                Log.i(TAG, "当前方向的屏幕分辨率: " + screenResolution);
                cameraResolution = ConfigUtil.findBestPreviewSizeValue(parameters, screenResolution);
                Log.i(TAG, "相机分辨率: " + cameraResolution);
                bestPreviewSize = ConfigUtil.findBestPreviewSizeValue(parameters, screenResolution);
                Log.i(TAG, "最佳可用预览尺寸: " + bestPreviewSize);
                bestPictureSize = ConfigUtil.findBestPictureSizeValue(parameters, screenResolution);
                Log.i(TAG, "最佳可用预览尺寸: " + bestPictureSize);
                //是否竖屏
                boolean isScreenPortrait = screenResolution.x < screenResolution.y;
                boolean isPreviewSizePortrait = bestPreviewSize.x < bestPreviewSize.y;

                if (isScreenPortrait != isPreviewSizePortrait) {
                    bestPreviewSize = new Point(bestPreviewSize.y, bestPreviewSize.x);
                }
                Log.i(TAG, "屏幕上预览大小: " + bestPreviewSize);
            }

        }
        setDesiredCameraParameters();
    }

    @Override
    public void setDesiredCameraParameters() {
        Camera.Parameters parameters = mCamera.getParameters();
        if (openAutoFocus) {
            if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
        } else {
            if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
        }
        //SetRecordingHint to true also a workaround for low framerate on Nexus 4
        //https://stackoverflow.com/questions/14131900/extreme-camera-lag-on-nexus-4
        parameters.setRecordingHint(true);
        parameters.setPreviewSize(bestPreviewSize.x, bestPreviewSize.y);
        parameters.setPictureSize(bestPictureSize.x, bestPictureSize.y);
        parameters.setJpegQuality(80);
        mCamera.setParameters(parameters);

        mCamera.setDisplayOrientation(cwRotationCamera);

        Camera.Parameters afterParameters = mCamera.getParameters();
        Camera.Size afterSize = afterParameters.getPreviewSize();
        if (afterSize != null && (bestPreviewSize.x != afterSize.width || bestPreviewSize.y != afterSize.height)) {
            Log.i(TAG, "相机支撑的预览大小 " + bestPreviewSize.x + 'x' + bestPreviewSize.y +
                    ", 通过设置后，预览大小 " + afterSize.width + 'x' + afterSize.height);
            bestPreviewSize.x = afterSize.width;
            bestPreviewSize.y = afterSize.height;
        }
    }

    /**
     * 相机是否已经打开
     *
     * @return true 打开,false 未打开
     */
    @Override
    public boolean isCameraOpen() {
        return mCamera != null;
    }

    public Camera getCamera() {
        return mCamera;
    }

    public int getCameraOrientation() {
        return cameraOrientation;
    }

    public int getCameraFacing() {
        return cameraFacing;
    }

    public boolean isOpenAutoFocus() {
        return openAutoFocus;
    }

    public void setOpenAutoFocus(boolean openAutoFocus) {
        this.openAutoFocus = openAutoFocus;
    }

    public Point getScreenResolution() {
        return screenResolution;
    }

    public Point getCameraResolution() {
        return cameraResolution;
    }

    @Override
    public synchronized void openDriver(SurfaceHolder holder) {
        initCameraParameters();
        try {
            if (isCameraOpen()) {
                mCamera.setPreviewDisplay(holder);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void closeDriver() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public synchronized void startPreview() {
        if (mCamera != null && !previewing) {
            mCamera.startPreview();
            previewing = true;
            if (isOpenAutoFocus()) {
                autoFocusManager = new AutoFocusManager(mCamera,openAutoFocus);
            }
        }
        if (mCamera != null && previewing) {
            mCamera.setPreviewCallback(this);
        }
    }

    @Override
    public synchronized void stopPreview() {
        if (autoFocusManager != null) {
            autoFocusManager.stop();
            autoFocusManager = null;
        }
        if (mCamera != null && previewing) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            previewing = false;
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (previewFrameListener != null) {
            previewFrameListener.onPreviewFrame(data, camera);
        }
    }

    public interface OnPreviewFrameListener {
        void onPreviewFrame(byte[] data, Camera camera);
    }

    public void setOnPreviewFrameListener(OnPreviewFrameListener listener) {
        previewFrameListener = listener;
    }
}
