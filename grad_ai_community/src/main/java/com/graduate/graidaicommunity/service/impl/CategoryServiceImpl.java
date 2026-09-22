package com.graduate.graidaicommunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.mapper.CategoryMapper;
import com.graduate.graidaicommunity.pojo.Category;
import com.graduate.graidaicommunity.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public Result<?> getCategoryList() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Category::getSortOrder);
        List<Category> list = this.list(wrapper);
        return Result.success(list);
    }
}