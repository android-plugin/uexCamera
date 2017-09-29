package org.zywx.wbpalmstar.plugin.uexcamera;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.text.TextUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.engine.DataHelper;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;
import org.zywx.wbpalmstar.engine.universalex.EUExUtil;
import org.zywx.wbpalmstar.plugin.uexcamera.ViewCamera.CallbackCameraViewClose;
import org.zywx.wbpalmstar.plugin.uexcamera.ViewCamera.CameraView;
import org.zywx.wbpalmstar.plugin.uexcamera.utils.BitmapUtil;
import org.zywx.wbpalmstar.plugin.uexcamera.utils.FileUtil;
import org.zywx.wbpalmstar.plugin.uexcamera.utils.MemoryUtil;
import org.zywx.wbpalmstar.plugin.uexcamera.utils.MLog;

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
import android.view.Display;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.zywx.wbpalmstar.plugin.uexcamera.utils.PermissionUtil;
import org.zywx.wbpalmstar.plugin.uexcamera.vo.OpenViewCameraVO;

public class EUExCamera extends EUExBase implements CallbackCameraViewClose {

	public static final String TAG = "uexCamera";

	// 回调
	private static final String FUNC_OPEN_CALLBACK = "uexCamera.cbOpen";// 打开系统相机回调
	private static final String FUNC_OPEN_INTERNAL_CALLBACK = "uexCamera.cbOpenInternal";// 打开自定义相机回调
	private static final String FUNC_OPEN_VIEW_CAMERA_CALLBACK = "uexCamera.cbOpenViewCamera";// 打开自定义相机View回调
	private static final String FUNC_CHANGE_FLASHMODE_CALLBACK = "uexCamera.cbChangeFlashMode";// 改变闪关灯模式的回调
	private static final String FUNC_CHANGE_CAMERA_POSITION_CALLBACK = "uexCamera.cbChangeCameraPosition";// 改变摄像头位置的回调
	private static final String FUNC_ON_PERMISSION_DENIED = "uexCamera.onPermissionDenied";//权限被拒绝监听

	private File mTempPath;// 临时文件路径
    //方法对应的回调函数
    private String openFunc;
    private String openInternalFunc;
    private String openViewCameraFunc;

	// 压缩相关成员变量
	private boolean mIsCompress;// 是否压缩标志
	private int mQuality;// 压缩质量
	private int mInSampleSize = 1;// 压缩比
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
		if (!PermissionUtil.checkCameraPermission()) {
			callbackCameraPermissionDenied();
			return;
		}
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
				MLog.getIns().e(e);
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
					MLog.getIns().e(e);
				}
				mQuality = quality;

				// 对mQuality进行容错处理
				if (mQuality < 1 || mQuality > 100) {
					mQuality = 100;
				}

				/**
				 * 如果参数>=3
				 */
//				if (parm.length >= 3) {
//
//					// 根据传入的宽高计算图片压缩比
//					String photoValue = parm[2];
//					try {
//
//						JSONObject jsonObject = new JSONObject(photoValue);
//						int width = Integer.parseInt(jsonObject.getString("width"));
//						int height = Integer.parseInt(jsonObject.getString("height"));
//
//						if (width > 0) {
//							mPhotoWidth = width;
//						}
//						if (height > 0) {
//							mPhotoHeight = height;
//						}
//
//					} catch (JSONException e) {
//
//						e.printStackTrace();
//						MLog.getIns().e(e);
//						mPhotoWidth = -1;
//						mPhotoHeight = -1;
//
//					} catch (NumberFormatException e) {
//
//						e.printStackTrace();
//						MLog.getIns().e(e);
//						mPhotoWidth = -1;
//						mPhotoHeight = -1;
//
//					}
//
//				}
			}
		}

		// 如果系统认为内存低
		if (MemoryUtil.isLowMemory(mContext)) {

			MLog.getIns().e("内存不足,无法打开相机   memoryInfo.lowMemory == true");
			Toast.makeText(mContext, EUExUtil.getString("plugin_camera_low_memory_tips"), Toast.LENGTH_LONG).show();
			Runtime.getRuntime().gc();// 提醒垃圾回收器
			return;
		}

		// 如果SD卡可以工作
		if (BUtility.sdCardIsWork()) {

			// 如果不压缩
			if (!mIsCompress) {

				// 直接已最终目录作为文件名
				String folderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/widgetone/apps/" + mBrwView.getRootWidget().m_appId + "/photo";// 获得文件夹路径
				FileUtil.checkFolderPath(folderPath);// 如果不存在，则创建所有的父文件夹
				String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/widgetone/apps/" + mBrwView.getRootWidget().m_appId + "/" + getName();// 获得新的存放目录
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
					MLog.getIns().e(e);
				}
			}

            int len = parm.length;
            if (len > 2) {
                openFunc = parm[2];

            }

			// 发Intent调用系统相机
			Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			Uri uri = Uri.fromFile(mTempPath);
			MLog.getIns().i("uri = " + uri.toString());
			cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);// 设置系统相机拍摄照片完成后图片文件的存放地址
			cameraIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);// 设置IntentFlag,singleTask
			startActivityForResult(cameraIntent, Constant.REQUEST_CODE_SYSTEM_CAMERA);
		}

		// 如果SD卡不可用
		else {

			MLog.getIns().e("SD卡不可用");
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
		if (!PermissionUtil.checkCameraPermission()) {
			callbackCameraPermissionDenied();
			return;
		}
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
				MLog.getIns().e(e);
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
					MLog.getIns().e(e);
				}
				mQuality = quality;

				// 对mQuality进行容错处理
				if (mQuality < 1 || mQuality > 100) {
					mQuality = 100;
				}

				/**
				 * 如果参数>=3
				 */
//				if (parm.length >= 3) {
//
//					// 根据传入的宽高计算图片压缩比
//					String photoValue = parm[2];
//					try {
//
//						JSONObject jsonObject = new JSONObject(photoValue);
//						int width = Integer.parseInt(jsonObject.getString("width"));
//						int height = Integer.parseInt(jsonObject.getString("height"));
//
//						if (width > 0) {
//							mPhotoWidth = width;
//						}
//						if (height > 0) {
//							mPhotoHeight = height;
//						}
//
//					} catch (JSONException e) {
//
//						e.printStackTrace();
//						MLog.getIns().e(e);
//						mPhotoWidth = -1;
//						mPhotoHeight = -1;
//
//					} catch (NumberFormatException e) {
//
//						e.printStackTrace();
//						MLog.getIns().e(e);
//						mPhotoWidth = -1;
//						mPhotoHeight = -1;
//
//					}
//				}
			}
		}

		// 如果系统认为内存低
		if (MemoryUtil.isLowMemory(mContext)) {

			MLog.getIns().e("内存不足,无法打开相机   memoryInfo.lowMemory == true");
			Toast.makeText(mContext, EUExUtil.getString("plugin_camera_low_memory_tips"), Toast.LENGTH_LONG).show();
			Runtime.getRuntime().gc();// 提醒垃圾回收器
			return;
		}

		// 如果SD卡可以工作
		if (BUtility.sdCardIsWork()) {

			// 如果不压缩
			if (!mIsCompress) {

				// 直接已最终目录作为文件名
				String folderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/widgetone/apps/" + mBrwView.getRootWidget().m_appId + "/photo";// 获得文件夹路径
				FileUtil.checkFolderPath(folderPath);// 如果不存在，则创建所有的父文件夹
				String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/widgetone/apps/" + mBrwView.getRootWidget().m_appId + "/" + getName();// 获得新的存放目录
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
					MLog.getIns().e(e);
				}
			}
            int len = parm.length;
            if (len > 2) {
                openInternalFunc = parm[2];

            }
			// 发Intent调用自定义相机
			Intent camaIntent = new Intent();
			MLog.getIns().i("mTempPath = " + mTempPath);
			camaIntent.setClass(mContext, CustomCameraActivity.class);
			camaIntent.putExtra(Constant.INTENT_EXTRA_NAME_PHOTO_PATH, mTempPath.getAbsolutePath());
			camaIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
			startActivityForResult(camaIntent, Constant.REQUEST_CODE_INTERNAL_CAMERA);

		}

		// 如果SD卡不可用
		else {

			MLog.getIns().e("SD卡不可用");
			Toast.makeText(mContext, EUExUtil.getString("error_sdcard_is_not_available"), Toast.LENGTH_SHORT).show();
			errorCallback(0, EUExCallback.F_E_UEXCAMERA_OPEN, EUExUtil.getString("error_sdcard_is_not_available"));
			return;
		}
	}

	boolean isJson(String str){
		if (TextUtils.isEmpty(str)){
			return false;
		}
		return str.startsWith("{") && str.endsWith("}")||
				str.startsWith("[") && str.endsWith("]");
	}

	/**
	 * 打开自定义View相机
	 *
	 * @param parm
	 */
	public void openViewCamera(String[] parm) {
		if (!PermissionUtil.checkCameraPermission()) {
			callbackCameraPermissionDenied();
			return;
		}
		int len = parm.length;
		OpenViewCameraVO openVO;
		if (isJson(parm[0])){
			openVO= DataHelper.gson.fromJson(parm[0],OpenViewCameraVO.class);
			if (len>1){
				openViewCameraFunc=parm[1];
			}
		}else{
			openVO=new OpenViewCameraVO();
			openVO.x=Integer.valueOf(parm[0]);
			openVO.y=Integer.valueOf(parm[1]);
			openVO.width=Integer.valueOf(parm[2]);
			openVO.height=Integer.valueOf(parm[3]);
			if (len>4){
				openVO.hint=parm[4];
			}
			if (len>5){
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
				openVO.quality=quality;
			}
			if (len > 6) {
				openViewCameraFunc = parm[6];
			}
		}
		label = openVO.hint;

		// 2016/1/19 增加超出分辨率判断
		/** 获得屏幕分辨率 **/
		Display display = ((Activity) mContext).getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int screenWidth = size.x;
		int screenHeight = size.y;

		if ((openVO.x + openVO.width) > screenWidth) {// 如果宽度超出屏幕宽度
			openVO.width = screenWidth - openVO.x;// 限制最大为屏幕宽度
		}
		if ((openVO.y + openVO.height) > screenHeight) {// 如果高度超出屏幕高度
			openVO.height = screenHeight - openVO.y;// 限制最大为屏幕高度
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
			MLog.getIns().i("filePath = " + filePath);
			mCameraView.setFilePath(filePath);
			Button cancelBtn= (Button) view.findViewById(EUExUtil.getResIdID("plugin_camera_bt_cancel"));
			cancelBtn.setVisibility(View.INVISIBLE);
			cancelBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
                    if (null != view) {
                        removeViewFromCurrentWindow(view);
                        view = null;
                    }
				}
			});
			mCameraView.setCallbackCameraViewClose(this);// 注册callback，将当前类传入
			mCameraView.setLabelText(label);// 调用方法写入地址
            mCameraView.options=openVO.options;
			if (openVO.quality != -1) {// 如果quality格式正确
				mCameraView.setQuality(openVO.quality);
			}
			RelativeLayout.LayoutParams lparm = new RelativeLayout.LayoutParams(openVO.width, openVO.height);
			lparm.leftMargin = openVO.x;
			lparm.topMargin = openVO.y;
			addViewToCurrentWindow(mCameraView, lparm);

		}
	}

	/**
	 * 移除自定义相机
	 *
	 * @param parm
	 */
	public boolean removeViewCameraFromWindow(String[] parm) {
		if (null != view) {
			removeViewFromCurrentWindow(view);
			view = null;
		}
        return true;
		// Toast.makeText(mContext, "removeViewCameraFromWindow",
		// Toast.LENGTH_SHORT).show();
	}

	/**
	 * 更改闪光灯模式,只允许输入0、1、2三个数字,0代表自动，1代表开启，2代表关闭,默认为关闭
	 *
	 * @param parm
	 */
	public Integer changeFlashMode(String[] parm) {
		String flashMode = parm[0];
		if (flashMode.equals("0") || flashMode.equals("1") || flashMode.equals("2")) {
			mCameraView.setFlashMode(Integer.valueOf(flashMode));
			return Integer.parseInt(flashMode);
		} else {
			return -1;
		}
	}

	/**
	 * 设置前后摄像头,只允许输入0、1两个数字,0代表前置，1代表后置,默认为后置
	 *
	 * @param parm
	 */
	public Integer changeCameraPosition(String[] parm) {
		String cameraPosition = parm[0];
		if (view == null) {
			return 0;
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
            return Integer.parseInt(cameraPosition);
		} else if (cameraPosition.equals("1")) {
			CameraView.cameraPosition = 0;
			((Activity) mContext).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mCameraView.overturnCamera();
				}
			});
            jsCallback(FUNC_CHANGE_CAMERA_POSITION_CALLBACK, 0, EUExCallback.F_C_TEXT, cameraPosition);
            return Integer.parseInt(cameraPosition);
		} else {
            jsCallback(FUNC_CHANGE_CAMERA_POSITION_CALLBACK, 0, EUExCallback.F_C_TEXT, "-1");
            return -1;
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
						MLog.getIns().i("finalPath = " + finalPath);

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
								MLog.getIns().i("realPath = " + realPath);

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
									BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(newfile)));
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

					MLog.getIns().i("finalPath = " + finalPath);

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

							MLog.getIns().e("null == photoPath");
							errorCallback(0, EUExCallback.F_E_UEXCAMERA_OPEN, "Storage error or no permission");

						} else {
                            if (TextUtils.isEmpty(openFunc)){
                                jsCallback(FUNC_OPEN_CALLBACK, 0, EUExCallback.F_C_TEXT, photoPath);
                            }else{
                                callbackToJs(Integer.parseInt(openFunc), false, photoPath);
                            }
                        }
					}
				} catch (Exception e) {
					e.printStackTrace();
					MLog.getIns().e(e);
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
                            if (TextUtils.isEmpty(openInternalFunc)){
                                jsCallback(FUNC_OPEN_INTERNAL_CALLBACK, 0, EUExCallback.F_C_TEXT, tPath);
                            }else{
                                callbackToJs(Integer.parseInt(openInternalFunc), false, tPath);
                            }
                        }
					}
				}
			} else if (requestCode == 68) {
				MLog.getIns().i("requestCode = " + requestCode);
				String photoPath = data.getStringExtra("photoPath");
				closeViewAndCallback(photoPath);
			}
		} else if (resultCode == Activity.RESULT_CANCELED) {// 如果是取消标志 change by
															// waka 2016-01-28
			if (requestCode == 68) {// 如果是SecondActivity传回来的取消标记
				mCameraView.setCameraTakingPhoto(false);// 设置正在照相标记为false
			}
		}
	}


	public void closeViewAndCallback(String photoPath){
        removeViewCameraFromWindow(null);// 移除自定义View相机
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
        if (null != openViewCameraFunc) {
            callbackToJs(Integer.parseInt(openViewCameraFunc), false, jsonObject);
        }else{
            jsCallback(FUNC_OPEN_VIEW_CAMERA_CALLBACK, 0, EUExCallback.F_C_TEXT, jsonResult);
        }
    }

	/**
	 * 压缩拍照生成的图片
	 *
	 * 因为都是压缩标志位true时调用该方法，所以不再判断是否压缩，一律直接压缩
	 *
	 * @param tempPath 图片临时存放路径
	 * @param degree 图片方向
	 * @return
	 */
	private String makePictrue(File tempPath, int degree) {

		// 之前的路径，因为有bug，所以作废
		// String newPath = BUtility.makeRealPath("wgt://" + getName(),
		// mBrwView.getWidgetPath(),
		// mBrwView.getWidgetType());

		MLog.getIns().i("图片临时存放路径tempPath = " + tempPath);
		MLog.getIns().i("图片方向degree = " + degree);

		/*
		 * 获得新的存放目录
		 */
		String folderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/widgetone/apps/" + mBrwView.getRootWidget().m_appId + "/photo";// 获得文件夹路径
		FileUtil.checkFolderPath(folderPath);// 如果不存在，则创建所有的父文件夹
		String newPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/widgetone/apps/" + mBrwView.getRootWidget().m_appId + "/" + getName();// 获得新的存放目录
		MLog.getIns().i("newPath = " + newPath);

		/*
		 * 压缩图片
		 */
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;// 不返回实际的bitmap，也不给其分配内存空间,但是允许我们查询图片的信息这其中就包括图片大小信息
		try {

			// 从文件中生成一个不占内存的bitmap
			@SuppressWarnings("unused")
			Bitmap boundBitmap = BitmapFactory.decodeStream(new FileInputStream(tempPath.getAbsolutePath()), null, options);
			boundBitmap = null;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			MLog.getIns().e(e);
		}

		if (mPhotoWidth != -1 && mPhotoHeight != -1) {

			// 根据压缩图片目标宽高计算压缩比
			mInSampleSize = BitmapUtil.calculateInSampleSize(options, mPhotoWidth, mPhotoHeight);
			MLog.getIns().i("根据压缩图片目标宽高计算压缩比  mPhotoWidth = " + mPhotoWidth + " mPhotoHeight = " + mPhotoHeight + " mInSampleSize = " + mInSampleSize);

		} else {

			// 如果没有压缩图片目标宽高，则默认压缩比设置为默认值
			MLog.getIns().i("没有压缩图片目标宽高，则默认压缩比设置为" + mInSampleSize);

		}

		// 如果是低内存状态
		if (MemoryUtil.isLowMemory(mContext)) {// 判断系统是否为低内存状态
			MLog.getIns().e("系统处于低内存状态");
			// TODO 强制进行压缩
			mInSampleSize = 2;
			mQuality = 60;
		}

		options.inSampleSize = mInSampleSize;
		options.inPurgeable = true;// 为True的话表示使用BitmapFactory创建的Bitmap用于存储Pixel的内存空间在系统内存不足时可以被回收;在Android5.0已过时;http://blog.sina.com.cn/s/blog_7607703f0101fzl7.html
		options.inInputShareable = true;// inInputShareable与inPurgeable一起使用，如果inPurgeable为false那该设置将被忽略，如果为true，那么它可以决定位图是否能够共享一个指向数据源的引用，或者是进行一份拷贝;在Android5.0已过时;http://blog.csdn.net/xu_fu/article/details/7340454
		// options.inTempStorage = new byte[64 *
		// 1024];//这个属性比较鸡肋,可能带来不必要的问题;http://gqdy365.iteye.com/blog/970156
		options.inJustDecodeBounds = false;// decode得到的bitmap将写入内存
		MLog.getIns().i("【makePictrue】	压缩比mInSampleSize---->" + mInSampleSize);
		MLog.getIns().i("【makePictrue】	压缩质量mQuality---->" + mQuality);

		Bitmap tempBitmap = null;
		File newFile = new File(newPath);
		try {

			// 生成临时位图
			tempBitmap = BitmapFactory.decodeStream(new FileInputStream(tempPath.getAbsolutePath()), null, options);

			if (tempBitmap == null) {
				MLog.getIns().i("【makePictrue】	生成临时位图失败，tmpPicture == null return");
				return null;
			}

			// 如果方向大于0
			if (degree > 0) {

				// 旋转bitmap
				tempBitmap = BitmapUtil.rotate(tempBitmap, degree);

				if (tempBitmap == null) {
					MLog.getIns().i("【makePictrue】	旋转临时位图失败，tmpPicture == null return");
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
			MLog.getIns().e("【makePictrue】 OutOfMemoryError 压缩质量mQuality = " + mQuality + " 压缩比mInSampleSize = " + mInSampleSize + e.getMessage(), e);

			if (options != null) {
				options = null;
			}
			if (tempBitmap != null) {
				tempBitmap.recycle();
				tempBitmap = null;
			}
			System.gc();

			mInSampleSize = mInSampleSize * 2;// 压缩比增加
			MLog.getIns().i("mInSampleSize = " + mInSampleSize);
			return makePictrue(tempPath, degree);// 继续压缩

		} catch (FileNotFoundException e) {

			e.printStackTrace();
			MLog.getIns().e(e);

		} catch (IOException e) {

			e.printStackTrace();
			MLog.getIns().e(e);

		} finally {

			mInSampleSize = 1;// 回归正常压缩比

			if (options != null) {
				options = null;
			}
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

	private void callbackCameraPermissionDenied() {
		String errorData = "{\"errCode\":\"1\",\"info\":\"" + EUExUtil.getString("plugin_camera_permission_denied") + "\"}";
		jsCallbackJsonObject(FUNC_ON_PERMISSION_DENIED, errorData);
	}

	/**
	 * javaScript json object callback
	 *
	 * @param jsCallbackName callback name
	 * @param data callback data
	 */
	private void jsCallbackJsonObject(String jsCallbackName, String data) {
		String js = SCRIPT_HEADER + "if(" + jsCallbackName + "){"
				+ jsCallbackName + "(" + data + ");}";
		onCallback(js);
	}
}
