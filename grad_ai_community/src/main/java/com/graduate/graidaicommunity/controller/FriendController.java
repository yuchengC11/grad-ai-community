package com.graduate.graidaicommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.common.UserContext;
import com.graduate.graidaicommunity.pojo.Friend;
import com.graduate.graidaicommunity.service.FriendService;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;

@RestController
@RequestMapping("/friend")
public class FriendController {
    @Resource
    private FriendService friendService;
    @DeleteMapping("/{friendId}")
    //@PathVariable路径变量
    public Result deleteFriend(@PathVariable Long friendId) {
        friendService.deleteFriend(friendId);
        return Result.success("删除成功");
    }

    @PostMapping("/accept")
    public Result<?> acceptFriend(@RequestParam Long friendId) {
        return friendService.acceptFriend(friendId);
    }

    @PostMapping("/reject")
    public Result<?> rejectFriend(@RequestParam Long friendId) {
        return friendService.rejectFriend(friendId);
    }

    @GetMapping("/list")
    public Result<?> getFriendList() {
        return friendService.getFriendList();
    }

    @GetMapping("/requests")
    public Result<?> getPendingRequests() {
        return friendService.getPendingRequests();
    }
    @GetMapping("/status")
    public Result<String> checkStatus(@RequestParam Long friendId) {
        Long userId = UserContext.getUserId();
        //LambdaQueryWrapper:MyBatis-Plus动态条件查询
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId);
        Friend f = friendService.getOne(wrapper);
        if (f != null) {
            if (f.getStatus() == 1) {
                return Result.success("accepted");
            } else if (f.getStatus() == 0) {
                return Result.success("pending");//待处理
            } else {
                return Result.success("rejected");//拒绝
            }
        }
        //反向查：对方发给我的申请
        LambdaQueryWrapper<Friend> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.eq(Friend::getUserId, friendId)
                .eq(Friend::getFriendId, userId)
                .eq(Friend::getStatus, 0);
        if (friendService.count(wrapper2) > 0) {
            return Result.success("pending");
        }
        return Result.success("none");
    }
    //添加好友申请
    @PostMapping("/add")
    public Result addFriend(@RequestParam Long friendId) {
        return friendService.addFriend(friendId);
    }
}