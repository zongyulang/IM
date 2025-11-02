package com.vim.modules.friend.result;

import com.vim.modules.friend.enums.AddFriendResultEnum;
import lombok.Data;

@Data
public class AddFriendResult {

    private AddFriendResultEnum code;

    private String msg;

    public AddFriendResult(AddFriendResultEnum code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
