package com.hrms.common.exception;

/**
 * 业务异常基类
 * 抛出后由 GlobalExceptionHandler 统一捕获并返回 Result
 */
public class BaseException extends RuntimeException {

    /** 业务状态码 */
    private final int code;

    public BaseException(String message) {
        super(message);
        this.code = 0;
    }

    public BaseException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    // ── 便捷工厂方法 ──

    /** 未认证 */
    public static BaseException unauthorized(String msg) {
        return new BaseException(401, msg);
    }

    /** 无权限 */
    public static BaseException forbidden(String msg) {
        return new BaseException(403, msg);
    }

    /** 资源不存在 */
    public static BaseException notFound(String msg) {
        return new BaseException(404, msg);
    }

    /** 参数校验失败 */
    public static BaseException badRequest(String msg) {
        return new BaseException(400, msg);
    }

}
