package com.vim.modules.chat.controller;

import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vim.common.annotation.Log;
import com.vim.common.enums.BusinessType;
import com.vim.modules.chat.domain.ImMessageImmunity;
import com.vim.modules.chat.service.IImMessageImmunityService;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

/**
 * 消息免打扰Controller
 */
@RestController
@RequestMapping("/vim/server/immunity")
public class ImmunityController {

    private static final String CACHE_KEY = "messages:immunity";

    @Resource
    private IImMessageImmunityService imMessageImmunityService;

    /**
     * 获取免打扰列表
     *
     * @param userId 用户id
     * @return SaResult
     */
    @GetMapping(value = "/{userId}")
    @Cacheable(value = CACHE_KEY, key = "#userId")
    @Log(title = "查询消息免打扰列表")
    public SaResult get(@PathVariable String userId) {
        LambdaQueryWrapper<ImMessageImmunity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImMessageImmunity::getUserId, userId);
        return SaResult.data(imMessageImmunityService.list(queryWrapper));
    }

    /**
     * 删除免打扰
     *
     * @param userId 用户id
     * @param chatId 聊天室id
     * @return SaResult
     */
    @DeleteMapping(value = "/{userId}-{chatId}")
    @CacheEvict(value = CACHE_KEY, key = "#userId")
    @Log(title = "删除消息免打扰", businessType = BusinessType.DELETE)
    public SaResult delete(@PathVariable String userId, @PathVariable String chatId) {
        LambdaQueryWrapper<ImMessageImmunity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImMessageImmunity::getUserId, userId)
                .eq(ImMessageImmunity::getChatId, chatId);
        return SaResult.data(imMessageImmunityService.remove(queryWrapper));
    }

    /**
     * 保存免打扰
     *
     * @param messageImmunity 消息免打扰
     * @return SaResult
     */
    @PostMapping
    @CacheEvict(value = CACHE_KEY, key = "#messageImmunity.userId")
    @Log(title = "设置消息免打扰", businessType = BusinessType.INSERT)
    public SaResult save(@RequestBody ImMessageImmunity messageImmunity) {
        LambdaQueryWrapper<ImMessageImmunity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImMessageImmunity::getUserId, messageImmunity.getUserId())
                .eq(ImMessageImmunity::getChatId, messageImmunity.getChatId());
        if (imMessageImmunityService.count(queryWrapper) == 0) {
            imMessageImmunityService.save(messageImmunity);
        }
        return SaResult.ok();
    }
}
