package com.vim.modules.login.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.google.code.kaptcha.Producer;
import com.vim.common.bridge.VimBridgeService;
import com.vim.common.config.CaptchaConfig;
import com.vim.modules.login.config.LoginConfig;
import com.vim.modules.login.param.LoginParam;
import com.vim.modules.login.param.RegisterParam;
import com.vim.modules.login.service.VimLoginService;
import com.vim.modules.sys.result.SysConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.FastByteArrayOutputStream;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 登录服务实现类
 * 处理用户登录、注册、登出以及密码管理相关的业务逻辑
 */
@Slf4j
@Service
public class VimLoginServiceImpl implements VimLoginService {

    private static final String LAST_PASSWORD_CHANGE_KEY = "user:pwd:last-change:";
    private static final String NEED_CHANGE_PASSWORD_KEY = "user:pwd:need-change:";

    @Resource
    private VimBridgeService vimBridgeService;

    @Resource
    private LoginConfig loginConfig;

    @Resource
    private SysConfig sysConfig;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource(name = "captchaProducerMath")
    private Producer captchaProducerMath;

    /**
     * 用户登录处理
     * @param loginParam 登录参数，包含用户名、密码、验证码等信息
     * @return 返回登录结果，包含token和是否需要修改密码的标识
     * @throws RuntimeException 当验证码错误、账号被锁定或其他登录异常时抛出
     */
    @Override
    public Map<String, Object> login(LoginParam loginParam) {
        // 定义登录失败计数的Redis键
        String failKey = "login:fail:" + loginParam.getUsername();

        try {
            // 验证码校验，错误则抛出异常
            if (!validateCaptcha(loginParam.getUuid(), loginParam.getCode())) {
                throw new RuntimeException("验证码错误或已过期");
            }

            // 检查账号是否被锁定（登录失败次数超过限制）
            Object failCountObj = redisTemplate.opsForValue().get(failKey);
            int failCount = failCountObj != null ? Integer.parseInt(failCountObj.toString()) : 0;
            if (failCount >= loginConfig.getMaxRetryCount()) {
                throw new RuntimeException(StrUtil.format("账号已被锁定，请{}分钟后再试", loginConfig.getLockTime()));
            }

            // 调用登录服务进行身份验证
            String token = vimBridgeService.login(loginParam);
            // 登录成功后清除失败记录
            redisTemplate.delete(failKey);

            Map<String, Object> map = new HashMap<>();
            map.put("token", token);

            // 获取登录用户ID，用于后续密码相关检查
            String userId = StpUtil.getLoginIdAsString();
            boolean needChange = false;

            // 首次登录修改密码检查（仅当系统配置开启时）
            if (sysConfig.isPasswordFirstLoginUpdate()) {
                needChange = redisTemplate.opsForValue().get(NEED_CHANGE_PASSWORD_KEY + userId) != null;
            }

            // 检查密码是否过期
            String lastChangeStr = redisTemplate.opsForValue().get(LAST_PASSWORD_CHANGE_KEY + userId);
            if (lastChangeStr == null) {
                // 首次登录，记录当前时间为密码修改时间
                redisTemplate.opsForValue().set(LAST_PASSWORD_CHANGE_KEY + userId, String.valueOf(System.currentTimeMillis()));
            } else {
                // 计算密码修改距今天数，超过配置天数则需要修改密码
                long lastChange = Long.parseLong(lastChangeStr);
                long now = System.currentTimeMillis();
                long daysDiff = (now - lastChange) / (1000 * 60 * 60 * 24);
                if (daysDiff >= sysConfig.getPasswordValidateDays()) {
                    needChange = true;
                }
            }

            map.put("needChangePassword", needChange);
            return map;
        } catch (RuntimeException e) {
            log.error(e.getMessage());
            // 登录失败，增加失败计数并设置锁定时间
            redisTemplate.opsForValue().increment(failKey, 1);
            redisTemplate.expire(failKey, loginConfig.getLockTime(), TimeUnit.MINUTES);
            throw e;
        }
    }

    /**
     * 用户登出处理
     * 清除用户登录状态并记录登出日志
     * @throws RuntimeException 当登出过程中发生异常时抛出
     */
    @Override
    public void logout() {
        StpUtil.logout();
    }

    /**
     * 用户注册处理
     * @param registerParam 注册参数，包含用户名、密码等注册信息
     * @return 返回注册成功的用户ID
     * @throws RuntimeException 当注册过程中发生异常时抛出
     */
    @Override
    public String register(RegisterParam registerParam) {

        String userId = vimBridgeService.register(registerParam);
        // 根据系统配置判断是否需要首次登录修改密码
        if (sysConfig.isPasswordFirstLoginUpdate()) {
            redisTemplate.opsForValue().set(NEED_CHANGE_PASSWORD_KEY + userId, "1");
        }
        return userId;
    }

    /**
     * 处理密码更新
     * 更新用户密码修改时间并清除密码修改标记
     * @param userId 需要处理密码更新的用户ID
     * @throws RuntimeException 当密码更新处理过程中发生异常时抛出
     */
    @Override
    public void handlePasswordUpdate(String userId) {
        // 更新最后修改密码时间
        redisTemplate.opsForValue().set(LAST_PASSWORD_CHANGE_KEY + userId, String.valueOf(System.currentTimeMillis()));
        // 删除需要修改密码的标记
        redisTemplate.delete(NEED_CHANGE_PASSWORD_KEY + userId);
    }

    /**
     * 验证图形验证码
     * @param uuid 验证码标识
     * @param code 用户输入的验证码
     * @return 验证结果：true-验证成功，false-验证失败
     */
    @Override
    public boolean validateCaptcha(String uuid, String code) {
        // 从Redis中获取存储的验证码
        String redisCode = redisTemplate.opsForValue().get(CaptchaConfig.CAPTCHA_KEY + uuid);
        // 验证码不存在或已过期
        if (redisCode == null) {
            return false;
        }
        // 验证完成后，立即删除Redis中的验证码，防止重复使用
        redisTemplate.delete(CaptchaConfig.CAPTCHA_KEY + uuid);
        // 比较用户输入的验证码与存储的验证码是否一致
        return code.equals(redisCode);
    }

    /**
     * 生成图形验证码
     * 生成数学运算类型的验证码，包含算式图片和结果
     * @return 返回包含验证码UUID和Base64编码的图片的Map
     * @throws RuntimeException 当验证码生成过程中发生异常时抛出
     */
    @Override
    public Map<String, String> generateCaptcha() {
        // 生成验证码
        String capText = captchaProducerMath.createText();
        String capStr = capText.substring(0, capText.lastIndexOf("@"));
        String code = capText.substring(capText.lastIndexOf("@") + 1);
        BufferedImage image = captchaProducerMath.createImage(capStr);

        // 生成uuid
        String uuid = UUID.randomUUID().toString();

        // 保存验证码到Redis，设置过期时间2分钟
        redisTemplate.opsForValue().set(CaptchaConfig.CAPTCHA_KEY + uuid, code, 2, TimeUnit.MINUTES);

        // 转换流信息写出
        FastByteArrayOutputStream os = new FastByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", os);
        } catch (IOException e) {
            throw new RuntimeException("验证码生成失败");
        }

        Map<String, String> result = new HashMap<>();
        result.put("uuid", uuid);
        result.put("img", Base64.getEncoder().encodeToString(os.toByteArray()));
        return result;
    }
}
