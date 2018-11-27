package com.scy.cameralib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.SurfaceView;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Custom surfaceView, get pictures, save the absolute path
 * Created by SCY on 2017/7/21 11:50.
 */

public class CameraSurfaceView extends SurfaceView {

    private File picFile;//file name
    private String fileDir="CameraLib";//dir name
    private OnTakePhotoListener onTakePhotoListener;
    private CameraManager cameraManager;


    public String getFileDir() {
        return fileDir;
    }


    public void setFileDir(String fileDir) {
        this.fileDir = fileDir;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    public CameraSurfaceView(Context context) {
        this(context, null);

    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);


    }

    /**
     * <p>ChatFile save root path</p>
     *
     * @return path
     */
    private File RootFilePath() {
        if (getFileDir() != null) {
            File rootPath = new File(Environment.getExternalStorageDirectory().getPath()
                    + File.separator + getFileDir());
            if (!rootPath.exists()) {
                rootPath.mkdir();
            }
            return rootPath;
        }
        return null;
    }

    /**
     * <p>
     * If the file does not exist, create a file
     * </p>
     */
    private void CreateFile() {
        picFile = new File(RootFilePath(), System.currentTimeMillis() + ".jpeg");
        if (!picFile.exists()) {
            try {
                picFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * take photo again
     */
    public void keepTakePicture() {
        cameraManager.stopPreview();
        cameraManager.startPreview();
    }

    /**
     * Take pictures
     */
    public void takePicture() {
        if (cameraManager!=null) {
            if (cameraManager.getCamera()!=null) {
                cameraManager.getCamera().takePicture(null, null, pictureCallback);
            }
        }
    }

    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(final byte[] data, Camera Camera) {
            CreateFile();
            if (onTakePhotoListener != null) {
                onTakePhotoListener.onTakePhoto(picFile.getAbsolutePath());
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    BufferedOutputStream bos = null;
                    Bitmap bm = null;
                    try {
                        BitmapFactory.Options newOpts = new BitmapFactory.Options();
                        newOpts.inPreferredConfig = Bitmap.Config.RGB_565;
                        bm = BitmapFactory.decodeByteArray(data, 0, data.length, newOpts);
                        Matrix matrix = new Matrix();
                        matrix.preRotate(90);
                        bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, false);

                        bos = new BufferedOutputStream(new FileOutputStream(picFile));
                        bm.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (bos != null) {
                                bos.flush();
                                bos.close();
                            }
                            if (bm != null) {
                                bm.recycle();
                            }
                            new MediaScanner(getContext()).scanFile(RootFilePath(), "image/*");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    };

    public interface OnTakePhotoListener {
        void onTakePhoto(String path);
    }

    public void setOnTakePhotoListener(OnTakePhotoListener onTakePhotoListener) {
        this.onTakePhotoListener = onTakePhotoListener;
    }
}
