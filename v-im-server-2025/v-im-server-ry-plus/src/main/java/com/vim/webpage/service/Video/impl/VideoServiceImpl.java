package com.vim.webpage.service.Video.impl;

import com.vim.webpage.service.Video.VideoService;
import com.vim.webpage.domain.Video;
import com.vim.webpage.Utils.CDN_decrypt;
import com.vim.webpage.Utils.VideoDataPackageUtil;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @description:视频获取的业务逻辑
 * 视频业务逻辑实现类
 */
@Slf4j
@Service
public class VideoServiceImpl implements VideoService {

    @Resource(name = "webMongoTemplate")
    private MongoTemplate mongoTemplate;

    @Autowired
    private CDN_decrypt cdnDecrypt;

    private static final String COLLECTION_NAME = "videos";

    /**
     * 创建新视频
     */
    @Override
    public Video createVideo(Video video) {
        try {
            if (video == null) {
                throw new IllegalArgumentException("视频对象不能为空");
            }

            // 设置默认值
            if (video.getViews() == null) video.setViews(0);
            if (video.getLikes() == null) video.setLikes(0);
            if (video.getDislikes() == null) video.setDislikes(0);
            if (video.getSaveVideo() == null) video.setSaveVideo(0);
            if (video.getCommentsCount() == null) video.setCommentsCount(0);
            if (video.getDate() == null) video.setDate(System.currentTimeMillis());

            // 保存到 MongoDB
            Video savedVideo = mongoTemplate.save(video, COLLECTION_NAME);
            log.info("✅ 成功创建视频: {}", savedVideo.getId());

            return savedVideo;
        } catch (Exception e) {
            log.error("❌ 创建视频失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建视频失败", e);
        }
    }

    /**
     * 根据 ID 获取视频
     */
    @Override
    public Optional<Video> getVideoById(String id) {
        return getVideoById(id, null);
    }

    /**
     * 根据 ID 获取视频（带语言参数）
     */
    @Override
    public Optional<Video> getVideoById(String id, String lang) {
        try {
            if (!StringUtils.hasText(id)) {
                return Optional.empty();
            }

            Video video = mongoTemplate.findById(id, Video.class, COLLECTION_NAME);
            
            if (video == null) {
                return Optional.empty();
            }

            // 处理视频和缩略图 URL
            processVideoUrls(video);

            // 应用多语言处理
            if (StringUtils.hasText(lang)) {
                video = VideoDataPackageUtil.packageVideoDataFromFields(video, lang);
            }

            return Optional.of(video);
        } catch (Exception e) {
            log.error("❌ 根据ID获取视频失败, ID: {}, 错误: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * 根据用户 ID 获取视频列表
     */
    @Override
    public List<Video> getVideosByUserId(String userId, int page, int size) {
        return getVideosByUserId(userId, page, size, null);
    }

    /**
     * 根据用户 ID 获取视频列表（带语言参数）
     */
    @Override
    public List<Video> getVideosByUserId(String userId, int page, int size, String lang) {
        try {
            if (!StringUtils.hasText(userId)) {
                return new ArrayList<>();
            }

            Query query = new Query(Criteria.where("userId").is(userId));
            query.with(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date")));

            List<Video> videos = mongoTemplate.find(query, Video.class, COLLECTION_NAME);

            return processVideoList(videos, lang);
        } catch (Exception e) {
            log.error("❌ 根据用户ID获取视频列表失败, userId: {}, 错误: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 批量获取视频
     */
    @Override
    public List<Video> getVideos(List<String> videoIds, String lang) {
        try {
            if (videoIds == null || videoIds.isEmpty()) {
                return new ArrayList<>();
            }

            Query query = new Query(Criteria.where("_id").in(videoIds));
            List<Video> videos = mongoTemplate.find(query, Video.class, COLLECTION_NAME);

            return processVideoList(videos, lang);
        } catch (Exception e) {
            log.error("❌ 批量获取视频失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取随机视频
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Video> getRandomVideos(int count, String lang) {
        try {
            Aggregation aggregation = Aggregation.newAggregation(
                    Aggregation.sample(count)
            );

            AggregationResults<Video> results = mongoTemplate.aggregate(
                    aggregation,
                    COLLECTION_NAME,
                    Video.class
            );

            List<Video> videos = results.getMappedResults();
            return processVideoList(videos, lang);
        } catch (Exception e) {
            log.error("❌ 获取随机视频失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 根据分类获取视频
     */
    @Override
    public List<Video> getVideosByCategory(String category, int page, int size, String lang) {
        try {
            if (!StringUtils.hasText(category)) {
                return new ArrayList<>();
            }

            Query query = new Query(Criteria.where("categories").is(category));
            query.with(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date")));

            List<Video> videos = mongoTemplate.find(query, Video.class, COLLECTION_NAME);

            return processVideoList(videos, lang);
        } catch (Exception e) {
            log.error("❌ 根据分类获取视频失败, category: {}, 错误: {}", category, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 根据标签获取视频
     */
    @Override
    public List<Video> getVideosByTag(String tag, int page, int size, String lang) {
        try {
            if (!StringUtils.hasText(tag)) {
                return new ArrayList<>();
            }

            Query query = new Query(Criteria.where("tags").in(tag));
            query.with(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date")));

            List<Video> videos = mongoTemplate.find(query, Video.class, COLLECTION_NAME);

            return processVideoList(videos, lang);
        } catch (Exception e) {
            log.error("❌ 根据标签获取视频失败, tag: {}, 错误: {}", tag, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 搜索视频（根据描述或作者）
     */
    @Override
    public List<Video> searchVideos(String keyword, int page, int size, String lang) {
        try {
            if (!StringUtils.hasText(keyword)) {
                return new ArrayList<>();
            }

            // 创建正则表达式进行模糊搜索
            Pattern pattern = Pattern.compile(keyword, Pattern.CASE_INSENSITIVE);

            Criteria criteria = new Criteria().orOperator(
                    Criteria.where("description").regex(pattern),
                    Criteria.where("descriptionZHTW").regex(pattern),
                    Criteria.where("descriptionENUS").regex(pattern),
                    Criteria.where("descriptionJAJP").regex(pattern),
                    Criteria.where("descriptionKOKR").regex(pattern),
                    Criteria.where("author").regex(pattern),
                    Criteria.where("authorZHTW").regex(pattern),
                    Criteria.where("authorENUS").regex(pattern),
                    Criteria.where("authorJAJP").regex(pattern),
                    Criteria.where("authorKOKR").regex(pattern)
            );

            Query query = new Query(criteria);
            query.with(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date")));

            List<Video> videos = mongoTemplate.find(query, Video.class, COLLECTION_NAME);

            return processVideoList(videos, lang);
        } catch (Exception e) {
            log.error("❌ 搜索视频失败, keyword: {}, 错误: {}", keyword, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取热门视频（按浏览量排序）
     */
    @Override
    public List<Video> getPopularVideos(int page, int size, String lang) {
        try {
            Query query = new Query();
            query.with(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "views")));

            List<Video> videos = mongoTemplate.find(query, Video.class, COLLECTION_NAME);

            return processVideoList(videos, lang);
        } catch (Exception e) {
            log.error("❌ 获取热门视频失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取最新视频（按日期排序）
     */
    @Override
    public List<Video> getLatestVideos(int page, int size, String lang) {
        try {
            Query query = new Query();
            query.with(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date")));

            List<Video> videos = mongoTemplate.find(query, Video.class, COLLECTION_NAME);

            return processVideoList(videos, lang);
        } catch (Exception e) {
            log.error("❌ 获取最新视频失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 更新视频信息
     */
    @Override
    public Video updateVideo(String id, Video video) {
        try {
            if (!StringUtils.hasText(id) || video == null) {
                throw new IllegalArgumentException("视频ID和视频对象不能为空");
            }

            Video existingVideo = mongoTemplate.findById(id, Video.class, COLLECTION_NAME);
            if (existingVideo == null) {
                throw new RuntimeException("视频不存在: " + id);
            }

            video.setId(id);
            Video updatedVideo = mongoTemplate.save(video, COLLECTION_NAME);
            log.info("✅ 成功更新视频: {}", id);

            return updatedVideo;
        } catch (Exception e) {
            log.error("❌ 更新视频失败, ID: {}, 错误: {}", id, e.getMessage(), e);
            throw new RuntimeException("更新视频失败", e);
        }
    }

    /**
     * 删除视频
     */
    @Override
    public void deleteVideo(String id) {
        try {
            if (!StringUtils.hasText(id)) {
                throw new IllegalArgumentException("视频ID不能为空");
            }

            Query query = new Query(Criteria.where("_id").is(id));
            mongoTemplate.remove(query, Video.class, COLLECTION_NAME);
            log.info("✅ 成功删除视频: {}", id);
        } catch (Exception e) {
            log.error("❌ 删除视频失败, ID: {}, 错误: {}", id, e.getMessage(), e);
            throw new RuntimeException("删除视频失败", e);
        }
    }

    /**
     * 增加视频浏览量
     */
    @Override
    public void incrementViews(String videoId) {
        try {
            if (!StringUtils.hasText(videoId)) {
                throw new IllegalArgumentException("视频ID不能为空");
            }

            Query query = new Query(Criteria.where("_id").is(videoId));
            Update update = new Update().inc("views", 1);
            mongoTemplate.updateFirst(query, update, Video.class, COLLECTION_NAME);
            log.debug("✅ 视频浏览量已增加: {}", videoId);
        } catch (Exception e) {
            log.error("❌ 增加浏览量失败, videoId: {}, 错误: {}", videoId, e.getMessage(), e);
        }
    }

    /**
     * 点赞视频
     */
    @Override
    public void likeVideo(String videoId) {
        try {
            if (!StringUtils.hasText(videoId)) {
                throw new IllegalArgumentException("视频ID不能为空");
            }

            Query query = new Query(Criteria.where("_id").is(videoId));
            Update update = new Update().inc("likes", 1);
            mongoTemplate.updateFirst(query, update, Video.class, COLLECTION_NAME);
            
            // 更新点赞率
            updateLikeRate(videoId);
            
            log.debug("✅ 视频点赞成功: {}", videoId);
        } catch (Exception e) {
            log.error("❌ 点赞视频失败, videoId: {}, 错误: {}", videoId, e.getMessage(), e);
        }
    }

    /**
     * 取消点赞视频
     */
    @Override
    public void unlikeVideo(String videoId) {
        try {
            if (!StringUtils.hasText(videoId)) {
                throw new IllegalArgumentException("视频ID不能为空");
            }

            Query query = new Query(Criteria.where("_id").is(videoId));
            Update update = new Update().inc("likes", -1);
            mongoTemplate.updateFirst(query, update, Video.class, COLLECTION_NAME);
            
            // 更新点赞率
            updateLikeRate(videoId);
            
            log.debug("✅ 取消点赞成功: {}", videoId);
        } catch (Exception e) {
            log.error("❌ 取消点赞失败, videoId: {}, 错误: {}", videoId, e.getMessage(), e);
        }
    }

    /**
     * 踩视频
     */
    @Override
    public void dislikeVideo(String videoId) {
        try {
            if (!StringUtils.hasText(videoId)) {
                throw new IllegalArgumentException("视频ID不能为空");
            }

            Query query = new Query(Criteria.where("_id").is(videoId));
            Update update = new Update().inc("dislikes", 1);
            mongoTemplate.updateFirst(query, update, Video.class, COLLECTION_NAME);
            
            // 更新点赞率
            updateLikeRate(videoId);
            
            log.debug("✅ 踩视频成功: {}", videoId);
        } catch (Exception e) {
            log.error("❌ 踩视频失败, videoId: {}, 错误: {}", videoId, e.getMessage(), e);
        }
    }

    /**
     * 取消踩视频
     */
    @Override
    public void undislikeVideo(String videoId) {
        try {
            if (!StringUtils.hasText(videoId)) {
                throw new IllegalArgumentException("视频ID不能为空");
            }

            Query query = new Query(Criteria.where("_id").is(videoId));
            Update update = new Update().inc("dislikes", -1);
            mongoTemplate.updateFirst(query, update, Video.class, COLLECTION_NAME);
            
            // 更新点赞率
            updateLikeRate(videoId);
            
            log.debug("✅ 取消踩视频成功: {}", videoId);
        } catch (Exception e) {
            log.error("❌ 取消踩视频失败, videoId: {}, 错误: {}", videoId, e.getMessage(), e);
        }
    }

    /**
     * 收藏视频
     */
    @Override
    public void saveVideo(String videoId) {
        try {
            if (!StringUtils.hasText(videoId)) {
                throw new IllegalArgumentException("视频ID不能为空");
            }

            Query query = new Query(Criteria.where("_id").is(videoId));
            Update update = new Update().inc("saveVideo", 1);
            mongoTemplate.updateFirst(query, update, Video.class, COLLECTION_NAME);
            log.debug("✅ 收藏视频成功: {}", videoId);
        } catch (Exception e) {
            log.error("❌ 收藏视频失败, videoId: {}, 错误: {}", videoId, e.getMessage(), e);
        }
    }

    /**
     * 取消收藏视频
     */
    @Override
    public void unsaveVideo(String videoId) {
        try {
            if (!StringUtils.hasText(videoId)) {
                throw new IllegalArgumentException("视频ID不能为空");
            }

            Query query = new Query(Criteria.where("_id").is(videoId));
            Update update = new Update().inc("saveVideo", -1);
            mongoTemplate.updateFirst(query, update, Video.class, COLLECTION_NAME);
            log.debug("✅ 取消收藏成功: {}", videoId);
        } catch (Exception e) {
            log.error("❌ 取消收藏失败, videoId: {}, 错误: {}", videoId, e.getMessage(), e);
        }
    }

    /**
     * 增加评论数
     */
    @Override
    public void incrementComments(String videoId) {
        try {
            if (!StringUtils.hasText(videoId)) {
                throw new IllegalArgumentException("视频ID不能为空");
            }

            Query query = new Query(Criteria.where("_id").is(videoId));
            Update update = new Update().inc("commentsCount", 1);
            mongoTemplate.updateFirst(query, update, Video.class, COLLECTION_NAME);
            log.debug("✅ 评论数已增加: {}", videoId);
        } catch (Exception e) {
            log.error("❌ 增加评论数失败, videoId: {}, 错误: {}", videoId, e.getMessage(), e);
        }
    }

    /**
     * 减少评论数
     */
    @Override
    public void decrementComments(String videoId) {
        try {
            if (!StringUtils.hasText(videoId)) {
                throw new IllegalArgumentException("视频ID不能为空");
            }

            Query query = new Query(Criteria.where("_id").is(videoId));
            Update update = new Update().inc("commentsCount", -1);
            mongoTemplate.updateFirst(query, update, Video.class, COLLECTION_NAME);
            log.debug("✅ 评论数已减少: {}", videoId);
        } catch (Exception e) {
            log.error("❌ 减少评论数失败, videoId: {}, 错误: {}", videoId, e.getMessage(), e);
        }
    }

    /**
     * 获取视频总数
     */
    @Override
    public long getTotalCount() {
        try {
            return mongoTemplate.count(new Query(), Video.class, COLLECTION_NAME);
        } catch (Exception e) {
            log.error("❌ 获取视频总数失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 获取用户的视频总数
     */
    @Override
    public long getUserVideoCount(String userId) {
        try {
            if (!StringUtils.hasText(userId)) {
                return 0;
            }

            Query query = new Query(Criteria.where("userId").is(userId));
            return mongoTemplate.count(query, Video.class, COLLECTION_NAME);
        } catch (Exception e) {
            log.error("❌ 获取用户视频总数失败, userId: {}, 错误: {}", userId, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 处理视频和缩略图 URL
     */
    private void processVideoUrls(Video video) {
        if (video != null) {
            if (StringUtils.hasText(video.getVideoUrl())) {
                String videoUrl = video.getVideoUrl();
                if (!videoUrl.startsWith("/")) {
                    videoUrl = "/" + videoUrl;
                }
                video.setVideoUrl(cdnDecrypt.generateSignedPathWithVersion(videoUrl));
            }

            if (StringUtils.hasText(video.getThumbnailUrl())) {
                String thumbnailUrl = video.getThumbnailUrl();
                if (!thumbnailUrl.startsWith("/")) {
                    thumbnailUrl = "/" + thumbnailUrl;
                }
                video.setThumbnailUrl(cdnDecrypt.generateSignedPathWithVersion(thumbnailUrl));
            }
        }
    }

    /**
     * 处理视频列表
     */
    private List<Video> processVideoList(List<Video> videos, String lang) {
        return videos.stream()
                .map(video -> {
                    processVideoUrls(video);
                    if (StringUtils.hasText(lang)) {
                        return VideoDataPackageUtil.packageVideoDataFromFields(video, lang);
                    }
                    return video;
                })
                .collect(Collectors.toList());
    }

    /**
     * 更新视频点赞率
     */
    private void updateLikeRate(String videoId) {
        try {
            Video video = mongoTemplate.findById(videoId, Video.class, COLLECTION_NAME);
            if (video != null) {
                int likes = video.getLikes() != null ? video.getLikes() : 0;
                int dislikes = video.getDislikes() != null ? video.getDislikes() : 0;
                int total = likes + dislikes;
                
                int likeRate = 0;
                if (total > 0) {
                    likeRate = (int) ((likes * 100.0) / total);
                }

                Query query = new Query(Criteria.where("_id").is(videoId));
                Update update = new Update().set("likeRate", likeRate);
                mongoTemplate.updateFirst(query, update, Video.class, COLLECTION_NAME);
            }
        } catch (Exception e) {
            log.error("❌ 更新点赞率失败, videoId: {}, 错误: {}", videoId, e.getMessage(), e);
        }
    }
}
