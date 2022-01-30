package org.zywx.wbpalmstar.plugin.uexcamera.utils;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 文件工具类
 * 
 * @author waka
 *
 */
public class FileUtil {

	private static final String TAG = "FileUtil";

	/**
	 * 得到以日期命名的文件名
	 * 
	 * @param prefix 前缀
	 * @param postfix 后缀
	 * @return 文件名
	 */
	public static String getSimpleDateFormatFileName(String prefix, String postfix) {

		Date date = new Date();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
		String fileName = prefix + simpleDateFormat.format(date) + postfix;

		return fileName;
	}

	/**
	 * 检查文件夹路径是否存在，不存在则创建
	 * 
	 * @param folderPath
	 * @return 返回创建结果
	 */
	public static boolean checkFolderPath(String folderPath) {

		File file = new File(folderPath);
		if (!file.exists()) {
			Log.i(TAG, "【checkFolderPath】	file.exists() == false");
			return file.mkdirs();
		}
		Log.i(TAG, "【checkFolderPath】	file.exists() == true");
		return true;
	}

	public static byte[] getByteArrayFromFile(File file) {
		FileInputStream fis = null;
		byte[] result = null;
		try {
			fis = new FileInputStream(file);
			result = InputStreamUtils.InputStreamTOByte(fis);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}
}
