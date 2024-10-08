package cn.wxl475.service;

import cn.wxl475.pojo.Page;
import cn.wxl475.pojo.exam.Paper;
import cn.wxl475.pojo.exam.PaperCreater;

import java.util.ArrayList;

public interface PaperService {
    Long createPaper(PaperCreater paperCreater);

    void deletePaper(ArrayList<Long> arrayList);

    Long updatePaper(PaperCreater paperCreater);

    Paper getPaperById(Long paperId);

    PaperCreater getPaperDetailById(Long paperId);

    Page<Paper> getPapers(String allField, Integer examTime, Integer totalScore, Integer pageNum, Integer pageSize, String sortField, Integer sortOrder);
}
