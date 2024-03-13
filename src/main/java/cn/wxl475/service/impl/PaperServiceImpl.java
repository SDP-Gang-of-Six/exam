package cn.wxl475.service.impl;

import cn.wxl475.mapper.PaperMapper;
import cn.wxl475.mapper.PaperScoreMapper;
import cn.wxl475.service.PaperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaperServiceImpl implements PaperService {

    @Autowired
    private PaperMapper paperMapper;
    @Autowired
    private PaperScoreMapper paperScoreMapper;
}
