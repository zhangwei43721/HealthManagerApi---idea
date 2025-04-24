# 健康管理系统 API 文档

## 用户管理 API

### 获取所有用户
**请求地址:** /user/all
**请求方式:** GET
**传入参数:** 无
**传出参数:**
```json
{
  "code": 20000,
  "message": "查询成功",
  "data": [
    {
      "id": 1,
      "username": "用户名",
      "password": "密码",
      "phone": "手机号",
      "status": 1
    }
  ]
}
```

### 用户登录
**请求地址:** /user/login
**请求方式:** POST
**传入参数:**
```json
{
  "username": "用户名",
  "password": "密码"
}
```
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": {
    "token": "登录令牌"
  }
}
```

### 微信登录
**请求地址:** /user/Wxlogin
**请求方式:** POST
**传入参数:**
```json
{
  "username": "用户名",
  "password": "密码"
}
```
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": {
    "token": "登录令牌"
  }
}
```

### 用户注册
**请求地址:** /user/register
**请求方式:** POST
**传入参数:**
```json
{
  "username": "用户名",
  "password": "密码",
  "phone": "手机号"
}
```
**传出参数:**
```json
{
  "code": 20000,
  "message": "注册成功",
  "data": null
}
```

### 获取用户信息
**请求地址:** /user/info
**请求方式:** GET
**传入参数:**
- token: 登录令牌 (query参数)
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": {
    "id": 1,
    "username": "用户名",
    "phone": "手机号",
    "status": 1,
    "roles": ["角色名称"]
  }
}
```

### 用户登出
**请求地址:** /user/logout
**请求方式:** POST
**传入参数:**
- X-Token: 登录令牌 (header参数)
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": null
}
```

### 获取用户列表
**请求地址:** /user/list
**请求方式:** GET
**传入参数:**
- username: 用户名 (可选)
- phone: 手机号 (可选)
- pageNo: 页码
- pageSize: 每页大小
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": {
    "total": 100,
    "rows": [
      {
        "id": 1,
        "username": "用户名",
        "phone": "手机号",
        "status": 1
      }
    ]
  }
}
```

### 添加用户
**请求地址:** /user/addUser
**请求方式:** POST
**传入参数:**
```json
{
  "username": "用户名",
  "password": "密码",
  "phone": "手机号",
  "status": 1
}
```
**传出参数:**
```json
{
  "code": 20000,
  "message": "新增成功",
  "data": null
}
```

### 更新用户
**请求地址:** /user/updateUser
**请求方式:** PUT
**传入参数:**
```json
{
  "id": 1,
  "username": "用户名",
  "phone": "手机号",
  "status": 1
}
```
**传出参数:**
```json
{
  "code": 20000,
  "message": "修改成功",
  "data": null
}
```

### 根据ID获取用户
**请求地址:** /user/getUserById/{id}
**请求方式:** GET
**传入参数:**
- id: 用户ID (路径参数)
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": {
    "id": 1,
    "username": "用户名",
    "phone": "手机号",
    "status": 1
  }
}
```

### 获取身体记录
**请求地址:** /user/getBodyNotes/{id}
**请求方式:** GET
**传入参数:**
- id: 用户ID (路径参数)
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": [
    {
      "notesid": 1,
      "id": 1,
      "weight": 70,
      "height": 175,
      "bmi": 22.9,
      "date": "2025-04-16"
    }
  ]
}
```

### 微信获取身体记录
**请求地址:** /user/WxgetBodyNotes/{token}
**请求方式:** GET
**传入参数:**
- token: 登录令牌 (路径参数)
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": [
    {
      "notesid": 1,
      "id": 1,
      "weight": 70,
      "height": 175,
      "bmi": 22.9,
      "date": "2025-04-16"
    }
  ]
}
```

### 删除用户
**请求地址:** /user/deleteUserById/{id}
**请求方式:** DELETE
**传入参数:**
- id: 用户ID (路径参数)
**传出参数:**
```json
{
  "code": 20000,
  "message": "删除成功",
  "data": null
}
```

### 上传身体信息
**请求地址:** /user/BodyInfomationUp
**请求方式:** POST
**传入参数:**
```json
{
  "id": 1,
  "name": "姓名",
  "weight": 70,
  "height": 175,
  "bmi": 22.9
}
```
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": null
}
```

### 记录身体信息
**请求地址:** /user/BodyInformationNotes
**请求方式:** POST
**传入参数:**
```json
{
  "id": 1,
  "weight": 70,
  "height": 175,
  "bmi": 22.9,
  "date": "2025-04-16"
}
```
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": null
}
```

### 获取当前用户ID
**请求地址:** /user/getUserId
**请求方式:** GET
**传入参数:** 无
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": {
    "id": 1
  }
}
```

### 获取身体信息
**请求地址:** /user/getBodyInfo
**请求方式:** GET
**传入参数:** 无
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": {
    "id": 1,
    "name": "姓名",
    "weight": 70,
    "height": 175,
    "bmi": 22.9
  }
}
```

### 获取身体信息列表
**请求地址:** /user/getBodyList
**请求方式:** GET
**传入参数:**
- name: 姓名 (可选)
- id: 用户ID (可选)
- pageNo: 页码
- pageSize: 每页大小
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": {
    "total": 100,
    "rows": [
      {
        "id": 1,
        "name": "姓名",
        "weight": 70,
        "height": 175,
        "bmi": 22.9
      }
    ]
  }
}
```

### 根据ID获取身体信息
**请求地址:** /user/getBodyById/{id}
**请求方式:** GET
**传入参数:**
- id: 用户ID (路径参数)
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": {
    "id": 1,
    "name": "姓名",
    "weight": 70,
    "height": 175,
    "bmi": 22.9
  }
}
```

### 更新身体信息
**请求地址:** /user/updateBody
**请求方式:** PUT
**传入参数:**
```json
{
  "id": 1,
  "name": "姓名",
  "weight": 70,
  "height": 175,
  "bmi": 22.9
}
```
**传出参数:**
```json
{
  "code": 20000,
  "message": "修改成功",
  "data": null
}
```

### 删除身体信息
**请求地址:** /user/deleteBodyById/{id}
**请求方式:** DELETE
**传入参数:**
- id: 用户ID (路径参数)
**传出参数:**
```json
{
  "code": 20000,
  "message": "删除成功",
  "data": null
}
```

### 修改密码
**请求地址:** /user/changePassword
**请求方式:** PUT
**传入参数:**
```json
{
  "id": 1,
  "password": "旧密码",
  "newPassword": "新密码"
}
```
**传出参数:**
```json
{
  "code": 20000,
  "message": "修改成功，本次已为您登陆，下次登陆请用您的新密码",
  "data": null
}
```

### 获取用户身体记录列表
**请求地址:** /user/getUserBodyList
**请求方式:** GET
**传入参数:**
- pageNo: 页码
- pageSize: 每页大小
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": {
    "total": 100,
    "rows": [
      {
        "notesid": 1,
        "id": 1,
        "weight": 70,
        "height": 175,
        "bmi": 22.9,
        "date": "2025-04-16"
      }
    ]
  }
}
```

### 根据ID获取用户身体记录
**请求地址:** /user/getUserBodyById/{notesid}
**请求方式:** GET
**传入参数:**
- notesid: 记录ID (路径参数)
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": {
    "notesid": 1,
    "id": 1,
    "weight": 70,
    "height": 175,
    "bmi": 22.9,
    "date": "2025-04-16"
  }
}
```

### 更新用户身体记录
**请求地址:** /user/updateUserBody
**请求方式:** PUT
**传入参数:**
```json
{
  "notesid": 1,
  "id": 1,
  "weight": 70,
  "height": 175,
  "bmi": 22.9,
  "date": "2025-04-16"
}
```
**传出参数:**
```json
{
  "code": 20000,
  "message": "修改成功",
  "data": null
}
```

### 删除用户身体记录
**请求地址:** /user/deleteUserBodyById/{notesid}
**请求方式:** DELETE
**传入参数:**
- notesid: 记录ID (路径参数)
**传出参数:**
```json
{
  "code": 20000,
  "message": "删除成功",
  "data": null
}
```

## 运动信息 API

### 获取所有运动信息
**请求地址:** /sport/getAllSportInfo
**请求方式:** GET
**传入参数:** 无
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": {
    "sportInfos": [
      {
        "id": 1,
        "sportType": "运动类型",
        "sportName": "运动名称",
        "sportDesc": "运动描述",
        "sportImage": "运动图片"
      }
    ]
  }
}
```

### 获取运动信息列表
**请求地址:** /sport/getSportList
**请求方式:** GET
**传入参数:**
- sportType: 运动类型 (可选)
- pageNo: 页码
- pageSize: 每页大小
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": {
    "total": 100,
    "rows": [
      {
        "id": 1,
        "sportType": "运动类型",
        "sportName": "运动名称",
        "sportDesc": "运动描述",
        "sportImage": "运动图片"
      }
    ]
  }
}
```

### 添加运动信息
**请求地址:** /sport/add
**请求方式:** POST
**传入参数:**
```json
{
  "sportType": "运动类型",
  "sportName": "运动名称",
  "sportDesc": "运动描述",
  "sportImage": "运动图片"
}
```
**传出参数:**
```json
{
  "code": 20000,
  "message": "新增成功",
  "data": null
}
```

### 更新运动信息
**请求地址:** /sport/update
**请求方式:** PUT
**传入参数:**
```json
{
  "id": 1,
  "sportType": "运动类型",
  "sportName": "运动名称",
  "sportDesc": "运动描述",
  "sportImage": "运动图片"
}
```
**传出参数:**
```json
{
  "code": 20000,
  "message": "修改成功",
  "data": null
}
```

### 根据ID获取运动信息
**请求地址:** /sport/{id}
**请求方式:** GET
**传入参数:**
- id: 运动ID (路径参数)
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": {
    "id": 1,
    "sportType": "运动类型",
    "sportName": "运动名称",
    "sportDesc": "运动描述",
    "sportImage": "运动图片"
  }
}
```

### 删除运动信息
**请求地址:** /sport/{id}
**请求方式:** DELETE
**传入参数:**
- id: 运动ID (路径参数)
**传出参数:**
```json
{
  "code": 20000,
  "message": "删除成功",
  "data": null
}
```

## 运动详情 API

### 获取运动详情
**请求地址:** /detail/DetailInfo/{sportName}
**请求方式:** GET
**传入参数:**
- sportName: 运动名称 (路径参数)
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": {
    "id": 1,
    "sportType": "运动类型",
    "sportName": "运动名称",
    "sportDesc": "运动描述",
    "sportDetail": "运动详情"
  }
}
```

### 获取运动详情列表
**请求地址:** /detail/getDetailList
**请求方式:** GET
**传入参数:**
- sportType: 运动类型 (可选)
- pageNo: 页码
- pageSize: 每页大小
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": {
    "total": 100,
    "rows": [
      {
        "id": 1,
        "sportType": "运动类型",
        "sportName": "运动名称",
        "sportDesc": "运动描述",
        "sportDetail": "运动详情"
      }
    ]
  }
}
```

### 添加运动详情
**请求地址:** /detail/addDetail
**请求方式:** POST
**传入参数:**
```json
{
  "sportType": "运动类型",
  "sportName": "运动名称",
  "sportDesc": "运动描述",
  "sportDetail": "运动详情"
}
```
**传出参数:**
```json
{
  "code": 20000,
  "message": "新增成功",
  "data": null
}
```

### 更新运动详情
**请求地址:** /detail/updateDetail
**请求方式:** PUT
**传入参数:**
```json
{
  "id": 1,
  "sportType": "运动类型",
  "sportName": "运动名称",
  "sportDesc": "运动描述",
  "sportDetail": "运动详情"
}
```
**传出参数:**
```json
{
  "code": 20000,
  "message": "修改成功",
  "data": null
}
```

### 根据ID获取运动详情
**请求地址:** /detail/getDetailById/{id}
**请求方式:** GET
**传入参数:**
- id: 详情ID (路径参数)
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": {
    "id": 1,
    "sportType": "运动类型",
    "sportName": "运动名称",
    "sportDesc": "运动描述",
    "sportDetail": "运动详情"
  }
}
```

### 删除运动详情
**请求地址:** /detail/deleteDetailById/{id}
**请求方式:** DELETE
**传入参数:**
- id: 详情ID (路径参数)
**传出参数:**
```json
{
  "code": 20000,
  "message": "删除成功",
  "data": null
}
```

## 角色管理 API

### 获取角色列表
**请求地址:** /role/list
**请求方式:** GET
**传入参数:**
- roleName: 角色名称 (可选)
- pageNo: 页码
- pageSize: 每页大小
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": {
    "total": 100,
    "rows": [
      {
        "roleId": 1,
        "roleName": "角色名称",
        "roleDesc": "角色描述"
      }
    ]
  }
}
```

### 添加角色
**请求地址:** /role
**请求方式:** POST
**传入参数:**
```json
{
  "roleName": "角色名称",
  "roleDesc": "角色描述"
}
```
**传出参数:**
```json
{
  "code": 20000,
  "message": "新增成功",
  "data": null
}
```

### 更新角色
**请求地址:** /role
**请求方式:** PUT
**传入参数:**
```json
{
  "roleId": 1,
  "roleName": "角色名称",
  "roleDesc": "角色描述"
}
```
**传出参数:**
```json
{
  "code": 20000,
  "message": "修改成功",
  "data": null
}
```

### 根据ID获取角色
**请求地址:** /role/{id}
**请求方式:** GET
**传入参数:**
- id: 角色ID (路径参数)
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": {
    "roleId": 1,
    "roleName": "角色名称",
    "roleDesc": "角色描述"
  }
}
```

### 删除角色
**请求地址:** /role/{id}
**请求方式:** DELETE
**传入参数:**
- id: 角色ID (路径参数)
**传出参数:**
```json
{
  "code": 20000,
  "message": "删除成功",
  "data": null
}
```

### 获取所有角色
**请求地址:** /role/all
**请求方式:** GET
**传入参数:** 无
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": [
    {
      "roleId": 1,
      "roleName": "角色名称",
      "roleDesc": "角色描述"
    }
  ]
}
```

## 菜单管理 API

### 获取所有菜单
**请求地址:** /menu
**请求方式:** GET
**传入参数:** 无
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": [
    {
      "menuId": 1,
      "component": "组件路径",
      "path": "路由路径",
      "redirect": "重定向路径",
      "name": "菜单名称",
      "title": "菜单标题",
      "icon": "菜单图标",
      "parentId": 0,
      "isLeaf": true,
      "hidden": false,
      "children": []
    }
  ]
}
```

## 用户角色关联 API

### 获取用户角色关联
**请求地址:** /userRole
**请求方式:** GET
**传入参数:** 无
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "roleId": 1
    }
  ]
}
```

### 添加用户角色关联
**请求地址:** /userRole
**请求方式:** POST
**传入参数:**
```json
{
  "userId": 1,
  "roleId": 1
}
```
**传出参数:**
```json
{
  "code": 20000,
  "message": "添加成功",
  "data": null
}
```

### 删除用户角色关联
**请求地址:** /userRole/{id}
**请求方式:** DELETE
**传入参数:**
- id: 关联ID (路径参数)
**传出参数:**
```json
{
  "code": 20000,
  "message": "删除成功",
  "data": null
}
```

## 角色菜单关联 API

### 获取角色菜单关联
**请求地址:** /roleMenu
**请求方式:** GET
**传入参数:** 无
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": [
    {
      "id": 1,
      "roleId": 1,
      "menuId": 1
    }
  ]
}
```

### 添加角色菜单关联
**请求地址:** /roleMenu
**请求方式:** POST
**传入参数:**
```json
{
  "roleId": 1,
  "menuId": 1
}
```
**传出参数:**
```json
{
  "code": 20000,
  "message": "添加成功",
  "data": null
}
```

### 删除角色菜单关联
**请求地址:** /roleMenu/{id}
**请求方式:** DELETE
**传入参数:**
- id: 关联ID (路径参数)
**传出参数:**
```json
{
  "code": 20000,
  "message": "删除成功",
  "data": null
}
```

## 身体信息 API

### 获取身体信息
**请求地址:** /body
**请求方式:** GET
**传入参数:** 无
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": {
    "id": 1,
    "name": "姓名",
    "weight": 70,
    "height": 175,
    "bmi": 22.9
  }
}
```

### 添加身体信息
**请求地址:** /body
**请求方式:** POST
**传入参数:**
```json
{
  "name": "姓名",
  "weight": 70,
  "height": 175,
  "bmi": 22.9
}
```
**传出参数:**
```json
{
  "code": 20000,
  "message": "添加成功",
  "data": null
}
```

### 更新身体信息
**请求地址:** /body
**请求方式:** PUT
**传入参数:**
```json
{
  "id": 1,
  "name": "姓名",
  "weight": 70,
  "height": 175,
  "bmi": 22.9
}
```
**传出参数:**
```json
{
  "code": 20000,
  "message": "修改成功",
  "data": null
}
```

### 删除身体信息
**请求地址:** /body/{id}
**请求方式:** DELETE
**传入参数:**
- id: 身体信息ID (路径参数)
**传出参数:**
```json
{
  "code": 20000,
  "message": "删除成功",
  "data": null
}
```

## 身体记录 API

### 获取身体记录
**请求地址:** /bodyNotes
**请求方式:** GET
**传入参数:** 无
**传出参数:**
```json
{
  "code": 20000,
  "message": "success",
  "data": [
    {
      "notesid": 1,
      "id": 1,
      "weight": 70,
      "height": 175,
      "bmi": 22.9,
      "date": "2025-04-16"
    }
  ]
}
```

### 添加身体记录
**请求地址:** /bodyNotes
**请求方式:** POST
**传入参数:**
```json
{
  "id": 1,
  "weight": 70,
  "height": 175,
  "bmi": 22.9,
  "date": "2025-04-16"
}
```
**传出参数:**
```json
{
  "code": 20000,
  "message": "添加成功",
  "data": null
}
```

### 更新身体记录
**请求地址:** /bodyNotes
**请求方式:** PUT
**传入参数:**
```json
{
  "notesid": 1,
  "id": 1,
  "weight": 70,
  "height": 175,
  "bmi": 22.9,
  "date": "2025-04-16"
}
```
**传出参数:**
```json
{
  "code": 20000,
  "message": "修改成功",
  "data": null
}
```

### 删除身体记录
**请求地址:** /bodyNotes/{notesid}
**请求方式:** DELETE
**传入参数:**
- notesid: 记录ID (路径参数)
**传出参数:**
```json
{
  "code": 20000,
  "message": "删除成功",
  "data": null
}
```

## 通用响应格式
所有API响应都遵循以下统一格式：

```json
{
  "code": 20000,    // 状态码：20000表示成功，其他值表示失败
  "message": "操作结果描述", // 操作结果的文字描述
  "data": {}        // 响应数据，可能是对象、数组或null
}
```

## 常见状态码
- 20000: 操作成功
- 20002: 用户名或密码错误
- 20003: 登录信息有误，请重新登录
- 20004: 注册失败，用户名已存在

## 认证与授权
- 用户通过 /user/login 接口获取 token
- 后续请求需要在请求头中携带 X-Token 或在查询参数中提供 token
- 退出登录时调用 /user/logout 接口

## 分页查询
支持分页的接口通常需要以下参数：
- pageNo: 当前页码，从1开始
- pageSize: 每页记录数
分页查询结果通常包含：
- total: 总记录数
- rows: 当前页的记录列表

## 注意事项
- 所有请求和响应的Content-Type均为application/json
- 日期格式为ISO 8601标准：YYYY-MM-DD
- 身体质量指数(BMI)计算公式：体重(kg) / 身高(m)²