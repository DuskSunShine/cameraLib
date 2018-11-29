package com.scy.cameralib;


import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import java.io.ByteArrayOutputStream;

public  class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private static final String TAG="CameraActivity";
    protected CameraSurfaceView surfaceView;

    protected CameraManager cameraManager;

    private SurfaceHolder surfaceHolder;

    private boolean hasSurface=false;//是否开始预览
    private Camera camera;
    ImageView image;
    ImageView image2;
    RectViewfinderView maskView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        surfaceView = findViewById(R.id.surface);
        maskView = findViewById(R.id.maskView);
        image = findViewById(R.id.image);
        image2 = findViewById(R.id.image2);
        cameraManager=CameraManager.init();
        surfaceHolder = surfaceView.getHolder();
        maskView.setCameraManager(cameraManager);
        surfaceView.setCameraManager(cameraManager);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setRequestedOrientation(getCurrentOrientation());
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            if (cameraManager.isOpen()) {
                Log.w(TAG, "已经打开摄像头");
                return;
            }
            cameraManager.openCamera(this,surfaceHolder,CameraManager.BACK);
            camera = cameraManager.getCamera();
            cameraManager.startPreview();
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this);
        }
        cameraManager.setOnPreviewFrameListener(new CameraManager.OnPreviewFrameListener() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {

                PlanarYUVLuminanceSource source = cameraManager
                        .buildLuminanceSource(data, cameraManager.getCameraResolution().x, cameraManager.getCameraResolution().y);
                bundleThumbnail(source);
                //这里byte数据即是实时获取的帧数据 只要相机正在预览就会一直回调此方法
                //需要注意的是 这里的byte数据不能够直接使用 需要转换下格式
                /*Bitmap bmp = null;
                try {
                    YuvImage image = new YuvImage(data, ImageFormat.NV21, 480, 640, null);
                    if (image != null) {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        image.compressToJpeg(new Rect(0, 0, 480, 640), 100, stream);
                        bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                        stream.close();
                        CameraActivity.this.image.setImageBitmap(bmp);
                    }
                } catch (Exception ex) {
                    Log.e("Sys", "Error:" + ex.getMessage());
                }*/
            }
        });
        image2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"click");
                surfaceView.takePicture();
            }
        });
        surfaceView.setOnTakePhotoListener(new CameraSurfaceView.OnTakePhotoListener() {
            @Override
            public void onTakePhoto(String path) {
                Log.i(TAG,path);
            }
        });

        maskView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    private  void bundleThumbnail(PlanarYUVLuminanceSource source) {
        int[] pixels = source.renderThumbnail();
        int width = source.getThumbnailWidth();
        int height = source.getThumbnailHeight();
        Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
        Matrix matrix = new Matrix();
        matrix.preRotate(90);
        bitmap=Bitmap.createBitmap(bitmap,0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);

        // Mutable copy:
        //barcode = barcode.copy(Bitmap.Config.ARGB_8888, true);
        CameraActivity.this.image.setImageBitmap(bitmap);
    }
    @Override
    protected void onPause() {
        cameraManager.stopPreview();
        cameraManager.closeCamera();
        if (!hasSurface) {
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            if (cameraManager.isOpen()) {
                Log.w(TAG, "已经打开摄像头");
                return;
            }
            cameraManager.openCamera(this,surfaceHolder,CameraManager.BACK);
            camera = cameraManager.getCamera();
            cameraManager.startPreview();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    private int getCurrentOrientation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            switch (rotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_90:
                    return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                default:
                    return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            }
        } else {
            switch (rotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_270:
                    return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                default:
                    return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            }
        }
    }
}
