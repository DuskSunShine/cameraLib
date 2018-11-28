package com.scy.cameralib;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.io.IOException;
import java.util.List;

/**
 * Created by SCY on 2018/11/26 at 9:27.
 */
public class CameraManager implements Camera.PreviewCallback {

    private static final String TAG = "CameraManager";

    private static final int MIN_FRAME_WIDTH = 240;
    private static final int MIN_FRAME_HEIGHT = 240;
    private static final int MAX_FRAME_WIDTH = 1200; // = 5/8 * 1920
    private static final int MAX_FRAME_HEIGHT = 675; // = 5/8 * 1080

    @SuppressLint("StaticFieldLeak")
    private static CameraManager CAMERA_MANAGER = null;

    private Context mContext;

    public static final int FRONT = 1;//1 foreground camera;

    public static final int BACK = 0;// 0 background camera;

    private Camera mCamera;

    private boolean initialized;//初始化完成

    private boolean previewing;//已经开始预览
    private int cameraOrientation;//相机方向
    private int cameraFacing;//相机前置/后置
    private int cwNeededRotation;//最终的方向????
    private int cwRotationFromDisplayToCamera;//显示方向向相机方向旋转
    private Point screenResolution;//屏幕分辨率
    private Point cameraResolution;//相机分辨率
    private Point bestPreviewSize;//最佳预览尺寸
    private Point previewSizeOnScreen;//在屏幕预览的大小
    private static final int MIN_PREVIEW_PIXELS = 480 * 320; // normal screen
    private static final double MAX_ASPECT_DISTORTION = 0.15;
    private int requestedFramingRectWidth;
    private int requestedFramingRectHeight;
    private Rect framingRect;//
    private Rect framingRectInPreview;//预览的取景框
    private OnPreviewFrameListener previewFrameListener;
    private AutoFocusManager autoFocusManager;
    private boolean openAutoFocus = false;//开启连续自动对焦(不适合拍照)


    public void setOpenAutoFocus(boolean openAutoFocus) {
        this.openAutoFocus = openAutoFocus;
    }

    public Point getCameraResolution() {
        return cameraResolution;
    }

    private CameraManager() {
    }

    public static CameraManager init() {
        if (CAMERA_MANAGER == null) {
            synchronized (CameraManager.class) {
                CAMERA_MANAGER = new CameraManager();
            }
        }
        return CAMERA_MANAGER;
    }


    /**
     * 打开相机
     *
     * @param cameraId 相机id
     */
    public synchronized void openCamera(Context context, SurfaceHolder holder, int cameraId) {
        this.mContext = context;
        int numberOfCameras = Camera.getNumberOfCameras();
        if (numberOfCameras == 0) {
            Log.w(TAG, "设备没有可用相机");
            return;
        }
        if (cameraId >= numberOfCameras) {
            Log.w(TAG, "没有存在的相机(0/后置，1/前置): " + cameraId);
            return;
        }
        Log.i(TAG, "打开相机#" + cameraId + "(0/后置，1/前置)");
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);
        cameraOrientation = cameraInfo.orientation;
        cameraFacing = cameraInfo.facing;
        mCamera = Camera.open(cameraId);
        initCameraParameters();
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Camera getCamera() {
        return mCamera;
    }


    /**
     * 关闭相机
     */
    public synchronized void closeCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
            framingRect = null;
            framingRectInPreview = null;
        }
    }

    /**
     * 开始预览
     */
    public synchronized void startPreview() {
        if (mCamera != null && !previewing) {
            mCamera.startPreview();
            previewing = true;
            if (openAutoFocus) {
                autoFocusManager = new AutoFocusManager(mCamera);
            }
        }
        if (mCamera != null && previewing) {
            mCamera.setPreviewCallback(this);
        }
    }

    /**
     * 结束预览
     */
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


    /**
     * 初始化相机参数等
     */
    private void initCameraParameters() {
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
                Log.i(TAG, "显示角度#: " + displayRotation + "," + displayDegree);

                int cwRotationFromNaturalToCamera = cameraOrientation;
                Log.i(TAG, "相机角度#: " + cwRotationFromNaturalToCamera);

                // Still not 100% sure about this. But acts like we need to flip this:
                if (cameraFacing == FRONT) {
                    cwRotationFromNaturalToCamera = (360 - cwRotationFromNaturalToCamera) % 360;
                    Log.i(TAG, "前置摄像头重置方向#: " + cwRotationFromNaturalToCamera);
                }

                cwRotationFromDisplayToCamera =
                        (360 + cwRotationFromNaturalToCamera - displayDegree) % 360;
                Log.i(TAG, "最终显示方向#: " + cwRotationFromDisplayToCamera);
                if (cameraFacing == FRONT) {
                    Log.i(TAG, "前置摄像头补偿旋转");
                    cwNeededRotation = (360 - cwRotationFromDisplayToCamera) % 360;
                } else {
                    cwNeededRotation = cwRotationFromDisplayToCamera;
                }
                Log.i(TAG, "显示方向-->照相机顺时针旋转#: " + cwNeededRotation);

                Point theScreenResolution = new Point();
                display.getSize(theScreenResolution);
                screenResolution = theScreenResolution;
                Log.i(TAG, "当前方向的屏幕分辨率: " + screenResolution);
                cameraResolution = findBestPreviewSizeValue(parameters, screenResolution);
                Log.i(TAG, "相机分辨率: " + cameraResolution);
                bestPreviewSize = findBestPreviewSizeValue(parameters, screenResolution);
                Log.i(TAG, "最佳可用预览尺寸: " + bestPreviewSize);
                //是否竖屏
                boolean isScreenPortrait = screenResolution.x < screenResolution.y;
                boolean isPreviewSizePortrait = bestPreviewSize.x < bestPreviewSize.y;

                if (isScreenPortrait == isPreviewSizePortrait) {
                    previewSizeOnScreen = bestPreviewSize;
                } else {
                    previewSizeOnScreen = new Point(bestPreviewSize.y, bestPreviewSize.x);
                }
                Log.i(TAG, "屏幕上预览大小: " + previewSizeOnScreen);


                if (requestedFramingRectWidth > 0 && requestedFramingRectHeight > 0) {
                    setManualFramingRect(requestedFramingRectWidth, requestedFramingRectHeight);
                    requestedFramingRectWidth = 0;
                    requestedFramingRectHeight = 0;
                }
            }

        }
        setDesiredCameraParameters();
    }

    /**
     * 设置取景框大小
     *
     * @param width  宽度
     * @param height 高度
     */
    private synchronized void setManualFramingRect(int width, int height) {
        if (initialized) {
            if (width > screenResolution.x) {
                width = screenResolution.x;
            }
            if (height > screenResolution.y) {
                height = screenResolution.y;
            }
            int leftOffset = (screenResolution.x - width) / 2;
            int topOffset = (screenResolution.y - height) / 2;
            framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
            Log.d(TAG, "计算的取景框大小: " + framingRect);
            framingRectInPreview = null;
        } else {
            requestedFramingRectWidth = width;
            requestedFramingRectHeight = height;
        }
    }

    /**
     * 相机是否已经打开
     *
     * @return true 打开,false 未打开
     */
    public synchronized boolean isOpen() {
        return mCamera != null;
    }

    public Point getPreviewSizeOnScreen() {
        return previewSizeOnScreen;
    }

    /**
     * 设置期望的相机参数
     */
    private void setDesiredCameraParameters() {
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

        parameters.setPreviewSize(bestPreviewSize.x, bestPreviewSize.y);
        parameters.setPictureSize(bestPreviewSize.x, bestPreviewSize.y);
        parameters.setJpegQuality(80);
        mCamera.setParameters(parameters);

        mCamera.setDisplayOrientation(cwRotationFromDisplayToCamera);

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
     * 计算最佳预览尺寸
     *
     * @param parameters
     * @param screenResolution
     * @return
     */
    private Point findBestPreviewSizeValue(Camera.Parameters parameters, Point screenResolution) {

        List<Camera.Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
        if (rawSupportedSizes == null) {
            Log.w(TAG, "设备返回未支持的预览大小；使用默认");
            Camera.Size defaultSize = parameters.getPreviewSize();
            if (defaultSize == null) {
                throw new IllegalStateException("参数中没有可预览大小！");
            }
            return new Point(defaultSize.width, defaultSize.height);
        }

        if (Log.isLoggable(TAG, Log.INFO)) {
            StringBuilder previewSizesString = new StringBuilder();
            for (Camera.Size size : rawSupportedSizes) {
                previewSizesString.append(size.width).append('x').append(size.height).append(' ');
            }
            Log.i(TAG, "设备支撑的预览大小: " + previewSizesString);
        }

        double screenAspectRatio = screenResolution.x / (double) screenResolution.y;

        // Find a suitable size, with max resolution
        int maxResolution = 0;
        Camera.Size maxResPreviewSize = null;
        for (Camera.Size size : rawSupportedSizes) {
            int realWidth = size.width;
            int realHeight = size.height;
            int resolution = realWidth * realHeight;
            if (resolution < MIN_PREVIEW_PIXELS) {
                continue;
            }

            boolean isCandidatePortrait = realWidth < realHeight;
            int maybeFlippedWidth = isCandidatePortrait ? realHeight : realWidth;
            int maybeFlippedHeight = isCandidatePortrait ? realWidth : realHeight;
            double aspectRatio = maybeFlippedWidth / (double) maybeFlippedHeight;
            double distortion = Math.abs(aspectRatio - screenAspectRatio);
            if (distortion > MAX_ASPECT_DISTORTION) {
                continue;
            }

            if (maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
                Point exactPoint = new Point(realWidth, realHeight);
                Log.i(TAG, "预览大小与屏幕尺寸完全匹配: " + exactPoint);
                return exactPoint;
            }

            // Resolution is suitable; record the one with max resolution
            if (resolution > maxResolution) {
                maxResolution = resolution;
                maxResPreviewSize = size;
            }
        }

        // 如果没有精确匹配，请使用最大预览大小。由于需要额外的计算，在旧设备上这不是一个好主意。
        // 我们可能会在更新的Android 4+设备上看到这一点，因为CPU更加强大。
        if (maxResPreviewSize != null) {
            Point largestSize = new Point(maxResPreviewSize.width, maxResPreviewSize.height);
            Log.i(TAG, "使用最大预览: " + largestSize);
            return largestSize;
        }

        // 如果没有任何合适的，返回当前的预览大小
        Camera.Size defaultPreview = parameters.getPreviewSize();
        if (defaultPreview == null) {
            throw new IllegalStateException("参数中没有可预览大小!");
        }
        Point defaultSize = new Point(defaultPreview.width, defaultPreview.height);
        Log.i(TAG, "设备返回未支持的预览大小；使用默认: " + defaultSize);
        return defaultSize;
    }

    /**
     * Like {@link #getFramingRect} but coordinates are in terms of the preview frame,
     * not UI / screen.
     * @return {@link Rect} expressing barcode scan area in terms of the preview size
     * 这是计算出的只取取景框中的帧数据
     */
    public synchronized Rect getFramingRectInPreview() {
        if (framingRectInPreview == null) {
            Rect framingRect = getFramingRect();
            if (framingRect == null) {
                return null;
            }
            Rect rect = new Rect(framingRect);
            if (cameraResolution == null || screenResolution == null) {
                // Called early, before init even finished
                return null;
            }
            rect.left = rect.left * cameraResolution.x / screenResolution.x;
            rect.right = rect.right * cameraResolution.x / screenResolution.x;
            rect.top = rect.top * cameraResolution.y / screenResolution.y;
            rect.bottom = rect.bottom * cameraResolution.y / screenResolution.y;
            framingRectInPreview = rect;
        }
        return framingRectInPreview;
    }

    /**
     * Calculates the framing rect which the UI should draw to show the user where to place the
     * barcode. This target helps with alignment as well as forces the user to hold the device
     * far enough away to ensure the image will be in focus.
     *取景框大小（通过绘制取景框外四个矩形形成）
     * @return The rectangle to draw on screen in window coordinates.
     */
    public synchronized Rect getFramingRect() {
        if (framingRect == null) {
            if (mCamera == null) {
                return null;
            }
            if (screenResolution == null) {
                // Called early, before init even finished
                return null;
            }

           /* int width = findDesiredDimensionInRange(screenResolution.x, MIN_FRAME_WIDTH, MAX_FRAME_WIDTH);
            int height = findDesiredDimensionInRange(screenResolution.y, MIN_FRAME_HEIGHT, MAX_FRAME_HEIGHT);

            int leftOffset = (screenResolution.x - width) / 2;
            int topOffset = (screenResolution.y - height) / 2;
            framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);*/
            int leftOffset = (screenResolution.x ) / 2;
            int topOffset = (screenResolution.y ) / 2;
            framingRect = new Rect(leftOffset-200, topOffset-200, leftOffset + 200, topOffset + 200);
            Log.d(TAG, "Calculated framing rect: " + framingRect);
        }
        return framingRect;
    }

    private static int findDesiredDimensionInRange(int resolution, int hardMin, int hardMax) {
        int dim = 5 * resolution / 8; // Target 5/8 of each dimension
        if (dim < hardMin) {
            return hardMin;
        }
        if (dim > hardMax) {
            return hardMax;
        }
        return dim;
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

    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        Rect rect = getFramingRectInPreview();
        if (rect == null) {
            return null;
        }
        // Go ahead and assume it's YUV rather than die.
        return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
                rect.width(), rect.height(), false);
    }
}
