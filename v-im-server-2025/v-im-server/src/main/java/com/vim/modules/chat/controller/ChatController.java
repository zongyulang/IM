package com.vim.modules.chat.controller;

import cn.dev33.satoken.util.SaResult;
import com.vim.common.annotation.Log;
import com.vim.common.enums.BusinessType;
import com.vim.modules.chat.result.Chat;
import com.vim.modules.chat.service.IChatService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天控制器
 *
 * @author vim
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/vim/server/chat")
public class ChatController {

    @Resource
    private IChatService chatService;

    /**
     * 添加聊天
     *
     * @param chat 聊天对象
     * @return 操作结果
     * @throws Exception 异常信息
     */
    @PostMapping()
    @Log(title = "添加聊天", businessType = BusinessType.INSERT)
    public SaResult add(@RequestBody Chat chat) throws Exception {
        chatService.add(chat);
        return SaResult.ok();
    }

    /**
     * 更新聊天
     *
     * @param chat 聊天对象
     * @return 操作结果
     * @throws Exception 异常信息
     */
    @PutMapping()
    public SaResult update(@RequestBody Chat chat) throws Exception {
        chatService.update(chat);
        return SaResult.ok();
    }

    /**
     * 批量更新聊天
     *
     * @param chatList 聊天列表
     * @return 操作结果
     */
    @PutMapping("batch")
    public SaResult batch(@RequestBody List<Chat> chatList) {
        chatService.batchUpdate(chatList);
        return SaResult.ok();
    }

    /**
     * 移动聊天位置
     *
     * @param chatId 聊天ID
     * @return 操作结果
     */
    @GetMapping("move")
    @Log(title = "移动聊天位置", businessType = BusinessType.UPDATE)
    public SaResult move(String chatId) {
        chatService.move(chatId);
        return SaResult.ok();
    }

    /**
     * 获取置顶聊天列表
     *
     * @return 置顶聊天列表
     */
    @GetMapping("/topList")
    public SaResult topList() {
        return SaResult.data(chatService.getTopList());
    }

    /**
     * 获取普通聊天列表
     *
     * @return 聊天列表
     */
    @GetMapping("/list")
    public SaResult list() {
        return SaResult.data(chatService.getList());
    }

    /**
     * 删除聊天
     *
     * @param chatId 聊天ID
     * @return 操作结果
     */
    @DeleteMapping("/{chatId}")
    @Log(title = "删除聊天", businessType = BusinessType.DELETE)
    public SaResult delete(@PathVariable String chatId) {
        chatService.delete(chatId);
        return SaResult.ok();
    }

    /**
     * 置顶聊天
     *
     * @param chatId 聊天ID
     * @return 操作结果
     */
    @GetMapping("/top")
    @Log(title = "置顶聊天", businessType = BusinessType.UPDATE)
    public SaResult top(String chatId) {
        chatService.top(chatId);
        return SaResult.ok();
    }

    /**
     * 取消置顶聊天
     *
     * @param chatId 聊天ID
     * @return 操作结果
     */
    @GetMapping("/cancelTop")
    @Log(title = "取消置顶聊天", businessType = BusinessType.UPDATE)
    public SaResult cancelTop(String chatId) {
        chatService.cancelTop(chatId);
        return SaResult.ok();
    }
}
