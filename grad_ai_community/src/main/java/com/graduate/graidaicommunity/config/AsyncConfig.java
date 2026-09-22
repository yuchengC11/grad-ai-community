package com.graduate.graidaicommunity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Configuration标记为配置类，告诉spring这是一个配置类，会在启动时扫描并加载其中@Bean定义；
 * @EnableAsync开启异步支持，启动spring的@Async注解功能，让项目中其他方法可以标记为异步执行
 */
@Configuration
@EnableAsync
//类定义
public class AsyncConfig {
    @Bean("aiExecutor")//定义bean并命名。向 Spring 容器注册一个名为 "aiExecutor" 的线程池 Bean。这个名字很重要，后续 @Async("aiExecutor") 会指定使用这个线程池
    //方法定义，返回 Executor 接口，Spring 会将返回的线程池对象纳入容器管理。
    public Executor aiExecutor() {
        //创建线程池对象
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);          // 核心线程=10，线程池保持的最小线程数，即使没有任务也会保留10个线程存活
        executor.setMaxPoolSize(50);           // 最大线程=50，当核心线程都在忙队列已满时，最多可扩展到50个线程
        executor.setQueueCapacity(100);        // 排队队列=100，当10个核心核心线程都在运行时，新任务会先进入队列排队，最多排100个
        executor.setThreadNamePrefix("ai-stream-");//线程名前缀：创建的线程名字会是 ai-stream-1、ai-stream-2 这样，方便日志排查和监控。
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());//拒绝策略：
        //当线程数达到 50 且队列也满（100个）时，新任务不会丢弃，而是由提交任务的线程（调用者）自己执行。这是一种比较温和的降级策略，避免丢数据。
        // ===== 新增：优雅关闭，防止任务中断导致数据丢失 =====
        executor.setWaitForTasksToCompleteOnShutdown(true);//应用停止时，线程池会等待正在执行的任务完成，而不是强制中断。
        executor.setAwaitTerminationSeconds(60);//最长等待 60 秒：优雅关闭时最多等 60 秒，超时后强制关闭，防止无限阻塞。

        executor.initialize();//初始化线程池：正式创建线程池，分配资源。
        return executor;
    }
}