package com.scy.cameralib.camera;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceHolder;


/**
 * Created by SCY on 2018/11/29 at 13:53.
 * 对应于{@link android.app.Activity}的生命周期
 * 方便管理
 */
public interface CameraLifecycle {

    /**
     * Activity{@link android.app.Activity#onCreate(Bundle)}
     * 中调用，初始化相机和控制类{@link CameraControllerImpl}
     *
     * @param surfaceHolder
     * @param cameraId
     */
    void cameraOpen(SurfaceHolder surfaceHolder, CameraFacing cameraId);

    /**
     * Activity{@link Activity#onResume()}
     * 中调用，每次屏幕可见时，打开相机开始预览
     */
    void onResume();

    /**
     * Activity{@link Activity#onPause()}中调用。
     * 当app处于后台等情况就销毁预览及释放相机。
     */
    void onPause();

}
