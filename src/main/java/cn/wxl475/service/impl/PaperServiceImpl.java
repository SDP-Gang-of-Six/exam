package cn.wxl475.service.impl;

import cn.wxl475.mapper.PaperMapper;
import cn.wxl475.mapper.PaperScoreMapper;
import cn.wxl475.pojo.exam.Paper;
import cn.wxl475.pojo.exam.PaperCreater;
import cn.wxl475.pojo.exam.PaperScore;
import cn.wxl475.pojo.exam.PaperScoreCreater;
import cn.wxl475.redis.CacheClient;
import cn.wxl475.repo.PaperEsRepo;
import cn.wxl475.service.PaperService;
import cn.wxl475.utils.ConvertUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static cn.wxl475.redis.RedisConstants.*;

@Service
public class PaperServiceImpl implements PaperService {

    @Autowired
    private PaperMapper paperMapper;
    @Autowired
    private PaperScoreMapper paperScoreMapper;
    @Autowired
    private CacheClient cacheClient;
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;
    @Autowired
    private PaperEsRepo paperEsRepo;


    @Override
    public Long createPaper(PaperCreater paperCreater) {
        Paper paper = ConvertUtil.convertPaperCreaterToPaper(paperCreater);
        ArrayList<PaperScoreCreater> paperScoreCreaters = paperCreater.getPaperScores();
        if (!isSumEqualTotalScore(paperScoreCreaters, paper.getTotalScore())) {
            return -1L;
        }
        paperMapper.insert(paper);
        paperEsRepo.save(paper);
        ArrayList<PaperScore> paperScores = ConvertUtil.convertPaperScoreCreatersToPaperScores(paperScoreCreaters, paper.getPaperId());
        for (PaperScore paperScore : paperScores) {
            paperScoreMapper.insert(paperScore);
        }
        return paper.getPaperId();
    }

    @Override
    public void deletePaper(ArrayList<Long> arrayList) {
        paperMapper.deleteBatchIds(arrayList);
        paperEsRepo.deleteAllById(arrayList);
        paperScoreMapper.deleteByPaperIds(arrayList.toArray(new Long[0]));
        for (Long id : arrayList) {
            cacheClient.delete(CACHE_PAPER_KEY + id);
            cacheClient.delete(CACHE_PAPER_SCORE_KEY + id);
        }
    }

    @Override
    @Transactional
    public Long updatePaper(PaperCreater paperCreater) {
        Paper paper = ConvertUtil.convertPaperCreaterToPaper(paperCreater);
        paperMapper.updateById(paper);
        Paper Repaper = cacheClient.resetKey(
                CACHE_PAPER_KEY,
                LOCK_PAPER_KEY,
                paper.getPaperId(),
                Paper.class,
                id -> paperMapper.selectById(paper.getPaperId()),
                CACHE_PAPER_TTL,
                TimeUnit.MINUTES
        );
        ArrayList<PaperScoreCreater> paperScoreCreaters = new ArrayList<PaperScoreCreater>();
        if (paperCreater.getPaperScores()!=null){
            paperScoreCreaters = paperCreater.getPaperScores();
            if (!isSumEqualTotalScore(paperScoreCreaters, Repaper.getTotalScore())) {
                throw new RuntimeException("试卷总分与题目分数不符");
            }
        }
        paperEsRepo.save(Repaper);
        if (paperScoreCreaters.size() != 0) {
            ArrayList<PaperScore> paperScores = ConvertUtil.convertPaperScoreCreatersToPaperScores(paperScoreCreaters, paper.getPaperId());
            ArrayList<Long> arrayList = new ArrayList<>();
            arrayList.add(paper.getPaperId());
            paperScoreMapper.deleteByPaperIds(arrayList.toArray(new Long[0]));
            for (PaperScore paperScore : paperScores) {
                paperScoreMapper.insert(paperScore);
            }
            cacheClient.resetKey(
                    CACHE_PAPER_SCORE_KEY,
                    LOCK_PAPER_SCORE_KEY,
                    paper.getPaperId(),
                    List.class,
                    id -> getPaperScoresByPaperId(paper.getPaperId()),
                    CACHE_PAPER_SCORE_TTL,
                    TimeUnit.MINUTES
            );
        }
        return paper.getPaperId();
    }

    @Override
    @DS("slave")
    public Paper getPaperById(Long paperId) {
        return cacheClient.queryWithPassThrough(
                CACHE_PAPER_KEY,
                LOCK_PAPER_KEY,
                paperId,
                Paper.class,
                id -> paperMapper.selectById(paperId),
                CACHE_PAPER_TTL,
                TimeUnit.MINUTES
        );
    }

    @Override
    public PaperCreater getPaperDetailById(Long paperId) {
        Paper paper = cacheClient.queryWithPassThrough(
                CACHE_PAPER_KEY,
                LOCK_PAPER_KEY,
                paperId,
                Paper.class,
                id -> paperMapper.selectById(paperId),
                CACHE_PAPER_TTL,
                TimeUnit.MINUTES
        );
        List<PaperScore> paperScores = cacheClient.queryListWithPassThrough(
                CACHE_PAPER_SCORE_KEY,
                LOCK_PAPER_SCORE_KEY,
                paperId,
                PaperScore.class,
                id -> getPaperScoresByPaperId(paperId),
                CACHE_PAPER_SCORE_TTL,
                TimeUnit.MINUTES
        );
        return ConvertUtil.convertPaperToPaperCreater(paper, (ArrayList<PaperScore>) paperScores);
    }

    @Override
    public List<Paper> getPapers(String allField, Integer examTime, Integer totalScore, Integer pageNum, Integer pageSize, String sortField, Integer sortOrder) {
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder().withPageable(PageRequest.of(pageNum-1, pageSize));
        if(allField!=null&& !allField.isEmpty()){
            queryBuilder.withQuery(QueryBuilders.multiMatchQuery(allField,"paperName","paperDescription","createTime","updateTime"));
        }
        if(examTime!=null){
            queryBuilder.withQuery(QueryBuilders.matchQuery("examTime", examTime));
        }
        if(totalScore!=null){
            queryBuilder.withQuery(QueryBuilders.matchQuery("totalScore",totalScore));
        }
        if(sortField==null || sortField.isEmpty()){
            sortField = "paperId";
        }
        if(sortOrder==null || !(sortOrder==1 || sortOrder==-1)){
            sortOrder=-1;
        }
        queryBuilder.withSorts(SortBuilders.fieldSort(sortField).order(sortOrder==-1? SortOrder.DESC:SortOrder.ASC));
        SearchHits<Paper> hits = elasticsearchRestTemplate.search(queryBuilder.build(), Paper.class);
        List<Paper> papers = new ArrayList<>();
        hits.forEach(paper -> papers.add(paper.getContent()));
        return papers;
    }


    private boolean isSumEqualTotalScore(ArrayList<PaperScoreCreater> paperScoreCreaters, Integer totalScore) {
        Integer sum = 0;
        for (PaperScoreCreater paperScoreCreater : paperScoreCreaters) {
            sum += paperScoreCreater.getScore();
        }
        return sum.equals(totalScore);
    }

    //延时120分钟执行一次，之后销毁
    @Scheduled
    private List<PaperScore> getPaperScoresByPaperId(Long paperId) {
        return paperScoreMapper.selectByPaperId(paperId);
    }
}
