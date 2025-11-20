package com.vim.webpage.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

import java.util.Date;

@Data
@Document(collection = "alltags") // 指定 MongoDB 集合名称
public class Tags {

    @Id
    private String id; // MongoDB 自动生成的 ID
    @Indexed(unique = true)
    private String zhcn; // 中文简体
    private String zhtw; // 中文繁体
    private String jajp; // 日文
    private String enus; // 英文
    private String kokr; // 韩文
    private String eses; // 西班牙文
    private String thth; // 泰文
    private String vivn; // 越南文
    private String msmy; // 马来文
    private Date updatedAt = new Date(); // 更新时间
}