package com.vim.tio.messages;

import cn.hutool.json.JSONObject;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * websocket 通讯的json 封装
 *
 * @author 乐天
 * @since 2018-10-07
 */
@Data
public class SendInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 发送信息的代码
     */
    private String code;

    /**
     * 信息的json 根据不同的消息类型分别解析
     */
    private JSONObject message;


    public SendInfo() {
    }



    public SendInfo(String code,JSONObject message) {
        this.code = code;
        this.message = message;
    }

}
