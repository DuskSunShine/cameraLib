package com.scy.cameralib.camera;

import android.graphics.Bitmap;
import android.hardware.Camera;

/**
 * Created by SCY on 2018/11/30 at 16:51.
 */
public interface OnTakePhotoListener {

    void onTakePhoto(byte[] data, Bitmap bitmap, Camera camera, String filePath);
}
