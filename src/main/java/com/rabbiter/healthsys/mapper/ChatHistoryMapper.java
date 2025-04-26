package com.rabbiter.healthsys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rabbiter.healthsys.entity.ChatHistory;
import org.apache.ibatis.annotations.Mapper; // 或者依赖 Spring Boot 的 @MapperScan

/**
 * <p>
 * 用户和AI聊天历史记录 Mapper 接口
 * </p>
 *
 * @author Your Name // TODO: 改成你的名字
 * @since 2024-07-25 // TODO: 改成当前日期
 */
@Mapper // 如果没有全局 @MapperScan，需要在 Mapper 接口上加 @Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {
    // BaseMapper 提供了基本的 CRUD 方法（insert, selectById, selectList, updateById, deleteById, etc.）
    // 对于按用户和对话ID查询历史，可以使用 BaseMapper 配合 Wrapper，不需要自定义方法。
    // 对于按用户和对话ID删除历史，也可以使用 BaseMapper 配合 Wrapper，不需要自定义方法。
}