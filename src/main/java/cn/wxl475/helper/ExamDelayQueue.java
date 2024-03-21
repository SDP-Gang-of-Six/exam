package cn.wxl475.helper;

import cn.wxl475.mapper.ExamMapper;
import cn.wxl475.utils.AbstractDelayQueueMachineFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 测试延时队列
 *
 */
@Slf4j
@Component
public class ExamDelayQueue extends AbstractDelayQueueMachineFactory {

    @Autowired
    private ExamMapper examMapper;
    /**
     * 处理业务逻辑
     */
    @Override
    public void invoke(String jobId) {

        log.info("延时队列处理业务逻辑,jobId:{}", jobId);

    }

    /**
     * 延时队列名统一设定
     */
    @Override
    public String setDelayQueueName() {
        return "exam_delay_queue";
    }
}
