package com.graduate.graidaicommunity.controller;

import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.service.CommentService;
import com.graduate.graidaicommunity.vo.CommentAddDTO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@RequestMapping("/comment")
public class CommentController {
    @Resource//Spring 自动注入 CategoryService 对象（业务层）
    private CommentService commentService;

      //发布评论
    @PostMapping("/add")
    public Result<?> add(@Valid @RequestBody CommentAddDTO dto) {
        return commentService.addComment(dto);
    }

    //获取指定帖子的全部评论
    @GetMapping("/list")
    public Result<?> list(@RequestParam Long postId) {
        return commentService.getCommentList(postId);
    }
}