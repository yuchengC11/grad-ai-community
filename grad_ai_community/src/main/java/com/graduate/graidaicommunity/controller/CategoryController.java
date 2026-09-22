package com.graduate.graidaicommunity.controller;

import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController//接口控制器，返回JSON，不是页面
@RequestMapping("/category")//本类所有接口前缀都是/categroy
public class CategoryController {
    @Resource
    private CategoryService categoryService;
    //获取全部分类列表,GET请求，完整地址/category/list
    @GetMapping("/list")
    public Result<?> getList() {
        return categoryService.getCategoryList();
    }
}