package com.vim.sdk.controller;

import cn.dev33.satoken.util.SaResult;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vim.common.annotation.Log;
import com.vim.common.enums.BusinessType;
import com.vim.common.enums.ChatTypeEnum;
import com.vim.common.enums.MessageTypeEnum;
import com.vim.common.utils.ChatUtils;
import com.vim.common.utils.VimUtil;
import com.vim.sdk.service.VimGroupApiService;
import com.vim.sdk.service.VimMessageService;
import com.vim.tio.messages.Message;
import com.vim.tio.result.MessagePage;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * 消息推送接口服务
 *
 */
@RestController
@RequestMapping("/vim/sdk/message")
public class MessageController {

    @Resource
    private VimMessageService vimMessageService;

    @Resource
    private VimGroupApiService vimGroupApiService;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 查询聊天记录
     *
     * @param chatId   聊天id
     * @param chatType 聊天类型
     * @param pageSize 每页多少条
     * @return List
     */
    @GetMapping
    @Log(title = "查询聊天记录")
    public SaResult list(String chatId, String chatType, Long pageSize) {
        String userId = VimUtil.getLoginId();
        if (chatType.equals(ChatTypeEnum.GROUP.getCode())) {
            boolean is = vimGroupApiService.isMember(chatId, userId);
            if (!is) {
                return SaResult.error("您不是该群成员");
            }
        }
        return SaResult.data(vimMessageService.list(chatId, userId, chatType, pageSize));
    }

    /**
     * 查询单个
     *
     * @param id id
     * @return SaResult
     */
    @GetMapping(value = "/{id}")
    @Log(title = "查询单条消息")
    public SaResult getById(@PathVariable String id, String chatKey) throws Exception {
        return SaResult.data(vimMessageService.get(id, chatKey));
    }


    /**
     * 分页查询
     *
     * @param page 分页条件
     * @return page
     */
    @GetMapping(value = "page")
    @Log(title = "分页查询消息")
    public SaResult page(MessagePage messagePage, Page<Message> page) {
        String userId = VimUtil.getLoginId();
        if (messagePage.getChatType().equals(ChatTypeEnum.GROUP.getCode())) {
            boolean is = vimGroupApiService.isMember(messagePage.getChatId(), userId);
            if (!is) {
                return SaResult.error("您不是该群成员");
            }
        }
        return SaResult.data(vimMessageService.page(messagePage.getChatId(), userId, messagePage.getMessageType(), messagePage.getChatType(), messagePage.getSearchText(), messagePage.getDateRange(), page));
    }



    /**
     * 获取chatId最后一个读取的时间
     *
     * @param chatId 接收人或者群id
     * @param fromId 文件路径
     * @return SaResult
     */
    @GetMapping(value = "getReadTime")
    @Log(title = "获取消息读取时间")
    public SaResult getReadTime(String chatId, String fromId) {
        String key = ChatUtils.getReadKey(fromId, chatId);
        return SaResult.data(redisTemplate.opsForValue().get(key));
    }
    /**
     * 推送消息
     * At@功能,这里只有提醒，没有在内容上显示@信息，需要发送人自己拼接
     *
     * @param chatId    接收人或者群id
     * @param content   消息内容
     * @param chatType  消息类型
     * @param atAll     是否@所有人
     * @param atUserIds @的用户id,多个用逗号隔开
     * @return SaResult
     */
    @PostMapping(value = "pushText")
    @Log(title = "推送文本消息", businessType = BusinessType.INSERT)
    public SaResult pushText(String chatId, String content, String chatType, boolean atAll, String atUserIds) {
        try {
            Message message = new Message();
            message.setMessageType(MessageTypeEnum.TEXT.getCode());
            message.setChatId(chatId);
            message.setContent(content);
            message.setFromId(VimUtil.getLoginId());
            message.setTimestamp(System.currentTimeMillis());
            message.setChatType(chatType);
            if (StrUtil.isBlank(message.getId())) {
                message.setId(IdUtil.getSnowflakeNextIdStr());
            }
            atExtend(atAll, atUserIds, message);
            vimMessageService.push(message);
        } catch (Exception e) {
            return SaResult.error(e.getMessage());
        }
        return SaResult.ok();
    }

    /**
     * At@功能,这里只有提醒，没有在内容上显示@信息，需要发送人自己拼接
     *
     * @param atAll     是否@所有人
     * @param atUserIds @的用户id,多个用逗号隔开
     * @param message   消息类型
     */
    private void atExtend(boolean atAll, String atUserIds, Message message) {
        JSONObject extend = new JSONObject();
        if (atAll) {
            extend.set("atAll", true);
        }
        if (StrUtil.isNotBlank(atUserIds)) {
            extend.set("atUserIds", StrUtil.split(atUserIds, ","));
        }
        message.setExtend(extend);
    }

    /**
     * 推送图片消息
     *
     * @param chatId    接收人或者群id
     * @param url       图片路径
     * @param chatType  消息类型
     * @param atAll     是否@所有人
     * @param atUserIds @的用户id,多个用逗号隔开
     * @return SaResult
     */
    @PostMapping(value = "pushImage")
    @Log(title = "推送图片消息", businessType = BusinessType.INSERT)
    public SaResult pushImage(String chatId, String url, String chatType, boolean atAll, String atUserIds) {
        try {
            Message message = new Message();
            message.setMessageType(MessageTypeEnum.IMAGE.getCode());
            message.setChatId(chatId);
            JSONObject object = new JSONObject();
            object.set("url", url);
            atExtend(atAll, atUserIds, message);
            initAndPush(chatType, message, object);

        } catch (Exception e) {
            return SaResult.error(e.getMessage());
        }
        return SaResult.ok();
    }

    /**
     * 推送附件消息
     *
     * @param chatId    接收人或者群id
     * @param url       文件路径
     * @param fileName  文件名称
     * @param chatType  消息类型
     * @param atAll     是否@所有人
     * @param atUserIds @的用户id,多个用逗号隔开
     * @return SaResult
     */
    @PostMapping(value = "pushFile")
    @Log(title = "推送文件消息", businessType = BusinessType.INSERT)
    public SaResult pushFile(String chatId, String url, String fileName, String chatType, boolean atAll, String atUserIds) {
        try {
            Message message = new Message();
            message.setMessageType(MessageTypeEnum.FILE.getCode());
            message.setChatId(chatId);
            JSONObject object = new JSONObject();
            object.set("url", url);
            object.set("fileName", fileName);
            atExtend(atAll, atUserIds, message);
            initAndPush(chatType, message, object);
        } catch (Exception e) {
            return SaResult.error(e.getMessage());
        }
        return SaResult.ok();
    }



    private void initAndPush(String chatType, Message message, JSONObject object) throws Exception {
        message.setExtend(object);
        message.setFromId(VimUtil.getLoginId());
        message.setTimestamp(System.currentTimeMillis());
        message.setChatType(chatType);
        if (StrUtil.isBlank(message.getId())) {
            message.setId(IdUtil.getSnowflakeNextIdStr());
        }
        vimMessageService.push(message);
    }
}
