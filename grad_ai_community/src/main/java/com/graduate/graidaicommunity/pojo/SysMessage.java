package com.graduate.graidaicommunity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_message")
public class SysMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long fromUserId;
    private String fromNickname;
    private String content;
    private Integer type;
    private Integer isRead;
    private LocalDateTime createTime;
}