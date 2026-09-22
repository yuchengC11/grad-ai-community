package com.graduate.graidaicommunity.common;

import com.graduate.graidaicommunity.interceptor.JwtInterceptor;

/**
 * 用户上下文，基于ThreadLocal，同一个请求线程内获取登录用户id
 */
public class UserContext {
    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
//ThreadLocal 可以理解成线程独立的容器：每个线程存的数据互相隔离
//尖括号Long：容器里面存的是用户 id（Long 类型）
//USER_ID_HOLDER：变量名，存放 userId 的容器
  //  重点：A 线程 set 的值，只有 A 线程能 get 到；B 线程看不到 A 线程的数据
    private UserContext(){}//私有构造方法，不让外部创建对象，

    public static void setUserId(Long userId){
        USER_ID_HOLDER.set(userId);
    }//静态方法：往当前线程的 ThreadLocal 里存入 userId👉 使用时机：JwtInterceptor 拦截器解析 token 成功后调用

    public static Long getUserId(){
        return USER_ID_HOLDER.get();
    }//静态方法：取出当前线程里保存的 userId

    public static void clear(){
        // 必须remove，防止tomcat线程池复用线程造成串号、内存泄漏
        USER_ID_HOLDER.remove();
    }
    /**
     * 重中之重（面试高频考点）
     *     Tomcat 使用线程池复用线程，请求处理完线程不会销毁，放回线程池。
     *     如果不执行remove()：
     *     下一个新请求复用这个旧线程时，get()还能拿到上一个用户的 userId → 用户串号 bug；
     *     另外 ThreadLocal 如果一直不清理，还会引发内存泄漏。
     *     👉 调用时机：JwtInterceptor 的finally里面一定要执行 clear ()，保证不管正常 / 异常结束都清理。
     */

}
