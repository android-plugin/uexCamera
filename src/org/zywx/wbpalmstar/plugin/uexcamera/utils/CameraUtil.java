package org.zywx.wbpalmstar.plugin.uexcamera.utils;

import android.app.Activity;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;

import org.zywx.wbpalmstar.plugin.uexcamera.utils.log.MLog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * File Description: 用于处理相机相关的工具类
 * <p>
 * Created by sandy with Email: sandy1108@163.com at Date: 2023/3/11.
 */
public class CameraUtil {
    public static final String TAG = "CameraUtil";

    public static class Size {
        public int width;
        public int height;
    }

    public static Size getTargetSize(Activity activity) {
        return getScreenSize(activity);
    }

    /**
     * 获取surfaceView的宽高并返回
     *
     * @param surfaceView
     * @return
     */
    public static Size getTargetSize(SurfaceView surfaceView) {
        Size size = new Size();
        size.width = surfaceView.getWidth();
        size.height = surfaceView.getHeight();
        MLog.getIns().i(TAG, "size log: surfaceView width:" + size.width + ",height:" + size.height);
        return size;
    }

    private static Size getScreenSize(Activity activity) {
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        Size size = new Size();
        size.width = dm.widthPixels;
        size.height = dm.heightPixels;
        return size;
    }

    public static Size getMaxTargetSize(Activity activity, View bottomView) {
        Size size = getScreenSize(activity);
        if (bottomView != null) {
            // 计算屏幕高度与bottomView高度的差
            int bottomViewHeight = bottomView.getHeight();
            int screenHeight = size.height;
            int diffHeight = screenHeight - bottomViewHeight;
            size.height = diffHeight;
        }
        return size;
    }


//    private Camera.Size getFitParametersSize(List<Camera.Size> sizes) {
//        CameraUtil.Size targetSize = CameraUtil.getTargetSize(mSurfaceView);
//        // 因为是相机会旋转90度，因此这里会交换宽高
//        return getFitParametersSize(sizes, targetSize.height, targetSize.width, true);
//    }

    public static Camera.Size getFitParametersSize(List<Camera.Size> sizes, int targetWidth, int targetHeight, boolean isConsiderRatio) {
        double dmFormat = getFormat(targetWidth, targetHeight);
        int maxWidth = 0, maxHeight = 0;
        Camera.Size maxFitSize = null;
        // 不再优先比例，而是找最大值
        if (isConsiderRatio) {
            // 优先选取比例相近的，从比例差在0.1以内的尺寸中，挑选一个最大的尺寸，尽量保证照片清晰
            for (Camera.Size size : sizes) {
                double abs = Math.abs(dmFormat - getFormat(size.width, size.height));
                if (abs <= 0.1d) {
                    if (size.width > maxWidth && size.height > maxHeight) {
                        maxWidth = size.width;
                        maxHeight = size.height;
                        maxFitSize = size;
                        MLog.getIns().i(TAG, "size log: abs<0.1 FitSize:" + maxFitSize.width + "x" + maxFitSize.height);
                    }
                }
            }
            // 如果没有比例相近的（0.1以内的），选取abs最小的，并且宽高都大于目标宽高的
            if (maxFitSize == null) {
                double minAbs = Double.MAX_VALUE;
                for (Camera.Size size : sizes) {
                    // 打印所有的尺寸宽高
                    MLog.getIns().i(TAG, "size log: supported size width:" + size.width + ",height:" + size.height);
                    double abs = Math.abs(dmFormat - getFormat(size.width, size.height));
                    if (abs < minAbs && size.width > targetWidth && size.height > targetHeight) {
                        minAbs = abs;
                        maxWidth = size.width;
                        maxHeight = size.height;
                        maxFitSize = size;
                        MLog.getIns().i(TAG, "size log: min abs FitSize:" + maxFitSize.width + "x" + maxFitSize.height);
                    }
                }
            }
        }
        // 如果仍然没有比例相近的，或者分辨率太低，则取最大的宽高
        if (maxFitSize == null || maxFitSize.width < targetWidth || maxFitSize.height < targetHeight) {
            for (Camera.Size size : sizes) {
                if (size.width > maxWidth && size.height > maxHeight) {
                    maxWidth = size.width;
                    maxHeight = size.height;
                    maxFitSize = size;
                    MLog.getIns().i(TAG, "maxFitSize-isUseLargerImageSize:" + maxFitSize.width + "x" + maxFitSize.height);
//					isUseLargerImageSize = true;
                }
            }
        }
        if (maxFitSize == null) {
            maxFitSize = sizes.get(0);
            MLog.getIns().i(TAG, "maxFitSize-final null, use get(0):" + maxFitSize.width + "x" + maxFitSize.height);
        }
        return maxFitSize;
    }


    public static double getFormat(int formatX, int formatY) {
        double oriResult = (double) formatX / (double) formatY;
        BigDecimal bigResult = new BigDecimal(oriResult);
        double result = bigResult.setScale(2, RoundingMode.HALF_UP).doubleValue();
        return result;
    }

    public static String getAllSupportedSize(List<Camera.Size> sizes) {
        StringBuilder sb = new StringBuilder();
        for (Camera.Size size : sizes) {
            sb.append(size.width);
            sb.append("x");
            sb.append(size.height);
            sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * 处理缩放，返回当前缩放值
     *
     * @param camera
     * @param isZoomIn
     * @return
     */
    public static int handleZoom(Camera camera, boolean isZoomIn) {
        MLog.getIns().i(TAG, "handleZoom isZoomIn:" + isZoomIn);
        Camera.Parameters parameters = camera.getParameters();
        if (parameters.isZoomSupported()) { // 首先还是要判断是否支持
            int maxZoom = parameters.getMaxZoom();
            int zoom = parameters.getZoom();
            if (isZoomIn && zoom < maxZoom) {
                zoom++;
            } else if (zoom > 0) {
                zoom--;
            }
            parameters.setZoom(zoom); // 通过这个方法设置放大缩小
            camera.setParameters(parameters);
            MLog.getIns().i(TAG, "handleZoom currentZoom:" + zoom);
            return zoom;
        } else {
            MLog.getIns().w(TAG + " zoom is not supported!!!");
            return -1;
        }
    }
    public static float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }
}
