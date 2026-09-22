package com.graduate.graidaicommunity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graduate.graidaicommunity.pojo.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * 检查用户是否收藏了某帖子
     * 返回 int：>0 → 已经收藏；=0 → 未收藏
     *
     * @Param("userId")：把方法参数命名，和 sql 里#{userId}对应
     */
    @Select("SELECT COUNT(*) FROM sys_favorite WHERE user_id = #{userId} AND post_id = #{postId}")
    int checkFavorite(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * 批量查询用户已收藏的帖子ID列表
     */
    @Select("<script>" +
            "SELECT post_id FROM sys_favorite " +
            "WHERE user_id = #{userId} " +
            "<if test='postIds != null and postIds.size() > 0'>" +
            "AND post_id IN " +
            "<foreach collection='postIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</if>" +
            "<if test='postIds == null or postIds.size() == 0'>" +
            "AND 1=0" +
            "</if>" +
            "</script>")
    List<Long> selectFavoritedPostIds(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}