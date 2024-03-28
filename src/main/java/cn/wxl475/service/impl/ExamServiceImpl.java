package cn.wxl475.service.impl;

import cn.wxl475.mapper.ExamDetailMapper;
import cn.wxl475.mapper.ExamMapper;
import cn.wxl475.mapper.QuestionMapper;
import cn.wxl475.pojo.exam.*;
import cn.wxl475.redis.CacheClient;
import cn.wxl475.service.ExamService;
import cn.wxl475.helper.ExamDelayQueue;
import cn.wxl475.service.PaperService;
import cn.wxl475.utils.ConvertUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static cn.wxl475.redis.RedisConstants.*;

@Service
public class ExamServiceImpl implements ExamService {

    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private ExamDetailMapper examDetailMapper;
    @Autowired
    private ExamDelayQueue examDelayQueue;
    @Autowired
    private CacheClient cacheClient;
    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private PaperService paperService;

    @Override
    public Long startExam(Exam exam) {
        Exam exam1 = cacheClient.queryWithPassThrough(
                CACHE_EXAM_KEY,
                LOCK_EXAM_KEY,
                exam.getExamId(),
                Exam.class,
                id -> examMapper.selectById(exam.getExamId()),
                CACHE_EXAM_TTL,
                TimeUnit.MINUTES
        );
        if (exam1.getStartTime() != null) {
            throw new RuntimeException("startExam: 考试已经开始,请勿重复开始考试");
        }
        if (LocalDateTime.now().isAfter(exam1.getAllowEndTime()) || LocalDateTime.now().isBefore(exam1.getAllowStartTime())) {
            throw new RuntimeException("startExam: 不在考试时间内");
        }
        exam1.setStartTime(LocalDateTime.now());
        boolean result = examDelayQueue.addJob(exam.getExamId().toString(), exam1.getDuration() * 60000 + 30000);
        if (!result) {
            throw new RuntimeException("startExam: 添加考试任务失败");
        }
        examMapper.updateById(exam);
        cacheClient.setWithRandomExpire(
                CACHE_EXAM_KEY+exam.getExamId(),
                exam1,
                CACHE_EXAM_TTL,
                TimeUnit.MINUTES
        );
        return exam.getExamId();
    }

    @Override
    @Transactional
    public Integer submitPaper(ExamCreater examCreater) {
        Exam exam = cacheClient.queryWithPassThrough(
                CACHE_EXAM_KEY,
                LOCK_EXAM_KEY,
                examCreater.getExamId(),
                Exam.class,
                id -> examMapper.selectById(examCreater.getExamId()),
                CACHE_EXAM_TTL,
                TimeUnit.MINUTES
        );

        // 获取试卷信息
        PaperCreater paperCreater = paperService.getPaperDetailById(exam.getPaperId());
        HashMap<Long,Integer> ScoreMap = ConvertUtil.convertPaperCreaterToMap(paperCreater);

        exam.setSubmitTime(LocalDateTime.now());
        if (exam.getSubmitTime().isAfter(exam.getStartTime().plusMinutes(paperCreater.getExamTime()+2))) {
            throw new RuntimeException("submitPaper: 超过考试时间");
        }
        exam.setExamScore(0);

        ArrayList<ExamDetail> newExamDetails = ConvertUtil.convertExamCreatersToExamDetails(examCreater);
        // 从缓存中获取旧的ExamDetail
        List<ExamDetail> oldExamDetail = cacheClient.queryListWithPassThrough(
                CACHE_EXAMDETAIL_KEY,
                LOCK_EXAMDETAIL_KEY,
                exam.getExamId(),
                ExamDetail.class,
                id -> examDetailMapper.selectByExamId(exam.getExamId()),
                CACHE_EXAMDETAIL_TTL,
                TimeUnit.MINUTES
        );
        // 混合新旧ExamDetail
        ArrayList<ExamDetail> examDetails = ConvertUtil.mixExamDetails(oldExamDetail, newExamDetails);

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
        return exam.getExamScore();
    }

    @Override
    public Long setExam(Exam exam) {
        examMapper.insert(exam);
        return exam.getExamId();
    }

    @Override
    public ArrayList<Long> setExams(List<Exam> exam) {
        ArrayList<Long> ids = new ArrayList<>();
        for (Exam e : exam) {
            examMapper.insert(e);
            ids.add(e.getExamId());
        }
        return ids;
    }

    @Override
    public void saveExam(ExamCreater examCreater) {
        Exam exam = cacheClient.queryWithPassThrough(
                CACHE_EXAM_KEY,
                LOCK_EXAM_KEY,
                examCreater.getExamId(),
                Exam.class,
                id -> examMapper.selectById(examCreater.getExamId()),
                CACHE_EXAM_TTL,
                TimeUnit.MINUTES
        );
        // 如果考试已经结束，不允许修改
        if (exam.isStatus()==true) {
            throw new RuntimeException("submitPaper: 考试已经结束，不允许修改");
        }

        ArrayList<ExamDetail> newExamDetails = ConvertUtil.convertExamCreatersToExamDetails(examCreater);
        // 从缓存中获取旧的ExamDetail
        List<ExamDetail> oldExamDetail = cacheClient.queryListWithPassThrough(
                CACHE_EXAMDETAIL_KEY,
                LOCK_EXAMDETAIL_KEY,
                exam.getExamId(),
                ExamDetail.class,
                id -> examDetailMapper.selectByExamId(exam.getExamId()),
                CACHE_EXAMDETAIL_TTL,
                TimeUnit.MINUTES
        );
        // 混合新旧ExamDetail
        ArrayList<ExamDetail> examDetails = ConvertUtil.mixExamDetails(oldExamDetail, newExamDetails);

        // 更新ExamDetail
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

    @Override
    @DS("slave")
    public ArrayList<Object> getExamDetail(Long examId) {
        Exam exam = cacheClient.queryWithPassThrough(
                CACHE_EXAM_KEY,
                LOCK_EXAM_KEY,
                examId,
                Exam.class,
                id -> examMapper.selectById(examId),
                CACHE_EXAM_TTL,
                TimeUnit.MINUTES
        );
        List<ExamDetail> ExamDetail = cacheClient.queryListWithPassThrough(
                CACHE_EXAMDETAIL_KEY,
                LOCK_EXAMDETAIL_KEY,
                examId,
                ExamDetail.class,
                id -> examDetailMapper.selectByExamId(examId),
                CACHE_EXAMDETAIL_TTL,
                TimeUnit.MINUTES
        );
        ArrayList<Object> result = new ArrayList<>();
        result.add(exam);
        result.add(ExamDetail);
        return result;
    }

    @Override
    @DS("slave")
    public ArrayList<Exam> getExams(Long userId, Long paperId, Boolean status, Integer pageNum, Integer pageSize) {
        IPage<Exam> page=new Page<>(pageNum,pageSize);
        List<Exam> exams = examMapper.selectList(page, new QueryWrapper<Exam>()
                .eq(userId!=null,"user_id",userId)
                .eq(paperId!=null,"paper_id",paperId)
                .eq(status!=null,"status",status)
        );
        return new ArrayList<Exam>(exams);
    }
}

