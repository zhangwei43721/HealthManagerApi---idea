package com.rabbiter.healthsys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rabbiter.healthsys.entity.Menu;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author
 * @since 2024-07-23
 */
public interface IMenuService extends IService<Menu> {

    List<Menu> getAllMenu();

    List<Menu> getMenuListByUserId(Integer userId);
}
