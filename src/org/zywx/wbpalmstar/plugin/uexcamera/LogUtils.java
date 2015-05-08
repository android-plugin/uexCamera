package org.zywx.wbpalmstar.plugin.uexcamera;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.zywx.wbpalmstar.base.BUtility;

import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

public class LogUtils {
	static String TAG = "uexCamera";
	static boolean isLog = true;
	static boolean isLogOutput = false;

	public static void i(String tag, String msg) {
		if (isLog) {
			Log.i(tag, msg);
		}
	}

	public static void w(String tag, String msg) {
		if (isLog) {
			Log.w(tag, msg);
		}
	}

	public static void e(String tag, String msg) {
		if (isLog) {
			Log.e(tag, msg);
		}
	}

	public static void v(String tag, String msg) {
		if (isLog) {
			Log.v(tag, msg);
		}
	}

	public static void d(String tag, String msg) {
		if (isLog) {
			Log.d(tag, msg);
		}
	}

	public static void o(String text) {
		LogUtils.i(TAG + "Log", text);
		if (!isLogOutput) {
			return;
		}
		if (!TextUtils.isEmpty(text) && BUtility.sdCardIsWork()) {
			String developPath = BUtility.getSdCardRootPath()
					+ "widgetone/log/";
			File dir = new File(developPath);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			String fileName = "plugin_" + TAG + "_log_" + getCurYearAndMonth()
					+ ".txt";
			File log = new File(developPath + fileName);
			BufferedWriter bw = null;
			try {
				if (!log.exists()) {
					log.createNewFile();
				}
				bw = new BufferedWriter(new FileWriter(log, true));
				bw.write("\r" + getNowTime() + "\r" + text);
				bw.flush();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (bw != null) {
						bw.close();
						bw = null;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
	}

	public static String getNowTime() {
		Time time = new Time();
		time.setToNow();
		int year = time.year;
		int month = time.month + 1;
		int day = time.monthDay;
		int minute = time.minute;
		int hour = time.hour;
		int sec = time.second;
		return year + "-" + month + "-" + day + " " + hour + ":" + minute + ":"
				+ sec;
	}

	public static String getCurYearAndMonth() {
		Time time = new Time();
		time.setToNow();
		int year = time.year;
		int month = time.month + 1;
		return year + "_" + month;
	}
}
