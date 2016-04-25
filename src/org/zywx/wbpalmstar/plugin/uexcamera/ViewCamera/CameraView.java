package org.zywx.wbpalmstar.plugin.uexcamera.ViewCamera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.zywx.wbpalmstar.engine.universalex.EUExUtil;
import org.zywx.wbpalmstar.plugin.uexcamera.EUExCamera;
import org.zywx.wbpalmstar.plugin.uexcamera.LogUtils;
import org.zywx.wbpalmstar.plugin.uexcamera.Util;
import org.zywx.wbpalmstar.plugin.uexcamera.utils.BitmapUtil;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Bitmap.Config;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 自定义View照相机
 * 
 * @author waka
 *
 */
public class CameraView extends RelativeLayout implements Callback, View.OnClickListener, AutoFocusCallback {

	private static final String TAG = "CameraView";

	// 外部传入
	private Context mContext;// 上下文
	private EUExCamera mEuExCamera;// 需要传入EUExCamera实例
	private String filePath;// 文件储存路径
	private CallbackCameraViewClose callbackClose;// 上下文removeView的回调

	// View
	private SurfaceView mSurfaceView;// 预览的SurfaceView
	private SurfaceHolder mSurfaceHolder;
	private Button btnTakePhoto, btnClose, btnDrawLine, btnOverturnCamera, btnFlash;// 各种按钮
	private TextView tvLabel;// 位置文本
	private View viewFocus;// 聚焦视图View

	// Camera信息
	private Camera mCamera;// Camera实例
	private CameraInfo cameraInfo;// 摄像头信息
	private boolean isCameraTakingPhoto = false;// camera正在照相标识
	private int cameraCount;// 摄像头数量
	private boolean isHasFrontCamera = false;// 是否有前置摄像头
	private boolean isHasBackCamera = false;// 是否有后置摄像头
	public static int cameraPosition = 0;// 摄像头位置，0代表后置，1代表前置，默认为0
	private int flashMode = 0;// 闪光灯状态，0为自动，1为打开，2为关闭，默认为自动

	// 图片处理信息
	private int quality = 60;// 图片质量，默认为60%
	private int inSampleSize = 1;// 压缩比，初始为1
	private int screenWidth;// 屏幕宽
	private int screenHeight;// 屏幕高
	private int resolutionWidth;// camera支持和屏幕最佳适配分辨率的宽
	private int resolutionheight;// camera支持和屏幕最佳适配分辨率的高

	// 方向
	private OrientationEventListener orientationEventListener = null;// 方向监听器
	private int currentOrientation = 0;// 预览的方向
	private int pictureOrientation = 0;// 图片的方向

	// 聚焦
	private MODE mode = MODE.NONE;// 默认聚焦模式

	private enum MODE {
		NONE, FOCUSING, FOCUSED, FOCUSFAIL// NONE：无 FOCUSING：正在聚焦. FOCUSED:聚焦成功
											// FOCUSFAIL：聚焦失败
	}

	/**
	 * 照相动作回调用的pictureCallback，在这里可以获得拍照后的图片数据
	 */
	private Camera.PictureCallback pictureCallback = new PictureCallback() {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {

			if (data.length != 0) {

				Log.i(TAG, "quality onPictureTaken---->" + quality);

				String fileName = savePhoto(data, quality);// 保存bitmap，压缩程度为60

				if (null == fileName) {
					Toast.makeText(mContext, "保存图片失败", Toast.LENGTH_SHORT).show();
					return;
				}

				int degree = getDegreeByFilePath(fileName);// 通过文件路径获得图片的degree
				if (degree > 0) {// 如果degree大于0，旋转图片
					// 删除原来的图片
					File file = new File(fileName);
					file.delete();
					// 重新生成新的图片
					fileName = savePhoto(data, quality, degree);
				}

				// 跳转到第二个Activity，携带着文件路径
				Intent intent = new Intent(mContext, SecondActivity.class);
				intent.putExtra("label", tvLabel.getText().toString());
				intent.putExtra("fileName", fileName);
				if (mEuExCamera != null) {
					mEuExCamera.startActivityForResult(intent, 68);
				} else {
					Toast.makeText(mContext, "跳转失败！", Toast.LENGTH_SHORT).show();
				}
				isCameraTakingPhoto = false;// camera活干完了，可以继续按了
			}
			camera.startPreview();// 重新预览
		}
	};

	/**
	 * 用一个参数的构造方法调用两个参数的构造方法，简化代码
	 */
	public CameraView(Context context) {
		this(context, null);// 调用两个参数的构造方法
	}

	/**
	 * 在两个参数的构造方法里初始化
	 * 
	 * @param context
	 * @param attrs
	 */
	public CameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.mContext = context;
		initView();
		initData();
		initEvent();
	}

	/**
	 * initView
	 */
	private void initView() {

		// 加载布局
		LayoutInflater.from(mContext).inflate(EUExUtil.getResLayoutID("plugin_camera_layout_camera_view"), this, true);// 将当前view关联xml文件

		// 初始化View
		mSurfaceView = (SurfaceView) findViewById(EUExUtil.getResIdID("plugin_camera_surfaceView"));
		tvLabel = (TextView) findViewById(EUExUtil.getResIdID("plugin_camera_tvLocation"));
		btnTakePhoto = (Button) findViewById(EUExUtil.getResIdID("plugin_camera_btnTakePhoto"));
		btnClose = (Button) findViewById(EUExUtil.getResIdID("plugin_camera_btnClose"));
		btnDrawLine = (Button) findViewById(EUExUtil.getResIdID("plugin_camera_btnDrawLine"));
		btnOverturnCamera = (Button) findViewById(EUExUtil.getResIdID("plugin_camera_btnOverturnCamera"));
		btnFlash = (Button) findViewById(EUExUtil.getResIdID("plugin_camera_btnFlash"));
		viewFocus = (View) findViewById(EUExUtil.getResIdID("plugin_camera_view_focus"));
	}

	/**
	 * initData
	 */
	private void initData() {

		// 获得文件存放的路径
		// filePath = mEuExCamera.filePath;

		// 实例化SurfaceHolder
		mSurfaceHolder = mSurfaceView.getHolder();

		// 得到摄像头的信息
		cameraInfo = new CameraInfo();
		cameraCount = Camera.getNumberOfCameras();// 得到摄像头的数量

		// 判断设备是否有前后置摄像头
		for (int i = 0; i < cameraCount; i++) {
			Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {// 如果是后置摄像头
				isHasBackCamera = true;
			} else if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {// 如果是前置摄像头
				isHasFrontCamera = true;
			}
		}

		// 获取屏幕分辨率
		Point point = new Point();
		WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

		// 不含虚拟按键
		windowManager.getDefaultDisplay().getSize(point);
		// 包含虚拟按键,API Level 17 +
		// windowManager.getDefaultDisplay().getRealSize(point);

		screenWidth = point.x;
		screenHeight = point.y;

	}

	/**
	 * initEvent
	 */
	private void initEvent() {

		mSurfaceHolder.addCallback(this);// 实现surfaceView回调
		btnTakePhoto.setOnClickListener(this);
		btnClose.setOnClickListener(this);
		btnDrawLine.setOnClickListener(this);
		btnOverturnCamera.setOnClickListener(this);
		btnFlash.setOnClickListener(this);

		// 设置方向监听器事件
		orientationEventListener = new OrientationEventListener(mContext) {
			@Override
			public void onOrientationChanged(int orientation) {

				// 如果位置是未知位置，return
				if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
					return;
				}
				int diff = Math.abs(orientation - currentOrientation);
				if (diff > 180) {
					diff = 360 - diff;
				}

				// 只有当充分改变时才改变方向
				if (diff > 60) {
					orientation = (orientation + 45) / 90 * 90;
					if (orientation != currentOrientation) {
						currentOrientation = orientation;
						setViewRotation();// 设置控件的方向
					}
				}
			}
		};
		orientationEventListener.enable();// 开始监听方向
	}

	/**
	 * 设置EUExCamera的实例
	 * 
	 * @param mEuExCamera
	 */
	public void setmEuExCamera(EUExCamera mEuExCamera) {
		this.mEuExCamera = mEuExCamera;
	}

	/**
	 * 设置文件存储路径
	 * 
	 * @param filePath
	 */
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	/**
	 * 当View被移除时
	 */
	@Override
	protected void onDetachedFromWindow() {
		orientationEventListener.disable();// 取消监听方向
		super.onDetachedFromWindow();
	}

	/**
	 * surfaceCreated，SurfaceView生命周期的开始，在这里打开相机
	 * 
	 * @param holder
	 */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (cameraPosition == 1) {
			mCamera = Camera.open(CameraInfo.CAMERA_FACING_FRONT);
		} else {
			try {
				mCamera = Camera.open();// 初始化camera
			} catch (RuntimeException e) {
				Toast.makeText(mContext, "相机打开失败，请检查内存是否充足，是否允许程序调用摄像头", Toast.LENGTH_SHORT).show();
				e.printStackTrace();
			}
		}
		try {

			if (mCamera == null) {

				Log.i(TAG, "surfaceCreated	camera == null");
				return;
			}
			mCamera.setPreviewDisplay(mSurfaceHolder);// 设置holder主要是用于surfaceView的图片的实时预览，以及获取图片等功能，可以理解为控制camera的操作

			Camera.Parameters parameters = mCamera.getParameters();// 得到一个已有的(默认的)设置

			// 个别设备需要翻转180度，大部分旋转90度
			String mod = Build.MODEL;// 获得设备型号
			// if ("M9".equalsIgnoreCase(mod) || "MX".equalsIgnoreCase(mod)) {
			// camera.setDisplayOrientation(180);
			// } else {
			// camera.setDisplayOrientation(90);
			// }
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

			getBestResolution(parameters);// 获得最佳分辨率
			parameters.setPreviewSize(resolutionheight, resolutionWidth);// 用算出的误差最小的支持分辨率作为预览分辨率
			// parameters.setRotation(90);
			parameters.setFlashMode(Parameters.FLASH_MODE_OFF);// 设置默认闪光灯模式为关
			mCamera.setParameters(parameters);
			mCamera.startPreview();// 开始预览
		} catch (IOException e) {
			// 在异常处理里释放camera并置为null
			mCamera.release();
			mCamera = null;
			e.printStackTrace();
		}
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

	/**
	 * surfaceChanged，在这里预览图像
	 * 
	 * @param holder
	 * @param format
	 * @param width
	 * @param height
	 */
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

	}

	/**
	 * surfaceDestroyed,SurfaceView生命周期的结束，在这里关闭相机
	 * 
	 * @param holder
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

		if (mCamera == null) {

			Log.e(TAG, "surfaceDestroyed	camera == null");
			return;
		}

		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;
	}

	/**
	 * 触摸屏幕进行对焦
	 */
	@SuppressLint("ClickableViewAccessibility") // touch事件可能会拦截click事件
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			int width = viewFocus.getWidth();
			int height = viewFocus.getHeight();
			viewFocus.setBackgroundResource(EUExUtil.getResDrawableID("plugin_camera_focus_focusing"));
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				viewFocus.setX(event.getX() - (width / 2));
				viewFocus.setY(event.getY() - (height / 2));
			}
			viewFocus.setVisibility(View.VISIBLE);
		}
		if (event.getAction() == MotionEvent.ACTION_UP) {
			mode = MODE.FOCUSING;
			focusOnTouch(event);
		}
		return true;// 返回true，让点击事件可以被监听
	}

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
				Rect focusRect = calculateTapArea(viewFocus.getWidth(), viewFocus.getHeight(), 1f, event.getRawX(),
						event.getRawY(), location[0], location[0] + mSurfaceView.getWidth(), location[1],
						location[1] + mSurfaceView.getHeight());
				Rect meteringRect = calculateTapArea(viewFocus.getWidth(), viewFocus.getHeight(), 1.5f, event.getRawX(),
						event.getRawY(), location[0], location[0] + mSurfaceView.getWidth(), location[1],
						location[1] + mSurfaceView.getHeight());
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

	/** 对焦时进行的详细操作 **/
	@Override
	public void onAutoFocus(boolean success, Camera camera) {
		if (success) {
			mode = MODE.FOCUSED;
			// handler.sendEmptyMessage(1);
			viewFocus.setBackgroundResource(EUExUtil.getResDrawableID("plugin_camera_focus_focused"));
		} else {
			mode = MODE.FOCUSFAIL;
			// handler.sendEmptyMessage(2);
			viewFocus.setBackgroundResource(EUExUtil.getResDrawableID("plugin_camera_focus_failed"));
		}
		// 让对焦框在1s后消失
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				viewFocus.setVisibility(View.INVISIBLE);
			}
		}, 1 * 1000);
	}

	/**
	 * 按钮点击事件
	 */
	@Override
	public void onClick(View v) {
		/** 照相 **/
		if (v.getId() == EUExUtil.getResIdID("plugin_camera_btnTakePhoto")) {
			// 如果焦点获取失败或者正在获取或者camera对象==null,return
			if (mode == MODE.FOCUSING || mCamera == null) {
				return;
			}
			// 如果照相机没有正在拍照，则拍照；因为连续按拍照键会报错，所以得设置这样一个标志位
			Log.i("uexCamera", "isCameraTakingPhoto---->" + isCameraTakingPhoto);
			if (isCameraTakingPhoto == false) {
				// 照相前判定如果是前置摄像头，则将照片位置旋转270度，必须这样做要不会前置摄像头重拍时会图像位置会发生颠倒
				if (cameraPosition == 1) {
					Camera.Parameters parameters = mCamera.getParameters();
					parameters.setRotation(270);
					mCamera.setParameters(parameters);
				}
				isCameraTakingPhoto = true;
				mCamera.takePicture(null, null, pictureCallback);// 拍照
			}
		}
		/** 关闭 **/
		else if (v.getId() == EUExUtil.getResIdID("plugin_camera_btnClose")) {
			callbackClose.callbackClose();// 点击关闭，通过回调，调用上下文的removeView

		}
		/** 画线 **/
		else if (v.getId() == EUExUtil.getResIdID("plugin_camera_btnDrawLine")) {

		}
		/** 翻转摄像头 **/
		else if (v.getId() == EUExUtil.getResIdID("plugin_camera_btnOverturnCamera")) {
			if (isHasBackCamera && isHasFrontCamera) {
				overturnCamera();
				// 如果是后置摄像头，设置闪光灯模式
				if (cameraPosition == 1) {
					setFlashMode(flashMode);
				}
			} else {
				Toast.makeText(mContext, "您的设备不支持摄像头翻转", Toast.LENGTH_LONG).show();
			}
		}
		/** 闪光灯状态 **/
		else if (v.getId() == EUExUtil.getResIdID("plugin_camera_btnFlash")) {
			// 如果是前置摄像头，闪光灯无效
			if (cameraPosition == 1) {
				// flashMode闪光灯模式一直以0,1,2循环
				flashMode++;
				flashMode = flashMode % 3;
				setFlashMode(flashMode);// 设置闪光灯模式
			}
		}
	}

	/**
	 * 得到旋转图片的角度
	 * 
	 * @return
	 */
	private int getRotate() {
		CameraInfo info = new CameraInfo();
		Camera.getCameraInfo(cameraPosition, info);
		int r = 0;
		if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
			r = (info.orientation - pictureOrientation + 360) % 360;
		} else {
			r = (info.orientation + pictureOrientation) % 360;
		}
		return r;
	}

	/**
	 * 保存照片
	 * 
	 * @param data
	 * @param quality
	 * @return
	 */
	public String savePhoto(byte[] data, int quality) {

		// 压缩图片
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;// 不返回实际的bitmap，也不给其分配内存空间,但是允许我们查询图片的信息这其中就包括图片大小信息
		@SuppressWarnings("unused")
		Bitmap boundBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);// 从字节流中生成一个不占内存的bitmap
		inSampleSize = BitmapUtil.calculateInSampleSize(options, getWidth(), getHeight());// 根据CameraView宽高计算压缩比
		options.inSampleSize = inSampleSize;
		options.inPurgeable = true;
		options.inInputShareable = true;
		options.inTempStorage = new byte[64 * 1024];
		options.inJustDecodeBounds = false;// decode到的bitmap将写入内存
		boundBitmap = null;// 将不占内存的boundBitmap置为null

		Bitmap bitmap = null;
		FileOutputStream outputStream = null;
		try {

			// 生成并旋转bitmap
			bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);// 生成bitmap
			if (bitmap == null) {
				Log.i(TAG, "bitmap is null!");
				return null;
			}
			bitmap = Util.rotate(bitmap, getRotate());// 旋转bitmap

			// 写入文件
			File file = new File(filePath);
			if (!file.exists()) {
				file.mkdirs();// 如果不存在，则创建所有的父文件夹
			}
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);// Date格式转换为字符串
			String fileName = filePath + "/" + simpleDateFormat.format(new Date()) + ".jpg";// 图片文件名
			outputStream = new FileOutputStream(fileName);
			bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);// 压缩图片，并放在输出流中

			return fileName;

		} catch (OutOfMemoryError e) {

			e.printStackTrace();
			LogUtils.o(e.toString());
			Toast.makeText(mContext, "内存要炸了！！！", Toast.LENGTH_LONG).show();

			inSampleSize = inSampleSize * 2;// 压缩比增加
			return savePhoto(data, quality);// 继续压缩

		} catch (FileNotFoundException e) {

			e.printStackTrace();
			LogUtils.o(e.toString());

		} finally {

			inSampleSize = 1;// 回归正常压缩比

			if (outputStream != null) {
				try {
					outputStream.close();// 关闭输出流
					outputStream = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (bitmap != null) {
				bitmap.recycle();// 回收bitmap
			}
			System.gc();// 提示垃圾回收线程
		}
		return null;
	}

	/**
	 * 保存照片，根据degree保存
	 * 
	 * @param data
	 * @param quality
	 * @return
	 */
	public String savePhoto(byte[] data, int quality, int degree) {

		// 压缩图片
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;// 不返回实际的bitmap，也不给其分配内存空间,但是允许我们查询图片的信息这其中就包括图片大小信息
		@SuppressWarnings("unused")
		Bitmap boundBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);// 从字节流中生成一个不占内存的bitmap
		inSampleSize = BitmapUtil.calculateInSampleSize(options, getWidth(), getHeight());// 根据CameraView宽高计算压缩比
		options.inSampleSize = inSampleSize;
		options.inPurgeable = true;
		options.inInputShareable = true;
		options.inTempStorage = new byte[64 * 1024];
		options.inJustDecodeBounds = false;// decode到的bitmap将写入内存
		boundBitmap = null;// 将不占内存的boundBitmap置为null

		Bitmap bitmap = null;
		FileOutputStream outputStream = null;
		try {

			// 生成并旋转bitmap
			bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);// 生成bitmap
			if (bitmap == null) {
				Log.i(TAG, "bitmap is null!");
				return null;
			}
			bitmap = Util.rotate(bitmap, getRotate());// 旋转bitmap
			if (degree > 0) {
				bitmap = Util.rotate(bitmap, degree);
			}

			// 写入文件
			File file = new File(filePath);
			if (!file.exists()) {
				file.mkdirs();// 如果不存在，则创建所有的父文件夹
			}
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);// Date格式转换为字符串
			String fileName = filePath + "/" + simpleDateFormat.format(new Date()) + ".jpg";// 图片文件名
			outputStream = new FileOutputStream(fileName);
			bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);// 压缩图片，并放在输出流中

			return fileName;

		} catch (OutOfMemoryError e) {

			e.printStackTrace();
			LogUtils.o(e.toString());
			Toast.makeText(mContext, "内存要炸了！！！", Toast.LENGTH_LONG).show();

			inSampleSize = inSampleSize * 2;// 压缩比增加
			return savePhoto(data, quality);// 继续压缩

		} catch (FileNotFoundException e) {

			e.printStackTrace();
			LogUtils.o(e.toString());

		} finally {

			inSampleSize = 1;// 回归正常压缩比

			if (outputStream != null) {
				try {
					outputStream.close();// 关闭输出流
					outputStream = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (bitmap != null) {
				bitmap.recycle();// 回收bitmap
			}
			System.gc();// 提示垃圾回收线程
		}
		return null;
	}

	/**
	 * 获得最佳分辨率
	 */
	private void getBestResolution(Camera.Parameters parameters) {
		List<Size> list = parameters.getSupportedPreviewSizes();
		int distance = 4000;
		// surfaceView的宽度和高度依次减去该设备支持的分辨率，得到两个差值的绝对值
		for (int i = 0; i < list.size(); i++) {
			int distance_width = Math.abs(screenWidth - list.get(i).height);
			int distance_height = Math.abs(screenHeight - list.get(i).width);
			// 找到最小的差值绝对值，将它的宽度和高度设为新的width_new和height_new
			if ((distance_width + distance_height) < distance) {
				distance = distance_width + distance_height;
				resolutionWidth = list.get(i).height;// 注意获得到设备所支持的分辨率是反的
				resolutionheight = list.get(i).width;
			}
		}
		// Toast.makeText(context, "widthNew--->" + resolutionWidth,
		// Toast.LENGTH_SHORT).show();
		// Toast.makeText(context, "heightNew--->" + resolutionheight,
		// Toast.LENGTH_SHORT).show();
	}

	/**
	 * 设置地理位置
	 * 
	 * @param string
	 */
	public void setLabelText(String string) {
		tvLabel.setText(string);
	}

	/**
	 * 根据当前手机方向，旋转各个按钮的方向
	 */
	private void setViewRotation() {
		int orientation = (360 - currentOrientation) % 360;
		btnTakePhoto.setRotation(orientation);
		btnClose.setRotation(orientation);
		btnDrawLine.setRotation(orientation);
		btnOverturnCamera.setRotation(orientation);
		btnFlash.setRotation(orientation);
	}

	/**
	 * 旋转Bitmap
	 * 
	 * @param bitmap
	 * @param orientation
	 * @return
	 */
	@SuppressWarnings("unused")
	private Bitmap rotateBitmap(Bitmap bitmap, int orientation) {

		Matrix matrix = new Matrix();
		Log.i(TAG, "cameraPosition" + cameraPosition);
		// 若是前置摄像头，则需要调整方向
		if (cameraPosition == 1) {
			if (orientation == 270) {
				orientation = 90;
			} else if (orientation == 90) {
				orientation = 270;
			}
		}
		matrix.setRotate(orientation, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
		try {
			Bitmap bitmapRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix,
					true);
			if (bitmap != bitmapRotated) {
				bitmap.recycle();// 回收旋转前的bitmap
			}
			return bitmapRotated;
		} catch (OutOfMemoryError outOfMemoryError) {// catch内存溢出错误
			Toast.makeText(mContext, "内存要炸了！！！", Toast.LENGTH_SHORT).show();
			outOfMemoryError.printStackTrace();
			return null;
		}
	}

	/**
	 * 通过文件路径获得degree
	 * 
	 * @param filePath
	 * @return
	 */
	private int getDegreeByFilePath(String filePath) {
		ExifInterface exif = null;
		int degree = 0;
		try {
			exif = new ExifInterface(filePath);
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
			return degree;
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}

	/**
	 * 为图片添加文字水印
	 * 
	 * @param bitmap
	 * @param text
	 * @return
	 */
	@SuppressWarnings("unused")
	private Bitmap addWaterMarkText(Bitmap bitmap, String text) {
		if (bitmap == null) {
			return null;
		}
		if (!TextUtils.isEmpty(text)) {
			int width = bitmap.getWidth();
			int height = bitmap.getHeight();
			Bitmap newBitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
			Canvas canvas = new Canvas(newBitmap);
			canvas.drawBitmap(bitmap, 0, 0, null);
			Paint textPaint = new Paint();
			textPaint.setColor(Color.BLACK);
			textPaint.setTextSize(40);
			Typeface typeface = Typeface.create("宋体", Typeface.NORMAL);
			textPaint.setTypeface(typeface);
			textPaint.setTextAlign(Align.CENTER);
			canvas.drawText(text, width / 2, height - 40, textPaint);
			canvas.save(Canvas.ALL_SAVE_FLAG);
			canvas.restore();
			return newBitmap;
		}
		return bitmap;
	}

	/**
	 * 关闭回调注册
	 */
	public void setCallbackCameraViewClose(CallbackCameraViewClose callbackCameraViewClose) {
		this.callbackClose = callbackCameraViewClose;
	}

	/**
	 * 翻转摄像头
	 */
	public void overturnCamera() {
		for (int i = 0; i < cameraCount; i++) {
			Camera.getCameraInfo(i, cameraInfo);// 得到每一个摄像头的信息
			if (cameraCount < 2) {
				return;
			}
			if (cameraPosition == 0) {
				// 现在是后置，变更为前置
				if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {// 代表摄像头的方位，CAMERA_FACING_FRONT前置CAMERA_FACING_BACK后置
					mCamera.stopPreview();// 停掉原来摄像头的预览
					mCamera.release();// 释放资源
					mCamera = null;// 取消原来摄像头
					mCamera = Camera.open(i);// 打开当前选中的摄像头
					try {
						mCamera.setDisplayOrientation(90);
						Camera.Parameters parameters = mCamera.getParameters();
						// 用算出的误差最小的支持分辨率作为预览分辨率
						parameters.setPreviewSize(resolutionheight, resolutionWidth);
						parameters.setRotation(270);
						mCamera.setParameters(parameters);
						mCamera.setPreviewDisplay(mSurfaceHolder);// 通过surfaceview显示取景画面
					} catch (IOException e) {
						e.printStackTrace();
					}
					mCamera.startPreview();// 开始预览
					cameraPosition = 1;
					break;
				}
			} else {
				// 现在是前置， 变更为后置
				if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {// 代表摄像头的方位，CAMERA_FACING_FRONT前置,CAMERA_FACING_BACK后置
					mCamera.stopPreview();// 停掉原来摄像头的预览
					mCamera.release();// 释放资源
					mCamera = null;// 取消原来摄像头
					mCamera = Camera.open(i);// 打开当前选中的摄像头
					try {
						mCamera.setDisplayOrientation(90);
						Camera.Parameters parameters = mCamera.getParameters();
						// 用算出的误差最小的支持分辨率作为预览分辨率
						parameters.setPreviewSize(resolutionheight, resolutionWidth);
						parameters.setRotation(90);
						mCamera.setParameters(parameters);
						mCamera.setPreviewDisplay(mSurfaceHolder);// 通过surfaceview显示取景画面
					} catch (IOException e) {
						e.printStackTrace();
					}
					mCamera.startPreview();// 开始预览
					cameraPosition = 0;
					break;
				}
			}
		}
	}

	/**
	 * 根据闪光灯flashMode设置闪光灯状态
	 * 
	 * @param flashMode
	 */
	public void setFlashMode(int flashMode) {
		Camera.Parameters parameters = mCamera.getParameters();
		switch (flashMode) {
		// 自动
		case 0:
			btnFlash.setBackgroundResource(EUExUtil.getResDrawableID("plugin_camera_flash_auto_selector"));
			parameters.setFlashMode(Parameters.FLASH_MODE_AUTO);
			mCamera.setParameters(parameters);
			break;
		// 开启
		case 1:
			btnFlash.setBackgroundResource(EUExUtil.getResDrawableID("plugin_camera_flash_open_selector"));
			parameters.setFlashMode(Parameters.FLASH_MODE_ON);
			mCamera.setParameters(parameters);
			break;
		// 关闭
		case 2:
			btnFlash.setBackgroundResource(EUExUtil.getResDrawableID("plugin_camera_flash_close_selector"));
			parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
			mCamera.setParameters(parameters);
			break;
		default:
			break;
		}
	}

	/**
	 * 设置图片质量
	 * 
	 * @param quality
	 */
	public void setQuality(int quality) {
		this.quality = quality;
	}

	/**
	 * 设置正在是否正在照相标志
	 * 
	 * @param isCameraTakingPhoto
	 */
	public void setCameraTakingPhoto(boolean isCameraTakingPhoto) {
		this.isCameraTakingPhoto = isCameraTakingPhoto;
	}

}
