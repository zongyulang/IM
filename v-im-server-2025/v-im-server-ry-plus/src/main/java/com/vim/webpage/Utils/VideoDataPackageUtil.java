package com.vim.webpage.Utils;

import com.vim.webpage.domain.Video;
import org.springframework.util.StringUtils;

/**
 * 视频数据打包工具类
 * 用于根据语言处理视频数据的多语言字段
 */
public class VideoDataPackageUtil {

    /**
     * 根据语言打包视频数据字段
     *
     * @param video 视频对象
     * @param lang 语言代码 (zhcn, zhtw, enus, jajp, kokr, eses, thth, vivn, msmy)
     * @return 处理后的视频对象
     */
    public static Video packageVideoDataFromFields(Video video, String lang) {
        if (video == null) {
            return null;
        }

        // 如果没有指定语言或语言为空，返回原始视频
        if (!StringUtils.hasText(lang)) {
            return video;
        }

        // 标准化语言代码（转小写）
        String normalizedLang = lang.toLowerCase();

        // 处理描述
        String localizedDescription = getLocalizedDescription(video, normalizedLang);
        if (StringUtils.hasText(localizedDescription)) {
            video.setDescription(localizedDescription);
        }

        // 处理作者
        String localizedAuthor = getLocalizedAuthor(video, normalizedLang);
        if (StringUtils.hasText(localizedAuthor)) {
            video.setAuthor(localizedAuthor);
        }

        return video;
    }

    /**
     * 获取本地化的描述
     */
    private static String getLocalizedDescription(Video video, String lang) {
        switch (lang) {
            case "zhtw":
                return StringUtils.hasText(video.getDescriptionZHTW()) ? video.getDescriptionZHTW() : video.getDescription();
            case "enus":
                return StringUtils.hasText(video.getDescriptionENUS()) ? video.getDescriptionENUS() : video.getDescription();
            case "jajp":
                return StringUtils.hasText(video.getDescriptionJAJP()) ? video.getDescriptionJAJP() : video.getDescription();
            case "kokr":
                return StringUtils.hasText(video.getDescriptionKOKR()) ? video.getDescriptionKOKR() : video.getDescription();
            case "eses":
                return StringUtils.hasText(video.getDescriptionESES()) ? video.getDescriptionESES() : video.getDescription();
            case "thth":
                return StringUtils.hasText(video.getDescriptionTHTH()) ? video.getDescriptionTHTH() : video.getDescription();
            case "vivn":
                return StringUtils.hasText(video.getDescriptionVIVN()) ? video.getDescriptionVIVN() : video.getDescription();
            case "msmy":
                return StringUtils.hasText(video.getDescriptionMSMY()) ? video.getDescriptionMSMY() : video.getDescription();
            default:
                return video.getDescription();
        }
    }

    /**
     * 获取本地化的作者
     */
    private static String getLocalizedAuthor(Video video, String lang) {
        switch (lang) {
            case "zhtw":
                return StringUtils.hasText(video.getAuthorZHTW()) ? video.getAuthorZHTW() : video.getAuthor();
            case "enus":
                return StringUtils.hasText(video.getAuthorENUS()) ? video.getAuthorENUS() : video.getAuthor();
            case "jajp":
                return StringUtils.hasText(video.getAuthorJAJP()) ? video.getAuthorJAJP() : video.getAuthor();
            case "kokr":
                return StringUtils.hasText(video.getAuthorKOKR()) ? video.getAuthorKOKR() : video.getAuthor();
            case "eses":
                return StringUtils.hasText(video.getAuthorESES()) ? video.getAuthorESES() : video.getAuthor();
            case "thth":
                return StringUtils.hasText(video.getAuthorTHTH()) ? video.getAuthorTHTH() : video.getAuthor();
            case "vivn":
                return StringUtils.hasText(video.getAuthorVIVN()) ? video.getAuthorVIVN() : video.getAuthor();
            case "msmy":
                return StringUtils.hasText(video.getAuthorMSMY()) ? video.getAuthorMSMY() : video.getAuthor();
            default:
                return video.getAuthor();
        }
    }
}
