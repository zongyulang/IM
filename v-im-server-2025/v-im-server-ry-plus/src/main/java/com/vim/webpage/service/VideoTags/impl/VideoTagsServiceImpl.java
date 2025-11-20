package com.vim.webpage.service.VideoTags.impl;

import com.vim.webpage.domain.Tags;
import com.vim.webpage.service.VideoTags.IVideoTagsService;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 视频标签服务实现类
 * 管理多语言标签的 CRUD 操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoTagsServiceImpl implements IVideoTagsService {
    
    private final MongoTemplate mongoTemplate;
    
    private static final String COLLECTION_NAME = "all_tags";
    
    @Override
    public Tags saveTag(Tags tags) {
        try {
            if (tags.getUpdatedAt() == null) {
                tags.setUpdatedAt(new Date());
            }
            Tags savedTag = mongoTemplate.save(tags, COLLECTION_NAME);
            log.info("✅ 标签保存成功: {}", tags.getZhcn());
            return savedTag;
        } catch (Exception e) {
            log.error("❌ 保存标签失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存标签失败: " + e.getMessage());
        }
    }
    
    @Override
    public int batchSaveTags(List<Tags> tagsList) {
        try {
            for (Tags tag : tagsList) {
                if (tag.getUpdatedAt() == null) {
                    tag.setUpdatedAt(new Date());
                }
            }
            Collection<Tags> savedTags = mongoTemplate.insert(tagsList, COLLECTION_NAME);
            log.info("✅ 批量保存标签成功: {} 条", savedTags.size());
            return savedTags.size();
        } catch (Exception e) {
            log.error("❌ 批量保存标签失败: {}", e.getMessage(), e);
            throw new RuntimeException("批量保存标签失败: " + e.getMessage());
        }
    }
    
    @Override
    public Tags getTagByZhcn(String zhcn) {
        try {
            Query query = new Query(Criteria.where("zhcn").is(zhcn));
            return mongoTemplate.findOne(query, Tags.class, COLLECTION_NAME);
        } catch (Exception e) {
            log.error("❌ 查询标签失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public Tags getTagById(String id) {
        try {
            return mongoTemplate.findById(id, Tags.class, COLLECTION_NAME);
        } catch (Exception e) {
            log.error("❌ 查询标签失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public List<Tags> getAllTags() {
        try {
            return mongoTemplate.findAll(Tags.class, COLLECTION_NAME);
        } catch (Exception e) {
            log.error("❌ 查询所有标签失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<String> getAllTagsByLanguage(String lang) {
        try {
            List<Tags> allTags = getAllTags();
            return allTags.stream()
                    .map(tag -> getTagValueByLanguage(tag, lang))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("❌ 获取多语言标签列表失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public String getTagValueByLanguage(Tags tag, String lang) {
        if (tag == null || lang == null) {
            return null;
        }
        
        return switch (lang.toLowerCase()) {
            case "zhcn" -> tag.getZhcn();
            case "zhtw" -> tag.getZhtw() != null ? tag.getZhtw() : tag.getZhcn();
            case "enus" -> tag.getEnus() != null ? tag.getEnus() : tag.getZhcn();
            case "jajp" -> tag.getJajp() != null ? tag.getJajp() : tag.getZhcn();
            case "kokr" -> tag.getKokr() != null ? tag.getKokr() : tag.getZhcn();
            case "eses" -> tag.getEses() != null ? tag.getEses() : tag.getZhcn();
            case "thth" -> tag.getThth() != null ? tag.getThth() : tag.getZhcn();
            case "vivn" -> tag.getVivn() != null ? tag.getVivn() : tag.getZhcn();
            case "msmy" -> tag.getMsmy() != null ? tag.getMsmy() : tag.getZhcn();
            default -> tag.getZhcn(); // 默认返回中文
        };
    }
    
    @Override
    public List<String> translateTags(List<String> zhcnTags, String lang) {
        if (zhcnTags == null || zhcnTags.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            return zhcnTags.stream()
                    .map(zhcn -> {
                        Tags tag = getTagByZhcn(zhcn);
                        if (tag != null) {
                            return getTagValueByLanguage(tag, lang);
                        }
                        return zhcn; // 如果找不到翻译，返回原标签
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("❌ 翻译标签失败: {}", e.getMessage(), e);
            return zhcnTags; // 翻译失败返回原标签
        }
    }
    
    @Override
    public Map<String, List<String>> batchTranslateTags(Map<String, List<String>> videoTagsMap, String lang) {
        if (videoTagsMap == null || videoTagsMap.isEmpty()) {
            return Collections.emptyMap();
        }
        
        try {
            // 收集所有唯一的中文标签
            Set<String> uniqueZhcnTags = videoTagsMap.values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toSet());
            
            // 批量查询所有标签
            Query query = new Query(Criteria.where("zhcn").in(uniqueZhcnTags));
            List<Tags> allTags = mongoTemplate.find(query, Tags.class, COLLECTION_NAME);
            
            // 构建翻译映射表
            Map<String, String> translationMap = allTags.stream()
                    .collect(Collectors.toMap(
                            Tags::getZhcn,
                            tag -> getTagValueByLanguage(tag, lang),
                            (v1, v2) -> v1
                    ));
            
            // 翻译每个视频的标签
            Map<String, List<String>> result = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : videoTagsMap.entrySet()) {
                List<String> translatedTags = entry.getValue().stream()
                        .map(zhcn -> translationMap.getOrDefault(zhcn, zhcn))
                        .collect(Collectors.toList());
                result.put(entry.getKey(), translatedTags);
            }
            
            return result;
        } catch (Exception e) {
            log.error("❌ 批量翻译标签失败: {}", e.getMessage(), e);
            return videoTagsMap; // 翻译失败返回原映射
        }
    }
    
    @Override
    public boolean deleteTag(String id) {
        try {
            Query query = new Query(Criteria.where("_id").is(id));
            mongoTemplate.remove(query, Tags.class, COLLECTION_NAME);
            log.info("✅ 标签删除成功: {}", id);
            return true;
        } catch (Exception e) {
            log.error("❌ 删除标签失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public List<Tags> searchTags(String keyword, String lang) {
        try {
            String fieldName = switch (lang.toLowerCase()) {
                case "zhcn" -> "zhcn";
                case "zhtw" -> "zhtw";
                case "enus" -> "enus";
                case "jajp" -> "jajp";
                case "kokr" -> "kokr";
                case "eses" -> "eses";
                case "thth" -> "thth";
                case "vivn" -> "vivn";
                case "msmy" -> "msmy";
                default -> "zhcn";
            };
            
            Query query = new Query(Criteria.where(fieldName).regex(keyword, "i"));
            return mongoTemplate.find(query, Tags.class, COLLECTION_NAME);
        } catch (Exception e) {
            log.error("❌ 搜索标签失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public long countTags() {
        try {
            return mongoTemplate.count(new Query(), Tags.class, COLLECTION_NAME);
        } catch (Exception e) {
            log.error("❌ 统计标签数量失败: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    @Override
    public boolean tagExists(String zhcn) {
        try {
            Query query = new Query(Criteria.where("zhcn").is(zhcn));
            return mongoTemplate.exists(query, Tags.class, COLLECTION_NAME);
        } catch (Exception e) {
            log.error("❌ 检查标签存在性失败: {}", e.getMessage(), e);
            return false;
        }
    }
}
