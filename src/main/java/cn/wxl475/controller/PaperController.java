package cn.wxl475.controller;

import cn.wxl475.pojo.exam.PaperCreater;
import cn.wxl475.pojo.Result;
import cn.wxl475.service.PaperService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
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
        try {
            paperService.deletePaper(arrayList);
        }catch (Exception e){
            log.info(e.getMessage());
            return Result.error(e.getMessage());
        }
        return Result.success();
    }
    @PostMapping("/updatePaper")
    public Result updatePaper(@RequestBody PaperCreater paperCreater) {
        Long paperId;
        try {
            paperId = paperService.updatePaper(paperCreater);
        } catch (Exception e) {
            log.info(Arrays.toString(e.getStackTrace()));
            return Result.error(e.getMessage());
        }
        return Result.success(paperId);
    }
    @GetMapping("/getPapers")
    public Result getPapers(@RequestParam(value = "allField", required = false) String allField,
                            @RequestParam(value = "examTime", required = false) Integer examTime,
                            @RequestParam(value = "totalScore", required = false) Integer totalScore,
                            @RequestParam(value = "pageNum", required = false) Integer pageNum,
                            @RequestParam(value = "pageSize", required = false) Integer pageSize,
                            @RequestParam(value = "sortField", required = false) String sortField,
                            @RequestParam(value = "sortOrder", required = false) Integer sortOrder) {
        if(pageNum<=0||pageSize<=0){
            return Result.error("页码或页大小不合法");
        }
        return Result.success(paperService.getPapers(allField, examTime, totalScore, pageNum, pageSize, sortField, sortOrder));
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
