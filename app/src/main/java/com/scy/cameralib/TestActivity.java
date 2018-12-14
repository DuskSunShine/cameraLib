package com.scy.cameralib;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.ImageView;
import com.scy.cameralib.camera.CameraFacing;
import com.scy.cameralib.camera.CameraManager;
import com.scy.cameralib.camera.OnPreviewFrameListener;
import com.scy.cameralib.camera.OnTakePhotoListener;
import com.scy.cameralib.viewfinder.RectViewfinderView;


public class TestActivity extends AppCompatActivity implements OnPreviewFrameListener, OnTakePhotoListener {

    private static final String TAG = "TestActivity";

    private CameraManager cameraManager;
    CameraSurfaceView surface;
    private SurfaceHolder holder;
    RectViewfinderView rectViewfinderView;
    ImageView image,image22;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        surface = findViewById(R.id.surface);
        rectViewfinderView = findViewById(R.id.rectViewfinder);
        image = findViewById(R.id.image);
        image22 = findViewById(R.id.image22);
        cameraManager = new CameraManager.Builder(this)
                .setFileDir("CameraLib测试")
                .setFileName("CAMERA_LIB_FILE_NAME")
                .setOpenAutoFocus(false)
                .setUseViewFinder(true)
                .setRectViewFinderSize(300,300)
                .build();

        holder = surface.getHolder();
        cameraManager.cameraOpen(holder, CameraFacing.BACK);
        rectViewfinderView.setCameraManager(cameraManager);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraManager.onResume();
        cameraManager.setOnPreviewFrameListener(this);
        cameraManager.setOnTakePhotoListener(this);

    }

    @Override
    protected void onPause() {
        cameraManager.onPause();
        super.onPause();
    }

    @Override
    public void onPreviewFrame(byte[] data,Bitmap bitmap, Camera camera) {
        image22.setImageBitmap(bitmap);
    }


    @Override
    public void onTakePhoto(byte[] data, Bitmap bitmap, Camera camera, String filePath) {
        image.setImageBitmap(bitmap);
    }

    public void take(View view) {
        cameraManager.takePhoto();
    }
}
