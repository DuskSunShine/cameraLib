package com.scy.cameralib;

import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;

import com.scy.cameralib.camera.CameraFacing;
import com.scy.cameralib.camera.CameraManager;
import com.scy.cameralib.camera.OnPreviewFrameListener;
import com.scy.cameralib.camera.OnTakePhotoListener;


public class TestActivity extends AppCompatActivity implements OnPreviewFrameListener,OnTakePhotoListener {

    private static final String TAG = "TestActivity";

   private CameraManager cameraManager;
    CameraSurfaceView surface;
    private SurfaceHolder holder;
    private boolean hasSurface=false;//是否开始预览
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

       cameraManager= new CameraManager.Builder(this).setFileDir("哈哈哈")
                .setFileName("YYYYY").build();


        surface=findViewById(R.id.surface);
        holder = surface.getHolder();
        cameraManager.cameraOpen(holder,CameraFacing.BACK);
        cameraManager.setOnPreviewFrameListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        //cameraManager.onResume();

    }

    @Override
    protected void onPause() {
        //cameraManager.onPause();
        super.onPause();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

    }

    @Override
    public void onTakePhoto(String filePath) {

    }
}
