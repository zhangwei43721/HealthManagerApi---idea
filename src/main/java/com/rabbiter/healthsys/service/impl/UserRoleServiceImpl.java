package com.rabbiter.healthsys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rabbiter.healthsys.entity.UserRole;
import com.rabbiter.healthsys.mapper.UserRoleMapper;
import com.rabbiter.healthsys.service.IUserRoleService;
import org.springframework.stereotype.Service;

/**
 * 用户角色服务实现类
 * 
 * @author Skyforever
 * @since 2025-05-01
 */
@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements IUserRoleService {

}
