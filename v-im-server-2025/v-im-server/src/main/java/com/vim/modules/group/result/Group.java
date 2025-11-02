package com.vim.modules.group.result;

import com.vim.modules.user.result.User;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class Group implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private String id;

    /**
     * 群名称
     */
    private String name;

    /**
     * 群头像
     */
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
     * 邀请审核
     */
    private String inviteCheck;

    /**
     * 公告
     */
    private String announcement;

    /**
     * 禁言
     */
    private String prohibition;

    /**
     * 不允许加好友
     */
    private String prohibitFriend;

    /**
     * 群成员
     */
    private List<User> userList;

    public Group() {
    }

    public Group(String id, String name, String avatar, String master, String openInvite, String inviteCheck, String prohibition, String prohibitFriend, String announcement) {
        this.id = id;
        this.name = name;
        this.avatar = avatar;
        this.master = master;
        this.openInvite = openInvite;
        this.inviteCheck = inviteCheck;
        this.prohibition = prohibition;
        this.prohibitFriend = prohibitFriend;
        this.announcement = announcement;
    }


}
