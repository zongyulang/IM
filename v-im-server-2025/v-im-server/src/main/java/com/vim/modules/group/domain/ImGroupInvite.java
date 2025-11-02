package com.vim.modules.group.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vim.common.domain.BaseEntity;
import com.vim.common.utils.VimUtil;
import com.vim.modules.group.result.GroupInvite;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 群邀请
 */
@Data
@TableName(value = "im_group_invite")
@EqualsAndHashCode(callSuper = true)
public class ImGroupInvite extends BaseEntity implements Serializable {

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
     * 邀请人
     */
    private String fromId;

    /**
     * 被邀请人
     */
    private String userId;

    /**
     * 审核人
     */
    private String checkUserId;

    /**
     * 审核意见
     */
    private String checkMessage;

    /**
     * 等待审核
     */
    private String waitCheck;

    /**
     * 审核结果
     */
    private String checkResult;


    /**
     * 0:正常 1:删除
     */
    @TableLogic(value = "0", delval = "1")
    private String delFlag;

    public void preInsert() {
        this.setCreateBy(VimUtil.getLoginId());
        this.setCreateTime(new Date());
    }

    public GroupInvite toGroupInvite() {
        GroupInvite groupInvite = new GroupInvite();
        //类型转换
        groupInvite.setId(this.id);
        groupInvite.setGroupId(this.groupId);
        groupInvite.setFromId(this.fromId);
        groupInvite.setUserId(this.userId);
        groupInvite.setCheckUserId(this.checkUserId);
        groupInvite.setCheckMessage(this.checkMessage);
        groupInvite.setWaitCheck(this.waitCheck);
        groupInvite.setCheckResult(this.checkResult);
        groupInvite.setCreateTime(this.getCreateTime());
        return groupInvite;
    }
}