package com.vim.webpage.service.VideoTags;

import com.vim.webpage.domain.Tags;

import java.util.List;
import java.util.Map;

/**
 * 视频标签服务接口
 * 管理多语言标签的 CRUD 操作
 */
public interface IVideoTagsService {
    
    /**
     * 创建或更新标签
     * @param tags 标签对象
     * @return 保存后的标签对象
     */
    Tags saveTag(Tags tags);
    
    /**
     * 批量创建标签
     * @param tagsList 标签列表
     * @return 保存的标签数量
     */
    int batchSaveTags(List<Tags> tagsList);
    
    /**
     * 根据中文标签查询
     * @param zhcn 中文简体标签
     * @return 标签对象
     */
    Tags getTagByZhcn(String zhcn);
    
    /**
     * 根据ID查询标签
     * @param id 标签ID
     * @return 标签对象
     */
    Tags getTagById(String id);
    
    /**
     * 获取所有标签
     * @return 标签列表
     */
    List<Tags> getAllTags();
    
    /**
     * 根据语言代码获取标签列表
     * @param lang 语言代码 (zhcn, zhtw, enus, jajp, kokr, eses, thth, vivn, msmy)
     * @return 该语言下的所有标签名称
     */
    List<String> getAllTagsByLanguage(String lang);
    
    /**
     * 根据语言代码获取标签的翻译值
     * @param tag 标签对象
     * @param lang 语言代码
     * @return 对应语言的标签名称
     */
    String getTagValueByLanguage(Tags tag, String lang);
    
    /**
     * 将中文标签列表转换为指定语言的标签列表
     * @param zhcnTags 中文标签列表
     * @param lang 目标语言代码
     * @return 翻译后的标签列表
     */
    List<String> translateTags(List<String> zhcnTags, String lang);
    
    /**
     * 批量翻译标签（用于视频列表）
     * @param videoTagsMap 视频ID -> 中文标签列表的映射
     * @param lang 目标语言代码
     * @return 视频ID -> 翻译后标签列表的映射
     */
    Map<String, List<String>> batchTranslateTags(Map<String, List<String>> videoTagsMap, String lang);
    
    /**
     * 删除标签
     * @param id 标签ID
     * @return 是否删除成功
     */
    boolean deleteTag(String id);
    
    /**
     * 搜索标签（模糊匹配）
     * @param keyword 搜索关键词
     * @param lang 搜索的语言字段
     * @return 匹配的标签列表
     */
    List<Tags> searchTags(String keyword, String lang);
    
    /**
     * 统计标签总数
     * @return 标签总数
     */
    long countTags();
    
    /**
     * 检查标签是否存在
     * @param zhcn 中文标签
     * @return 是否存在
     */
    boolean tagExists(String zhcn);
}
