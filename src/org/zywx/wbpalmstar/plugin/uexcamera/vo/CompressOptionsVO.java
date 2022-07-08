package org.zywx.wbpalmstar.plugin.uexcamera.vo;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * File Description: 压缩参数
 * <p>
 * Created by sandy with Email: sandy1108@163.com at Date: 2022/1/17.
 */

public class CompressOptionsVO implements Parcelable {
    private long fileSize;
    private PhotoSizeVO photoSize;
    private int quality = 100;
    private int isCompress;

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public PhotoSizeVO getPhotoSize() {
        return photoSize;
    }

    public void setPhotoSize(PhotoSizeVO photoSize) {
        this.photoSize = photoSize;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public int getIsCompress() {
        return isCompress;
    }

    public void setIsCompress(int isCompress) {
        this.isCompress = isCompress;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.fileSize);
        dest.writeParcelable(this.photoSize, flags);
        dest.writeInt(this.quality);
        dest.writeInt(this.isCompress);
    }

    public void readFromParcel(Parcel source) {
        this.fileSize = source.readLong();
        this.photoSize = source.readParcelable(PhotoSizeVO.class.getClassLoader());
        this.quality = source.readInt();
        this.isCompress = source.readInt();
    }

    public CompressOptionsVO() {
    }

    protected CompressOptionsVO(Parcel in) {
        this.fileSize = in.readLong();
        this.photoSize = in.readParcelable(PhotoSizeVO.class.getClassLoader());
        this.quality = in.readInt();
        this.isCompress = in.readInt();
    }

    public static final Creator<CompressOptionsVO> CREATOR = new Creator<CompressOptionsVO>() {
        @Override
        public CompressOptionsVO createFromParcel(Parcel source) {
            return new CompressOptionsVO(source);
        }

        @Override
        public CompressOptionsVO[] newArray(int size) {
            return new CompressOptionsVO[size];
        }
    };
}
