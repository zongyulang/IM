package com.vim.webpage.service.MonthlyVideoRanking.impl;

import com.vim.webpage.domain.MonthlyVideoRanking;
import com.vim.webpage.domain.Video;
import com.vim.webpage.service.MonthlyVideoRanking.IMonthlyVideoRankingService;
import com.vim.webpage.service.Video.VideoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 月度视频排行榜服务实现类
 * 负责从Redis读取当前月份的视频播放量数据，并返回排行榜
 * 
 * @author fres
 */
@Slf4j
@Service
public class MonthlyVideoRankingService implements IMonthlyVideoRankingService {

    @Resource(name = "webpageStringRedisTemplate")
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private VideoService videoService;

    private static final String REDIS_MONTHLY_VIEWS_PREFIX = "video:monthly:views:";
    private static final String MONGO_COLLECTION = "monthly_video_rankings";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int DEFAULT_TOP_N = 100;

    /**
     * 获取当前月份的热门视频排行榜
     */
    @Override
    public List<Video> getCurrentMonthTopVideos(int topN, String lang) {
        String currentMonth = getCurrentMonth();
        return getMonthTopVideos(currentMonth, topN, lang);
    }

    /**
     * 获取指定月份的热门视频排行榜
     */
    @Override
    public List<Video> getMonthTopVideos(String month, int topN, String lang) {
        try {
            if (!StringUtils.hasText(month)) {
                month = getCurrentMonth();
            }

            if (topN <= 0) {
                topN = DEFAULT_TOP_N;
            }

            // 1. 先从MongoDB查询历史数据
            MonthlyVideoRanking ranking = getMonthlyRankingFromMongo(month);
            
            // 2. 如果是当前月份，优先从Redis获取最新数据
            if (month.equals(getCurrentMonth())) {
                List<Video> redisVideos = getTopVideosFromRedis(month, topN, lang);
                if (!redisVideos.isEmpty()) {
                    log.info("✅ 从Redis获取当前月份 {} 的排行榜数据，共 {} 条", month, redisVideos.size());
                    return redisVideos;
                }
            }

            // 3. 从MongoDB获取历史月份数据
            if (ranking != null && ranking.getVideos() != null) {
                List<MonthlyVideoRanking.VideoRanking> videoRankings = ranking.getVideos().stream()
                        .limit(topN)
                        .collect(Collectors.toList());

                List<String> videoIds = videoRankings.stream()
                        .map(MonthlyVideoRanking.VideoRanking::getVideoId)
                        .collect(Collectors.toList());

                // 批量获取视频详情
                List<Video> videos = videoService.getVideos(videoIds, lang);

                // 按照排行榜顺序排序并设置views
                Map<String, Integer> viewsMap = videoRankings.stream()
                        .collect(Collectors.toMap(
                                MonthlyVideoRanking.VideoRanking::getVideoId,
                                MonthlyVideoRanking.VideoRanking::getViews
                        ));

                videos.forEach(video -> {
                    Integer views = viewsMap.get(video.getId());
                    if (views != null) {
                        video.setViews(views);
                    }
                });

                // 按照播放量降序排序
                videos.sort((v1, v2) -> Integer.compare(v2.getViews(), v1.getViews()));

                log.info("✅ 从MongoDB获取月份 {} 的排行榜数据，共 {} 条", month, videos.size());
                return videos;
            }

            log.warn("⚠️ 未找到月份 {} 的排行榜数据", month);
            return new ArrayList<>();

        } catch (Exception e) {
            log.error("❌ 获取月度排行榜失败, month: {}, topN: {}, 错误: {}", month, topN, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 从Redis获取当前月份的TOP视频
     */
    private List<Video> getTopVideosFromRedis(String month, int topN, String lang) {
        // 已改造：当前方法改为从 MongoDB 获取（命名保持兼容）
        try {
            MonthlyVideoRanking ranking = getMonthlyRankingFromMongo(month);
            if (ranking == null || ranking.getVideos() == null || ranking.getVideos().isEmpty()) {
                log.debug("MongoDB 中月份 {} 没有排行数据", month);
                return new ArrayList<>();
            }

            List<MonthlyVideoRanking.VideoRanking> limited = ranking.getVideos().stream()
                    .sorted((a, b) -> Integer.compare(b.getViews(), a.getViews()))
                    .limit(topN)
                    .collect(Collectors.toList());

            List<String> videoIds = limited.stream().map(MonthlyVideoRanking.VideoRanking::getVideoId).collect(Collectors.toList());
            List<Video> videos = videoService.getVideos(videoIds, lang);

            Map<String, Integer> viewMap = limited.stream()
                    .collect(Collectors.toMap(MonthlyVideoRanking.VideoRanking::getVideoId, MonthlyVideoRanking.VideoRanking::getViews));

            videos.forEach(v -> {
                Integer views = viewMap.get(v.getId());
                if (views != null) {
                    v.setViews(views);
                }
            });

            videos.sort((v1, v2) -> Integer.compare(v2.getViews(), v1.getViews()));
            return videos;
        } catch (Exception e) {
            log.error("❌ 从MongoDB获取排行榜失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 从MongoDB获取月度排行榜
     */
    private MonthlyVideoRanking getMonthlyRankingFromMongo(String month) {
        try {
            Query query = new Query(Criteria.where("month").is(month));
            return mongoTemplate.findOne(query, MonthlyVideoRanking.class, MONGO_COLLECTION);
        } catch (Exception e) {
            log.error("❌ 从MongoDB查询排行榜失败, month: {}, 错误: {}", month, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 手动触发当前月份排行榜数据同步
     */
    @Override
    public boolean syncCurrentMonthRanking() {
        try {
            String currentMonth = getCurrentMonth();
            log.info("🔄 开始手动同步月度排行榜 [增量模式]: {}", currentMonth);

            // 1️⃣ 从Redis读取当前5分钟内的增量数据
            String redisKey = REDIS_MONTHLY_VIEWS_PREFIX + currentMonth;
            Set<ZSetOperations.TypedTuple<String>> redisIncrements = stringRedisTemplate.opsForZSet()
                    .reverseRangeWithScores(redisKey, 0, -1);

            if (redisIncrements == null || redisIncrements.isEmpty()) {
                log.warn("⚠️ Redis中没有当前月份的增量数据: {}", currentMonth);
                return false;
            }

            // 2️⃣ 从MongoDB读取已有的累计数据
            Query query = new Query(Criteria.where("month").is(currentMonth));
            MonthlyVideoRanking existing = mongoTemplate.findOne(query, MonthlyVideoRanking.class, MONGO_COLLECTION);

            // 构建现有数据的Map: videoId -> currentViews
            Map<String, Integer> existingViewsMap = new HashMap<>();
            if (existing != null && existing.getVideos() != null) {
                for (MonthlyVideoRanking.VideoRanking vr : existing.getVideos()) {
                    existingViewsMap.put(vr.getVideoId(), vr.getViews());
                }
            }

            log.info("📊 MongoDB累计 {} 个视频，本次增量 {} 个视频", existingViewsMap.size(), redisIncrements.size());

            // 3️⃣ 累加增量到MongoDB数据
            for (ZSetOperations.TypedTuple<String> tuple : redisIncrements) {
                String videoId = tuple.getValue();
                int increment = tuple.getScore() != null ? tuple.getScore().intValue() : 0;

                int currentViews = existingViewsMap.getOrDefault(videoId, 0);
                int newViews = currentViews + increment;

                existingViewsMap.put(videoId, newViews);
                log.debug("🔢 视频 {} 累加: MongoDB现有={}, Redis增量={}, 累计后={}", 
                    videoId, currentViews, increment, newViews);
            }

            // 4️⃣ 转换为排序后的列表
            List<MonthlyVideoRanking.VideoRanking> rankings = existingViewsMap.entrySet().stream()
                    .map(entry -> {
                        MonthlyVideoRanking.VideoRanking ranking = new MonthlyVideoRanking.VideoRanking();
                        ranking.setVideoId(entry.getKey());
                        ranking.setViews(entry.getValue());
                        return ranking;
                    })
                    .sorted((a, b) -> Integer.compare(b.getViews(), a.getViews())) // 降序排序
                    .collect(Collectors.toList());

            // 5️⃣ 保存累加后的结果到MongoDB
            MonthlyVideoRanking monthlyRanking = new MonthlyVideoRanking();
            monthlyRanking.setMonth(currentMonth);
            monthlyRanking.setVideos(rankings);

            if (existing != null) {
                monthlyRanking.setId(existing.getId());
            }

            mongoTemplate.save(monthlyRanking, MONGO_COLLECTION);

            // 6️⃣ 清除Redis增量缓冲区（为下一个5分钟做准备）
            try {
                Boolean removed = stringRedisTemplate.delete(redisKey);
                log.info("🧹 已清除 Redis 增量缓冲区 [{}]，结果: {}", redisKey, removed);
            } catch (Exception ex) {
                log.warn("⚠️ 清除 Redis 增量缓冲区失败: {}", ex.getMessage());
            }

            log.info("✅ 成功同步月度排行榜 [增量模式]: {}, 总计 {} 条记录（本次增量: {} 条）", 
                currentMonth, rankings.size(), redisIncrements.size());
            return true;

        } catch (Exception e) {
            log.error("❌ 同步月度排行榜失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取指定视频在当前月份的排名
     */
    @Override
    public int getVideoRankingInCurrentMonth(String videoId) {
        try {
            if (!StringUtils.hasText(videoId)) {
                return -1;
            }

            String currentMonth = getCurrentMonth();
            String redisKey = REDIS_MONTHLY_VIEWS_PREFIX + currentMonth;

            // 获取视频在ZSet中的排名（reverseRank返回降序排名，0表示第一名）
            Long rank = stringRedisTemplate.opsForZSet().reverseRank(redisKey, videoId);

            if (rank == null) {
                log.debug("视频 {} 不在当前月份 {} 的排行榜中", videoId, currentMonth);
                return -1;
            }

            // 转换为从1开始的排名
            return rank.intValue() + 1;

        } catch (Exception e) {
            log.error("❌ 获取视频排名失败, videoId: {}, 错误: {}", videoId, e.getMessage(), e);
            return -1;
        }
    }

    /**
     * 获取当前月份总观看次数
     */
    @Override
    public long getCurrentMonthTotalViews() {
        try {
            String currentMonth = getCurrentMonth();
            
            // 优先从MongoDB获取（已同步的数据）
            MonthlyVideoRanking ranking = getMonthlyRankingFromMongo(currentMonth);
            if (ranking != null && ranking.getVideos() != null) {
                long totalViews = ranking.getVideos().stream()
                        .mapToLong(v -> v.getViews() != null ? v.getViews() : 0L)
                        .sum();
                log.debug("当前月份 {} 总观看次数（MongoDB）: {}", currentMonth, totalViews);
                return totalViews;
            }

            // 如果MongoDB没有数据，从Redis获取
            String redisKey = REDIS_MONTHLY_VIEWS_PREFIX + currentMonth;
            Set<ZSetOperations.TypedTuple<String>> allVideos = stringRedisTemplate.opsForZSet()
                    .rangeWithScores(redisKey, 0, -1);

            if (allVideos == null || allVideos.isEmpty()) {
                return 0L;
            }

            long totalViews = allVideos.stream()
                    .mapToLong(tuple -> tuple.getScore() != null ? tuple.getScore().longValue() : 0L)
                    .sum();

            log.debug("当前月份 {} 总观看次数（Redis）: {}", currentMonth, totalViews);
            return totalViews;

        } catch (Exception e) {
            log.error("❌ 获取总观看次数失败: {}", e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * 获取可用的月份列表
     */
    @Override
    public List<String> getAvailableMonths() {
        try {
            log.debug("🔍 查询可用的月份列表");

            // 从MongoDB查询所有月度排行榜记录，只返回month字段
            Query query = new Query();
            query.fields().include("month");

            List<MonthlyVideoRanking> rankings = mongoTemplate.find(
                    query,
                    MonthlyVideoRanking.class,
                    MONGO_COLLECTION
            );

            // 提取月份并按时间降序排序
            List<String> months = rankings.stream()
                    .map(MonthlyVideoRanking::getMonth)
                    .filter(month -> month != null && !month.isEmpty())
                    .distinct()
                    .sorted((m1, m2) -> m2.compareTo(m1)) // 降序：最新月份在前
                    .collect(Collectors.toList());

            log.info("✅ 查询到 {} 个可用月份: {}", months.size(), months);
            return months;

        } catch (Exception e) {
            log.error("❌ 获取可用月份列表失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取当前月份字符串（格式：yyyy-MM）
     */
    private String getCurrentMonth() {
        return LocalDate.now().format(MONTH_FORMATTER);
    }
}

