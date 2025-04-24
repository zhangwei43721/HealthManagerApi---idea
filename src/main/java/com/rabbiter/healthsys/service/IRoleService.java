package com.rabbiter.healthsys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rabbiter.healthsys.entity.Role;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author
 * @since 2024-07-23
 */
public interface IRoleService extends IService<Role> {

    boolean addRole(Role role);

    Role getRoleById(Integer id);

    void updateRole(Role role);

    void deleteRoleById(Integer id);
}
