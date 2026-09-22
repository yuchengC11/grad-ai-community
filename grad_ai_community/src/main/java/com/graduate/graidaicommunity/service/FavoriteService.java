package com.graduate.graidaicommunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.pojo.Favorite;

public interface FavoriteService extends IService<Favorite> {
    Result<?> toggleFavorite(Long postId);
    Result<?> getMyFavorites(Integer pageNum, Integer pageSize);
}