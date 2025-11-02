package com.vim.modules.group.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vim.modules.group.domain.ImGroupInvite;
import com.vim.modules.group.result.GroupInviteCount;

import java.util.List;

/**
 * @author 乐天
 */
public interface ImGroupInviteMapper extends BaseMapper<ImGroupInvite> {

    /**
     * 获取群邀请列表 (待审核)
     *
     * @param checkUserId 审核人id
     * @return List 群邀请列表
     */
    List<GroupInviteCount> waitCheckList(String checkUserId);
}




