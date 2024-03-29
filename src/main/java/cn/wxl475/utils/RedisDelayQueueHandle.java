package cn.wxl475.utils;

/**
 * 延迟队列执行器
 */
public interface RedisDelayQueueHandle<T> {

    void execute(T t);

}