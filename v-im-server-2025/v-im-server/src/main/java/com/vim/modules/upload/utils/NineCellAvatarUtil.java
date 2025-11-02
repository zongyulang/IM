package com.vim.modules.upload.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * 九宫格头像生成工具类
 */
public class NineCellAvatarUtil extends BaseAvatarUtil {

    private static final Logger log = LoggerFactory.getLogger(NineCellAvatarUtil.class);

    /**
     * 生成九宫格头像
     *
     * @param bufferedImages 图片列表
     * @param totalWidth     总宽度
     * @param interval       间隔
     * @return 头像URL
     */
    public static String generate(List<BufferedImage> bufferedImages, int totalWidth, int interval) {
        int imageCount = bufferedImages.size();
        BufferedImage combinedImage = new BufferedImage(totalWidth, totalWidth, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = combinedImage.createGraphics();

        // Set background color
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, totalWidth, totalWidth);

        int rows = (imageCount > 4) ? 3 : 2; // 3 rows for 9宫格, 2 rows for 4宫格
        int cols = (rows == 3) ? 3 : 2; // 3 columns for 9宫格, 2 columns for 4宫格
        int cellWidth = (totalWidth - interval * (cols + 1)) / cols;
        int cellHeight = (totalWidth - interval * (rows + 1)) / rows;

        // Calculate positions
        int currentX = interval;
        int currentY = interval;

        // Draw images in a grid
        for (int i = 0; i < imageCount; i++) {
            BufferedImage img = bufferedImages.get(i);
            int drawWidth = Math.min(cellWidth, img.getWidth());
            int drawHeight = Math.min(cellHeight, img.getHeight());

            // Center the image within the cell
            int x = currentX + (cellWidth - drawWidth) / 2;
            int y = currentY + (cellHeight - drawHeight) / 2;

            g2d.drawImage(img, x, y, drawWidth, drawHeight, null);

            // Move to the next column or row
            currentX += cellWidth + interval;
            if (currentX + cellWidth > totalWidth) {
                currentX = interval;
                currentY += cellHeight + interval;
            }
        }

        g2d.dispose();

        try {
            return BaseAvatarUtil.uploadImage(combinedImage);
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

}
