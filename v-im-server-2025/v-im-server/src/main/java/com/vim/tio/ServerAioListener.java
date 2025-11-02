package com.vim.tio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.core.intf.Packet;
import org.tio.websocket.common.WsSessionContext;
import org.tio.websocket.server.WsTioServerListener;

/**
 * TIO 监控类
 * 本类主要用于监听和记录TIO服务器的各种事件，如连接建立、数据发送、数据接收等
 * @author 乐天
 * @since 2018-04-10
 */
public class ServerAioListener extends WsTioServerListener {

    // 日志对象，用于记录服务器事件信息
    private static Logger log = LoggerFactory.getLogger(ServerAioListener.class);

    // 单例模式，确保只有一个ServerAioListener实例
    public static ServerAioListener me = new ServerAioListener();


    private ServerAioListener() {

    }

    /**
     * 在连接建立之后调用
     * 记录连接信息和当前所有连接的数量
     *
     * @param channelContext 通道上下文，包含连接信息
     * @param isConnected 是否已连接
     * @param isReconnect 是否是重新连接
     * @throws Exception 可能抛出的异常
     */
    @Override
    public void onAfterConnected(ChannelContext channelContext, boolean isConnected, boolean isReconnect) throws Exception {
        super.onAfterConnected(channelContext, isConnected, isReconnect);
        if (log.isInfoEnabled()) {
            log.info("onAfterConnected\r\n{}", channelContext);
            log.info("all \r\n{}", Tio.getAll(channelContext.tioConfig).getObj().size());
        }

    }

    /**
     * 数据发送之后调用
     * 记录发送的数据包和通道上下文信息
     *
     * @param channelContext 通道上下文，包含连接信息
     * @param packet 发送的数据包
     * @param isSentSuccess 数据是否发送成功
     * @throws Exception 可能抛出的异常
     */
    @Override
    public void onAfterSent(ChannelContext channelContext, Packet packet, boolean isSentSuccess) throws Exception {
        super.onAfterSent(channelContext, packet, isSentSuccess);
        if (log.isInfoEnabled()) {
            log.debug("onAfterSent\r\n{}\r\n{}", packet.logstr(), channelContext);
        }
    }

    /**
     * 关闭连接之前调用
     * 记录即将关闭的通道上下文信息，并统计当前在线用户数量
     *
     * @param channelContext 通道上下文，包含连接信息
     * @param throwable 异常信息，如果有的话
     * @param remark 关闭连接的备注信息
     * @param isRemove 是否从管理器中移除
     * @throws Exception 可能抛出的异常
     */
    @Override
    public void onBeforeClose(ChannelContext channelContext, Throwable throwable, String remark, boolean isRemove) throws Exception {
        super.onBeforeClose(channelContext, throwable, remark, isRemove);
        if (log.isInfoEnabled()) {
            log.debug("onBeforeClose\r\n{}", channelContext);
        }
        WsSessionContext wsSessionContext = (WsSessionContext) channelContext.getAttribute();
        log.debug("channelId:", channelContext.getId());
        if (wsSessionContext.isHandshaked()) {
            int count = Tio.getAll(channelContext.tioConfig).getObj().size();
            log.debug("在线用户数量：" + count);
        }
    }

    /**
     * 数据解码之后调用
     * 记录解码后的数据包和通道上下文信息
     *
     * @param channelContext 通道上下文，包含连接信息
     * @param packet 解码后的数据包
     * @param packetSize 数据包大小
     * @throws Exception 可能抛出的异常
     */
    @Override
    public void onAfterDecoded(ChannelContext channelContext, Packet packet, int packetSize) throws Exception {
        super.onAfterDecoded(channelContext, packet, packetSize);
        if (log.isInfoEnabled()) {
            log.debug("onAfterDecoded\r\n{}\r\n{}", packet.logstr(), channelContext);
        }
    }

    /**
     * 接收到字节之后调用
     * 记录接收到字节的通道上下文信息
     *
     * @param channelContext 通道上下文，包含连接信息
     * @param receivedBytes 接收到的字节数
     * @throws Exception 可能抛出的异常
     */
    @Override
    public void onAfterReceivedBytes(ChannelContext channelContext, int receivedBytes) throws Exception {
        super.onAfterReceivedBytes(channelContext, receivedBytes);
        if (log.isInfoEnabled()) {
            log.debug("onAfterReceivedBytes\r\n{}", channelContext);
        }
    }

    /**
     * 数据处理之后调用
     * 目前未实现任何逻辑
     *
     * @param channelContext 通道上下文，包含连接信息
     * @param packet 处理的数据包
     * @param cost 处理数据包所花费的时间
     * @throws Exception 可能抛出的异常
     */
    @Override
    public void onAfterHandled(ChannelContext channelContext, Packet packet, long cost) throws Exception {
        super.onAfterHandled(channelContext, packet, cost);
    }
}
