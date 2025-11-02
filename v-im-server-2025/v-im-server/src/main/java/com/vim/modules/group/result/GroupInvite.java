package com.vim.modules.group.result;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 群邀请
 *
 * @Author zkp
 */
@Data
public class GroupInvite implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
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
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;


    public GroupInvite() {
    }

    public GroupInvite(String id, String groupId, String fromId, String userId, String checkUserId, String checkMessage, String waitCheck, String checkResult) {
        this.id = id;
        this.groupId = groupId;
        this.fromId = fromId;
        this.userId = userId;
        this.checkUserId = checkUserId;
        this.checkMessage = checkMessage;
        this.waitCheck = waitCheck;
        this.checkResult = checkResult;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getFromId() {
        return fromId;
    }

    public void setFromId(String fromId) {
        this.fromId = fromId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCheckUserId() {
        return checkUserId;
    }

    public void setCheckUserId(String checkUserId) {
        this.checkUserId = checkUserId;
    }

    public String getCheckMessage() {
        return checkMessage;
    }

    public void setCheckMessage(String checkMessage) {
        this.checkMessage = checkMessage;
    }

    public String getWaitCheck() {
        return waitCheck;
    }

    public void setWaitCheck(String waitCheck) {
        this.waitCheck = waitCheck;
    }

    public String getCheckResult() {
        return checkResult;
    }

    public void setCheckResult(String checkResult) {
        this.checkResult = checkResult;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
