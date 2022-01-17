package org.zywx.wbpalmstar.plugin.uexcamera.vo;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * File Description: 打开自定义相机的参数实体类
 * <p>
 * Created by sandy with Email: sandy1108@163.com at Date: 2022/1/13.
 */
public class OpenInternalVO implements Parcelable {

    private StorageOptionsVO storageOptions;
    private WatermarkOptionsVO watermarkOptions;
    private CompressOptionsVO compressOptions;
    private String openInternalCallbackFuncId;

    public OpenInternalVO() {
    }

    public StorageOptionsVO getStorageOptions() {
        return storageOptions;
    }

    public void setStorageOptions(StorageOptionsVO storageOptions) {
        this.storageOptions = storageOptions;
    }

    public WatermarkOptionsVO getWatermarkOptions() {
        return watermarkOptions;
    }

    public void setWatermarkOptions(WatermarkOptionsVO watermarkOptions) {
        this.watermarkOptions = watermarkOptions;
    }

    public CompressOptionsVO getCompressOptions() {
        return compressOptions;
    }

    public void setCompressOptions(CompressOptionsVO compressOptions) {
        this.compressOptions = compressOptions;
    }

    public String getOpenInternalCallbackFuncId() {
        return openInternalCallbackFuncId;
    }

    public void setOpenInternalCallbackFuncId(String openInternalCallbackFuncId) {
        this.openInternalCallbackFuncId = openInternalCallbackFuncId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.storageOptions, flags);
        dest.writeParcelable(this.watermarkOptions, flags);
        dest.writeParcelable(this.compressOptions, flags);
        dest.writeString(this.openInternalCallbackFuncId);
    }

    public void readFromParcel(Parcel source) {
        this.storageOptions = source.readParcelable(StorageOptionsVO.class.getClassLoader());
        this.watermarkOptions = source.readParcelable(WatermarkOptionsVO.class.getClassLoader());
        this.compressOptions = source.readParcelable(CompressOptionsVO.class.getClassLoader());
        this.openInternalCallbackFuncId = source.readString();
    }

    protected OpenInternalVO(Parcel in) {
        this.storageOptions = in.readParcelable(StorageOptionsVO.class.getClassLoader());
        this.watermarkOptions = in.readParcelable(WatermarkOptionsVO.class.getClassLoader());
        this.compressOptions = in.readParcelable(CompressOptionsVO.class.getClassLoader());
        this.openInternalCallbackFuncId = in.readString();
    }

    public static final Creator<OpenInternalVO> CREATOR = new Creator<OpenInternalVO>() {
        @Override
        public OpenInternalVO createFromParcel(Parcel source) {
            return new OpenInternalVO(source);
        }

        @Override
        public OpenInternalVO[] newArray(int size) {
            return new OpenInternalVO[size];
        }
    };
}
