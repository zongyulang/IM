package com.vim.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.SslOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.File;
import java.time.Duration;

/**
 * redis配置
 *
 * @author 乐天
 */
@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:3}")
    private int defaultDatabase;

    @Value("${spring.data.redis.username:}")
    private String redisUsername;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${spring.data.redis.ssl.ca-certificate-path:}")
    private String caCertificatePath;

    @Value("${spring.data.redis.ssl.client-certificate-path:}")
    private String clientCertificatePath;

    @Value("${spring.data.redis.ssl.client-key-path:}")
    private String clientKeyPath;

    @Value("${spring.data.redis.ssl.client-key-password:}")
    private String clientKeyPassword;

    @Autowired
    private ResourceLoader resourceLoader;

    /**
     * 默认 RedisConnectionFactory - DB3 (用于系统业务)
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return createRedisConnectionFactory(defaultDatabase);
    }

    /**
     * Webpage业务专用 RedisConnectionFactory - DB0
     */
    @Bean(name = "webpageRedisConnectionFactory")
    public RedisConnectionFactory webpageRedisConnectionFactory() {
        return createRedisConnectionFactory(0);
    }

    /**
     * 创建 RedisConnectionFactory 的通用方法
     * 
     * @param database 数据库索引
     * @return RedisConnectionFactory
     */
    private RedisConnectionFactory createRedisConnectionFactory(int database) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);

        if (redisUsername != null && !redisUsername.isEmpty()) {
            config.setUsername(redisUsername);
        }

        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }

        config.setDatabase(database);

        // 如果启用了 SSL，配置 SSL 选项
        if (sslEnabled) {
            try {
                SslOptions.Builder sslOptionsBuilder = SslOptions.builder()
                        .jdkSslProvider();

                // 如果提供了 CA 证书路径，加载 CA 证书
                if (caCertificatePath != null && !caCertificatePath.isEmpty()) {
                    Resource caResource = resourceLoader.getResource(caCertificatePath);
                    File caCertFile = caResource.getFile();
                    sslOptionsBuilder.trustManager(caCertFile);
                }

                // 如果提供了客户端证书和密钥，使用 Netty 的 SslContextBuilder
                if (clientCertificatePath != null && !clientCertificatePath.isEmpty()
                        && clientKeyPath != null && !clientKeyPath.isEmpty()) {

                    Resource clientCertResource = resourceLoader.getResource(clientCertificatePath);
                    Resource clientKeyResource = resourceLoader.getResource(clientKeyPath);

                    File clientCertFile = clientCertResource.getFile();
                    File clientKeyFile = clientKeyResource.getFile();
                    File caCertFile = null;

                    if (caCertificatePath != null && !caCertificatePath.isEmpty()) {
                        Resource caResource = resourceLoader.getResource(caCertificatePath);
                        caCertFile = caResource.getFile();
                    }

                    // 使用 Netty 的 SslContextBuilder 配置客户端证书
                    final File finalCaCertFile = caCertFile;
                    sslOptionsBuilder = SslOptions.builder()
                            .sslContext(sslContextBuilder -> {
                                sslContextBuilder
                                    .keyManager(clientCertFile, clientKeyFile,
                                        (clientKeyPassword != null && !clientKeyPassword.isEmpty())
                                                ? clientKeyPassword
                                                : null);

                                // 添加信任管理器（CA 证书）
                                if (finalCaCertFile != null) {
                                    sslContextBuilder.trustManager(finalCaCertFile);
                                }
                            });
                }

                SslOptions sslOptions = sslOptionsBuilder.build();

                LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                        .useSsl()
                        .and()
                        .clientOptions(io.lettuce.core.ClientOptions.builder()
                                .sslOptions(sslOptions)
                                .build())
                        .commandTimeout(Duration.ofSeconds(10))
                        .build();

                LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig);
                factory.afterPropertiesSet();
                return factory;
            } catch (Exception e) {
                throw new RuntimeException("Failed to configure Redis TLS connection: " + e.getMessage(), e);
            }
        }

        return new LettuceConnectionFactory(config);
    }

    /**
     * 默认 RedisTemplate - 使用DB3
     */
    @Bean
    @Primary
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        // 使用 StringRedisSerializer 来序列化键
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        // 使用 Jackson2JsonRedisSerializer 来序列化值
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(
                Object.class);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper());
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Webpage业务专用 RedisTemplate - 使用DB0
     */
    @Bean(name = "webpageRedisTemplate")
    public RedisTemplate<Object, Object> webpageRedisTemplate() {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(webpageRedisConnectionFactory());
        // 使用 StringRedisSerializer 来序列化键
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        // 使用 Jackson2JsonRedisSerializer 来序列化值
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(
                Object.class);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper());
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Webpage业务专用 StringRedisTemplate - 使用DB0
     */
    @Bean(name = "webpageStringRedisTemplate")
    public StringRedisTemplate webpageStringRedisTemplate() {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(webpageRedisConnectionFactory());
        return template;
    }

    @Bean
    public DefaultRedisScript<Long> limitScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(limitScriptText());
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    /**
     * 设置缓存默认过期时间
     *
     * @param redisConnectionFactory method
     * @return RedisScript
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // 设置缓存的默认过期时间 5分钟
                .entryTtl(Duration.ofMinutes(5));
        return RedisCacheManager.builder(redisConnectionFactory).cacheDefaults(config).build();
    }

    /**
     * 配置ObjectMapper
     *
     * @return ObjectMapper实例
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 在这里添加其他需要的配置
        return objectMapper;
    }

    /**
     * 限流脚本
     */
    private String limitScriptText() {
        return "local key = KEYS[1]\n" +
                "local count = tonumber(ARGV[1])\n" +
                "local time = tonumber(ARGV[2])\n" +
                "local current = redis.call('get', key);\n" +
                "if current and tonumber(current) > count then\n" +
                "    return tonumber(current);\n" +
                "end\n" +
                "current = redis.call('incr', key)\n" +
                "if tonumber(current) == 1 then\n" +
                "    redis.call('expire', key, time)\n" +
                "end\n" +
                "return tonumber(current);";
    }
}
