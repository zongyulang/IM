package com.vim.sdk.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vim.common.enums.DictSwitchEnum;
import com.vim.common.utils.VimUtil;
import com.vim.modules.friend.domain.ImFriend;
import com.vim.modules.friend.enums.AddFriendResultEnum;
import com.vim.modules.friend.result.AddFriendResult;
import com.vim.modules.friend.result.Friend;
import com.vim.modules.friend.service.IImFriendService;
import com.vim.modules.group.enums.FriendStatusEnum;
import com.vim.modules.setting.result.Setting;
import com.vim.modules.user.result.User;
import com.vim.sdk.service.VimFriendApiService;
import com.vim.sdk.service.VimSettingService;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * vim 用户操作类，如果需要对接其他的系统，重新下面的方法即可
 *
 * @author 乐天
 */
@Service
@DS("master")
public class VimFriendApiServiceImpl implements VimFriendApiService {

    private static final String CACHE_KEY = "user";

    @Resource
    private IImFriendService iImFriendService;

    @Resource
    private VimSettingService vimSettingService;

    /**
     * 获取用户的好友 同时缓存
     *
     * @param userId 用户id
     * @return List<User>
     */
    @Override
    @Cacheable(value = CACHE_KEY + ":friend", key = "#userId")
    public List<User> getFriends(String userId) {
        return iImFriendService.getUserFriends(VimUtil.getLoginId(), FriendStatusEnum.COMMON.getCode());
    }

    /**
     * 获取待审核状态好友列表
     *
     * @param state state
     * @return List<User> List
     */
    @Override
    public List<Friend> getOthers(String state) {
        LambdaQueryWrapper<ImFriend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImFriend::getFriendId, VimUtil.getLoginId());
        queryWrapper.eq(ImFriend::getState, FriendStatusEnum.WAIT.getCode());
        return iImFriendService.list(queryWrapper)
                .stream().map(this::transformFriend).collect(Collectors.toList());
    }


    /**
     * 添加好友同时清除双方的好友缓存关系
     *
     * @param friendId 好友id
     * @param userId   用户id
     * @return boolean
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = CACHE_KEY + ":friend", key = "#userId"),
            @CacheEvict(value = CACHE_KEY + ":friend", key = "#friendId")
    })
    public AddFriendResult addFriends(String friendId, String message, String userId) {
        if (iImFriendService.getFriendIdsByUser(userId, FriendStatusEnum.COMMON.getCode()).contains(friendId)) {
            return new AddFriendResult(AddFriendResultEnum.ALREADY_FRIEND, AddFriendResultEnum.ALREADY_FRIEND.getMsg());
        }
        if (iImFriendService.getFriendIdsByUser(userId, FriendStatusEnum.WAIT.getCode()).contains(friendId)) {
            return new AddFriendResult(AddFriendResultEnum.ALREADY_REQUEST, AddFriendResultEnum.ALREADY_REQUEST.getMsg());
        }
        Setting setting = vimSettingService.getByUserId(Long.parseLong(friendId));
        if (DictSwitchEnum.NO.getCode().equals(setting.getCanAddFriend())) {
            return new AddFriendResult(AddFriendResultEnum.NOT_ALLOW_FRIEND, AddFriendResultEnum.NOT_ALLOW_FRIEND.getMsg());
        }
        ImFriend imFriend = new ImFriend();
        imFriend.setCreateTime(new Date());
        imFriend.setCreateBy(userId);
        imFriend.setFriendId(friendId);
        imFriend.setUserId(userId);
        imFriend.setMessage(message);
        if (DictSwitchEnum.YES.getCode().equals(setting.getAddFriendValidate())) {
            //已经有一个好友申请
            List<User> users = iImFriendService.getUserFriendState(userId, friendId, FriendStatusEnum.WAIT.getCode());
            if (!users.isEmpty()) {
                return new AddFriendResult(AddFriendResultEnum.WAIT_CHECK, AddFriendResultEnum.WAIT_CHECK.getMsg());
            }
            //如果已经有拒绝的申请，先删除拒绝的申请
            List<User> users1 = iImFriendService.getUserFriendState(userId, friendId, FriendStatusEnum.REFUSE.getCode());
            if (!users1.isEmpty()) {
                users1.forEach(user -> {
                    LambdaQueryWrapper<ImFriend> query = new LambdaQueryWrapper<>();
                    query.eq(ImFriend::getFriendId, user.getId());
                    query.eq(ImFriend::getUserId, userId);
                    query.eq(ImFriend::getState, FriendStatusEnum.REFUSE.getCode());
                    iImFriendService.remove(query);
                });
            }
            imFriend.setState(FriendStatusEnum.WAIT.getCode());
            iImFriendService.save(imFriend);
            return new AddFriendResult(AddFriendResultEnum.WAIT_CHECK, AddFriendResultEnum.WAIT_CHECK.getMsg());
        } else {
            imFriend.setState(FriendStatusEnum.COMMON.getCode());
            iImFriendService.save(imFriend);
            return new AddFriendResult(AddFriendResultEnum.SUCCESS, AddFriendResultEnum.SUCCESS.getMsg());

        }
    }

    /**
     * 同意添加好友同时清除双方的好友缓存关系
     *
     * @param friendId 好友id
     * @param userId   用户id
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = CACHE_KEY + ":friend", key = "#userId"),
            @CacheEvict(value = CACHE_KEY + ":friend", key = "#friendId")
    })
    public void agree(String friendId, String userId) {
        LambdaQueryWrapper<ImFriend> query = new LambdaQueryWrapper<>();
        query.eq(ImFriend::getFriendId, friendId);
        query.eq(ImFriend::getUserId, userId);
        query.eq(ImFriend::getState, FriendStatusEnum.WAIT.getCode());
        ImFriend imFriend = iImFriendService.getOne(query);
        if (imFriend != null) {
            imFriend.setState(FriendStatusEnum.COMMON.getCode());
            imFriend.setUpdateTime(new Date());
            imFriend.setUpdateBy(userId);
            iImFriendService.updateById(imFriend);
        }
    }

    /**
     * 删除好友同时清除双方的好友缓存关系
     *
     * @param friendId 好友id
     * @param userId   用户id
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = CACHE_KEY + ":friend", key = "#userId"),
            @CacheEvict(value = CACHE_KEY + ":friend", key = "#friendId")
    })
    public void delFriends(String friendId, String userId) {
        iImFriendService.realDelete(userId, friendId);
    }


    private Friend transformFriend(ImFriend imFriend) {
        return new Friend(String.valueOf(imFriend.getId()), String.valueOf(imFriend.getUserId()), String.valueOf(imFriend.getFriendId()), imFriend.getState(), imFriend.getMessage(), imFriend.getCreateBy(), imFriend.getCreateTime());
    }

    @Override
    public boolean isFriends(String friendId, String userId) {
        LambdaQueryWrapper<ImFriend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImFriend::getUserId, userId);
        wrapper.eq(ImFriend::getFriendId, friendId);
        wrapper.eq(ImFriend::getState, FriendStatusEnum.COMMON.getCode());
        boolean f0 = iImFriendService.count(wrapper) == 1;

        LambdaQueryWrapper<ImFriend> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(ImFriend::getUserId, friendId);
        wrapper1.eq(ImFriend::getFriendId, userId);
        wrapper1.eq(ImFriend::getState, FriendStatusEnum.COMMON.getCode());
        boolean f1 = iImFriendService.count(wrapper1) == 1;

        return f0 || f1;
    }

}
