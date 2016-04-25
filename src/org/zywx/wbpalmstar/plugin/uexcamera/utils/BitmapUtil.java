package org.zywx.wbpalmstar.plugin.uexcamera.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

public class BitmapUtil {

	/**
	 * 计算SampleSize
	 * 
	 * @param options
	 * @param reqWidth
	 * @param reqHeight
	 * @return
	 */
	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

		// 源图片的高度和宽度
		final int height = options.outHeight;
		final int width = options.outWidth;

		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			// 计算出实际宽高和目标宽高的比率,Math.round()四舍五入
			final int heightRatio = Math.round((float) height / (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);

			// 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高
			// 一定都会大于等于目标的宽和高。
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}

		return inSampleSize;
	}

	/**
	 * 旋转Bitmap
	 * 
	 * Rotates the bitmap by the specified degree. If a new bitmap is created,
	 * the original bitmap is recycled.
	 * 
	 * @param bitmap
	 * @param degrees
	 * @return
	 */
	public static Bitmap rotate(Bitmap bitmap, int degree) {
		return rotateAndMirror(bitmap, degree, false);
	}

	/**
	 * 旋转和镜像Bitmap
	 * 
	 * Rotates and/or mirrors the bitmap. If a new bitmap is created, the
	 * original bitmap is recycled.
	 * 
	 * @param bitmap
	 * @param degree
	 * @param isMirror
	 * @return
	 * @throws OutOfMemoryError
	 */
	public static Bitmap rotateAndMirror(Bitmap bitmap, int degree, boolean isMirror) throws OutOfMemoryError {

		if ((degree != 0 || isMirror) && bitmap != null) {
			Matrix m = new Matrix();
			m.setRotate(degree, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);

			if (isMirror) {
				m.postScale(-1, 1);
				degree = (degree + 360) % 360;
				if (degree == 0 || degree == 180) {
					m.postTranslate((float) bitmap.getWidth(), 0);
				} else if (degree == 90 || degree == 270) {
					m.postTranslate((float) bitmap.getHeight(), 0);
				} else {
					throw new IllegalArgumentException("Invalid degrees=" + degree);
				}
			}

			Bitmap bitmap2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
			if (bitmap != bitmap2) {
				bitmap.recycle();
				System.gc();
				bitmap = bitmap2;
			}
		}
		return bitmap;
	}

}
