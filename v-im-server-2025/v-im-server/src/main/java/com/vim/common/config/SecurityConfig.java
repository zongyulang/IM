package com.vim.common.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.vim.common.config.properties.SecurityProperties;
import com.vim.modules.upload.config.UploadConfig;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 安全配置类
 * 用于配置系统的安全拦截器和静态资源访问
 *
 * @author your-name
 */
@Configuration
public class SecurityConfig implements WebMvcConfigurer {

    /**
     * 安全属性配置，包含需要排除的路径等安全相关配置
     */
    @Resource
    private SecurityProperties securityProperties;

    @Resource
    private UploadConfig uploadConfig;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
                    // 如果是OPTIONS请求，直接放行
                    String method = SaHolder.getRequest().getMethod();
                    if ("OPTIONS".equals(method)) {
                        return;
                    }
                    // 其他请求进行登录校验
                    StpUtil.checkLogin();
                }))
                .addPathPatterns("/**")
                .excludePathPatterns(securityProperties.getExcludes());
    }

    /**
     * 配置静态资源的访问映射
     * 将URL路径映射到服务器的实际文件路径
     *
     * @param registry 资源处理器注册表
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置上传文件的访问路径
        // 将 /profile/** 的请求映射到实际的文件上传目录
        registry.addResourceHandler("/profile/**")
                .addResourceLocations("file:" + uploadConfig.getLocal().getUploadPath());
        
        // 配置网站图标的访问路径
        // 将 /favicon.ico 请求映射到类路径下的静态资源
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/favicon.ico");    
    }

    /**
     * 配置跨域访问
     *
     * @param registry 资源处理器注册表
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 允许所有路径
                .allowedOriginPatterns("*") // 允许的源
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") // 允许的 HTTP 方法
                .allowedHeaders("*") // 允许自定义头部
                .allowCredentials(true) // 允许携带凭证（如 cookies）
                .maxAge(3600); // 预检请求的缓存时间
    }
}