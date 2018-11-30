package com.scy.cameralib.camera;


import android.view.SurfaceHolder;

/**
 * Created by SCY on 2018/11/29 at 11:31.
 * 相机的一系列操作，比如打开/关闭相机。
 */
public interface CameraController {

    /**
     * 打开硬件相机
     * @param cameraId 相机id，单个设备一般有多个摄像头
     */
    void openCamera(int cameraId);

    /**
     * 初始化相机参数，包括预览大小，相机方向等
     */
    void initCameraParameters();


    /**
     * 参数选择好以后，设置期望配置
     */
    void setDesiredCameraParameters();

    /**
     * 判断相机是否打开
     */
    boolean isCameraOpen();


    /**
     * 打开相机驱动
     * 打开相机后，配置完成设置预览等。
     * @param holder
     */
    void openDriver(SurfaceHolder holder,int cameraId);

    /**
     *关闭相机驱动，释放camera
     */
    void closeDriver();

    /**
     * 开始预览
     */
    void startPreview();

    /**
     * 结束预览
     */
    void stopPreview();
}
