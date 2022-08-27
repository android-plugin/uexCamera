package org.zywx.wbpalmstar.plugin.uexcamera.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.text.TextUtils;
import android.util.Log;

import org.zywx.wbpalmstar.plugin.uexcamera.vo.WatermarkOptionsVO;

/**
 * File Description: 为图片增加水印
 * <p>
 * Created by sandy with Email: sandy1108@163.com at Date: 2022/1/12.
 *
 * copy from https://juejin.cn/post/6960579316191068197
 */
public class ImageWatermarkUtil {

    private static final String TAG = "ImageWatermarkUtil";

    public static Bitmap handleWatermark(Context context, byte[] data, ExifInterface originExif, WatermarkOptionsVO watermarkOptionsVO) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap sourceBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
        return ImageWatermarkUtil.handleWatermark(context, sourceBitmap, originExif, watermarkOptionsVO);
    }

    public static Bitmap handleWatermark(Context context, Bitmap sourceBitmap, ExifInterface originExif, WatermarkOptionsVO watermarkOptionsVO) {
        int degree = ExifUtil.getExifOrientationDegree(originExif);
        Bitmap result = null;
        if (watermarkOptionsVO != null) {
            if (WatermarkOptionsVO.POSITION_LEFT_TOP.equals(watermarkOptionsVO.getPosition())) {
                result = ImageWatermarkUtil.drawTextToLeftTop(context,
                        sourceBitmap, watermarkOptionsVO.getMarkText(),
                        watermarkOptionsVO.getSize(), Color.parseColor(watermarkOptionsVO.getColor()),
                        watermarkOptionsVO.getPaddingX(), watermarkOptionsVO.getPaddingY(), degree);
            } else if (WatermarkOptionsVO.POSITION_RIGHT_TOP.equals(watermarkOptionsVO.getPosition())) {
                result = ImageWatermarkUtil.drawTextToRightTop(context,
                        sourceBitmap, watermarkOptionsVO.getMarkText(),
                        watermarkOptionsVO.getSize(), Color.parseColor(watermarkOptionsVO.getColor()),
                        watermarkOptionsVO.getPaddingX(), watermarkOptionsVO.getPaddingY(), degree);
            } else if (WatermarkOptionsVO.POSITION_LEFT_BOTTOM.equals(watermarkOptionsVO.getPosition())) {
                result = ImageWatermarkUtil.drawTextToLeftBottom(context,
                        sourceBitmap, watermarkOptionsVO.getMarkText(),
                        watermarkOptionsVO.getSize(), Color.parseColor(watermarkOptionsVO.getColor()),
                        watermarkOptionsVO.getPaddingX(), watermarkOptionsVO.getPaddingY(), degree);
            } else if (WatermarkOptionsVO.POSITION_RIGHT_BOTTOM.equals(watermarkOptionsVO.getPosition())) {
                result = ImageWatermarkUtil.drawTextToRightBottom(context,
                        sourceBitmap, watermarkOptionsVO.getMarkText(),
                        watermarkOptionsVO.getSize(), Color.parseColor(watermarkOptionsVO.getColor()),
                        watermarkOptionsVO.getPaddingX(), watermarkOptionsVO.getPaddingY(), degree);
            } else {
                result = ImageWatermarkUtil.drawTextToCenter(context,
                        sourceBitmap, watermarkOptionsVO.getMarkText(),
                        watermarkOptionsVO.getSize(), Color.parseColor(watermarkOptionsVO.getColor()), degree);
            }
        }
        if (result != null && sourceBitmap != null) {
            Log.i(TAG, "watermark result is not null.");
            if (!sourceBitmap.isRecycled()) {
                sourceBitmap.recycle();
            }
        }
        return result;
    }

    /**
     * 设置水印图片在左上角
     *
     * @param context     上下文
     * @param src
     * @param watermark
     * @param paddingLeft
     * @param paddingTop
     * @return
     */
    public static Bitmap createWaterMaskLeftTop(Context context, Bitmap src, Bitmap watermark, int paddingLeft, int paddingTop) {
        return createWaterMaskBitmap(src, watermark,
                dp2px(context, paddingLeft), dp2px(context, paddingTop));
    }

    private static Bitmap createWaterMaskBitmap(Bitmap src, Bitmap watermark, int paddingLeft, int paddingTop) {
        if (src == null) {
            return null;
        }
        int width = src.getWidth();
        int height = src.getHeight();
        //创建一个bitmap
        Bitmap newb = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);// 创建一个新的和SRC长度宽度一样的位图
        //将该图片作为画布
        Canvas canvas = new Canvas(newb);
        //在画布 0，0坐标上开始绘制原始图片
        canvas.drawBitmap(src, 0, 0, null);
        //在画布上绘制水印图片
        canvas.drawBitmap(watermark, paddingLeft, paddingTop, null);
        // 保存
//        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.save();
        // 存储
        canvas.restore();
        return newb;
    }

    /**
     * 设置水印图片在右下角
     *
     * @param context       上下文
     * @param src
     * @param watermark
     * @param paddingRight
     * @param paddingBottom
     * @return
     */
    public static Bitmap createWaterMaskRightBottom(Context context, Bitmap src, Bitmap watermark, int paddingRight, int paddingBottom) {
        return createWaterMaskBitmap(src, watermark,
                src.getWidth() - watermark.getWidth() - dp2px(context, paddingRight),
                src.getHeight() - watermark.getHeight() - dp2px(context, paddingBottom));
    }

    /**
     * 设置水印图片到右上角
     *
     * @param context
     * @param src
     * @param watermark
     * @param paddingRight
     * @param paddingTop
     * @return
     */
    public static Bitmap createWaterMaskRightTop(Context context, Bitmap src, Bitmap watermark, int paddingRight, int paddingTop) {
        return createWaterMaskBitmap(src, watermark,
                src.getWidth() - watermark.getWidth() - dp2px(context, paddingRight),
                dp2px(context, paddingTop));
    }

    /**
     * 设置水印图片到左下角
     *
     * @param context
     * @param src
     * @param watermark
     * @param paddingLeft
     * @param paddingBottom
     * @return
     */
    public static Bitmap createWaterMaskLeftBottom(Context context, Bitmap src, Bitmap watermark, int paddingLeft, int paddingBottom) {
        return createWaterMaskBitmap(src, watermark, dp2px(context, paddingLeft),
                src.getHeight() - watermark.getHeight() - dp2px(context, paddingBottom));
    }

    /**
     * 设置水印图片到中间
     *
     * @param src
     * @param watermark
     * @return
     */
    public static Bitmap createWaterMaskCenter(Bitmap src, Bitmap watermark) {
        return createWaterMaskBitmap(src, watermark,
                (src.getWidth() - watermark.getWidth()) / 2,
                (src.getHeight() - watermark.getHeight()) / 2);
    }

    /**
     * 给图片添加文字到左上角
     *
     * @param context
     * @param bitmap
     * @param text
     * @return
     */
    public static Bitmap drawTextToLeftTop(Context context, Bitmap bitmap, String text, int size, int color, int paddingLeft, int paddingTop, int degree) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int parentSize = Math.min(width, height);
        int fontSize = (size / 100) * parentSize;
        paint.setColor(color);
        paint.setTextSize(fontSize);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        return drawTextToBitmap(context, bitmap, text, paint, bounds,
                dp2px(context, paddingLeft),
                dp2px(context, paddingTop) + bounds.height(), degree);
    }

    /**
     * 绘制文字到右下角
     *
     * @param context
     * @param bitmap
     * @param text
     * @param size
     * @param color
     * @return
     */
    public static Bitmap drawTextToRightBottom(Context context, Bitmap bitmap, String text, int size, int color, int paddingRight, int paddingBottom, int degree) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int parentSize = Math.min(width, height);
        int fontSize = (int)(((float)size / 100) * parentSize);
        paint.setColor(color);
        paint.setTextSize(fontSize);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        return drawTextToBitmap(context, bitmap, text, paint, bounds,
                bitmap.getWidth() - bounds.width() - dp2px(context, paddingRight),
                bitmap.getHeight() - bounds.height() - dp2px(context, paddingBottom), degree);
    }

    /**
     * 绘制文字到右上方
     *
     * @param context
     * @param bitmap
     * @param text
     * @param size
     * @param color
     * @param paddingRight
     * @param paddingTop
     * @return
     */
    public static Bitmap drawTextToRightTop(Context context, Bitmap bitmap, String text, int size, int color, int paddingRight, int paddingTop, int degree) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int parentSize = Math.min(width, height);
        int fontSize = (size / 100) * parentSize;
        paint.setColor(color);
        paint.setTextSize(fontSize);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        return drawTextToBitmap(context, bitmap, text, paint, bounds,
                bitmap.getWidth() - bounds.width() - dp2px(context, paddingRight),
                dp2px(context, paddingTop) + bounds.height(), degree);
    }

    /**
     * 绘制文字到左下方
     *
     * @param context
     * @param bitmap
     * @param text
     * @param size
     * @param color
     * @param paddingLeft
     * @param paddingBottom
     * @return
     */
    public static Bitmap drawTextToLeftBottom(Context context, Bitmap bitmap, String text, int size, int color, int paddingLeft, int paddingBottom, int degree) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int parentSize = Math.min(width, height);
        int fontSize = (size / 100) * parentSize;
        paint.setColor(color);
        paint.setTextSize(fontSize);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        return drawTextToBitmap(context, bitmap, text, paint, bounds,
                dp2px(context, paddingLeft),
                bitmap.getHeight() - dp2px(context, paddingBottom), degree);
    }

    /**
     * 绘制文字到中间
     *
     * @param context
     * @param bitmap
     * @param text
     * @param size
     * @param color
     * @return
     */
    public static Bitmap drawTextToCenter(Context context, Bitmap bitmap, String text, int size, int color, int degree) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int parentSize = Math.min(width, height);
        int fontSize = (size / 100) * parentSize;
        paint.setColor(color);
        paint.setTextSize(fontSize);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        return drawTextToBitmap(context, bitmap, text, paint, bounds,
                (bitmap.getWidth() - bounds.width()) / 2,
                (bitmap.getHeight() + bounds.height()) / 2, degree);
    }

    //图片上绘制文字
    private static Bitmap drawTextToBitmap(Context context, Bitmap bitmap, String text, Paint paint, Rect bounds, int paddingLeft, int paddingTop, int degree) {
        android.graphics.Bitmap.Config bitmapConfig = bitmap.getConfig();

        paint.setDither(true); // 获取跟清晰的图像采样
        paint.setFilterBitmap(true);// 过滤一些
        if (bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
        }
        bitmap = bitmap.copy(bitmapConfig, true);
        Canvas canvas = new Canvas(bitmap);
        canvas.rotate(-degree, paddingLeft, paddingTop);
        if (!TextUtils.isEmpty(text) && text.contains("<br/>")) {
            int fontSize = (int)paint.getTextSize();
            int newPaddingTop = paddingTop;
            String[] textArr = text.split("<br/>");
            for (String textPart : textArr) {
                canvas.drawText(textPart, paddingLeft, newPaddingTop, paint);
                newPaddingTop += fontSize;
            }
        } else if (!TextUtils.isEmpty(text) && text.contains("\n")) {
            int fontSize = (int)paint.getTextSize();
            int newPaddingTop = paddingTop;
            String[] textArr = text.split("\n");
            for (String textPart : textArr) {
                canvas.drawText(textPart, paddingLeft, newPaddingTop, paint);
                newPaddingTop += fontSize;
            }
        } else {
            canvas.drawText(text, paddingLeft, paddingTop, paint);
        }
        canvas.rotate(degree, paddingLeft, paddingTop);
        return bitmap;
    }

    /**
     * 缩放图片
     *
     * @param src
     * @param w
     * @param h
     * @return
     */
    public static Bitmap scaleWithWH(Bitmap src, double w, double h) {
        if (w == 0 || h == 0 || src == null) {
            return src;
        } else {
            // 记录src的宽高
            int width = src.getWidth();
            int height = src.getHeight();
            // 创建一个matrix容器
            Matrix matrix = new Matrix();
            // 计算缩放比例
            float scaleWidth = (float) (w / width);
            float scaleHeight = (float) (h / height);
            // 开始缩放
            matrix.postScale(scaleWidth, scaleHeight);
            // 创建缩放后的图片
            return Bitmap.createBitmap(src, 0, 0, width, height, matrix, true);
        }
    }

    /**
     * dip转pix
     *
     * @param context
     * @param dp
     * @return
     */
    public static int dp2px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }
}
