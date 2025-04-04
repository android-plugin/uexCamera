package org.zywx.wbpalmstar.plugin.uexcamera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.Toast;

import org.zywx.wbpalmstar.plugin.uexcamera.utils.BitmapUtil;
import org.zywx.wbpalmstar.plugin.uexcamera.utils.CameraUtil;
import org.zywx.wbpalmstar.plugin.uexcamera.utils.CoordinateTransformer;
import org.zywx.wbpalmstar.plugin.uexcamera.utils.ExifUtil;
import org.zywx.wbpalmstar.plugin.uexcamera.utils.FileUtil;
import org.zywx.wbpalmstar.plugin.uexcamera.utils.ImageWatermarkUtil;
import org.zywx.wbpalmstar.plugin.uexcamera.utils.log.MLog;
import org.zywx.wbpalmstar.plugin.uexcamera.vo.CameraDisplayInfo;
import org.zywx.wbpalmstar.plugin.uexcamera.vo.OpenInternalVO;
import org.zywx.wbpalmstar.plugin.uexcamera.vo.PhotoSizeVO;
import org.zywx.wbpalmstar.plugin.uexcamera.vo.WatermarkOptionsVO;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 自定义相机Activity
 *
 * @author waka
 *
 */
public class CustomCameraActivity extends Activity implements Callback, AutoFocusCallback {

	private static final String TAG = "CustomCameraActivity";

	// View
	public SurfaceView mSurfaceView;
	private View mBottomBar;
	private Button mBtnCancel;
	private Button mBtnHandler;
	private Button mBtnTakePic;
	private Button mBtnChangeFacing;
	private Button mBtnFlash1;
	private Button mBtnFlash2;
	private Button mBtnFlash3;
	private Button mBtnFlash4;
	private ImageView mIvPreShow;

	private Button mBtnShowInfo;

	// Camera相关
	public Camera mCamera;
	public String filePath = null;// 照片保存路径
	public OpenInternalVO openInternalVO = null;// 额外参数
	private boolean hasSurface;
	private boolean isOpenFlash = true;
	public int cameraCurrentlyLocked;

	private byte[] mPictureBytesData;
	private ExecutorService mExecutorService;

	// The first rear facing camera
	private ArrayList<Integer> flashDrawableIds;
	private boolean mPreviewing = false;
	protected boolean isHasPic = false;
	private boolean isHasFrontCamera = false;// 是否有前置摄像头
	private boolean isHasBackCamera = false;
	private boolean ismCameraCanFlash = false;
	private View view_focus;
	private MODE mode = MODE.NONE;// 默认模式
	private final static int NEED_CLOSE_FLASH_BTS = 1;
	private final static int AUTO_FOCUS_AGAIN = 2;
	private final static int HIDE_AUTO_FOCUS_FRAME = 3;
	private OrientationEventListener orientationEventListener = null;
	private int current_orientation = 0;
	private int picture_orientation = 0;
	private int mCurrentZoom = -1;
	// 竖屏时相机旋转角度
	private final static int DEFAULT_ROTATE_DEGREE = 90;

	private boolean isUseLargerImageSize;

	private Dialog mLoadingDialog;

	private static class MyRunnable implements Runnable {
		private final WeakReference<CustomCameraActivity> mActivityRef;

		public MyRunnable(CustomCameraActivity activity) {
			super();
			mActivityRef = new WeakReference<CustomCameraActivity>(activity);
		}

		public WeakReference<CustomCameraActivity> getActivityRef() {
			return mActivityRef;
		}

		@Override
		public void run() {
		}
	}

	private static class MyHandler extends Handler {
		private final WeakReference<CustomCameraActivity> mActivityRef;

		public MyHandler(CustomCameraActivity activity) {
			super(Looper.getMainLooper());
			mActivityRef = new WeakReference<CustomCameraActivity>(activity);
		}

		@Override
		public void handleMessage(@NonNull Message msg) {
			super.handleMessage(msg);
			CustomCameraActivity activity = mActivityRef.get();
			if (activity == null || activity.isFinishing()) {
				MLog.getIns().e(TAG, "activity is null, maybe is finished.");
				return;
			}
			activity.handleHandlerMessage(msg);
		}
	}

	private MyHandler uiHandler;

	private String supportedFocusMode;
	private AlertDialog mInfoDialog;
	private CameraDisplayInfo mCameraInfo;

	/**
	 * 处理Handler消息，分离在static Handler外部
	 */
	private void handleHandlerMessage(@NonNull Message msg) {
		if (msg.what == NEED_CLOSE_FLASH_BTS) {
			try {
				if (isOpenFlash) {
					isOpenFlash = false;
					mBtnFlash2.setVisibility(View.INVISIBLE);
					mBtnFlash3.setVisibility(View.INVISIBLE);
					mBtnFlash4.setVisibility(View.INVISIBLE);
					Log.d("visible", " after close flash view,bts visible is" + mBtnFlash2.getVisibility() + " ,"
							+ mBtnFlash3.getVisibility() + " ,"
							+ mBtnFlash4.getVisibility());
				}
			} catch (Exception e) {
				MLog.getIns().e(TAG, e);
			}
		} else if (msg.what == AUTO_FOCUS_AGAIN) {
			try {
				// 执行自动对焦
				mCamera.autoFocus(CustomCameraActivity.this);
			} catch (Exception e) {
				MLog.getIns().e(TAG, e);
			}
		} else if (msg.what == HIDE_AUTO_FOCUS_FRAME) {
			view_focus.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		CRes.init(getApplication());

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		mExecutorService = Executors.newFixedThreadPool(1);
		uiHandler = new MyHandler(this);

		mCameraInfo = new CameraDisplayInfo();

		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(CRes.plugin_camera_layout);
		filePath = getIntent().getStringExtra(Constant.INTENT_EXTRA_NAME_PHOTO_PATH);
		openInternalVO = getIntent().getParcelableExtra(Constant.INTENT_EXTRA_OPTIONS);

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
		mBottomBar = (View) findViewById(CRes.bottombar);
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
		mBtnFlash4 = (Button) findViewById(CRes.plugin_camera_bt_flash4);
		mBtnShowInfo = (Button) findViewById(CRes.plugin_camera_bt_show_info);

		flashDrawableIds = new ArrayList<Integer>();
		flashDrawableIds.add(Integer.valueOf(CRes.plugin_camera_flash_drawale_auto));
		flashDrawableIds.add(Integer.valueOf(CRes.plugin_camera_flash_drawale_open));
		flashDrawableIds.add(Integer.valueOf(CRes.plugin_camera_flash_drawale_close));
		flashDrawableIds.add(Integer.valueOf(CRes.plugin_camera_flash_drawale_torch));

		mBtnFlash1.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (isOpenFlash) {
					updateFlashButtonState(0);
					Log.d("visible", " when open,after click flash1 view,bts visible is" + mBtnFlash2.getVisibility()
							+ " ," + mBtnFlash3.getVisibility() + " ," + mBtnFlash4.getVisibility());
				} else {
					isOpenFlash = true;
					mBtnFlash2.setVisibility(View.VISIBLE);
					mBtnFlash3.setVisibility(View.VISIBLE);
					mBtnFlash4.setVisibility(View.VISIBLE);
					mBtnFlash2.bringToFront();
					mBtnFlash3.bringToFront();
					mBtnFlash4.bringToFront();
					Log.d("visible", "when close, after click flash1 view,bts visible is" + mBtnFlash2.getVisibility()
							+ " ," + mBtnFlash3.getVisibility() + " ," + mBtnFlash4.getVisibility());
					uiHandler.sendEmptyMessageDelayed(NEED_CLOSE_FLASH_BTS, 4000);
				}
			}
		});
		mBtnShowInfo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// 弹出一个Dialog，显示一些调试信息
				showInfoDialog();
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
		mBtnFlash4.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				updateFlashButtonState(3);
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

			private boolean isRunning = false;

			@Override
			public synchronized void onClick(View v) {
				// 增加显示进度提示框等待
				if (isHasPic && !isRunning && mLoadingDialog == null && mPictureBytesData != null) {
					MLog.getIns().i("CustomCameraActivity", "-------isRunning is false, mPictureBytesData != null");

					isRunning = true;

					mLoadingDialog = ProgressDialog.show(CustomCameraActivity.this, "", "正在处理拍照，请稍候", true);
					mExecutorService.submit(new MyRunnable(CustomCameraActivity.this) {
						@Override
						public void run() {
                            CustomCameraActivity mActivity = getActivityRef().get();
                            if (mActivity == null || mActivity.isFinishing()) {
                                MLog.getIns().i("CustomCameraActivity", "-------before saving photo, mActivity is null, maybe is finished.");
                                return;
                            }
							MLog.getIns().i("CustomCameraActivity", "-------正常点击，开始处理拍照");
							mActivity.saveImage(mPictureBytesData);
							mPictureBytesData = null;
							uiHandler.post(new MyRunnable(CustomCameraActivity.this) {
								@Override
								public void run() {
                                    CustomCameraActivity mActivity = getActivityRef().get();
                                    if (mActivity == null || mActivity.isFinishing()) {
                                        MLog.getIns().i("CustomCameraActivity", "-------after saving photo, mActivity is null, maybe is finished.");
                                        return;
                                    }
									// 关闭进度提示框等待
									// 更新你的UI
									mActivity.setResult(RESULT_OK);
                                    mActivity.onPause();
                                    mActivity.finish();
									isRunning = false;
									if (mActivity.mLoadingDialog != null) {
                                        mActivity.mLoadingDialog.dismiss();
                                        mActivity.mLoadingDialog = null;
									}
								}
							});
						}
					});
				} else {
					MLog.getIns().i("CustomCameraActivity", "-------isRunning may be true, mPictureBytesData may be null, 无效点击");
				}

			}
		});
		mBtnTakePic.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				takePictureAction();
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
					mCamera.cancelAutoFocus();
					mPreviewing = true;
					Log.d("mPreviewing", "mPreviewing changed to :" + mPreviewing);
				}
			}
		});
		uiHandler.sendEmptyMessageDelayed(NEED_CLOSE_FLASH_BTS, 1000);
		view_focus = (View) findViewById(CRes.plugin_camera_view_focus);
		mSurfaceView.setOnTouchListener(onTouchListener);
		Log.d("visible", " after oncreate flash view,bts visible is" + mBtnFlash2.getVisibility() + " ,"
				+ mBtnFlash3.getVisibility() + " ,"
				+ mBtnFlash4.getVisibility());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (uiHandler != null) {
			uiHandler.removeCallbacksAndMessages(null);
		}
		if (mExecutorService != null) {
			mExecutorService.shutdownNow();
		}
	}

	protected void onOrientationChanged(int orientation) {
		if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
			return;
		}
		int diff = Math.abs(orientation - current_orientation);
		if (diff > 180) {
			diff = 360 - diff;
		}
		orientation = (orientation + 45) / 90 * 90;
		// only change orientation when sufficiently changed
		if (diff > 60) {
			if (orientation != current_orientation) {
				current_orientation = orientation;
				if (mPreviewing) {
					picture_orientation = orientation;
				}
				setViewRotation();
			}
		}
		int rotation = getRotate();
//		Log.i(TAG,"onOrientationChanged orientation: " + orientation);
//		Log.i(TAG,"onOrientationChanged getRotate: " + rotation);
		if (null != mCamera) {
			Camera.Parameters parameters = mCamera.getParameters();
			parameters.setRotation(rotation);
			mCamera.setParameters(parameters);
		}
	}

	private void takePictureAction() {
		if (!Parameters.FOCUS_MODE_CONTINUOUS_PICTURE.equals(supportedFocusMode) && (mode == MODE.FOCUSFAIL || mode == MODE.FOCUSING || mCamera == null)) {
			Toast.makeText(CustomCameraActivity.this, "相机正在准备中，请稍候", Toast.LENGTH_SHORT).show();
			return;
		}
		if (mPreviewing) {
			mPreviewing = false;
			mCamera.takePicture(null, null, pictureCallback);
		} else {
			Toast.makeText(CustomCameraActivity.this, "摄像机正忙", Toast.LENGTH_SHORT).show();
		}
	}

	private void showInfoDialog() {
		String message = "";
		// 将mCameraInfo的预览宽高与输出宽高，以及宽高比拼接显示
		message = "当前预览宽高:\n"
				+ mCameraInfo.previewSize.width + "x"
				+ mCameraInfo.previewSize.height
				+ "，（" + mCameraInfo.getPreviewRatio() + "）"
				+ "\n当前输出宽高:\n"
				+ mCameraInfo.pictureSize.width + "x"
				+ mCameraInfo.pictureSize.height
				+ "，（" + mCameraInfo.getPictureRatio() + "）";
		if (mInfoDialog == null) {
			mInfoDialog = new AlertDialog.Builder(this)
					.setTitle("相机信息")
					.setMessage(message)
					.setPositiveButton("查看全部", null)
					.setNegativeButton("关闭", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					}).create();
			mInfoDialog.setOnShowListener(new DialogInterface.OnShowListener() {
				@Override
				public void onShow(DialogInterface dialog) {
					mInfoDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							List<Camera.Size> previewsizes = mCamera.getParameters().getSupportedPreviewSizes();
							String previewSizesStr = CameraUtil.getAllSupportedSize(previewsizes);
							List<Camera.Size> pictureSizes = mCamera.getParameters().getSupportedPictureSizes();
							String pictureSizesStr = CameraUtil.getAllSupportedSize(pictureSizes);
							String message = "预览支持:\n" + previewSizesStr + "\n\n输出支持:\n" + pictureSizesStr;
							mInfoDialog.setMessage(message);
						}
					});
				}
			});
		} else {
			mInfoDialog.setMessage(message);
		}
		mInfoDialog.show();
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
		mBtnFlash4.setRotation(orientation);
		mIvPreShow.setRotation(orientation);
	}

	private void updateFlashButtonState(int index) {
		isOpenFlash = false;
		uiHandler.removeMessages(NEED_CLOSE_FLASH_BTS);
		mBtnFlash2.setVisibility(View.INVISIBLE);
		mBtnFlash3.setVisibility(View.INVISIBLE);
		mBtnFlash4.setVisibility(View.INVISIBLE);

		Integer i = flashDrawableIds.get(index);
		flashDrawableIds.remove(index);
		flashDrawableIds.add(0, i);
		mBtnFlash1.setBackgroundResource(flashDrawableIds.get(0));
		mBtnFlash2.setBackgroundResource(flashDrawableIds.get(1));
		mBtnFlash3.setBackgroundResource(flashDrawableIds.get(2));
		mBtnFlash4.setBackgroundResource(flashDrawableIds.get(3));
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
			} else if (j == CRes.plugin_camera_flash_drawale_torch) {
				par.setFlashMode(Parameters.FLASH_MODE_TORCH);
			}
			mCamera.setParameters(par);
		} catch (Exception e) {
			MLog.getIns().e(TAG + "checkFlash", e);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (uiHandler != null) {
			uiHandler.removeCallbacksAndMessages(null);
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
			MLog.getIns().i("mPreviewing after inti camera mPreviewing changed to :" + mPreviewing);
			uiHandler.sendEmptyMessage(AUTO_FOCUS_AGAIN);
			mCamera.cancelAutoFocus();
		} catch (Exception e) {
			MLog.getIns().e(TAG + "initCamera", e);
		}
	}

	private void initCameraParameters() {
		Camera.Parameters parameters = mCamera.getParameters();
		String mod = Build.MODEL;
		// MZ 180， other 90...
		if ("M9".equalsIgnoreCase(mod) || "MX".equalsIgnoreCase(mod)) {
			setDisplayOrientation(mCamera, 180);
		} else {
			setDisplayOrientation(mCamera, DEFAULT_ROTATE_DEGREE);
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
			mBtnFlash4.setVisibility(View.INVISIBLE);
		} else {
			mBtnFlash1.setVisibility(View.VISIBLE);
		}

		// 根据设备实际支持情况，设置不同的自动对焦模式。优先采用拍照持续对焦模式。
		List<String> focusModes = parameters.getSupportedFocusModes();
		if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
			// Continuous picture mode is supported
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
			supportedFocusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
			MLog.getIns().i(TAG, "focus mode is continuous picture!!!");
		} else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
			// Autofocus mode is supported
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
			MLog.getIns().i(TAG, "focus mode is auto!!!");
		} else {
			MLog.getIns().i(TAG, "focus mode is not supported!!!");
		}
		// 获取屏幕高度，减去bottomBar的高度，就是剩余的surfaceView的最大区域（因为bottomBar是固定高度，因此计算比较准确）
		CameraUtil.Size maxTargetSize = CameraUtil.getMaxTargetSize(this, mBottomBar);
		// 打印最大预览区域高度
		MLog.getIns().i(TAG, "size log: screen height:" + maxTargetSize.height);
		// 打印surfaceView的宽高
		MLog.getIns().i(TAG, "size log: surfaceView width:" + mSurfaceView.getWidth() + ",height:" + mSurfaceView.getHeight());
		// 根据预览区域的最大值，找到适合的相机分辨率
		Camera.Size previewSize = CameraUtil.getFitParametersSize(parameters.getSupportedPreviewSizes(), maxTargetSize.height, maxTargetSize.width, true);
		mCameraInfo.previewSize.height = previewSize.height;
		mCameraInfo.previewSize.width = previewSize.width;
		MLog.getIns().i(TAG, "size log: previewSize width:" + previewSize.width + ",height:" + previewSize.height);
		parameters.setPreviewSize(previewSize.width, previewSize.height);
		// 根据预览分辨率，找到合适的输出图片分辨率
		Camera.Size pictureSize = CameraUtil.getFitParametersSize(parameters.getSupportedPictureSizes(), maxTargetSize.height, maxTargetSize.width, true);
		MLog.getIns().i(TAG, "size log: pictureSize width:" + pictureSize.width + ",height:" + pictureSize.height);
		mCameraInfo.pictureSize.height = pictureSize.height;
		mCameraInfo.pictureSize.width = pictureSize.width;
		parameters.setPictureSize(pictureSize.width, pictureSize.height);
		// 根据预览分辨率，调整surfaceView的高度，防止比例失调
		int newSurfaceViewHeight = (int) ((double)previewSize.width * (double)mSurfaceView.getWidth() / (double)previewSize.height);
		mSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, newSurfaceViewHeight));

		//设置闪光灯默认为自动
		parameters.setFlashMode(Parameters.FLASH_MODE_AUTO);

		try {
			mCamera.setParameters(parameters);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private void setDisplayOrientation(Camera camera, int angle) {
		camera.setDisplayOrientation(angle);
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
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
			keyCode == KeyEvent.KEYCODE_VOLUME_UP ) {
            takePictureAction();
			return true;
        }
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mOnKeyDown) {
				setResult(Activity.RESULT_CANCELED);
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

	PictureCallback pictureCallback = new PictureCallback() {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			Log.d(TAG, "into picture call back");
			mExecutorService.submit(new MyRunnable(CustomCameraActivity.this) {

				@Override
				public void run() {
					CustomCameraActivity mActivity = getActivityRef().get();
					if (mActivity == null || mActivity.isFinishing()) {
						return;
					}
					mPictureBytesData = data;
					final Bitmap bm  = createThumbnail(data);
					uiHandler.post(new MyRunnable(CustomCameraActivity.this) {

						@Override
						public void run() {
							CustomCameraActivity mActivity = getActivityRef().get();
							if (mActivity == null || mActivity.isFinishing()) {
								return;
							}
							mActivity.setIvPreShowBitmap(bm);
							mCamera.startPreview();
							mCamera.cancelAutoFocus();
							mPreviewing =true;
							Log.d(TAG," after take pic mPreviewing changed to :"+mPreviewing);
						}
					});
				}
			});
			Log.d(TAG, "execute asynctask");
		}
	};

	private byte[] processLargeImage(byte[] data) {
		// 如果使用了大尺寸的图片，即没有找到与手机屏幕比例相近的图片，那么需要对图片进行裁剪
		if (isUseLargerImageSize) {
			CameraUtil.Size targetSize = CameraUtil.getTargetSize(mSurfaceView);
			double dmFormat = CameraUtil.getFormat(targetSize.height, targetSize.width);
			Bitmap originBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
			Bitmap cropBitmap = BitmapUtil.cropBitmap(originBitmap, dmFormat);
			int bytes = cropBitmap.getByteCount();
			ByteBuffer buffer = ByteBuffer.allocate(bytes);
			cropBitmap.copyPixelsToBuffer(buffer);
			data = buffer.array();
		}
		return data;
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

	public Bitmap createThumbnail(byte[] data) {
		ExifInterface originExif = ExifUtil.getExifInfo(data);
		int degree = ExifUtil.getExifOrientationDegree(originExif);
		Bitmap bm = null;
		try {
			int width = mCamera.getParameters().getPictureSize().width;
			int previewWidth = mIvPreShow.getWidth();
			int ratio = (int) Math.ceil((double) width / previewWidth);
			int inSampleSize = Integer.highestOneBit(ratio);
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = inSampleSize;
			bm = BitmapFactory.decodeByteArray(data, 0, data.length, options);
			if (this.isUseLargerImageSize) {
				CameraUtil.Size targetSize = CameraUtil.getTargetSize(mSurfaceView);
				double dmFormat = CameraUtil.getFormat(targetSize.height, targetSize.width);
				Bitmap newBitmap = BitmapUtil.cropBitmap(bm, dmFormat);
				bm.recycle();
				bm = newBitmap;
				MLog.getIns().i(TAG, "createThumbnail isUseLargerImageSize is true, cropBitmap");
			}
			bm = BitmapUtil.rotate(bm, degree);
		} catch (Exception e) {
			MLog.getIns().e(TAG + "createThumbnail exception", e);
		}
		return bm;
	}

	private void saveImage(byte[] data) {
		File pictureFile = new File(filePath);
		if (pictureFile.exists()) {
			pictureFile.delete();
		} else {
			try {
				pictureFile.createNewFile();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		ExifInterface originExif = ExifUtil.getExifInfo(data);
		// note: 压缩情况下会处理isUseLargerImageSize为true的大照片剪裁问题，不压缩的话不处理
		if (!(openInternalVO == null
				|| openInternalVO.getCompressOptions() == null
				|| openInternalVO.getCompressOptions().getIsCompress() == 0)) {
			MLog.getIns().i("saveImage: starting to handle Compress...");
			Bitmap bm = null;
			int quality = 100;
			// 开启压缩
			if(openInternalVO.getCompressOptions().getIsCompress() == 1 || openInternalVO.getCompressOptions().getIsCompress() == 2) {
				PhotoSizeVO photoSizeVO = openInternalVO.getCompressOptions().getPhotoSize();
				quality = openInternalVO.getCompressOptions().getQuality();
				if (photoSizeVO != null) {
					BitmapFactory.Options opts = new BitmapFactory.Options();
					opts.inJustDecodeBounds = true;
					BitmapFactory.decodeByteArray(data, 0, data.length, opts);
					opts.inSampleSize = BitmapUtil.calculateInSampleSize(opts, photoSizeVO.getHeight(), photoSizeVO.getWidth());
					opts.inPurgeable = true;
					opts.inInputShareable = true;
					opts.inTempStorage = new byte[64 * 1024];
					opts.inJustDecodeBounds = false;
					bm = BitmapFactory.decodeByteArray(data, 0, data.length, opts);
				} else {
					bm = BitmapFactory.decodeByteArray(data, 0, data.length);
					// 使用quality压缩，取值1-100
					if (quality > 100 || quality <= 0) {
						quality = 100;
						MLog.getIns().w(TAG + "compress quality is invalid: " + quality + ". change it to 100");
					}
				}
				if (this.isUseLargerImageSize) {
					CameraUtil.Size targetSize = CameraUtil.getTargetSize(mSurfaceView);
					double dmFormat = CameraUtil.getFormat(targetSize.height, targetSize.width);
					Bitmap newBitmap = BitmapUtil.cropBitmap(bm, dmFormat);
					bm.recycle();
					bm = newBitmap;
					MLog.getIns().i(TAG, "saveImage isUseLargerImageSize is true, cropBitmap");
				}
			} else {
				// 非0情况下，非1非2，目前只有3的情况，即给定目标文件大小，循环压缩到指定大小。
				long targetSizeLong = openInternalVO.getCompressOptions().getFileSize();
				bm = BitmapUtil.compressBmpFileToTargetSize(data, targetSizeLong);
				if (this.isUseLargerImageSize) {
					CameraUtil.Size targetSize = CameraUtil.getTargetSize(mSurfaceView);
					double dmFormat = CameraUtil.getFormat(targetSize.height, targetSize.width);
					Bitmap newBitmap = BitmapUtil.cropBitmap(bm, dmFormat);
					bm.recycle();
					bm = newBitmap;
					MLog.getIns().i(TAG, "saveImage compress-3 isUseLargerImageSize is true, cropBitmap");
				}
			}
			try {
				if (bm != null) {
					// 用完后立即置null，防止内存占用过多
					mPictureBytesData = null;
					// 开始处理水印
					MLog.getIns().i("saveImage(compress): starting to handle WaterMark...");
					WatermarkOptionsVO watermarkOptionsVO = openInternalVO.getWatermarkOptions();
					Bitmap result = ImageWatermarkUtil.handleWatermark(CustomCameraActivity.this, bm, originExif, watermarkOptionsVO);
					if (result == null) {
						MLog.getIns().i("saveImage(compress): no need to handle WaterMark...");
						result = bm;
					} else {
						MLog.getIns().i("saveImage(compress): handle WaterMark finished...");
					}
					// 处理水印结束，开始写入文件
					BufferedOutputStream bos = null;
					try {
						bos = new BufferedOutputStream(new FileOutputStream(pictureFile));
						result.compress(Bitmap.CompressFormat.JPEG, quality, bos);



						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						result.compress(Bitmap.CompressFormat.JPEG, quality, baos);
						long currentSize = baos.toByteArray().length;
						MLog.getIns().i(TAG, "compressBmpFileToTargetSize baos.toByteArray().length " + currentSize);



						bos.flush();
					} catch (IOException e) {
						MLog.getIns().e(TAG + "AppCan Camera Watermark", e);
					} finally {
						if (bos != null) {
							try {
								bos.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
					// 如果需要存入相册，则需要转为byte数组存起来，后面备用
					if (isNeedToUpdateAlbum()) {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						result.compress(Bitmap.CompressFormat.JPEG, quality, baos);
						// 用完后立即置null，防止内存占用过多
						mPictureBytesData = null;
//					Runtime.getRuntime().gc();
						data = baos.toByteArray();
					}
					if (!result.isRecycled()) {
						result.recycle();
					}
				} else {
					MLog.getIns().e(TAG + "compressed bitmap is null!!!");
				}
				// 图片文件以JPEG格式写入本地完毕
			} catch (Exception e) {
				MLog.getIns().e(TAG + "AppCan Camera Compress", e);
			}
		} else {
			MLog.getIns().i("saveImage(no compress): starting to handle WaterMark...");
			// 开始处理水印
			WatermarkOptionsVO watermarkOptionsVO = openInternalVO.getWatermarkOptions();
			Bitmap result = ImageWatermarkUtil.handleWatermark(CustomCameraActivity.this, data, originExif, watermarkOptionsVO);
			if (result != null) {
				// 用完后立即置null，防止内存占用过多
				mPictureBytesData = null;
				BufferedOutputStream bos = null;
				try {
					bos = new BufferedOutputStream(new FileOutputStream(pictureFile));
					result.compress(Bitmap.CompressFormat.JPEG, 100, bos);
					bos.flush();
				} catch (IOException e) {
					MLog.getIns().e(TAG + "AppCan Camera Watermark", e);
				} finally {
					if (bos != null) {
						try {
							bos.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				// 如果需要存入相册，则需要转为byte数组存起来，后面备用
				if (isNeedToUpdateAlbum()) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					result.compress(Bitmap.CompressFormat.JPEG, 100, baos);
					// 用完后立即置null，防止内存占用过多
					mPictureBytesData = null;
//					Runtime.getRuntime().gc();
					data = baos.toByteArray();
				}
			} else {
				// 无压缩，无水印，正常保存
				MLog.getIns().i("不进行压缩，不处理水印，正常保存： " + pictureFile);
				// 用完后立即置null，防止内存占用过多
				mPictureBytesData = null;
				FileOutputStream fos = null;
				try {
					MLog.getIns().i(TAG, "compress log: no compress: bitmap byte data length=" + data.length);
					fos = new FileOutputStream(pictureFile);
					fos.write(data);
					fos.close();
					MLog.getIns().i(TAG, "compress log: no compress:  file length=" + pictureFile.length());
				} catch (Exception e) {
					MLog.getIns().e(TAG + "saveImage 照片存储失败", e);
				} finally {
					if (fos != null) {
						try {
							fos.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

		// 如果原图存在exif，则需要重新写入exif防止图片方向错误（压缩后exif会丢失，故需要重新写入）
		if (ExifUtil.getExifOrientationDegree(originExif) != 0) {
			MLog.getIns().i(TAG + "getExifOrientationDegree is not 0 degree, need to handle exif orientation");
			try {
				ExifInterface exifOutput = new ExifInterface(pictureFile.getAbsolutePath());
				// 目前只处理了exif中的方向参数
				ExifUtil.copyExifOrientation(originExif, exifOutput);
				data = FileUtil.getByteArrayFromFile(pictureFile);
			} catch (Exception e) {
				MLog.getIns().e(TAG + "saveImage", e);
			}
		}

		// 根据参数处理将图片保存在应用内部还是保存在相册中
		if (isNeedToUpdateAlbum()) {
			// 开启了公共存储，需要将图片写入相册
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				updateAlbum(data, pictureFile.getName());
			} else {
				MLog.getIns().e(TAG + "系统版本低于R，采用MediaScanner存入相册");
				String relativeDirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "AppCanDCIM";
				File parentDirFile = new File(relativeDirPath);
				if (!parentDirFile.exists()) {
					parentDirFile.mkdirs();
				}
				// 将data写入sdcard的图片目录中，需要先获取sdcard路径并拼接
				File albumOutputFile = new File(relativeDirPath, pictureFile.getName());
				FileOutputStream fos = null;
				try {
					fos = new FileOutputStream(albumOutputFile);
					fos.write(data);
					fos.close();
					MediaScannerConnection.scanFile(this, new String[]{albumOutputFile.getAbsolutePath()}, null, null);
				} catch (Exception e) {
					MLog.getIns().e(TAG + "copyImageToAlbum 照片添加失败", e);
				} finally {
					if (fos != null) {
						try {
							fos.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		} else {
			// 关闭了公共存储，故不需要更新到相册
			MLog.getIns().i("storageOptions isPublic is 0, no need to updateAlbum");
		}
	}

	private boolean isNeedToUpdateAlbum() {
		return openInternalVO != null
			&& openInternalVO.getStorageOptions() != null
			&& "1".equals(openInternalVO.getStorageOptions().getIsPublic());
	}

	/**
	 * 更新到相册
	 *
	 * @param data
	 */
	@RequiresApi(api = Build.VERSION_CODES.R)
	private void updateAlbum(byte[] data, String fileName){

//		Uri fileUri = BUtility.getUriForFileWithFileProvider(this, file.getAbsolutePath());
//		String fileName = "test.jpg";
		long imageTokenTime = System.currentTimeMillis();
		// 插入file数据到相册
		ContentValues values = new ContentValues(9);
		values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
		String relativeDirPath = Environment.DIRECTORY_PICTURES + File.separator + "AppCanDCIM";
		values.put(MediaStore.Images.Media.TITLE, "AppCanCamera");
		values.put(MediaStore.Images.Media.DATE_TAKEN, imageTokenTime);
		values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
//		values.put(MediaStore.Images.Media.ORIENTATION, getRotate());
		values.put(MediaStore.Images.Media.SIZE, data.length);
		values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDirPath);
		values.put(MediaStore.MediaColumns.IS_PENDING, 1);
		values.put(MediaStore.MediaColumns.DATE_EXPIRES, (imageTokenTime + DateUtils.DAY_IN_MILLIS) / 1000);
		Uri uri = CustomCameraActivity.this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
		OutputStream outputStream = null;
		try {
			outputStream = CustomCameraActivity.this.getContentResolver().openOutputStream(uri);
			outputStream.write(data);
			outputStream.flush();
			MLog.getIns().i("storageOptions isPublic is 1, updateAlbum and write to ExternalStorage Path: " + relativeDirPath);
			// Everything went well above, publish it!
			values.clear();
			values.put(MediaStore.MediaColumns.IS_PENDING, 0);
			values.putNull(MediaStore.MediaColumns.DATE_EXPIRES);
			CustomCameraActivity.this.getContentResolver().update(uri, values, null, null);
			MLog.getIns().i("updateAlbum complete!");
		} catch (IOException e) {
			MLog.getIns().e(TAG + "updateAlbum", e);
			CustomCameraActivity.this.getContentResolver().delete(uri, null);
		} finally {
			try {
				if (outputStream != null) {
					outputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// 通知相册更新
		CustomCameraActivity.this.sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", uri));
	}

	@Override
	public void onAutoFocus(boolean success, Camera camera) {
		MLog.getIns().i(TAG, "onAutoFocus success: " + success);
		if (success) {
			mode = MODE.FOCUSED;
			view_focus.setBackgroundResource(CRes.plugin_camera_view_focused_bg);
		} else {
			mode = MODE.FOCUSFAIL;
			view_focus.setBackgroundResource(CRes.plugin_camera_view_focus_fail_bg);
			if (!Parameters.FOCUS_MODE_CONTINUOUS_PICTURE.equals(supportedFocusMode)) {
				// 延时2秒再执行一次自动对焦
				uiHandler.sendEmptyMessageDelayed(AUTO_FOCUS_AGAIN, 2 * 1000);
			}
		}
		resetFocusView();
	}

	private void resetFocusView() {
		uiHandler.removeMessages(HIDE_AUTO_FOCUS_FRAME);
		uiHandler.sendEmptyMessageDelayed(HIDE_AUTO_FOCUS_FRAME,  2000);
	}

	private float mOldDistance = 0;

	/**
	 * 点击显示焦点区域
	 */
	OnTouchListener onTouchListener = new OnTouchListener() {

		@SuppressLint("ClickableViewAccessibility")
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getPointerCount() == 1) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					int width = view_focus.getWidth();
					int height = view_focus.getHeight();
					view_focus.setBackgroundResource(CRes.plugin_camera_view_focusing_bg);
					view_focus.setX(event.getX() - ((float) width / 2));
					view_focus.setY(event.getY() - ((float) height / 2));
					view_focus.setVisibility(View.VISIBLE);
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					mode = MODE.FOCUSING;
					focusOnTouch(event);
				}
				return true;
			} else {
				switch (event.getAction() & MotionEvent.ACTION_MASK) {
					case MotionEvent.ACTION_POINTER_DOWN:
						mOldDistance = CameraUtil.getFingerSpacing(event);
						break;
					case MotionEvent.ACTION_MOVE:
						float newDistance = CameraUtil.getFingerSpacing(event);
						if (newDistance > mOldDistance) {
							mCurrentZoom = CameraUtil.handleZoom(mCamera, true);
						} else if (newDistance < mOldDistance) {
							mCurrentZoom = CameraUtil.handleZoom(mCamera, false);
						}
						mOldDistance = newDistance;
						break;
					default:
						break;
				}
				return CustomCameraActivity.super.onTouchEvent(event);
			}
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
				// 这里修改为直接用0，不偏移，因为坐标应该是相对于surfaceView内部的。之前应该是因为surfaceView与相机预览区域不一致所以偏移了一部分，但是现在已经修改了逻辑，保持一致了。
				int[] location = new int[2];
//				mSurfaceView.getLocationOnScreen(location);
				Rect focusRect = calculateTapArea(view_focus.getWidth(), view_focus.getHeight(), 1f, event.getRawX(),
						event.getRawY(), location[0], location[0] + mSurfaceView.getWidth(), location[1],
						location[1] + mSurfaceView.getHeight());
				Rect meteringRect = calculateTapArea(view_focus.getWidth(), view_focus.getHeight(), 1.5f,
						event.getRawX(), event.getRawY(), location[0], location[0] + mSurfaceView.getWidth(),
						location[1], location[1] + mSurfaceView.getHeight());
				if (parameters.getMaxNumFocusAreas() > 0) {
					List<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
					focusAreas.add(new Camera.Area(focusRect, 1000));
					// 对焦区域
					parameters.setFocusAreas(focusAreas);
				}

				if (parameters.getMaxNumMeteringAreas() > 0) {
					List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
					meteringAreas.add(new Camera.Area(meteringRect, 1000));
					// 测光区域
					parameters.setMeteringAreas(meteringAreas);
				}
			}
			// 触摸手动对焦后，切换为自动对焦模式，而非持续对焦模式，防止手动对焦后的结果被持续对焦覆盖
			// 也就是说，只要手动对焦一次，就切换为自动对焦模式。不点击屏幕进行手动对焦，就是持续对焦模式
			List<String> focusModes = parameters.getSupportedFocusModes();
			if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
				// Autofocus mode is supported
				parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
				MLog.getIns().i(TAG, "focus mode is auto!!!");
			}
			mCamera.setParameters(parameters);
			uiHandler.sendEmptyMessage(AUTO_FOCUS_AGAIN);
		} catch (Exception e) {
			MLog.getIns().e(TAG + "focusOnTouch", e);
		}
	}

	/**
	 * 计算触摸对焦和测光区域，并根据旋转角度进行坐标转换
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
		CoordinateTransformer transformer = new CoordinateTransformer(false, DEFAULT_ROTATE_DEGREE, mSurfaceView.getWidth(), mSurfaceView.getHeight());
		// 默认对焦区域为正方形，如果不是，则取最小值作为正方形的边长
		Rect rect = transformer.getFocusRect(x, y, Math.min(focusWidth, focusHeight), areaMultiple);
		MLog.getIns().i("CalculateTapArea", "Raw Rect: " + rect.left + "," + rect.top + "," + rect.right + "," + rect.bottom);
		return rect;
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
