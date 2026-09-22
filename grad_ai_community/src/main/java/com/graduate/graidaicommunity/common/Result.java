package com.graduate.graidaicommunity.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Data：自动生成 get、set、toString、equals、hashCode
 * @NoArgsConstructor：自动生成无参构造方法
 * @AllArgsConstructor：自动生成全参构造方法
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Integer code;
    private String msg;
    private T data;
    /**
     *三个成员变量：
     * code：状态码，200 成功、400 参数错误、500 系统异常、503AI 异常
     * msg：提示信息（给用户看的文字）
     * T data：返回的业务数据，成功时放数据，失败一般为 null
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> fail(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }
}