package com.vim.webpage.controller.Web.Video;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vim.webpage.Utils.multi_language;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    // 首页的视频数据
    @GetMapping("/getIndexPageData")
    public Map<String, Object> getIndexPageVideo(@RequestParam(required = false) String lang) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 直接使用静态方法调用
            String processedLang = multi_language.processLanguage(lang);
            
            // 模拟获取视频数据的逻辑
            result.put("success", true);
            result.put("message", "获取成功");
            result.put("lang", processedLang.isEmpty() ? "zh-CN" : lang);
            
            return result;
        } catch (IllegalArgumentException e) {
            // 处理 undefined 参数
            result.put("success", false);
            result.put("code", "0001");
            result.put("message", "Language parameter is invalid: " + e.getMessage());
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取失败: " + e.getMessage());
            return result;
        }
    }

}
