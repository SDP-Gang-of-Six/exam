package cn.wxl475.service.impl;

import cn.wxl475.mapper.PaperMapper;
import cn.wxl475.mapper.PaperScoreMapper;
import cn.wxl475.pojo.Paper;
import cn.wxl475.redis.CacheClient;
import cn.wxl475.service.PaperService;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public Long createPaper(Paper paper) {
        paperMapper.insert(paper);
        return paper.getPaperId();
    }

    @Override
    public void deletePaper(ArrayList<Long> arrayList) {
        paperMapper.deleteBatchIds(arrayList);
        paperScoreMapper.deleteBatchIds(arrayList);
        for (Long id : arrayList) {
            cacheClient.delete(CACHE_PAPER_KEY + id.toString());
        }
    }

    @Override
    public Long updatePaper(Paper paper) {
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
}
