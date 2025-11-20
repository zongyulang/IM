package com.vim.webpage.service.RedisCache2Mongodb.impl;

import com.vim.webpage.domain.MonthlyVideoRanking;
import com.vim.webpage.service.HotSearch.IHotSearchService;
import com.vim.webpage.service.RedisCache2Mongodb.IRedisCache2MongodbService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Redis周期缓存同步到MongoDB服务实现类（用来做缓存键同步到mongodb和elasticsearch）
 * 
 * 功能：
 * 1. 定时任务：每5分钟自动将Redis中的视频播放量数据同步到MongoDB
 * 2. Pub/Sub：监听 im:sync_monthly_ranking 频道，接收到消息时触发同步
 * 3. 手动触发：提供手动同步接口
 * 
 * Redis数据结构：
 * - Key格式：video:monthly:views:yyyy-MM
 * - 类型：ZSet（Sorted Set）
 * - Member：videoId
 * - Score：播放量（views）
 * 
 * @author fres
 */
@Slf4j
@Service
public class RedisCache2MongodbService implements IRedisCache2MongodbService {

    @Resource(name = "webpageStringRedisTemplate")
    private StringRedisTemplate stringRedisTemplate;

    @Resource(name = "webMongoTemplate")
    private MongoTemplate mongoTemplate;

    @Autowired(required = false)
    private IHotSearchService hotSearchService;

    private static final String REDIS_MONTHLY_VIEWS_PREFIX = "video:monthly:views:";
    private static final String MONGO_COLLECTION = "monthly_video_rankings";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    // 用于跟踪热搜任务的执行周期
    private int syncCounter = 0;

    /**
     * 定时任务：每5分钟执行一次同步
     * 每5分钟的第0秒执行一次
     * 0：秒（第0秒）
     * 5：分钟（每5分钟）
     * ：小时（每小时都执行）
     * ：日（每天都执行）
     * ：月（每月都执行）
     * ?：星期（不指定）
     * cron表达式说明：每5分钟的第0秒执行
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void scheduledSync() {
        log.info("⏰ 定时任务触发：开始同步月度视频排行榜和热搜数据...");
        
        // 1. 同步月度视频排行榜
        boolean videoRankingSuccess = syncMonthlyVideoRanking();
        if (videoRankingSuccess) {
            log.info("✅ 定时任务完成：月度排行榜同步成功");
        } else {
            log.warn("⚠️ 定时任务完成：月度排行榜同步失败或无数据");
        }
        
        // 2. 同步热搜数据（每5分钟执行一次，与视频排行榜保持一致）
        syncHotSearchData();
        
        log.info("✅ 定时任务全部完成");
    }
    
    /**
     * 同步热搜数据
     */
    private void syncHotSearchData() {
        // 只用一个Redis key做缓存，所有榜单都从MongoDB获取
        final String REDIS_HOT_SEARCH_KEY = "hot_search:today";
        final String HOT_SEARCH_COLLECTION = "hot_searches";
        if (hotSearchService == null) {
            log.debug("ℹ️ HotSearchService未注入，跳过热搜数据同步");
            return;
        }
        try {
            syncCounter++;
            log.info("🔍 开始同步热搜数据 (第 {} 次执行)", syncCounter);
            LocalDate now = LocalDate.now();
            String today = now.format(DateTimeFormatter.ISO_LOCAL_DATE);
            // 1. 从Redis获取今日热搜缓存
            Set<ZSetOperations.TypedTuple<String>> redisHotSearch = stringRedisTemplate.opsForZSet().reverseRangeWithScores(REDIS_HOT_SEARCH_KEY, 0, -1);
            List<com.vim.webpage.domain.HotSearch.SearchKeyword> keywords = new ArrayList<>();
            int rank = 1;
            if (redisHotSearch != null && !redisHotSearch.isEmpty()) {
                for (ZSetOperations.TypedTuple<String> tuple : redisHotSearch) {
                    com.vim.webpage.domain.HotSearch.SearchKeyword keyword = new com.vim.webpage.domain.HotSearch.SearchKeyword();
                    keyword.setRank(rank++);
                    keyword.setKeyword(tuple.getValue());
                    keyword.setSearchCount(tuple.getScore() != null ? tuple.getScore().longValue() : 0L);
                    keywords.add(keyword);
                }
            }
            // 2. 保存/更新今日热搜榜到MongoDB
            com.vim.webpage.domain.HotSearch todayHotSearch = new com.vim.webpage.domain.HotSearch();
            todayHotSearch.setPeriod("daily");
            todayHotSearch.setPeriodDate(today);
            todayHotSearch.setKeywords(keywords);
            todayHotSearch.setUpdatedAt(new Date());
            Query query = new Query(Criteria.where("period").is("daily").and("periodDate").is(today));
            com.vim.webpage.domain.HotSearch existing = mongoTemplate.findOne(query, com.vim.webpage.domain.HotSearch.class, HOT_SEARCH_COLLECTION);
            if (existing != null) {
                todayHotSearch.setId(existing.getId());
                todayHotSearch.setCreatedAt(existing.getCreatedAt());
            } else {
                todayHotSearch.setCreatedAt(new Date());
            }
            mongoTemplate.save(todayHotSearch, HOT_SEARCH_COLLECTION);
            log.info("✅ 今日热搜榜已同步到MongoDB: {}，关键词数:{}", today, keywords.size());
            // 3. 清空Redis缓存
            stringRedisTemplate.delete(REDIS_HOT_SEARCH_KEY);
            log.info("🧹 已清空Redis热搜缓存 [{}]", REDIS_HOT_SEARCH_KEY);

            // 4. 每小时聚合生成周榜
            if (syncCounter % 12 == 0) {
                String thisWeek = getWeekDate(now);
                // 聚合本周所有日榜
                List<com.vim.webpage.domain.HotSearch> weekDailyList = mongoTemplate.find(
                        new Query(Criteria.where("period").is("daily").and("periodDate").regex("^" + thisWeek.substring(0, 4) + ".*")),
                        com.vim.webpage.domain.HotSearch.class, HOT_SEARCH_COLLECTION);
                Map<String, Long> weekMap = new HashMap<>();
                for (com.vim.webpage.domain.HotSearch day : weekDailyList) {
                    if (day.getKeywords() != null) {
                        for (com.vim.webpage.domain.HotSearch.SearchKeyword k : day.getKeywords()) {
                            weekMap.put(k.getKeyword(), weekMap.getOrDefault(k.getKeyword(), 0L) + k.getSearchCount());
                        }
                    }
                }
                List<com.vim.webpage.domain.HotSearch.SearchKeyword> weekKeywords = weekMap.entrySet().stream()
                        .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                        .limit(50)
                        .map(e -> {
                            com.vim.webpage.domain.HotSearch.SearchKeyword k = new com.vim.webpage.domain.HotSearch.SearchKeyword();
                            k.setKeyword(e.getKey());
                            k.setSearchCount(e.getValue());
                            return k;
                        }).collect(Collectors.toList());
                com.vim.webpage.domain.HotSearch weekHotSearch = new com.vim.webpage.domain.HotSearch();
                weekHotSearch.setPeriod("weekly");
                weekHotSearch.setPeriodDate(thisWeek);
                weekHotSearch.setKeywords(weekKeywords);
                weekHotSearch.setUpdatedAt(new Date());
                Query weekQuery = new Query(Criteria.where("period").is("weekly").and("periodDate").is(thisWeek));
                com.vim.webpage.domain.HotSearch existingWeek = mongoTemplate.findOne(weekQuery, com.vim.webpage.domain.HotSearch.class, HOT_SEARCH_COLLECTION);
                if (existingWeek != null) {
                    weekHotSearch.setId(existingWeek.getId());
                    weekHotSearch.setCreatedAt(existingWeek.getCreatedAt());
                } else {
                    weekHotSearch.setCreatedAt(new Date());
                }
                mongoTemplate.save(weekHotSearch, HOT_SEARCH_COLLECTION);
                log.info("✅ 本周热搜榜已聚合并存入MongoDB: {}，关键词数:{}", thisWeek, weekKeywords.size());
            }

            // 5. 每天聚合生成月榜
            if (syncCounter % 288 == 0) {
                String thisMonth = now.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                // 聚合本月所有日榜
                List<com.vim.webpage.domain.HotSearch> monthDailyList = mongoTemplate.find(
                        new Query(Criteria.where("period").is("daily").and("periodDate").regex("^" + thisMonth + ".*")),
                        com.vim.webpage.domain.HotSearch.class, HOT_SEARCH_COLLECTION);
                Map<String, Long> monthMap = new HashMap<>();
                for (com.vim.webpage.domain.HotSearch day : monthDailyList) {
                    if (day.getKeywords() != null) {
                        for (com.vim.webpage.domain.HotSearch.SearchKeyword k : day.getKeywords()) {
                            monthMap.put(k.getKeyword(), monthMap.getOrDefault(k.getKeyword(), 0L) + k.getSearchCount());
                        }
                    }
                }
                List<com.vim.webpage.domain.HotSearch.SearchKeyword> monthKeywords = monthMap.entrySet().stream()
                        .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                        .limit(50)
                        .map(e -> {
                            com.vim.webpage.domain.HotSearch.SearchKeyword k = new com.vim.webpage.domain.HotSearch.SearchKeyword();
                            k.setKeyword(e.getKey());
                            k.setSearchCount(e.getValue());
                            return k;
                        }).collect(Collectors.toList());
                com.vim.webpage.domain.HotSearch monthHotSearch = new com.vim.webpage.domain.HotSearch();
                monthHotSearch.setPeriod("monthly");
                monthHotSearch.setPeriodDate(thisMonth);
                monthHotSearch.setKeywords(monthKeywords);
                monthHotSearch.setUpdatedAt(new Date());
                Query monthQuery = new Query(Criteria.where("period").is("monthly").and("periodDate").is(thisMonth));
                com.vim.webpage.domain.HotSearch existingMonth = mongoTemplate.findOne(monthQuery, com.vim.webpage.domain.HotSearch.class, HOT_SEARCH_COLLECTION);
                if (existingMonth != null) {
                    monthHotSearch.setId(existingMonth.getId());
                    monthHotSearch.setCreatedAt(existingMonth.getCreatedAt());
                } else {
                    monthHotSearch.setCreatedAt(new Date());
                }
                mongoTemplate.save(monthHotSearch, HOT_SEARCH_COLLECTION);
                log.info("✅ 本月热搜榜已聚合并存入MongoDB: {}，关键词数:{}", thisMonth, monthKeywords.size());
                // 重置计数器，防止溢出
                syncCounter = 0;
            }
        } catch (Exception e) {
            log.error("❌ 同步热搜数据异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 执行Redis到MongoDB的数据同步（增量累加模式）
     * Redis作为5分钟内的临时缓冲区，同步时累加到MongoDB，然后清空Redis
     */
    @Override
    public boolean syncMonthlyVideoRanking() {
        try {
            String currentMonth = getCurrentMonth();
            String redisKey = REDIS_MONTHLY_VIEWS_PREFIX + currentMonth;

            log.info("🔄 开始同步月度排行榜（增量模式）: {}", currentMonth);

            // 1. 从Redis获取本次5分钟内的增量数据
            Set<ZSetOperations.TypedTuple<String>> redisIncrements = stringRedisTemplate.opsForZSet()
                    .reverseRangeWithScores(redisKey, 0, -1);

            if (redisIncrements == null || redisIncrements.isEmpty()) {
                log.info("ℹ️ Redis中没有新增数据，跳过本次同步: {}", currentMonth);
                return true; // 没有数据不算失败
            }

            log.info("📊 本次同步Redis增量数据: {} 个视频", redisIncrements.size());

            // 2. 查询MongoDB中已存在的月度排行榜
            Query query = new Query(Criteria.where("month").is(currentMonth));
            MonthlyVideoRanking existingRanking = mongoTemplate.findOne(
                    query,
                    MonthlyVideoRanking.class,
                    MONGO_COLLECTION);

            // 3. 构建videoId到当前累计播放量的映射（MongoDB中的数据）
            Map<String, Integer> existingViewsMap = new HashMap<>();
            if (existingRanking != null && existingRanking.getVideos() != null) {
                existingRanking.getVideos().forEach(v -> 
                    existingViewsMap.put(v.getVideoId(), v.getViews())
                );
                log.info("📝 MongoDB中已有 {} 个视频的累计数据", existingViewsMap.size());
            } else {
                log.info("📝 MongoDB中暂无该月份记录，将创建新记录");
            }

            // 4. 将Redis的增量数据累加到MongoDB的数据上
            redisIncrements.forEach(tuple -> {
                String videoId = tuple.getValue();
                int increment = tuple.getScore() != null ? tuple.getScore().intValue() : 0;
                
                // 累加：已有播放量 + Redis增量
                int currentViews = existingViewsMap.getOrDefault(videoId, 0);
                int newViews = currentViews + increment;
                existingViewsMap.put(videoId, newViews);
                
                log.debug("  📈 视频 {}: MongoDB累计={}, Redis增量=+{}, 新累计={}", 
                        videoId, currentViews, increment, newViews);
            });

            // 5. 转换为MonthlyVideoRanking格式并按播放量降序排序
            List<MonthlyVideoRanking.VideoRanking> rankings = existingViewsMap.entrySet().stream()
                    .map(entry -> {
                        MonthlyVideoRanking.VideoRanking ranking = new MonthlyVideoRanking.VideoRanking();
                        ranking.setVideoId(entry.getKey());
                        ranking.setViews(entry.getValue());
                        return ranking;
                    })
                    .sorted((a, b) -> Integer.compare(b.getViews(), a.getViews())) // 降序
                    .collect(Collectors.toList());

            // 6. 保存到MongoDB（upsert）
            MonthlyVideoRanking monthlyRanking = new MonthlyVideoRanking();
            monthlyRanking.setMonth(currentMonth);
            monthlyRanking.setVideos(rankings);
            
            if (existingRanking != null) {
                monthlyRanking.setId(existingRanking.getId());
            }

            mongoTemplate.save(monthlyRanking, MONGO_COLLECTION);

            // 7. 清空Redis中的增量缓冲区，准备收集下一个5分钟的数据
            try {
                stringRedisTemplate.delete(redisKey);
                log.info("🧹 已清空Redis增量缓冲区 [{}]，准备收集新增量", redisKey);
            } catch (Exception clearEx) {
                log.warn("⚠️ 清空Redis失败: {}", clearEx.getMessage());
            }

            log.info("✅ 同步完成: MongoDB累计 {} 个视频，本次增量 {} 个视频", 
                    rankings.size(), redisIncrements.size());
            
            // 预览TOP 10
            log.debug("📊 当前TOP 10: {}",
                    rankings.stream()
                            .limit(10)
                            .map(r -> String.format("%s(%d)", r.getVideoId(), r.getViews()))
                            .collect(Collectors.joining(", ")));

            return true;

        } catch (Exception e) {
            log.error("❌ 同步月度排行榜失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 增加当月视频的播放量（在Redis中）
     */
    @Override
    public long incrementMonthlyVideoViews(String videoId, int incrementBy) {
        try {
            if (videoId == null || videoId.trim().isEmpty()) {
                log.warn("⚠️ 视频ID为空，无法增加播放量");
                return 0L;
            }

            if (incrementBy <= 0) {
                incrementBy = 1; // 默认增加1
            }

            String currentMonth = getCurrentMonth();
            String redisKey = REDIS_MONTHLY_VIEWS_PREFIX + currentMonth;

            // 使用ZINCRBY命令增加分数（增量累加到Redis缓冲区）
            Double newScore = stringRedisTemplate.opsForZSet().incrementScore(redisKey, videoId, incrementBy);

            long currentBufferViews = newScore != null ? newScore.longValue() : 0L;

            log.debug("📈 视频 {} 增量 +{}, Redis缓冲区当前累计: {} (注意：每5分钟清空一次)", 
                videoId, incrementBy, currentBufferViews);

            // 注意：返回值仅代表Redis缓冲区中的增量累计，每5分钟同步到MongoDB后会清空
            // 要查询真实的月度总播放量，请使用 MonthlyVideoRankingService.getCurrentMonthTotalViews()
            return currentBufferViews;

        } catch (Exception e) {
            log.error("❌ 增加视频月度播放量失败, videoId: {}, incrementBy: {}, 错误: {}",
                    videoId, incrementBy, e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * 获取视频在当前月份的播放量（从Redis）
     */
    @Override
    public long getMonthlyVideoViews(String videoId) {
        try {
            if (videoId == null || videoId.trim().isEmpty()) {
                return 0L;
            }

            String currentMonth = getCurrentMonth();
            String redisKey = REDIS_MONTHLY_VIEWS_PREFIX + currentMonth;

            Double score = stringRedisTemplate.opsForZSet().score(redisKey, videoId);

            long views = score != null ? score.longValue() : 0L;

            log.debug("📊 视频 {} 当前月度播放量: {}", videoId, views);

            return views;

        } catch (Exception e) {
            log.error("❌ 获取视频月度播放量失败, videoId: {}, 错误: {}", videoId, e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * 手动触发同步任务
     */
    @Override
    public String manualSync() {
        log.info("🔧 手动触发同步任务");

        try {
            boolean success = syncMonthlyVideoRanking();

            if (success) {
                String message = String.format("✅ 手动同步成功！月份: %s", getCurrentMonth());
                log.info(message);
                return message;
            } else {
                String message = String.format("⚠️ 手动同步失败或无数据！月份: %s", getCurrentMonth());
                log.warn(message);
                return message;
            }

        } catch (Exception e) {
            String message = String.format("❌ 手动同步异常: %s", e.getMessage());
            log.error(message, e);
            return message;
        }
    }

    /**
     * 获取当前月份字符串（格式：yyyy-MM）
     */
    private String getCurrentMonth() {
        return LocalDate.now().format(MONTH_FORMATTER);
    }
    
    /**
     * 获取周日期 (格式: 2025-W03)
     */
    private String getWeekDate(LocalDate date) {
        WeekFields weekFields = WeekFields.ISO;
        int year = date.get(weekFields.weekBasedYear());
        int week = date.get(weekFields.weekOfWeekBasedYear());
        return String.format("%d-W%02d", year, week);
    }
}
