package cn.wxl475.helper;

import cn.wxl475.mapper.ExamDetailMapper;
import cn.wxl475.mapper.ExamMapper;
import cn.wxl475.mapper.QuestionMapper;
import cn.wxl475.pojo.exam.Exam;
import cn.wxl475.pojo.exam.ExamDetail;
import cn.wxl475.pojo.exam.PaperCreater;
import cn.wxl475.pojo.exam.Question;
import cn.wxl475.redis.CacheClient;
import cn.wxl475.service.PaperService;
import cn.wxl475.utils.AbstractDelayQueueMachineFactory;
import cn.wxl475.utils.ConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static cn.wxl475.redis.RedisConstants.*;

/**
 * 测试延时队列
 *
 */
@Slf4j
@Component
public class ExamDelayQueue extends AbstractDelayQueueMachineFactory {

    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private CacheClient cacheClient;
    @Autowired
    private ExamDetailMapper examDetailMapper;
    @Autowired
    private PaperService paperService;
    @Autowired
    private QuestionMapper questionMapper;

    /**
     * 处理业务逻辑
     */
    @Override
    public void invoke(String jobId) {
        log.info("延时队列处理业务逻辑,jobId:{}", jobId);
        Long examId = Long.parseLong(jobId);
        Exam exam = cacheClient.queryWithPassThrough(
                CACHE_EXAM_KEY,
                LOCK_EXAM_KEY,
                examId,
                Exam.class,
                id -> examMapper.selectById(examId),
                CACHE_EXAM_TTL,
                TimeUnit.MINUTES
        );
        exam.setSubmitTime(LocalDateTime.now());
        if (exam.getSubmitTime().isAfter(exam.getStartTime().plusMinutes(exam.getDuration()+1))) {
            throw new RuntimeException("submitPaper: 超过考试时间");
        }
        exam.setExamScore(0);

        List<ExamDetail> examDetails = cacheClient.queryListWithPassThrough(
                CACHE_EXAMDETAIL_KEY,
                LOCK_EXAMDETAIL_KEY,
                exam.getExamId(),
                ExamDetail.class,
                id -> examDetailMapper.selectByExamId(exam.getExamId()),
                CACHE_EXAMDETAIL_TTL,
                TimeUnit.MINUTES
        );
        // 获取试卷信息
        PaperCreater paperCreater = paperService.getPaperDetailById(exam.getPaperId());
        HashMap<Long,Integer> ScoreMap = ConvertUtil.convertPaperCreaterToMap(paperCreater);

        // 计算分数
        for (ExamDetail examDetail : examDetails) {
            // 获取题目信息
            Question question = cacheClient.queryWithPassThrough(
                    CACHE_QUESTION_KEY,
                    LOCK_QUESTION_KEY,
                    examDetail.getQuestionId(),
                    Question.class,
                    id -> questionMapper.selectById(examDetail.getQuestionId()),
                    CACHE_QUESTION_TTL,
                    TimeUnit.MINUTES
            );
            // 判断答案是否正确
            switch (question.getQuestionType()) {
                case option:
                    if (examDetail.getOption() == question.getRightOption()) {
                        examDetail.setRight(true);
                    } else {
                        examDetail.setRight(false);
                    }
                    break;
                case judge:
                    if (examDetail.isJudge() == question.isRightJudge()) {
                        examDetail.setRight(true);
                    } else {
                        examDetail.setRight(false);
                    }
                    break;
                case blank:
                    if (examDetail.getBlank().equals(question.getRightBlank())) {
                        examDetail.setRight(true);
                    } else {
                        examDetail.setRight(false);
                    }
                    break;
            }
            // 计算分数
            if (examDetail.isRight()) {
                exam.setExamScore(exam.getExamScore() + ScoreMap.get(examDetail.getQuestionId()));
            }
        }
        // 更新Exam和ExamDetail
        exam.setStatus(true);  //Ture表示已经提交
        Duration duration = Duration.between(exam.getStartTime(), exam.getSubmitTime());
        exam.setDuration(duration.toMinutesPart());
        examMapper.updateById(exam);
        cacheClient.resetKey(
                CACHE_EXAM_KEY,
                LOCK_EXAM_KEY,
                exam.getExamId(),
                Exam.class,
                id -> examMapper.selectById(exam.getExamId()),
                CACHE_EXAM_TTL,
                TimeUnit.MINUTES
        );
        List<Long> examIds = new ArrayList<>();
        examIds.add(exam.getExamId());
        examDetailMapper.deleteByExamIds(examIds);
        for (ExamDetail examDetail : examDetails) {
            examDetailMapper.insert(examDetail);
        }
        cacheClient.resetKey(
                CACHE_EXAMDETAIL_KEY,
                LOCK_EXAMDETAIL_KEY,
                exam.getExamId(),
                List.class,
                id -> examDetailMapper.selectByExamId(exam.getExamId()),
                CACHE_EXAMDETAIL_TTL,
                TimeUnit.MINUTES
        );
    }

    /**
     * 延时队列名统一设定
     */
    @Override
    public String setDelayQueueName() {
        return "exam_delay_queue";
    }
}
