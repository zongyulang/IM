package com.vim.webpage.service.MonthlyVideoRanking;

import com.vim.webpage.domain.Video;
import java.util.List;

/**
 * 月度视频排行榜服务接口
 * 
 * @author fres
 */
public interface IMonthlyVideoRankingService {

    /**
     * 获取当前月份的热门视频排行榜
     * 
     * @param topN 获取前N名，默认100
     * @param lang 语言代码 (zhcn, zhtw, enus, jajp, kokr, eses, thth, vivn, msmy)
     * @return 视频列表（按播放量降序）
     */
    List<Video> getCurrentMonthTopVideos(int topN, String lang);

    /**
     * 获取指定月份的热门视频排行榜
     * 
     * @param month 月份格式：YYYY-MM
     * @param topN 获取前N名
     * @param lang 语言代码
     * @return 视频列表（按播放量降序）
     */
    List<Video> getMonthTopVideos(String month, int topN, String lang);

    /**
     * 手动触发当前月份排行榜数据同步（从Redis到MongoDB）
     * 
     * @return 是否同步成功
     */
    boolean syncCurrentMonthRanking();

    /**
     * 获取指定视频在当前月份的排名
     * 
     * @param videoId 视频ID
     * @return 排名（从1开始），如果不在排行榜中返回-1
     */
    int getVideoRankingInCurrentMonth(String videoId);

    /**
     * 获取当前月份总观看次数
     * 
     * @return 总观看次数
     */
    long getCurrentMonthTotalViews();

    /**
     * 获取可用的月份列表
     * 从MongoDB中查询所有已存在的月度排行榜月份
     * 
     * @return 月份列表（格式：YYYY-MM），按时间降序排列
     */
    List<String> getAvailableMonths();

}
