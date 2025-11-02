package com.vim.tio;

import com.vim.common.config.VimConfig;
import jakarta.annotation.Resource;
import lombok.Getter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 开启类
 *
 * @author 乐天
 * @since 2018-04-10
 */
@Component
public class StartTioRunner implements CommandLineRunner {

    @Resource
    private TioWsMsgHandler tioWsMsgHandler;

    @Getter
    private TioWebsocketStarter appStarter;

    @Resource
    private VimConfig vimConfig;

    @Override
    public void run(String... args) throws Exception {
        appStarter = new TioWebsocketStarter(vimConfig.getWsPort(), tioWsMsgHandler);
        appStarter.getWsServerStarter().start();
    }

}
