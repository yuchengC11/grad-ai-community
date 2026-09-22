package com.graduate.graidaicommunity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graduate.graidaicommunity.pojo.SysMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<SysMessage> {
}