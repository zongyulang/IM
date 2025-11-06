package com.vim.webpage.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Document(collection = "users") // 指定 MongoDB 集合名称
public class User {

    @Id
    private String id; // MongoDB 自动生成的 ID
    private String username; // 用户名
    private String usernameZHTW; // 繁体中文用户名
    private String usernameENUS; // 英文用户名
    private String usernameJAJP; // 日文用户名
    private String usernameKOKR; // 韩文用户名
    private String usernameESES; // 西班牙文用户名
    private String usernameTHTH; // 泰文用户名
    private String usernameVIVN; // 越南文用户名
    private String usernameMSMY; // 马来文用户名
    @Indexed(unique = true)
    private String email; // 邮箱
    private String password; // 密码
    private String key; // 密钥
    private List<String> style; // 风格
    private Integer publicCount = 0; // 公开计数
    private Integer likeVideoCount = 0; // 点赞视频计数
    private Integer videoHistoryCount = 0; // 视频历史计数
    private Integer saveVideoCount = 0; // 保存视频计数
    private Integer dislikeVideoCount = 0; // 不喜欢视频计数
    private String avatarUrl = ""; // 头像 URL
    private Boolean isVerifyEmail = false; // 是否验证邮箱
    private Integer totalViews = 0; // 总浏览量
    private Integer rank = 0; // 排名
    private Integer views = 0; // 浏览量
    private Integer subscribe = 0; // 订阅数
    private String introduce = "签名等待编辑"; // 介绍
    private String introduceZHTW = "签名等待编辑"; // 繁体中文介绍
    private String introduceENUS = "签名等待编辑"; // 英文介绍
    private String introduceJAJP = "签名等待编辑"; // 日文介绍
    private String introduceKOKR = "签名等待编辑"; // 韩文介绍
    private String introduceESES = "签名等待编辑"; // 西班牙文介绍
    private String introduceTHTH = "签名等待编辑"; // 泰文介绍
    private String introduceVIVN = "签名等待编辑"; // 越南文介绍
    private String introduceMSMY = "签名等待编辑"; // 马来文介绍
    private String sex = "其他"; // 性别
    private Integer height; // 身高，单位厘米
    private String relationshipStatus = "保密"; // 关系状态
    private Integer likeCommentCount = 0; // 点赞评论计数
    private String currentCity; // 现居城市
    private String hometown; // 出生地
    private String ethnicity; // 种族
    private List<String> hobbies; // 爱好列表
    private Integer homepageViews = 0; // 主页浏览量
    private List<SocialLink> socialLinks; // 社交链接
    private Date birthdate; // 出生日期
    private Integer followerCount = 0; // 粉丝数
    private Integer followingCount = 0; // 关注数
    private String followInfo; // 关注信息
    private Date createdAt = new Date(); // 创建时间
    private Date updatedAt = new Date(); // 更新时间

    @Data
    public static class SocialLink {
        private String platform; // 平台
        private String url; // URL
    }
}