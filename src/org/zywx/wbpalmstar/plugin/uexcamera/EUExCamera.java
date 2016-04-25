package org.zywx.wbpalmstar.plugin.uexcamera;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;
import org.zywx.wbpalmstar.engine.universalex.EUExUtil;
import org.zywx.wbpalmstar.plugin.uexcamera.ViewCamera.CallbackCameraViewClose;
import org.zywx.wbpalmstar.plugin.uexcamera.ViewCamera.CameraView;
import org.zywx.wbpalmstar.plugin.uexcamera.utils.BitmapUtil;
import org.zywx.wbpalmstar.plugin.uexcamera.utils.FileUtil;
import org.zywx.wbpalmstar.plugin.uexcamera.utils.MemoryUtil;
import org.zywx.wbpalmstar.plugin.uexcamera.utils.MyLog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;
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

	public static final String TAG = "uexCamera";

	// 回调
	private static final String FUNC_OPEN_CALLBACK = "uexCamera.cbOpen";// 打开系统相机回调
	private static final String FUNC_OPEN_INTERNAL_CALLBACK = "uexCamera.cbOpenInternal";// 打开自定义相机回调
	private static final String FUNC_OPEN_VIEW_CAMERA_CALLBACK = "uexCamera.cbOpenViewCamera";// 打开自定义相机View回调
	private static final String FUNC_CHANGE_FLASHMODE_CALLBACK = "uexCamera.cbChangeFlashMode";// 改变闪关灯模式的回调
	private static final String FUNC_CHANGE_CAMERA_POSITION_CALLBACK = "uexCamera.cbChangeCameraPosition";// 改变摄像头位置的回调

	private File mTempPath;// 临时文件路径

	// 压缩相关成员变量
	private boolean mIsCompress;// 是否压缩标志
	private int mQuality;// 压缩质量
	private int mInSampleSize;// 压缩比
	private int mPhotoWidth;// 压缩图片目标宽度
	private int mPhotoHeight;// 压缩图片目标高度

	private String label = "";// 拍照时显示在界面中的提示语或标签
	private View view;// 自定义相机View
	private CameraView mCameraView;// 自定义相机View实例

	/**
	 * 构造方法
	 * 
	 * @param context
	 * @param inParent
	 */
	public EUExCamera(Context context, EBrowserView inParent) {
		super(context, inParent);
	}

	/**
	 * 初始化压缩相关成员变量
	 */
	private void initCompressFields() {

		mIsCompress = false;// 初始化压缩标志
		mQuality = 100;// 初始化压缩质量，默认为100，即不压缩
		mInSampleSize = 1;// 初始化图片压缩比，默认为1，即不压缩
		mPhotoWidth = -1;// 初始化图片目标压缩宽度，-1代表未传入该参数
		mPhotoHeight = -1;// 初始化图片目标压缩高度，-1代表未传入该参数

	}

	/**
	 * 打开系统相机
	 * 
	 * @formatter:off
	 * @param parm
	 *            isCompress:(Number类型)可选,图片是否压缩,0表示压缩,非0或者不传表示不压缩
	 *            quality:(Number类型),可选,图片压缩质量,comtextareass为0时有效,取值范围[0,100]
	 *            photoValue:(String类型),可选，期望图片参数 ,程序会根据期待图片的宽高自动计算压缩比，JSON格式如下:
	 *            													{
	 *            															width:800,
	 *            															height:600
	 *            													}
	 */
	public void open(String[] parm) {

		// @formatter:on
		// 初始化压缩相关成员变量
		initCompressFields();

		/**
		 * 如果参数>=1
		 */
		if (parm.length >= 1) {

			// 得到压缩标志
			int value = 1;
			try {
				value = Integer.parseInt(parm[0]);
			} catch (NumberFormatException e) {
				e.printStackTrace();
				Log.e(TAG, "【open】 NumberFormatException " + e.getMessage(), e);
			}
			mIsCompress = value == 0 ? true : false;

			/**
			 * 如果参数>=2 且 可压缩
			 */
			if (parm.length >= 2 && mIsCompress) {

				// 得到压缩质量
				int quality = 100;
				try {
					quality = Integer.parseInt(parm[1]);
				} catch (NumberFormatException e) {
					e.printStackTrace();
					Log.e(TAG, "【open】 【parm.length == 2】 quality  NumberFormatException" + e.getMessage(), e);
				}
				mQuality = quality;

				// 对mQuality进行容错处理
				if (mQuality < 1 || mQuality > 100) {
					mQuality = 100;
				}

				/**
				 * 如果参数>=3
				 */
				if (parm.length >= 3) {

					// 根据传入的宽高计算图片压缩比
					String photoValue = parm[2];
					try {

						JSONObject jsonObject = new JSONObject(photoValue);
						int width = Integer.parseInt(jsonObject.getString("width"));
						int height = Integer.parseInt(jsonObject.getString("height"));

						if (width > 0) {
							mPhotoWidth = width;
						}
						if (height > 0) {
							mPhotoHeight = height;
						}

					} catch (JSONException e) {

						e.printStackTrace();
						Log.e(TAG, "【open】 【parm.length == 3】 photoValue  JSONException" + e.getMessage(), e);
						mPhotoWidth = -1;
						mPhotoHeight = -1;

					} catch (NumberFormatException e) {

						e.printStackTrace();
						Log.e(TAG, "【open】 【parm.length == 3】 photoValue  NumberFormatException" + e.getMessage(), e);
						mPhotoWidth = -1;
						mPhotoHeight = -1;

					}
				}
			}
		}

		// 如果系统认为内存低
		if (MemoryUtil.isLowMemory(mContext)) {

			Log.i(TAG, "【open】	   内存不足,无法打开相机   memoryInfo.lowMemory == true ");
			Toast.makeText(mContext, EUExUtil.getString("plugin_camera_low_memory_tips"), Toast.LENGTH_LONG).show();
			Runtime.getRuntime().gc();// 提醒垃圾回收器
			return;
		}

		// 如果SD卡可以工作
		if (BUtility.sdCardIsWork()) {

			// 如果不压缩
			if (!mIsCompress) {

				// 直接已最终目录作为文件名
				String path = mBrwView.getCurrentWidget().getWidgetPath() + getName();
				mTempPath = new File(path);
			} else {

				// 使用临时文件名存储文件
				mTempPath = new File(BUtility.getSdCardRootPath() + "demo.jpg");
			}

			// 如果不存在，创建
			if (!mTempPath.exists()) {
				try {
					mTempPath.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
					Log.e(TAG, "【open】	 【mTempPath.createNewFile()】" + e.getMessage(), e);
				}
			}

			// 发Intent调用系统相机
			Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			Uri uri = Uri.fromFile(mTempPath);
			cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);// 设置系统相机拍摄照片完成后图片文件的存放地址
			cameraIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);// 设置IntentFlag,singleTask
			startActivityForResult(cameraIntent, Constant.REQUEST_CODE_SYSTEM_CAMERA);

		}

		// 如果SD卡不可用
		else {

			Log.i(TAG, "【open】 SD卡不可用");
			Toast.makeText(mContext, EUExUtil.getString("error_sdcard_is_not_available"), Toast.LENGTH_SHORT).show();
			errorCallback(0, EUExCallback.F_E_UEXCAMERA_OPEN, EUExUtil.getString("error_sdcard_is_not_available"));
			return;
		}
	}

	/**
	 * 打开自定义相机
	 * 
	 * @formatter:off
	 * @param parm
	 *            isCompress:(Number类型)可选,图片是否压缩,0表示压缩,非0或者不传表示不压缩
	 *            quality:(Number类型),可选,图片压缩质量,comtextareass为0时有效,取值范围[0,100]
	 *            photoValue:(String类型),可选，期望图片参数 ,程序会根据期待图片的宽高自动计算压缩比，JSON格式如下:
	 *            													{
	 *            															width:800,
	 *            															height:600
	 *            													}
	 */
	public void openInternal(String[] parm) {

		// @formatter:on
		// 初始化压缩相关成员变量
		initCompressFields();

		/**
		 * 如果参数>=1
		 */
		if (parm.length >= 1) {

			// 得到压缩标志
			int value = 1;
			try {
				value = Integer.parseInt(parm[0]);
			} catch (NumberFormatException e) {
				e.printStackTrace();
				Log.e(TAG, "【openInternal】 NumberFormatException " + e.getMessage(), e);
			}
			mIsCompress = value == 0 ? true : false;

			/**
			 * 如果参数>=2 且 可压缩
			 */
			if (parm.length >= 2 && mIsCompress) {

				// 得到压缩质量
				int quality = 100;
				try {
					quality = Integer.parseInt(parm[1]);
				} catch (NumberFormatException e) {
					e.printStackTrace();
					Log.e(TAG, "【openInternal】 【parm.length == 2】 quality  NumberFormatException" + e.getMessage(), e);
				}
				mQuality = quality;

				// 对mQuality进行容错处理
				if (mQuality < 1 || mQuality > 100) {
					mQuality = 100;
				}

				/**
				 * 如果参数>=3
				 */
				if (parm.length >= 3) {

					// 根据传入的宽高计算图片压缩比
					String photoValue = parm[2];
					try {

						JSONObject jsonObject = new JSONObject(photoValue);
						int width = Integer.parseInt(jsonObject.getString("width"));
						int height = Integer.parseInt(jsonObject.getString("height"));

						if (width > 0) {
							mPhotoWidth = width;
						}
						if (height > 0) {
							mPhotoHeight = height;
						}

					} catch (JSONException e) {

						e.printStackTrace();
						Log.e(TAG, "【openInternal】 【parm.length == 3】 photoValue  JSONException" + e.getMessage(), e);
						mPhotoWidth = -1;
						mPhotoHeight = -1;

					} catch (NumberFormatException e) {

						e.printStackTrace();
						Log.e(TAG,
								"【openInternal】 【parm.length == 3】 photoValue  NumberFormatException" + e.getMessage(),
								e);
						mPhotoWidth = -1;
						mPhotoHeight = -1;

					}
				}
			}
		}

		// 如果系统认为内存低
		if (MemoryUtil.isLowMemory(mContext)) {

			Log.i(TAG, "【openInternal】	   内存不足,无法打开相机   memoryInfo.lowMemory == true ");
			Toast.makeText(mContext, EUExUtil.getString("plugin_camera_low_memory_tips"), Toast.LENGTH_LONG).show();
			Runtime.getRuntime().gc();// 提醒垃圾回收器
			return;
		}

		// 如果SD卡可以工作
		if (BUtility.sdCardIsWork()) {

			// 如果不压缩
			if (!mIsCompress) {

				// 直接已最终目录作为文件名
				String path = mBrwView.getCurrentWidget().getWidgetPath() + getName();
				mTempPath = new File(path);
			} else {

				// 使用临时文件名存储文件
				mTempPath = new File(BUtility.getSdCardRootPath() + "demo.jpg");
			}

			// 如果不存在，创建
			if (!mTempPath.exists()) {
				try {
					mTempPath.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
					Log.e(TAG, "【openInternal】	 【mTempPath.createNewFile()】" + e.getMessage(), e);
				}
			}

			// 发Intent调用自定义相机
			Intent camaIntent = new Intent();
			camaIntent.setClass(mContext, CustomCameraActivity.class);
			camaIntent.putExtra(Constant.INTENT_EXTRA_NAME_PHOTO_PATH, mTempPath.getAbsolutePath());
			camaIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
			startActivityForResult(camaIntent, Constant.REQUEST_CODE_INTERNAL_CAMERA);

		}

		// 如果SD卡不可用
		else {

			Log.i(TAG, "【openInternal】 SD卡不可用");
			Toast.makeText(mContext, EUExUtil.getString("error_sdcard_is_not_available"), Toast.LENGTH_SHORT).show();
			errorCallback(0, EUExCallback.F_E_UEXCAMERA_OPEN, EUExUtil.getString("error_sdcard_is_not_available"));
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
		label = parm[4];
		// 新字段 图片质量
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
		Log.i(TAG, "location" + label);
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
		// 2016/1/19 增加超出分辨率判断
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
			String filePath = mBrwView.getWidgetPath() + "uexViewCameraPhotos";
			MyLog.getLog().i("filePath = " + filePath);
			mCameraView.setFilePath(filePath);
			mCameraView.setCallbackCameraViewClose(this);// 注册callback，将当前类传入
			mCameraView.setLabelText(label);// 调用方法写入地址
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

	/**
	 * onActivityResult
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		String finalPath = "";// 最终图片存储路径
		ExifInterface exif = null;// 主要描述多媒体文件比如JPG格式图片的一些附加信息
		int degree = 0;// 方向

		if (resultCode == Activity.RESULT_OK) {

			// 系统相机
			if (requestCode == Constant.REQUEST_CODE_SYSTEM_CAMERA) {
				try {

					/**
					 * 获取最终图片存储路径
					 */

					// 若临时文件目录不为空，直接取
					if (null != mTempPath) {

						finalPath = mTempPath.getAbsolutePath();
						Log.i(TAG, "【onActivityResult】【系统相机】  finalPath = mTempPath.getAbsolutePath()");

					}

					// 尝试从Intent中获取地址
					else if (null != data) {

						Uri content = data.getData();

						// 若Uri不为null
						if (null != content) {

							String realPath = null;
							String url = content.toString();

							// 如果是文件Url
							if (URLUtil.isFileUrl(url)) {

								realPath = url.replace("file://", "");
								Log.i(TAG, "【onActivityResult】【系统相机】  finalPath = mTempPath.getAbsolutePath()");

							}

							// 如果是内容Url
							else if (URLUtil.isContentUrl(url)) {

								// 从URL中得到真实路径
								Cursor c = mContext.getContentResolver().query(content, null, null, null, null);
								boolean isExist = c.moveToFirst();
								if (isExist) {
									realPath = c.getString(c.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
								}
								c.close();
							}

							if (null != realPath) {
								finalPath = realPath;
							}
						}

						// 若Uri为null
						else {

							// 尝试从Bundle里获得Bitmap
							Bundle bundle = data.getExtras();

							if (null != bundle) {

								Bitmap bitmap = (Bitmap) bundle.get("data");

								// 将Bitmap写入文件
								if (null != bitmap) {

									// 路径为SD卡 根目录
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
									bitmap.recycle();
									bitmap = null;
									System.gc();
								}
							}
						}
					}

					if (null == finalPath) {
						errorCallback(0, EUExCallback.F_E_UEXCAMERA_OPEN, "Storage error or no permission");
						return;
					}

					Log.i(TAG, "【onActivityResult】【系统相机】  finalPath = " + finalPath);

					if (URLUtil.isFileUrl(finalPath)) {
						finalPath = finalPath.replace("file://", "");
					}

					/**
					 * 获得图片附加信息
					 */
					exif = new ExifInterface(finalPath);

					// 获得方向
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

					// 如果不压缩，且方向==0
					if (!mIsCompress && 0 == degree) {

						// 直接回调最终地址
						jsCallback(FUNC_OPEN_CALLBACK, 0, EUExCallback.F_C_TEXT, finalPath);
					}

					// 否则，压缩拍照生成的图片
					else {
						String photoPath = makePictrue(new File(finalPath), degree);
						if (null == photoPath) {

							Log.e(TAG, "【onActivityResult】【系统相机】  makePictrue return null");
							errorCallback(0, EUExCallback.F_E_UEXCAMERA_OPEN, "Storage error or no permission");

						} else {

							jsCallback(FUNC_OPEN_CALLBACK, 0, EUExCallback.F_C_TEXT, photoPath);

						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					Log.e(TAG, "【onActivityResult】【系统相机】  catch (Exception e) " + e.getMessage(), e);
					errorCallback(0, EUExCallback.F_E_UEXCAMERA_OPEN, "Storage error or no permission");
					return;
				}
			} else if (requestCode == 67) {
				if (null != data)
					;
				finalPath = mTempPath.getAbsolutePath();
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
					if (!mIsCompress && 0 == degree) {
						jsCallback(FUNC_OPEN_INTERNAL_CALLBACK, 0, EUExCallback.F_C_TEXT, finalPath);
					} else {
						String tPath = makePictrue(new File(finalPath), degree);
						if (null == tPath) {
							errorCallback(0, EUExCallback.F_E_UEXCAMERA_OPEN, "Storage error or no permission");
						} else {
							jsCallback(FUNC_OPEN_INTERNAL_CALLBACK, 0, EUExCallback.F_C_TEXT, tPath);
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
					jsonObject.put("location", label);
					jsonObject.put("label", label);
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
	 * 因为都是压缩标志位true时调用该方法，所以不再判断是否压缩，一律直接压缩
	 * 
	 * @param tempPath_图片临时存放路径
	 * @param degree_图片方向
	 * @return
	 */
	private String makePictrue(File tempPath, int degree) {

		// 之前的路径，因为有bug，所以作废
		// String newPath = BUtility.makeRealPath("wgt://" + getName(),
		// mBrwView.getWidgetPath(),
		// mBrwView.getWidgetType());

		Log.i(TAG, "【makePictrue】	图片临时存放路径tempPath---->" + tempPath);
		Log.i(TAG, "【makePictrue】	图片方向degree---->" + degree);

		/*
		 * 获得新的存放目录
		 */
		String folderPath = Environment.getExternalStorageDirectory().toString() + "/widgetone/apps/"
				+ mBrwView.getRootWidget().m_appId + "/photo";// 获得文件夹路径
		FileUtil.checkFolderPath(folderPath);// 如果不存在，则创建所有的父文件夹
		String newPath = Environment.getExternalStorageDirectory().toString() + "/widgetone/apps/"
				+ mBrwView.getRootWidget().m_appId + "/" + getName();// 获得新的存放目录
		Log.i(TAG, "【makePictrue】	newPath---->" + newPath);

		/*
		 * 压缩图片
		 */
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;// 不返回实际的bitmap，也不给其分配内存空间,但是允许我们查询图片的信息这其中就包括图片大小信息
		try {

			// 从文件中生成一个不占内存的bitmap
			@SuppressWarnings("unused")
			Bitmap boundBitmap = BitmapFactory.decodeStream(new FileInputStream(tempPath.getAbsolutePath()), null,
					options);
			boundBitmap = null;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Log.e(TAG, "【makePictrue】  FileNotFoundException" + e.getMessage(), e);
		}

		if (mPhotoWidth != -1 && mPhotoHeight != -1) {

			// 根据压缩图片目标宽高计算压缩比
			mInSampleSize = BitmapUtil.calculateInSampleSize(options, mPhotoWidth, mPhotoHeight);
			Log.i(TAG, "【makePictrue】	根据压缩图片目标宽高计算压缩比  mPhotoWidth = " + mPhotoWidth + " mPhotoHeight = "
					+ mPhotoHeight + " mInSampleSize = " + mInSampleSize);

		} else {

			// 如果没有压缩图片目标宽高，则默认压缩比设置为1
			mInSampleSize = 1;
			Log.i(TAG, "【makePictrue】	没有压缩图片目标宽高，则默认压缩比设置为1");

		}
		options.inSampleSize = mInSampleSize;
		options.inPurgeable = true;
		options.inInputShareable = true;
		options.inTempStorage = new byte[64 * 1024];
		options.inJustDecodeBounds = false;// decode得到的bitmap将写入内存
		Log.i(TAG, "【makePictrue】	压缩比mInSampleSize---->" + mInSampleSize);
		Log.i(TAG, "【makePictrue】	压缩质量mQuality---->" + mQuality);

		Bitmap tempBitmap = null;
		File newFile = new File(newPath);
		try {

			// 生成临时位图
			tempBitmap = BitmapFactory.decodeStream(new FileInputStream(tempPath.getAbsolutePath()), null, options);

			if (tempBitmap == null) {
				Log.i(TAG, "【makePictrue】	生成临时位图失败，tmpPicture == null return");
				return null;
			}

			// 如果方向大于0
			if (degree > 0) {

				// 旋转bitmap
				tempBitmap = BitmapUtil.rotate(tempBitmap, degree);

				if (tempBitmap == null) {
					Log.i(TAG, "【makePictrue】	旋转临时位图失败，tmpPicture == null return");
					return null;
				}
			}

			// 写入文件
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(newFile));
			tempBitmap.compress(Bitmap.CompressFormat.JPEG, mQuality, bos);
			bos.flush();
			bos.close();

			return newPath;

		} catch (OutOfMemoryError e) {

			// Toast.makeText(mContext, "照片尺寸过大，内存溢出，\n请降低尺寸拍摄！",
			// Toast.LENGTH_LONG).show();
			Log.e(TAG, "【makePictrue】 OutOfMemoryError 压缩质量mQuality = " + mQuality + " 压缩比mInSampleSize = "
					+ mInSampleSize + e.getMessage(), e);

			mInSampleSize = mInSampleSize * 2;// 压缩比增加
			return makePictrue(tempPath, degree);// 继续压缩

		} catch (FileNotFoundException e) {

			e.printStackTrace();
			Log.e(TAG, "【makePictrue】  FileNotFoundException" + e.getMessage(), e);

		} catch (IOException e) {

			e.printStackTrace();
			Log.e(TAG, "【makePictrue】  IOException" + e.getMessage(), e);

		} finally {

			mInSampleSize = 1;// 回归正常压缩比

			if (tempBitmap != null) {
				tempBitmap.recycle();
				tempBitmap = null;
			}

			tempPath.delete();// 删除临时图片
			System.gc();
		}
		return null;
	}

	/**
	 * 得到文件名
	 * 
	 * @return
	 */
	private String getName() {

		String fileName = FileUtil.getSimpleDateFormatFileName("photo/scan", ".jpg");

		return fileName;
	}

	@Override
	protected boolean clean() {
		if (null != mTempPath) {
			mTempPath = null;
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
