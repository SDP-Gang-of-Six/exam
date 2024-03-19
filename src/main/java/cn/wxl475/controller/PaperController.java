package cn.wxl475.controller;

import cn.wxl475.pojo.Paper;
import cn.wxl475.pojo.PaperCreater;
import cn.wxl475.pojo.Result;
import cn.wxl475.service.PaperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/exam")
public class PaperController {

    @Autowired
    private PaperService paperService;

    @PostMapping("/createPaper")
    public Result createPaper(@RequestBody PaperCreater paperCreater) {
        Long paperId = paperService.createPaper(paperCreater);
        if (paperId.equals(-1L)) {
            return Result.error("createPaper: 试卷总分与题目分数不符");
        }
        return Result.success(paperId);
    }
    @PostMapping("/deletePaper")
    public Result deletePaper(@RequestBody() List<Long> paperIds) {
        ArrayList<Long> arrayList = new ArrayList<>(paperIds);
        paperService.deletePaper(arrayList);
        return Result.success();
    }
    @PostMapping("/updatePaper")
    public Result updatePaper(@RequestBody PaperCreater paperCreater) {
        Long paperId = paperService.updatePaper(paperCreater);
        if (paperId.equals(-1L)) {
            return Result.error("updatePaper: 试卷总分与题目分数不符");
        }
        return Result.success(paperId);
    }
    @PostMapping("/getPapers")
    public Result getPapers(@RequestBody Paper paper) {
        return Result.success();
    }
    @GetMapping("/getPaperById")
    public Result getPaperById(@RequestParam("paperId") Long paperId) {
        return Result.success(paperService.getPaperById(paperId));
    }
    @GetMapping("/getPaperDetailById")
    public Result getPaperDetailById(@RequestParam("paperId") Long paperId) {
        return Result.success(paperService.getPaperDetailById(paperId));
    }
}
