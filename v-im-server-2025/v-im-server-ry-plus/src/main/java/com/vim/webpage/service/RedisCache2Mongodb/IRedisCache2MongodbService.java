package com.vim.webpage.service.RedisCache2Mongodb;

/**
 * Redis缓存同步到MongoDB服务接口
 * 负责将Redis中的视频播放量数据定期同步到MongoDB
 * 
 * @author fres
 */
public interface IRedisCache2MongodbService {

    /**
     * 执行Redis到MongoDB的数据同步
     * 将当前月份的视频播放量排行榜从Redis同步到MongoDB
     * 
     * @return 是否同步成功
     */
    boolean syncMonthlyVideoRanking();

    /**
     * 增加视频的月度播放量（在Redis中）
     * 
     * @param videoId 视频ID
     * @param incrementBy 增加的播放量，默认为1
     * @return 增加后的总播放量
     */
    long incrementMonthlyVideoViews(String videoId, int incrementBy);

    /**
     * 获取视频在当前月份的播放量（从Redis）
     * 
     * @param videoId 视频ID
     * @return 播放量，如果不存在返回0
     */
    long getMonthlyVideoViews(String videoId);

    /**
     * 手动触发同步任务（用于测试或手动触发）
     * 
     * @return 同步结果信息
     */
    String manualSync();


}
