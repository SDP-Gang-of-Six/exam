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
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
        ArrayList<PaperScore> paperScores = ConvertUtil.convertPaperScoreCreatersToPaperScores(paperScoreCreaters, paper.getPaperId());
        paperMapper.insert(paper);
        for (PaperScore paperScore : paperScores) {
            paperScoreMapper.insert(paperScore);
        }
        return paper.getPaperId();
    }

    @Override
    public void deletePaper(ArrayList<Long> arrayList) {
        paperMapper.deleteBatchIds(arrayList);
        paperScoreMapper.deleteByPaperIds(arrayList);
        for (Long id : arrayList) {
            cacheClient.delete(CACHE_PAPER_KEY + id.toString());
        }
    }

    @Override
    @Transactional
    public Long updatePaper(PaperCreater paperCreater) {
        Paper paper = ConvertUtil.convertPaperCreaterToPaper(paperCreater);
        ArrayList<PaperScoreCreater> paperScoreCreaters = paperCreater.getPaperScores();
        if (!isSumEqualTotalScore(paperScoreCreaters, paper.getTotalScore())) {
            return -1L;
        }
        ArrayList<PaperScore> paperScores = ConvertUtil.convertPaperScoreCreatersToPaperScores(paperScoreCreaters, paper.getPaperId());
        paperMapper.updateById(paper);
        cacheClient.resetKey(
                CACHE_PAPER_KEY,
                LOCK_PAPER_KEY,
                paper.getPaperId(),
                Paper.class,
                id -> paperMapper.selectById(paper.getPaperId()),
                CACHE_PAPER_TTL,
                TimeUnit.MINUTES
        );
        paperScoreMapper.deleteByPaperIds(new ArrayList<Long>() {{
            add(paper.getPaperId());
        }});
        for (PaperScore paperScore : paperScores) {
            paperScoreMapper.insert(paperScore);
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

    private boolean isSumEqualTotalScore(ArrayList<PaperScoreCreater> paperScoreCreaters, Integer totalScore) {
        Integer sum = 0;
        for (PaperScoreCreater paperScoreCreater : paperScoreCreaters) {
            sum += paperScoreCreater.getScore();
        }
        return sum.equals(totalScore);
    }
}
