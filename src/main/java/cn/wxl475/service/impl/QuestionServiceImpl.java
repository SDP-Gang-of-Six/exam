package cn.wxl475.service.impl;

import cn.wxl475.mapper.QuestionMapper;
import cn.wxl475.pojo.Question;
import cn.wxl475.pojo.enums.QuestionType;
import cn.wxl475.redis.CacheClient;
import cn.wxl475.repo.QuestionEsRepo;
import cn.wxl475.service.QuestionService;
import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static cn.wxl475.redis.RedisConstants.*;

@Slf4j
@Service
public class QuestionServiceImpl implements QuestionService {

    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private CacheClient cacheClient;
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;
    @Autowired
    private QuestionEsRepo questionEsRepo;

    @Override
    public Long createQuestion(Question question) {
        questionMapper.insert(question);
        questionEsRepo.save(question);
        return question.getQuestionId();
    }

    @Override
    public void deleteQuestion(ArrayList<Long> questionIds) {
        questionMapper.deleteBatchIds(questionIds);
        questionEsRepo.deleteAllById(questionIds);
        for (Long id : questionIds) {
            cacheClient.delete(CACHE_QUESTION_KEY + id.toString());
        }
    }

    @Override
    public Long updateQuestion(Question question) {
        questionMapper.updateById(question);
        Question Requestion = cacheClient.resetKey(
                CACHE_QUESTION_KEY,
                LOCK_QUESTION_KEY,
                question.getQuestionId(),
                Question.class,
                id -> questionMapper.selectById(question.getQuestionId()),
                CACHE_QUESTION_TTL,
                TimeUnit.MINUTES
        );
        questionEsRepo.save(Requestion);
        return question.getQuestionId();
    }

    @Override
    @DS("slave")
    public Question getQuestionById(Long questionId) {
        return cacheClient.queryWithPassThrough(
                CACHE_QUESTION_KEY,
                LOCK_QUESTION_KEY,
                questionId,
                Question.class,
                id ->  questionMapper.selectById(questionId),
                CACHE_QUESTION_TTL,
                TimeUnit.MINUTES
        );
    }

    @Override
    @DS("slave")
    public List<Question> getQuestions(String allField, String tag, QuestionType questionType, Integer pageNum, Integer pageSize, String sortField, Integer sortOrder) {
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder().withPageable(PageRequest.of(pageNum-1, pageSize));
        if(allField!=null&& !allField.isEmpty()){
            queryBuilder.withQuery(QueryBuilders.multiMatchQuery(allField,"description","optionA","optionB","optionC","optionD","right_blank","questionType","createTime","updateTime"));
        }
        if(tag!=null && !tag.isEmpty()){
            queryBuilder.withQuery(QueryBuilders.termQuery("tag",tag));
        }
        if(questionType!=null){
            queryBuilder.withQuery(QueryBuilders.termQuery("questionType",questionType.toString()));
        }
        if(sortField==null || sortField.isEmpty()){
            sortField = "questionId";
        }
        if(sortOrder==null || !(sortOrder==1 || sortOrder==-1)){
            sortOrder=-1;
        }
        queryBuilder.withSorts(SortBuilders.fieldSort(sortField).order(sortOrder==-1? SortOrder.DESC:SortOrder.ASC));
        SearchHits<Question> hits = elasticsearchRestTemplate.search(queryBuilder.build(), Question.class);
        List<Question> questions = new ArrayList<>();
        hits.forEach(question -> questions.add(question.getContent()));
        return questions;
    }

}
