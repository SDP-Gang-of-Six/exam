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


/**
 * 启动延迟队列
 */
@Slf4j
@Component
public class RedisDelayQueueRunner implements CommandLineRunner {

    private final RedisDelayQueueUtil redisDelayQueueUtil;
    private final ThreadPoolTaskExecutor threadPool;
    @Autowired
    public RedisDelayQueueRunner(RedisDelayQueueUtil redisDelayQueueUtil, ThreadPoolTaskExecutor threadPool) {
        this.redisDelayQueueUtil = redisDelayQueueUtil;
        this.threadPool = threadPool;
    }

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
                            try {
                                redisDelayQueueHandle.execute(value);
                            }catch (Exception e) {
                                log.error("(Redis延迟队列执行异常) {}", e.getMessage());
                            }
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
