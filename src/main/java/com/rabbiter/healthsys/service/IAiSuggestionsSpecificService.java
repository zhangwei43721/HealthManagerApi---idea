package com.rabbiter.healthsys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rabbiter.healthsys.entity.AiSuggestionsSpecific;

/**
 * <p>
 * 存储AI针对不同页面生成的健康建议表 (分列存储) 服务类
 * </p>
 *
 * @author Skyforever
 * @since 2025-04-27
 */
public interface IAiSuggestionsSpecificService extends IService<AiSuggestionsSpecific> {

    // IService 已经提供了基本的 CRUD 方法 (save, getById, list, updateById, removeById 等)
    // 你可以在这里添加自定义的业务逻辑方法声明，例如：

    /**
     * 获取指定用户的最新建议记录
     * @param userId 用户ID
     * @return 最新的建议记录，如果没有则返回 null
     */
    AiSuggestionsSpecific getLatestSuggestionByUserId(Integer userId);

    // 其他你可能需要的业务方法...

}