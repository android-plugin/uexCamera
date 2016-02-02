package org.zywx.wbpalmstar.plugin.uexcamera;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;
import org.zywx.wbpalmstar.engine.universalex.EUExUtil;
import org.zywx.wbpalmstar.plugin.uexcamera.ViewCamera.CallbackCameraViewClose;
import org.zywx.wbpalmstar.plugin.uexcamera.ViewCamera.CameraView;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class EUExCamera extends EUExBase implements CallbackCameraViewClose {

	public static final String TAG = "EUExCamera";
	public static final String function = "uexCamera.cbOpen";
	public static final String function1 = "uexCamera.cbOpenInternal";
	private static final String FUNC_OPEN_VIEW_CAMERA_CALLBACK = "uexCamera.cbOpenViewCamera";
	private static final String FUNC_CHANGE_FLASHMODE_CALLBACK = "uexCamera.cbChangeFlashMode";// 改变闪关灯模式的回调
	private static final String FUNC_CHANGE_CAMERA_POSITION_CALLBACK = "uexCamera.cbChangeCameraPosition";// 改变摄像头位置的回调

	public static String filePath = "";
	private String location = "";

	private File m_tempPath;
	private boolean mWillCompress;
	private int mQuality;
	private View view;// 自定义相机View
	private CameraView mCameraView;// 自定义相机View实例

	public EUExCamera(Context context, EBrowserView inParent) {
		super(context, inParent);
		filePath = mBrwView.getWidgetPath() + "uexViewCameraPhotos";
		Log.i("ttest", "filePath--->" + filePath);
	}

	/**
	 * 打开系统相机
	 * 
	 * @param parm
	 */
	public void open(String[] parm) {
		mWillCompress = false;
		mQuality = -1;
		if (parm.length == 1) {
			int value = 1;
			try {
				value = Integer.parseInt(parm[0]);
			} catch (Exception e) {
				e.printStackTrace();
			}
			mWillCompress = value != 0 ? false : true;
		} else if (parm.length == 2) {
			int value = 1;
			try {
				value = Integer.parseInt(parm[0]);
			} catch (Exception e) {
				e.printStackTrace();
			}
			mWillCompress = value != 0 ? false : true;
			if (mWillCompress) {
				int quality = -1;
				try {
					quality = Integer.parseInt(parm[1]);
				} catch (Exception e) {
					e.printStackTrace();
				}
				mQuality = quality;
			}
		}
		MemoryInfo outInfo = new MemoryInfo();
		ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		activityManager.getMemoryInfo(outInfo);
		if (outInfo.lowMemory /* || outInfo.availMem < 31000000 */) {
			Toast.makeText(mContext, "内存不足,无法打开相机", Toast.LENGTH_LONG).show();
			Runtime.getRuntime().gc();
			return;
		}
		if (BUtility.sdCardIsWork()) {
			if (!mWillCompress) {
				String path = mBrwView.getCurrentWidget().getWidgetPath() + getName();
				m_tempPath = new File(path);
			} else {
				m_tempPath = new File(BUtility.getSdCardRootPath() + "demo.jpg");
			}
			if (!m_tempPath.exists()) {
				try {
					m_tempPath.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			checkPath();
			if (Build.VERSION.SDK_INT >= 14) {
				// setProcessForeground();
			}
			Intent camaIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			Uri uri = Uri.fromFile(m_tempPath);
			camaIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
			camaIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
			startActivityForResult(camaIntent, 66);
		} else {
			Toast.makeText(mContext, ResoureFinder.getInstance().getString(mContext, "error_sdcard_is_not_available"),
					Toast.LENGTH_SHORT).show();
			errorCallback(0, EUExCallback.F_E_UEXCAMERA_OPEN, "Storage error");
			return;
		}
	}

	/**
	 * 打开自定义相机
	 * 
	 * @param parm
	 */
	public void openInternal(String[] parm) {

		mWillCompress = false;
		mQuality = -1;
		if (parm.length == 1) {
			int value = 1;
			try {
				value = Integer.parseInt(parm[0]);
			} catch (Exception e) {
				e.printStackTrace();
			}
			mWillCompress = value != 0 ? false : true;
		} else if (parm.length == 2) {
			int value = 1;
			try {
				value = Integer.parseInt(parm[0]);
			} catch (Exception e) {
				e.printStackTrace();
			}
			mWillCompress = value != 0 ? false : true;
			if (mWillCompress) {
				int quality = -1;
				try {
					quality = Integer.parseInt(parm[1]);
				} catch (Exception e) {
					e.printStackTrace();
				}
				mQuality = quality;
			}
		}
		MemoryInfo outInfo = new MemoryInfo();
		ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		activityManager.getMemoryInfo(outInfo);
		if (outInfo.lowMemory /* || outInfo.availMem < 31000000 */) {
			Toast.makeText(mContext, "内存不足,无法打开相机", Toast.LENGTH_LONG).show();
			Runtime.getRuntime().gc();
			return;
		}
		if (BUtility.sdCardIsWork()) {
			if (!mWillCompress) {
				String path = mBrwView.getCurrentWidget().getWidgetPath() + getName();
				m_tempPath = new File(path);
			} else {
				m_tempPath = new File(BUtility.getSdCardRootPath() + "demo.jpg");
			}
			if (!m_tempPath.exists()) {
				try {
					m_tempPath.createNewFile();

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			checkPath();
			if (Build.VERSION.SDK_INT >= 14) {
				// setProcessForeground();
			}
			Intent camaIntent = new Intent();
			camaIntent.setClass(mContext, CustomCamera.class);
			camaIntent.putExtra("photoPath", m_tempPath.getAbsolutePath());
			camaIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
			startActivityForResult(camaIntent, 67);
		} else {
			Toast.makeText(mContext, ResoureFinder.getInstance().getString(mContext, "error_sdcard_is_not_available"),
					Toast.LENGTH_SHORT).show();
			errorCallback(0, EUExCallback.F_E_UEXCAMERA_OPEN, "Storage error");
			return;
		}

	}

	/**
	 * 打开自定义View相机
	 * 
	 * @param parm
	 */
	public void openViewCamera(String[] parm) {
		// Toast.makeText(mContext, "openViewCamera",
		// Toast.LENGTH_SHORT).show();
		if (parm.length < 6) {
			return;
		}
		String inX = parm[0];
		String inY = parm[1];
		String inW = parm[2];
		String inH = parm[3];
		location = parm[4];
		// TODO 新字段 图片质量
		int quality = -1;// 初始化为-1
		try {
			quality = Integer.valueOf(parm[5]);
			// 对quality进行容错处理
			if (quality < 0) {
				quality = 0;
			} else if (quality > 100) {
				quality = 100;
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		Log.i(TAG, "location" + location);
		Log.i("quality", "quality openViewCamera---->" + quality);

		int x = 0;
		int y = 0;
		int w = 0;
		int h = 0;
		try {
			x = Integer.parseInt(inX);
			y = Integer.parseInt(inY);
			w = Integer.parseInt(inW);
			h = Integer.parseInt(inH);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		// TODO 2016/1/19 增加超出分辨率判断
		/** 获得屏幕分辨率 **/
		Display display = ((Activity) mContext).getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int screenWidth = size.x;
		int screenHeight = size.y;

		if ((x + w) > screenWidth) {// 如果宽度超出屏幕宽度
			w = screenWidth - x;// 限制最大为屏幕宽度
		}
		if ((y + h) > screenHeight) {// 如果高度超出屏幕高度
			h = screenHeight - y;// 限制最大为屏幕高度
		}

		if (null == view) {
			// // Dynamic get resources ID, does not allow use R
			// int myViewID =
			// EUExUtil.getResLayoutID("plugin_uex_demo_test_view");
			// if (myViewID <= 0) {
			// Toast.makeText(mContext, "找不到名为:my_uex_test_view的layout文件!",
			// Toast.LENGTH_LONG).show();
			// return;
			// }
			view = View.inflate(mContext, EUExUtil.getResLayoutID("plugin_camera_view_camera"), null);// 用view引入布局文件
			mCameraView = (CameraView) view;// 将View强转为CameraView，获得CameraView的实例
			mCameraView.setmEuExCamera(this);// 设置EUExCamera的实例
			mCameraView.setCallbackCameraViewClose(this);// 注册callback，将当前类传入
			mCameraView.setLocationText(location);// 调用方法写入地址
			if (quality != -1) {// 如果quality格式正确
				mCameraView.setQuality(quality);
			}
			RelativeLayout.LayoutParams lparm = new RelativeLayout.LayoutParams(w, h);
			lparm.leftMargin = x;
			lparm.topMargin = y;
			addViewToCurrentWindow(mCameraView, lparm);
		}
	}

	/**
	 * 移除自定义相机
	 * 
	 * @param parm
	 */
	public void removeViewCameraFromWindow(String[] parm) {
		if (null != view) {
			removeViewFromCurrentWindow(view);
			view = null;
		}
		// Toast.makeText(mContext, "removeViewCameraFromWindow",
		// Toast.LENGTH_SHORT).show();
	}

	/**
	 * 更改闪光灯模式,只允许输入0、1、2三个数字,0代表自动，1代表开启，2代表关闭,默认为关闭
	 * 
	 * @param parm
	 */
	public void changeFlashMode(String[] parm) {
		String flashMode = parm[0];
		if (flashMode.equals("0") || flashMode.equals("1") || flashMode.equals("2")) {
			mCameraView.setFlashMode(Integer.valueOf(flashMode));
			jsCallback(FUNC_CHANGE_FLASHMODE_CALLBACK, 0, EUExCallback.F_C_TEXT, flashMode);
		} else {
			jsCallback(FUNC_CHANGE_FLASHMODE_CALLBACK, 0, EUExCallback.F_C_TEXT, "-1");
		}
	}

	/**
	 * 设置前后摄像头,只允许输入0、1两个数字,0代表前置，1代表后置,默认为后置
	 * 
	 * @param parm
	 */
	public void changeCameraPosition(String[] parm) {
		String cameraPosition = parm[0];
		if (view == null) {
			return;
		}
		if (cameraPosition.equals("0")) {
			CameraView.cameraPosition = 1;
			((Activity) mContext).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mCameraView.overturnCamera();
				}
			});
			jsCallback(FUNC_CHANGE_CAMERA_POSITION_CALLBACK, 0, EUExCallback.F_C_TEXT, cameraPosition);
		} else if (cameraPosition.equals("1")) {
			CameraView.cameraPosition = 0;
			((Activity) mContext).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mCameraView.overturnCamera();
				}
			});
			jsCallback(FUNC_CHANGE_CAMERA_POSITION_CALLBACK, 0, EUExCallback.F_C_TEXT, cameraPosition);
		} else {
			jsCallback(FUNC_CHANGE_CAMERA_POSITION_CALLBACK, 0, EUExCallback.F_C_TEXT, "-1");
		}

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (Build.VERSION.SDK_INT >= 14) {
			// setProcessBackground();
		}
		String finalPath = "";
		ExifInterface exif = null;
		int degree = 0;
		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == 66) {
				try {
					if (null != m_tempPath) {
						finalPath = m_tempPath.getAbsolutePath();
					} else if (null != data) {
						Uri content = data.getData();
						if (null != content) {
							String realPath = null;
							String url = content.toString();
							if (URLUtil.isFileUrl(url)) {
								realPath = url.replace("file://", "");
							} else if (URLUtil.isContentUrl(url)) {
								Activity activity = (Activity) mContext;
								@SuppressWarnings("deprecation")
								Cursor c = activity.managedQuery(content, null, null, null, null);
								boolean isExist = c.moveToFirst();
								if (isExist) {
									realPath = c.getString(c.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
								}
								c.close();
							}
							if (null != realPath) {
								finalPath = realPath;
							}
						} else {
							Bundle bundle = data.getExtras();
							if (null != bundle) {
								Bitmap bitmap = (Bitmap) bundle.get("data");
								if (null != bitmap) {
									String newfile = BUtility.getSdCardRootPath() + "demo.jpg";
									File newFile = new File(newfile);
									if (!newFile.exists()) {
										newFile.createNewFile();
									}
									BufferedOutputStream bos = new BufferedOutputStream(
											new FileOutputStream(new File(newfile)));
									bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
									bos.flush();
									bos.close();
									finalPath = newfile;
								}
							}
						}
					}
					if (null == finalPath) {
						errorCallback(0, EUExCallback.F_E_UEXCAMERA_OPEN, "Storage error or no permission");
						return;
					}
					if (URLUtil.isFileUrl(finalPath)) {
						finalPath = finalPath.replace("file://", "");
					}
					exif = new ExifInterface(finalPath);
					int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
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
					if (!mWillCompress && 0 == degree) {
						jsCallback(function, 0, EUExCallback.F_C_TEXT, finalPath);
					} else {
						String tPath = makePictrue(new File(finalPath), degree);
						if (null == tPath) {
							errorCallback(0, EUExCallback.F_E_UEXCAMERA_OPEN, "Storage error or no permission");
						} else {
							jsCallback(function, 0, EUExCallback.F_C_TEXT, tPath);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					errorCallback(0, EUExCallback.F_E_UEXCAMERA_OPEN, "Storage error or no permission");
					return;
				}
			} else if (requestCode == 67) {
				if (null != data)
					;
				finalPath = m_tempPath.getAbsolutePath();
				if (finalPath != null) {
					try {
						exif = new ExifInterface(finalPath);
					} catch (IOException e) {
						e.printStackTrace();
					}
					int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
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
					if (!mWillCompress && 0 == degree) {
						jsCallback(function1, 0, EUExCallback.F_C_TEXT, finalPath);
					} else {
						String tPath = makePictrue(new File(finalPath), degree);
						if (null == tPath) {
							errorCallback(0, EUExCallback.F_E_UEXCAMERA_OPEN, "Storage error or no permission");
						} else {
							jsCallback(function1, 0, EUExCallback.F_C_TEXT, tPath);
						}
					}
				}
			} else if (requestCode == 68) {
				Log.i(TAG, "68");
				removeViewCameraFromWindow(null);// 移除自定义View相机
				String photoPath = data.getStringExtra("photoPath");
				String jsonResult = "";
				JSONObject jsonObject = new JSONObject();
				try {
					jsonObject.put("photoPath", photoPath);
					jsonObject.put("location", location);
					jsonResult = jsonObject.toString();
				} catch (JSONException e) {
					e.printStackTrace();
				}
				jsCallback(FUNC_OPEN_VIEW_CAMERA_CALLBACK, 0, EUExCallback.F_C_TEXT, jsonResult);
			}
		} else if (resultCode == Activity.RESULT_CANCELED) {// 如果是取消标志 change by
															// waka 2016-01-28
			if (requestCode == 68) {// 如果是SecondActivity传回来的取消标记
				mCameraView.setCameraTakingPhoto(false);// 设置正在照相标记为false
			}
		}
	}

	/**
	 * 压缩拍照生成的图片
	 * 
	 * @param inPath
	 * @param degree
	 * @return
	 */
	private String makePictrue(File inPath, int degree) {

		// String newPath = BUtility.makeRealPath("wgt://"+ getName(),
		// mBrwView.getWidgetPath(), mBrwView.getWidgetType());
		String rootPath = Environment.getExternalStorageDirectory().toString() + "/widgetone/apps/"
				+ mBrwView.getRootWidget().m_appId + "/photo";
		File file = new File(rootPath);
		if (!file.exists()) {
			file.mkdirs();// 如果不存在，则创建所有的父文件夹
		}
		String newPath = Environment.getExternalStorageDirectory().toString() + "/widgetone/apps/"
				+ mBrwView.getRootWidget().m_appId + "/" + getName();
		Log.i("ttest", "newPath---->" + newPath);
		BitmapFactory.Options opts = new BitmapFactory.Options();
		Bitmap boundBitmap = null;
		LogUtils.o("rotate: " + degree);
		if (mWillCompress) {
			LogUtils.o("mWillCompress == true");
			opts.inJustDecodeBounds = true;
			String path = inPath.getAbsolutePath();
			try {
				boundBitmap = BitmapFactory.decodeStream(new FileInputStream(path), null, opts);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			LogUtils.o(path);
			if (mQuality > 0) {
				LogUtils.o("mQuality > 0");
				;
			} else {
				// 这里之前的逻辑就是如果传入了mQuality，则不再进行压缩，只进行质量的压缩，那样大小不会有很大影响，因此如果崩溃则默认不传mQuality，用这里的默认方法。
				// 后期可以添加一个接口参数，传入一个需要压缩到的目标宽高，这样就可以在前端自定义压缩比了。by yipeng
				opts.inSampleSize = Util.calculateInSampleSize(opts, 500, 500);
				opts.inPurgeable = true;
				opts.inInputShareable = true;
				opts.inTempStorage = new byte[64 * 1024];
				mQuality = 60;
			}
		} else {
			opts.inSampleSize = 2;
			opts.inPurgeable = true;
			opts.inInputShareable = true;
			opts.inTempStorage = new byte[64 * 1024];
			mQuality = 100;
		}
		opts.inSampleSize = 2;
		opts.inPurgeable = true;
		opts.inInputShareable = true;
		opts.inTempStorage = new byte[64 * 1024];
		mQuality = 60;
		opts.inJustDecodeBounds = false;
		LogUtils.o("inSampleSize == " + opts.inSampleSize);
		File newFile = new File(newPath);
		try {
			Bitmap tmpPicture = BitmapFactory.decodeStream(new FileInputStream(inPath.getAbsolutePath()), null, opts);
			if (degree > 0 && null != tmpPicture) {
				tmpPicture = Util.rotate(tmpPicture, degree);
			}
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(newFile));
			tmpPicture.compress(Bitmap.CompressFormat.JPEG, mQuality, bos);
			bos.flush();
			bos.close();
		} catch (OutOfMemoryError e) {
			Toast.makeText(mContext, "照片尺寸过大，内存溢出，\n请降低尺寸拍摄！", Toast.LENGTH_LONG).show();
			LogUtils.o(e.toString());
			return null;
		} catch (IOException e) {
			LogUtils.o(e.toString());
			return null;
		} finally {
			inPath.delete();
			if (boundBitmap != null) {
				boundBitmap.recycle();
			}
			System.gc();
		}
		mWillCompress = false;
		mQuality = -1;
		return newPath;
	}

	@SuppressLint("SimpleDateFormat")
	private String getName() {
		Date date = new Date();
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddhhmmss");
		return "photo/scan" + df.format(date) + ".jpg";
	}

	private void checkPath() {
		String widgetPath = mBrwView.getCurrentWidget().getWidgetPath() + "photo";
		File temp = new File(widgetPath);
		if (!temp.exists()) {
			temp.mkdirs();
		}
	}

	@Override
	protected boolean clean() {
		if (null != m_tempPath) {
			m_tempPath = null;
		}
		return true;
	}

	/**
	 * CameraView的关闭回调，在这里移除View
	 */
	@Override
	public void callbackClose() {
		if (null != view) {
			removeViewFromCurrentWindow(view);
			view = null;
		}
	}
}
