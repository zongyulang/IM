package com.vim.modules.group.result;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 群邀请统计(待审核)
 *
 * @author 乐天
 */
@Data
public class GroupInviteCount implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public String groupId;

    public Long count;


}
