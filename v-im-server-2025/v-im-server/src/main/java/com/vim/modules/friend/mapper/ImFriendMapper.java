package com.vim.modules.friend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vim.modules.friend.domain.ImFriend;
import org.apache.ibatis.annotations.Param;

/**
 * 好友Mapper接口
 *
 * @author 乐天
 * @since 2022-02-03
 */
public interface ImFriendMapper extends BaseMapper<ImFriend> {


    /**
     * 物理删除好友，不能用status，防止出现脏数据
     *
     * @param userId   用户ID
     * @param friendId 好友id
     */
    void realDelete(@Param("userId") String userId, @Param("friendId") String friendId);
}
