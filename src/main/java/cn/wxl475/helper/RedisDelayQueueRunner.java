package cn.wxl475.helper;

import cn.hutool.extra.spring.SpringUtil;
import cn.wxl475.pojo.enums.RedisDelayQueueEnum;
import cn.wxl475.utils.RedisDelayQueueHandle;
import cn.wxl475.utils.RedisDelayQueueUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 启动延迟队列
 */
@Slf4j
@Component
public class RedisDelayQueueRunner implements CommandLineRunner {

    @Autowired
    private RedisDelayQueueUtil redisDelayQueueUtil;
    @Autowired
    private ThreadPoolTaskExecutor threadPool;

    @Override
    public void run(String... args) {
        threadPool.execute(() -> {
            while (true){
                try {
                    RedisDelayQueueEnum[] queueEnums = RedisDelayQueueEnum.values();
                    for (RedisDelayQueueEnum queueEnum : queueEnums) {
                        Object value = redisDelayQueueUtil.getDelayQueue(queueEnum.getCode());
                        if (value != null) {
                            RedisDelayQueueHandle redisDelayQueueHandle = SpringUtil.getBean(queueEnum.getBeanId());
                            redisDelayQueueHandle.execute(value);
                        }
                    }
                } catch (InterruptedException e) {
                    log.error("(Redis延迟队列异常中断) {}", e.getMessage());
                }
            }
        });
        log.info("(Redis延迟队列启动成功)");
    }
}
