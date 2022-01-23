package org.zywx.wbpalmstar.plugin.uexcamera.vo;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * File Description: 水印参数
 * <p>
 * Created by sandy with Email: sandy1108@163.com at Date: 2022/1/17.
 */

public class WatermarkOptionsVO implements Parcelable {

    public final static String POSITION_CENTER = "center";
    public final static String POSITION_LEFT_TOP = "left-top";
    public final static String POSITION_RIGHT_TOP = "right-top";
    public final static String POSITION_LEFT_BOTTOM = "left-bottom";
    public final static String POSITION_RIGHT_BOTTOM = "right-bottom";

    private int paddingY;
    private int paddingX;
    private String color = "#FFFFFF";
    private int size = 32;
    private String position = POSITION_CENTER;
    private String markImage;
    private String markText;

    public int getPaddingY() {
        return paddingY;
    }

    public void setPaddingY(int paddingY) {
        this.paddingY = paddingY;
    }

    public int getPaddingX() {
        return paddingX;
    }

    public void setPaddingX(int paddingX) {
        this.paddingX = paddingX;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getMarkImage() {
        return markImage;
    }

    public void setMarkImage(String markImage) {
        this.markImage = markImage;
    }

    public String getMarkText() {
        return markText;
    }

    public void setMarkText(String markText) {
        this.markText = markText;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.paddingY);
        dest.writeInt(this.paddingX);
        dest.writeString(this.position);
        dest.writeString(this.markImage);
        dest.writeString(this.markText);
    }

    public void readFromParcel(Parcel source) {
        this.paddingY = source.readInt();
        this.paddingX = source.readInt();
        this.position = source.readString();
        this.markImage = source.readString();
        this.markText = source.readString();
    }

    public WatermarkOptionsVO() {
    }

    protected WatermarkOptionsVO(Parcel in) {
        this.paddingY = in.readInt();
        this.paddingX = in.readInt();
        this.position = in.readString();
        this.markImage = in.readString();
        this.markText = in.readString();
    }

    public static final Creator<WatermarkOptionsVO> CREATOR = new Creator<WatermarkOptionsVO>() {
        @Override
        public WatermarkOptionsVO createFromParcel(Parcel source) {
            return new WatermarkOptionsVO(source);
        }

        @Override
        public WatermarkOptionsVO[] newArray(int size) {
            return new WatermarkOptionsVO[size];
        }
    };
}
