package com.scy.cameralib.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;

import com.scy.cameralib.MediaScanner;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by SCY on 2018/11/30 at 17:18.
 * 拍照获取照片
 */
public class PhotoCallback implements Camera.PictureCallback {

    private Context context;
    private File photoFile;
    private String fileName;
    private String fileDir;
    private Rect framingRect;
    private CameraControllerImpl cameraController;
    private OnTakePhotoListener onTakePhotoListener;

    public PhotoCallback(Context context, String fileName, String fileDir, Rect framingRect, CameraControllerImpl cameraController, OnTakePhotoListener onTakePhotoListener) {
        this.context = context;
        this.fileName = fileName;
        this.fileDir = fileDir;
        this.framingRect = framingRect;
        this.cameraController = cameraController;
        this.onTakePhotoListener = onTakePhotoListener;
    }

    @Override
    public void onPictureTaken(final byte[] data, Camera camera) {
        CreateFile();
        if (onTakePhotoListener != null) {
            onTakePhotoListener.onTakePhoto(photoFile.getAbsolutePath());
        } else {
            throw new IllegalStateException("OnTakePhotoListener为空," +
                    "需调用#CameraManeger中setOnTakePhotoListener方法");
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedOutputStream bos = null;
                Bitmap bm = null;
                Bitmap rectBitmap = null;
                try {
                    BitmapFactory.Options newOpts = new BitmapFactory.Options();
                    newOpts.inPreferredConfig = Bitmap.Config.RGB_565;
                    bm = BitmapFactory.decodeByteArray(data, 0, data.length, newOpts);
                    Matrix matrix = new Matrix();
                    matrix.preRotate(90);
                    bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, false);
                    Log.i("拍照原始尺寸", bm.getWidth() + "," + bm.getHeight());
                    //旋转后rotaBitmap是960×1280.预览surfaview的大小是540×800
                    //将960×1280缩放到540×800
                    int width = framingRect.width();
                    int height = framingRect.height();
                    Point bestPreviewSize = cameraController.getPreviewOnScreen();
                    Bitmap sizeBitmap = Bitmap.createScaledBitmap(bm, bestPreviewSize.x, bestPreviewSize.y, true);
                    Log.i("拍照缩放到surface大小", sizeBitmap.getWidth() + "," + sizeBitmap.getHeight());
                    Log.i("矩形取景框区域", framingRect.toString());
                    //保证开始截取的像素+需要截取宽度宽<=图片大小
                    if (framingRect.left + width > sizeBitmap.getWidth()) {
                        width = sizeBitmap.getWidth();
                    }
                    if (framingRect.top + height > sizeBitmap.getHeight()) {
                        height = sizeBitmap.getHeight();
                    }
                    int x = sizeBitmap.getWidth() / 2 - width / 2;
                    int y = sizeBitmap.getHeight() / 2 - height / 2;
                    rectBitmap = Bitmap.createBitmap(sizeBitmap, x, y, width, height);//截取
                    Log.i("拍照最终大小", rectBitmap.getWidth() + "," + rectBitmap.getHeight());
                    bos = new BufferedOutputStream(new FileOutputStream(photoFile));
                    rectBitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (bos != null) {
                            bos.flush();
                            bos.close();
                        }
                        if (rectBitmap != null) {
                            rectBitmap.recycle();
                        }
                        new MediaScanner(context).scanFile(RootFilePath(), "image/*");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void CreateFile() {
        photoFile = new File(RootFilePath(), fileName + "_" + System.currentTimeMillis() + ".jpeg");
        if (!photoFile.exists()) {
            try {
                photoFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private File RootFilePath() {
        if (fileDir != null) {
            File rootPath = new File(Environment.getExternalStorageDirectory().getPath()
                    + File.separator + fileDir);
            if (!rootPath.exists()) {
                rootPath.mkdir();
            }
            return rootPath;
        }
        return null;
    }
}
