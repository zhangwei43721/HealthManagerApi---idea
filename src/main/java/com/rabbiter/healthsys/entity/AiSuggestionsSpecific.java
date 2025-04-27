package com.rabbiter.healthsys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 存储AI针对不同页面生成的健康建议表 (分列存储)
 * </p>
 *
 * @author Skyforever
 * @since 2025-04-27
 */
@Data
@NoArgsConstructor // Lombok: 自动生成无参构造函数
@AllArgsConstructor // Lombok: 自动生成全参构造函数
@TableName("ai_suggestions_specific") // MyBatis-Plus: 关联数据库表名
public class AiSuggestionsSpecific implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 建议主键ID
     */
    @TableId(value = "id", type = IdType.AUTO) // MyBatis-Plus: 声明主键, 策略为自增
    private Integer id;

    /**
     * 用户ID (外键, 关联 j_user.id)
     */
    private Integer userId;

    /**
     * 针对历史健康记录页面的建议内容
     */
    private String suggestionHistoricalHealth;

    /**
     * 针对现有健康记录页面的建议内容
     */
    private String suggestionCurrentHealth;

    /**
     * 针对运动信息表页面的建议内容
     */
    private String suggestionSportInfo;

    /**
     * 建议生成时间 (触发时间)
     */
    private LocalDateTime generatedAt;
}