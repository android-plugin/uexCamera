package org.zywx.wbpalmstar.plugin.uexcamera.utils;

import android.media.ExifInterface;
import android.text.TextUtils;

import org.zywx.wbpalmstar.plugin.uexcamera.utils.log.MLog;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * File Description: 修改EXIF参数
 * <p>
 * Created by sandy with Email: sandy1108@163.com at Date: 2022/1/29.
 */
public class ExifUtil {

    private static final String TAG = "ExifUtil";

    public static ExifInterface getExifInfo(String imgFilePath) {
        ExifInterface exifInfo = null;
        try {
            exifInfo = new ExifInterface(imgFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return exifInfo;
    }

    public static ExifInterface getExifInfo(byte[] bitmapData) {
        ByteArrayInputStream bais = null;
        ExifInterface exifInfo = null;
        try {
            bais = new ByteArrayInputStream(bitmapData);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                exifInfo = new ExifInterface(bais);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bais != null) {
                try {
                    bais.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return exifInfo;
    }

    public static int getExifOrientationDegree(ExifInterface exif) {
        int degree = 0;
        if (exif != null) {
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            MLog.getIns().i("ExifInterface Orientation: " + orientation);
            if (orientation != -1) {
                switch (orientation) {
                    case ExifInterface.ORIENTATION_NORMAL:
                        degree = 0;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        degree = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        degree = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        degree = 270;
                        break;
                }
            }
        } else {
            MLog.getIns().i("getExifOrientationDegree: ExifInterface is null");
        }
        MLog.getIns().i("ExifInterface degree: " + degree);
        return degree;
    }

    public static void copyExifOrientation(ExifInterface oldExif, ExifInterface newExif) {
        try {
            String orientation = oldExif.getAttribute(ExifInterface.TAG_ORIENTATION);
            if (!TextUtils.isEmpty(orientation)) {
                newExif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation);
                newExif.saveAttributes();
            } else {
                MLog.getIns().i(TAG + "copyExifOrientation skipped. No orientation info.");
            }
        } catch (Exception e) {
            MLog.getIns().e(TAG + "copyExifOrientation", e);
        }
    }
}
