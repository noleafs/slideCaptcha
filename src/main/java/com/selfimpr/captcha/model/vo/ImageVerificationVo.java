package com.selfimpr.captcha.model.vo;


import java.io.Serializable;

/**
 * @Date 2021年4月27日
 * @Description: 封装图片验证码
 */

public class ImageVerificationVo implements Serializable {


    /**
     * 滑动验证码，源图
     *   都是以base64字符串来存储的
     */
    private String originImage;

    /**
     * 滑动验证码，遮罩图
     */
    private String shadeImage;

    /**
     * 滑动验证码，切块图
     */
    private String cutoutImage;

    /**
     * 滑动验证码，X轴
     */
    private int x;

    /**
     * 滑动验证码，Y轴
     */
    private int y;

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public String getOriginImage() {
        return originImage;
    }

    public void setOriginImage(String originImage) {
        this.originImage = originImage;
    }

    public String getShadeImage() {
        return shadeImage;
    }

    public void setShadeImage(String shadeImage) {
        this.shadeImage = shadeImage;
    }

    public String getCutoutImage() {
        return cutoutImage;
    }

    public void setCutoutImage(String cutoutImage) {
        this.cutoutImage = cutoutImage;
    }
}