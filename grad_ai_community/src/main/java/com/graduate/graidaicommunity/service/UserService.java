package com.graduate.graidaicommunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.pojo.SysUser;
import com.graduate.graidaicommunity.vo.LoginDTO;
import com.graduate.graidaicommunity.vo.RegisterDTO;

import java.util.Map;

public interface UserService extends IService<SysUser> {
    Result<?> register(RegisterDTO dto);
    Result<Map<String, Object>> login(LoginDTO dto);
    SysUser getUserById(Long id);
}