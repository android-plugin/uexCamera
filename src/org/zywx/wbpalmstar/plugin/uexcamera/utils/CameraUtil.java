package org.zywx.wbpalmstar.plugin.uexcamera.utils;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.SurfaceView;
import android.view.View;

import org.zywx.wbpalmstar.plugin.uexcamera.utils.log.MLog;

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
}
