package com.vim.common.exception;


import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * vim 自定义类
 *
 * @author 乐天kp
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VimBaseException extends RuntimeException {

    private Integer code;

    private String msg;

    public VimBaseException() {
        super("服务器异常");
        this.code = 500;
        this.msg = "服务器异常";
    }

    public VimBaseException(String msg, Object... arguments) {
        super(StrUtil.format(msg, arguments));
        this.code = 500;
        this.msg = StrUtil.format(msg, arguments);
    }

    public VimBaseException(Integer code, String msg, Object... arguments) {
        super(StrUtil.format(msg, arguments));
        this.code = code;
        this.msg = StrUtil.format(msg, arguments);
    }
}
