package com.graduate.graidaicommunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.pojo.Category;

public interface CategoryService extends IService<Category> {
    Result<?> getCategoryList();
}