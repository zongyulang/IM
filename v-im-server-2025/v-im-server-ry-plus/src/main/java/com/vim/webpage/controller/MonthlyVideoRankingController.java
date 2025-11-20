package com.vim.webpage.controller;

import com.vim.webpage.domain.Video;
import com.vim.webpage.service.MonthlyVideoRanking.IMonthlyVideoRankingService;
import com.vim.webpage.service.RedisCache2Mongodb.IRedisCache2MongodbService;
import com.vim.webpage.service.RedisPublishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 月度视频排行榜控制器
 * 
 * @author vim
 */
@Slf4j
@RestController
@RequestMapping("/api/video/ranking")
public class MonthlyVideoRankingController {

    @Autowired
    private IMonthlyVideoRankingService monthlyVideoRankingService;

    @Autowired
    private IRedisCache2MongodbService redisCache2MongodbService;

    @Autowired
    private RedisPublishService redisPublishService;

    /**
     * 获取当前月份的热门视频排行榜
     * 
     * GET /api/video/ranking/current?topN=100&lang=zhcn
     * 
     * @param topN 获取前N名，默认100
     * @param lang 语言代码
     * @return 视频列表
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentMonthTopVideos(
            @RequestParam(value = "topN", defaultValue = "100") int topN,
            @RequestParam(value = "lang", required = false) String lang) {
        
        try {
            log.info("📊 请求当前月份排行榜: topN={}, lang={}", topN, lang);
            
            List<Video> videos = monthlyVideoRankingService.getCurrentMonthTopVideos(topN, lang);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", videos);
            result.put("count", videos.size());
            result.put("message", "获取成功");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ 获取当前月份排行榜失败: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "获取排行榜失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 获取指定月份的热门视频排行榜
     * 
     * GET /api/video/ranking/month/2025-01?topN=50&lang=enus
     * 
     * @param month 月份格式：YYYY-MM
     * @param topN 获取前N名
     * @param lang 语言代码
     * @return 视频列表
     */
    @GetMapping("/month/{month}")
    public ResponseEntity<Map<String, Object>> getMonthTopVideos(
            @PathVariable String month,
            @RequestParam(value = "topN", defaultValue = "100") int topN,
            @RequestParam(value = "lang", required = false) String lang) {
        
        try {
            log.info("📊 请求指定月份排行榜: month={}, topN={}, lang={}", month, topN, lang);
            
            List<Video> videos = monthlyVideoRankingService.getMonthTopVideos(month, topN, lang);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", videos);
            result.put("month", month);
            result.put("count", videos.size());
            result.put("message", "获取成功");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ 获取指定月份排行榜失败: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "获取排行榜失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 手动触发同步（将Redis数据同步到MongoDB）
     * 
     * POST /api/video/ranking/sync
     * 
     * @return 同步结果
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> manualSync() {
        try {
            log.info("🔧 手动触发排行榜同步");
            
            String result = redisCache2MongodbService.manualSync();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ 手动同步失败: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "同步失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 通过Redis Pub/Sub触发同步
     * 
     * POST /api/video/ranking/sync/publish
     * 
     * @return 发布结果
     */
    @PostMapping("/sync/publish")
    public ResponseEntity<Map<String, Object>> publishSyncMessage() {
        try {
            log.info("📢 发布同步消息到 Redis Pub/Sub");
            
            redisPublishService.publish("im:sync_monthly_ranking", "sync");
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "同步消息已发布，等待处理");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ 发布同步消息失败: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "发布失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 获取视频在当前月份的排名
     * 
     * GET /api/video/ranking/video/{videoId}/rank
     * 
     * @param videoId 视频ID
     * @return 排名信息
     */
    @GetMapping("/video/{videoId}/rank")
    public ResponseEntity<Map<String, Object>> getVideoRank(@PathVariable String videoId) {
        try {
            log.info("🔍 查询视频排名: videoId={}", videoId);
            
            int rank = monthlyVideoRankingService.getVideoRankingInCurrentMonth(videoId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("videoId", videoId);
            result.put("rank", rank);
            result.put("inRanking", rank > 0);
            result.put("message", rank > 0 ? "排名: 第" + rank + "名" : "未进入排行榜");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ 获取视频排名失败: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "获取排名失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 增加视频播放量
     * 
     * POST /api/video/ranking/video/{videoId}/view
     * 
     * @param videoId 视频ID
     * @param increment 增加的播放量，默认1
     * @return 操作结果
     */
    @PostMapping("/video/{videoId}/view")
    public ResponseEntity<Map<String, Object>> incrementVideoView(
            @PathVariable String videoId,
            @RequestParam(value = "increment", defaultValue = "1") int increment) {
        
        try {
            log.info("📈 增加视频播放量: videoId={}, increment={}", videoId, increment);
            
            long newViews = redisCache2MongodbService.incrementMonthlyVideoViews(videoId, increment);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("videoId", videoId);
            result.put("monthlyViews", newViews);
            result.put("message", "播放量已增加");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ 增加视频播放量失败: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "操作失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 获取当前月份总观看次数
     * 
     * GET /api/video/ranking/stats/total-views
     * 
     * @return 统计信息
     */
    @GetMapping("/stats/total-views")
    public ResponseEntity<Map<String, Object>> getTotalViews() {
        try {
            log.info("📊 查询当前月份总观看次数");
            
            long totalViews = monthlyVideoRankingService.getCurrentMonthTotalViews();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("totalViews", totalViews);
            result.put("message", "获取成功");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ 获取总观看次数失败: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "获取失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }


    /**
     * 获取可用的月份列表
     * 
     * GET /api/video/ranking/months
     * 
     * @return 可用月份列表
     */
    @GetMapping("/months")
    public ResponseEntity<Map<String, Object>> getAvailableMonths() {
        try {
            log.info("📅 查询可用月份列表");
            
            List<String> months = monthlyVideoRankingService.getAvailableMonths();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", months);
            result.put("count", months.size());
            result.put("message", "获取成功");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ 获取可用月份列表失败: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "获取失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }
}
