package com.graduate.graidaicommunity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graduate.graidaicommunity.pojo.PostLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostLikeMapper extends BaseMapper<PostLike> {

    @Delete("DELETE FROM post_like WHERE post_id = #{postId}")
    int deleteByPostId(@Param("postId") Long postId);
}