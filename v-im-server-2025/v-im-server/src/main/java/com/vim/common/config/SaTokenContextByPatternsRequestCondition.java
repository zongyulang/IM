package com.vim.common.config;

import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
import cn.dev33.satoken.spring.pathmatch.SaPatternsRequestConditionHolder;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class SaTokenContextByPatternsRequestCondition  extends SaTokenContextForSpringInJakartaServlet {

    @Override
    public boolean matchPath(String pattern, String path) {
        return SaPatternsRequestConditionHolder.match(pattern, path);
    }

}
