package com.rabbiter.healthsys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rabbiter.healthsys.entity.User;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author
 * @since 2024-07-23
 */
public interface UserMapper extends BaseMapper<User> {
    //这个方法是用来根据用户ID查询其拥有的角色名称列表的
    List<String> getRoleNameByUserId(Integer userId);

}
