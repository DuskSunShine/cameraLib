package com.scy.cameralib.camera;

import android.view.SurfaceHolder;

/**
 * Created by SCY on 2018/11/29 at 13:53.
 * 对应于{@link com.scy.cameralib.CameraActivity}的生命周期
 * 方便管理
 */
public interface CameraLifecycle {

    void onCreate(SurfaceHolder surfaceHolder, CameraFacing cameraId);

    void onResume();

    void onPause();
}
