package com.rabbiter.healthsys.controller;

import com.rabbiter.healthsys.common.Unification;
import com.rabbiter.healthsys.entity.Menu;
import com.rabbiter.healthsys.service.IMenuService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author Skyforever
 * @since 2024-07-23
 */
@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
@Slf4j
public class MenuController {
    private final IMenuService menuService;

    /**
     * 查询所有菜单数据
     *
     * @return 返回Unification对象，包含查询到的所有菜单数据
     */
    @ApiOperation("查询所有菜单数据")
    @GetMapping
    public Unification<List<Menu>> getAllMenu() {
        List<Menu> menuList = menuService.getAllMenu();
        return Unification.success(menuList);
    }
}

