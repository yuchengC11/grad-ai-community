package com.graduate.graidaicommunity.controller;

import com.graduate.graidaicommunity.common.Result;
import com.graduate.graidaicommunity.service.FavoriteService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/favorite")
public class FavoriteController {

    @Resource
    private FavoriteService favoriteService;

    @PostMapping("/toggle")
    public Result<?> toggle(@RequestParam Long postId) {
        return favoriteService.toggleFavorite(postId);
    }

    @GetMapping("/list")
    //pageNum默认第1页，pageSize默认10条
    public Result<?> list(@RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "10") Integer pageSize) {
        return favoriteService.getMyFavorites(pageNum, pageSize);
    }
}