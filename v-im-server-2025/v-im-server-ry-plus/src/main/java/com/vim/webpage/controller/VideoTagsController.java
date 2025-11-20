package com.vim.webpage.controller;

import com.vim.webpage.domain.Tags;
import com.vim.webpage.service.VideoTags.IVideoTagsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 视频标签控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class VideoTagsController {
    
    private final IVideoTagsService videoTagsService;
    
    /**
     * 创建或更新标签
     * POST /api/tags
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createTag(@RequestBody Tags tags) {
        try {
            Tags savedTag = videoTagsService.saveTag(tags);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "标签保存成功");
            response.put("data", savedTag);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("创建标签失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "标签保存失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 批量创建标签
     * POST /api/tags/batch
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchCreateTags(@RequestBody List<Tags> tagsList) {
        try {
            int count = videoTagsService.batchSaveTags(tagsList);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "批量保存成功");
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("批量创建标签失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "批量保存失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取所有标签
     * GET /api/tags
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTags() {
        List<Tags> tags = videoTagsService.getAllTags();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", tags);
        response.put("count", tags.size());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 根据语言获取标签列表
     * GET /api/tags/lang/{lang}
     */
    @GetMapping("/lang/{lang}")
    public ResponseEntity<Map<String, Object>> getTagsByLanguage(@PathVariable String lang) {
        List<String> tags = videoTagsService.getAllTagsByLanguage(lang);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("language", lang);
        response.put("data", tags);
        response.put("count", tags.size());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 根据中文标签查询
     * GET /api/tags/zhcn/{zhcn}
     */
    @GetMapping("/zhcn/{zhcn}")
    public ResponseEntity<Map<String, Object>> getTagByZhcn(@PathVariable String zhcn) {
        Tags tag = videoTagsService.getTagByZhcn(zhcn);
        Map<String, Object> response = new HashMap<>();
        if (tag != null) {
            response.put("success", true);
            response.put("data", tag);
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "标签不存在");
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 根据ID查询标签
     * GET /api/tags/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTagById(@PathVariable String id) {
        Tags tag = videoTagsService.getTagById(id);
        Map<String, Object> response = new HashMap<>();
        if (tag != null) {
            response.put("success", true);
            response.put("data", tag);
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "标签不存在");
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 翻译标签列表
     * POST /api/tags/translate
     * Body: { "tags": ["标签1", "标签2"], "lang": "enus" }
     */
    @PostMapping("/translate")
    public ResponseEntity<Map<String, Object>> translateTags(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> zhcnTags = (List<String>) request.get("tags");
        String lang = (String) request.get("lang");
        
        List<String> translatedTags = videoTagsService.translateTags(zhcnTags, lang);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("language", lang);
        response.put("original", zhcnTags);
        response.put("translated", translatedTags);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 搜索标签
     * GET /api/tags/search?keyword=xxx&lang=zhcn
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchTags(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "zhcn") String lang) {
        List<Tags> tags = videoTagsService.searchTags(keyword, lang);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("keyword", keyword);
        response.put("language", lang);
        response.put("data", tags);
        response.put("count", tags.size());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 删除标签
     * DELETE /api/tags/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTag(@PathVariable String id) {
        boolean success = videoTagsService.deleteTag(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "删除成功" : "删除失败");
        return success ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    /**
     * 统计标签总数
     * GET /api/tags/stats/count
     */
    @GetMapping("/stats/count")
    public ResponseEntity<Map<String, Object>> countTags() {
        long count = videoTagsService.countTags();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", count);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 检查标签是否存在
     * GET /api/tags/exists/{zhcn}
     */
    @GetMapping("/exists/{zhcn}")
    public ResponseEntity<Map<String, Object>> tagExists(@PathVariable String zhcn) {
        boolean exists = videoTagsService.tagExists(zhcn);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("zhcn", zhcn);
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }
}
