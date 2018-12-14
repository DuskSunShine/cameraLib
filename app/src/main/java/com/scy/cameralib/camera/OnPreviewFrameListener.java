package com.scy.cameralib.camera;

import android.graphics.Bitmap;
import android.hardware.Camera;

/**
 * Created by SCY on 2018/11/30 at 16:42.
 * 流的监听
 */
public interface OnPreviewFrameListener {

    void onPreviewFrame(byte[] data, Bitmap bitmap, Camera camera);
}
