package com.vim.webpage.config;

import com.vim.webpage.listener.RedisMessageListener;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SslOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.util.ResourceUtils;

import java.io.File;

/**
 * Redis 订阅客户端配置
 * 用于配置独立的 Redis 订阅连接，使用单独的密码
 *
 * @author vim
 */
@Slf4j
@Configuration
public class RedisSubscriberConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    // 订阅专用用户名，未配置时回退到常规 redis 用户名
    @Value("${spring.data.redis.redisSubscriberusername:${spring.data.redis.username}}")
    private String redisUserName;

    // 订阅专用密码，按顺序尝试：redisSubscriberPassword -> subscribe-password -> 默认值
    @Value("${spring.data.redis.redisSubscriberPassword:${spring.data.redis.subscribe-password:ypwsCYF95733714!Test%*SubUserYaKnowHaHa}}")
    private String redisSubscribePassword;

    @Value("${spring.data.redis.database:3}")
    private Integer database;

    @Value("${spring.data.redis.ssl.ca-certificate-path}")
    private String caCertPath;

    @Value("${spring.data.redis.ssl.client-certificate-path}")
    private String clientCertPath;

    @Value("${spring.data.redis.ssl.client-key-path}")
    private String clientKeyPath;

    @Value("${spring.data.redis.ssl.client-key-password:}")
    private String clientKeyPassword;

    @Autowired(required = false)
    private RedisMessageListener redisMessageListener;

    // 是否启用订阅容器
    @Value("${spring.data.redis.subscriber.enabled:true}")
    private boolean subscriberEnabled;

    // 订阅的频道模式
    @Value("${spring.data.redis.subscriber.pattern:im:*}")
    private String subscriberPattern;

    /**
     * 创建用于订阅的 Redis 连接工厂
     * 使用独立的订阅密码
     */
    @Bean(name = "redisSubscriberConnectionFactory")
    public LettuceConnectionFactory redisSubscriberConnectionFactory() throws Exception {
        log.info("初始化 Redis 订阅客户端连接，使用独立的订阅密码");
        
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setPassword(redisSubscribePassword);  // 使用订阅专用密码
        config.setUsername(redisUserName);
        config.setDatabase(database);

        // 构建SSL选项 - 禁用端点识别
        SslOptions sslOptions = SslOptions.builder()
                .jdkSslProvider()
                .trustManager(loadTrustStore())
                .keyManager(loadCertFile(), loadKeyFile(), clientKeyPassword.toCharArray())
                .build();

        // 配置ClientOptions
        ClientOptions clientOptions = ClientOptions.builder()
                .sslOptions(sslOptions)
                .protocolVersion(ProtocolVersion.RESP3)
                .build();

        // 构建Lettuce客户端配置 - 禁用对等验证
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientOptions(clientOptions)
                .useSsl()
                .disablePeerVerification()  // 关键：禁用对等验证
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig);
        factory.afterPropertiesSet();
        
        log.info("Redis 订阅客户端连接初始化完成");
        return factory;
    }

    /**
     * Redis 消息监听容器
     * 用于管理所有的消息监听器
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
        LettuceConnectionFactory redisSubscriberConnectionFactory) {
        
        log.info("初始化 Redis 消息监听容器");
        
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisSubscriberConnectionFactory);
        // 订阅错误统一处理，避免刷屏并给出 ACL 提示
        container.setErrorHandler(throwable -> {
            String msg = throwable.getMessage();
            if (msg != null && msg.contains("NOPERM") && msg.contains("channel")) {
                log.error("Redis 订阅权限不足: {}\n请为用户 [{}] 在 Redis ACL 中授权频道访问: {}\n示例: ACL SETUSER {} on ><password> +@pubsub resetchannels channel ~{}",
                        msg, redisUserName, subscriberPattern, redisUserName, subscriberPattern);
            } else {
                log.error("Redis 订阅异常", throwable);
            }
        });
        
        if (!subscriberEnabled) {
            log.warn("Redis 订阅容器已禁用: spring.data.redis.subscriber.enabled=false");
            return container;
        }

        // 添加消息监听器 - 监听配置的频道模式
        if (redisMessageListener != null) {
            container.addMessageListener(redisMessageListener, new PatternTopic(subscriberPattern));
            log.info("已添加消息监听器，监听频道模式: {}", subscriberPattern);
        }
        
        // 您可以添加更多的监听器和频道
        // container.addMessageListener(anotherListener, new ChannelTopic("specific-channel"));
        // container.addMessageListener(yetAnotherListener, new PatternTopic("pattern:*"));
        
        log.info("Redis 消息监听容器初始化完成");
        return container;
    }

    /**
     * 加载CA证书文件
     */
    private File loadTrustStore() throws Exception {
        return ResourceUtils.getFile(caCertPath);
    }

    /**
     * 加载客户端证书文件
     */
    private File loadCertFile() throws Exception {
        return ResourceUtils.getFile(clientCertPath);
    }

    /**
     * 加载客户端私钥文件
     */
    private File loadKeyFile() throws Exception {
        return ResourceUtils.getFile(clientKeyPath);
    }
}
