package org.zywx.wbpalmstar.plugin.uexcamera.vo;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * File Description: 给图片添加水印的参数实体类
 * <p>
 * Created by sandy with Email: sandy1108@163.com at Date: 2022/8/27.
 */
public class AddWatermarkVO implements Parcelable {

    private String srcImgPath;
    private String dstImgPath;
    private StorageOptionsVO storageOptions;
    private WatermarkOptionsVO watermarkOptions;
    private CompressOptionsVO compressOptions;
    private String addWatermarkCallbackFuncId;

    public AddWatermarkVO() {
    }

    public String getDstImgPath() {
        return dstImgPath;
    }

    public void setDstImgPath(String dstImgPath) {
        this.dstImgPath = dstImgPath;
    }

    public String getSrcImgPath() {
        return srcImgPath;
    }

    public void setSrcImgPath(String srcImgPath) {
        this.srcImgPath = srcImgPath;
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

    public String getAddWatermarkCallbackFuncId() {
        return addWatermarkCallbackFuncId;
    }

    public void setAddWatermarkCallbackFuncId(String addWatermarkCallbackFuncId) {
        this.addWatermarkCallbackFuncId = addWatermarkCallbackFuncId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.srcImgPath);
        dest.writeString(this.dstImgPath);
        dest.writeParcelable(this.storageOptions, flags);
        dest.writeParcelable(this.watermarkOptions, flags);
        dest.writeParcelable(this.compressOptions, flags);
        dest.writeString(this.addWatermarkCallbackFuncId);
    }

    public void readFromParcel(Parcel source) {
        this.srcImgPath = source.readString();
        this.dstImgPath = source.readString();
        this.storageOptions = source.readParcelable(StorageOptionsVO.class.getClassLoader());
        this.watermarkOptions = source.readParcelable(WatermarkOptionsVO.class.getClassLoader());
        this.compressOptions = source.readParcelable(CompressOptionsVO.class.getClassLoader());
        this.addWatermarkCallbackFuncId = source.readString();
    }

    protected AddWatermarkVO(Parcel in) {
        this.srcImgPath = in.readString();
        this.dstImgPath = in.readString();
        this.storageOptions = in.readParcelable(StorageOptionsVO.class.getClassLoader());
        this.watermarkOptions = in.readParcelable(WatermarkOptionsVO.class.getClassLoader());
        this.compressOptions = in.readParcelable(CompressOptionsVO.class.getClassLoader());
        this.addWatermarkCallbackFuncId = in.readString();
    }

    public static final Creator<AddWatermarkVO> CREATOR = new Creator<AddWatermarkVO>() {
        @Override
        public AddWatermarkVO createFromParcel(Parcel source) {
            return new AddWatermarkVO(source);
        }

        @Override
        public AddWatermarkVO[] newArray(int size) {
            return new AddWatermarkVO[size];
        }
    };
}
