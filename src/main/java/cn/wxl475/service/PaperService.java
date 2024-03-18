package cn.wxl475.service;

import cn.wxl475.pojo.Paper;
import cn.wxl475.pojo.PaperCreater;

import java.util.ArrayList;

public interface PaperService {
    Long createPaper(PaperCreater paperCreater);

    void deletePaper(ArrayList<Long> arrayList);

    Long updatePaper(PaperCreater paperCreater);

    Paper getPaperById(Long paperId);
}
