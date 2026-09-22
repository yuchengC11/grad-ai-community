package com.graduate.graidaicommunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.pojo.Post;

public interface PostService extends IService<Post> {

    Result<?> getPostList(Integer pageNum, Integer pageSize, Long parentCategoryId, Long categoryId);

    Result<?> getPostDetail(Long id);

    Result<?> addPost(Post post);

    Result<?> toggleLike(Long postId);

    Result<?> getLikeStatus(Long postId);

    Result<?> getMyLikedPosts(Integer pageNum, Integer pageSize);

    Result<?> searchPosts(String keyword, Integer pageNum, Integer pageSize);

    Result<?> getPostsByUser(Long userId, Integer pageNum, Integer pageSize);

    Result<?> getMyPosts(Integer pageNum, Integer pageSize);
    Result<?> deletePost(Long postId);
}