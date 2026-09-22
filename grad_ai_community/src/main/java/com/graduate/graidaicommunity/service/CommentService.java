package com.graduate.graidaicommunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.pojo.Comment;
import com.graduate.graidaicommunity.vo.CommentAddDTO;

public interface CommentService extends IService<Comment> {
    Result<?> addComment(CommentAddDTO dto);
    Result<?> getCommentList(Long postId);
}