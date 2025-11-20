package com.vim.webpage.controller.Web;

import com.vim.webpage.domain.HotSearch;
import com.vim.webpage.service.HotSearch.IHotSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 热搜榜单控制器
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HotSearchController {
    
    private final IHotSearchService hotSearchService;
    
    /**
     * 记录搜索关键词
     * POST /api/hot-search/record?keyword=xxx
     */
    @PostMapping("/SearchRecord")
    public ResponseEntity<Map<String, Object>> recordSearch(@RequestParam String keyword) {
        boolean success = hotSearchService.recordSearch(keyword);
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "记录成功" : "记录失败");
        return ResponseEntity.ok(response);
    }
    
    
    /**
     * 获取今日热搜
     * GET /api/hot-search/today?topN=10
     */
    @GetMapping("/todayHotSearch")
    public ResponseEntity<Map<String, Object>> getTodayHotSearch(
            @RequestParam(defaultValue = "10") int count) {
        HotSearch hotSearch = hotSearchService.getTodayHotSearch(count);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", hotSearch);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取本周热搜
     * GET /api/hot-search/this-week?topN=50
     */
    @GetMapping("/this-week")
    public ResponseEntity<Map<String, Object>> getThisWeekHotSearch(
            @RequestParam(defaultValue = "50") int topN) {
        HotSearch hotSearch = hotSearchService.getThisWeekHotSearch(topN);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", hotSearch);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取本月热搜
     * GET /api/hot-search/this-month?topN=50
     */
    @GetMapping("/this-month")
    public ResponseEntity<Map<String, Object>> getThisMonthHotSearch(
            @RequestParam(defaultValue = "50") int topN) {
        HotSearch hotSearch = hotSearchService.getThisMonthHotSearch(topN);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", hotSearch);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取指定日期的日榜
     * GET /api/hot-search/daily/{date}?topN=50
     * 例如: /api/hot-search/daily/2025-01-20
     */
    @GetMapping("/daily/{date}")
    public ResponseEntity<Map<String, Object>> getDailyHotSearch(
            @PathVariable String date,
            @RequestParam(defaultValue = "50") int topN) {
        HotSearch hotSearch = hotSearchService.getDailyHotSearch(date, topN);
        Map<String, Object> response = new HashMap<>();
        response.put("success", hotSearch != null);
        response.put("data", hotSearch);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取指定周的周榜
     * GET /api/hot-search/weekly/{weekDate}?topN=50
     * 例如: /api/hot-search/weekly/2025-W03
     */
    @GetMapping("/weekly/{weekDate}")
    public ResponseEntity<Map<String, Object>> getWeeklyHotSearch(
            @PathVariable String weekDate,
            @RequestParam(defaultValue = "50") int topN) {
        HotSearch hotSearch = hotSearchService.getWeeklyHotSearch(weekDate, topN);
        Map<String, Object> response = new HashMap<>();
        response.put("success", hotSearch != null);
        response.put("data", hotSearch);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取指定月的月榜
     * GET /api/hot-search/monthly/{month}?topN=50
     * 例如: /api/hot-search/monthly/2025-01
     */
    @GetMapping("/monthly/{month}")
    public ResponseEntity<Map<String, Object>> getMonthlyHotSearch(
            @PathVariable String month,
            @RequestParam(defaultValue = "50") int topN) {
        HotSearch hotSearch = hotSearchService.getMonthlyHotSearch(month, topN);
        Map<String, Object> response = new HashMap<>();
        response.put("success", hotSearch != null);
        response.put("data", hotSearch);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取实时热搜
     * GET /api/hot-search/realtime?topN=50
     */
    @GetMapping("/realtime")
    public ResponseEntity<Map<String, Object>> getRealtimeHotSearch(
            @RequestParam(defaultValue = "50") int topN) {
        List<HotSearch.SearchKeyword> keywords = hotSearchService.getRealtimeHotSearch(topN);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", keywords);
        response.put("count", keywords.size());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 生成日榜（手动触发）
     * POST /api/hot-search/generate/daily?date=2025-01-20
     */
    @PostMapping("/generate/daily")
    public ResponseEntity<Map<String, Object>> generateDailyHotSearch(@RequestParam String date) {
        boolean success = hotSearchService.generateDailyHotSearch(date);
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "生成成功" : "生成失败");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 生成周榜（手动触发）
     * POST /api/hot-search/generate/weekly?weekDate=2025-W03
     */
    @PostMapping("/generate/weekly")
    public ResponseEntity<Map<String, Object>> generateWeeklyHotSearch(@RequestParam String weekDate) {
        boolean success = hotSearchService.generateWeeklyHotSearch(weekDate);
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "生成成功" : "生成失败");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 生成月榜（手动触发）
     * POST /api/hot-search/generate/monthly?month=2025-01
     */
    @PostMapping("/generate/monthly")
    public ResponseEntity<Map<String, Object>> generateMonthlyHotSearch(@RequestParam String month) {
        boolean success = hotSearchService.generateMonthlyHotSearch(month);
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "生成成功" : "生成失败");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 清理过期数据
     * DELETE /api/hot-search/clean?daysToKeep=90
     */
    @DeleteMapping("/clean")
    public ResponseEntity<Map<String, Object>> cleanExpiredData(
            @RequestParam(defaultValue = "90") int daysToKeep) {
        long deletedCount = hotSearchService.cleanExpiredData(daysToKeep);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "清理完成");
        response.put("deletedCount", deletedCount);
        return ResponseEntity.ok(response);
    }
}
