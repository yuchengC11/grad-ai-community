package com.graduate.graidaicommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.common.UserContext;
import com.graduate.graidaicommunity.pojo.SysUser;
import com.graduate.graidaicommunity.service.UserService;
import com.graduate.graidaicommunity.vo.LoginDTO;
import com.graduate.graidaicommunity.vo.RegisterDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;
    @Value("${upload.local-path}")  
    private String uploadLocalPath;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginDTO dto) {
        return userService.login(dto);
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody RegisterDTO dto) {
        return userService.register(dto);
    }

    @GetMapping("/info")
    public Result<SysUser> getUserInfo() {
        Long userId = UserContext.getUserId();
        SysUser user = userService.getById(userId);
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }
        user.setPassword(null);//请求结束清理
        return Result.success(user);
    }

    @GetMapping("/test-ctx")
    public Result<Long> testCurrentUserId() {
        return Result.success(UserContext.getUserId());
    }

    @PostMapping("/update")
    public Result<?> updateInfo(@RequestBody SysUser user) {
        Long userId = UserContext.getUserId();
        // 白名单更新：只接受允许修改的字段，防止前端传入敏感字段
        SysUser update = new SysUser();
        update.setId(userId);
        update.setNickname(user.getNickname());
        update.setAvatar(user.getAvatar());
        update.setSchool(user.getSchool());
        update.setMajor(user.getMajor());
        update.setInterestTag(user.getInterestTag());
        userService.updateById(update);
        return Result.success("修改成功");
    }

    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("avatar") MultipartFile file) {
        Long userId = UserContext.getUserId();

        if (file.getSize() > 2 * 1024 * 1024) {
            return Result.fail(400, "图片大小不能超过2MB");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isEmpty()) {
            return Result.fail(400, "文件名不能为空");
        }
        int dot = originalName.lastIndexOf(".");
        if (dot == -1 || dot == originalName.length() - 1) {
            return Result.fail(400, "文件缺少扩展名");
        }
        String ext = originalName.substring(dot + 1).toLowerCase();
        List<String> allowed = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
        if (!allowed.contains(ext)) {
            return Result.fail(400, "仅支持jpg/png/gif/webp格式");
        }

        String fileName = UUID.randomUUID().toString() + "." + ext;
        try {
            String uploadPath = uploadLocalPath + File.separator + "avatars" + File.separator;
            File dir = new File(uploadPath);
            if (!dir.exists()) dir.mkdirs();
            File dest = new File(uploadPath + fileName);
            file.transferTo(dest);

            String avatarUrl = "/uploads/avatars/" + fileName;
            SysUser user = userService.getById(userId);
            if (user == null) {
                return Result.fail(404, "用户不存在");
            }
            user.setAvatar(avatarUrl);
            userService.updateById(user);
            return Result.success(avatarUrl);
        } catch (IOException e) {
            return Result.fail(500, "头像上传失败");
        }
    }

    @GetMapping("/search")
    public Result<List<SysUser>> searchUsers(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.fail(400, "搜索关键词不能为空");
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(SysUser::getNickname, keyword)
                .or()
                .like(SysUser::getUsername, keyword);
        wrapper.last("LIMIT 20");
        List<SysUser> users = userService.list(wrapper);
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }

    @GetMapping("/{id:[0-9]+}")
    public Result<SysUser> getUserById(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user == null) return Result.fail(404, "用户不存在");
        user.setPassword(null);
        return Result.success(user);
    }
}