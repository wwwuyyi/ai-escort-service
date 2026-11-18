package com.example.escort.module.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    @JsonIgnore
    private String password;
    private String nickname;
    private String avatar;
    private String phone;
    private String role;
    private String status;
    /** JWT令牌版本号：登出/改密/禁用时递增，使旧令牌立即失效 */
    @JsonIgnore
    private Integer tokenVersion;
    private LocalDateTime lastLoginTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}