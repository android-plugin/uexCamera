package org.zywx.wbpalmstar.plugin.uexcamera.vo;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * File Description: 存储配置参数
 * <p>
 * Created by sandy with Email: sandy1108@163.com at Date: 2022/1/17.
 */
public class StorageOptionsVO implements Parcelable {
    private String isPublic;

    public String getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(String isPublic) {
        this.isPublic = isPublic;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.isPublic);
    }

    public void readFromParcel(Parcel source) {
        this.isPublic = source.readString();
    }

    public StorageOptionsVO() {
    }

    protected StorageOptionsVO(Parcel in) {
        this.isPublic = in.readString();
    }

    public static final Creator<StorageOptionsVO> CREATOR = new Creator<StorageOptionsVO>() {
        @Override
        public StorageOptionsVO createFromParcel(Parcel source) {
            return new StorageOptionsVO(source);
        }

        @Override
        public StorageOptionsVO[] newArray(int size) {
            return new StorageOptionsVO[size];
        }
    };
}
