package cn.wxl475.service;

import cn.wxl475.pojo.Paper;

import java.util.ArrayList;

public interface PaperService {
    Long createPaper(Paper paper);

    void deletePaper(ArrayList<Long> arrayList);
}
