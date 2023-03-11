package org.zywx.wbpalmstar.plugin.uexcamera.utils.log;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.base.BUtility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * 自定义log工具类
 * 
 * The class for print log
 */
public class MLog {

	/**
	 * 自定义选项
	 * 
	 * 可以根据需要自行修改
	 *
	 */
	private static final String TAG = "uexCamera";// TAG
	private static final String DEVELOPER_NAME = "@waka@";// 开发者姓名

	public final static String LOG_FILE_PREFIX = "camera_log_";
	//    public static final boolean DEBUG = true;
	public static boolean DEBUG = false;
	// log输出到文件的开关
	private static boolean log2fileSwitch = false;

	public static final String SDCARD_LOG_DIR = "widgetone/log/";
	public static final String LOG_DIR = "appcanlog/";

	private static String packageName = TAG;
	private static String outputLogPath = "";
	private static String outputLogFilePath = "";

	/**
	 * 饿汉式单例
	 */
	private final static MLog instance = new MLog();

	/**
	 * 获得单例
	 *
	 */
	public static MLog getIns() {
		return instance;
	}

	/**
	 * 私有构造方法
	 */
	private MLog() {
	}

	/**
	 * 初始化方法(可选)
	 *
	 * 若调用了初始化方法，则使用谷歌推荐的外部存储路径，否则直接使用SD卡根目录
	 *
	 */
	public void init(Context context) {
		if (context == null){
			Log.e(TAG, "LogUtils init error!!! context is null");
			return;
		}
		packageName = context.getApplicationContext().getPackageName();
		DEBUG = BDebug.DEBUG;
		if (TextUtils.isEmpty(outputLogPath)){
			outputLogPath = getOutputLogBasePath(context.getApplicationContext());
			outputLogFilePath = getLogPath();
		}
	}

	private static String getOutputLogBasePath(Context applicationContext){
		if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
			return BUtility.getSdCardRootPath() + SDCARD_LOG_DIR;
		}else{
			return BUtility.getExterBoxPath(applicationContext) + LOG_DIR;
		}
	}

	/**
	 * 得到log存放路径
	 *
	 */
	private String getLogPath() {
		if (!TextUtils.isEmpty(outputLogPath)) {
			String developPath = outputLogPath;
			File dir = new File(developPath);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			String fileName = LOG_FILE_PREFIX + SystemTime.getCurYearAndMonth() + "_"
					+ packageName + ".txt";
			File log = new File(developPath + fileName);
			if (!log.exists()) {
				try {
					log.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return log.getAbsolutePath();
		} else {
			Log.e(TAG, "LogUtils getLogPath error!!! outputLogPath is null");
			return "";
		}
	}

	/**
	 * Get The Current Function Name
	 * 
	 * 得到当前方法名
	 *
	 */
	private String getFunctionName() {
		StackTraceElement[] sts = Thread.currentThread().getStackTrace();
		for (StackTraceElement st : sts) {
			if (st.isNativeMethod()) {
				continue;
			}
			if (st.getClassName().equals(Thread.class.getName())) {
				continue;
			}
			if (st.getClassName().equals(this.getClass().getName())) {
				continue;
			}
			return DEVELOPER_NAME + " [ " + Thread.currentThread().getName() + ": " + st.getFileName() + ":"
					+ st.getLineNumber() + " " + st.getMethodName() + " ]";
		}
		return null;
	}

	/**
	 * 外部代码控制log输出到文件
	 */
	public void setLog2fileSwitch(boolean inputLog2fileSwitch) {
		log2fileSwitch = inputLog2fileSwitch;
	}

	public void i(String tag, String str) {
		i(tag + " " + str);
	}

	public void e(String tag, String str) {
		e(tag + " " + str);
	}

	/**
	 * The Log Level:i
	 *
	 */
	public void i(Object str) {

		String name = getFunctionName();
		if (name != null) {
			Log.i(TAG, name + " - " + str);

			if (log2fileSwitch) {
				writeToFile('i', name + " - " + str);
			}
		} else {
			Log.i(TAG, str.toString());

			if (log2fileSwitch) {
				writeToFile('i', str.toString());
			}
		}

	}

	/**
	 * The Log Level:d
	 *
	 */
	public void d(Object str) {

		String name = getFunctionName();
		if (name != null) {
			Log.d(TAG, name + " - " + str);

			if (log2fileSwitch) {
				writeToFile('d', name + " - " + str);
			}
		} else {
			Log.d(TAG, str.toString());

			if (log2fileSwitch) {
				writeToFile('d', str.toString());
			}
		}
	}

	/**
	 * The Log Level:V
	 *
	 */
	public void v(Object str) {

		String name = getFunctionName();
		if (name != null) {
			Log.v(TAG, name + " - " + str);

			if (log2fileSwitch) {
				writeToFile('v', name + " - " + str);
			}
		} else {
			Log.v(TAG, str.toString());

			if (log2fileSwitch) {
				writeToFile('v', str.toString());
			}
		}
	}

	/**
	 * The Log Level:w
	 *
	 */
	public void w(Object str) {

		String name = getFunctionName();
		if (name != null) {
			Log.w(TAG, name + " - " + str);

			if (log2fileSwitch) {
				writeToFile('w', name + " - " + str);
			}
		} else {
			Log.w(TAG, str.toString());

			if (log2fileSwitch) {
				writeToFile('w', str.toString());
			}
		}
	}

	/**
	 * The Log Level:e
	 *
	 */
	public void e(Object str) {

		String name = getFunctionName();
		if (name != null) {
			Log.e(TAG, name + " - " + str);

			if (log2fileSwitch) {
				writeToFile('e', name + " - " + str);
			}
		} else {
			Log.e(TAG, str.toString());

			if (log2fileSwitch) {
				writeToFile('e', str.toString());
			}
		}
	}

	/**
	 * The Log Level:e
	 *
	 */
	public void e(Exception ex) {

		Log.e(TAG, "error", ex);

		if (log2fileSwitch) {
			writeToFile('e', ex.getMessage());
		}
	}

	/**
	 * The Log Level:e
	 *
	 */
	public void e(String log, Throwable tr) {

		String line = getFunctionName();
		Log.e(TAG, "{Thread:" + Thread.currentThread().getName() + "}" + "[" + DEVELOPER_NAME + " " + line + ":] " + log
				+ "\n", tr);

		if (log2fileSwitch) {
			writeToFile('e', "{Thread:" + Thread.currentThread().getName() + "}" + "[" + DEVELOPER_NAME + line + ":] "
					+ log + "\n" + tr.getMessage());
		}
	}

	/**
	 * 将log写入文件
	 *
	 */
	private void writeToFile(char type, String msg) {

		String log = SystemTime.getNowTime() + " " + type + " " + TAG + " " + msg + "\n";// log日志内容，可以自行定制

		FileOutputStream fos = null;
		BufferedWriter bw = null;
		try {
			fos = new FileOutputStream(outputLogFilePath, true);// 这里的第二个参数代表追加还是覆盖，true为追加，false为覆盖
			bw = new BufferedWriter(new OutputStreamWriter(fos));
			bw.write(log);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (bw != null) {
					bw.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}