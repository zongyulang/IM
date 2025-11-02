package com.vim.modules.chat.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 消息免打扰
 *
 * @author 乐天
 * @since 2022-01-25
 */
@Data
@TableName(value = "im_message_immunity")
public class ImMessageImmunity implements Serializable {

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    /**
     * 用户
     */
    private String userId;

    /**
     * 聊天对象
     */
    private String chatId;

}
