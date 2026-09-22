package com.graduate.graidaicommunity.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//MybatisPlus分页插件配置
@Configuration
public class MybatisPlusConfig {
   // 注册 Bean：不指定名称，默认方法名 mybatisPlusInterceptor 作为 Bean 名。
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();//创建总拦截器：这是 MP 3.5+ 版本的插件注册方式，所有插件都往这里加。
        /**
         * PaginationInnerInterceptor：自动拦截带有分页参数的查询
         * DbType.MYSQL：告诉插件按 MySQL 方言生成 LIMIT 语句
         * 这样你在 Service 层调用 Page 对象时，MP 会自动拼接分页 SQL 并查询总记录数
         */
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;//返回拦截器：Spring 将其注入到 Mybatis 的插件链中。

    }
}