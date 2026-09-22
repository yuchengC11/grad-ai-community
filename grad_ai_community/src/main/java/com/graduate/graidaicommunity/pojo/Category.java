package com.graduate.graidaicommunity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
/**pojo包用来和数据库表一一映射；
*@Data lombok 自动 get/set，MyBatisPlus 注解用来绑定数据库表、主键等。
*/
@Data
@TableName("category")
public class Category {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    @TableField("sort")
    private Integer sortOrder;  // 映射到数据库 sort 字段
    private Integer status;
    private LocalDateTime createTime;
    private Long parentId;
}