package com.scy.cameralib.camera;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;

import java.io.ByteArrayOutputStream;

/**
 * Created by SCY on 2018/12/14 at 11:44.
 */
public class PreviewFrameCallback implements Camera.PreviewCallback {
    private static final String TAG = PreviewFrameCallback.class.getSimpleName();
    private OnPreviewFrameListener previewFrameListener;
    private CameraController cameraController;
    private Bitmap bitmap;
    private boolean useViewFinder;
    private Rect rect;
    public PreviewFrameCallback(CameraController cameraController,boolean useViewFinder,Rect rect ,OnPreviewFrameListener onPreviewFrameListener) {
        this.previewFrameListener = onPreviewFrameListener;
        this.cameraController = cameraController;
        this.useViewFinder=useViewFinder;
        this.rect=rect;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.w(TAG,"开始预览回调,原始数据大小："+data.length);
        if (previewFrameListener != null) {
            PlanarYUVLuminanceSource source =
                    buildLuminanceSource(data, cameraController.getCameraResolution().x, cameraController.getCameraResolution().y);
            bitmap = bundleThumbnail(source);
            previewFrameListener.onPreviewFrame(data, bitmap, camera);
        } else {
            Log.w(TAG, "OnPreviewFrameListener为空," +
                    "需调用#CameraManeger中setOnPreviewFrameListener方法");
        }
    }

    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        // Rect rect = getFramingRect();
        if (useViewFinder) {
            if (rect == null) {
                return null;
            }
            //取景框宽高
            int interceptWidth = rect.width();
            int interceptHeight = rect.height();
            // Go ahead and assume it's YUV rather than die.
            //保证开始截取的像素+需要截取宽度宽<=图片大小
            if (rect.left + interceptWidth > width) {
                interceptWidth = width;
            }
            if (rect.top + interceptHeight > width) {
                interceptHeight = height;
            }
            int x = width / 2 - interceptWidth / 2;
            int y = height / 2 - interceptHeight / 2;
            return new PlanarYUVLuminanceSource(data, width, height, x, y,
                    interceptWidth, interceptHeight, false);
        } else {
            return new PlanarYUVLuminanceSource(data, width, height, 0, 0,
                    width, height, false);
        }

//            return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
//                    rect.width(), rect.height(), false);
    }

    private Bitmap bundleThumbnail(PlanarYUVLuminanceSource source) {
        int[] pixels = source.renderThumbnail();
        int width = source.getThumbnailWidth();
        int height = source.getThumbnailHeight();
        Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
        Matrix matrix = new Matrix();
        matrix.preRotate(90);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
        return bitmap;
        // Mutable copy:
        //barcode = barcode.copy(Bitmap.Config.ARGB_8888, true);
        //CameraActivity.this.image.setImageBitmap(bitmap);
    }
}
