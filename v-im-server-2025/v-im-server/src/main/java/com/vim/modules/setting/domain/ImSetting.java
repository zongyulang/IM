package com.vim.modules.setting.domain;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vim.common.domain.BaseEntity;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * V-IM设置
 *
 * @author 乐天
 */
@Data
@TableName(value = "im_setting")
public class ImSetting extends BaseEntity implements Serializable {

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

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
     * 当用户表来自别的项目，头像不能通用时候，使用该头像
     */
    private String avatar;


    @TableLogic(value = "0", delval = "1")
    private String delFlag;


}