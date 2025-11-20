package com.vim.webpage.service.HotSearch.impl;

import com.vim.webpage.domain.HotSearch;
import com.vim.webpage.service.HotSearch.IHotSearchService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 热搜榜单服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotSearchServiceImpl implements IHotSearchService {
    
    @Resource(name = "webMongoTemplate")
    private final MongoTemplate mongoTemplate;
    
    @Resource(name = "webpageStringRedisTemplate")
    private final StringRedisTemplate stringRedisTemplate;
    
    private final com.vim.webpage.service.VideoTags.IVideoTagsService videoTagsService;
    
    // 只用一个Redis key做缓存，所有榜单都从MongoDB获取
    private static final String REDIS_HOT_SEARCH_KEY = "hot_search:today";
    private static final String COLLECTION_NAME = "hot_searches";
    
    //记录搜索关键词
    @Override
    public boolean recordSearch(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            log.warn("⚠️ 搜索关键词为空，跳过记录");
            return false;
        }
        
        try {
            keyword = keyword.trim();
            // 只用作缓存，无需日期变量
            
            // 记录到日榜Redis
            stringRedisTemplate.opsForZSet().incrementScore(REDIS_HOT_SEARCH_KEY, keyword, 1);
            
            log.debug("📊 记录搜索关键词: {}", keyword);
            return true;
            
        } catch (Exception e) {
            log.error("❌ 记录搜索关键词失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    //获取日榜
    @Override
    public HotSearch getDailyHotSearch(String date, int topN) {
        try {
            Query query = new Query(Criteria.where("period").is("daily")
                    .and("periodDate").is(date));
            HotSearch hotSearch = mongoTemplate.findOne(query, HotSearch.class, COLLECTION_NAME);
            
            if (hotSearch != null && hotSearch.getKeywords() != null) {
                // 只返回前topN个
                List<HotSearch.SearchKeyword> limitedKeywords = hotSearch.getKeywords().stream()
                        .limit(topN)
                        .collect(Collectors.toList());
                hotSearch.setKeywords(limitedKeywords);
            }
            
            return hotSearch;
        } catch (Exception e) {
            log.error("❌ 获取日榜失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    //获取周榜
    @Override
    public HotSearch getWeeklyHotSearch(String weekDate, int topN) {
        try {
            Query query = new Query(Criteria.where("period").is("weekly")
                    .and("periodDate").is(weekDate));
            HotSearch hotSearch = mongoTemplate.findOne(query, HotSearch.class, COLLECTION_NAME);
            
            if (hotSearch != null && hotSearch.getKeywords() != null) {
                List<HotSearch.SearchKeyword> limitedKeywords = hotSearch.getKeywords().stream()
                        .limit(topN)
                        .collect(Collectors.toList());
                hotSearch.setKeywords(limitedKeywords);
            }
            
            return hotSearch;
        } catch (Exception e) {
            log.error("❌ 获取周榜失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    //获取月榜
    @Override
    public HotSearch getMonthlyHotSearch(String month, int topN) {
        try {
            Query query = new Query(Criteria.where("period").is("monthly")
                    .and("periodDate").is(month));
            HotSearch hotSearch = mongoTemplate.findOne(query, HotSearch.class, COLLECTION_NAME);
            
            if (hotSearch != null && hotSearch.getKeywords() != null) {
                List<HotSearch.SearchKeyword> limitedKeywords = hotSearch.getKeywords().stream()
                        .limit(topN)
                        .collect(Collectors.toList());
                hotSearch.setKeywords(limitedKeywords);
            }
            
            return hotSearch;
        } catch (Exception e) {
            log.error("❌ 获取月榜失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    //获取今日排行榜，保证获取topN个词，热搜没有随机从标签库补足
    @Override
    public HotSearch getTodayHotSearch(int topN) {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        HotSearch hotSearch = getDailyHotSearch(today, topN);
        // 只从MongoDB获取，不再从Redis生成榜单

        // 如果依然没有，或数量不足，则从VideoTags中补足
        if (hotSearch == null) {
            hotSearch = new HotSearch();
            hotSearch.setPeriod("daily");
            hotSearch.setPeriodDate(today);
            hotSearch.setKeywords(new ArrayList<>());
        }
        if (hotSearch.getKeywords() == null) {
            hotSearch.setKeywords(new ArrayList<>());
        }
        int currentSize = hotSearch.getKeywords().size();
        if (currentSize < topN) {
            int need = topN - currentSize;
            // 先尝试一次性获取所需数量的随机标签，去重
            Set<String> used = hotSearch.getKeywords().stream().map(HotSearch.SearchKeyword::getKeyword).collect(Collectors.toSet());
            List<String> randomTags = videoTagsService.getRandomTagsByLanguage("zhcn", need * 2); // 多取一些避免重复
            List<String> filtered = randomTags.stream().filter(t -> !used.contains(t)).collect(Collectors.toList());
            if (filtered.size() < need) {
                // 不足时再补：获取全部语言标签做兜底
                List<String> allTags = videoTagsService.getAllTagsByLanguage("zhcn");
                for (String tag : allTags) {
                    if (filtered.size() >= need) break;
                    if (!used.contains(tag) && !filtered.contains(tag)) {
                        filtered.add(tag);
                    }
                }
            }
            for (int i = 0; i < Math.min(need, filtered.size()); i++) {
                HotSearch.SearchKeyword keyword = new HotSearch.SearchKeyword();
                keyword.setRank(currentSize + i + 1);
                keyword.setKeyword(filtered.get(i));
                hotSearch.getKeywords().add(keyword);
            }
        }
        // 最终只返回topN个
        if (hotSearch.getKeywords().size() > topN) {
            hotSearch.setKeywords(hotSearch.getKeywords().subList(0, topN));
        }
        return hotSearch;
    }
    
    @Override
    public HotSearch getThisWeekHotSearch(int topN) {
        String thisWeek = getWeekDate(LocalDate.now());
        HotSearch hotSearch = getWeeklyHotSearch(thisWeek, topN);
        
        // 只从MongoDB获取，不再从Redis生成榜单
        
        return hotSearch;
    }
    
    @Override
    public HotSearch getThisMonthHotSearch(int topN) {
        String thisMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        HotSearch hotSearch = getMonthlyHotSearch(thisMonth, topN);
        
        // 只从MongoDB获取，不再从Redis生成榜单
        
        return hotSearch;
    }
    
    @Override
    public boolean generateDailyHotSearch(String date) {
        try {
            // 只从MongoDB获取，不再从Redis生成榜单
            return false;
        } catch (Exception e) {
            log.error("❌ 生成日榜失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean generateWeeklyHotSearch(String weekDate) {
        try {
            // 只从MongoDB获取，不再从Redis生成榜单
            return false;
        } catch (Exception e) {
            log.error("❌ 生成周榜失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean generateMonthlyHotSearch(String month) {
        try {
            // 只从MongoDB获取，不再从Redis生成榜单
            return false;
        } catch (Exception e) {
            log.error("❌ 生成月榜失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public List<HotSearch.SearchKeyword> getRealtimeHotSearch(int topN) {
        try {
        Set<ZSetOperations.TypedTuple<String>> topKeywords = 
            stringRedisTemplate.opsForZSet().reverseRangeWithScores(REDIS_HOT_SEARCH_KEY, 0, topN - 1);
            
            if (topKeywords == null || topKeywords.isEmpty()) {
                return Collections.emptyList();
            }
            
            List<HotSearch.SearchKeyword> keywords = new ArrayList<>();
            int rank = 1;
            for (ZSetOperations.TypedTuple<String> tuple : topKeywords) {
                HotSearch.SearchKeyword keyword = new HotSearch.SearchKeyword();
                keyword.setRank(rank++);
                keyword.setKeyword(tuple.getValue());

                keywords.add(keyword);
            }
            
            return keywords;
            
        } catch (Exception e) {
            log.error("❌ 获取实时热搜失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public long cleanExpiredData(int daysToKeep) {
        try {
            LocalDate cutoffDate = LocalDate.now().minusDays(daysToKeep);
            String cutoffDateStr = cutoffDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            Query query = new Query(Criteria.where("periodDate").lt(cutoffDateStr));
            long deletedCount = mongoTemplate.remove(query, HotSearch.class, COLLECTION_NAME).getDeletedCount();
            
            log.info("🧹 清理过期热搜数据: {} 条 (保留 {} 天)", deletedCount, daysToKeep);
            return deletedCount;
            
        } catch (Exception e) {
            log.error("❌ 清理过期数据失败: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    // 已废弃：generateHotSearchFromRedis 方法已移除，Redis只做缓存
    
    /**
     * 生成并保存热搜榜单到MongoDB
     */
    // 已废弃：generateAndSaveHotSearch 方法已移除，Redis只做缓存
    
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
