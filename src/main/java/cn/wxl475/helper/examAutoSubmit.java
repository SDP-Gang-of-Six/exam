package cn.wxl475.helper;

import cn.wxl475.exception.ExamSolveException;
import cn.wxl475.mapper.ExamDetailMapper;
import cn.wxl475.mapper.ExamMapper;
import cn.wxl475.mapper.QuestionMapper;
import cn.wxl475.pojo.exam.Exam;
import cn.wxl475.pojo.exam.ExamDetail;
import cn.wxl475.pojo.exam.PaperCreater;
import cn.wxl475.pojo.exam.Question;
import cn.wxl475.redis.CacheClient;
import cn.wxl475.service.PaperService;
import cn.wxl475.utils.ConvertUtil;
import cn.wxl475.utils.RedisDelayQueueHandle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static cn.wxl475.redis.RedisConstants.*;

@Component
@Slf4j
public class examAutoSubmit implements RedisDelayQueueHandle<Long> {


    private final ExamMapper examMapper;
    private final CacheClient cacheClient;
    private final ExamDetailMapper examDetailMapper;
    private final PaperService paperService;
    private final QuestionMapper questionMapper;
    @Autowired
    public examAutoSubmit(ExamMapper examMapper, CacheClient cacheClient, ExamDetailMapper examDetailMapper, PaperService paperService, QuestionMapper questionMapper) {
        this.examMapper = examMapper;
        this.cacheClient = cacheClient;
        this.examDetailMapper = examDetailMapper;
        this.paperService = paperService;
        this.questionMapper = questionMapper;
    }


    @Override
    public void execute(Long jobId) {
        log.info("(收到试卷提交延迟消息) {}", jobId);
        Exam exam = cacheClient.queryWithPassThrough(
                CACHE_EXAM_KEY,
                LOCK_EXAM_KEY,
                jobId,
                Exam.class,
                id -> examMapper.selectById(jobId),
                CACHE_EXAM_TTL,
                TimeUnit.MINUTES
        );
        if (exam.isStatus()) {
            throw new ExamSolveException("submitPaper: 试卷已经提交");
        }

        // 获取试卷信息
        PaperCreater paperCreater = paperService.getPaperDetailById(exam.getPaperId());
        HashMap<Long,Integer> ScoreMap = ConvertUtil.convertPaperCreaterToMap(paperCreater);

        exam.setSubmitTime(LocalDateTime.now());
        if (exam.getSubmitTime().isAfter(exam.getStartTime().plusMinutes(paperCreater.getExamTime()+2))) {
            throw new ExamSolveException("submitPaper: 超过考试时间");
        }
        exam.setExamScore(0);

        // 获取考试详情
        List<ExamDetail> examDetails = cacheClient.queryListWithPassThrough(
                CACHE_EXAM_DETAIL_KEY,
                LOCK_EXAM_DETAIL_KEY,
                exam.getExamId(),
                ExamDetail.class,
                id -> examDetailMapper.selectByExamId(exam.getExamId()),
                CACHE_EXAM_DETAIL_TTL,
                TimeUnit.MINUTES
        );

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
                    examDetail.setRight(examDetail.getYourOption() == question.getRightOption());
                    break;
                case judge:
                    examDetail.setRight(examDetail.getJudge() == question.isRightJudge());
                    break;
                case blank:
                    examDetail.setRight(examDetail.getBlank().equals(question.getRightBlank()));
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
        Long[] examIds = new Long[1];
        examIds[0] = exam.getExamId();
        examDetailMapper.deleteByExamIds(examIds);
        for (ExamDetail examDetail : examDetails) {
            examDetailMapper.insert(examDetail);
        }
        cacheClient.resetKey(
                CACHE_EXAM_DETAIL_KEY,
                LOCK_EXAM_DETAIL_KEY,
                exam.getExamId(),
                List.class,
                id -> examDetailMapper.selectByExamId(exam.getExamId()),
                CACHE_EXAM_DETAIL_TTL,
                TimeUnit.MINUTES
        );
    }
}
