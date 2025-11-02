package com.vim.tio.messages;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class GroupUserUpdate  implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String groupId;

    private String userId;

    /**
     * delete | add
     */
    private String type;

    public GroupUserUpdate() {
    }

    public GroupUserUpdate(String groupId, String userId, String type) {
        this.groupId = groupId;
        this.userId = userId;
        this.type = type;
    }
}
