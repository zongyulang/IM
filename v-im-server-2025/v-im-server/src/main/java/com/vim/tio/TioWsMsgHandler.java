package com.vim.tio;


import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.vim.common.config.VimConfig;
import com.vim.common.enums.SendCodeEnum;
import com.vim.tio.messages.ReadyAuth;
import com.vim.tio.messages.SendInfo;
import com.vim.tio.service.ConnStatusService;
import com.vim.tio.service.MessageHandlerService;
import com.vim.tio.service.MessageLogService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.core.TioConfig;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpResponse;
import org.tio.websocket.common.WsRequest;
import org.tio.websocket.common.WsResponse;
import org.tio.websocket.server.handler.IWsMsgHandler;


/**
 * websocket 处理函数
 *
 * @author 乐天
 * @since 2018-10-08
 */
@Component
public class TioWsMsgHandler implements IWsMsgHandler {

    private static final Logger log = LoggerFactory.getLogger(TioWsMsgHandler.class);
    public static final String PING = "ping";
    public static final String PONG = "pong";

    public static TioConfig tioConfig;

    @Resource
    private VimConfig vimConfig;

    @Resource
    @Qualifier("singleMessageHandlerService")
    private MessageHandlerService singleMessageHandlerService;

    private MessageHandlerService messageHandlerService;

    /**
     * 初始化消息处理服务
     * 自动区分是否是单机模式还是集群模式
     */
    @PostConstruct
    public void messageHandlerService() {
        messageHandlerService = singleMessageHandlerService;
    }


    @Resource
    private MessageLogService messageLogService;

    @Resource
    private ConnStatusService connStatusService;


    /**
     * 握手时走这个方法，业务可以在这里获取cookie，request参数等
     *
     * @param request        request
     * @param httpResponse   httpResponse
     * @param channelContext channelContext
     * @return HttpResponse
     */
    @Override
    public HttpResponse handshake(HttpRequest request, HttpResponse httpResponse, ChannelContext channelContext) {
      return httpResponse;
    }

    /**
     * 握手成功后走这个方法
     * @param readyAuth readyAuth
     * @param channelContext channelContext
     */
    private void bindUserInfo(ReadyAuth readyAuth, ChannelContext channelContext)  {
        try {
            tioConfig = channelContext.tioConfig;
            String token = readyAuth.getToken();
            String client = readyAuth.getClient();
            String uuid = readyAuth.getUuid();
            String userId = StpUtil.getLoginIdByToken(token).toString();
            connStatusService.setConnStatus(userId, true);
            String userClientId = userId + ":" + client;
            clearOtherLogin(userClientId, uuid);
            // 绑定用户到当前节点channelContext
            Tio.bindUser(channelContext, userId);
            // 绑定用户组到当前节点channelContext
            Tio.bindGroup(channelContext, userClientId);
            // 绑定bsId到当前节点channelContext
            Tio.bindBsId(channelContext, client);

            // 绑定群和用户关系
            messageHandlerService.bindUserGroups(channelContext, userId);
        }catch (Exception e){
            log.error("绑定用户失败", e);
            Tio.close(channelContext, "绑定用户失败");
        }

    }


    /**
     * 字符消息（binaryType = blob）过来后会走这个方法
     *
     * @param wsRequest      wsRequest
     * @param text           text
     * @param channelContext channelContext
     * @return obj
     */
    @Override
    public Object onText(WsRequest wsRequest, String text, ChannelContext channelContext) {
        try {
            String userId = channelContext.userid;

            if (PING.equals(text)) {
                WsResponse wsResponse = WsResponse.fromText(PONG, "utf-8");
                Tio.send(channelContext, wsResponse);
                return null;
            }
            // 记录消息日志
            messageLogService.logMessage(text, userId);

            SendInfo sendInfo = JSON.parseObject(text, SendInfo.class);
            String code = sendInfo.getCode();
            if (SendCodeEnum.MESSAGE.getCode().equals(code)) {
                messageHandlerService.handleMessage(channelContext, sendInfo);
            } else if (SendCodeEnum.READY.getCode().equals(code)) {
                bindUserInfo(JSONUtil.toBean(sendInfo.getMessage(), ReadyAuth.class), channelContext);
                messageHandlerService.handleOffLineMessage(channelContext);
            } else if (SendCodeEnum.READ.getCode().equals(code)) {
                messageHandlerService.handleReadMessage(sendInfo);
            }else {
                messageHandlerService.handleOtherMessage(channelContext, sendInfo, text);
            }

        } catch (Exception e) {
            log.error("处理消息失败", e);
        }
        return null;
    }

    /**
     * 清理其他登录
     *
     * @param userGroupId 用户组id
     * @param uuid        uuid
     */
    private  void clearOtherLogin(String userGroupId, String uuid)  {
        JSONObject jsonObject = new JSONObject();
        jsonObject.set("uuid", uuid);
        SendInfo sendInfo = new SendInfo(SendCodeEnum.OTHER_LOGIN.getCode(),jsonObject);
        WsResponse wsResponse = WsResponse.fromText(JSON.toJSONString(sendInfo), "utf-8");
        Tio.sendToGroup(tioConfig, userGroupId, wsResponse);
    }

    /**
     * @param httpRequest    httpRequest
     * @param httpResponse   httpResponse
     * @param channelContext channelContext
     * @author tanyaowu tanyaowu
     */
    @Override
    public void onAfterHandshaked(HttpRequest httpRequest, HttpResponse httpResponse, ChannelContext channelContext) {

    }

    /**
     * 字节消息（binaryType = arraybuffer）过来后会走这个方法
     */
    @Override
    public Object onBytes(WsRequest wsRequest, byte[] bytes, ChannelContext channelContext) {
        return null;
    }

    /**
     * 当客户端发close flag时，会走这个方法
     */
    @Override
    public Object onClose(WsRequest wsRequest, byte[] bytes, ChannelContext channelContext) {
        Tio.remove(channelContext, "receive close flag");
        return null;
    }

}
