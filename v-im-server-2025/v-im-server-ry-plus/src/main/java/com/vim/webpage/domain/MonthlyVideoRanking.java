package com.vim.webpage.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

import java.util.List;

@Data
@Document(collection = "monthly_video_rankings") // 指定 MongoDB 集合名称
public class MonthlyVideoRanking {

    @Id
    private String id; // MongoDB 自动生成的 ID
    private String month; // 存储月份，格式：YYYY-MM
    private List<VideoRanking> videos; // 视频排行榜

    @Data
    public static class VideoRanking {
        private String videoId; // 视频 ID
        private Integer views; // 视频观看次数
    }
}