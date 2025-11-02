package com.vim.modules.group.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vim.common.domain.BaseEntity;
import com.vim.common.enums.SysDelEnum;
import com.vim.common.utils.VimUtil;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 群关系对象 im_group_user
 *
 * @author 乐天
 * @since 2022-01-25
 */
@Data
@TableName(value = "im_group_user")
public class ImGroupUser extends BaseEntity implements Serializable {

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @TableId
    private String id;
    /**
     * 群id
     */
    private String groupId;

    /**
     * 用户id
     */
    private String userId;

    /**
     * 审核状态
     */
    private String state;

    /**
     * 申请信息
     */
    private String message;

    /**
     *
     */
    @TableLogic(value = "0", delval = "1")
    private String delFlag;


    public void preInsert() {
        this.setDelFlag(SysDelEnum.DEL_NO.getCode());
        this.setCreateBy(VimUtil.getLoginId());
        this.setCreateTime(new Date());
    }

}
