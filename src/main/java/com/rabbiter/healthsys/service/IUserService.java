package com.rabbiter.healthsys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rabbiter.healthsys.entity.User;

import java.util.Map;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author
 * @since 2024-07-23
 */
public interface IUserService extends IService<User> {

    Map<String, Object> login(User user);

    Map<String, Object> getUserInfo(String token);

    void logout(String token);

    boolean addUser(User user);

    User getUserById(Integer id);

    void updateUser(User user);

    void deletUserById(Integer id);

    Map<String, Object> register(User register);


    Map<String, Object> getUserId();

    Map<String, Object> getBodyInfo(Integer userId);

    boolean updateuser(User user);

    Map<String, Object> Wxlogin(User user);

    Map<String, Object> WxgetUserId(String token);
    
    /**
     * 更新用户头像
     * @param userId 用户ID
     * @param avatarUrl 头像URL
     * @return 是否更新成功
     */
    boolean updateUserAvatar(Integer userId, String avatarUrl);
}
