package com.selfimpr.captcha.service.impl;


import com.google.code.kaptcha.Producer;
import com.selfimpr.captcha.controller.CaptchaController;
import com.selfimpr.captcha.exception.ServiceException;
import com.selfimpr.captcha.exception.code.ServiceExceptionCode;
import com.selfimpr.captcha.model.dto.ImageVerificationDto;
import com.selfimpr.captcha.model.vo.ImageVerificationVo;
import com.selfimpr.captcha.service.CaptchaService;
import com.selfimpr.captcha.utils.ImageRead;
import com.selfimpr.captcha.utils.ImageVerificationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;


/**
 * @Description: 验证码业务实现类
 */

@Service
public class CaptchaServiceImpl implements CaptchaService {

    /**
     * 日志
     */
    private static final Logger log = LoggerFactory.getLogger(CaptchaController.class);


    /**
     * 源图路径前缀
     */
    @Value("${captcha.slide-verification-code.path.origin-image:classpath:static/targets}")
    private String verificationImagePathPrefix;

    /**
     * 模板图路径前缀
     */
    @Value("${captcha.slide-verification-code.path.template-image:classpath:static/templates}")
    private String templateImagePathPrefix;


    /**
     * 获取request对象
     *
     * @return 返回request对象
     */
    protected static HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    /**
     * 获取response对象
     *
     * @return 返回response对象
     */
    protected static HttpServletResponse getResponse() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
    }


    /**
     * 根据类型获取验证码
     *
     * @param imageVerificationDto 用户信息
     * @return 图片验证码
     * @throws ServiceException 查询图片验证码异常
     */
    @Override
    public ImageVerificationVo selectImageVerificationCode(ImageVerificationDto imageVerificationDto) throws ServiceException {
        ImageVerificationVo imageVerificationVo = null;
        try {
            imageVerificationVo = selectSlideVerificationCode(imageVerificationDto);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(ServiceExceptionCode.SELECT_VERIFICATION_CODE_ERROR);
        }
        return imageVerificationVo;
    }


    /**
     * 获取滑动验证码
     *
     * @param imageVerificationDto 验证码参数
     * @return 滑动验证码
     * @throws ServiceException 获取滑动验证码异常
     */
    public ImageVerificationVo selectSlideVerificationCode(ImageVerificationDto imageVerificationDto) throws ServiceException {

        ImageVerificationVo imageVerificationVo = null;
        try {

            //  随机取得原图文件夹中一张图片
            ImageRead originImageRead = readTargetImage();
            /*
              获取模板图片文件 classpath:static/templates
              templateImagePathPrefix.concat("/template.png") 拼接字符串
               */
            ImageRead templateImageRead = readTemplateImage(templateImagePathPrefix.concat("/template.png"));
            //  获取描边图片文件
            ImageRead borderImageRead = readBorderImageFile(templateImagePathPrefix.concat("/border.png"));
            //  获取原图文件类型
            String originImageFileType = originImageRead.getFileExtension();
            //  获取模板图文件类型
            String templateImageFileType = templateImageRead.getFileExtension();
            //  获取描边图文件类型
            String borderImageFileType = borderImageRead.getFileExtension();

            //  读取原图
            BufferedImage verificationImage = originImageRead.getImage();
            //  读取模板图
            BufferedImage readTemplateImage = templateImageRead.getImage();
            //  读取描边图片
            BufferedImage borderImage = borderImageRead.getImage();

            //  获取原图感兴趣区域坐标
            imageVerificationVo = ImageVerificationUtil.generateCutoutCoordinates(verificationImage, readTemplateImage);

            int y = imageVerificationVo.getY();
            //  在分布式应用中，可将session改为redis存储
            getRequest().getSession().setAttribute("imageVerificationVo", imageVerificationVo);

            // 根据原图生成遮罩图（地下带有凹槽的那张图）和切块图（就是滑动的那张图 ）
            imageVerificationVo = ImageVerificationUtil.pictureTemplateCutout(verificationImage, originImageRead.getInputStream(), originImageFileType, readTemplateImage, templateImageFileType, imageVerificationVo.getX(), imageVerificationVo.getY());

            // 切块图描边
            imageVerificationVo = ImageVerificationUtil.cutoutImageEdge(imageVerificationVo, borderImage, borderImageFileType);
            imageVerificationVo.setY(y);

            //  =============================================
            //  输出图片
//            HttpServletResponse response = getResponse();
//            response.setContentType("image/jpeg");
//            ServletOutputStream outputStream = response.getOutputStream();
//            outputStream.write(oriCopyImages);
//            BufferedImage bufferedImage = ImageIO.read(originImageFile);
//            ImageIO.write(bufferedImage, originImageType, outputStream);
//            outputStream.flush();
            //  =================================================

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(ServiceExceptionCode.IO_EXCEPTION);
        }

        return imageVerificationVo;
    }

    /**
     * 读取目标图
     *  从指定文件路径下随机读取一张图片 输入流 后缀 放入ImageRead对象中返回
     * @return
     * @throws ServiceException
     */
    public ImageRead readTargetImage() throws ServiceException {
        ImageRead imageRead = null;

        try {
            Random random = new Random(System.currentTimeMillis());
            // 是否配置了源图路径前缀 有classpath是没有配置
            if (verificationImagePathPrefix.indexOf("classpath") >= 0) {
                ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                // classpath:static/targets 获得此路径下所有文件资源
                Resource[] resources = resolver.getResources(verificationImagePathPrefix.concat("/*"));

                if (resources == null) {
                    throw new RuntimeException("not found target image");
                }
                int i = random.nextInt(resources.length);
                imageRead = new ImageRead();
                imageRead.setImage(ImageIO.read(resources[i].getInputStream()));
                String extension = resources[i].getFilename().substring(resources[i].getFilename().lastIndexOf(".") + 1);
                imageRead.setInputStream(resources[i].getInputStream());
                imageRead.setFileExtension(extension);

            // 配置了原图路径前缀
            } else {
                File importImage = new File(verificationImagePathPrefix);
                if (importImage == null) {
                    throw new RuntimeException("not found target image");
                }
                // 列出此路径下所有文件
                File[] files = importImage.listFiles();
                // 随机抽取一张图片
                int i = random.nextInt(files.length);
                imageRead = new ImageRead();
                imageRead.setImage(ImageIO.read(files[i]));
                // 获取图片后缀
                String extension = files[i].getName().substring(files[i].getName().lastIndexOf(".") + 1);
                imageRead.setFileExtension(extension);
                // 图片输入流
                imageRead.setInputStream(new FileInputStream(files[i]));
            }


        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(ServiceExceptionCode.IO_EXCEPTION);
        }
        return imageRead;
    }

    /**
     * 读取模板图
     *
     * @param path
     * @return
     * @throws ServiceException
     */
    public ImageRead readTemplateImage(String path) throws ServiceException {
        ImageRead templateImageFile = null;
        try {
            if (templateImageFile != null) {
                return templateImageFile;
            }
            templateImageFile = getImageRead(path);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(ServiceExceptionCode.IO_EXCEPTION);
        }
        return templateImageFile;
    }


    /**
     * 读取边框图
     *
     * @param path
     * @return
     * @throws ServiceException
     */
    public ImageRead readBorderImageFile(String path) throws ServiceException {
        ImageRead borderImageFile = null;
        try {
            if (borderImageFile != null) {
                return borderImageFile;
            }
            borderImageFile = getImageRead(path);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(ServiceExceptionCode.IO_EXCEPTION);
        }
        return borderImageFile;
    }


    private ImageRead getImageRead(String path) throws IOException {
        ImageRead templateImageFile;
        templateImageFile = new ImageRead();
        if (verificationImagePathPrefix.indexOf("classpath") >= 0) {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResource(path);
            if (resource == null) {
                throw new RuntimeException("not found template image");
            }
            templateImageFile.setImage(ImageIO.read(resource.getInputStream()));
            String extension = resource.getFilename().substring(resource.getFilename().lastIndexOf(".") + 1);
            templateImageFile.setInputStream(resource.getInputStream());
            templateImageFile.setFileExtension(extension);
        } else {
            File file = new File(path);
            templateImageFile.setImage(ImageIO.read(file));
            String extension = file.getName().substring(file.getName().lastIndexOf(".") + 1);
            templateImageFile.setInputStream(new FileInputStream(file));
            templateImageFile.setFileExtension(extension);
        }
        return templateImageFile;
    }


    /**
     * 滑动验证码验证方法
     *
     * @param x x轴坐标
     * @param y y轴坐标
     * @return 滑动验证码验证状态
     * @throws ServiceException 验证滑动验证码异常
     */
    @Override
    public boolean checkVerificationResult(String x, String y) throws ServiceException {

        int threshold = 5;

        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            ImageVerificationVo imageVerificationVo = (ImageVerificationVo) request.getSession().getAttribute("imageVerificationVo");
            if (imageVerificationVo != null) {
                if ((Math.abs(Integer.parseInt(x) - imageVerificationVo.getX()) <= threshold) && y.equals(String.valueOf(imageVerificationVo.getY()))) {
                    System.out.println("验证成功");
                    return true;
                } else {
                    System.out.println("验证失败");
                    return false;
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(ServiceExceptionCode.IO_EXCEPTION);
        }
    }


}