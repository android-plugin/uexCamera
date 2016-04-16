package org.zywx.wbpalmstar.plugin.uexcamera.utils;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;

/**
 * 内存信息工具类
 * 
 * @author waka 2016/04/13
 *
 */
public class MemoryUtil {

	/**
	 * 判断系统当前是否是低内存状态
	 * 
	 * @param context
	 * @return
	 */
	public synchronized static boolean isLowMemory(Context context) {

		// 获得内存信息
		MemoryInfo memoryInfo = new MemoryInfo();
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		activityManager.getMemoryInfo(memoryInfo);

		// 返回低内存标识
		return memoryInfo.lowMemory;
	}
}
