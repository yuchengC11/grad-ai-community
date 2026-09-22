package com.graduate.graidaicommunity.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PostVO {
    private Long id;
    private String title;
    private String content;
    private Integer categoryId;
    private Long userId;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String nickname;
    private String avatar;
}