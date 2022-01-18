package org.zywx.wbpalmstar.plugin.uexcamera.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;

import org.zywx.wbpalmstar.base.BDebug;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;

public class BitmapUtil {

	private static final String TAG = "BitmapUtil";

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

	private static int calculateInSampleSizeWithWhile(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and
			// keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}
		return inSampleSize;
	}

	/**
	 * 压缩图片到目标大小以下
	 *
	 * @param file
	 * @param targetSize
	 */
	public static void compressBmpFileToTargetSize(File file, long targetSize) {
		BDebug.i(TAG, String.format(Locale.US, "compressBmpFileToTargetSize start file.length():%d", file.length()));
		if (file.length() > targetSize) {
			// 每次宽高各缩小一半
			int ratio = 2;
			// 获取图片原始宽高
			BitmapFactory.Options options = new BitmapFactory.Options();
			Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
			int targetWidth = options.outWidth / ratio;
			int targetHeight = options.outHeight / ratio;

			// 压缩图片到对应尺寸
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int quality = 100;
			Bitmap result = generateScaledBmp(bitmap, targetWidth, targetHeight, baos, quality);

			// 计数保护，防止次数太多太耗时。
			int count = 0;
			while (baos.size() > targetSize && count <= 10) {
				targetWidth /= ratio;
				targetHeight /= ratio;
				count++;

				// 重置，不然会累加
				baos.reset();
				result = generateScaledBmp(result, targetWidth, targetHeight, baos, quality);
			}
			try {
				FileOutputStream fos = new FileOutputStream(file);
				fos.write(baos.toByteArray());
				fos.flush();
				fos.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		BDebug.i(TAG, String.format(Locale.US, "compressBmpFileToTargetSize end file.length():%d", file.length()));
	}

	/**
	 * 压缩图片到目标大小以下
	 *
	 * @param bitmapData
	 * @param targetSize
	 */
	public static Bitmap compressBmpFileToTargetSize(byte[] bitmapData, long targetSize) {
		BDebug.i(TAG, String.format(Locale.US, "compressBmpFileToTargetSize start file.length():%d", bitmapData.length));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Bitmap finalResult = null;
		if (bitmapData.length > targetSize) {
			// 每次宽高各缩小一半
			int ratio = 2;
			// 获取图片原始宽高
			BitmapFactory.Options options = new BitmapFactory.Options();
			Bitmap sourceBitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length, options);
			int targetWidth = options.outWidth / ratio;
			int targetHeight = options.outHeight / ratio;

			// 压缩图片到对应尺寸
			int quality = 100;
			Bitmap result = generateScaledBmp(sourceBitmap, targetWidth, targetHeight, baos, quality);

			// 计数保护，防止次数太多太耗时。
			int count = 0;
			while (baos.size() > targetSize && count <= 10) {
				targetWidth /= ratio;
				targetHeight /= ratio;
				count++;
				// 重置，不然会累加
				baos.reset();
				result = generateScaledBmp(result, targetWidth, targetHeight, baos, quality);
			}
			finalResult = result;
		}
		BDebug.i(TAG, String.format(Locale.US, "compressBmpFileToTargetSize end baos.size():%d", baos.size()));
		return finalResult;
	}

	/**
	 * 图片缩小一半
	 *
	 * @param srcBmp
	 * @param targetWidth
	 * @param targetHeight
	 * @param baos
	 * @param quality
	 * @return
	 */
	private static Bitmap generateScaledBmp(Bitmap srcBmp, int targetWidth, int targetHeight, ByteArrayOutputStream baos, int quality) {
		Bitmap result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(result);
		Rect rect = new Rect(0, 0, result.getWidth(), result.getHeight());
		canvas.drawBitmap(srcBmp, null, rect, null);
		if (!srcBmp.isRecycled()) {
			srcBmp.recycle();
		}
		result.compress(Bitmap.CompressFormat.JPEG, quality, baos);
		return result;
	}

	/**
	 * 旋转Bitmap
	 * 
	 * Rotates the bitmap by the specified degree. If a new bitmap is created,
	 * the original bitmap is recycled.
	 * 
	 * @param bitmap
	 * @param degree
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

	/**
	 * @deprecated 已废弃方法
	 */
	private static Bitmap rotateImage(Bitmap bitmap, int degree) {
		Matrix m = new Matrix();
		m.setRotate(degree, bitmap.getWidth() * 0.5f, bitmap.getHeight() * 0.5f);
		try {
			Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
			// If the rotated bitmap is the original bitmap, then it
			// should not be recycled.
			if (rotated != bitmap)
				bitmap.recycle();
			return rotated;
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return bitmap;
	}

}
