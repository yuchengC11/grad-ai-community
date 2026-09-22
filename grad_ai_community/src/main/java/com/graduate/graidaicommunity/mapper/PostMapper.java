package com.graduate.graidaicommunity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduate.graidaicommunity.pojo.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    @Update("UPDATE sys_post SET like_count = #{count} WHERE id = #{postId}")
    int updateLikeCount(@Param("postId") Long postId, @Param("count") Long count);

    @Select("SELECT like_count FROM sys_post WHERE id = #{postId}")
    Long selectLikeCount(Long postId);
/**
 * 查询帖子列表，并标记当前用户是否收藏
 */
    @Select("SELECT p.*, " +
        "CASE WHEN f.id IS NOT NULL THEN 1 ELSE 0 END as is_favorited " +
        "FROM sys_post p " +
        "LEFT JOIN sys_favorite f ON p.id = f.post_id AND f.user_id = #{userId} " +
        "ORDER BY p.create_time DESC")
        IPage<Post> selectListWithFavorite(Page<Post> page, @Param("userId") Long userId);

    @Select("SELECT p.*, " +
            "CASE WHEN f.id IS NOT NULL THEN 1 ELSE 0 END as is_favorited " +
            "FROM sys_post p " +
            "LEFT JOIN sys_favorite f ON p.id = f.post_id AND f.user_id = #{userId} " +
            "WHERE p.category_id = #{categoryId} " +
            "ORDER BY p.create_time DESC")
    IPage<Post> selectListByCategoryWithFavorite(Page<Post> page,
                                                 @Param("categoryId") Integer categoryId,
                                                 @Param("userId") Long userId);
}