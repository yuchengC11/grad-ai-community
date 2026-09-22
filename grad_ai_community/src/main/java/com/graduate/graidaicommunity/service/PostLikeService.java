package com.graduate.graidaicommunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.pojo.PostLike;

public interface PostLikeService extends IService<PostLike> {
    Result<?> toggleLike(Long postId);
}