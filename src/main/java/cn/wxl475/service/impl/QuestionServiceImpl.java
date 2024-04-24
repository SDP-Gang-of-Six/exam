package cn.wxl475.service.impl;

import cn.wxl475.exception.PaperScoreException;
import cn.wxl475.mapper.PaperScoreMapper;
import cn.wxl475.mapper.QuestionMapper;
import cn.wxl475.pojo.Page;
import cn.wxl475.pojo.enums.QuestionType;
import cn.wxl475.pojo.exam.PaperScore;
import cn.wxl475.pojo.exam.Question;
import cn.wxl475.redis.CacheClient;
import cn.wxl475.repo.QuestionEsRepo;
import cn.wxl475.service.QuestionService;
import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
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
import java.util.concurrent.TimeUnit;

import static cn.wxl475.redis.RedisConstants.*;

@Slf4j
@Service
public class QuestionServiceImpl implements QuestionService {

    private final QuestionMapper questionMapper;
    private final CacheClient cacheClient;
    private final ElasticsearchRestTemplate elasticsearchRestTemplate;
    private final QuestionEsRepo questionEsRepo;
    private final PaperScoreMapper paperScoreMapper;
    @Autowired
    public QuestionServiceImpl(QuestionMapper questionMapper, CacheClient cacheClient, ElasticsearchRestTemplate elasticsearchRestTemplate, QuestionEsRepo questionEsRepo, PaperScoreMapper paperScoreMapper) {
        this.questionMapper = questionMapper;
        this.cacheClient = cacheClient;
        this.elasticsearchRestTemplate = elasticsearchRestTemplate;
        this.questionEsRepo = questionEsRepo;
        this.paperScoreMapper = paperScoreMapper;
    }


    @Override
    public Long createQuestion(Question question) {
        questionMapper.insert(question);
        questionEsRepo.save(question);
        return question.getQuestionId();
    }

    @Override
    public void deleteQuestion(ArrayList<Long> questionIds) {
        for ( Long questionId : questionIds) {
            ArrayList<PaperScore> arrayList = paperScoreMapper.selectByQuestionId(questionId);
            if(!arrayList.isEmpty()){
                throw new PaperScoreException("deleteQuestion: 该题目已被试卷引用");
            }
        }
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
    public Page<Question> getQuestions(String allField, String tag, QuestionType questionType, Integer pageNum, Integer pageSize, String sortField, Integer sortOrder) {
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder().withPageable(PageRequest.of(pageNum-1, pageSize));
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if(allField!=null&& !allField.isEmpty()){
            boolQueryBuilder.must(QueryBuilders.multiMatchQuery(allField,"description","optionA","optionB","optionC","optionD","right_blank","questionType","createTime","updateTime"));
        }
        if(tag!=null && !tag.isEmpty()){
            boolQueryBuilder.filter(QueryBuilders.termQuery("tag",tag));
        }
        if(questionType!=null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("questionType",questionType.toString()));
        }
        if(sortField==null || sortField.isEmpty()){
            sortField = "questionId";
        }
        if(sortOrder==null || !(sortOrder==1 || sortOrder==-1)){
            sortOrder=-1;
        }
        if (boolQueryBuilder.hasClauses()) {
            queryBuilder.withQuery(boolQueryBuilder);
        }
        queryBuilder.withSorts(SortBuilders.fieldSort(sortField).order(sortOrder==-1? SortOrder.DESC:SortOrder.ASC));
        SearchHits<Question> hits = elasticsearchRestTemplate.search(queryBuilder.build(), Question.class);
        Long total = hits.getTotalHits();
        ArrayList<Question> questions = new ArrayList<>();
        hits.forEach(question -> questions.add(question.getContent()));
        return new Page<>(total, questions);
    }

//    public Boolean LockQuestions(ArrayList<Question> questions) {
//        for (Question question : questions) {
//            RLock lock = redisson.getLock(LOCK_QUESTION_KEY + question.getQuestionId());
//            lock.
//        }
//        return true;
//    }
}
