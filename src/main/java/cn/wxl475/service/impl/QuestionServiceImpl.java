package cn.wxl475.service.impl;

import cn.wxl475.mapper.QuestionMapper;
import cn.wxl475.pojo.Question;
import cn.wxl475.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuestionServiceImpl implements QuestionService {

    @Autowired
    private QuestionMapper questionMapper;

    @Override
    public Long createQuestion(Question question) {
        questionMapper.insert(question);
        return null;
    }
}
