package cn.wxl475.service.impl;

import cn.wxl475.mapper.QuestionMapper;
import cn.wxl475.pojo.Question;
import cn.wxl475.pojo.User;
import cn.wxl475.redis.CacheClient;
import cn.wxl475.service.PaperService;
import cn.wxl475.service.QuestionService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static cn.wxl475.redis.RedisConstants.CACHE_QUESTION_KEY;
import static cn.wxl475.redis.RedisConstants.CACHE_QUESTION_TTL;

@Service
public class QuestionServiceImpl implements QuestionService {

    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private CacheClient cacheClient;

    @Override
    public Long createQuestion(Question question) {
        questionMapper.insert(question);
        cacheClient.setWithLogicalExpire(CACHE_QUESTION_KEY + question.getQuestionId().toString(), question, CACHE_QUESTION_TTL, TimeUnit.MINUTES);
        return question.getQuestionId();
    }

    @Override
    public void deleteQuestion(ArrayList<Long> questionIds) {
        questionMapper.deleteBatchIds(questionIds);
        for (Long id : questionIds) {
            cacheClient.delete(CACHE_QUESTION_KEY + id.toString());
        }
    }

    @Override
    public Long updateQuestion(Question question) {
        questionMapper.updateById(question);
        cacheClient.setWithLogicalExpire(CACHE_QUESTION_KEY + question.getQuestionId().toString(), question, CACHE_QUESTION_TTL, TimeUnit.MINUTES);
        return question.getQuestionId();
    }

    @Override
    public Question getQuestionById(Long questionId) {
        return cacheClient.queryWithPassThrough(
                CACHE_QUESTION_KEY,
                questionId,
                Question.class,
                id ->  questionMapper.selectById(questionId),
                CACHE_QUESTION_TTL,
                TimeUnit.MINUTES
        );
    }

    @Override
    public List<Question> getQuestions(String allField, String tag) {
        if (tag.isEmpty() && allField.isEmpty()) {
            return questionMapper.selectList(null);
        }
        QueryWrapper<Question> wrapper = new QueryWrapper<Question>().eq("tag", tag);
        return questionMapper.selectList(wrapper);
    }
}
