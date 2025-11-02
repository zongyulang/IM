package com.vim.modules.friend.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vim.common.domain.BaseEntity;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 好友对象 im_friend
 *
 * @author 乐天
 * @since 2022-02-03
 */
@Data
@TableName(value = "im_friend")
public class ImFriend extends BaseEntity implements Serializable {

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    /**
     * 用户id
     */
    private String userId;

    /**
     * 用户id
     */
    private String friendId;

    /**
     * 状态
     */
    private String state;

    /**
     * 申请信息
     */
    private String message;

    /**
     * $column.columnComment
     */
    @TableLogic(value = "0", delval = "1")
    private String delFlag;

}
