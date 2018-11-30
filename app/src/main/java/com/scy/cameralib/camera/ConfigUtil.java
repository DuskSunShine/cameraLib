package com.scy.cameralib.camera;

import android.graphics.Point;
import android.hardware.Camera;
import android.util.Log;
import java.util.List;

/**
 * Created by SCY on 2018/11/29 at 14:13.
 * 最佳预览尺寸最好==最佳照片尺寸
 * 不然旋转屏幕会出现预览变形的情况
 */
class ConfigUtil {

    private static final String TAG="ConfigUtil";

    private static final int MIN_PREVIEW_PIXELS = 480 * 320; // normal screen

    private static final double MAX_ASPECT_DISTORTION = 0.15;
    /**
     * 计算最佳预览尺寸
     *
     * @param parameters
     * @param screenResolution
     * @return
     */
     static Point findBestPreviewSizeValue(Camera.Parameters parameters, Point screenResolution) {

        List<Camera.Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
        if (rawSupportedSizes == null) {
            Log.w(TAG, "设备返回未支持的预览大小；使用默认");
            Camera.Size defaultSize = parameters.getPreviewSize();
            if (defaultSize == null) {
                throw new IllegalStateException("参数中没有可预览大小！");
            }
            return new Point(defaultSize.width, defaultSize.height);
        }

        if (Log.isLoggable(TAG, Log.INFO)) {
            StringBuilder previewSizesString = new StringBuilder();
            for (Camera.Size size : rawSupportedSizes) {
                previewSizesString.append(size.width).append('x').append(size.height).append(' ');
            }
            Log.i(TAG, "设备支撑的预览大小: " + previewSizesString);
        }

        double screenAspectRatio = screenResolution.x / (double) screenResolution.y;

        // Find a suitable size, with max resolution
        int maxResolution = 0;
        Camera.Size maxResPreviewSize = null;
        for (Camera.Size size : rawSupportedSizes) {
            int realWidth = size.width;
            int realHeight = size.height;
            int resolution = realWidth * realHeight;
            if (resolution < MIN_PREVIEW_PIXELS) {
                continue;
            }

            boolean isCandidatePortrait = realWidth < realHeight;
            int maybeFlippedWidth = isCandidatePortrait ? realHeight : realWidth;
            int maybeFlippedHeight = isCandidatePortrait ? realWidth : realHeight;
            double aspectRatio = maybeFlippedWidth / (double) maybeFlippedHeight;
            double distortion = Math.abs(aspectRatio - screenAspectRatio);
            if (distortion > MAX_ASPECT_DISTORTION) {
                continue;
            }

            if (maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
                Point exactPoint = new Point(realWidth, realHeight);
                Log.i(TAG, "预览大小与屏幕尺寸完全匹配: " + exactPoint);
                return exactPoint;
            }

            // Resolution is suitable; record the one with max resolution
            if (resolution > maxResolution) {
                maxResolution = resolution;
                maxResPreviewSize = size;
            }
        }

        // 如果没有精确匹配，请使用最大预览大小。由于需要额外的计算，在旧设备上这不是一个好主意。
        // 我们可能会在更新的Android 4+设备上看到这一点，因为CPU更加强大。
        if (maxResPreviewSize != null) {
            Point largestSize = new Point(maxResPreviewSize.width, maxResPreviewSize.height);
            Log.i(TAG, "使用最大预览: " + largestSize);
            return largestSize;
        }

        // 如果没有任何合适的，返回当前的预览大小
        Camera.Size defaultPreview = parameters.getPreviewSize();
        if (defaultPreview == null) {
            throw new IllegalStateException("参数中没有可预览大小!");
        }
        Point defaultSize = new Point(defaultPreview.width, defaultPreview.height);
        Log.i(TAG, "设备返回未支持的预览大小；使用默认: " + defaultSize);
        return defaultSize;
    }

    /**
     * 最佳照片尺寸
     * @param parameters
     * @param screenResolution
     * @return
     */
     static Point findBestPictureSizeValue(Camera.Parameters parameters, Point screenResolution){

        List<Camera.Size> rawSupportedSizes = parameters.getSupportedPictureSizes();
        if (rawSupportedSizes == null) {
            Log.w(TAG, "设备返回未支持的图片大小；使用默认");
            Camera.Size defaultSize = parameters.getPictureSize();
            if (defaultSize == null) {
                throw new IllegalStateException("参数中没有图片大小！");
            }
            return new Point(defaultSize.width, defaultSize.height);
        }

        if (Log.isLoggable(TAG, Log.INFO)) {
            StringBuilder previewSizesString = new StringBuilder();
            for (Camera.Size size : rawSupportedSizes) {
                previewSizesString.append(size.width).append('x').append(size.height).append(' ');
            }
            Log.i(TAG, "设备支撑的图片大小: " + previewSizesString);
        }

        double screenAspectRatio = screenResolution.x / (double) screenResolution.y;

        // Find a suitable size, with max resolution
        int maxResolution = 0;
        Camera.Size maxResPreviewSize = null;
        for (Camera.Size size : rawSupportedSizes) {
            int realWidth = size.width;
            int realHeight = size.height;
            int resolution = realWidth * realHeight;
            if (resolution < MIN_PREVIEW_PIXELS) {
                continue;
            }

            boolean isCandidatePortrait = realWidth < realHeight;
            int maybeFlippedWidth = isCandidatePortrait ? realHeight : realWidth;
            int maybeFlippedHeight = isCandidatePortrait ? realWidth : realHeight;
            double aspectRatio = maybeFlippedWidth / (double) maybeFlippedHeight;
            double distortion = Math.abs(aspectRatio - screenAspectRatio);
            if (distortion > MAX_ASPECT_DISTORTION) {
                continue;
            }

            if (maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
                Point exactPoint = new Point(realWidth, realHeight);
                Log.i(TAG, "照片大小与屏幕尺寸完全匹配: " + exactPoint);
                return exactPoint;
            }

            // Resolution is suitable; record the one with max resolution
            if (resolution > maxResolution) {
                maxResolution = resolution;
                maxResPreviewSize = size;
            }
        }

        // 如果没有精确匹配，请使用最大预览大小。由于需要额外的计算，在旧设备上这不是一个好主意。
        // 我们可能会在更新的Android 4+设备上看到这一点，因为CPU更加强大。
        if (maxResPreviewSize != null) {
            Point largestSize = new Point(maxResPreviewSize.width, maxResPreviewSize.height);
            Log.i(TAG, "使用最大图片大小: " + largestSize);
            return largestSize;
        }

        // 如果没有任何合适的，返回当前的预览大小
        Camera.Size defaultPreview = parameters.getPictureSize();
        if (defaultPreview == null) {
            throw new IllegalStateException("参数中没有可使用大小!");
        }
        Point defaultSize = new Point(defaultPreview.width, defaultPreview.height);
        Log.i(TAG, "设备返回未支持的图片大小；使用默认: " + defaultSize);
        return defaultSize;
    }
}
