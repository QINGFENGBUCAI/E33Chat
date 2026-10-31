package com.niuqu.chatbubble;

// 统一日志封装
public final class E33Log {
    private static final org.apache.logging.log4j.Logger LOGGER =
        org.apache.logging.log4j.LogManager.getLogger("e33chat");

    // 禁止外部实例化
    private E33Log() {}

    // 输出信息日志
    public static void info(String message) {
        LOGGER.info(message);
    }

    // 输出格式化信息
    public static void info(String message, Object... args) {
        LOGGER.info(message, args);
    }

    // 输出警告日志
    public static void warn(String message) {
        LOGGER.warn(message);
    }

    // 输出警告与异常
    public static void warn(String message, Throwable t) {
        LOGGER.warn(message, t);
    }

    // 输出格式化警告
    public static void warn(String message, Object... args) {
        LOGGER.warn(message, args);
    }

    // 输出错误与异常
    public static void error(String message, Throwable t) {
        LOGGER.error(message, t);
    }

    // 输出格式化错误
    public static void error(String message, Object... args) {
        LOGGER.error(message, args);
    }

    // 输出调试日志
    public static void debug(String message, Object... args) {
        LOGGER.debug(message, args);
    }
}
