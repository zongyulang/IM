package com.vim.webpage.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 热搜榜单
 * 按日、周、月维度存储搜索热度数据
 */
@Data
@Document(collection = "hot_searches")
@CompoundIndexes({
    @CompoundIndex(name = "period_date_idx", def = "{'period': 1, 'periodDate': -1}", unique = true)
})
public class HotSearch {
    
    @Id
    private String id; // MongoDB 自动生成的 ID
    
    /**
     * 周期类型: daily(日榜), weekly(周榜), monthly(月榜)
     */
    private String period;
    
    /**
     * 周期日期
     * 日榜: 2025-01-20
     * 周榜: 2025-W03 (第3周)
     * 月榜: 2025-01
     */
    private String periodDate;
    
    /**
     * 热搜关键词列表
     */
    private List<SearchKeyword> keywords;
    
    /**
     * 创建时间
     */
    private Date createdAt = new Date();
    
    /**
     * 更新时间
     */
    private Date updatedAt = new Date();
    
    /**
     * 热搜关键词内部类
     */
    @Data
    public static class SearchKeyword {
        /**
         * 排名
         */
        private Integer rank;
        
        /**
         * 搜索关键词
         */
        private String keyword;
        
        /**
         * 搜索次数
         */
        private Long searchCount;
        
        /**
         * 热度值（可以是搜索次数、点击次数等综合计算）
         */
        private Long hotScore;
        
        /**
         * 涨幅（相比上一周期的排名变化）
         * 正数表示上升，负数表示下降，0表示持平
         */
        private Integer rankChange;
        
        /**
         * 是否为新上榜
         */
        private Boolean isNew = false;
        
        /**
         * 标签（如：热、新、爆）
         */
        private String tag;
    }
}

