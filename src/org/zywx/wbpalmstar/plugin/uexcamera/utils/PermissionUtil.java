package org.zywx.wbpalmstar.plugin.uexcamera.utils;

import android.hardware.Camera;
import android.os.Environment;
import android.text.TextUtils;

public class PermissionUtil {
    public static boolean checkCameraPermission() {
        boolean isCanUse = true;
        Camera mCamera = null;
        try {
            mCamera = Camera.open();
            Camera.Parameters mParameters = mCamera.getParameters(); //针对魅族手机
            mCamera.setParameters(mParameters);
        } catch (Exception e) {
            isCanUse = false;
        }

        if (mCamera != null) {
            try {
                mCamera.release();
            } catch (Exception e) {
                e.printStackTrace();
                return isCanUse;
            }
        }
        return isCanUse;
    }

    public static boolean isNeedStoragePermission(String filePath) {
        return !TextUtils.isEmpty(filePath) && filePath.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath());
    }
}
