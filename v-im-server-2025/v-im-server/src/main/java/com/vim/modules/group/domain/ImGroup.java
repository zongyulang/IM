package com.vim.modules.group.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vim.common.domain.BaseEntity;
import com.vim.common.utils.VimUtil;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

import static com.baomidou.mybatisplus.annotation.FieldFill.INSERT;

/**
 * 群管理对象 im_group
 *
 * @author 乐天
 * @since 2022-01-26
 */
@Data
@TableName(value = "im_group")
public class ImGroup extends BaseEntity implements Serializable {

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    /**
     * 群名称
     */
    private String name;

    /**
     * 群头像
     */
    @TableField(fill = INSERT)
    private String avatar;

    /**
     * 群主
     */
    private String master;

    /**
     * 开启邀请
     */
    private String openInvite;

    /**
     * 审核
     */
    private String inviteCheck;

    /**
     * 禁言
     */
    private String prohibition;

    /**
     * 不允许加好友
     */
    private String prohibitFriend;

    /**
     * 公告
     */
    @TableField(fill = INSERT)
    private String announcement;

    @TableLogic(value = "0", delval = "1")
    private String delFlag;


    public void preInsert() {
        this.setCreateBy(VimUtil.getLoginId());
        this.setCreateTime(new Date());
    }

    public void preUpdate() {
        this.setUpdateBy(VimUtil.getLoginId());
        this.setUpdateTime(new Date());
    }


}
