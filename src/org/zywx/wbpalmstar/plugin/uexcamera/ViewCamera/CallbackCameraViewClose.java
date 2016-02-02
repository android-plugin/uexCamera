package org.zywx.wbpalmstar.plugin.uexcamera.ViewCamera;

/**
 * 此接口为联系的方式，CameraView关闭时自身不能关闭，需要上下文调用removeView方法, 所以必须通过回调通知上下文该关闭了
 * 
 * @author waka
 *
 */
public interface CallbackCameraViewClose {
	public void callbackClose();
}
