package com.graduate.graidaicommunity.controller;

import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.pojo.Post;
import com.graduate.graidaicommunity.service.PostService;
import com.graduate.graidaicommunity.vo.PostPublishDTO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@RequestMapping("/post")
public class PostController {
    @Resource
    private PostService postService;
//帖子列表（支持分类筛选、分页）
    @GetMapping("/list")
    public Result<?> getList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long parentCategoryId,
            @RequestParam(required = false) Long categoryId) {
        return postService.getPostList(pageNum, pageSize, parentCategoryId, categoryId);
    }
//帖子详情
    @GetMapping("/{id}")
    public Result<?> getDetail(@PathVariable Long id) {
        return postService.getPostDetail(id);
    }
//新增帖子
    @PostMapping("/add")
    public Result<?> addPost(@Valid @RequestBody PostPublishDTO dto) {
        Post post = new Post();
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setCategoryId(dto.getCategoryId());
        return postService.addPost(post);
    }
//点赞切换
    @PostMapping("/like/toggle")
    public Result<?> toggleLike(@RequestParam Long postId) {
        return postService.toggleLike(postId);
    }
//查询是否点赞
    @GetMapping("/like/status")
    public Result<?> getLikeStatus(@RequestParam Long postId) {
        return postService.getLikeStatus(postId);
    }
//我点赞过的帖子列表
    @GetMapping("/liked/list")
    public Result<?> getLikedList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return postService.getMyLikedPosts(pageNum, pageSize);
    }
//帖子搜索
    @GetMapping("/search")
    public Result<?> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return postService.searchPosts(keyword, pageNum, pageSize);
    }
//查询某个用户发布的帖子
    @GetMapping("/user/{userId}")
    public Result<?> getPostsByUser(@PathVariable Long userId,
                                    @RequestParam(defaultValue = "1") Integer pageNum,
                                    @RequestParam(defaultValue = "10") Integer pageSize) {
        return postService.getPostsByUser(userId, pageNum, pageSize);
    }
//我的帖子
    @GetMapping("/my")
    public Result<?> getMyPosts(@RequestParam(defaultValue = "1") Integer pageNum,
                                @RequestParam(defaultValue = "10") Integer pageSize) {
        return postService.getMyPosts(pageNum, pageSize);
    }
    //删除帖子
    @DeleteMapping("/{postId}")
    public Result<?> deletePost(@PathVariable Long postId) {
        return postService.deletePost(postId);
    }
}