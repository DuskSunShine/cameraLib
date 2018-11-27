package com.scy.cameralib;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;

import java.io.File;

/**Save photos and add multimedia content to the system library
 * More than 6.0 requires dynamic permissions
 * <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
 * Created by SCY on 2017/7/21 16:30.
 */

public class MediaScanner {

    private MediaScannerConnection mConn = null;
    private ScannerClient mClient = null;
    private File mFile = null;
    private String mMimeType = null;
    public MediaScanner(Context context) {
        if (mClient == null) {
            mClient = new ScannerClient();
        }
        if (mConn == null) {
            mConn = new MediaScannerConnection(context, mClient);
        }
    }

    private class ScannerClient implements MediaScannerConnection.MediaScannerConnectionClient {

        public void onMediaScannerConnected() {

            if (mFile == null) {
                return;
            }
            scan(mFile, mMimeType);
        }

        public void onScanCompleted(String path, Uri uri) {
            mConn.disconnect();
        }

        private void scan(File file, String type) {
            if (file.isFile()) {
                mConn.scanFile(file.getAbsolutePath(), null);//Must be null, otherwise the file will not be updated and will only be final
                return;
            }
            File[] files = file.listFiles();
            if (files == null) {
                return;
            }
            for (File f : file.listFiles()) {
                scan(f, type);
            }
        }
    }

    public void scanFile(File file, String mimeType) {
        mFile = file;
        mMimeType = mimeType;
        mConn.connect();
    }
}
