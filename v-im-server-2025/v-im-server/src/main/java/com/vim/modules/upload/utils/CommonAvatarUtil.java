package com.vim.modules.upload.utils;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.regex.Pattern;

/**
 * 通用头像工具类，生成文字头像
 */
public class CommonAvatarUtil extends BaseAvatarUtil {

    private static final Logger log = LoggerFactory.getLogger(CommonAvatarUtil.class);

    /**
     * 中文正则
     */
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]");

    /**
     * 预置颜色
     */
    private static final int[] BEAUTIFUL_COLORS = {
            0x7265E6,
            0xFCBF00,
            0x00A2AE,
            0xF56A00,
            0x1890FF,
            0x606D80
    };

    /**
     * 此工具类不可被实例化
     */
    private CommonAvatarUtil() {
    }

    /**
     * 生成文字头像
     *
     * @param name 用户名
     * @return 头像URL
     */
    public static String generateImg(String name) {
        try {
            if (StrUtil.isBlank(name)) {
                return null;
            }

            // 获取一个字符
            String nameChar = getNameChar(name);
            // 获取随机颜色
            Color color = getRandomColor();
            // 生成图片
            BufferedImage bi = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = (Graphics2D) bi.getGraphics();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setBackground(getRandomColor());
            g2.clearRect(0, 0, 400, 400);
            g2.setPaint(color);
            Font font = new Font("微软雅黑", Font.PLAIN, 200);
            g2.setFont(font);
            // 文字长度
            FontMetrics metrics = g2.getFontMetrics(font);
            // 字符位置
            int x = (400 - metrics.stringWidth(nameChar)) / 2;
            int y = (400 - metrics.getHeight()) / 2 + metrics.getAscent();
            g2.drawString(nameChar, x, y);

            return BaseAvatarUtil.uploadImage(bi);
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    /**
     * 获得随机颜色
     **/
    private static Color getRandomColor() {
        return new Color(BEAUTIFUL_COLORS[RandomUtil.randomInt(BEAUTIFUL_COLORS.length)]);
    }

    /**
     * 获得一个字符
     *
     * @param name 名称
     * @return 字符
     */
    private static String getNameChar(String name) {
        // 如果用户输入的姓名少于等于2个字符，不用截取
        String nameWritten = name;
        if (name.length() > 2) {
            // 如果用户输入的姓名大于等于3个字符，截取后面两位
            if (isChinese(StrUtil.sub(name, 0, 1))) {
                // 截取倒数两位汉字
                nameWritten = name.substring(name.length() - 2);
            } else {
                // 截取前面的两个英文字母
                nameWritten = StrUtil.sub(name, 0, 1).toUpperCase();
            }
        }
        // 两个字及以上
        if (nameWritten.length() >= 2) {
            // 两个中文 如 言曌
            if (isChinese(StrUtil.sub(nameWritten, 0, 1)) && isChinese(StrUtil.sub(nameWritten, 0, 2))) {
                return nameWritten;
            }
            // 首中次英 如 罗Q
            else if (isChinese(StrUtil.sub(nameWritten, 0, 1)) && !isChinese(StrUtil.sub(nameWritten, 0, 2))) {
                return nameWritten;
            }
            // 首英 如 AB
            else {
                return nameWritten.substring(0, 1);
            }
        }
        // 一个字
        if (nameWritten.length() == 1) {
            // 中文
            if (isChinese(nameWritten)) {
                return nameWritten;
            } else {
                return nameWritten.toUpperCase();
            }
        }
        return nameWritten;
    }

    /**
     * 判断字符串是否为中文
     **/
    private static boolean isChinese(String str) {
        return CHINESE_PATTERN.matcher(str).find();
    }
}
