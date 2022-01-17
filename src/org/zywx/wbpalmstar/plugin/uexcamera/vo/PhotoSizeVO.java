package org.zywx.wbpalmstar.plugin.uexcamera.vo;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * File Description: 图片压缩目标尺寸参数
 * <p>
 * Created by sandy with Email: sandy1108@163.com at Date: 2022/1/17.
 */


public class PhotoSizeVO implements Parcelable {
    private int height;
    private int width;

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.height);
        dest.writeInt(this.width);
    }

    public void readFromParcel(Parcel source) {
        this.height = source.readInt();
        this.width = source.readInt();
    }

    public PhotoSizeVO() {
    }

    protected PhotoSizeVO(Parcel in) {
        this.height = in.readInt();
        this.width = in.readInt();
    }

    public static final Creator<PhotoSizeVO> CREATOR = new Creator<PhotoSizeVO>() {
        @Override
        public PhotoSizeVO createFromParcel(Parcel source) {
            return new PhotoSizeVO(source);
        }

        @Override
        public PhotoSizeVO[] newArray(int size) {
            return new PhotoSizeVO[size];
        }
    };
}
