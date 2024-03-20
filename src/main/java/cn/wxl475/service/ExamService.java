package cn.wxl475.service;

import cn.wxl475.pojo.Exam;
import cn.wxl475.pojo.ExamCreater;

public interface ExamService {
    Long startExam(Exam exam);

    void submitPaper(ExamCreater examCreater);
}
