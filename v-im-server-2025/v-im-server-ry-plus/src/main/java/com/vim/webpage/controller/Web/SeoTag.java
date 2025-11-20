package com.vim.webpage.controller.Web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vim.webpage.service.SeoHotTag.SeoHotTag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
    首页tag的控制器
*/
@RestController
@RequestMapping("/api")
public class SeoTag {

    @GetMapping("/IndexPageTags")
    public ResponseEntity<Map<String, Object>> IndexPageTags(@RequestParam String lang) {
        try {
            // 处理语言代码：转小写、移除"-"、空值默认为"zhcn"
            lang = (lang == null || lang.trim().isEmpty()) 
                ? "zhcn" 
                : lang.toLowerCase().replace("-", "");
            
            // 获取SEO关键词字符串
            String fixedTagsStr = SeoHotTag.getSEOkeywords(lang);
            
            // 分割字符串并去除空格
            List<String> fixedTag = Arrays.stream(fixedTagsStr.split("\\|"))
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .collect(Collectors.toList());
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("SeoTag", fixedTag);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            
            // 构建错误响应
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", "0001");
            errorResponse.put("message", "get Tags Error: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

}
