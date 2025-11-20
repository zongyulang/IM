package com.vim.webpage.service.HotSearch;

import com.vim.webpage.domain.HotSearch;

import java.util.List;

/**
 * 热搜榜单服务接口
 */
public interface IHotSearchService {
    
    /**
     * 记录搜索关键词
     * @param keyword 搜索关键词
     * @return 是否记录成功
     */
    boolean recordSearch(String keyword);
    
    
    /**
     * 获取日榜
     * @param date 日期 (格式: 2025-01-20)
     * @param topN 获取前N名，默认50
     * @return 热搜榜单
     */
    HotSearch getDailyHotSearch(String date, int topN);
    
    /**
     * 获取周榜
     * @param weekDate 周日期 (格式: 2025-W03)
     * @param topN 获取前N名，默认50
     * @return 热搜榜单
     */
    HotSearch getWeeklyHotSearch(String weekDate, int topN);
    
    /**
     * 获取月榜
     * @param month 月份 (格式: 2025-01)
     * @param topN 获取前N名，默认50
     * @return 热搜榜单
     */
    HotSearch getMonthlyHotSearch(String month, int topN);
    
    /**
     * 获取今日热搜榜
     * @param topN 获取前N名，默认50
     * @return 热搜榜单
     */
    HotSearch getTodayHotSearch(int topN);
    
    /**
     * 获取本周热搜榜
     * @param topN 获取前N名，默认50
     * @return 热搜榜单
     */
    HotSearch getThisWeekHotSearch(int topN);
    
    /**
     * 获取本月热搜榜
     * @param topN 获取前N名，默认50
     * @return 热搜榜单
     */
    HotSearch getThisMonthHotSearch(int topN);
    
    /**
     * 生成日榜（从Redis统计数据生成并保存到MongoDB）
     * @param date 日期 (格式: 2025-01-20)
     * @return 是否生成成功
     */
    boolean generateDailyHotSearch(String date);
    
    /**
     * 生成周榜
     * @param weekDate 周日期 (格式: 2025-W03)
     * @return 是否生成成功
     */
    boolean generateWeeklyHotSearch(String weekDate);
    
    /**
     * 生成月榜
     * @param month 月份 (格式: 2025-01)
     * @return 是否生成成功
     */
    boolean generateMonthlyHotSearch(String month);
    
    /**
     * 获取实时热搜（从Redis直接读取，不保存）
     * @param topN 获取前N名
     * @return 关键词列表及搜索次数
     */
    List<HotSearch.SearchKeyword> getRealtimeHotSearch(int topN);
    
    /**
     * 清理过期数据
     * @param daysToKeep 保留天数
     * @return 清理的记录数
     */
    long cleanExpiredData(int daysToKeep);
}
