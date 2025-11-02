package com.vim.tio.service.impl;

import cn.hutool.core.util.StrUtil;
import com.vim.tio.domain.MessageLog;
import com.vim.tio.service.MessageLogService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MessageLogServiceImpl implements MessageLogService {

    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * 消息日志处理线程池
     */
    private ThreadPoolExecutor logExecutor;

    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 4;
    private static final int QUEUE_CAPACITY = 1000;
    private static final long KEEP_ALIVE_TIME = 300L;

    /**
     * 初始化线程池
     */
    @PostConstruct
    public void init() {
        logExecutor = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(QUEUE_CAPACITY),
            new ThreadPoolExecutor.CallerRunsPolicy() // 队列满时，由调用线程执行
        );
    }

    /**
     * 异步记录消息日志
     *
     * @param text   消息内容
     * @param userid 发送者ID
     */
    @Override
    public void logMessage(String text, String userid) {
        logExecutor.execute(() -> {
            try {
                String collectionName = StrUtil.format("message-log-{}",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

                MessageLog messageLog = MessageLog.builder()
                    .content(text)
                    .senderId(userid)
                    .sendTime(System.currentTimeMillis())
                    .build();

                mongoTemplate.save(messageLog, collectionName);

            } catch (Exception e) {
                log.error("异步记录消息日志失败", e);
            }
        });
    }

    /**
     * 优雅关闭线程池
     */
    @PreDestroy
    public void shutdown() {
        if (logExecutor != null) {
            try {
                // 停止接收新任务
                logExecutor.shutdown();
                // 等待当前任务完成
                if (!logExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    logExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.error("关闭消息日志线程池失败", e);
                Thread.currentThread().interrupt();
            }
        }
    }

}
