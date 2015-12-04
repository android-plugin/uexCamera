package org.zywx.wbpalmstar.plugin.uexcamera;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.Toast;

import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EUExCamera extends EUExBase {

	public static final String TAG = "EUExCamera";
	public static final String function = "uexCamera.cbOpen";
	public static final String function1 = "uexCamera.cbOpenInternal";

	private File m_tempPath;
	private boolean mWillCompress;
	private int mQuality;

	public EUExCamera(Context context, EBrowserView inParent) {
		super(context, inParent);
	}

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
		ActivityManager activityManager = (ActivityManager) mContext
				.getSystemService(Context.ACTIVITY_SERVICE);
		activityManager.getMemoryInfo(outInfo);
		if (outInfo.lowMemory /* || outInfo.availMem < 31000000 */) {
			Toast.makeText(mContext, "内存不足,无法打开相机", Toast.LENGTH_LONG).show();
			Runtime.getRuntime().gc();
			return;
		}
		if (BUtility.sdCardIsWork()) {
			if (!mWillCompress) {
				String path = mBrwView.getCurrentWidget().getWidgetPath()
						+ getName();
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
			Toast.makeText(
					mContext,
					ResoureFinder.getInstance().getString(mContext,
							"error_sdcard_is_not_available"),
					Toast.LENGTH_SHORT).show();
			errorCallback(0, EUExCallback.F_E_UEXCAMERA_OPEN, "Storage error");
			return;
		}
	}

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
		ActivityManager activityManager = (ActivityManager) mContext
				.getSystemService(Context.ACTIVITY_SERVICE);
		activityManager.getMemoryInfo(outInfo);
		if (outInfo.lowMemory /* || outInfo.availMem < 31000000 */) {
			Toast.makeText(mContext, "内存不足,无法打开相机", Toast.LENGTH_LONG).show();
			Runtime.getRuntime().gc();
			return;
		}
		if (BUtility.sdCardIsWork()) {
			if (!mWillCompress) {
				String path = mBrwView.getCurrentWidget().getWidgetPath()
						+ getName();
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
			Toast.makeText(
					mContext,
					ResoureFinder.getInstance().getString(mContext,
							"error_sdcard_is_not_available"),
					Toast.LENGTH_SHORT).show();
			errorCallback(0, EUExCallback.F_E_UEXCAMERA_OPEN, "Storage error");
			return;
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
								Cursor c = activity.managedQuery(content, null,
										null, null, null);
								boolean isExist = c.moveToFirst();
								if (isExist) {
									realPath = c
											.getString(c
													.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
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
									String newfile = BUtility
											.getSdCardRootPath() + "demo.jpg";
									File newFile = new File(newfile);
									if (!newFile.exists()) {
										newFile.createNewFile();
									}
									BufferedOutputStream bos = new BufferedOutputStream(
											new FileOutputStream(new File(
													newfile)));
									bitmap.compress(Bitmap.CompressFormat.JPEG,
											100, bos);
									bos.flush();
									bos.close();
									finalPath = newfile;
								}
							}
						}
					}
					if (null == finalPath) {
						errorCallback(0, EUExCallback.F_E_UEXCAMERA_OPEN,
								"Storage error or no permission");
						return;
					}
					if (URLUtil.isFileUrl(finalPath)) {
						finalPath = finalPath.replace("file://", "");
					}
					exif = new ExifInterface(finalPath);
					int orientation = exif.getAttributeInt(
							ExifInterface.TAG_ORIENTATION, -1);
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
						jsCallback(function, 0, EUExCallback.F_C_TEXT,
								finalPath);
					} else {
						String tPath = makePictrue(new File(finalPath), degree);
						if (null == tPath) {
							errorCallback(0, EUExCallback.F_E_UEXCAMERA_OPEN,
									"Storage error or no permission");
						} else {
							jsCallback(function, 0, EUExCallback.F_C_TEXT,
									tPath);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					errorCallback(0, EUExCallback.F_E_UEXCAMERA_OPEN,
							"Storage error or no permission");
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
					int orientation = exif.getAttributeInt(
							ExifInterface.TAG_ORIENTATION, -1);
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
						jsCallback(function1, 0, EUExCallback.F_C_TEXT,
								finalPath);
					} else {
						String tPath = makePictrue(new File(finalPath), degree);
						if (null == tPath) {
							errorCallback(0, EUExCallback.F_E_UEXCAMERA_OPEN,
									"Storage error or no permission");
						} else {
							jsCallback(function1, 0, EUExCallback.F_C_TEXT,
									tPath);
						}
					}
				}
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
		String rootPath = Environment.getExternalStorageDirectory().toString()+"/widgetone/apps/"+mBrwView.getRootWidget().m_appId+"/photo";
		File file = new File(rootPath);
		if (!file.exists()) {
			file.mkdirs();// 如果不存在，则创建所有的父文件夹
		}
		String newPath = Environment.getExternalStorageDirectory().toString()+"/widgetone/apps/"+mBrwView.getRootWidget().m_appId+"/"+getName();
		Log.i("ttest", "newPath---->"+newPath);
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

	private String getName() {
		Date date = new Date();
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddhhmmss");
		return "photo/scan" + df.format(date) + ".jpg";
	}

	private void checkPath() {
		String widgetPath = mBrwView.getCurrentWidget().getWidgetPath()
				+ "photo";
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
}
