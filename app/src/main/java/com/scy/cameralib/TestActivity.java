package com.scy.cameralib;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;

import com.scy.cameralib.camera.CameraFacing;
import com.scy.cameralib.camera.CameraManager;

public class TestActivity extends AppCompatActivity {

    private static final String TAG = "TestActivity";

    private CameraManager cameraManager;
    CameraSurfaceView surface;
    private SurfaceHolder holder;
    private boolean hasSurface=false;//是否开始预览
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        cameraManager = CameraManager.init(this);
        surface=findViewById(R.id.surface);
        holder = surface.getHolder();
        cameraManager.onCreate(holder,CameraFacing.BACK);


    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraManager.onResume();

    }

    @Override
    protected void onPause() {
        cameraManager.onPause();
        super.onPause();
    }

}
