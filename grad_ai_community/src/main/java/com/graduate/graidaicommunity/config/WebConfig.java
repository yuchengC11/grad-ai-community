package com.graduate.graidaicommunity.config;

import com.graduate.graidaicommunity.interceptor.JwtInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import java.io.File;

/**
 * JwtInterceptor：自定义的 JWT 鉴权拦截器
 * @Slf4j：Lombok 日志注解，自动生成 log 变量
 * @Value：注入配置文件中的属性值
 * WebMvcConfigurer：SpringMVC 配置接口，实现它可以自定义 MVC 行为
 */
@Slf4j
//SpringMVC 主配置，注册拦截器、真正生效跨域、本地文件静态资源映射
@Configuration
//实现 WebMvcConfigurer：通过重写接口方法来自定义 SpringMVC 的拦截器、跨域、静态资源等行为。
public class WebConfig implements WebMvcConfigurer {

    // 使用新JwtInterceptor鉴权拦截器
    //注入 JWT 拦截器：按名称注入自定义的鉴权拦截器，用于拦截请求校验 Token。
    @Resource
    private JwtInterceptor jwtInterceptor;
//注入配置项：从 application.yml/properties 中读取 upload.local-path 的值，即本地上传文件的存储根目录。
    @Value("${upload.local-path}")
    private String uploadLocalPath;

    // 注册拦截器，重写此方法配置哪些请求需要被拦截。
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)//添加 JWT 拦截器：把自定义拦截器加入拦截器链。
                .addPathPatterns("/**")// 拦截所有路径
                .excludePathPatterns(
                        "/user/login",
                        "/user/register",
                        "/error",
                        "/uploads/**",
                        // 静态资源放行：前端页面本身不需要登录，数据接口才需要鉴权
                        "/",
                        "/index.html",
                        "/static/**",
                        "/*.html",
                        "/*.js",
                        "/*.css",
                        "/*.ico",
                        "/*.png",
                        "/*.jpg",
                        "/*.jpeg",
                        "/*.gif",
                        "/*.svg",
                        "/*.woff",
                        "/*.woff2",
                        "/*.ttf",
                        "/favicon.ico"
                );//排除特定路径
    }

    // 全局跨域配置（配置跨域，解决前端（如 Vue/React）访问后端时的浏览器跨域限制）
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")//所有接口允许跨域
                .allowedOriginPatterns("http://localhost:8080","https://your-domaiin.com")//允许所有来源：* 表示不限制前端域名。生产环境建议指定具体域名。
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")//允许的 HTTP 方法：涵盖常见的 RESTful 操作，OPTIONS 是浏览器预检请求用的。
                .allowCredentials(true)//允许携带凭证：前端请求可以携带 Cookie、Authorization Header 等身份凭证。
                .maxAge(3600);//预检缓存时间：浏览器对同一跨域请求的预检（OPTIONS）结果缓存 3600 秒（1小时），减少重复预检。
    }

    //配置静态资源映射：让浏览器可以通过 HTTP 直接访问服务器本地的上传文件。
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File file = new File(uploadLocalPath);
        String absolutePath = file.getAbsolutePath();
        //获取绝对路径：将配置文件中的相对路径转为绝对路径，避免路径解析错误。
        registry.addResourceHandler("/uploads/**")
                //URL 映射规则：当浏览器访问 /uploads/xxx.jpg 时，Spring 会映射到本地文件系统。
                .addResourceLocations("file:" + absolutePath + File.separator);
        /**
         * 映射到本地磁盘：
         * file: 前缀表示这是本地文件系统路径，不是 classpath
         * 例如：/uploads/avatar.jpg → D:/uploads/avatar.jpg
         * 这样前端可以直接用 <img src="/uploads/xxx.jpg"> 显示用户上传的图片
         */
        log.info("上传文件映射物理路径：{}", absolutePath);
    }
}
