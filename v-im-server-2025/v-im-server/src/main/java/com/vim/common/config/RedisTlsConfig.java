package com.vim.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SslOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.time.Duration;

@Profile("prod")
@EnableCaching
@Configuration
public class RedisTlsConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;
    @Value("${spring.data.redis.username}")
    private String redisUserName;
    @Value("${spring.data.redis.password}")
    private String redisPassword;
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

    @Bean
    @Profile("prod")
    public RedisConnectionFactory redisConnectionFactory() throws Exception {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setPassword(redisPassword);
        config.setUsername(redisUserName);
        config.setDatabase(database);

        // 2. 构建SSL选项 - 禁用端点识别
        SslOptions sslOptions = SslOptions.builder()
                .jdkSslProvider()
                .trustManager(loadTrustStore())
                .keyManager(loadCertFile(), loadKeyFile(), clientKeyPassword.toCharArray())
                .build();

        // 3. 配置ClientOptions
        ClientOptions clientOptions = ClientOptions.builder()
                .sslOptions(sslOptions)
                .protocolVersion(ProtocolVersion.RESP3)
                .build();

        // 4. 构建Lettuce客户端配置 - 禁用对等验证
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientOptions(clientOptions)
                .useSsl()
                .disablePeerVerification()  // 关键：禁用对等验证
                .build();

        return new LettuceConnectionFactory(config, clientConfig);
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

    @Bean
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        // 使用 StringRedisSerializer 来序列化键
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        // 使用 Jackson2JsonRedisSerializer 来序列化值
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper());
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        template.afterPropertiesSet();
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