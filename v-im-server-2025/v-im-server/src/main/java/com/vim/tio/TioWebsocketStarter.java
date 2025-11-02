package com.vim.tio;

import lombok.Getter;
import org.tio.websocket.server.WsServerStarter;

/**
 * TIO 配置文件
 *
 * @author 乐天
 * @since 2018-04-10
 */
@Getter
public class TioWebsocketStarter {

    private final WsServerStarter wsServerStarter;

    /**
     * @author tanyaowu
     */
    public TioWebsocketStarter(int port, TioWsMsgHandler wsMsgHandler) throws Exception {
        wsServerStarter = new WsServerStarter(port, wsMsgHandler);

        org.tio.server.TioServerConfig serverGroupContext = wsServerStarter.getTioServerConfig();
        serverGroupContext.setName("V-IM");
        serverGroupContext.setTioServerListener(ServerAioListener.me);

        //如果你希望通过wss来访问，就加上下面的代码吧，不过首先你得有SSL证书（证书必须和域名相匹配，否则可能访问不了ssl）
//		String keyStoreFile = "classpath:config/ssl/keystore.jks";
//		String trustStoreFile = "classpath:config/ssl/keystore.jks";
//		String keyStorePwd = "214323428310224";
//		serverGroupContext.useSsl(keyStoreFile, trustStoreFile, keyStorePwd);

    }


}
