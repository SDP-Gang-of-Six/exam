package cn.wxl475.service.impl;

import cn.hutool.core.thread.ThreadUtil;
import cn.wxl475.client.UserClient;
import cn.wxl475.exception.ExamSolveException;
import cn.wxl475.mapper.ExamDetailMapper;
import cn.wxl475.mapper.ExamMapper;
import cn.wxl475.mapper.QuestionMapper;
import cn.wxl475.pojo.enums.RedisDelayQueueEnum;
import cn.wxl475.pojo.exam.*;
import cn.wxl475.redis.CacheClient;
import cn.wxl475.service.ExamService;
import cn.wxl475.service.PaperService;
import cn.wxl475.service.QuestionService;
import cn.wxl475.utils.ConvertUtil;
import cn.wxl475.utils.RedisDelayQueueUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static cn.wxl475.redis.RedisConstants.*;

@Service
@Slf4j
public class ExamServiceImpl implements ExamService {

    private final ExamMapper examMapper;
    private final ExamDetailMapper examDetailMapper;
    private final RedisDelayQueueUtil redisDelayQueueUtil;
    private final CacheClient cacheClient;
    private final QuestionMapper questionMapper;
    private final PaperService paperService;
    private final UserClient userClient;
    private final QuestionService questionService;

    @Autowired
    public ExamServiceImpl(ExamMapper examMapper, ExamDetailMapper examDetailMapper, RedisDelayQueueUtil redisDelayQueueUtil, CacheClient cacheClient, QuestionMapper questionMapper, PaperService paperService, UserClient userClient, QuestionService questionService) {
        this.examMapper = examMapper;
        this.examDetailMapper = examDetailMapper;
        this.redisDelayQueueUtil = redisDelayQueueUtil;
        this.cacheClient = cacheClient;
        this.questionMapper = questionMapper;
        this.paperService = paperService;
        this.userClient = userClient;
        this.questionService = questionService;
    }

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
            throw new ExamSolveException("startExam: 考试已经开始,请勿重复开始考试");
        }
        if (LocalDateTime.now().isAfter(exam1.getAllowEndTime()) || LocalDateTime.now().isBefore(exam1.getAllowStartTime())) {
            throw new ExamSolveException("startExam: 不在考试时间内");
        }
        exam1.setStartTime(LocalDateTime.now());
        PaperCreater paperCreater = paperService.getPaperDetailById(exam1.getPaperId());
        redisDelayQueueUtil.addDelayQueue(exam1.getExamId(), paperCreater.getExamTime() * 60 + 30, TimeUnit.SECONDS, RedisDelayQueueEnum.EXAM_AUTO_SUBMIT.getCode());
        examMapper.updateById(exam1);
        cacheClient.setWithRandomExpire(
                CACHE_EXAM_KEY+exam1.getExamId(),
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
            throw new ExamSolveException("submitPaper: 超过考试时间");
        }
        exam.setExamScore(0);

        ArrayList<ExamDetail> newExamDetails = ConvertUtil.convertExamCreatersToExamDetails(examCreater);
        // 从缓存中获取旧的ExamDetail
        List<ExamDetail> oldExamDetail = cacheClient.queryListWithPassThrough(
                CACHE_EXAM_DETAIL_KEY,
                LOCK_EXAM_DETAIL_KEY,
                exam.getExamId(),
                ExamDetail.class,
                id -> examDetailMapper.selectByExamId(exam.getExamId()),
                CACHE_EXAM_DETAIL_TTL,
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
        //解除自动提交
        redisDelayQueueUtil.deleteDelayQueue(RedisDelayQueueEnum.EXAM_AUTO_SUBMIT.getCode(), exam.getExamId());
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
    @Transactional
    public ArrayList<Long> setExamsForUsers(ExamsWithUserId exams) {
        ArrayList<Long> ids = new ArrayList<>();
        Exam e = new Exam();
        e.setPaperId(exams.getPaperId());
        e.setAllowStartTime(exams.getAllowStartTime());
        e.setAllowEndTime(exams.getAllowEndTime());
        for (Long id : exams.getUserIds()) {
            e.setUserId(id);
            examMapper.insert(e);
            e.setExamId(e.getExamId()+1);
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
        if (exam.isStatus()) {
            throw new ExamSolveException("submitPaper: 考试已经结束，不允许修改");
        }

        ArrayList<ExamDetail> newExamDetails = ConvertUtil.convertExamCreatersToExamDetails(examCreater);
        // 从缓存中获取旧的ExamDetail
        List<ExamDetail> oldExamDetail = cacheClient.queryListWithPassThrough(
                CACHE_EXAM_DETAIL_KEY,
                LOCK_EXAM_DETAIL_KEY,
                exam.getExamId(),
                ExamDetail.class,
                id -> examDetailMapper.selectByExamId(exam.getExamId()),
                CACHE_EXAM_DETAIL_TTL,
                TimeUnit.MINUTES
        );
        // 混合新旧ExamDetail
        ArrayList<ExamDetail> examDetails = ConvertUtil.mixExamDetails(oldExamDetail, newExamDetails);

        // 更新ExamDetail
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

    @Override
    @DS("slave")
    public ExamOut getExamDetail(Long examId) {
        Exam exam = cacheClient.queryWithPassThrough(
                CACHE_EXAM_KEY,
                LOCK_EXAM_KEY,
                examId,
                Exam.class,
                id -> examMapper.selectById(examId),
                CACHE_EXAM_TTL,
                TimeUnit.MINUTES
        );
        List<ExamDetail> ExamDetails = cacheClient.queryListWithPassThrough(
                CACHE_EXAM_DETAIL_KEY,
                LOCK_EXAM_DETAIL_KEY,
                examId,
                ExamDetail.class,
                id -> examDetailMapper.selectByExamId(examId),
                CACHE_EXAM_DETAIL_TTL,
                TimeUnit.MINUTES
        );
        HashMap<String,ExamDetail> map = new HashMap<>();
        for (ExamDetail examDetail : ExamDetails) {
            map.put(examDetail.getQuestionId().toString(),examDetail);
        }
        ExamOut examOut = new ExamOut();
        examOut.setExam(exam);
        examOut.setExamDetails(ExamDetails);
        CountDownLatch countDownLatch = ThreadUtil.newCountDownLatch(2);
        ArrayList<PaperCreater> paperCreaters = new ArrayList<>();
        ThreadUtil.execAsync(()->{
            paperCreaters.add(paperService.getPaperDetailById(exam.getPaperId()));
            examOut.setPaper(ConvertUtil.convertPaperCreaterToPaper(paperCreaters.get(0)));
            countDownLatch.countDown();
        });
        ThreadUtil.execAsync(()->{
            examOut.setNickname(userClient.getNicknameById(exam.getUserId()).getData().toString());
            countDownLatch.countDown();
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        examOut.setQuestionOuts(new ArrayList<>());
        for(int i = 0; i < paperCreaters.get(0).getPaperScores().size(); i++) {
            Question question = questionService.getQuestionById(paperCreaters.get(0).getPaperScores().get(i).getQuestionId());
            examOut.getQuestionOuts().add(ConvertUtil.convertQuestionToQuestionOut(question, paperCreaters.get(0).getPaperScores().get(i).getScore()));
            if (map.containsKey(question.getQuestionId().toString())) {
                ExamDetail examDetail = map.get(question.getQuestionId().toString());
                if (examDetail.getYourOption() != null){
                    examOut.getQuestionOuts().get(i).setYourOption(examDetail.getYourOption().toString());
                }
                if (examDetail.getJudge() != null){
                    examOut.getQuestionOuts().get(i).setJudge(examDetail.getJudge());
                }
                if (examDetail.getBlank() != null){
                    examOut.getQuestionOuts().get(i).setBlank(examDetail.getBlank());
                }
            }
        }
        return examOut;
    }

    @Override
    @DS("slave")
    public cn.wxl475.pojo.Page<ExamOut> getExams(Long userId, Long paperId, Boolean status, Integer pageNum, Integer pageSize) {
        Page<Exam> page = new Page<>( pageNum, pageSize, true);
        ArrayList<ExamOut> examOuts = new ArrayList<>();
        IPage<Exam> examIPage = examMapper.selectPage(page, new QueryWrapper<Exam>()
                .eq(userId!=null,"user_id",userId)
                .eq(paperId!=null,"paper_id",paperId)
                .eq(status!=null,"status",status)
        );
        ArrayList<Exam> exams = new ArrayList<>(examIPage.getRecords());

        for (Exam exam : exams) {
            ExamOut examOut = new ExamOut();
            examOut.setExam(exam);
            examOut.setPaper(paperService.getPaperById(exam.getPaperId()));
            try {
                String str = userClient.getNicknameById(exam.getUserId()).getData().toString();
                examOut.setNickname(str);
            }catch (Exception e) {
                log.info("getExams: 用户不存在");
                examOut.setNickname("用户不存在");
            }
            examOuts.add(examOut);
        }
        Long total = examIPage.getTotal();

        return new cn.wxl475.pojo.Page<>(total,examOuts);
    }

    @Override
    public void deleteExams(List<Long> examIds) {
        examMapper.deleteBatchIds(examIds);
        examDetailMapper.deleteByExamIds(examIds.toArray(new Long[0]));
        for (Long examId : examIds) {
            cacheClient.delete(CACHE_EXAM_KEY+examId);
            cacheClient.delete(CACHE_EXAM_DETAIL_KEY +examId);
        }
    }
}

