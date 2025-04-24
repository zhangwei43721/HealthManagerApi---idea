package com.rabbiter.healthsys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rabbiter.healthsys.common.Unification;
import com.rabbiter.healthsys.entity.Role;
import com.rabbiter.healthsys.service.IRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author Skyforever
 * @since 2024-07-23
 */

@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
@Slf4j
public class RoleController {
    private final IRoleService roleService;

    @GetMapping("/list")
    // @RequestParam获取参数
    public Unification<Map<String, Object>> getRoleList(@RequestParam(value = "roleName", required = false) String roleName,
                                                        @RequestParam(value = "pageNo") Long pageNo,
                                                        @RequestParam(value = "pageSize") Long pageSize) {

        // 构造查询条件
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasLength(roleName), Role::getRoleName, roleName);
        wrapper.orderByDesc(Role::getRoleId);

        // 分页查询
        Page<Role> page = new Page<>(pageNo, pageSize);
        roleService.page(page, wrapper);

        // 将查询结果封装到Map中返回
        Map<String, Object> data = new HashMap<>();
        data.put("total", page.getTotal());
        data.put("rows", page.getRecords());
        return Unification.success(data);
    }


    @PostMapping
    public Unification<?> addRole(@RequestBody Role role) {
        boolean result = roleService.addRole(role);
        if (result) {
            return Unification.success("新增成功");
        } else {
            return Unification.fail("用户名已存在");
        }
    }

    @PutMapping
    public Unification<?> updateRole(@RequestBody Role role) {
        roleService.updateRole(role);
        return Unification.success("修改成功");
    }

    @GetMapping("/{id}")
    public Unification<Role> getRoleById(@PathVariable("id") Integer id) {
        //根据id获取角色
        Role role = roleService.getRoleById(id);
        return Unification.success(role);
    }

    @DeleteMapping("/{id}")
    public Unification<Role> deleteRoleById(@PathVariable("id") Integer id) {
        //根据id删除角色
        roleService.deleteRoleById(id);
        return Unification.success("删除成功");
    }

    @GetMapping("/all")
    public Unification<List<Role>> getAllRole() {
        //获取所有角色
        List<Role> roleList = roleService.list();
        return Unification.success(roleList);
    }


}
