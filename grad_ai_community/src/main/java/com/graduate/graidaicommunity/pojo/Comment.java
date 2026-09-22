package com.graduate.graidaicommunity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private Long userId;
    private String content;
    private LocalDateTime createTime;
    @TableField(exist = false)//这个字段在数据库表里面不存在，只是业务临时使用，MyBatisPlus 不要去数据库映射这个列
    private String nickname;
    private Long parentId;
}