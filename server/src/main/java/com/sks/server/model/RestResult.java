package com.sks.server.model;

/**
 * 统一响应结果包装
 */
public class RestResult<T> {

    private int code;
    private String message;
    private T data;

    public static <T> RestResult<T> ok(T data) {
        RestResult<T> r = new RestResult<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> RestResult<T> failure(int code, String message) {
        RestResult<T> r = new RestResult<>();
        r.code = code;
        r.message = message;
        r.data = null;
        return r;
    }

    public static <T> RestResult<T> error(String message) {
        return failure(400, message);
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}