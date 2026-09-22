package com.graduate.graidaicommunity.common;

//继承
public class AiException extends RuntimeException {
    //构造方法，异常提示信息
    public AiException(String message) {
        super(message);//调用父类 RuntimeException 的构造方法，把异常信息传给父类
    }

    //构造方法，两个参数：
    //message：给前端 / 日志看的提示文字
    //cause：原始底层异常对象（比如大模型 SDK 抛出的原始 IOException）
    public AiException(String message, Throwable cause) {
        super(message, cause);
    }
}