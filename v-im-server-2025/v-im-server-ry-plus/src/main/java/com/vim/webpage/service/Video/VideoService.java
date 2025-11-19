package com.vim.webpage.service.Video;

import com.vim.webpage.domain.Video;

import java.util.List;
import java.util.Optional;

/**
 * 视频业务逻辑接口
 */
public interface VideoService {
    
    /**
     * 创建新视频
     */
    Video createVideo(Video video);
    
    /**
     * 根据 ID 获取视频
     */
    Optional<Video> getVideoById(String id);
    
    /**
     * 根据 ID 获取视频（带语言参数）
     */
    Optional<Video> getVideoById(String id, String lang);
    
    /**
     * 根据用户 ID 获取视频列表
     */
    List<Video> getVideosByUserId(String userId, int page, int size);
    
    /**
     * 根据用户 ID 获取视频列表（带语言参数）
     */
    List<Video> getVideosByUserId(String userId, int page, int size, String lang);
    
    /**
     * 批量获取视频
     */
    List<Video> getVideos(List<String> videoIds, String lang);
    
    /**
     * 获取随机视频
     */
    List<Video> getRandomVideos(int count, String lang);
    
    /**
     * 根据分类获取视频
     */
    List<Video> getVideosByCategory(String category, int page, int size, String lang);
    
    /**
     * 根据标签获取视频
     */
    List<Video> getVideosByTag(String tag, int page, int size, String lang);
    
    /**
     * 搜索视频（根据描述或作者）
     */
    List<Video> searchVideos(String keyword, int page, int size, String lang);
    
    /**
     * 获取热门视频（按浏览量排序）
     */
    List<Video> getPopularVideos(int page, int size, String lang);
    
    /**
     * 获取最新视频（按日期排序）
     */
    List<Video> getLatestVideos(int page, int size, String lang);
    
    /**
     * 更新视频信息
     */
    Video updateVideo(String id, Video video);
    
    /**
     * 删除视频
     */
    void deleteVideo(String id);
    
    /**
     * 增加视频浏览量
     */
    void incrementViews(String videoId);
    
    /**
     * 点赞视频
     */
    void likeVideo(String videoId);
    
    /**
     * 取消点赞视频
     */
    void unlikeVideo(String videoId);
    
    /**
     * 踩视频
     */
    void dislikeVideo(String videoId);
    
    /**
     * 取消踩视频
     */
    void undislikeVideo(String videoId);
    
    /**
     * 收藏视频
     */
    void saveVideo(String videoId);
    
    /**
     * 取消收藏视频
     */
    void unsaveVideo(String videoId);
    
    /**
     * 增加评论数
     */
    void incrementComments(String videoId);
    
    /**
     * 减少评论数
     */
    void decrementComments(String videoId);
    
    /**
     * 获取视频总数
     */
    long getTotalCount();
    
    /**
     * 获取用户的视频总数
     */
    long getUserVideoCount(String userId);
}
