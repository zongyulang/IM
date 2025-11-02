package com.vim.modules.setting.result;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 聊天设置
 *
 * @author z
 */
@Data
public class Setting implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public Setting() {
    }

    /**
     * 允许加好友
     */
    private String canAddFriend;

    /**
     * 好友审核
     */
    private String addFriendValidate;

    /**
     * 允许私聊
     */
    private String canSendMessage;

    /**
     * 语音消息提醒
     */
    private String canSoundRemind;

    /**
     * 通话消息提醒
     */
    private String canVoiceRemind;

    /**
     * 展示手机号
     */
    private String showMobile;

    /**
     * 展示邮箱
     */
    private String showEmail;

    /**
     * userId
     */
    private String userId;


}
