package com.vim.webpage.controller.Web;

import com.vim.webpage.domain.User;
import com.vim.webpage.domain.Video;
import com.vim.webpage.service.User.UserService;
import com.vim.webpage.service.Video.VideoService;
import com.vim.webpage.Utils.VideoDataPackageUtil;
import com.vim.webpage.Utils.UserDataPackageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 首页视频数据获取控制器
 * 获取首页展示的各分类视频数据
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class VideoGetController {

    @Autowired
    private VideoService videoService;

    @Autowired
    private UserService userService;

    /**
     * 获取首页数据
     * GET /api/getIndexPageData?lang=zhcn
     * 
     * @param lang 语言代码 (可选, 默认 zhcn)
     * @return 首页数据，包含各分类视频和随机用户数据
     */
    @GetMapping("/getIndexPageData")
    public ResponseEntity<Map<String, Object>> getIndexPageData(
            @RequestParam(value = "lang", required = false) String lang) {
        
        try {
            // 处理语言参数
            if (!StringUtils.hasText(lang) || "undefined".equals(lang)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("code", "0001");
                errorResponse.put("message", "need query");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // 标准化语言代码（使用新变量避免 final 问题）
            String normalizedLang = lang.toLowerCase().replace("-", "");
            if ("zhcn".equals(normalizedLang)) {
                normalizedLang = "";
            }
            final String finalLang = normalizedLang; // 用于 lambda 表达式

            log.info("📺 获取首页数据, 语言: {}", finalLang.isEmpty() ? "zhcn" : finalLang);

            // 定义分类顺序
            List<String> categoryOrder = Arrays.asList(
                    "国产自拍", "日韩精品", "onlyfans精选", "欧美视频", 
                    "coser福利姬", "精品探花", "国产传媒", "卡通动漫", "AI生成"
            );

            // 获取随机用户数据
            List<String> userFields = getUserFieldsByLang(finalLang);
            List<User> randomUsers = userService.getRandomUsers(6, userFields, finalLang);
            // 处理用户数据并转换为 Map
            List<User> processedUsers = randomUsers.stream()
                    .map(user -> UserDataPackageUtil.packageUserDataFromFields(user, finalLang))
                    .collect(Collectors.toList());

            // 获取最新视频数据 (第1页, 每页16条)
            List<Video> recentVideos = videoService.getLatestVideos(0, 16, finalLang);
            log.info("📹 获取到最新视频数量: {}", recentVideos.size());

            // 构建首页数据数组
            List<Map<String, Object>> indexPageData = new ArrayList<>();

            // 首先添加最新视频数据
            if (!recentVideos.isEmpty()) {
                Map<String, Object> recentSection = new HashMap<>();
                recentSection.put("type", "recent");
                recentSection.put("title", "最新视频");
                recentSection.put("videos", recentVideos.stream()
                        .map(video -> VideoDataPackageUtil.packageVideoDataFromFields(video, finalLang))
                        .collect(Collectors.toList()));
                indexPageData.add(recentSection);
            }

            // 按顺序获取并添加各分类视频数据
            for (String category : categoryOrder) {
                List<Video> categoryVideos = videoService.getVideosByCategory(category, 0, 16, finalLang);
                
                Map<String, Object> categorySection = new HashMap<>();
                categorySection.put("type", category);
                categorySection.put("videos", categoryVideos.stream()
                        .map(video -> VideoDataPackageUtil.packageVideoDataFromFields(video, finalLang))
                        .collect(Collectors.toList()));
                indexPageData.add(categorySection);
                
                log.debug("📂 分类 {} 获取到 {} 个视频", category, categoryVideos.size());
            }

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("code", 0);
            response.put("message", "success");
            response.put("data", indexPageData);
            response.put("UserRandomData", processedUsers);
            response.put("total", indexPageData.size());

            log.info("✅ 首页数据获取成功, 共 {} 个分区", indexPageData.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ 获取首页数据失败: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", "0001");
            errorResponse.put("message", "get IndexPageData Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 根据语言获取用户字段列表
     * 
     * @param lang 语言代码
     * @return 字段列表
     */
    private List<String> getUserFieldsByLang(String lang) {
        List<String> fields = new ArrayList<>();
        fields.add("_id");
        fields.add("username");
        fields.add("avatarUrl");
        fields.add("introduce");
        fields.add("publicCount");
        
        if (StringUtils.hasText(lang)) {
            switch (lang.toLowerCase()) {
                case "zhtw":
                    fields.add("usernameZHTW");
                    fields.add("introduceZHTW");
                    break;
                case "enus":
                    fields.add("usernameENUS");
                    fields.add("introduceENUS");
                    break;
                case "jajp":
                    fields.add("usernameJAJP");
                    fields.add("introduceJAJP");
                    break;
                case "kokr":
                    fields.add("usernameKOKR");
                    fields.add("introduceKOKR");
                    break;
                case "eses":
                    fields.add("usernameESES");
                    fields.add("introduceESES");
                    break;
                case "thth":
                    fields.add("usernameTHTH");
                    fields.add("introduceTHTH");
                    break;
                case "vivn":
                    fields.add("usernameVIVN");
                    fields.add("introduceVIVN");
                    break;
                case "msmy":
                    fields.add("usernameMSMY");
                    fields.add("introduceMSMY");
                    break;
            }
        }
        
        return fields;
    }
}
