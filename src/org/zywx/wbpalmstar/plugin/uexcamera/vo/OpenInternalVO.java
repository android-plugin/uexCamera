package org.zywx.wbpalmstar.plugin.uexcamera.vo;

/**
 * File Description: 打开自定义相机的参数实体类
 * <p>
 * Created by sandy with Email: sandy1108@163.com at Date: 2022/1/13.
 */
public class OpenInternalVO {

    private WatermarkOptions watermarkOptions;
    private CompressOptions compressOptions;
    private String openInternalCallbackFuncId;

    public OpenInternalVO() {
    }

    public WatermarkOptions getWatermarkOptions() {
        return watermarkOptions;
    }

    public void setWatermarkOptions(WatermarkOptions watermarkOptions) {
        this.watermarkOptions = watermarkOptions;
    }

    public CompressOptions getCompressOptions() {
        return compressOptions;
    }

    public void setCompressOptions(CompressOptions compressOptions) {
        this.compressOptions = compressOptions;
    }

    public String getOpenInternalCallbackFuncId() {
        return openInternalCallbackFuncId;
    }

    public void setOpenInternalCallbackFuncId(String openInternalCallbackFuncId) {
        this.openInternalCallbackFuncId = openInternalCallbackFuncId;
    }

    public static class WatermarkOptions {
        private int paddingY;
        private int paddingX;
        private String position;
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
    }

    public static class CompressOptions {
        private long fileSize;
        private PhotoSize photoSize;
        private int quality;
        private int isCompress;

        public long getFileSize() {
            return fileSize;
        }

        public void setFileSize(long fileSize) {
            this.fileSize = fileSize;
        }

        public PhotoSize getPhotoSize() {
            return photoSize;
        }

        public void setPhotoSize(PhotoSize photoSize) {
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
    }

    public static class PhotoSize {
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
    }
}
