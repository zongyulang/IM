package com.vim.sdk.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vim.common.enums.DictSwitchEnum;
import com.vim.common.utils.VimUtil;
import com.vim.modules.group.domain.ImGroupInvite;
import com.vim.modules.group.domain.ImGroupUser;
import com.vim.modules.group.mapper.ImGroupInviteMapper;
import com.vim.modules.group.mapper.ImGroupUserMapper;
import com.vim.modules.group.result.GroupInvite;
import com.vim.modules.group.result.GroupInviteCount;
import com.vim.sdk.service.VimGroupInviteService;
import com.vim.tio.StartTioRunner;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tio.core.Tio;
import org.tio.server.TioServerConfig;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 群邀请接口实现
 *
 * @author 乐天
 */
@Service
@DS("master")
public class VimGroupInviteServiceImpl implements VimGroupInviteService {

    private static final String CACHE_KEY = "group";

    @Resource
    private ImGroupInviteMapper imGroupInviteMapper;

    @Resource
    private ImGroupUserMapper imGroupUserMapper;

    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 获取群邀请列表
     *
     * @return List
     */
    @Override
    public List<GroupInvite> list(String groupId) {
        LambdaQueryWrapper<ImGroupInvite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImGroupInvite::getCheckUserId, VimUtil.getLoginId())
                .eq(ImGroupInvite::getWaitCheck, DictSwitchEnum.NO.getCode())
                .eq(ImGroupInvite::getGroupId, groupId)
                .orderByDesc(ImGroupInvite::getId);
        return imGroupInviteMapper.selectList(queryWrapper).stream().map(ImGroupInvite::toGroupInvite).collect(Collectors.toList());
    }

    @Override
    public List<GroupInviteCount> waitCheckList() {
        return imGroupInviteMapper.waitCheckList(VimUtil.getLoginId());
    }

    /**
     * 获取我发出的群邀请列表
     *
     * @return List
     */
    @Override
    public List<GroupInvite> mySendList() {
        LambdaQueryWrapper<ImGroupInvite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImGroupInvite::getFromId, VimUtil.getLoginId())
                .orderByDesc(ImGroupInvite::getId);
        return imGroupInviteMapper.selectList(queryWrapper).stream().map(ImGroupInvite::toGroupInvite).collect(Collectors.toList());
    }

    /**
     * 同意群邀请
     *
     * @param groupInviteId 群邀请id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void agree(String groupInviteId) {
        //根据群邀请id获取群邀请信息
        ImGroupInvite imGroupInvite = imGroupInviteMapper.selectById(groupInviteId);
        //修改群邀请信息
        imGroupInvite.setWaitCheck(DictSwitchEnum.YES.getCode());
        imGroupInvite.setCheckResult(DictSwitchEnum.YES.getCode());
        imGroupInvite.setUpdateBy(VimUtil.getLoginId());
        imGroupInvite.setUpdateTime(new Date());
        imGroupInviteMapper.updateById(imGroupInvite);
        //判断是否已经存在同样的邀请
        LambdaQueryWrapper<ImGroupInvite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImGroupInvite::getGroupId, imGroupInvite.getGroupId())
                .eq(ImGroupInvite::getUserId, imGroupInvite.getUserId())
                .eq(ImGroupInvite::getWaitCheck, DictSwitchEnum.NO.getCode());
        List<ImGroupInvite> list = imGroupInviteMapper.selectList(queryWrapper);
        for (ImGroupInvite invite : list) {
            invite.setWaitCheck(DictSwitchEnum.YES.getCode());
            invite.setCheckResult(DictSwitchEnum.YES.getCode());
            invite.setCheckUserId(VimUtil.getLoginId());
            imGroupInviteMapper.updateById(invite);
        }
        //添加群成员
        ImGroupUser imGroupUser = new ImGroupUser();
        imGroupUser.preInsert();
        imGroupUser.setGroupId(imGroupInvite.getGroupId());
        imGroupUser.setUserId(imGroupInvite.getUserId());
        imGroupUser.setState(DictSwitchEnum.NO.getCode());
        imGroupUserMapper.insert(imGroupUser);
        StartTioRunner startTioRunner = applicationContext.getBean(StartTioRunner.class);
        TioServerConfig tioServerConfig = startTioRunner.getAppStarter().getWsServerStarter().getTioServerConfig();
        //用户绑定到tio群组里，这样用户会收到消息
        Tio.bindGroup(tioServerConfig, imGroupInvite.getUserId(), imGroupInvite.getGroupId());
        //清除缓存的群用户，等待重新加载
        redisTemplate.delete(CACHE_KEY + ":user:list::" + imGroupInvite.getGroupId());
    }

    /**
     * 不同意群邀请
     *
     * @param groupInviteId 群邀请id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refuse(String groupInviteId) {
        //根据群邀请id获取群邀请信息
        ImGroupInvite imGroupInvite = imGroupInviteMapper.selectById(groupInviteId);
        //修改群邀请信息
        imGroupInvite.setWaitCheck(DictSwitchEnum.YES.getCode());
        imGroupInvite.setCheckResult(DictSwitchEnum.NO.getCode());
        imGroupInvite.setCheckMessage("不同意");
        imGroupInvite.setUpdateBy(VimUtil.getLoginId());
        imGroupInvite.setUpdateTime(new Date());
        imGroupInviteMapper.updateById(imGroupInvite);
    }
}
