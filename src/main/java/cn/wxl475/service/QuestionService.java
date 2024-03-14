package cn.wxl475.service;

import cn.wxl475.pojo.Question;

import java.util.ArrayList;
import java.util.List;


public interface QuestionService {
    Long createQuestion(Question question);

    void deleteQuestion(ArrayList<Long> questionIds);

    Long updateQuestion(Question question);

    Question getQuestionById(Long questionId);

    List<Question> getQuestions(String allField, String tag);
}
