package com.vim.webpage.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

import java.util.List;

@Data
@Document(collection = "videos") // 指定 MongoDB 集合名称
public class Video {

    @Id
    private String id; // MongoDB 自动生成的 ID
    @Indexed
    private String userId; // 引用 UserModel 的 _id
    private String thumbnailUrl;
    private String videoUrl;
    private Boolean isFullHD;
    private Integer views;
    private Integer duration;
    private String description;
    private String descriptionZHTW;
    private String descriptionJAJP;
    private String descriptionENUS;
    private String descriptionKOKR;
    private String descriptionESES;
    private String descriptionTHTH;
    private String descriptionVIVN;
    private String descriptionMSMY;
    private String author;
    private String authorZHTW;
    private String authorJAJP;
    private String authorENUS;
    private String authorKOKR;
    private String authorESES;
    private String authorTHTH;
    private String authorVIVN;
    private String authorMSMY;
    @Indexed
    private Long date; // 使用 Long 类型存储时间戳
    private String categories;
    private List<String> tags;
    private Integer likes;
    private Integer dislikes;
    private Integer likeRate;
    private Integer saveVideo;
    private Integer commentsCount;

}