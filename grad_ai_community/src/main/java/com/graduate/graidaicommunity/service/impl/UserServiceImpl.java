package com.graduate.graidaicommunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.mapper.SysUserMapper;
import com.graduate.graidaicommunity.pojo.SysUser;
import com.graduate.graidaicommunity.service.UserService;
import com.graduate.graidaicommunity.util.JwtUtil;
import com.graduate.graidaicommunity.util.PasswordUtil;
import com.graduate.graidaicommunity.vo.LoginDTO;
import com.graduate.graidaicommunity.vo.RegisterDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserService {

    @Resource
    private PasswordUtil passwordUtil;

    @Resource
    private JwtUtil jwtUtil;
    @Override
    public Result<?> register(RegisterDTO dto) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, dto.getUsername());
        SysUser existUser = this.getOne(queryWrapper);
        if (existUser != null) {
            return Result.fail(400, "该账号已被注册");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());   //账号

        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setPassword(passwordUtil.encrypt(dto.getPassword()));//BCrypt加密
        user.setInterestTag(dto.getInterestTag() != null ? dto.getInterestTag() : 1);

//入库
        boolean saveOk = this.save(user);
        if (!saveOk) {
            return Result.fail(500, "注册保存失败");
        }
        return Result.success("注册成功");

    }

    @Override
    public Result<Map<String, Object>> login(LoginDTO dto) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, dto.getUsername());
        SysUser user = this.getOne(queryWrapper);
        if (user == null) {
            return Result.fail(400, "账号不存在");
        }

        if (!passwordUtil.match(dto.getPassword(), user.getPassword())) {
            return Result.fail(400, "密码错误");
        }

        String token = jwtUtil.generateToken(user.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        user.setPassword(null);
        data.put("userInfo", user);
        return Result.success(data);
    }
    @Override
    public SysUser getUserById(Long id) {
        return getById(id);
    }
}