package com.vim.modules.friend.result;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class Friend implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private String id;

    /**
     * 操作人
     */
    private String userId;

    /**
     * 好友
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
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Friend() {
    }

    public Friend(String id, String userId, String friendId, String state, String message, String createBy, Date createTime) {
        this.id = id;
        this.userId = userId;
        this.friendId = friendId;
        this.state = state;
        this.message = message;
        this.createBy = createBy;
        this.createTime = createTime;
    }


}
