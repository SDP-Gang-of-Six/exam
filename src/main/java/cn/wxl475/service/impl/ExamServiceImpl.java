package cn.wxl475.service.impl;

import cn.wxl475.mapper.ExamDetailMapper;
import cn.wxl475.mapper.ExamMapper;
import cn.wxl475.pojo.Exam;
import cn.wxl475.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExamServiceImpl implements ExamService {

    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private ExamDetailMapper examDetailMapper;

    @Override
    public Long startExam(Exam exam) {
        examMapper.insert(exam);

        return exam.getExamId();
    }
}

