package com.rabbiter.healthsys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rabbiter.healthsys.common.Unification;
import com.rabbiter.healthsys.config.JwtConfig;
import com.rabbiter.healthsys.entity.Body;
import com.rabbiter.healthsys.entity.BodyNotes;
import com.rabbiter.healthsys.entity.SportInfo;
import com.rabbiter.healthsys.entity.User;
import com.rabbiter.healthsys.service.IAiSuggestionsSpecificService;
import com.rabbiter.healthsys.service.IBodyNotesService;
import com.rabbiter.healthsys.service.IBodyService;
import com.rabbiter.healthsys.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户相关操作的前端控制器。
 * 提供用户的注册、登录、信息获取、体征信息管理等功能接口。
 *
 * @author Skyforever
 * @since 2024-07-23
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    /** JWT 配置类，用于解析和校验用户 token */
    private final JwtConfig jwtConfig;
    /** 用户服务，处理用户相关的业务逻辑 */
    private final IUserService userService;
    /** 体征服务，处理体征信息相关的业务逻辑 */
    private final IBodyService bodyService;
    /** 体征记录服务，处理体征日志相关的业务逻辑 */
    private final IBodyNotesService bodyNotesService;
    /** AI 建议服务，同步生成并存库 */
    private final IAiSuggestionsSpecificService aiSuggestionsSpecificService;

    /**
     * 获取所有用户
     *
     * @return 返回用户列表
     */
    @GetMapping("/all")
    public Unification<List<User>> getAllUser() {
        List<User> list = userService.list();
        return Unification.success(list, "查询成功");
    }


    @PostMapping("/login")
    public Unification<Map<String, Object>> login(@RequestBody User user) {
        Map<String, Object> data = userService.login(user);
        if (data != null) {
            return Unification.success(data);
        }
        return Unification.fail(20002, "用户名或密码错误");
    }


    @PostMapping("/Wxlogin")
    public Unification<Map<String, Object>> Wxlogin(@RequestBody User user) {
        Map<String, Object> data = userService.login(user);
        if (data != null) {
            return Unification.success(data);
        }
        return Unification.fail();
    }


    @PostMapping("/register")
    public Unification<Map<String, Object>> register(@RequestBody User register) {
        Map<String, Object> data = userService.register(register);
        if (data.get("success") != null) {
            return Unification.success("注册成功");
        } else {
            return Unification.fail(20004, "注册失败，用户名已存在");
        }
    }


    @GetMapping("/info")
    public Unification<Map<String, Object>> getUserInfo(@RequestHeader("X-Token") String token) {
        // 根据token获取用户信息
        Map<String, Object> data = userService.getUserInfo(token); // 调用userService的getUserInfo方法，传递token参数，返回一个Map<String,Object>类型的data
        if (data != null) {
            return Unification.success(data); // 如果data不为null，返回成功响应，将data作为响应数据返回
        }
        return Unification.fail(20003, "登录信息有误，请重新登录"); // 如果data为null，返回失败响应，返回错误码和错误信息
    }


    @PostMapping("/logout")
    public Unification<?> logout(@RequestHeader("X-Token") String token) {
        userService.logout(token);//将当前用户的登录状态从系统中注销
        return Unification.success();
    }


    /**
     * 根据查询条件获取用户列表，分页查询
     *
     * @param username 查询条件：用户名，可选
     * @param phone    查询条件：手机号，可选
     * @param pageNo   当前页码
     * @param pageSize 页面大小
     * @return 返回Unification包装后的用户列表，包含总数和当前页码的用户信息列表
     */
    @GetMapping("/list")
    public Unification<Map<String, Object>> getUserList(@RequestParam(value = "username", required = false) String username,
                                                        @RequestParam(value = "phone", required = false) String phone,
                                                        @RequestParam("pageNo") Long pageNo,
                                                        @RequestParam("pageSize") Long pageSize) {

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(StringUtils.hasLength(username), User::getUsername, username);
        wrapper.eq(StringUtils.hasLength(phone), User::getPhone, phone);
        Page<User> page = new Page<>(pageNo, pageSize);

        userService.page(page, wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("total", page.getTotal()); // 用户总数
        data.put("rows", page.getRecords()); // 用户列表
        return Unification.success(data);
    }


    @PostMapping("/add")
    public Unification<?> addUser(@RequestBody User user) {
        boolean result = userService.addUser(user);
        if (result) {
            return Unification.success("新增成功");
        } else {
            return Unification.fail("用户名已存在");
        }
    }


    @PutMapping("/update")
    public Unification<?> updateUser(@RequestBody User user) {
        // 防止密码被修改，将密码设为null
        user.setPassword(null);
        
        // 如果没有提供头像信息，则从数据库获取原来的头像信息
        if (user.getAvatar() == null || user.getAvatar().isEmpty()) {
            User dbUser = userService.getUserById(user.getId());
            if (dbUser != null) {
                user.setAvatar(dbUser.getAvatar());
            }
        }
        
        userService.updateUser(user);
        return Unification.success("修改成功");
    }


    @GetMapping("/{id}")
    public Unification<User> getUserById(@PathVariable("id") Integer id) {
        User user = userService.getUserById(id);
        return Unification.success(user);
    }


    @GetMapping("/getBodyNotes/{id}")
    public Unification<List<BodyNotes>> getBodyNotes(@PathVariable("id") Integer id) {
        List<BodyNotes> bodyNotesList = bodyNotesService.getBodyNotes(id);
        if (bodyNotesList == null || bodyNotesList.isEmpty()) { // 判断列表是否为空
            return Unification.fail("没有找到多余的记录");
        }
        return Unification.success(bodyNotesList);
    }


    @GetMapping("/WxgetBodyNotes")
    public Unification<Map<String, Object>> WxgetBodyNotes(@RequestHeader("X-Token") String token) {
        // 根据token获取用户信息
        Map<String, Object> data = userService.WxgetUserId(token);
        Integer userId = Integer.parseInt(data.get("id").toString());
        List<BodyNotes> bodyNotes = bodyNotesService.getBodyNotes(userId);
        data.put("bodyNotes", bodyNotes);
        System.out.println(data);
        return Unification.success(data);
    }


    @DeleteMapping("/{id}")
    public Unification<User> deleteUserById(@PathVariable("id") Integer id) {
        userService.deletUserById(id);
        return Unification.success("删除成功");
    }


    @PostMapping("/BodyInformation")
    public Unification<?> BodyInfomationUp(
            @RequestHeader("X-Token") String token,
            @RequestBody Body body) {
        boolean result = bodyService.insert(body);
        // 异步生成 AI 报告
        new Thread(() -> {
            try {
                aiSuggestionsSpecificService.generateHistoricalReport(token);
                aiSuggestionsSpecificService.generateCurrentReport(token);
                aiSuggestionsSpecificService.generateSportReport(token);
            } catch (Exception e) {
                log.error("异步生成AI报告失败", e);
            }
        }).start();
        if (result) {
            return Unification.success("数据上传成功，AI报告正在生成中");
        } else {
            return Unification.success("数据更新成功，AI报告正在生成中");
        }
    }


    @PostMapping("/BodyInformationNotes")
    public Unification<?> BodyInformationNotes(@RequestBody BodyNotes bodyNotes) {
        bodyNotesService.insert(bodyNotes);
        return Unification.success();
    }


    @GetMapping("/getUserId")
    public Unification<Map<String, Object>> getUserId(@RequestHeader("X-Token") String token) {
        try {
            User user = jwtConfig.parseToken(token, User.class);
            if (user != null) {
                Map<String, Object> data = new HashMap<>();
                data.put("id", user.getId());
                return Unification.success(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Unification.fail("用户id获取失败");
    }


    @GetMapping("/getBodyInfo")
    public Unification<Map<String, Object>> getBodyInfo(@RequestHeader("X-Token") String token) {
        try {
            User user = jwtConfig.parseToken(token, User.class);
            if (user != null) {
                Map<String, Object> data = userService.getBodyInfo(user.getId());
                if (data != null) {
                    return Unification.success(data);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Unification.fail(20002);
    }


    @GetMapping("/getBodyList")
    public Unification<Map<String, Object>> getBodyList(@RequestParam(value = "name", required = false) String name,
                                                        @RequestParam(value = "id", required = false) String id,
                                                        @RequestParam("pageNo") Long pageNo,
                                                        @RequestParam("pageSize") Long pageSize) {

        LambdaQueryWrapper<Body> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasLength(name), Body::getName, name);
        wrapper.eq(StringUtils.hasLength(id), Body::getId, id);
        Page<Body> page = new Page<>(pageNo, pageSize); // 构建分页对象，指定页码和每页大小

        bodyService.page(page, wrapper); // 调用userService的分页查询方法，查询指定页码、每页大小和查询条件的用户列表
        Map<String, Object> data = new HashMap<>();

        data.put("total", page.getTotal()); // 将查询到的用户总数放入响应数据中
        data.put("rows", page.getRecords()); // 将查询到的用户列表放入响应数据中
        return Unification.success(data);
    }


    @GetMapping("/getBodyById/{id}")
    public Unification<Body> getBodyById(@PathVariable("id") Integer id) {
        Body body = bodyService.getBodyById(id);
        return Unification.success(body);
    }


    @RequestMapping("/updateBody")
    public Unification<?> updateBody(@RequestBody Body body) {
        bodyService.updateBody(body);
        return Unification.success("修改成功");
    }


    @DeleteMapping("/deleteBodyById/{id}")
    public Unification<SportInfo> deleteBodyById(@PathVariable("id") Integer id) {
        bodyService.deletBodyById(id);
        bodyNotesService.delete(id);
        return Unification.success("删除成功");
    }


    @PutMapping("/changePassword")
    public Unification<?> changePassword(@RequestBody User user) {
        if (userService.updateuser(user)) {
            return Unification.success("修改成功，本次已为您登陆，下次登陆请用您的新密码");
        }
        return Unification.fail("修改失败，用户名或密码错误");
    }


    @GetMapping("/getUserBodyList")
    public Unification<Map<String, Object>> getUserBodyList(
            @RequestHeader("X-Token") String token,
            @RequestParam("pageNo") Long pageNo,
            @RequestParam("pageSize") Long pageSize) {
        try {
            User user = jwtConfig.parseToken(token, User.class);
            if (user != null) {
                LambdaQueryWrapper<BodyNotes> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(BodyNotes::getId, user.getId());
                Page<BodyNotes> page = new Page<>(pageNo, pageSize);
                bodyNotesService.page(page, wrapper);
                Map<String, Object> data = new HashMap<>();
                data.put("total", page.getTotal());
                data.put("rows", page.getRecords());
                return Unification.success(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Unification.fail("用户信息获取失败");
    }


        /**
     * 根据体征记录ID获取单条体征记录详情。
     * <p>
     * 请求方式：GET<br>
     * 路径参数：notesid - 体征记录ID
     * </p>
     * @param notesid 体征记录ID
     * @return Unification<BodyNotes> 返回指定ID的体征记录详情，若不存在则返回null。
     * <pre>
     * 示例：GET /user/getUserBodyById/123
     * </pre>
     */
    @GetMapping("/getUserBodyById/{notesid}")
    public Unification<BodyNotes> getUserBodyById(@PathVariable("notesid") Integer notesid) {
        System.out.println(notesid);
        BodyNotes bodyNotes = bodyNotesService.getUserBodyById(notesid);
        return Unification.success(bodyNotes);
    }

        /**
     * 更新用户体征记录。
     * <p>
     * 请求方式：POST/PUT<br>
     * 请求体：BodyNotes 对象，包含要更新的体征记录信息
     * </p>
     * @param bodyNotes 要更新的体征记录对象
     * @return Unification<?> 操作结果，成功返回"修改成功"
     * <pre>
     * 示例：POST /user/updateUserBody
     * {
     *   "id": 123,
     *   "height": 180,
     *   "weight": 70
     * }
     * </pre>
     */
    @RequestMapping("/updateUserBody")
    public Unification<?> updateUserBody(@RequestBody BodyNotes bodyNotes) {
        bodyNotesService.updateUserBody(bodyNotes);
        return Unification.success("修改成功");
    }


        /**
     * 根据体征记录ID删除用户体征记录。
     * <p>
     * 请求方式：DELETE<br>
     * 路径参数：notesid - 体征记录ID
     * </p>
     * @param notesid 体征记录ID
     * @return Unification<SportInfo> 操作结果，成功返回"删除成功"
     * <pre>
     * 示例：DELETE /user/deleteUserBodyById/123
     * </pre>
     */
    @DeleteMapping("/deleteUserBodyById/{notesid}")
    public Unification<SportInfo> deleteUserBodyById(@PathVariable("notesid") Integer notesid) {
        bodyNotesService.deleteUserBodyById(notesid);
        return Unification.success("删除成功");
    }


    /**
     * 更新用户头像
     * @param userId 用户ID
     * @param avatarUrl 头像URL
     * @return 更新结果
     */
    @PutMapping("/updateAvatar")
    public Unification<?> updateUserAvatar(@RequestParam("userId") Integer userId, 
                                          @RequestParam("avatarUrl") String avatarUrl) {
        boolean result = userService.updateUserAvatar(userId, avatarUrl);
        if (result) {
            return Unification.success("头像更新成功");
        } else {
            return Unification.fail("头像更新失败");
        }
    }

}
