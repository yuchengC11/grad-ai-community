package com.graduate.graidaicommunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.common.UserContext;
import com.graduate.graidaicommunity.mapper.FriendMapper;
import com.graduate.graidaicommunity.pojo.Friend;
import com.graduate.graidaicommunity.pojo.SysUser;
import com.graduate.graidaicommunity.service.FriendService;
import com.graduate.graidaicommunity.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FriendServiceImpl extends ServiceImpl<FriendMapper, Friend> implements FriendService {
    @Resource
    private UserService userService;
    @Transactional
    @Override
    public Result<?> addFriend(Long toUserId) {
        Long fromUserId = UserContext.getUserId();
        System.out.println("=== fromUserId=" + fromUserId + ", toUserId=" + toUserId);
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, fromUserId)
                .eq(Friend::getFriendId, toUserId);
        Friend exist = this.getOne(wrapper);
        System.out.println("=== exist=" + (exist == null ? "null" : exist.getStatus()));

        if (exist != null) {
            int status = exist.getStatus();
            if (status == 1) {
                return Result.fail(400, "你们已经是好友了");
            } else if (status == 0) {
                return Result.fail(400, "已发送申请，请等待对方确认");
            } else if (status == 2) {
                LocalDateTime canRetry = exist.getUpdateTime().plusDays(7);
                if (canRetry.isAfter(LocalDateTime.now())) {
                    long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), canRetry) + 1;
                    return Result.fail(400, "对方已拒绝，" + daysLeft + "天后可再次申请");
                }
                this.removeById(exist.getId());
            }
        }

        Friend friend = new Friend();
        friend.setUserId(fromUserId);
        friend.setFriendId(toUserId);
        friend.setStatus(0);
        this.save(friend);
        return Result.success("好友申请已发送");
    }
    @Transactional
    @Override
    public void deleteFriend(Long friendId) {
        Long userId = UserContext.getUserId();

        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId)
                .or(w -> w.eq(Friend::getUserId, friendId).eq(Friend::getFriendId, userId));
        this.remove(wrapper);
    }

    @Override
    @Transactional
    public Result<?> acceptFriend(Long friendId) {
        Long userId = UserContext.getUserId();
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, friendId)
                .eq(Friend::getFriendId, userId)
                .eq(Friend::getStatus, 0);
        Friend f = this.getOne(wrapper);
        if (f == null) {
            return Result.fail(404, "未找到好友请求");
        }
        f.setStatus(1);
        this.updateById(f);
        return Result.success("已接受好友请求");
    }

    @Override
    @Transactional
    public Result<?> rejectFriend(Long friendId) {
        Long userId = UserContext.getUserId();
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, friendId)
                .eq(Friend::getFriendId, userId)
                .eq(Friend::getStatus, 0);
        Friend f = this.getOne(wrapper);
        if (f == null) {
            return Result.fail(404, "未找到好友请求");
        }
        f.setStatus(2);
        this.updateById(f);
        return Result.success("已拒绝好友请求");
    }

    @Override
    public Result<?> getFriendList() {
        Long userId = UserContext.getUserId();
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, userId)
                .eq(Friend::getStatus, 1)
                .or()
                .eq(Friend::getFriendId, userId)
                .eq(Friend::getStatus, 1);
        List<Friend> friends = this.list(wrapper);
        List<Long> friendIds = friends.stream()
                .map(f -> f.getUserId().equals(userId) ? f.getFriendId() : f.getUserId())
                .collect(Collectors.toList());
        if (friendIds.isEmpty()) {
            return Result.success(friendIds);
        }
        List<SysUser> users = userService.listByIds(friendIds);
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }

    @Override
    public Result<?> getPendingRequests() {
        Long userId = UserContext.getUserId();
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getFriendId, userId)
                .eq(Friend::getStatus, 0);
        List<Friend> requests = this.list(wrapper);
        List<Long> userIds = requests.stream()
                .map(Friend::getUserId)
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return Result.success(requests);
        }
        List<SysUser> users = userService.listByIds(userIds);
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }
}