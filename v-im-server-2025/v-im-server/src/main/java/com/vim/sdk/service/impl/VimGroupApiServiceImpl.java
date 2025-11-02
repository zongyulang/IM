package com.vim.sdk.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vim.common.bridge.VimBridgeService;
import com.vim.common.enums.ChatTypeEnum;
import com.vim.common.enums.DictSwitchEnum;
import com.vim.common.enums.MessageTypeEnum;
import com.vim.common.enums.SysDelEnum;
import com.vim.common.exception.VimBaseException;
import com.vim.common.utils.VimUtil;
import com.vim.modules.group.domain.ImGroup;
import com.vim.modules.group.domain.ImGroupInvite;
import com.vim.modules.group.domain.ImGroupUser;
import com.vim.modules.group.enums.FriendStatusEnum;
import com.vim.modules.group.mapper.ImGroupInviteMapper;
import com.vim.modules.group.mapper.ImGroupUserMapper;
import com.vim.modules.group.param.QuickGroup;
import com.vim.modules.group.result.Group;
import com.vim.modules.group.service.IImGroupService;
import com.vim.modules.upload.utils.CommonAvatarUtil;
import com.vim.modules.upload.utils.NineCellAvatarUtil;
import com.vim.modules.user.result.User;
import com.vim.sdk.service.VimGroupApiService;
import com.vim.sdk.service.VimMessageService;
import com.vim.tio.StartTioRunner;
import com.vim.tio.messages.Message;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.tio.core.Tio;
import org.tio.server.TioServerConfig;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * vim 群操作类，如果需要对接其他的系统，重新下面的方法即可
 *
 * @author 乐天
 */
@Slf4j
@Service
@DS("master")
public class VimGroupApiServiceImpl implements VimGroupApiService {


    // 缓存键前缀，用于存储群信息
    private static final String CACHE_GROUP = "group";

    // 缓存键，用于存储单个群的信息
    private static final String CACHE_GROUP_ONE = CACHE_GROUP + ":one";

    // 缓存键，用于存储用户所属的所有群的信息
    private static final String CACHE_GROUP_LIST = CACHE_GROUP + ":list";

    // 缓存键，用于存储群内所有用户的信息
    private static final String CACHE_GROUP_USER_LIST = CACHE_GROUP + ":user:list";

    // 缓存键，用于存储群内所有用户的ID列表
    private static final String CACHE_GROUP_USERID_LIST = CACHE_GROUP + ":user:ids";

    // 缓存键前缀，用于存储群头像信息
    private static final String GROUP_AVATAR = "group:avatar:";


    @Resource
    private IImGroupService iImGroupService;
    @Resource
    private ImGroupUserMapper imGroupUserMapper;
    @Resource
    private ImGroupInviteMapper imGroupInviteMapper;
    @Resource
    private ApplicationContext applicationContext;
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private VimMessageService vimMessageService;
    @Resource
    private VimBridgeService vimBridgeService;
    /**
     * 群消息推送
     *
     * @param message 消息内容
     * @param groupId 群id
     */
    private void pushGroupText(String message, String groupId) {
        Message msg = new Message();
        msg.setId(IdUtil.getSnowflakeNextIdStr());
        msg.setChatId(groupId);
        msg.setFromId(VimUtil.getLoginId());
        msg.setTimestamp(System.currentTimeMillis());
        msg.setContent(message);
        msg.setMessageType(MessageTypeEnum.EVENT.getCode());
        msg.setChatType(ChatTypeEnum.GROUP.getCode());
        try {
            vimMessageService.push(msg);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取用户所在的群
     *
     * @param userId 用户id
     * @return List<Group>
     */
    @Override
    @Cacheable(value = CACHE_GROUP_LIST, key = "#userId", unless = "#userId == null")
    public List<Group> getGroups(String userId) {
        return iImGroupService.getByUserId(userId).stream().map(this::transform).toList();
    }


    /**
     * 获取群里所有的用户
     *
     * @param groupId 群id
     * @return List<User>
     */
    @Override
    @Cacheable(value = CACHE_GROUP_USER_LIST, key = "#groupId")
    public List<User> getUsers(String groupId) {
        return iImGroupService.getUserByGroupId(groupId, -1);
    }

    /**
     * 获取群里用户
     *
     * @param groupId 群id
     * @return List<User>
     */
    @Override
    public List<User> getUsers(String groupId, int num) {
        return iImGroupService.getUserByGroupId(groupId, num);
    }

    /**
     * 获取群
     *
     * @param groupId 群id
     * @return Group
     */
    @Override
    @Cacheable(value = CACHE_GROUP_ONE, key = "#groupId")
    public Group get(String groupId) {
        ImGroup group = iImGroupService.getById(groupId);
        if (group == null) {
            throw new RuntimeException("不能找到对应的群，可能群已经被删除");
        }
        return transform(group);
    }

    /**
     * 给群新增用户
     *
     * @param groupId 群id
     * @param userIds 用户id，多个
     */
    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_GROUP_ONE, key = "#groupId"),
            @CacheEvict(cacheNames = CACHE_GROUP_USER_LIST, key = "#groupId"),
            @CacheEvict(cacheNames = CACHE_GROUP_USERID_LIST, key = "#groupId")
    })
    public String[] addUsers(String groupId, String[] userIds) {
        ImGroup group = iImGroupService.getById(groupId);
        // 如果不是群主，允许邀请，开启审核，需要添加到邀请表
        if ((!group.getMaster().equals(VimUtil.getLoginId())) && DictSwitchEnum.YES.getCode().equals(group.getOpenInvite()) && DictSwitchEnum.YES.getCode().equals(group.getInviteCheck())) {
            for (String userId : userIds) {
                ImGroupInvite imGroupInvite = new ImGroupInvite();
                imGroupInvite.setFromId(VimUtil.getLoginId());
                imGroupInvite.setUserId(userId);
                imGroupInvite.setGroupId(groupId);
                imGroupInvite.setWaitCheck(DictSwitchEnum.NO.getCode());
                //审核人一定是群主
                imGroupInvite.setCheckUserId(group.getMaster());
                imGroupInvite.preInsert();
                imGroupInviteMapper.insert(imGroupInvite);

            }
            return new String[0];
        }
        //群主可以直接添加
        else if (group.getMaster().equals(VimUtil.getLoginId())) {
            addUsers(groupId, userIds, group.getProhibition());
            return userIds;
        }
        //允许邀请，不需要审核可以直接添加
        else if (group.getOpenInvite().equals(DictSwitchEnum.YES.getCode()) && group.getInviteCheck().equals(DictSwitchEnum.NO.getCode())) {
            addUsers(groupId, userIds, group.getProhibition());
            return userIds;
        } else {
            throw new VimBaseException("非群主不能操作");
        }
    }

    @Override
    public Group quickGroup(QuickGroup quickGroup) {
        ImGroup imGroup = new ImGroup();
        imGroup.setMaster(VimUtil.getLoginId());
        imGroup.setName(quickGroup.getName());
        imGroup.setDelFlag(SysDelEnum.DEL_NO.getCode());
        imGroup.setInviteCheck(DictSwitchEnum.NO.getCode());
        imGroup.setOpenInvite(DictSwitchEnum.YES.getCode());
        imGroup.setProhibition(DictSwitchEnum.NO.getCode());
        imGroup.setProhibitFriend(DictSwitchEnum.NO.getCode());
        imGroup.preInsert();
        iImGroupService.save(imGroup);
        addUsers(imGroup.getId(), quickGroup.getUserIds(), imGroup.getProhibition());
        imGroup = iImGroupService.getById(imGroup.getId());
        return new Group(imGroup.getId(), imGroup.getName(), imGroup.getAvatar(), imGroup.getMaster(), imGroup.getOpenInvite(), imGroup.getInviteCheck(), imGroup.getProhibition(), imGroup.getProhibitFriend(), imGroup.getAnnouncement());
    }

    /**
     * 转让
     *
     * @param groupId 群id
     * @param userId  用户id
     */
    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_GROUP_ONE, key = "#groupId"),
            @CacheEvict(cacheNames = CACHE_GROUP_USER_LIST, key = "#groupId"),
            @CacheEvict(cacheNames = CACHE_GROUP_USERID_LIST, key = "#groupId")
    })
    public void transference(String groupId, String userId) {
        ImGroup group = iImGroupService.getById(groupId);
        //当前人必须是群主才能转让
        if (group.getMaster().equals(VimUtil.getLoginId())) {
            group.setMaster(userId);
            group.preUpdate();
            iImGroupService.updateById(group);

            LambdaQueryWrapper<ImGroupInvite> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ImGroupInvite::getCheckUserId, VimUtil.getLoginId())
                    .eq(ImGroupInvite::getGroupId, groupId)
                    .eq(ImGroupInvite::getWaitCheck, DictSwitchEnum.YES.getCode());
            //转让群也要把群待审核的信息一起转让
            List<ImGroupInvite> imGroupInvites = imGroupInviteMapper.selectList(queryWrapper);
            imGroupInvites.forEach(imGroupInvite -> {
                imGroupInvite.setCheckUserId(userId);
                imGroupInviteMapper.updateById(imGroupInvite);
            });
            User user = vimBridgeService.getUserById(userId);
            this.pushGroupText(StrUtil.format("{} 成为了新的管理员", user.getName()), groupId);
        } else {
            throw new VimBaseException("非群主不能操作");
        }
    }


    /**
     * 直接添加群用户
     *
     * @param groupId     群id
     * @param userIds     用户id
     * @param prohibition 是否禁言
     */
    private void addUsers(String groupId, String[] userIds, String prohibition) {
        StartTioRunner startTioRunner = applicationContext.getBean(StartTioRunner.class);
        TioServerConfig tioServerConfig = startTioRunner.getAppStarter().getWsServerStarter().getTioServerConfig();
        List<String> nameList = new ArrayList<>();
        for (String userId : userIds) {
            ImGroupUser imGroupUser = new ImGroupUser();
            imGroupUser.setUserId(userId);
            imGroupUser.setGroupId(groupId);
            imGroupUser.setState(FriendStatusEnum.COMMON.getCode());
            imGroupUser.preInsert();
            imGroupUserMapper.insert(imGroupUser);
            Tio.bindGroup(tioServerConfig, userId, groupId);
            nameList.add(vimBridgeService.getUserById(userId).getName());
            if (DictSwitchEnum.YES.getCode().equals(prohibition)) {
                Tio.unbindGroup(tioServerConfig, userId, groupId);
            }
            redisTemplate.delete(CACHE_GROUP_LIST + "::" + userId);

        }
        if (canGenGroupAvatar(groupId)) {
            ImGroup group = iImGroupService.getById(groupId);
            group.setAvatar(genGroupAvatar(groupId));
            iImGroupService.updateById(group);
        }
        this.pushGroupText(StrUtil.format("{}加入了群", String.join(",", nameList)), groupId);
    }


    /**
     * 删除群用户
     *
     * @param groupId  群id
     * @param userIds  用户id，多个
     * @param sendTips 是否发送通知
     */
    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_GROUP_ONE, key = "#groupId"),
            @CacheEvict(cacheNames = CACHE_GROUP_USER_LIST, key = "#groupId"),
            @CacheEvict(cacheNames = CACHE_GROUP_USERID_LIST, key = "#groupId")
    })
    public void delUsers(String groupId, List<String> userIds, boolean sendTips) {
        if (CollUtil.isEmpty(userIds)) {
            return;
        }

        // 获取TioServerConfig
        TioServerConfig tioServerConfig = getTioServerConfig();

        // 批量处理用户
        LambdaQueryWrapper<ImGroupUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImGroupUser::getGroupId, groupId)
                .in(ImGroupUser::getUserId, userIds);
        imGroupUserMapper.delete(queryWrapper);
        List<String> names = new ArrayList<>();
        // 批量清除用户缓存
        userIds.forEach(userId -> {
            redisTemplate.delete(CACHE_GROUP_LIST + "::" + userId);
            Tio.unbindGroup(tioServerConfig, userId, groupId);

            if (sendTips) {
                names.add(vimBridgeService.getUserById(userId).getName());
            }
        });

        if (sendTips && !names.isEmpty()) {
            pushGroupText(StrUtil.format("{}离开了群聊", String.join(",", names)), groupId);
        }
        // 重新生成群头像
        updateGroupAvatarIfNeeded(groupId);
    }

    /**
     * 解散一个群，不删除群信息，只清除用户
     *
     * @param groupId 群Id
     */
    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_GROUP_ONE, key = "#groupId"),
            @CacheEvict(cacheNames = CACHE_GROUP_USER_LIST, key = "#groupId"),
            @CacheEvict(cacheNames = CACHE_GROUP_USERID_LIST, key = "#groupId")
    })
    public void del(String groupId) {
        this.pushGroupText("群聊已解散", groupId);
        this.delUsers(groupId, iImGroupService.getUserIdsByGroupId(groupId, -1), false);
        this.delInvite(groupId);
    }


    /**
     * 群类型转换
     *
     * @param imGroup 群
     * @return Group
     */
    private Group transform(ImGroup imGroup) {
        return new Group(imGroup.getId(), imGroup.getName(), imGroup.getAvatar(), String.valueOf(imGroup.getMaster()), imGroup.getOpenInvite(), imGroup.getInviteCheck(), imGroup.getProhibition(), imGroup.getProhibitFriend(), imGroup.getAnnouncement());
    }

    /**
     * 新建一个群
     *
     * @param group 群
     * @return Group
     */
    @Override
    public Group save(Group group) {
        ImGroup imGroup = new ImGroup();
        imGroup.setMaster(VimUtil.getLoginId());
        imGroup.setName(group.getName());
        imGroup.setAvatar(CommonAvatarUtil.generateImg(group.getName()));
        imGroup.setDelFlag(SysDelEnum.DEL_NO.getCode());
        imGroup.setInviteCheck(group.getInviteCheck());
        imGroup.setOpenInvite(group.getOpenInvite());
        imGroup.setAnnouncement(group.getAnnouncement());
        imGroup.setProhibition(group.getProhibition());
        imGroup.setProhibitFriend(group.getProhibitFriend());
        imGroup.preInsert();
        iImGroupService.save(imGroup);

        String[] userIds = {VimUtil.getLoginId()};
        this.addUsers(imGroup.getId(), userIds);
        group.setId(imGroup.getId());
        //禁言操作
        prohibition(imGroup, true, true);
        return transform(imGroup);
    }

    /**
     * 更新一个群
     *
     * @param group 群
     */
    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_GROUP_ONE, key = "#group.id"),
    })
    public void update(Group group) {
        ImGroup imGroup = iImGroupService.getById(group.getId());
        if (imGroup == null) {
            throw new RuntimeException("不能找到对应的群，可能群已经被删除");
        }
        boolean isUpdate = !group.getName().equals(imGroup.getName());
        imGroup.setName(group.getName());
        imGroup.setInviteCheck(group.getInviteCheck());
        imGroup.setOpenInvite(group.getOpenInvite());
        if (StrUtil.isNotBlank(group.getAnnouncement())) {
            imGroup.setAnnouncement(group.getAnnouncement());
        }
        imGroup.setProhibition(group.getProhibition());
        imGroup.setProhibitFriend(group.getProhibitFriend());
        imGroup.setDelFlag(SysDelEnum.DEL_NO.getCode());
        imGroup.preUpdate();
        iImGroupService.updateById(imGroup);
        //禁言操作
        boolean isChange = !imGroup.getProhibition().equals(group.getProhibition());
        prohibition(imGroup, isChange, false);
        redisTemplate.delete(CACHE_GROUP_ONE + "::" + group.getId());
        if(isUpdate){
            this.pushGroupText(StrUtil.format("群名称修改为：{}", group.getName()), imGroup.getId());
        }
    }

    /**
     * 生成群头像
     *
     * @param groupId 群id
     * @return 生成的图像地址
     */
    private String genGroupAvatar(String groupId) {
        List<User> users = iImGroupService.getUserByGroupId(groupId, 9);

        if (users.size() == 9) {
            redisTemplate.opsForValue().set(GROUP_AVATAR + groupId, DictSwitchEnum.YES.getCode());
        }

        return users.stream()
                .map(this::getUserAvatar)
                .filter(Objects::nonNull)
                .limit(9)
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        images -> NineCellAvatarUtil.generate(images, 128, 2)
                ));
    }

    /**
     * 是否能够生成群头像
     *
     * @param groupId 群id
     * @return boolean
     */
    private boolean canGenGroupAvatar(String groupId) {
        return !DictSwitchEnum.YES.getCode().equals(redisTemplate.opsForValue().get(GROUP_AVATAR + groupId));
    }


    /**
     * 禁言
     *
     * @param imGroup  群
     * @param isChange 是否改变
     * @param isNew    是否新建
     */
    private void prohibition(ImGroup imGroup, boolean isChange, boolean isNew) {
        if (isChange) {
            StartTioRunner startTioRunner = applicationContext.getBean(StartTioRunner.class);
            TioServerConfig tioServerConfig = startTioRunner.getAppStarter().getWsServerStarter().getTioServerConfig();
            List<String> userIds = iImGroupService.getUserIdsByGroupId(imGroup.getId(), -1);
            //禁言
            if (DictSwitchEnum.YES.getCode().equals(imGroup.getProhibition())) {
                this.pushGroupText("群已被管理员禁言", imGroup.getId());
                userIds.forEach(id -> Tio.unbindGroup(tioServerConfig, id, imGroup.getId()));
            } else {
                userIds.forEach(id -> Tio.bindGroup(tioServerConfig, id, imGroup.getId()));
                //新增的不要出现这个提示
                if (!isNew) {
                    this.pushGroupText("群已被管理员解除禁言", imGroup.getId());
                }
            }
        }
    }

    /**
     * 删除群邀请
     *
     * @param groupId 群id
     */
    public void delInvite(String groupId) {
        LambdaQueryWrapper<ImGroupInvite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImGroupInvite::getGroupId, groupId);
        imGroupInviteMapper.delete(queryWrapper);
    }

    /**
     * 检查用户是否为群成员
     *
     * @param groupId 群id
     * @param userId  用户id
     * @return boolean 是否为群成员
     */
    @Override
    public boolean isMember(String groupId, String userId) {
        LambdaQueryWrapper<ImGroupUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImGroupUser::getGroupId, groupId)
                .eq(ImGroupUser::getUserId, userId);
        return imGroupUserMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    @Cacheable(value = CACHE_GROUP_USERID_LIST, key = "#groupId")
    public List<String> getUserIdsByGroupId(String groupId) {
        LambdaQueryWrapper<ImGroupUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImGroupUser::getGroupId, groupId)
                .orderByAsc(ImGroupUser::getCreateTime);
        List<ImGroupUser> imGroupUsers = imGroupUserMapper.selectList(wrapper);
        return imGroupUsers.stream()
                .map(ImGroupUser::getUserId)
                .collect(Collectors.toList());
    }

    @Override
    public void updateGroupName(String id, String groupName) {
        ImGroup imGroup = iImGroupService.getById(id);
        if (imGroup == null) {
            throw new RuntimeException("不能找到对应的群，可能群已经被删除");
        }
        boolean isUpdate = !groupName.equals(imGroup.getName());
        imGroup.setName(groupName);
        imGroup.preUpdate();
        iImGroupService.updateById(imGroup);
        redisTemplate.delete(CACHE_GROUP_ONE + "::" + id);
        if(isUpdate){
            this.pushGroupText(StrUtil.format("群名称修改为：{}", groupName), imGroup.getId());
        }
    }

    // 抽取的辅助方法
    private TioServerConfig getTioServerConfig() {
        StartTioRunner startTioRunner = applicationContext.getBean(StartTioRunner.class);
        return startTioRunner.getAppStarter().getWsServerStarter().getTioServerConfig();
    }

    private void updateGroupAvatarIfNeeded(String groupId) {
        int currentUserCount = iImGroupService.getUserIdsByGroupId(groupId, -1).size();
        if (currentUserCount < 9) {
            redisTemplate.opsForValue().set(GROUP_AVATAR + groupId, DictSwitchEnum.NO.getCode());
            ImGroup group = iImGroupService.getById(groupId);
            group.setAvatar(genGroupAvatar(groupId));
            iImGroupService.updateById(group);
        }
    }

    private BufferedImage getUserAvatar(User user) {
        try {
            return ImgUtil.toBufferedImage(
                    ImgUtil.scale(
                            ImgUtil.getImage(URI.create(user.getAvatar()).toURL()),
                            100,
                            100
                    )
            );
        } catch (Exception e) {
            log.warn("获取用户头像失败: userId={}, name={}", user.getId(), user.getName(), e);
            try {
                String defaultAvatar = CommonAvatarUtil.generateImg(user.getName());
                return defaultAvatar != null ?
                        ImgUtil.toBufferedImage(ImgUtil.getImage(URI.create(defaultAvatar).toURL())) :
                        null;
            } catch (Exception ex) {
                log.error("生成默认头像失败: userId={}, name={}", user.getId(), user.getName(), ex);
                return null;
            }
        }
    }

    public static void main(String[] args) throws Exception{
        Image image = ImgUtil.getImage(URI.create("https://www.dl.gov.cn/picture/0/s_25030107540681757861.jpg").toURL());
        System.out.println(image);
    }
}
