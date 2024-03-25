package cn.wxl475.service;

import cn.wxl475.pojo.Paper;
import cn.wxl475.pojo.PaperCreater;
import cn.wxl475.pojo.enums.QuestionType;

import java.util.ArrayList;
import java.util.List;

public interface PaperService {
    Long createPaper(PaperCreater paperCreater);

    void deletePaper(ArrayList<Long> arrayList);

    Long updatePaper(PaperCreater paperCreater);

    Paper getPaperById(Long paperId);

    PaperCreater getPaperDetailById(Long paperId);

    List<Paper> getPapers(String allField, Integer examTime, Integer totalScore, Integer pageNum, Integer pageSize, String sortField, Integer sortOrder);
}
