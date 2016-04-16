/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.zywx.wbpalmstar.plugin.uexcamera;

import java.io.Closeable;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

/**
 * Collection of utility functions used in this package.
 */
public class Util {
	private static final String TAG = "Util";
	public static final int DIRECTION_LEFT = 0;
	public static final int DIRECTION_RIGHT = 1;
	public static final int DIRECTION_UP = 2;
	public static final int DIRECTION_DOWN = 3;

	public static final String REVIEW_ACTION = "com.cooliris.media.action.REVIEW";

	private Util() {
	}

	// Rotates the bitmap by the specified degree.
	// If a new bitmap is created, the original bitmap is recycled.
	public static Bitmap rotate(Bitmap b, int degrees) {
		return rotateAndMirror(b, degrees, false);
	}

	// Rotates and/or mirrors the bitmap. If a new bitmap is created, the
	// original bitmap is recycled.
	public static Bitmap rotateAndMirror(Bitmap b, int degrees, boolean mirror) {
		if ((degrees != 0 || mirror) && b != null) {
			Matrix m = new Matrix();
			m.setRotate(degrees, (float) b.getWidth() / 2, (float) b.getHeight() / 2);
			if (mirror) {
				m.postScale(-1, 1);
				degrees = (degrees + 360) % 360;
				if (degrees == 0 || degrees == 180) {
					m.postTranslate((float) b.getWidth(), 0);
				} else if (degrees == 90 || degrees == 270) {
					m.postTranslate((float) b.getHeight(), 0);
				} else {
					throw new IllegalArgumentException("Invalid degrees=" + degrees);
				}
			}

			try {
				Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
				if (b != b2) {
					b.recycle();
					b = b2;
				}
			} catch (OutOfMemoryError ex) {
				// We have no memory to rotate. Return the original bitmap.
			}
		}
		return b;
	}

	/*
	 * Compute the sample size as a function of minSideLength and
	 * maxNumOfPixels. minSideLength is used to specify that minimal width or
	 * height of a bitmap. maxNumOfPixels is used to specify the maximal size in
	 * pixels that is tolerable in terms of memory usage.
	 * 
	 * The function returns a sample size based on the constraints. Both size
	 * and minSideLength can be passed in as IImage.UNCONSTRAINED, which
	 * indicates no care of the corresponding constraint. The functions prefers
	 * returning a sample size that generates a smaller bitmap, unless
	 * minSideLength = IImage.UNCONSTRAINED.
	 * 
	 * Also, the function rounds up the sample size to a power of 2 or multiple
	 * of 8 because BitmapFactory only honors sample size this way. For example,
	 * BitmapFactory downsamples an image by 2 even though the request is 3. So
	 * we round up the sample size to avoid OOM.
	 */
	public static int computeSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
		int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);

		int roundedSize;
		if (initialSize <= 8) {
			roundedSize = 1;
			while (roundedSize < initialSize) {
				roundedSize <<= 1;
			}
		} else {
			roundedSize = (initialSize + 7) / 8 * 8;
		}

		return roundedSize;
	}

	private static int computeInitialSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
		double w = options.outWidth;
		double h = options.outHeight;

		int lowerBound = (maxNumOfPixels == IImage.UNCONSTRAINED) ? 1
				: (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
		int upperBound = (minSideLength == IImage.UNCONSTRAINED) ? 128
				: (int) Math.min(Math.floor(w / minSideLength), Math.floor(h / minSideLength));

		if (upperBound < lowerBound) {
			// return the larger one when there is no overlapping zone.
			return lowerBound;
		}

		if ((maxNumOfPixels == IImage.UNCONSTRAINED) && (minSideLength == IImage.UNCONSTRAINED)) {
			return 1;
		} else if (minSideLength == IImage.UNCONSTRAINED) {
			return lowerBound;
		} else {
			return upperBound;
		}
	}

	/**
	 * Calculate an inSampleSize for use in a {@link BitmapFactory.Options}
	 * object when decoding bitmaps using the decode* methods from
	 * {@link BitmapFactory}. This implementation calculates the closest
	 * inSampleSize that is a power of 2 and will result in the final decoded
	 * bitmap having a width and height equal to or larger than the requested
	 * width and height.
	 * 
	 * @param options
	 *            An options object with out* params already populated (run
	 *            through a decode* method with inJustDecodeBounds==true
	 * @param reqWidth
	 *            The requested width of the resulting bitmap
	 * @param reqHeight
	 *            The requested height of the resulting bitmap
	 * 
	 * @modified 此方法从网络取得，并修改了一些处理逻辑，使其更准确，压缩比更大。
	 * @return The value to be used for inSampleSize
	 */
	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;

		LogUtils.o("reqWidth== " + reqWidth + " reqHeight== " + reqHeight);
		LogUtils.o("options.outHeight== " + options.outHeight + " options.outWidth== " + options.outWidth);

		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			// final int halfHeight = height / 2;
			// final int halfWidth = width / 2;

			inSampleSize = 2;
			// Calculate the largest inSampleSize value that is a power of 2 and
			// keeps both
			// height and width larger than the requested height and width.
			while ((height / inSampleSize) > reqHeight && (width / inSampleSize) > reqWidth)
			// 此处判断如果宽和高均比要求的高度和宽度大，则将压缩比翻倍
			{
				inSampleSize *= 2;
			}

			// This offers some additional logic in case the image has a strange
			// aspect ratio. For example, a panorama may have a much larger
			// width than height. In these cases the total pixels might still
			// end up being too large to fit comfortably in memory, so we should
			// be more aggressive with sample down the image (=larger
			// inSampleSize).

			long totalPixels = (width / inSampleSize) * (height / inSampleSize);

			// Anything more than 2x the requested pixels we'll sample down
			// further
			final long totalReqPixelsCap = reqWidth * reqHeight * 2;
			// 此处用于验证图片的总像素是否大于请求的宽和高组成的像素数的2倍，防止某些长宽比例不均匀的情况计算不准确

			while (totalPixels > totalReqPixelsCap) {
				inSampleSize *= 2;
				totalPixels /= 4;
			}
		}
		return inSampleSize;
	}

	public static <T> int indexOf(T[] array, T s) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].equals(s)) {
				return i;
			}
		}
		return -1;
	}

	public static void closeSilently(Closeable c) {
		if (c == null)
			return;
		try {
			c.close();
		} catch (Throwable t) {
			// do nothing
		}
	}

	public static Bitmap makeBitmap(byte[] jpegData, int maxNumOfPixels) {
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, options);
			if (options.mCancel || options.outWidth == -1 || options.outHeight == -1) {
				return null;
			}
			options.inSampleSize = computeSampleSize(options, IImage.UNCONSTRAINED, maxNumOfPixels);
			options.inJustDecodeBounds = false;

			options.inDither = false;
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;
			return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, options);
		} catch (OutOfMemoryError ex) {
			Log.e(TAG, "Got oom exception ", ex);
			return null;
		}
	}

	public static void Assert(boolean cond) {
		if (!cond) {
			throw new AssertionError();
		}
	}

	public static Animation slideOut(View view, int to) {
		view.setVisibility(View.INVISIBLE);
		Animation anim;
		switch (to) {
		case DIRECTION_LEFT:
			anim = new TranslateAnimation(0, -view.getWidth(), 0, 0);
			break;
		case DIRECTION_RIGHT:
			anim = new TranslateAnimation(0, view.getWidth(), 0, 0);
			break;
		case DIRECTION_UP:
			anim = new TranslateAnimation(0, 0, 0, -view.getHeight());
			break;
		case DIRECTION_DOWN:
			anim = new TranslateAnimation(0, 0, 0, view.getHeight());
			break;
		default:
			throw new IllegalArgumentException(Integer.toString(to));
		}
		anim.setDuration(500);
		view.startAnimation(anim);
		return anim;
	}

	public static Animation slideIn(View view, int from) {
		view.setVisibility(View.VISIBLE);
		Animation anim;
		switch (from) {
		case DIRECTION_LEFT:
			anim = new TranslateAnimation(-view.getWidth(), 0, 0, 0);
			break;
		case DIRECTION_RIGHT:
			anim = new TranslateAnimation(view.getWidth(), 0, 0, 0);
			break;
		case DIRECTION_UP:
			anim = new TranslateAnimation(0, 0, -view.getHeight(), 0);
			break;
		case DIRECTION_DOWN:
			anim = new TranslateAnimation(0, 0, view.getHeight(), 0);
			break;
		default:
			throw new IllegalArgumentException(Integer.toString(from));
		}
		anim.setDuration(500);
		view.startAnimation(anim);
		return anim;
	}

	public static <T> T checkNotNull(T object) {
		if (object == null)
			throw new NullPointerException();
		return object;
	}

	public static boolean equals(Object a, Object b) {
		return (a == b) || (a == null ? false : a.equals(b));
	}

	public static boolean isPowerOf2(int n) {
		return (n & -n) == n;
	}

	public static int nextPowerOf2(int n) {
		n -= 1;
		n |= n >>> 16;
		n |= n >>> 8;
		n |= n >>> 4;
		n |= n >>> 2;
		n |= n >>> 1;
		return n + 1;
	}

	public static float distance(float x, float y, float sx, float sy) {
		float dx = x - sx;
		float dy = y - sy;
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	public static int clamp(int x, int min, int max) {
		if (x > max)
			return max;
		if (x < min)
			return min;
		return x;
	}

	public static int getDisplayRotation(Activity activity) {
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		switch (rotation) {
		case Surface.ROTATION_0:
			return 0;
		case Surface.ROTATION_90:
			return 90;
		case Surface.ROTATION_180:
			return 180;
		case Surface.ROTATION_270:
			return 270;
		}
		return 0;
	}

	@SuppressLint("NewApi")
	public static void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
		// See android.hardware.Camera.setCameraDisplayOrientation for
		// documentation.
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		int degrees = getDisplayRotation(activity);
		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360; // compensate the mirror
		} else { // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		camera.setDisplayOrientation(result);
	}
	
}