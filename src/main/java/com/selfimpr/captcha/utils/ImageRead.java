package com.selfimpr.captcha.utils;

import java.awt.image.BufferedImage;
import java.io.InputStream;

public class ImageRead {

    /**
     * 图片对象
     */
    private BufferedImage image;

    /**
     * 文件后缀
     */
    private String fileExtension;

    private InputStream InputStream;

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public InputStream getInputStream() {
        return InputStream;
    }

    public void setInputStream(InputStream inputStream) {
        InputStream = inputStream;
    }
}
