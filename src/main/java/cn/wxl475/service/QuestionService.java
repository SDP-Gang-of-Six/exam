package cn.wxl475.service;

import cn.wxl475.pojo.exam.Question;
import cn.wxl475.pojo.enums.QuestionType;

import java.util.ArrayList;
import java.util.List;


public interface QuestionService {
    Long createQuestion(Question question);

    void deleteQuestion(ArrayList<Long> questionIds);

    Long updateQuestion(Question question);

    Question getQuestionById(Long questionId);

    List<Question> getQuestions(String allField, String tag, QuestionType questionType, Integer pageNum, Integer pageSize, String sortField, Integer sortOrder);
}
