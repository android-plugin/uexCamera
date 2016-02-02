package org.zywx.wbpalmstar.plugin.uexcamera.ViewCamera;

import java.io.File;

import org.zywx.wbpalmstar.engine.universalex.EUExUtil;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class SecondActivity extends Activity implements OnClickListener {
	private static final String TAG = "SecondActivity";
	private ImageView imgPhoto;
	private TextView tvLocation;
	private Button btnAgain, btnSubmit;
	private String fileName;
	private String location;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(EUExUtil.getResLayoutID("plugin_camera_activity_second"));
		initView();
		initData();
		initEvent();
	}

	private void initView() {
		imgPhoto = (ImageView) findViewById(EUExUtil.getResIdID("plugin_camera_imgPhoto"));
		tvLocation = (TextView) findViewById(EUExUtil.getResIdID("plugin_camera_tvLocation"));
		btnAgain = (Button) findViewById(EUExUtil.getResIdID("plugin_camera_btnAgain"));
		btnSubmit = (Button) findViewById(EUExUtil.getResIdID("plugin_camera_btnSubmit"));
	}

	private void initData() {
		Intent intent = getIntent();
		fileName = intent.getStringExtra("fileName");
		location = intent.getStringExtra("location");
		tvLocation.setText(location);
		// 从文件中读取图片，并将图片赋给layoutPhoto的背景，需要放大两倍
		Bitmap bitmap = null;
		if (bitmap == null) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;// 得到bitmapBound，不将bitmap写入内存
			@SuppressWarnings("unused") // 其实是用了的。。。不将它写入内存，用来获取长宽数据
			Bitmap bitmapBound = BitmapFactory.decodeFile(fileName, options);
			Log.i(TAG, "before--->" + options.outWidth);
			Log.i(TAG, "before--->" + options.outHeight);
			options.outWidth = options.outWidth * 2;
			options.outHeight = options.outHeight * 2;
			Log.i(TAG, "after--->" + options.outWidth);
			Log.i(TAG, "after--->" + options.outHeight);
			options.inJustDecodeBounds = false;// 将bitmap写入内存
			bitmap = BitmapFactory.decodeFile(fileName, options);
			imgPhoto.setImageBitmap(bitmap);
			// bitmap = BitmapFactory.decodeFile(fileName);
			// imgPhoto.setBackgroundDrawable(new BitmapDrawable(bitmap));
		}
	}

	private void initEvent() {
		btnAgain.setOnClickListener(this);
		btnSubmit.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		// 重拍
		if (v.getId() == EUExUtil.getResIdID("plugin_camera_btnAgain")) {
			deleteFile(fileName);

			// 回传取消标记给主Activity
			Intent intent = new Intent();
			setResult(RESULT_CANCELED, intent);

			finish();
		}
		// 提交
		else if (v.getId() == EUExUtil.getResIdID("plugin_camera_btnSubmit")) {
			// EUExCamera.photoPath = fileName;// 赋值给EUExCamera的照片路径

			// 回传数据给主Activity
			Intent intent = new Intent();
			intent.putExtra("photoPath", fileName);
			setResult(RESULT_OK, intent);
			finish();
		}
	}

	/**
	 * 删除文件
	 * 
	 * @param filePath
	 */
	public boolean deleteFile(String filePath) {
		File file = new File(filePath);
		if (file.exists()) {
			return file.delete();
		}
		return false;
	}
}
