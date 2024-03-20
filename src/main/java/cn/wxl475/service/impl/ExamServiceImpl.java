package cn.wxl475.service.impl;

import cn.wxl475.mapper.ExamDetailMapper;
import cn.wxl475.mapper.ExamMapper;
import cn.wxl475.pojo.Exam;
import cn.wxl475.pojo.ExamCreater;
import cn.wxl475.service.ExamService;
import cn.wxl475.helper.ExamDelayQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExamServiceImpl implements ExamService {

    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private ExamDetailMapper examDetailMapper;
    @Autowired
    private ExamDelayQueue examDelayQueue;

    @Override
    public Long startExam(Exam exam) {
        examMapper.insert(exam);
        examDelayQueue.addJob(exam.getExamId().toString(), exam.getDuration() * 60000 + 30000);
        return exam.getExamId();
    }

    @Override
    public void submitPaper(ExamCreater examCreater) {

    }
}

