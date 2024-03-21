package cn.wxl475.service.impl;

import cn.wxl475.mapper.PaperMapper;
import cn.wxl475.mapper.PaperScoreMapper;
import cn.wxl475.pojo.Paper;
import cn.wxl475.pojo.PaperCreater;
import cn.wxl475.pojo.PaperScore;
import cn.wxl475.pojo.PaperScoreCreater;
import cn.wxl475.redis.CacheClient;
import cn.wxl475.service.PaperService;
import cn.wxl475.utils.ConvertUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import jdk.jfr.Enabled;
import org.springframework.beans.factory.annotation.Autowired;
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


    @Override
    public Long createPaper(PaperCreater paperCreater) {
        Paper paper = ConvertUtil.convertPaperCreaterToPaper(paperCreater);
        ArrayList<PaperScoreCreater> paperScoreCreaters = paperCreater.getPaperScores();
        if (!isSumEqualTotalScore(paperScoreCreaters, paper.getTotalScore())) {
            return -1L;
        }
        paperMapper.insert(paper);
        ArrayList<PaperScore> paperScores = ConvertUtil.convertPaperScoreCreatersToPaperScores(paperScoreCreaters, paper.getPaperId());
        for (PaperScore paperScore : paperScores) {
            paperScoreMapper.insert(paperScore);
        }
        return paper.getPaperId();
    }

    @Override
    public void deletePaper(ArrayList<Long> arrayList) {
        paperMapper.deleteBatchIds(arrayList);
        paperScoreMapper.deleteByPaperIds(arrayList.toArray(new Long[0]));
        for (Long id : arrayList) {
            cacheClient.delete(CACHE_PAPER_KEY + id);
            cacheClient.delete(CACHE_PAPERSCORE_KEY + id);
        }
    }

    @Override
    @Transactional
    public Long updatePaper(PaperCreater paperCreater) {
        Paper paper = ConvertUtil.convertPaperCreaterToPaper(paperCreater);
        ArrayList<PaperScoreCreater> paperScoreCreaters = paperCreater.getPaperScores();
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
        if (!isSumEqualTotalScore(paperScoreCreaters, Repaper.getTotalScore())) {
            throw new RuntimeException("试卷总分与题目分数不符");
        }
        if (paperScoreCreaters.size() != 0) {
            ArrayList<PaperScore> paperScores = ConvertUtil.convertPaperScoreCreatersToPaperScores(paperScoreCreaters, paper.getPaperId());
            ArrayList<Long> arrayList = new ArrayList<>();
            arrayList.add(paper.getPaperId());
            paperScoreMapper.deleteByPaperIds(arrayList.toArray(new Long[0]));
            for (PaperScore paperScore : paperScores) {
                paperScoreMapper.insert(paperScore);
            }
            cacheClient.resetKey(
                    CACHE_PAPERSCORE_KEY,
                    LOCK_PAPERSCORE_KEY,
                    paper.getPaperId(),
                    List.class,
                    id -> getPaperScoresByPaperId(paper.getPaperId()),
                    CACHE_PAPERSCORE_TTL,
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
                CACHE_PAPERSCORE_KEY,
                LOCK_PAPERSCORE_KEY,
                paperId,
                PaperScore.class,
                id -> getPaperScoresByPaperId(paperId),
                CACHE_PAPERSCORE_TTL,
                TimeUnit.MINUTES
        );
        return ConvertUtil.convertPaperToPaperCreater(paper, (ArrayList<PaperScore>) paperScores);
    }


    private boolean isSumEqualTotalScore(ArrayList<PaperScoreCreater> paperScoreCreaters, Integer totalScore) {
        Integer sum = 0;
        for (PaperScoreCreater paperScoreCreater : paperScoreCreaters) {
            sum += paperScoreCreater.getScore();
        }
        return sum.equals(totalScore);
    }

    private List<PaperScore> getPaperScoresByPaperId(Long paperId) {
        return paperScoreMapper.selectByPaperId(paperId);
    }
}
