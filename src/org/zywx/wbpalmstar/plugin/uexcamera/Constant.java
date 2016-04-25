package org.zywx.wbpalmstar.plugin.uexcamera;

/**
 * 常量类
 * 
 * @author waka
 *
 */
public class Constant {

	// RequestCode
	public static final int REQUEST_CODE_SYSTEM_CAMERA = 66;// 系统相机
	public static final int REQUEST_CODE_INTERNAL_CAMERA = 67;// 自定义相机
	
	// EUExCamera跳转到自定义相机需要传的数据
	public static final String INTENT_EXTRA_NAME_PHOTO_PATH = "photoPath";// 图片路径

}
