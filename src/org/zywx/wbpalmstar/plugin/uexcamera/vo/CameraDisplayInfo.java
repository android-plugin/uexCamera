package org.zywx.wbpalmstar.plugin.uexcamera.vo;

import org.zywx.wbpalmstar.plugin.uexcamera.utils.CameraUtil;

/**
 * File Description: 相机信息
 * <p>
 * Created by sandy with Email: sandy1108@163.com at Date: 2023/5/13.
 */
public class CameraDisplayInfo {
    public CameraUtil.Size previewSize = new CameraUtil.Size();
    public CameraUtil.Size pictureSize = new CameraUtil.Size();

    // 下面两个方法用来分别计算两个size的宽高比
    public double getPreviewRatio() {
        return (double)previewSize.width / (double)previewSize.height;
    }

    public double getPictureRatio() {
        return (double)pictureSize.width / (double)pictureSize.height;
    }
}
