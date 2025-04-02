package org.zywx.wbpalmstar.plugin.uexcamera.utils;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;

/**
 * File Description: UI坐标到相机坐标的转换算法
 * <p>
 * Created by zyp with Email: sandy1108@163.com at Date: 2025/4/2 Wednesday.
 *
 * ref: <a href="https://blog.csdn.net/Scott_S/article/details/122499597">...</a>
 */
public class CoordinateTransformer {
    // ui坐标系转换到相机坐标系的Matrix
    private final Matrix mTransformMatrix;
    // 相机的范围RectF
    private final RectF mCamera1RectF = new RectF(-1000, -1000, 1000, 1000);
    // ui坐标范围
    private int mPreviewUIWidth, mPreviewUIHeight;

    /**
     * @isCameraFront : 相机是否前置、前置需要做一次水平翻转
     * @rotateDegree：matrix旋转角度 为相机的rotation
     * @mCamera1RectF：camera1坐标系
     * @previewRect：ui坐标系
     * @mTransformMatrix transform Matrix
     **/
    public CoordinateTransformer(boolean isCameraFront, int rotateDegree, int mPreviewUIWidth, int mPreviewUIHeight) {
        this.mPreviewUIWidth = mPreviewUIWidth;
        this.mPreviewUIHeight = mPreviewUIHeight;
        RectF previewRect = new RectF(0, 0, mPreviewUIWidth, mPreviewUIHeight);
        mTransformMatrix = previewToCameraTransform(isCameraFront, rotateDegree, previewRect);
    }

    //核心方法
    private Matrix previewToCameraTransform(boolean mirrorX, int sensorOrientation,
                                            RectF previewRect) {
        Matrix transform = new Matrix();
        //前置的话需要镜像，scaleX进行反转
        transform.setScale(mirrorX ? -1 : 1, 1);
        //ui的坐标系和相机的坐标系有夹角，需要做rotate处理
        //如后置，这里传入的是90度，ui坐标系转换为相机坐标系就需要顺时针旋转90度 即postRotate(-90)
        transform.postRotate(-sensorOrientation);
        // 先在previewRect添加旋转操作
        transform.mapRect(previewRect);
        // Map  preview coordinates to driver coordinates
        Matrix fill = new Matrix();
        fill.setRectToRect(previewRect, mCamera1RectF, Matrix.ScaleToFit.FILL);
        // fill做坐标转换映射到镜像和旋转
        transform.setConcat(fill, transform);
        // finally get transform matrix
        return transform;
    }

    /**
     * ui坐标系下的rect转换到相机坐标系的rect
     * Transform a rectangle in preview view space into a new rectangle in
     * camera view space.
     *
     * @param source the rectangle in preview view space
     * @return the rectangle in camera view space.
     */
    private RectF toCameraCoorRectF(RectF source) {
        RectF result = new RectF();
        mTransformMatrix.mapRect(result, source);
        return result;
    }

    public Camera.Area getArea(float x, float y, boolean isFocusArea) {
        if (isFocusArea) {
            return calcTapArea(x, y, mPreviewUIWidth / 6f, 1000);
        } else {
            return calcTapArea(x, y, mPreviewUIWidth / 3f, 1000);
        }
    }

    /**
     * 获取转换坐标后的对焦区域
     *
     * @param x
     * @param y
     * @param focusSize 这里简化了对焦区域的计算，默认为正方形，宽高都是focusSize
     * @return
     */
    public Rect getFocusRect(float x, float y, int focusSize, float areaMultiple) {
        return calcTapRect(x, y, focusSize * areaMultiple);
    }

    /**
     * 处理为整数值，并且限制范围
     *
     * @param rectF
     * @return
     */
    private Rect toFocusRect(RectF rectF) {
        Rect mFocusRect = new Rect();
        mFocusRect.left = clamp(Math.round(rectF.left), -1000, 1000);
        mFocusRect.top = clamp(Math.round(rectF.top), -1000, 1000);
        mFocusRect.right = clamp(Math.round(rectF.right), -1000, 1000);
        mFocusRect.bottom = clamp(Math.round(rectF.bottom), -1000, 1000);
        return mFocusRect;
    }

    private Rect calcTapRect(float currentX, float currentY, float areaSize) {
        float left = currentX - areaSize / 2f;
        float top = currentY - areaSize / 2f;
        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
        return toFocusRect(toCameraCoorRectF(rectF));
    }

    private Camera.Area calcTapArea(float currentX, float currentY, float areaSize, int weight) {
        return new Camera.Area(calcTapRect(currentX, currentY, areaSize), weight);
    }

    public int clamp(int x, int min, int max) {
        if (x > max)
            return max;
        if (x < min)
            return min;
        return x;
    }


}
