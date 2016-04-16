package org.zywx.wbpalmstar.plugin.uexcamera;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import android.R.integer;
import android.annotation.SuppressLint;
import android.app.Activity;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;

import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.View;

import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;

import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import android.widget.Toast;

/**
 * 自定义相机Activity
 * 
 * @author waka
 *
 */
public class CustomCameraActivity extends Activity implements Callback, AutoFocusCallback {

	// View
	public SurfaceView mSurfaceView;
	private Button mBtnCancel;
	private Button mBtnHandler;
	private Button mBtnTakePic;
	private Button mBtnChangeFacing;
	private Button mBtnFlash1;
	private Button mBtnFlash2;
	private Button mBtnFlash3;
	private ImageView mIvPreShow;

	// Camera相关
	public Camera mCamera;
	public String filePath = null;// 照片保存路径
	private boolean hasSurface;
	private boolean isOpenFlash = true;
	public int cameraCurrentlyLocked;

	// The first rear facing camera
	private ArrayList<Integer> flashDrawableIds;
	private boolean mPreviewing = false;
	protected boolean isHasPic = false;
	private boolean isHasFrontCamera = false;// 是否有前置摄像头
	private boolean isHasBackCamera = false;
	private boolean ismCameraCanFlash = false;
	private HandlePicAsyncTask mHandleTask;
	private View view_focus;
	private MODE mode = MODE.NONE;// 默认模式
	private final int NEED_CLOSE_FLASH_BTS = 1;
	private OrientationEventListener orientationEventListener = null;
	private int current_orientation = 0;
	private int picture_orientation = 0;

	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == NEED_CLOSE_FLASH_BTS) {
				if (isOpenFlash) {
					isOpenFlash = false;
					mBtnFlash2.setVisibility(View.INVISIBLE);
					mBtnFlash3.setVisibility(View.INVISIBLE);
					Log.d("visible", " after close flash view,bts visible is" + mBtnFlash2.getVisibility() + " ,"
							+ mBtnFlash3.getVisibility());
				}
			}
			super.handleMessage(msg);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		CRes.init(getApplication());

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(CRes.plugin_camera_layout);
		filePath = getIntent().getStringExtra(Constant.INTENT_EXTRA_NAME_PHOTO_PATH);

		int numberOfCameras = Camera.getNumberOfCameras();
		CameraInfo cameraInfo = new CameraInfo();
		for (int i = 0; i < numberOfCameras; i++) {
			Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
				isHasBackCamera = true;
			} else if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
				isHasFrontCamera = true;
			}
		}
		if (isHasBackCamera) {
			cameraCurrentlyLocked = CameraInfo.CAMERA_FACING_BACK;
		} else if (isHasFrontCamera) {
			cameraCurrentlyLocked = CameraInfo.CAMERA_FACING_FRONT;
		} else {
			Toast.makeText(this, "no camera find", Toast.LENGTH_SHORT).show();
			return;
		}

		orientationEventListener = new OrientationEventListener(this) {
			@Override
			public void onOrientationChanged(int orientation) {
				CustomCameraActivity.this.onOrientationChanged(orientation);
			}
		};

		mSurfaceView = (SurfaceView) findViewById(CRes.plugin_camera_surfaceview);
		mBtnCancel = (Button) findViewById(CRes.plugin_camera_bt_cancel);
		mBtnHandler = (Button) findViewById(CRes.plugin_camera_bt_complete);
		mBtnChangeFacing = (Button) findViewById(CRes.plugin_camera_bt_changefacing);
		if (!isHasBackCamera || !isHasFrontCamera) {
			mBtnChangeFacing.setVisibility(View.INVISIBLE);
		}
		mIvPreShow = (ImageView) findViewById(CRes.plugin_camera_iv_preshow);
		mBtnTakePic = (Button) findViewById(CRes.plugin_camera_bt_takepic);
		mBtnFlash1 = (Button) findViewById(CRes.plugin_camera_bt_flash1);
		mBtnFlash2 = (Button) findViewById(CRes.plugin_camera_bt_flash2);
		mBtnFlash3 = (Button) findViewById(CRes.plugin_camera_bt_flash3);

		flashDrawableIds = new ArrayList<Integer>();
		flashDrawableIds.add(Integer.valueOf(CRes.plugin_camera_flash_drawale_auto));
		flashDrawableIds.add(Integer.valueOf(CRes.plugin_camera_flash_drawale_open));
		flashDrawableIds.add(Integer.valueOf(CRes.plugin_camera_flash_drawale_close));

		mBtnFlash1.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (isOpenFlash) {
					updateFlashButtonState(0);
					Log.d("visible", " when open,after click flash1 view,bts visible is" + mBtnFlash2.getVisibility()
							+ " ," + mBtnFlash3.getVisibility());
				} else {
					isOpenFlash = true;
					mBtnFlash2.setVisibility(View.VISIBLE);
					mBtnFlash3.setVisibility(View.VISIBLE);
					mBtnFlash2.bringToFront();
					mBtnFlash3.bringToFront();
					Log.d("visible", "when close, after click flash1 view,bts visible is" + mBtnFlash2.getVisibility()
							+ " ," + mBtnFlash3.getVisibility());
					mHandler.sendEmptyMessageDelayed(NEED_CLOSE_FLASH_BTS, 4000);
				}
			}
		});
		mBtnFlash2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				updateFlashButtonState(1);
			}
		});
		mBtnFlash3.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				updateFlashButtonState(2);
			}
		});

		mBtnCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(Activity.RESULT_CANCELED);
				onPause();
				finish();
			}
		});
		mBtnHandler.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isHasPic) {
					setResult(RESULT_OK);
					onPause();
					finish();
				}

			}
		});
		mBtnTakePic.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mode == MODE.FOCUSFAIL || mode == MODE.FOCUSING || mCamera == null) {
					return;
				}
				if (mPreviewing) {
					mPreviewing = false;
					mCamera.takePicture(null, null, jpeg);
				} else {
					Toast.makeText(CustomCameraActivity.this, "摄像机正忙", Toast.LENGTH_SHORT).show();
				}
			}
		});
		mBtnChangeFacing.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mCamera != null) {
					mCamera.setPreviewCallback(null);
					mCamera.stopPreview();// 停掉原来摄像头的预览
					mPreviewing = false;
					Log.d("mPreviewing", " into change facing mPreviewing changed to :" + mPreviewing);
					mCamera.release();
					mCamera = null;
					if (cameraCurrentlyLocked == Camera.CameraInfo.CAMERA_FACING_BACK) {
						cameraCurrentlyLocked = Camera.CameraInfo.CAMERA_FACING_FRONT;
						mCamera = Camera.open(cameraCurrentlyLocked);
					} else {
						cameraCurrentlyLocked = Camera.CameraInfo.CAMERA_FACING_BACK;
						mCamera = Camera.open(cameraCurrentlyLocked);// 打开当前选中的摄像头
					}
					try {
						mCamera.setPreviewDisplay(mSurfaceView.getHolder());// 通过surfaceview显示取景画面
					} catch (IOException e) {
						e.printStackTrace();
					}
					initCameraParameters();
					mCamera.startPreview();// 开始预览
					mPreviewing = true;
					Log.d("mPreviewing", "mPreviewing changed to :" + mPreviewing);
				}
			}
		});
		mHandler.sendEmptyMessageDelayed(NEED_CLOSE_FLASH_BTS, 1000);
		view_focus = (View) findViewById(CRes.plugin_camera_view_focus);
		mSurfaceView.setOnTouchListener(onTouchListener);
		Log.d("visible", " after oncreate flash view,bts visible is" + mBtnFlash2.getVisibility() + " ,"
				+ mBtnFlash3.getVisibility());
	}

	protected void onOrientationChanged(int orientation) {
		if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
			return;
		}
		int diff = Math.abs(orientation - current_orientation);
		if (diff > 180) {
			diff = 360 - diff;
		}
		// only change orientation when sufficiently changed
		if (diff > 60) {
			orientation = (orientation + 45) / 90 * 90;
			if (orientation != current_orientation) {
				current_orientation = orientation;
				if (mPreviewing) {
					picture_orientation = orientation;
				}
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
					setViewRotation();
				}
			}
		}
	}

	private void setViewRotation() {
		int orientation = (360 - current_orientation) % 360;
		mBtnCancel.setRotation(orientation);
		mBtnHandler.setRotation(orientation);
		mBtnTakePic.setRotation(orientation);
		mBtnChangeFacing.setRotation(orientation);
		mBtnFlash1.setRotation(orientation);
		mBtnFlash2.setRotation(orientation);
		mBtnFlash3.setRotation(orientation);
		mIvPreShow.setRotation(orientation);
	}

	private void updateFlashButtonState(int index) {
		isOpenFlash = false;
		mHandler.removeMessages(NEED_CLOSE_FLASH_BTS);
		mBtnFlash2.setVisibility(View.INVISIBLE);
		mBtnFlash3.setVisibility(View.INVISIBLE);

		Integer i = flashDrawableIds.get(index);
		flashDrawableIds.remove(index);
		flashDrawableIds.add(0, i);
		mBtnFlash1.setBackgroundResource(flashDrawableIds.get(0));
		mBtnFlash2.setBackgroundResource(flashDrawableIds.get(1));
		mBtnFlash3.setBackgroundResource(flashDrawableIds.get(2));
		checkFlash(i);

	}

	private void checkFlash(Integer i) {
		int j = i;
		try {
			Parameters par = mCamera.getParameters();
			if (j == CRes.plugin_camera_flash_drawale_auto) {
				par.setFlashMode(Parameters.FLASH_MODE_AUTO);
			} else if (j == CRes.plugin_camera_flash_drawale_open) {
				par.setFlashMode(Parameters.FLASH_MODE_ON);
			} else if (j == CRes.plugin_camera_flash_drawale_close) {
				par.setFlashMode(Parameters.FLASH_MODE_OFF);
			}
			mCamera.setParameters(par);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mHandleTask != null) {
			if (mHandleTask.cancel(true))
				;
		}
		if (mCamera != null) {
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
			mPreviewing = false;
			Log.d("mPreviewing", "mPreviewing changed to :" + mPreviewing);

		}
		orientationEventListener.disable();
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();
		SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
		if (hasSurface) {
			initCamera(surfaceHolder);
		} else {
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
		orientationEventListener.enable();
	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		try {
			if (null == mCamera) {
				mCamera = Camera.open();
			}
			if (null == mCamera) {
				throw new RuntimeException("camera error!");
			}
			initCameraParameters();
			mCamera.setPreviewDisplay(surfaceHolder);
			mCamera.startPreview();
			mPreviewing = true;
			Log.d("mPreviewing", "after inti camera mPreviewing changed to :" + mPreviewing);
			mCamera.autoFocus(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initCameraParameters() {
		Camera.Parameters parameters = mCamera.getParameters();
		String mod = Build.MODEL;
		if (Build.VERSION.SDK_INT >= 8) {
			// MZ 180， other 90...
			if ("M9".equalsIgnoreCase(mod) || "MX".equalsIgnoreCase(mod)) {
				setDisplayOrientation(mCamera, 180);
			} else {
				setDisplayOrientation(mCamera, 90);
			}
		} else {
			parameters.set("orientation", "portrait");
			parameters.set("rotation", 90);
		}

		if (cameraCurrentlyLocked == CameraInfo.CAMERA_FACING_FRONT) {
			ismCameraCanFlash = false;
		} else {
			ismCameraCanFlash = true;
		}
		if (!ismCameraCanFlash) {
			mBtnFlash1.setVisibility(View.INVISIBLE);
			mBtnFlash2.setVisibility(View.INVISIBLE);
			mBtnFlash3.setVisibility(View.INVISIBLE);
		} else {
			mBtnFlash1.setVisibility(View.VISIBLE);
		}

		List<String> focusModes = parameters.getSupportedFocusModes();
		if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
			// Autofocus mode is supported
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
		}

		if (parameters.isZoomSupported()) {
			parameters.setZoom(parameters.getZoom());// 测试通过
		}
		Camera.Size previewSize = getFitParametersSize(parameters.getSupportedPreviewSizes());
		parameters.setPreviewSize(previewSize.width, previewSize.height);
		Camera.Size pictureSize = getFitParametersSize(parameters.getSupportedPictureSizes());
		parameters.setPictureSize(pictureSize.width, pictureSize.height);

		try {
			mCamera.setParameters(parameters);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Camera.Size getFitParametersSize(List<Camera.Size> sizes) {
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		double dmFormat = getFormat(dm.heightPixels, dm.widthPixels);
		for (Camera.Size size : sizes) {
			double abs = Math.abs(dmFormat - getFormat(size.width, size.height));
			if (abs == 0.0d || abs == 0.1d) {
				return size;
			}
		}
		return sizes.get(0);
	}

	private double getFormat(int formatX, int formatY) {
		DecimalFormat format = new DecimalFormat("#.0");
		double result = Double.parseDouble(format.format((double) formatX / (double) formatY));
		return result;
	}

	private void setDisplayOrientation(Camera camera, int angle) {
		Method downPolymorphic;
		try {
			downPolymorphic = camera.getClass().getMethod("setDisplayOrientation", new Class[] { int.class });
			if (downPolymorphic != null) {
				downPolymorphic.invoke(camera, new Object[] { angle });
			}
		} catch (Exception e1) {
		}
	}

	@SuppressWarnings("unused")
	private Camera.Size computeNeedSize(List<Camera.Size> sizes) {
		if (null == sizes || 0 == sizes.size()) {
			return null;
		}
		DisplayMetrics dm = getResources().getDisplayMetrics();
		Camera.Size best = null;
		int screenPix = dm.widthPixels * dm.heightPixels;
		for (Camera.Size size : sizes) {
			int sizeOne = size.width * size.height;
			if (sizeOne == screenPix) {
				best = size;
				break;
			}
		}
		if (null == best) {
			int length = sizes.size();
			if (2 >= length) {
				best = sizes.get(0);
			} else {
				int harf = length / 2 + 1;
				best = sizes.get(harf);
			}
		}
		return best;
	}

	private boolean mOnKeyDown;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			mOnKeyDown = true;
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mOnKeyDown) {
				finish();
			}
			mOnKeyDown = false;
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;
	}

	PictureCallback jpeg = new PictureCallback() {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			Log.d("run", "into picture call back");
			mHandleTask = new HandlePicAsyncTask();
			mHandleTask.execute(data);
			Log.d("run", "execute asynctask");
		}
	};

	private class HandlePicAsyncTask extends AsyncTask<byte[], integer, Bitmap> {

		@Override
		protected Bitmap doInBackground(byte[]... params) {
			Log.d("run", "background  start run");
			Bitmap bm = null;
			final byte[] data = params[0];
			saveImage(data);
			bm = createThumbnail(data);
			return bm;
		}

		@Override
		protected void onPostExecute(Bitmap bm) {
			super.onPostExecute(bm);
			Log.d("run", "postexecute  start run");
			setIvPreShowBitmap(bm);
			mCamera.startPreview();
			mPreviewing = true;
			Log.d("mPreviewing", " after take pic mPreviewing changed to :" + mPreviewing);
			Log.d("run", "postexecute  end run");
		}
	}

	private void setIvPreShowBitmap(Bitmap bm) {
		if (bm != null) {
			mIvPreShow.setScaleType(ScaleType.CENTER_CROP);
			mIvPreShow.setImageBitmap(bm);
			isHasPic = true;
			mIvPreShow.setVisibility(View.VISIBLE);
		} else {
			Toast.makeText(CustomCameraActivity.this, "拍照失败", Toast.LENGTH_SHORT).show();
		}
	}

	private int getRotate() {
		CameraInfo info = new CameraInfo();
		Camera.getCameraInfo(cameraCurrentlyLocked, info);
		int r = 0;
		if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
			r = (info.orientation - picture_orientation + 360) % 360;
		} else {
			r = (info.orientation + picture_orientation) % 360;
		}
		return r;
	}

	private Bitmap rotateImage(Bitmap bitmap) {
		Matrix m = new Matrix();
		m.setRotate(getRotate(), bitmap.getWidth() * 0.5f, bitmap.getHeight() * 0.5f);
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

	public Bitmap createThumbnail(byte[] data) {
		Bitmap bm = null;
		try {
			int width = mCamera.getParameters().getPictureSize().width;
			int previewWidth = mIvPreShow.getWidth();
			int ratio = (int) Math.ceil((double) width / previewWidth);
			int inSampleSize = Integer.highestOneBit(ratio);
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = inSampleSize;
			bm = BitmapFactory.decodeByteArray(data, 0, data.length, options);
			bm = rotateImage(bm);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bm;
	}

	private void saveImage(byte[] data) {
		try {
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inJustDecodeBounds = true;
			BitmapFactory.decodeByteArray(data, 0, data.length, opts);
			DisplayMetrics dm = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(dm);
			opts.inSampleSize = calculateInSampleSize(opts, dm.heightPixels, dm.widthPixels);
			opts.inPurgeable = true;
			opts.inInputShareable = true;
			opts.inTempStorage = new byte[64 * 1024];
			opts.inJustDecodeBounds = false;
			Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length, opts);
			if (bm != null) {
				bm = Util.rotate(bm, getRotate());
				File file = new File(filePath);
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
				bm.compress(Bitmap.CompressFormat.JPEG, 100, bos);
				bos.flush();
				bos.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
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

	@SuppressWarnings("unused")
	private int mOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;

	public static int roundOrientation(int orientation) {
		return ((orientation + 45) / 90 * 90) % 360;
	}

	@Override
	public void onAutoFocus(boolean success, Camera camera) {
		if (success) {
			mode = MODE.FOCUSED;
			view_focus.setBackgroundResource(CRes.plugin_camera_view_focused_bg);
		} else {
			mode = MODE.FOCUSFAIL;
			view_focus.setBackgroundResource(CRes.plugin_camera_view_focus_fail_bg);
		}
		setFocusView();
	}

	private void setFocusView() {
		new Handler().postDelayed(new Runnable() {

			@Override
			public void run() {
				view_focus.setVisibility(View.INVISIBLE);
			}
		}, 1 * 1000);
	}

	/**
	 * 点击显示焦点区域
	 */
	OnTouchListener onTouchListener = new OnTouchListener() {

		@SuppressLint("ClickableViewAccessibility")
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				int width = view_focus.getWidth();
				int height = view_focus.getHeight();
				view_focus.setBackgroundResource(CRes.plugin_camera_view_focusing_bg);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
					view_focus.setX(event.getX() - (width / 2));
					view_focus.setY(event.getY() - (height / 2));
				}
				view_focus.setVisibility(View.VISIBLE);
			} else if (event.getAction() == MotionEvent.ACTION_UP) {
				mode = MODE.FOCUSING;
				focusOnTouch(event);
			}
			return true;
		}
	};

	/**
	 * 设置焦点和测光区域
	 * 
	 * @param event
	 */
	public void focusOnTouch(MotionEvent event) {
		try {
			if (mCamera == null) {
				return;
			}
			Camera.Parameters parameters = mCamera.getParameters();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				int[] location = new int[2];
				mSurfaceView.getLocationOnScreen(location);
				Rect focusRect = calculateTapArea(view_focus.getWidth(), view_focus.getHeight(), 1f, event.getRawX(),
						event.getRawY(), location[0], location[0] + mSurfaceView.getWidth(), location[1],
						location[1] + mSurfaceView.getHeight());
				Rect meteringRect = calculateTapArea(view_focus.getWidth(), view_focus.getHeight(), 1.5f,
						event.getRawX(), event.getRawY(), location[0], location[0] + mSurfaceView.getWidth(),
						location[1], location[1] + mSurfaceView.getHeight());
				if (parameters.getMaxNumFocusAreas() > 0) {
					List<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
					focusAreas.add(new Camera.Area(focusRect, 1000));
					parameters.setFocusAreas(focusAreas);
				}

				if (parameters.getMaxNumMeteringAreas() > 0) {
					List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
					meteringAreas.add(new Camera.Area(meteringRect, 1000));
					parameters.setMeteringAreas(meteringAreas);
				}
			}

			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
			mCamera.setParameters(parameters);
			mCamera.autoFocus(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 计算焦点及测光区域
	 * 
	 * @param focusWidth
	 * @param focusHeight
	 * @param areaMultiple
	 * @param x
	 * @param y
	 * @param previewleft
	 * @param previewRight
	 * @param previewTop
	 * @param previewBottom
	 * @return Rect(left,top,right,bottom) : left、top、right、bottom是以显示区域中心为原点的坐标
	 */
	public Rect calculateTapArea(int focusWidth, int focusHeight, float areaMultiple, float x, float y, int previewleft,
			int previewRight, int previewTop, int previewBottom) {
		int areaWidth = (int) (focusWidth * areaMultiple);
		int areaHeight = (int) (focusHeight * areaMultiple);
		int centerX = (previewleft + previewRight) / 2;
		int centerY = (previewTop + previewBottom) / 2;
		double unitx = ((double) previewRight - (double) previewleft) / 2000;
		double unity = ((double) previewBottom - (double) previewTop) / 2000;
		int left = clamp((int) (((x - areaWidth / 2) - centerX) / unitx), -1000, 1000);
		int top = clamp((int) (((y - areaHeight / 2) - centerY) / unity), -1000, 1000);
		int right = clamp((int) (left + areaWidth / unitx), -1000, 1000);
		int bottom = clamp((int) (top + areaHeight / unity), -1000, 1000);

		return new Rect(left, top, right, bottom);
	}

	public int clamp(int x, int min, int max) {
		if (x > max)
			return max;
		if (x < min)
			return min;
		return x;
	}

	/**
	 * 模式 NONE：无 FOCUSING：正在聚焦. FOCUSED:聚焦成功 FOCUSFAIL：聚焦失败
	 * 
	 * 
	 */
	private enum MODE {
		NONE, FOCUSING, FOCUSED, FOCUSFAIL
	}
}
