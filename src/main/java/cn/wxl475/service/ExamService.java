package cn.wxl475.service;

import cn.wxl475.pojo.Exam;
import cn.wxl475.pojo.ExamCreater;

import java.util.ArrayList;
import java.util.List;

public interface ExamService {
    Long startExam(Exam exam);

    Integer submitPaper(ExamCreater examCreater);

    Long setExam(Exam exam);

    ArrayList<Long> setExams(List<Exam> exam);

    void saveExam(ExamCreater examCreater);

    ArrayList<Object> getExamDetail(Long examId);

    ArrayList<Exam> getExams(Long userId, Long paperId, Boolean status, Integer pageNum, Integer pageSize);
}
