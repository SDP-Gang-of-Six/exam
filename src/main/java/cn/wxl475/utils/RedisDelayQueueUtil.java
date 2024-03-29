package cn.wxl475.utils;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisDelayQueueUtil {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 添加延迟队列
     *
     * @param value     队列值
     * @param delay     延迟时间
     * @param timeUnit  时间单位
     * @param queueCode 队列键
     * @param <T>
     */
    public <T> void addDelayQueue(T value, long delay, TimeUnit timeUnit, String queueCode) {
        try {
            RBlockingDeque<Object> blockingDeque = redissonClient.getBlockingDeque(queueCode);
            RDelayedQueue<Object> delayedQueue = redissonClient.getDelayedQueue(blockingDeque);
            delayedQueue.offer(value, delay, timeUnit);
            log.info("(添加延时队列成功) 队列键：{}，队列值：{}，延迟时间：{},单位：{}", queueCode, value, delay,timeUnit);
        } catch (Exception e) {
            log.error("(添加延时队列失败) {}", e.getMessage());
            throw new RuntimeException("(添加延时队列失败)");
        }
    }

    /*
        * 删除延迟队列
        * @param queueCode
        * @param value
        * @param <T>
     */
    public <T> void deleteDelayQueue(String queueCode, T value) {
        RBlockingDeque<Object> blockingDeque = redissonClient.getBlockingDeque(queueCode);
        blockingDeque.remove(value);
    }

    /**
     * 获取延迟队列
     *
     * @param queueCode
     * @param <T>
     * @return
     * @throws InterruptedException
     */
    public <T> T getDelayQueue(String queueCode) throws InterruptedException {
        RBlockingDeque<Map> blockingDeque = redissonClient.getBlockingDeque(queueCode);
        T value = (T) blockingDeque.take();
        return value;
    }
}
